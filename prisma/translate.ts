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
  hasProficiency,
  getAllModels,
  getAllLanguages,
  getAllMasterStrings,
} from "./lib/db.ts";

// ── Provider Ranking (ascending = cheapest/worst -> most expensive/best) ─────
const PROVIDER_RANKING = [
  "privatemode-ai",
  "nvidia",
  "opencode-go",
  "opencode",
  "openrouter",
  "github-copilot",
  "google",
  "openai",
  "anthropic",
];

function getProviderRank(providerID: string): number {
  const idx = PROVIDER_RANKING.indexOf(providerID);
  return idx === -1 ? 99 : idx;
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
    const modelPath = slug.slice(firstSlash + 1);
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

// ── Logging ───────────────────────────────────────────────────────────────────
const LOG_DIR = "logs";
const LOG_FILE = "logs/orchestrator.log";

function ts(): string {
  return new Date().toISOString();
}

function ensureLogDir(): void {
  try {
    Deno.mkdirSync(LOG_DIR, { recursive: true });
  } catch {
    // ignore
  }
}

function writeLog(line: string): void {
  ensureLogDir();
  try {
    Deno.writeTextFileSync(LOG_FILE, line + "\n", { append: true, create: true });
  } catch {
    // ignore write errors
  }
}

function log(msg: string): void {
  const line = `[${ts()}] ${msg}`;
  console.log(msg);
  writeLog(line);
}

function logError(msg: string): void {
  const line = `[${ts()}] ERROR: ${msg}`;
  console.error(msg);
  writeLog(line);
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
  minProficiency: number;
  model?: string;
  locale?: string;
}

function parseArgs(): CliArgs {
  const args = Deno.args;
  let all = false;
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
      case "--min-proficiency":
        minProficiency = parseInt(args[++i], 10);
        break;
      case "--help":
        log(
          `Usage: deno task translate -- [OPTIONS]

Options:
  --model <name> --locale <bcp47>    Single (model, locale) pair
  --all                               Full matrix over all models x locales
  --min-proficiency <N>               Skip locales below this proficiency (default: ${DEFAULT_MIN_PROFICIENCY})
  --help                              Show this help`,
        );
        Deno.exit(0);
    }
  }

  if (all) {
    return { all: true, model: undefined, locale: undefined, minProficiency };
  }
  if (model && locale) {
    return { all: false, model, locale, minProficiency };
  }
  logError("Specify --model <name> --locale <bcp47> or --all");
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
  log(`Recovering stale ${OUTPUT_FILE}...`);

  try {
    const parsed = JSON.parse(raw);
    if (parsed.locale && parsed.model && parsed.proficiency) {
      const language = await getOrCreateLanguage(parsed.locale);
      const modelDb = await getOrCreateModel(parsed.model);

      if (parsed.translations) {
        const result = verifyTranslation(raw, parsed.locale, parsed.model, []);
        await upsertProficiency(language.id, modelDb.id, result.proficiency);
        const allMs = await getAllMasterStrings();
        for (const t of result.translations) {
          if (t.translation === null) continue;
          const ms = allMs.find((s) => s.text === t.key);
          if (ms) await upsertVote(language.id, modelDb.id, ms.id, t.translation);
        }
      } else {
        const result = verifyProficiency(raw, parsed.locale, parsed.model);
        await upsertProficiency(language.id, modelDb.id, result.proficiency);
      }

      await Deno.remove(OUTPUT_FILE);
      log("Recovery successful");
    }
  } catch (e) {
    logError(`Cannot recover ${OUTPUT_FILE}: ${e}`);
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
      const modelDb = await getOrCreateModel(modelName);
      await upsertProficiency(language.id, modelDb.id, result.proficiency);
      await Deno.remove(OUTPUT_FILE);

      const rank = getProviderRank(modelRef.providerID);
      log(`proficiency ${modelName} ${langBcp47} ${modelRef.providerID} rank=${rank} → ${result.proficiency}`);
      return result.proficiency;
    } catch (e) {
      logError(
        `${modelRef.providerID}/${modelRef.modelID} failed: ${e instanceof Error ? e.message : e}`,
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
        const modelDb = await getOrCreateModel(modelName);

        await upsertProficiency(language.id, modelDb.id, result.proficiency);

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
          await upsertVote(language.id, modelDb.id, ms.id, t.translation);
          stored++;
        }

        await Deno.remove(OUTPUT_FILE);

        const rank = getProviderRank(modelRef.providerID);
        log(`translate ${modelName} ${langBcp47} ${modelRef.providerID} rank=${rank} → stored ${stored} skipped ${skipped}`);
        return;
      } catch (e) {
        lastError = e instanceof VerifyError ? e.message : String(e);
        logError(
          `${modelRef.providerID}/${modelRef.modelID} retry ${retry + 1}/${MAX_RETRIES}: ${lastError}`,
        );
      }
    }
  }

  await Deno.remove(OUTPUT_FILE);
  logError(
    `${modelName} ${langBcp47}: failed after ${MAX_RETRIES} retries across ${chain.length} provider(s)`,
  );
}

// ── Single (model, locale) run ──────────────────────────────────────────────
async function runOne(
  opencode: OpencodeClient,
  modelName: string,
  langBcp47: string,
  langEnglishName: string,
  availableModels: Map<string, ModelEntry[]>,
  minProficiency: number,
): Promise<void> {
  const modelDb = await getOrCreateModel(modelName);
  const language = await getOrCreateLanguage(langBcp47);

  // Step 1: Proficiency (on-demand if not in DB)
  if (!(await hasProficiency(modelDb.id, language.id))) {
    try {
      const p = await dispatchProficiency(
        opencode,
        modelName,
        langBcp47,
        langEnglishName,
        availableModels,
      );
      log(`proficiency ${modelName} ${langBcp47} → ${p}`);
    } catch (e) {
      logError(`${modelName} ${langBcp47}: proficiency failed, skipping translate: ${e instanceof Error ? e.message : String(e)}`);
      return;
    }
  }

  // Step 2: Translate (only missing strings)
  const allMs = await getAllMasterStrings();
  const existing = await getExistingVotes(modelDb.id, language.id);
  const missing = allMs.filter((s) => !existing.has(s.id));

  if (missing.length === 0) {
    log(`${modelName} ${langBcp47}: all strings have votes, skipping`);
    return;
  }

  const strings = missing.map((s) => ({ key: s.text, text: s.text }));
  try {
    await dispatchTranslate(
      opencode,
      modelName,
      langBcp47,
      langEnglishName,
      strings,
      availableModels,
    );
  } catch (e) {
    logError(`${modelName} ${langBcp47}: ${e instanceof Error ? e.message : String(e)}`);
  }
}

// ── Full matrix run ──────────────────────────────────────────────────────────
async function runAll(
  opencode: OpencodeClient,
  models: { id: number; name: string }[],
  languages: { id: number; bcp_47: string; english_name: string }[],
  availableModels: Map<string, ModelEntry[]>,
  minProficiency: number,
): Promise<void> {
  for (const m of models) {
    for (const lang of languages) {
      if (isTimeUp()) {
        log(`Timeout. Stopped at model=${m.name}, locale=${lang.bcp_47}`);
        Deno.exit(0);
      }
      await runOne(opencode, m.name, lang.bcp_47, lang.english_name, availableModels, minProficiency);
    }
  }
}

// ── Main ─────────────────────────────────────────────────────────────────────
async function main() {
  const args = parseArgs();
  const opencode = new OpencodeClient();

  log("Fetching available models from opencode...");
  const availableModels = await fetchAvailableModels();
  log(`Found ${availableModels.size} unique models across providers`);

  await recoverStaleOutput();

  if (args.all) {
    const models = await getAllModels();
    const languages = await getAllLanguages();
    log(`Full run: ${models.length} models x ${languages.length} locales`);

    await runAll(opencode, models, languages, availableModels, args.minProficiency);

    log("Full run complete");
  } else if (args.model && args.locale) {
    const language = await getOrCreateLanguage(args.locale);
    await runOne(opencode, args.model, args.locale, language.english_name, availableModels, args.minProficiency);
  }
}

if (import.meta.main) {
  await main();
}
