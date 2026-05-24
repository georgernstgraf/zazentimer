import { OpencodeClient, ModelRef, SendOptions } from "./lib/opencode_client.ts";
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

// ── Provider Ranking (ascending = cheapest/worst -> most expensive/best) ─────
// The orchestrator starts with the lowest rank and falls back to higher ranks.
// nvidia is ranked lowest (cheapest, often lower quality), anthropic highest.
const PROVIDER_RANKING: { prefix: string; rank: number; note: string }[] = [
  { prefix: "zai", rank: 1, note: "Zhipu AI — cheap" },
  { prefix: "zai-coding-plan", rank: 1, note: "Zhipu AI coding — cheap" },
  { prefix: "nvidia", rank: 2, note: "Nvidia NIM — cheap, often lower quality" },
  { prefix: "privatemode-ai", rank: 3, note: "Private mode" },
  { prefix: "opencode", rank: 4, note: "Free via Zen credits" },
  { prefix: "opencode-go", rank: 5, note: "Community models" },
  { prefix: "openrouter", rank: 6, note: "Paid aggregation" },
  { prefix: "github-copilot", rank: 7, note: "GitHub subscription" },
  { prefix: "google", rank: 8, note: "Direct Google API" },
  { prefix: "openai", rank: 9, note: "Direct OpenAI API" },
  { prefix: "anthropic", rank: 10, note: "Direct Anthropic API" },
];

function getProviderRank(providerID: string): number {
  const entry = PROVIDER_RANKING.find((r) => r.prefix === providerID);
  return entry?.rank ?? 99;
}

function normalizeModelName(name: string): string {
  return name.replace(/[.-]/g, "").toLowerCase();
}

interface ModelEntry {
  providerID: string;
  modelID: string;
  rank: number;
}

async function fetchAvailableModels(): Promise<Map<string, ModelEntry[]>> {
  const cmd = new Deno.Command("opencode", { args: ["models"] });
  const { stdout, stderr, success } = await cmd.output();
  if (!success) {
    throw new Error(
      `opencode models failed: ${new TextDecoder().decode(stderr)}`,
    );
  }

  const lines = new TextDecoder().decode(stdout).trim().split("\n");
  const rawMap = new Map<string, ModelEntry[]>();

  for (const line of lines) {
    const slug = line.trim();
    const firstSlash = slug.indexOf("/");
    if (firstSlash === -1) continue;

    const providerID = slug.slice(0, firstSlash);
    // modelPath may contain sub-provider (e.g. "anthropic/claude-opus-4.7")
    const modelPath = slug.slice(firstSlash + 1);
    // For matching against seed names use the last component after the final slash
    const lastSlash = modelPath.lastIndexOf("/");
    const matchKey = lastSlash === -1 ? modelPath : modelPath.slice(lastSlash + 1);
    const normalized = normalizeModelName(matchKey);

    if (!rawMap.has(normalized)) rawMap.set(normalized, []);
    rawMap.get(normalized)!.push({
      providerID,
      modelID: modelPath,
      rank: getProviderRank(providerID),
    });
  }

  for (const [, entries] of rawMap) {
    entries.sort((a, b) => a.rank - b.rank);
  }

  return rawMap;
}

function buildFallbackChain(
  seedModelName: string,
  available: Map<string, ModelEntry[]>,
): ModelRef[] {
  const normalized = normalizeModelName(seedModelName);
  const entries = available.get(normalized);
  if (!entries || entries.length === 0) return [];
  return entries.map((e) => ({ providerID: e.providerID, modelID: e.modelID }));
}

// ── Skill files ──────────────────────────────────────────────────────────────
const SKILL_TRANSLATE = await Deno.readTextFile(
  new URL("../.opencode/skills/translate/SKILL.md", import.meta.url),
);
const SKILL_PROFICIENCY = await Deno.readTextFile(
  new URL("../.opencode/skills/proficiency/SKILL.md", import.meta.url),
);

// ── Constants ────────────────────────────────────────────────────────────────
const INPUT_FILE = "translate-input.json";
const OUTPUT_FILE = "translate-output.json";

const START_TIME = Date.now();
const MAX_DURATION_MS = 600_000;
const MAX_RETRIES = 3;
const DEFAULT_MIN_PROFICIENCY = 2;

function isTimeUp(): boolean {
  return Date.now() - START_TIME >= MAX_DURATION_MS;
}

// ── CLI args ─────────────────────────────────────────────────────────────────
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
  --all                               Full matrix over all models x locales
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

// ── File helpers ─────────────────────────────────────────────────────────────
async function writeInput(data: unknown): Promise<void> {
  await Deno.writeTextFile(INPUT_FILE, JSON.stringify(data, null, 2));
}

async function writeOutput(raw: string): Promise<void> {
  await Deno.writeTextFile(OUTPUT_FILE, raw);
}

async function recoverStaleOutput(): Promise<void> {
  try {
    await Deno.stat(OUTPUT_FILE);
  } catch {
    return;
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
      console.log("  Recovery successful");
    }
  } catch (e) {
    console.error(`  Cannot recover ${OUTPUT_FILE}: ${e}`);
    await Deno.remove(OUTPUT_FILE);
  }
}

// ── Dispatch functions ───────────────────────────────────────────────────────
async function dispatchProficiency(
  opencode: OpencodeClient,
  modelName: string,
  langBcp47: string,
  langEnglishName: string,
  availableModels: Map<string, ModelEntry[]>,
): Promise<number> {
  const input = {
    locale: { bcp_47: langBcp47, english_name: langEnglishName },
    app_name: "ZazenTimer",
  };
  await writeInput(input);

  const chain = buildFallbackChain(modelName, availableModels);
  if (chain.length === 0) {
    throw new Error(`No available provider for model '${modelName}'`);
  }

  const sessionId = await opencode.createSession();

  for (const modelRef of chain) {
    const opts: SendOptions = {
      system: SKILL_PROFICIENCY,
      model: modelRef,
    };
    const raw = await opencode.sendMessage(
      sessionId,
      JSON.stringify(input, null, 2),
      opts,
    );
    await writeOutput(raw);

    try {
      const result = verifyProficiency(raw, langBcp47, modelName);
      const language = await getOrCreateLanguage(langBcp47);
      const model = await getOrCreateModel(modelName);
      await upsertProficiency(language.id, model.id, result.proficiency);
      await Deno.remove(OUTPUT_FILE);
      return result.proficiency;
    } catch (e) {
      console.error(
        `  ${modelRef.providerID}/${modelRef.modelID} failed: ${e instanceof Error ? e.message : e}`,
      );
    }
  }

  await Deno.remove(OUTPUT_FILE);
  throw new VerifyError(
    `Proficiency assessment for '${modelName}' in '${langBcp47}' failed across all ${chain.length} provider(s)`,
  );
}

async function dispatchTranslate(
  opencode: OpencodeClient,
  modelName: string,
  langBcp47: string,
  langEnglishName: string,
  strings: InputString[],
  availableModels: Map<string, ModelEntry[]>,
): Promise<void> {
  const input = {
    locale: { bcp_47: langBcp47, english_name: langEnglishName },
    app_name: "ZazenTimer",
    strings,
  };
  await writeInput(input);

  const chain = buildFallbackChain(modelName, availableModels);
  if (chain.length === 0) {
    throw new Error(`No available provider for model '${modelName}'`);
  }

  const sessionId = await opencode.createSession();

  for (const modelRef of chain) {
    let lastError: string | undefined;

    for (let retry = 0; retry < MAX_RETRIES; retry++) {
      const opts: SendOptions = {
        system: retry === 0 ? SKILL_TRANSLATE : undefined,
        model: retry === 0 ? modelRef : undefined,
      };

      const text = retry === 0
        ? JSON.stringify(input, null, 2)
        : `Verification failed: ${lastError}. Please fix and return valid JSON.`;

      const raw = await opencode.sendMessage(sessionId, text, opts);
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
        console.log(`  ${langBcp47}: stored ${stored}, skipped ${skipped}`);
        return;
      } catch (e) {
        lastError = e instanceof VerifyError ? e.message : String(e);
        console.error(
          `  Retry ${retry + 1}/${MAX_RETRIES}: ${lastError}`,
        );
      }
    }
  }

  await Deno.remove(OUTPUT_FILE);
  console.error(
    `  ${langBcp47}: failed after ${MAX_RETRIES} retries across ${chain.length} provider(s)`,
  );
}

// ── Phases ───────────────────────────────────────────────────────────────────
async function phaseProficiency(
  opencode: OpencodeClient,
  models: { id: number; name: string }[],
  languages: { id: number; bcp_47: string; english_name: string }[],
  availableModels: Map<string, ModelEntry[]>,
): Promise<void> {
  console.log("\n=== Phase 1: Proficiency Assessment ===");
  for (const m of models) {
    for (const lang of languages) {
      if (isTimeUp()) {
        console.log(`Timeout. Stopped at model=${m.name}, locale=${lang.bcp_47}`);
        Deno.exit(0);
      }
      process.stdout.write(`[${m.name}] [${lang.bcp_47}] `);
      try {
        const p = await dispatchProficiency(
          opencode,
          m.name,
          lang.bcp_47,
          lang.english_name,
          availableModels,
        );
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
  availableModels: Map<string, ModelEntry[]>,
  minProficiency: number,
): Promise<void> {
  console.log(`\n=== Phase 2: Translate (min-proficiency=${minProficiency}) ===`);
  const allMs = await getAllMasterStrings();

  for (const m of models) {
    for (const lang of languages) {
      if (isTimeUp()) {
        console.log(`Timeout. Stopped at model=${m.name}, locale=${lang.bcp_47}`);
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
        await dispatchTranslate(
          opencode,
          m.name,
          lang.bcp_47,
          lang.english_name,
          strings,
          availableModels,
        );
      } catch (e) {
        console.error(`  ${e instanceof Error ? e.message : String(e)}`);
      }
    }
  }
}

async function singleRun(
  opencode: OpencodeClient,
  modelName: string,
  langBcp47: string,
  availableModels: Map<string, ModelEntry[]>,
): Promise<void> {
  const language = await getOrCreateLanguage(langBcp47);

  const p = await dispatchProficiency(
    opencode,
    modelName,
    langBcp47,
    language.english_name,
    availableModels,
  );
  console.log(`  proficiency=${p}`);

  const allMs = await getAllMasterStrings();
  const model = await getOrCreateModel(modelName);
  const existing = await getExistingVotes(model.id, language.id);
  const missing = allMs.filter((s) => !existing.has(s.id));

  if (missing.length === 0) {
    console.log(`  ${langBcp47}: all strings have votes`);
    return;
  }

  const strings = missing.map((s) => ({ key: s.text, text: s.text }));
  console.log(`  ${strings.length} missing strings`);
  await dispatchTranslate(
    opencode,
    modelName,
    langBcp47,
    language.english_name,
    strings,
    availableModels,
  );
}

// ── Main ─────────────────────────────────────────────────────────────────────
async function main() {
  const args = parseArgs();
  const opencode = new OpencodeClient();

  console.log("Fetching available models from opencode...");
  const availableModels = await fetchAvailableModels();
  console.log(`  Found ${availableModels.size} unique models across providers`);

  await recoverStaleOutput();

  if (args.all) {
    const models = await getAllModels();
    const languages = await getAllLanguages();
    console.log(
      `Full run: ${models.length} models x ${languages.length} locales`,
    );

    if (!args.translateOnly) {
      await phaseProficiency(opencode, models, languages, availableModels);
    }
    if (!args.proficiencyOnly) {
      await phaseTranslate(
        opencode,
        models,
        languages,
        availableModels,
        args.minProficiency,
      );
    }

    console.log("\nFull run complete");
  } else if (args.model && args.locale) {
    await singleRun(opencode, args.model, args.locale, availableModels);
  }
}

if (import.meta.main) {
  await main();
}
