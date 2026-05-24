import { OpencodeClient } from "./lib/opencode_client.ts";
import {
  verifyTranslation,
  verifyProficiency,
  InputString,
  VerifyError,
} from "./lib/verify.ts";
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

const SKILL_TRANSLATE = await Deno.readTextFile(
  new URL("../.opencode/skills/translate/SKILL.md", import.meta.url),
);
const SKILL_PROFICIENCY = await Deno.readTextFile(
  new URL("../.opencode/skills/proficiency/SKILL.md", import.meta.url),
);

const INPUT_FILE = "translate-input.json";
const OUTPUT_FILE = "translate-output.json";

const START_TIME = Date.now();
const MAX_DURATION_MS = 600_000;
const MAX_RETRIES = 3;
const DEFAULT_MIN_PROFICIENCY = 2;

function elapsed(): number {
  return Date.now() - START_TIME;
}

function isTimeUp(): boolean {
  return elapsed() >= MAX_DURATION_MS;
}

interface CliArgs {
  all: boolean;
  proficiencyOnly: boolean;
  translateOnly: boolean;
  minProficiency: number;
  model?: string;
  locale?: string;
}

function parseArgs(): CliArgs {
  const args = Deno.args;
  let all = false;
  let proficiencyOnly = false;
  let translateOnly = false;
  let minProficiency = DEFAULT_MIN_PROFICIENCY;
  let model: string | undefined;
  let locale: string | undefined;

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
      case "--proficiency-only":
        proficiencyOnly = true;
        break;
      case "--translate-only":
        translateOnly = true;
        break;
      case "--min-proficiency":
        minProficiency = parseInt(args[++i], 10);
        break;
      case "--help":
        console.log(
          `Usage: deno task translate -- [OPTIONS]

Options:
  --model <name> --locale <bcp47>    Single (model, locale) pair
  --all                               Full matrix over all models × locales
  --proficiency-only                  Only assess proficiencies (phase 1)
  --translate-only                    Only translate where proficiency exists (phase 2)
  --min-proficiency <N>               Skip locales below this proficiency (default: ${DEFAULT_MIN_PROFICIENCY})
  --help                              Show this help`,
        );
        Deno.exit(0);
    }
  }

  if (all) {
    return { all: true, model: undefined, locale: undefined, proficiencyOnly, translateOnly, minProficiency };
  }
  if (model && locale) {
    return { all: false, model, locale, proficiencyOnly, translateOnly, minProficiency };
  }
  console.error("Specify --model <name> --locale <bcp47> or --all");
  Deno.exit(1);
}

interface ProficiencyInput {
  locale: { bcp_47: string; english_name: string };
  app_name: string;
}

interface TranslateInput {
  locale: { bcp_47: string; english_name: string };
  app_name: string;
  strings: InputString[];
}

async function writeInput(data: unknown): Promise<void> {
  await Deno.writeTextFile(INPUT_FILE, JSON.stringify(data, null, 2));
}

async function writeOutput(raw: string): Promise<void> {
  await Deno.writeTextFile(OUTPUT_FILE, raw);
}

async function recoverStaleOutput(
  opencode: OpencodeClient,
): Promise<void> {
  try {
    await Deno.stat(OUTPUT_FILE);
  } catch {
    return; // no stale file
  }

  const raw = await Deno.readTextFile(OUTPUT_FILE);
  console.warn(`Found stale ${OUTPUT_FILE}, attempting recovery...`);

  try {
    const parsed = JSON.parse(raw);
    if (parsed.locale && parsed.model && parsed.proficiency) {
      const language = await getOrCreateLanguage(parsed.locale);
      const model = await getOrCreateModel(parsed.model);

      if (parsed.translations) {
        const result = verifyTranslation(raw, parsed.locale, parsed.model, []);
        await upsertProficiency(language.id, model.id, result.proficiency);
        const allMs = await getAllMasterStrings();
        for (const t of result.translations) {
          if (t.translation === null) continue;
          const ms = allMs.find((s) => s.text === t.key);
          if (ms) await upsertVote(language.id, model.id, ms.id, t.translation);
        }
      } else {
        const result = verifyProficiency(raw, parsed.locale, parsed.model);
        await upsertProficiency(language.id, model.id, result.proficiency);
      }

      await Deno.remove(OUTPUT_FILE);
      console.log("  ✓ Recovery successful");
    }
  } catch (e) {
    console.error(`  ✗ Cannot recover ${OUTPUT_FILE}: ${e}`);
    await Deno.remove(OUTPUT_FILE);
  }
}

async function dispatchProficiency(
  opencode: OpencodeClient,
  modelName: string,
  langBcp47: string,
  langEnglishName: string,
): Promise<number> {
  const input: ProficiencyInput = {
    locale: { bcp_47: langBcp47, english_name: langEnglishName },
    app_name: "ZazenTimer",
  };
  await writeInput(input);

  const sessionId = await opencode.createSession(SKILL_PROFICIENCY);
  const raw = await opencode.sendMessage(sessionId, JSON.stringify(input, null, 2));
  await writeOutput(raw);

  try {
    const result = verifyProficiency(raw, langBcp47, modelName);
    const language = await getOrCreateLanguage(langBcp47);
    const model = await getOrCreateModel(modelName);
    await upsertProficiency(language.id, model.id, result.proficiency);

    await Deno.remove(OUTPUT_FILE);
    return result.proficiency;
  } catch {
    await Deno.remove(OUTPUT_FILE);
    throw new VerifyError(`Proficiency assessment failed for ${langBcp47}`);
  }
}

async function dispatchTranslate(
  opencode: OpencodeClient,
  modelName: string,
  langBcp47: string,
  langEnglishName: string,
  strings: InputString[],
): Promise<void> {
  const input: TranslateInput = {
    locale: { bcp_47: langBcp47, english_name: langEnglishName },
    app_name: "ZazenTimer",
    strings,
  };
  await writeInput(input);

  const sessionId = await opencode.createSession(SKILL_TRANSLATE);

  let lastError: string | undefined;
  for (let retry = 0; retry < MAX_RETRIES; retry++) {
    const raw = await opencode.sendMessage(
      sessionId,
      JSON.stringify(input, null, 2),
    );
    await writeOutput(raw);

    try {
      const result = verifyTranslation(raw, langBcp47, modelName, strings);
      const language = await getOrCreateLanguage(langBcp47);
      const model = await getOrCreateModel(modelName);

      await upsertProficiency(language.id, model.id, result.proficiency);

      const allMs = await getAllMasterStrings();
      let stored = 0;
      let skipped = 0;
      for (const t of result.translations) {
        if (t.translation === null) {
          skipped++;
          continue;
        }
        const ms = allMs.find((s) => s.text === t.key);
        if (!ms) {
          skipped++;
          continue;
        }
        await upsertVote(language.id, model.id, ms.id, t.translation);
        stored++;
      }

      await Deno.remove(OUTPUT_FILE);
      console.log(`  ✓ ${langBcp47}: stored ${stored}, skipped ${skipped}`);
      return;
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

  await Deno.remove(OUTPUT_FILE);
  console.error(`  ✗ ${langBcp47}: failed after ${MAX_RETRIES} retries: ${lastError}`);
}

async function phaseProficiency(
  opencode: OpencodeClient,
  models: { id: number; name: string }[],
  languages: { id: number; bcp_47: string; english_name: string }[],
): Promise<void> {
  console.log("\n=== Phase 1: Proficiency Assessment ===");
  for (const m of models) {
    for (const lang of languages) {
      if (isTimeUp()) {
        console.log(`⏱ Timeout. Stopped at model=${m.name}, locale=${lang.bcp_47}`);
        Deno.exit(0);
      }
      process.stdout.write(`[${m.name}] [${lang.bcp_47}] `);
      try {
        const p = await dispatchProficiency(opencode, m.name, lang.bcp_47, lang.english_name);
        console.log(`  proficiency=${p}`);
      } catch (e) {
        console.log(`  ${e instanceof Error ? e.message : String(e)}`);
      }
    }
  }
}

async function phaseTranslate(
  opencode: OpencodeClient,
  models: { id: number; name: string }[],
  languages: { id: number; bcp_47: string; english_name: string }[],
  minProficiency: number,
): Promise<void> {
  console.log(`\n=== Phase 2: Translate (min-proficiency=${minProficiency}) ===`);
  const allMs = await getAllMasterStrings();

  for (const m of models) {
    for (const lang of languages) {
      if (isTimeUp()) {
        console.log(`⏱ Timeout. Stopped at model=${m.name}, locale=${lang.bcp_47}`);
        Deno.exit(0);
      }
      process.stdout.write(`[${m.name}] [${lang.bcp_47}] `);

      const existing = await getExistingVotes(m.id, lang.id);
      const missing = allMs.filter((s) => !existing.has(s.id));

      if (missing.length === 0) {
        console.log("  all strings have votes, skipping");
        continue;
      }

      if (existing.size === 0) {
        console.log("  no proficiency assessed yet, skipping");
        continue;
      }

      const strings = missing.map((s) => ({ key: s.text, text: s.text }));
      console.log(`  ${strings.length} missing`);
      try {
        await dispatchTranslate(opencode, m.name, lang.bcp_47, lang.english_name, strings);
      } catch (e) {
        console.error(`  ✗ ${e instanceof Error ? e.message : String(e)}`);
      }
    }
  }
}

async function singleRun(
  opencode: OpencodeClient,
  modelName: string,
  langBcp47: string,
): Promise<void> {
  const language = await getOrCreateLanguage(langBcp47);
  const model = await getOrCreateModel(modelName);

  // Proficiency check
  if (!model.id) {
    const p = await dispatchProficiency(opencode, modelName, langBcp47, language.english_name);
    console.log(`  proficiency=${p}`);
  }

  const allMs = await getAllMasterStrings();
  const existing = await getExistingVotes(model.id, language.id);
  const missing = allMs.filter((s) => !existing.has(s.id));

  if (missing.length === 0) {
    console.log(`  ${langBcp47}: all strings have votes`);
    return;
  }

  const strings = missing.map((s) => ({ key: s.text, text: s.text }));
  console.log(`  ${strings.length} missing strings`);
  await dispatchTranslate(opencode, modelName, langBcp47, language.english_name, strings);
}

async function main() {
  const args = parseArgs();
  const opencode = new OpencodeClient();
  await recoverStaleOutput(opencode);

  if (args.all) {
    const models = await getAllModels();
    const languages = await getAllLanguages();
    console.log(`Full run: ${models.length} models × ${languages.length} locales`);

    if (!args.translateOnly) await phaseProficiency(opencode, models, languages);
    if (!args.proficiencyOnly) await phaseTranslate(opencode, models, languages, args.minProficiency);

    console.log("\n✔ Full run complete");
  } else if (args.model && args.locale) {
    await singleRun(opencode, args.model, args.locale);
  }
}

if (import.meta.main) {
  await main();
}
