import {
    getEvaluation,
    getLanguagesWithVotes,
    getMasterStringByText,
} from "./lib/db.ts";

// ── XML Helpers ────────────────────────────────────────────────────────────

function escapeXml(text: string): string {
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/'/g, "\\'")
        .replace(/"/g, '\\"');
}

function decodeXml(text: string): string {
    return text
        .replace(/&lt;/g, "<")
        .replace(/&gt;/g, ">")
        .replace(/&amp;/g, "&")
        .replace(/&quot;/g, '"')
        .replace(/&#39;/g, "'");
}

// ── CLI Args ───────────────────────────────────────────────────────────────

function parseArgs(): { targetDir: string } {
    let targetDir = "";
    for (const arg of Deno.args) {
        if (arg.startsWith("--target=")) {
            targetDir = arg.substring("--target=".length);
        }
    }
    if (!targetDir) {
        console.error("Error: --target=<directory> is required.");
        console.error(
            "Usage: deno run -A export.ts -- --target=<directory>",
        );
        Deno.exit(1);
    }
    return { targetDir };
}

// ── Data Types ──────────────────────────────────────────────────────────────

interface SourceString {
    key: string;
    id: number; // master_stringsId
    text: string; // decoded English text
}

// ── Main ────────────────────────────────────────────────────────────────────

async function main() {
    const { targetDir } = parseArgs();

    // Safety: target directory must NOT already exist
    try {
        await Deno.stat(targetDir);
        console.error(
            `Error: Target directory '${targetDir}' already exists. Remove it first.`,
        );
        Deno.exit(1);
    } catch (err) {
        if (!(err instanceof Deno.errors.NotFound)) throw err;
    }

    // ── 1. Read English master strings.xml ────────────────────────────
    const masterPath = new URL(
        "../app/src/main/res/values/strings.xml",
        import.meta.url,
    );
    const masterXml = await Deno.readTextFile(masterPath);

    // Copy master to target
    const valuesDir = `${targetDir}/values`;
    await Deno.mkdir(valuesDir, { recursive: true });
    await Deno.writeTextFile(`${valuesDir}/strings.xml`, masterXml);
    console.log(`Copied: values/strings.xml`);

    // Parse translatable keys from master (same logic as seed.ts)
    const sourceStrings: SourceString[] = [];
    const stringRegex =
        /<string\s+name="([^"]*)"([^>]*)>([\s\S]*?)<\/string>/g;
    let match: RegExpExecArray | null;
    while ((match = stringRegex.exec(masterXml)) !== null) {
        const attrs = match[2];
        const text = match[3].trim();
        if (!text) continue;
        if (/translatable\s*=\s*"false"/.test(attrs)) continue;

        const decoded = decodeXml(text);
        const ms = await getMasterStringByText(decoded);
        if (!ms) {
            console.warn(`Warning: No DB entry for string "${match[1]}"`);
            continue;
        }
        sourceStrings.push({ key: match[1], id: ms.id, text: decoded });
    }
    console.log(
        `Parsed ${sourceStrings.length} translatable keys from master`,
    );

    // ── 2. Get all languages with votes ──────────────────────────────
    const languages = await getLanguagesWithVotes();
    console.log(`Found ${languages.length} languages with votes`);

    // ── 3. Per language: pick tiebreak winner, write XML ─────────────
    let totalWritten = 0;

    for (const lang of languages) {
        const evaluation = await getEvaluation(lang.id);

        // First entry per master_stringsId is the tiebreak winner
        // (getEvaluation sorts by modelCount desc, then score desc)
        const winners = new Map<number, string>();
        for (const row of evaluation) {
            if (!winners.has(row.master_stringsId)) {
                winners.set(row.master_stringsId, row.translation);
            }
        }

        const lines: string[] = [];
        lines.push('<?xml version="1.0" encoding="utf-8"?>');
        lines.push(
            '<resources xmlns:tools="http://schemas.android.com/tools">',
        );

        let translatedCount = 0;
        for (const src of sourceStrings) {
            const translation = winners.get(src.id);
            if (translation) {
                lines.push(
                    `    <string name="${src.key}">${escapeXml(translation)}</string>`,
                );
                translatedCount++;
            }
        }

        lines.push("</resources>");

        const langDir = `${targetDir}/${lang.directory}`;
        await Deno.mkdir(langDir, { recursive: true });
        await Deno.writeTextFile(
            `${langDir}/strings.xml`,
            lines.join("\n") + "\n",
        );

        console.log(
            `  ${lang.directory}: ${translatedCount}/${sourceStrings.length} strings`,
        );
        totalWritten += translatedCount;
    }

    console.log(
        `\nDone: ${totalWritten} strings written across ${languages.length} languages.`,
    );
    console.log(`Target: ${targetDir}/`);
}

if (import.meta.main) {
    await main();
}
