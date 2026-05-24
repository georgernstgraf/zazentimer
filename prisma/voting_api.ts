import { Hono } from "hono";
import { HTTPException } from "hono/http-exception";

const app = new Hono();

type PrismaClient = import("prismaclient").PrismaClient;

let _queue = Promise.resolve();

async function withPrisma<T>(fn: (client: PrismaClient) => Promise<T>): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    _queue = _queue.then(async () => {
      try {
        const { PrismaClient } = await import("prismaclient");
        const client = new PrismaClient();
        await client.$connect();
        try {
          resolve(await fn(client));
        } finally {
          await client.$disconnect();
        }
      } catch (e) {
        reject(e);
      }
    }).catch(() => {});
  });
}

app.post("/api/votes", async (c) => {
  const body = await c.req.json<{
    bcp_47?: string;
    model?: string;
    master_text?: string;
    translation?: string;
    confidence?: number;
  }>();

  const { bcp_47, model, master_text, translation, confidence } = body;

  if (!bcp_47 || !model || !master_text || !translation || confidence == null) {
    throw new HTTPException(400, {
      message: "bcp_47, model, master_text, translation, and confidence are required",
    });
  }

  if (
    typeof confidence !== "number" || confidence < 1 || confidence > 5 ||
    !Number.isInteger(confidence)
  ) {
    throw new HTTPException(400, {
      message: "confidence must be an integer between 1 and 5",
    });
  }

  const vote = await withPrisma(async (prisma) => {
    const language = await prisma.languages.findUnique({ where: { bcp_47 } });
    if (!language) {
      throw new HTTPException(404, { message: `Language '${bcp_47}' not found` });
    }

    const llmModel = await prisma.llm_models.findUnique({ where: { name: model } });
    if (!llmModel) {
      throw new HTTPException(404, { message: `Model '${model}' not found` });
    }

    const masterString = await prisma.master_strings.findUnique({
      where: { text: master_text },
    });
    if (!masterString) {
      throw new HTTPException(404, {
        message: `Master string '${master_text}' not found`,
      });
    }

    return await prisma.votes.upsert({
      where: {
        languagesId_llm_modelsId_master_stringsId_translation: {
          languagesId: language.id,
          llm_modelsId: llmModel.id,
          master_stringsId: masterString.id,
          translation,
        },
      },
      update: { confidence },
      create: {
        languagesId: language.id,
        llm_modelsId: llmModel.id,
        master_stringsId: masterString.id,
        translation,
        confidence,
      },
    });
  });

  return c.json(vote, 201);
});

app.onError((err, c) => {
  if (err instanceof HTTPException) {
    return err.getResponse();
  }

  if (err instanceof SyntaxError) {
    return c.json({ error: "Invalid JSON body" }, 400);
  }

  if (typeof err === "object" && err !== null && "code" in err) {
    const prismaErr = err as { code: string; message: string };
    if (prismaErr.code === "P2002") {
      return c.json({ error: prismaErr.message }, 409);
    }
    return c.json({ error: prismaErr.message }, 400);
  }

  console.error("Unhandled error:", err);
  return c.json({ error: "Internal server error" }, 500);
});

Deno.serve({ port: 8000 }, app.fetch);
