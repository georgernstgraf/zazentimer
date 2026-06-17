// Threshold for a translation to be considered "settled" (final).
// A string is settled when its best translation's score (sum of voter
// proficiency levels) reaches this value. Shared by db.ts (data layer)
// and voting_api.tsx (dashboard) so they cannot diverge. Pure module:
// no Prisma import so it loads cleanly under `deno test`.
export const SETTLED_SCORE_THRESHOLD = 7;

/** A translation is settled iff its score reached the threshold. */
export function isSettled(score: number): boolean {
    return score >= SETTLED_SCORE_THRESHOLD;
}
