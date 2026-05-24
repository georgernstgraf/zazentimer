export interface TranslationEntry {
  key: string;
  translation: string | null;
}

export interface TranslationOutput {
  locale: string;
  model: string;
  proficiency: number;
  translations: TranslationEntry[];
}

export class VerifyError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "VerifyError";
  }
}

const PLACEHOLDER_RE = /(%[sd]|%\d+\$[sd]|%%)|(&lt;)|(&gt;)|(&amp;)|(&quot;)|(&#39;)|(<br\s*\/?>)|(<\/?[a-z]+\s*[^>]*>)/gi;

export function extractPlaceholders(text: string): string[] {
  const matches = text.match(PLACEHOLDER_RE);
  return matches || [];
}

export interface InputString {
  key: string;
  text: string;
}

export function verify(
  raw: string,
  expectedLocale: string,
  expectedModel: string,
  inputStrings: InputString[],
): TranslationOutput {
  const inputKeyToText = new Map(inputStrings.map((s) => [s.key, s.text]));
  const inputKeys = new Set(inputStrings.map((s) => s.key));
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    throw new VerifyError("Response is not valid JSON");
  }

  if (typeof parsed !== "object" || parsed === null) {
    throw new VerifyError("Response must be a JSON object");
  }

  const obj = parsed as Record<string, unknown>;

  if (obj.locale !== expectedLocale) {
    throw new VerifyError(
      `locale mismatch: expected '${expectedLocale}', got '${obj.locale}'`,
    );
  }

  if (obj.model !== expectedModel) {
    throw new VerifyError(
      `model mismatch: expected '${expectedModel}', got '${obj.model}'`,
    );
  }

  const proficiency = obj.proficiency;
  if (
    typeof proficiency !== "number" || proficiency < 1 || proficiency > 5 ||
    !Number.isInteger(proficiency)
  ) {
    throw new VerifyError(
      `proficiency must be an integer 1-5, got ${JSON.stringify(proficiency)}`,
    );
  }

  if (!Array.isArray(obj.translations)) {
    throw new VerifyError("translations must be an array");
  }

  const outputKeys = new Set<string>();
  const translations: TranslationEntry[] = [];

  for (const entry of obj.translations) {
    if (typeof entry !== "object" || entry === null) {
      throw new VerifyError("Each translation entry must be an object");
    }
    const { key, translation } = entry as Record<string, unknown>;

    if (typeof key !== "string" || !key) {
      throw new VerifyError("Each entry must have a non-empty 'key' string");
    }

    if (translation !== null && typeof translation !== "string") {
      throw new VerifyError(
        `translation for '${key}' must be a string or null`,
      );
    }

    if (outputKeys.has(key)) {
      throw new VerifyError(`Duplicate key '${key}' in output`);
    }
    outputKeys.add(key);

    if (translation !== null) {
      const inputText = inputKeyToText.get(key as string);
      if (inputText) {
        const srcPlaceholders = extractPlaceholders(inputText);
        const tgtPlaceholders = extractPlaceholders(translation as string);
        if (srcPlaceholders.length !== tgtPlaceholders.length) {
          throw new VerifyError(
            `Placeholder count mismatch for '${key}': source has ${srcPlaceholders.length}, translation has ${tgtPlaceholders.length}`,
          );
        }
      }
      translations.push({ key, translation: translation as string });
    } else {
      translations.push({ key, translation: null });
    }
  }

  for (const key of inputKeys) {
    if (!outputKeys.has(key)) {
      throw new VerifyError(`Missing key '${key}' in output`);
    }
  }

  return {
    locale: obj.locale as string,
    model: obj.model as string,
    proficiency,
    translations,
  };
}
