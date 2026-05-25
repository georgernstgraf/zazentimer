import { getPrisma } from "./prisma.ts";

type PMasterString = {
    id: number;
    text: string;
};

type PLanguage = {
    id: number;
    bcp_47: string;
    english_name: string;
};

type PModel = {
    id: number;
    name: string;
};

const NOT_EMPTY = { translation: { not: "" as const } };

// ── Models ──────────────────────────────────────────────────────────────────

export async function getAllModels(): Promise<PModel[]> {
    const prisma = await getPrisma();
    return await prisma.llm_models.findMany({ orderBy: { name: "asc" } });
}

export async function getModels() {
    return await getAllModels();
}

export async function getOrCreateModel(name: string): Promise<PModel> {
    const prisma = await getPrisma();
    let model = await prisma.llm_models.findUnique({ where: { name } });
    if (!model) {
        model = await prisma.llm_models.create({ data: { name } });
    }
    return model;
}

// ── Proficiencies ───────────────────────────────────────────────────────────

export async function getProficiencies(modelId: number) {
    const prisma = await getPrisma();
    return await prisma.language_proficiencies.findMany({
        where: { llm_models: { some: { id: modelId } } },
        include: { languages: true },
        orderBy: { level: "desc" },
    });
}

export async function upsertProficiency(
    languageId: number,
    modelId: number,
    level: number,
) {
    const prisma = await getPrisma();
    return await prisma.language_proficiencies.create({
        data: {
            level,
            llm_models: { connect: { id: modelId } },
            languages: { connect: { id: languageId } },
        },
    });
}

export async function hasProficiency(
    modelId: number,
    languageId: number,
): Promise<boolean> {
    const prisma = await getPrisma();
    const rows = await prisma.language_proficiencies.findMany({
        where: {
            llm_models: { some: { id: modelId } },
            languages: { some: { id: languageId } },
        },
        take: 1,
        select: { id: true },
    });
    return rows.length > 0;
}

// ── Languages ───────────────────────────────────────────────────────────────

export async function getAllLanguages(): Promise<PLanguage[]> {
    const prisma = await getPrisma();
    return await prisma.languages.findMany({ orderBy: { bcp_47: "asc" } });
}

export async function getLanguages() {
    return await getAllLanguages();
}

export async function getOrCreateLanguage(bcp47: string): Promise<PLanguage> {
    const prisma = await getPrisma();
    const lang = await prisma.languages.findUnique({ where: { bcp_47: bcp47 } });
    if (!lang) {
        throw new Error(`Language '${bcp47}' not found in DB. Run seed first.`);
    }
    return lang;
}

export async function getLanguagesWithStats(search: string) {
    const prisma = await getPrisma();
    const where = search
        ? {
            OR: [
                { english_name: { contains: search } },
                { bcp_47: { contains: search } },
            ],
        }
        : {};
    const languages = await prisma.languages.findMany({
        where,
        orderBy: { bcp_47: "asc" },
    });

    return await Promise.all(languages.map(async (lang) => {
        const [modelCount, voteCount] = await Promise.all([
            prisma.language_proficiencies.count({
                where: { languages: { some: { id: lang.id } } },
            }),
            prisma.votes.count({
                where: { languagesId: lang.id, ...NOT_EMPTY },
            }),
        ]);
        return { ...lang, modelCount, voteCount };
    }));
}

// ── Master Strings ──────────────────────────────────────────────────────────

export async function getAllMasterStrings(): Promise<PMasterString[]> {
    const prisma = await getPrisma();
    return await prisma.master_strings.findMany({ orderBy: { id: "asc" } });
}

export async function getOrCreateMasterString(text: string): Promise<PMasterString> {
    const prisma = await getPrisma();
    let ms = await prisma.master_strings.findUnique({ where: { text } });
    if (!ms) {
        ms = await prisma.master_strings.create({ data: { text } });
    }
    return ms;
}

export async function getStrings(search: string) {
    const prisma = await getPrisma();
    const where = search ? { text: { contains: search } } : {};
    const strings = await prisma.master_strings.findMany({
        where,
        orderBy: { text: "asc" },
    });
    const result = await Promise.all(strings.map(async (s) => {
        const count = await prisma.votes.count({
            where: { master_stringsId: s.id, ...NOT_EMPTY },
        });
        return { ...s, voteCount: count };
    }));
    return result;
}

// ── Votes / Translations ────────────────────────────────────────────────────

export async function getExistingVotes(
    modelId: number,
    languageId: number,
): Promise<Set<number>> {
    const prisma = await getPrisma();
    const rows = await prisma.votes.findMany({
        where: { llm_modelsId: modelId, languagesId: languageId, ...NOT_EMPTY },
        select: { master_stringsId: true },
        distinct: ["master_stringsId"],
    });
    return new Set(rows.map((r) => r.master_stringsId));
}

export async function upsertVote(
    languageId: number,
    modelId: number,
    masterStringId: number,
    translation: string,
) {
    const prisma = await getPrisma();
    return await prisma.votes.upsert({
        where: {
            languagesId_llm_modelsId_master_stringsId_translation: {
                languagesId: languageId,
                llm_modelsId: modelId,
                master_stringsId: masterStringId,
                translation,
            },
        },
        update: {},
        create: {
            languagesId: languageId,
            llm_modelsId: modelId,
            master_stringsId: masterStringId,
            translation,
        },
    });
}

export async function getVotesGrouped(modelId: number, langId: number) {
    const prisma = await getPrisma();
    const votes = await prisma.votes.findMany({
        where: { languagesId: langId, llm_modelsId: modelId, ...NOT_EMPTY },
        include: { master_string: true },
        orderBy: [{ master_stringsId: "asc" }, { created_at: "desc" }],
    });

    const grouped: Record<number, { master_string: string; translations: string[] }> = {};
    for (const v of votes) {
        if (!grouped[v.master_stringsId]) {
            grouped[v.master_stringsId] = {
                master_string: v.master_string.text,
                translations: [],
            };
        }
        if (!grouped[v.master_stringsId].translations.includes(v.translation)) {
            grouped[v.master_stringsId].translations.push(v.translation);
        }
    }
    return Object.values(grouped);
}

export async function getCoverage(modelId: number, langId: number) {
    const prisma = await getPrisma();
    const [translated, total] = await Promise.all([
        prisma.votes.findMany({
            where: { languagesId: langId, llm_modelsId: modelId, ...NOT_EMPTY },
            select: { master_stringsId: true },
            distinct: ["master_stringsId"],
        }),
        prisma.master_strings.count(),
    ]);
    return {
        translated: translated.length,
        total,
        coverage_pct: total > 0
            ? Math.round((translated.length / total) * 100 * 10) / 10
            : 0,
    };
}

export async function getComparison(stringId: number, langId: number) {
    const prisma = await getPrisma();
    const masterString = await prisma.master_strings.findUnique({
        where: { id: stringId },
    });
    if (!masterString) throw new Error("Master string not found");

    const where: Record<string, unknown> = { master_stringsId: stringId, ...NOT_EMPTY };
    if (langId) where.languagesId = langId;

    const votes = await prisma.votes.findMany({
        where,
        include: { llm_model: true, language: true },
        orderBy: [{ llm_modelsId: "asc" }, { created_at: "desc" }],
    });

    const byModel: Record<number, { model: string; translations: string[]; modelId: number }> = {};
    for (const v of votes) {
        if (!byModel[v.llm_modelsId]) {
            byModel[v.llm_modelsId] = {
                model: v.llm_model.name,
                modelId: v.llm_modelsId,
                translations: [],
            };
        }
        if (!byModel[v.llm_modelsId].translations.includes(v.translation)) {
            byModel[v.llm_modelsId].translations.push(v.translation);
        }
    }
    return { master_string: masterString.text, comparisons: Object.values(byModel) };
}

// ── Evaluation ───────────────────────────────────────────────────────────────

export async function getEvaluation(langId: number) {
    const prisma = await getPrisma();

    const votes = await prisma.votes.findMany({
        where: { languagesId: langId, ...NOT_EMPTY },
        include: { master_string: true, llm_model: true },
    });

    const profs = await prisma.language_proficiencies.findMany({
        where: { languages: { some: { id: langId } }, level: { gte: 2 } },
        include: { llm_models: true },
    });

    const modelLevels = new Map<number, number>();
    for (const p of profs) {
        for (const m of p.llm_models) modelLevels.set(m.id, p.level);
    }

    const groups = new Map<number, Map<string, {
        master_string: string; translation: string;
        score: number; modelNames: string[];
    }>>();

    for (const v of votes) {
        const level = modelLevels.get(v.llm_modelsId);
        if (!level) continue;
        const msId = v.master_stringsId;
        if (!groups.has(msId)) groups.set(msId, new Map());
        const tmap = groups.get(msId)!;
        if (!tmap.has(v.translation)) {
            tmap.set(v.translation, {
                master_string: v.master_string.text,
                translation: v.translation,
                score: 0,
                modelNames: [],
            });
        }
        const entry = tmap.get(v.translation)!;
        entry.score += level;
        entry.modelNames.push(v.llm_model.name);
    }

    const result = [];
    for (const [, tmap] of groups) {
        const ranked = [...tmap.values()].sort((a, b) => b.score - a.score);
        result.push(...ranked);
    }
    return result.map((r) => ({
        ...r,
        modelCount: r.modelNames.length,
        modelNames: r.modelNames.join(", "),
    }));
}

// ── Stats ───────────────────────────────────────────────────────────────────

export async function getStats() {
    const prisma = await getPrisma();
    const [models, languages, votes, strings] = await Promise.all([
        prisma.llm_models.count(),
        prisma.languages.count(),
        prisma.votes.count({ where: NOT_EMPTY }),
        prisma.master_strings.count(),
    ]);
    return { models, languages, votes, strings };
}
