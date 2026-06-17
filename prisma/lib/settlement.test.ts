import { assertEquals } from "@std/assert";
import { isSettled, SETTLED_SCORE_THRESHOLD } from "./settlement.ts";

Deno.test({
    name: "isSettled is driven by score, not vote count",
    fn() {
        // Boundary at the threshold
        assertEquals(isSettled(SETTLED_SCORE_THRESHOLD), true);
        assertEquals(isSettled(SETTLED_SCORE_THRESHOLD - 1), false);

        // Regression guard for #271: many votes but low score is NOT settled.
        // The old `modelCount >= 3` check wrongly marked this settled.
        assertEquals(isSettled(5), false); // e.g. { modelCount: 10, score: 5 }

        // Inverse: few votes but high score IS settled.
        assertEquals(isSettled(9), true); // e.g. { modelCount: 1, score: 9 }
    },
});
