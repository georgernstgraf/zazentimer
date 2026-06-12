import { getPrisma } from "../lib/prisma.ts";
import {
    checkLanguageConsistency,
    checkMasterStringConsistency,
    checkModelConsistency,
    parseMasterStringsXml,
} from "../lib/db.ts";
import masterModels from "./llmmodels_master.json" with { type: "json" };
import supportedLanguages from "./supported_languages.json" with { type: "json" };

const force = Deno.args.includes("--force");
const prisma = await getPrisma();

try {
    // ── 1. Seed languages from supported_languages.json ──────────────
    const langsBefore = await prisma.languages.count();
    for (const lang of supportedLanguages) {
        await prisma.languages.upsert({
            where: { bcp_47: lang.bcp_47 },
            update: {
                posix_code: lang.posix_code,
                iso_639_3: lang.iso_639_3,
                directory: lang.directory,
                english_name: lang.english_name,
                whisper_response: lang.whisper_response,
            },
            create: {
                bcp_47: lang.bcp_47,
                posix_code: lang.posix_code,
                iso_639_3: lang.iso_639_3,
                directory: lang.directory,
                english_name: lang.english_name,
                whisper_response: lang.whisper_response,
            },
        });
    }
    const langsNew = await prisma.languages.count() - langsBefore;
    console.log(`✔ Seeded ${supportedLanguages.length} languages (${langsNew} new, ${supportedLanguages.length - langsNew} existing)`);

    // ── 2. Seed LLM models ────────────────────────────────────────────
    const modelsBefore = await prisma.llm_models.count();
    for (const m of masterModels) {
        await prisma.llm_models.upsert({
            where: { name: m.name },
            update: {},
            create: { name: m.name },
        });
    }
    const modelsNew = await prisma.llm_models.count() - modelsBefore;
    console.log(`✔ Seeded ${masterModels.length} LLM models (${modelsNew} new, ${masterModels.length - modelsNew} existing)`);

    // ── 3. Seed master strings from English strings.xml ──────────────
    const xml = await Deno.readTextFile(
        new URL("../../app/src/main/res/values/strings.xml", import.meta.url),
    );
    const currentTexts = parseMasterStringsXml(xml);
    const stringsBefore = await prisma.master_strings.count();
    for (const text of currentTexts) {
        await prisma.master_strings.upsert({
            where: { text },
            update: {},
            create: { text },
        });
    }
    const stringsNew = await prisma.master_strings.count() - stringsBefore;
    console.log(`✔ Seeded ${currentTexts.length} master strings (${stringsNew} new, ${currentTexts.length - stringsNew} existing)`);

    // ── 4. Check for obsolete data ───────────────────────────────────
    const { extraInDb: obsoleteLangs } = await checkLanguageConsistency(
        supportedLanguages.map((l) => l.bcp_47),
    );
    const { extraInDb: obsoleteModels } = await checkModelConsistency(
        masterModels.map((m) => m.name),
    );
    const { extraInDb: obsoleteStrings } = await checkMasterStringConsistency(currentTexts);

    const hasObsolete = obsoleteLangs.length + obsoleteModels.length + obsoleteStrings.length > 0;

    if (hasObsolete && !force) {
        console.log("\nℹ Obsolete data in DB (not in source files):\n");

        if (obsoleteLangs.length > 0) {
            const langVotes = await prisma.votes.count({
                where: { languagesId: { in: obsoleteLangs.map((l) => l.id) } },
            });
            console.log(`  Languages (${obsoleteLangs.length}) — ${langVotes} votes exist:`);
            for (const l of obsoleteLangs) {
                console.log(`    - ${l.bcp_47} (${l.english_name})`);
            }
            console.log("");
        }

        if (obsoleteModels.length > 0) {
            const modelVotes = await prisma.votes.count({
                where: { llm_modelsId: { in: obsoleteModels.map((m) => m.id) } },
            });
            const modelProfs = await prisma.language_proficiencies.count({
                where: { modelId: { in: obsoleteModels.map((m) => m.id) } },
            });
            console.log(`  Models (${obsoleteModels.length}) — ${modelVotes} votes, ${modelProfs} proficiencies exist:`);
            for (const m of obsoleteModels) {
                console.log(`    - ${m.name}`);
            }
            console.log("");
        }

        if (obsoleteStrings.length > 0) {
            const stringVotes = await prisma.votes.count({
                where: { master_stringsId: { in: obsoleteStrings.map((s) => s.id) } },
            });
            console.log(`  Master strings (${obsoleteStrings.length}) — ${stringVotes} votes exist:`);
            for (const s of obsoleteStrings) {
                const display = s.text.length > 60 ? s.text.slice(0, 60) + "..." : s.text;
                console.log(`    - "${display}"`);
            }
            console.log("");
        }

        console.log("Run with --force to remove obsolete data.");
    }

    if (hasObsolete && force) {
        if (obsoleteLangs.length > 0) {
            const langRemoved = await prisma.languages.deleteMany({
                where: { id: { in: obsoleteLangs.map((l) => l.id) } },
            });
            console.log(`✔ Removed ${langRemoved.count} obsolete languages`);
        }
        if (obsoleteModels.length > 0) {
            const removedModels = await prisma.llm_models.deleteMany({
                where: { id: { in: obsoleteModels.map((m) => m.id) } },
            });
            console.log(`✔ Removed ${removedModels.count} obsolete LLM models`);
        }
        if (obsoleteStrings.length > 0) {
            const removedStrings = await prisma.master_strings.deleteMany({
                where: { id: { in: obsoleteStrings.map((s) => s.id) } },
            });
            console.log(`✔ Removed ${removedStrings.count} obsolete master strings (cascaded to votes)`);
        }
    }
} finally {
    await prisma.$disconnect();
}
