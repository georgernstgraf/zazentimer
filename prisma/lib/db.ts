import { PrismaClient } from "prismaclient";

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

let lastClient: PrismaClient | null = null;
let queue = Promise.resolve();

async function getClient(): Promise<PrismaClient> {
  return new Promise<PrismaClient>((resolve, reject) => {
    queue = queue.then(async () => {
      try {
        if (!lastClient) {
          const { PrismaClient: PC } = await import("prismaclient");
          lastClient = new PC();
          await lastClient.$connect();
        }
        resolve(lastClient);
      } catch (e) {
        reject(e);
      }
    }).catch(() => {});
  });
}

export async function getOrCreateLanguage(bcp47: string): Promise<PLanguage> {
  const prisma = await getClient();
  let lang = await prisma.languages.findUnique({ where: { bcp_47: bcp47 } });
  if (!lang) {
    throw new Error(`Language '${bcp47}' not found in DB. Run seed first.`);
  }
  return lang;
}

export async function getOrCreateModel(name: string): Promise<PModel> {
  const prisma = await getClient();
  let model = await prisma.llm_models.findUnique({ where: { name } });
  if (!model) {
    model = await prisma.llm_models.create({ data: { name } });
  }
  return model;
}

export async function getOrCreateMasterString(text: string): Promise<PMasterString> {
  const prisma = await getClient();
  let ms = await prisma.master_strings.findUnique({ where: { text } });
  if (!ms) {
    ms = await prisma.master_strings.create({ data: { text } });
  }
  return ms;
}

export async function getExistingVotes(
  modelId: number,
  languageId: number,
): Promise<Set<number>> {
  const prisma = await getClient();
  const rows = await prisma.votes.findMany({
    where: { llm_modelsId: modelId, languagesId: languageId },
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
  const prisma = await getClient();
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

export async function upsertProficiency(
  languageId: number,
  modelId: number,
  level: number,
) {
  const prisma = await getClient();
  return await prisma.language_proficiencies.create({
    data: {
      level,
      llm_models: { connect: { id: modelId } },
      languages: { connect: { id: languageId } },
    },
  });
}

export async function getAllModels(): Promise<PModel[]> {
  const prisma = await getClient();
  return await prisma.llm_models.findMany({ orderBy: { name: "asc" } });
}

export async function getAllLanguages(): Promise<PLanguage[]> {
  const prisma = await getClient();
  return await prisma.languages.findMany({ orderBy: { bcp_47: "asc" } });
}

export async function getAllMasterStrings(): Promise<PMasterString[]> {
  const prisma = await getClient();
  return await prisma.master_strings.findMany({ orderBy: { id: "asc" } });
}

export async function hasProficiency(
  modelId: number,
  languageId: number,
): Promise<boolean> {
  const prisma = await getClient();
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
