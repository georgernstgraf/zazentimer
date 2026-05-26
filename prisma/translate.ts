import {
    ModelRef,
    OpencodeClient,
    SendOptions,
} from "./lib/opencode_client.ts";
import {
    InputString,
    VerifyError,
    verifyProficiencyFile,
    verifyTranslationFile,
} from "./lib/verify.ts";
import { getPrisma } from "./lib/prisma.ts";
import {
    getAllLanguages,
    getAllMasterStrings,
    getAllModels,
    getExistingVotes,
    getNullExistingVotes,
    getOrCreateLanguage,
    getOrCreateModel,
    getProficiencyLevel,
    getSettledStrings,
    hasProficiency,
    upsertProficiency,
    upsertVote,
} from "./lib/db.ts";

// ── Provider Ranking (ascending = cheapest/worst -> most expensive/best) ─────
const PROVIDER_RANKING = [
    "zai-coding-plan",
    // "nvidia",
    "opencode-go",
    "github-copilot",
    "google",
    "opencode",
    "openrouter",
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
        const matchKey = lastSlash === -1
            ? modelPath
            : modelPath.slice(lastSlash + 1);
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
    return entries.map((e) => ({
        providerID: e.providerID,
        modelID: e.modelID,
    }));
}

// ── Logging ───────────────────────────────────────────────────────────────────
const LOG_DIR = "logs";
const LOG_FILE = "logs/orchestrator.log";

function ts(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}_${String(d.getHours()).padStart(2, "0")}-${String(d.getMinutes()).padStart(2, "0")}-${String(d.getSeconds()).padStart(2, "0")}`;
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
        Deno.writeTextFileSync(LOG_FILE, line + "\n", {
            append: true,
            create: true,
        });
    } catch {
        // ignore write errors
    }
}

function log(msg: string): void {
    const line = `[${ts()}] ${msg}`;
    console.log(line);
    writeLog(line);
}

function logError(msg: string): void {
    const line = `[${ts()}] ERROR: ${msg}`;
    console.error(line);
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
const PROJECT_DIR = Deno.cwd();
const INPUT_FILE = `${PROJECT_DIR}/translate-input.json`;
const OUTPUT_FILE = `${PROJECT_DIR}/translate-output.json`;
const PROFICIENCY_OUTPUT_FILE = `${PROJECT_DIR}/proficiency-output.json`;

const START_TIME = Date.now();
const MAX_RETRIES = 3;
const SESSION_TIMEOUT_MS = 21 * 60 * 1000; // 12 minutes inactivity timeout
const MAX_STALL_RETRIES = 3;
const DEFAULT_MIN_PROFICIENCY = 2;
const DEFAULT_MAX_DURATION_MIN = 10;

function isTimeUp(maxDurationMs: number): boolean {
    return Date.now() - START_TIME >= maxDurationMs;
}

async function sendMessageWithTimeout(
    opencode: OpencodeClient,
    sessionId: string,
    text: string,
    opts: SendOptions,
): Promise<void> {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), SESSION_TIMEOUT_MS);
    try {
        await opencode.sendMessage(sessionId, text, opts, controller.signal);
    } finally {
        clearTimeout(timer);
    }
}

// ── CLI args ─────────────────────────────────────────────────────────────────
interface CliArgs {
    all: boolean;
    minProficiency: number;
    maxDuration: number;
    model?: string;
    locale?: string;
}

function parseArgs(): CliArgs {
    const args = Deno.args
        .flatMap((a) =>
            a.startsWith("--") && a.includes("=")
                ? a.split("=", 2)
                : [a]
        );
    let all = false;
    let minProficiency = DEFAULT_MIN_PROFICIENCY;
    let maxDuration = DEFAULT_MAX_DURATION_MIN;
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
            case "--max-duration":
                maxDuration = parseInt(args[++i], 10);
                break;
            case "--help":
                log(
                    `Usage: deno task translate -- [OPTIONS]

Options:
  --model <name> --locale <bcp47>    Single (model, locale) pair
  --model <name> --all                Single model over all locales
  --all                               Full matrix over all models x locales
  --min-proficiency <N>               Skip locales below this proficiency (default: ${DEFAULT_MIN_PROFICIENCY})
  --max-duration <minutes>            Max runtime before graceful stop (default: ${DEFAULT_MAX_DURATION_MIN})
  --help                              Show this help`,
                );
                Deno.exit(0);
        }
    }

    if (all && !model) {
        return {
            all: true,
            model: undefined,
            locale: undefined,
            minProficiency,
            maxDuration,
        };
    }
    if (model && all) {
        return { all: true, model, locale: undefined, minProficiency, maxDuration };
    }
    if (model && locale) {
        return { all: false, model, locale, minProficiency, maxDuration };
    }
    logError("Specify --model <name> --locale <bcp47>, --model <name> --all, or --all");
    Deno.exit(1);
}

// ── File helpers ─────────────────────────────────────────────────────────────
async function writeInput(data: unknown): Promise<void> {
    await Deno.writeTextFile(INPUT_FILE, JSON.stringify(data, null, 2));
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

    let sessionId = await opencode.createSession(PROJECT_DIR);

    for (const modelRef of chain) {
        let lastError: string | undefined;

        for (let retry = 0; retry < MAX_RETRIES; retry++) {
            const opts: SendOptions = {
                system: retry === 0 ? SKILL_PROFICIENCY : undefined,
                model: retry === 0 ? modelRef : undefined,
            };

            const text = retry === 0
                ? `Write the proficiency assessment to ${PROFICIENCY_OUTPUT_FILE}. Read the input data from ${INPUT_FILE}.`
                : `Verification failed: ${lastError}. Please fix ${PROFICIENCY_OUTPUT_FILE}.`;

            const context = `${langEnglishName} to ${modelRef.providerID}(rank ${getProviderRank(modelRef.providerID)})/${modelRef.modelID}`;
            let stallTry = 0;
            while (true) {
                stallTry++;
                try {
                    await sendMessageWithTimeout(opencode, sessionId, text, opts);
                    break;
                } catch (e) {
                    if (e instanceof DOMException && e.name === "AbortError") {
                        logError(`proficiency session timeout after ${SESSION_TIMEOUT_MS / 60000} min for ${context}, closing session (stall ${stallTry}/${MAX_STALL_RETRIES})`);
                        await opencode.closeSession(sessionId);
                        if (stallTry < MAX_STALL_RETRIES) {
                            sessionId = await opencode.createSession(PROJECT_DIR);
                            continue;
                        }
                        throw new Error(`Proficiency session timed out for ${context} after ${MAX_STALL_RETRIES} retries`);
                    }
                    throw e;
                }
            }

            try {
                const result = await verifyProficiencyFile(
                    PROFICIENCY_OUTPUT_FILE,
                    langBcp47,
                );
                const language = await getOrCreateLanguage(langBcp47);
                const modelDb = await getOrCreateModel(modelName);
                await upsertProficiency(
                    language.id,
                    modelDb.id,
                    result.proficiency,
                );

                const rank = getProviderRank(modelRef.providerID);
                log(
                    `proficiency ${modelName} ${langBcp47} ${modelRef.providerID} rank=${rank} → ${result.proficiency}`,
                );
                return result.proficiency;
            } catch (e) {
                lastError = e instanceof VerifyError ? e.message : String(e);
                logError(
                    `${modelRef.providerID}/${modelRef.modelID} retry ${retry + 1}/${MAX_RETRIES}: ${lastError}`,
                );
            }
        }
    }

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
    proficiency: number,
    unsettledCount: number,
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

    let sessionId = await opencode.createSession(PROJECT_DIR);

    for (const modelRef of chain) {
        let lastError: string | undefined;

        for (let retry = 0; retry < MAX_RETRIES; retry++) {
            const opts: SendOptions = {
                system: retry === 0 ? SKILL_TRANSLATE : undefined,
                model: retry === 0 ? modelRef : undefined,
            };

            const text = retry === 0
                ? `Write the translations to ${OUTPUT_FILE}. Read the input data from ${INPUT_FILE}.`
                : `Verification failed: ${lastError}. Please fix ${OUTPUT_FILE}.`;

            const rank = getProviderRank(modelRef.providerID);
            log(
                `submitting translation request for ${langEnglishName} to ${modelRef.providerID}(rank ${rank})/${modelRef.modelID}, proficiency: ${proficiency}. searching for ${unsettledCount} unsettled strings`,
            );

            const context = `${langEnglishName} to ${modelRef.providerID}(rank ${rank})/${modelRef.modelID}`;
            let stallTry = 0;
            while (true) {
                stallTry++;
                try {
                    await sendMessageWithTimeout(opencode, sessionId, text, opts);
                    break;
                } catch (e) {
                    if (e instanceof DOMException && e.name === "AbortError") {
                        logError(`translation session timeout after ${SESSION_TIMEOUT_MS / 60000} min for ${context}, closing session (stall ${stallTry}/${MAX_STALL_RETRIES})`);
                        await opencode.closeSession(sessionId);
                        if (stallTry < MAX_STALL_RETRIES) {
                            sessionId = await opencode.createSession(PROJECT_DIR);
                            continue;
                        }
                        throw new Error(`Translation session timed out for ${context} after ${MAX_STALL_RETRIES} retries`);
                    }
                    throw e;
                }
            }

            try {
                const result = await verifyTranslationFile(
                    OUTPUT_FILE,
                    langBcp47,
                    strings,
                );
                const language = await getOrCreateLanguage(langBcp47);
                const modelDb = await getOrCreateModel(modelName);

                const allMs = await getAllMasterStrings();
                let stored = 0;
                let skipped = 0;
                for (const t of result.translations) {
                    const ms = allMs.find((s) => s.text === t.key);
                    if (!ms) {
                        skipped++;
                        continue;
                    }
                    if (t.translation === null) {
                        await upsertVote(language.id, modelDb.id, ms.id, "");
                        skipped++;
                        continue;
                    }
                    const items = Array.isArray(t.translation)
                        ? t.translation
                        : [t.translation];
                    for (const item of items) {
                        await upsertVote(
                            language.id,
                            modelDb.id,
                            ms.id,
                            item,
                        );
                        stored++;
                    }
                }

                log(
                    `got translation result for ${langEnglishName} to ${modelRef.providerID}(rank ${rank})/${modelRef.modelID}: stored ${stored}, skipped ${skipped}`,
                );
                await (await getPrisma()).$queryRawUnsafe("PRAGMA wal_checkpoint(TRUNCATE)");
                return;
            } catch (e) {
                lastError = e instanceof VerifyError ? e.message : String(e);
                logError(
                    `${modelRef.providerID}/${modelRef.modelID} retry ${
                        retry + 1
                    }/${MAX_RETRIES}: ${lastError}`,
                );
            }
        }
    }

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
            await dispatchProficiency(
                opencode,
                modelName,
                langBcp47,
                langEnglishName,
                availableModels,
            );
        } catch (e) {
            logError(
                `${modelName} ${langBcp47}: proficiency failed, skipping translate: ${
                    e instanceof Error ? e.message : String(e)
                }`,
            );
            return;
        }
    }

    const proficiency = await getProficiencyLevel(modelDb.id, language.id) ?? 0;

    if (proficiency < minProficiency) {
        log(`${modelName} ${langBcp47}: proficiency ${proficiency} below threshold ${minProficiency}, skipping`);
        return;
    }

    // Step 2: Translate (only missing strings)
    const allMs = await getAllMasterStrings();
    const existing = await getExistingVotes(modelDb.id, language.id);
    const nullVotes = await getNullExistingVotes(modelDb.id, language.id);
    const settled = await getSettledStrings(language.id);
    const skip = new Set([...existing, ...nullVotes, ...settled]);
    const missing = allMs.filter((s) => !skip.has(s.id));

    if (missing.length === 0) {
        log(`${modelName} ${langBcp47}: all ${allMs.length} strings settled or existing, skipping`);
        return;
    }

    if (settled.size > 0) {
        log(`${modelName} ${langBcp47}: ${allMs.length} total, ${skip.size} skip (${existing.size} existing, ${nullVotes.size} null, ${settled.size} settled), ${missing.length} remaining`);
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
            proficiency,
            missing.length,
        );
    } catch (e) {
        logError(
            `${modelName} ${langBcp47}: ${
                e instanceof Error ? e.message : String(e)
            }`,
        );
    }
}

// ── Full matrix run ──────────────────────────────────────────────────────────
async function runAll(
    opencode: OpencodeClient,
    models: { id: number; name: string }[],
    languages: { id: number; bcp_47: string; english_name: string }[],
    availableModels: Map<string, ModelEntry[]>,
    minProficiency: number,
    maxDurationMs: number,
): Promise<void> {
    for (const m of models) {
        for (const lang of languages) {
            if (isTimeUp(maxDurationMs)) {
                log(`Stopping: configured runtime reached at model=${m.name}, locale=${lang.bcp_47}`);
                Deno.exit(0);
            }
            await runOne(
                opencode,
                m.name,
                lang.bcp_47,
                lang.english_name,
                availableModels,
                minProficiency,
            );
        }
    }
}

// ── Single model over all locales ────────────────────────────────────────────
async function runOneModelAllLocales(
    opencode: OpencodeClient,
    modelName: string,
    availableModels: Map<string, ModelEntry[]>,
    minProficiency: number,
    maxDurationMs: number,
): Promise<void> {
    const languages = await getAllLanguages();
    log(`Model '${modelName}' over ${languages.length} locales`);

    for (const lang of languages) {
        if (isTimeUp(maxDurationMs)) {
            log(`Stopping: configured runtime reached at model=${modelName}, locale=${lang.bcp_47}`);
            Deno.exit(0);
        }
        await runOne(
            opencode,
            modelName,
            lang.bcp_47,
            lang.english_name,
            availableModels,
            minProficiency,
        );
    }
}

// ── Main ─────────────────────────────────────────────────────────────────────
async function main() {
    const args = parseArgs();
    const opencode = new OpencodeClient();

    log("Fetching available models from opencode...");
    const availableModels = await fetchAvailableModels();
    log(`Found ${availableModels.size} unique models across providers`);

    if (args.all && !args.model) {
        const models = await getAllModels();
        const languages = await getAllLanguages();
        log(`Full run: ${models.length} models x ${languages.length} locales`);

        await runAll(
            opencode,
            models,
            languages,
            availableModels,
            args.minProficiency,
            args.maxDuration * 60_000,
        );

        log("Full run complete");
    } else if (args.model && args.all) {
        await runOneModelAllLocales(
            opencode,
            args.model,
            availableModels,
            args.minProficiency,
            args.maxDuration * 60_000,
        );

        log(`Model '${args.model}' complete`);
    } else if (args.model && args.locale) {
        const language = await getOrCreateLanguage(args.locale);
        await runOne(
            opencode,
            args.model,
            args.locale,
            language.english_name,
            availableModels,
            args.minProficiency,
        );
    }
}

if (import.meta.main) {
    await main();
}
