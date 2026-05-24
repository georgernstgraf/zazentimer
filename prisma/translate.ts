import { OpencodeClient } from "./lib/opencode_client.ts";
import { verify, InputString, VerifyError } from "./lib/verify.ts";
import {
  getOrCreateLanguage,
  getOrCreateModel,
  getExistingVotes,
  upsertVote,
  upsertProficiency,
  getAllModels,
  getAllLanguages,
  getAllMasterStrings,
} from "./lib/db.ts";

const SKILL_MD = await Deno.readTextFile(
  new URL("../.opencode/skills/translate/SKILL.md", import.meta.url),
);

const START_TIME = Date.now();
const MAX_DURATION_MS = 600_000;
const MAX_RETRIES = 3;

function isTimeUp(): boolean {
  return Date.now() - START_TIME >= MAX_DURATION_MS;
}

function parseArgs() {
  const args = Deno.args;
  let model: string | undefined;
  let locale: string | undefined;
  let all = false;

  for (let i = 0; i < args.length; i++) {
    switch (args[i]) {
      case "--model":
        model = args[++i];
        break;
      case "--locale":
        locale = args[++i];
        break;
      case "--all":
        all = true;
        break;
      case "--help":
        console.log(
          "Usage: deno task translate -- [--model <name> --locale <bcp47> | --all]",
        );
        Deno.exit(0);
    }
  }

  if (all) {
    return { all: true as const, model: undefined, locale: undefined };
  }
  if (model && locale) {
    return { all: false as const, model, locale };
  }
  console.error("Specify --model <name> --locale <bcp47> or --all");
  Deno.exit(1);
}

interface DispatchInput {
  locale: { bcp_47: string; english_name: string };
  app_name: string;
  strings: InputString[];
}

interface DispatchOutput {
  locale: string;
  model: string;
  proficiency: number;
  translations: Array<{ key: string; translation: string | null }>;
}

async function dispatch(
  opencode: OpencodeClient,
  modelName: string,
  langBcp47: string,
  langEnglishName: string,
  strings: InputString[],
): Promise<DispatchOutput> {
  const input: DispatchInput = {
    locale: { bcp_47: langBcp47, english_name: langEnglishName },
    app_name: "ZazenTimer",
    strings,
  };

  const sessionId = await opencode.createSession(SKILL_MD);

  let lastError: string | undefined;
  for (let retry = 0; retry < MAX_RETRIES; retry++) {
    const raw = await opencode.sendMessage(
      sessionId,
      JSON.stringify(input, null, 2),
    );

    try {
      const result = verify(raw, langBcp47, modelName, strings);
      return result;
    } catch (e) {
      lastError = e instanceof VerifyError ? e.message : String(e);
      console.error(
        `  Retry ${retry + 1}/${MAX_RETRIES} for ${langBcp47}: ${lastError}`,
      );
      if (retry < MAX_RETRIES - 1) {
        await opencode.sendMessage(
          sessionId,
          `Verification failed: ${lastError}. Please fix and return valid JSON.`,
        );
      }
    }
  }

  await opencode.closeSession(sessionId);
  throw new Error(
    `Failed after ${MAX_RETRIES} retries for ${langBcp47}: ${lastError}`,
  );
}

async function processOne(
  opencode: OpencodeClient,
  modelName: string,
  langBcp47: string,
) {
  const language = await getOrCreateLanguage(langBcp47);
  const model = await getOrCreateModel(modelName);
  const allMasterStrings = await getAllMasterStrings();

  const existingVoteIds = await getExistingVotes(model.id, language.id);
  const missingStrings = allMasterStrings
    .filter((s) => !existingVoteIds.has(s.id))
    .map((s) => ({ key: s.text, text: s.text }));

  if (missingStrings.length === 0) {
    console.log(`  ✓ ${langBcp47}: all strings already have votes, skipping`);
    return;
  }

  console.log(
    `  → ${langBcp47}: ${missingStrings.length} missing strings, dispatching...`,
  );

  const output = await dispatch(
    opencode,
    modelName,
    langBcp47,
    language.english_name,
    missingStrings,
  );

  await upsertProficiency(language.id, model.id, output.proficiency);

  let stored = 0;
  let skipped = 0;
  for (const t of output.translations) {
    if (t.translation === null) {
      skipped++;
      continue;
    }
    const ms = allMasterStrings.find((s) => s.text === t.key);
    if (!ms) {
      console.error(`  ⚠ Master string not found for key '${t.key}', skipping`);
      skipped++;
      continue;
    }
    await upsertVote(language.id, model.id, ms.id, t.translation);
    stored++;
  }

  console.log(`  ✓ ${langBcp47}: stored ${stored}, skipped ${skipped}`);
}

async function main() {
  const args = parseArgs();
  const opencode = new OpencodeClient();

  if (args.all) {
    const models = await getAllModels();
    const languages = await getAllLanguages();

    console.log(
      `Full run: ${models.length} models × ${languages.length} locales`,
    );

    for (const m of models) {
      for (const lang of languages) {
        if (isTimeUp()) {
          console.log(
            `⏱ Timeout after 10 min. Stopped at model=${m.name}, locale=${lang.bcp_47}`,
          );
          Deno.exit(0);
        }
        console.log(`\n[${m.name}] [${lang.bcp_47}]`);
        try {
          await processOne(opencode, m.name, lang.bcp_47);
        } catch (e) {
          console.error(`  ✗ ${lang.bcp_47}: ${e instanceof Error ? e.message : String(e)}`);
        }
      }
    }

    console.log("\n✔ Full run complete");
  } else {
    console.log(`Single run: model=${args.model}, locale=${args.locale}`);
    await processOne(opencode, args.model, args.locale);
  }
}

if (import.meta.main) {
  await main();
}
