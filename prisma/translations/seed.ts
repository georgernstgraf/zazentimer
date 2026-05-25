import { getPrisma } from "../lib/prisma.ts";

const prisma = await getPrisma();

try {
    // ── 1. Seed languages ──────────────────────────────────────────────
    const languages: Array<{
        bcp_47: string;
        posix_code: string;
        iso_639_3: string;
        directory: string;
        english_name: string;
        whisper_response: string | null;
    }> = JSON.parse(
        await Deno.readTextFile(
            new URL("languages_seed.json", import.meta.url),
        ),
    );

    for (const lang of languages) {
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
    console.log(`✔ Seeded ${languages.length} languages`);

    // ── 2. Seed LLM models ────────────────────────────────────────────
    const models: Array<{ name: string }> = JSON.parse(
        await Deno.readTextFile(
            new URL("llmmodels_seed.json", import.meta.url),
        ),
    );

    for (const m of models) {
        await prisma.llm_models.upsert({
            where: { name: m.name },
            update: {},
            create: { name: m.name },
        });
    }
    console.log(`✔ Seeded ${models.length} LLM models`);

    // ── 3. Seed master strings from English strings.xml ──────────────
    const xml = await Deno.readTextFile(
        new URL("../../app/src/main/res/values/strings.xml", import.meta.url),
    );

    let count = 0;
    let skipped = 0;
    const stringRegex = /<string\s+name="([^"]*)"([^>]*)>([\s\S]*?)<\/string>/g;
    let match: RegExpExecArray | null;
    while ((match = stringRegex.exec(xml)) !== null) {
        const attrs = match[2];
        const text = match[3].trim();
        if (!text) continue;
        if (/translatable\s*=\s*"false"/.test(attrs)) {
            skipped++;
            continue;
        }
        // Decode common XML entities
        const decoded = text
            .replace(/&lt;/g, "<")
            .replace(/&gt;/g, ">")
            .replace(/&amp;/g, "&")
            .replace(/&quot;/g, '"')
            .replace(/&#39;/g, "'");
        await prisma.master_strings.upsert({
            where: { text: decoded },
            update: {},
            create: { text: decoded },
        });
        count++;
    }
    console.log(
        `✔ Seeded ${count} master strings (skipped ${skipped} non-translatable)`,
    );
} finally {
    await prisma.$disconnect();
}
