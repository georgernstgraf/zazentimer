const BASE_URL = Deno.env.get("OPENCODE_SERVER_URL") || "http://localhost:4096";
const USERNAME = Deno.env.get("OPENCODE_SERVER_USERNAME") || "georg";
const PASSWORD = Deno.env.get("OPENCODE_SERVER_PASSWORD") || "home5home";

function authHeaders(): Record<string, string> {
  const encoded = btoa(`${USERNAME}:${PASSWORD}`);
  return {
    "Content-Type": "application/json",
    Authorization: `Basic ${encoded}`,
  };
}

export interface ModelRef {
  providerID: string;
  modelID: string;
}

export interface SendOptions {
  model?: ModelRef;
  system?: string;
}

export class OpencodeClient {
  async createSession(directory?: string): Promise<string> {
    const url = directory
      ? `${BASE_URL}/session?directory=${encodeURIComponent(directory)}`
      : `${BASE_URL}/session`;
    const res = await fetch(url, {
      method: "POST",
      headers: authHeaders(),
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
      headers: authHeaders(),
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      throw new Error(
        `sendMessage failed: ${res.status} ${await res.text()}`,
      );
    }
    const data = await res.json();
    const parts = data.parts as Array<{ type: string; text?: string }> | undefined;
    if (parts) {
      const textPart = parts.find((p) => p.type === "text" || p.type === "tool-use");
      if (textPart?.text) return textPart.text;
    }
    return data.message || data.text || JSON.stringify(data);
  }

  async closeSession(sessionId: string): Promise<void> {
    try {
      await fetch(`${BASE_URL}/session/${sessionId}`, {
        method: "DELETE",
        headers: authHeaders(),
      });
    } catch {
      // Ignore close errors — session may already be gone
    }
  }
}
