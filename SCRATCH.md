// in translate.ts
// STATT:
const PROVIDER_RANKING = [
    "zai-coding-plan",
    // "nvidia",
    "opencode-go",
    "github-copilot",
    "google",
    "opencode",
    "openrouter",
];

// NEU und BESSER:
const MODEL_PROVIDERS = [
    { "claude-opus-4.7": "openrouter" },
    { "deepseek-v4-pro": "opencode-go" },
    { "gemini-3.1-pro-preview": ["github-copilot", "google"] },
    { "gemini-3.1-pro": ["opencode"] },
    {
        "gemini-3.5-flash": [
            "opencode",
            "github-copilot",
            "google",
            "openrouter",
        ],
    },
    { "glm-5.1": ["zai-coding-plan", "opencode-go"] },
    { "gpt-5.4": "github-copilot" },
    { "gpt-5.5": "opencode" },
    { "kimi-k2.6": "opencode-go" },
    { "minimax-m2.7": "opencode-go" },
    { "mistral-large": "opencode-go" },
    { "qwen3.6-plus": "opencode-go" },
];
