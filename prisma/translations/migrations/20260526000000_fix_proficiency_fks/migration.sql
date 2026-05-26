-- Migration: Fix language_proficiencies from implicit N:M to explicit FK columns
-- Hand-written because Prisma migrate dev requires interactive TTY.

-- Step 1: Deduplicate — keep the lowest ID per (modelId, languageId) pair
DELETE FROM "language_proficiencies" WHERE "id" NOT IN (
    SELECT MIN(lp."id")
    FROM "language_proficiencies" lp
    JOIN "_language_proficienciesTollm_models" jm ON jm."A" = lp."id"
    JOIN "_language_proficienciesTolanguages" jl ON jl."A" = lp."id"
    GROUP BY jm."B", jl."B"
);

-- Step 2: Add nullable FK columns and migrate data from junction tables
ALTER TABLE "language_proficiencies" ADD COLUMN "modelId" INTEGER;
ALTER TABLE "language_proficiencies" ADD COLUMN "languageId" INTEGER;
UPDATE "language_proficiencies" SET "modelId" = (
    SELECT "B" FROM "_language_proficienciesTollm_models" WHERE "A" = "id"
);
UPDATE "language_proficiencies" SET "languageId" = (
    SELECT "B" FROM "_language_proficienciesTolanguages" WHERE "A" = "id"
);

-- Step 3: Drop junction tables (no longer needed with explicit FKs)
DROP TABLE "_language_proficienciesTollm_models";
DROP TABLE "_language_proficienciesTolanguages";

-- Step 4: Recreate table with NOT NULL, CHECK, FKs, and Unique
-- SQLite doesn't support ALTER COLUMN NOT NULL, so recreate is needed.
CREATE TABLE "language_proficiencies_new" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "level" INTEGER NOT NULL CHECK("level" BETWEEN 1 AND 5),
    "modelId" INTEGER NOT NULL,
    "languageId" INTEGER NOT NULL,
    CONSTRAINT "language_proficiencies_modelId_fkey" FOREIGN KEY ("modelId") REFERENCES "llm_models" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "language_proficiencies_languageId_fkey" FOREIGN KEY ("languageId") REFERENCES "languages" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);

-- Step 5: Copy migrated data into new table
INSERT INTO "language_proficiencies_new" ("id", "level", "modelId", "languageId")
SELECT "id", "level", "modelId", "languageId" FROM "language_proficiencies";

-- Step 6: Replace old table with new one
DROP TABLE "language_proficiencies";
ALTER TABLE "language_proficiencies_new" RENAME TO "language_proficiencies";

-- Step 7: Unique index for @@unique([modelId, languageId])
-- Naming follows Prisma convention: tablename_field1_field2_key
CREATE UNIQUE INDEX "language_proficiencies_modelId_languageId_key" ON "language_proficiencies"("modelId", "languageId");
