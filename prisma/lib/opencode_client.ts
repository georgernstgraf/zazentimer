const BASE_URL = Deno.env.get("OPENCODE_SERVER_URL") || "http://localhost:3001";

export class OpencodeClient {
  async createSession(systemPrompt: string): Promise<string> {
    const res = await fetch(`${BASE_URL}/session`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        system: systemPrompt,
      }),
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

  async sendMessage(sessionId: string, text: string): Promise<string> {
    const res = await fetch(`${BASE_URL}/session/${sessionId}/message`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        parts: [{ type: "text", text }],
      }),
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
