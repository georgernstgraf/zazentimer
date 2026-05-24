const BASE_URL = Deno.env.get("OPENCODE_SERVER_URL") || "http://localhost:3001";

export interface ModelRef {
  providerID: string;
  modelID: string;
}

export interface SendOptions {
  model?: ModelRef;
  system?: string;
}

export class OpencodeClient {
  async createSession(): Promise<string> {
    const res = await fetch(`${BASE_URL}/session`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: "{}",
    });
    if (!res.ok) {
      throw new Error(`createSession failed: ${res.status} ${await res.text()}`);
    }
    const data = await res.json();
    const sessionId: string | undefined =
      data.sessionId || data.sessionID || data.id;
    if (!sessionId) {
      throw new Error(
        `createSession: no sessionId in response: ${JSON.stringify(data)}`,
      );
    }
    return sessionId;
  }

  async sendMessage(
    sessionId: string,
    text: string,
    opts?: SendOptions,
  ): Promise<string> {
    const body: Record<string, unknown> = {
      parts: [{ type: "text", text }],
    };

    if (opts?.system) {
      body.system = opts.system;
    }
    if (opts?.model) {
      body.model = {
        providerID: opts.model.providerID,
        modelID: opts.model.modelID,
      };
    }

    const res = await fetch(`${BASE_URL}/session/${sessionId}/message`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      throw new Error(
        `sendMessage failed: ${res.status} ${await res.text()}`,
      );
    }
    const data = await res.json();
    const reply =
      data.parts?.[0]?.text || data.message || data.text || JSON.stringify(data);
    return reply;
  }

  async closeSession(sessionId: string): Promise<void> {
    try {
      await fetch(`${BASE_URL}/session/${sessionId}`, { method: "DELETE" });
    } catch {
      // Ignore close errors — session may already be gone
    }
  }
}
