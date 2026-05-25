import { Hono } from "hono";
import { HTTPException } from "hono/http-exception";
import { getPrisma } from "./lib/prisma.ts";
import {
    getComparison,
    getCoverage,
    getEvaluation,
    getLanguages,
    getLanguagesWithStats,
    getModels,
    getProficiencies,
    getStats,
    getStrings,
    getVotesGrouped,
} from "./lib/db.ts";

const app = new Hono();

// ── JSX Components ────────────────────────────────────────────────────────────

function Layout(
    { title, children }: { title: string; children: unknown },
) {
    return (
        <html lang="en">
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <title>{title} — ZazenTimer Votes</title>
                <link
                    rel="stylesheet"
                    href="https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.violet.min.css"
                />
                <script src="https://unpkg.com/htmx.org@2.0.4">
                </script>
            </head>
            <body>
                <nav class="container-fluid">
                    <ul>
                        <li>
                            <a href="/" class="contrast"><strong>ZazenTimer Votes</strong></a>
                        </li>
                    </ul>
                    <ul>
                        <li><a href="/models">Models</a></li>
                        <li><a href="/strings">Strings</a></li>
                        <li><a href="/languages">Languages</a></li>
                        <li><a href="/evaluation">Evaluation</a></li>
                    </ul>
                </nav>
                <main class="container">
                    {children}
                </main>
            </body>
        </html>
    );
}

function LevelBadge({ level }: { level: number }) {
    const colors: Record<number, string> = {
        1: "var(--pico-color-red)",
        2: "var(--pico-color-orange)",
        3: "var(--pico-color-yellow)",
        4: "var(--pico-color-green-500)",
        5: "var(--pico-color-green)",
    };
    const labels: Record<number, string> = {
        1: "Basic",
        2: "Adequate",
        3: "Good",
        4: "Strong",
        5: "Native",
    };
    return (
        <span
            style={`color: ${colors[level] || "inherit"}; font-weight: bold;`}
            title={labels[level]}
        >
            {"★".repeat(level)}{"☆".repeat(5 - level)}
        </span>
    );
}

function CoverageBar({ pct }: { pct: number }) {
    const color = pct >= 90
        ? "var(--pico-color-green)"
        : pct >= 50
        ? "var(--pico-color-yellow)"
        : "var(--pico-color-red)";
    return (
        <div
            style={`
        background: var(--pico-color-zinc-200);
        border-radius: var(--pico-border-radius);
        height: 1.2rem;
        overflow: hidden;
      `}
        >
            <div
                style={`
          background: ${color};
          width: ${pct}%;
          height: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 0.7rem;
          color: white;
          font-weight: bold;
          transition: width 0.3s ease;
        `}
            >
                {pct >= 15 ? `${pct}%` : ""}
            </div>
        </div>
    );
}

// ── API Routes (JSON) ─────────────────────────────────────────────────────────

app.get("/api/models", async (c) => {
    const models = await getModels();
    return c.json(models);
});

app.get("/api/models/:id/proficiencies", async (c) => {
    const id = parseInt(c.req.param("id"), 10);
    if (isNaN(id)) throw new HTTPException(400, { message: "Invalid model id" });
    const proficiencies = await getProficiencies(id);
    return c.json(proficiencies);
});

app.get("/api/languages", async (c) => {
    const languages = await getLanguages();
    return c.json(languages);
});

app.get("/api/strings", async (c) => {
    const search = c.req.query("search") || "";
    const strings = await getStrings(search);
    return c.json(strings);
});

app.get("/api/models/:mid/languages/:lid/votes", async (c) => {
    const mid = parseInt(c.req.param("mid"), 10);
    const lid = parseInt(c.req.param("lid"), 10);
    if (isNaN(mid) || isNaN(lid)) {
        throw new HTTPException(400, { message: "Invalid model or language id" });
    }
    const groups = await getVotesGrouped(mid, lid);
    return c.json(groups);
});

app.get("/api/models/:mid/languages/:lid/coverage", async (c) => {
    const mid = parseInt(c.req.param("mid"), 10);
    const lid = parseInt(c.req.param("lid"), 10);
    if (isNaN(mid) || isNaN(lid)) {
        throw new HTTPException(400, { message: "Invalid model or language id" });
    }
    const coverage = await getCoverage(mid, lid);
    return c.json(coverage);
});

app.get("/api/strings/:sid/comparison", async (c) => {
    const sid = parseInt(c.req.param("sid"), 10);
    const langId = parseInt(c.req.query("langId") || "0", 10);
    if (isNaN(sid)) throw new HTTPException(400, { message: "Invalid string id" });
    const comparison = await getComparison(sid, langId);
    return c.json(comparison);
});

app.get("/api/stats", async (c) => {
    const stats = await getStats();
    return c.json(stats);
});

// ── POST /api/votes (bestehendes Pattern) ─────────────────────────────────────

app.post("/api/votes", async (c) => {
    const body = await c.req.json<{
        bcp_47?: string;
        model?: string;
        master_text?: string;
        translation?: string;
    }>();

    const { bcp_47, model, master_text, translation } = body;
    if (!bcp_47 || !model || !master_text || !translation) {
        throw new HTTPException(400, {
            message: "bcp_47, model, master_text, and translation are required",
        });
    }

    const prisma = await getPrisma();
    const language = await prisma.languages.findUnique({ where: { bcp_47 } });
    if (!language) throw new HTTPException(404, { message: `Language '${bcp_47}' not found` });

    const llmModel = await prisma.llm_models.findUnique({ where: { name: model } });
    if (!llmModel) throw new HTTPException(404, { message: `Model '${model}' not found` });

    const masterString = await prisma.master_strings.findUnique({
        where: { text: master_text },
    });
    if (!masterString) {
        throw new HTTPException(404, { message: `Master string '${master_text}' not found` });
    }

    const vote = await prisma.votes.upsert({
        where: {
            languagesId_llm_modelsId_master_stringsId_translation: {
                languagesId: language.id,
                llm_modelsId: llmModel.id,
                master_stringsId: masterString.id,
                translation,
            },
        },
        update: {},
        create: {
            languagesId: language.id,
            llm_modelsId: llmModel.id,
            master_stringsId: masterString.id,
            translation,
        },
    });

    return c.json(vote, 201);
});

// ── Frontend Pages (JSX) ──────────────────────────────────────────────────────

app.get("/", async (c) => {
    const { models, languages, votes, strings } = await getStats();

    return c.html(
        <Layout title="Dashboard">
            <hgroup>
                <h1>Dashboard</h1>
                <p>Translation Voting Database</p>
            </hgroup>
            <div class="grid">
                <article>
                    <h2>{models}</h2>
                    <footer><a href="/models">Models</a></footer>
                </article>
                <article>
                    <h2>{languages}</h2>
                    <footer><a href="/languages">Languages</a></footer>
                </article>
                <article>
                    <h2>{votes}</h2>
                    <footer>Votes (Translations)</footer>
                </article>
                <article>
                    <h2>{strings}</h2>
                    <footer><a href="/strings">Master Strings</a></footer>
                </article>
            </div>
        </Layout>,
    );
});

async function renderProficiencyTableContent(
    modelId: number,
    sort: string,
    dir: string,
) {
    if (!modelId) return <p>Select a model above.</p>;

    const [proficiencies, coverage] = await Promise.all([
        getProficiencies(modelId),
        (async () => {
            const results: { langId: number; translated: number; total: number; pct: number }[] = [];
            for (const l of await getLanguages()) {
                const c = await getCoverage(modelId, l.id);
                results.push({
                    langId: l.id,
                    translated: c.translated,
                    total: c.total,
                    pct: c.coverage_pct,
                });
            }
            return results;
        })(),
    ]);

    type Row = typeof proficiencies[number] & { coverage?: { langId: number; translated: number; total: number; pct: number } };
    const rows: Row[] = proficiencies.map((p) => {
        const langData = p.languages[0];
        if (!langData) return null;
        const cov = coverage.find((c) => c.langId === langData.id);
        return { ...p, coverage: cov };
    }).filter(Boolean) as Row[];

    if (sort && dir) {
        const getVal = (r: Row): string | number => {
            const lang = r.languages[0];
            switch (sort) {
                case "language": return lang?.english_name || "";
                case "bcp47": return lang?.bcp_47 || "";
                case "proficiency": return r.level;
                case "coverage": return r.coverage?.pct || 0;
                case "votes": return r.coverage?.translated || 0;
                default: return 0;
            }
        };
        rows.sort((a, b) => {
            const va = getVal(a);
            const vb = getVal(b);
            if (typeof va === "string" && typeof vb === "string") {
                return dir === "asc" ? va.localeCompare(vb) : vb.localeCompare(va);
            }
            const na = Number(va);
            const nb = Number(vb);
            return dir === "asc" ? na - nb : nb - na;
        });
    }

    const nextDir = dir === "asc" ? "desc" : "asc";

    function sortLink(field: string, label: string) {
        const indicator = sort === field ? (dir === "asc" ? "▲" : "▼") : "";
        return (
            <a
                href="#"
                hx-get={`/models?modelId=${modelId}&sort=${field}&dir=${nextDir}`}
                hx-target="#prof-output"
                hx-push-url="true"
                style="text-decoration: none; color: inherit;"
            >
                {label} {indicator}
            </a>
        );
    }

    return (
        <div>
            <table>
                <thead>
                    <tr>
                        <th>{sortLink("language", "Language")}</th>
                        <th>{sortLink("bcp47", "BCP 47")}</th>
                        <th>{sortLink("proficiency", "Proficiency")}</th>
                        <th style="text-align: center;">
                            {sortLink("coverage", "Coverage")}
                        </th>
                        <th>{sortLink("votes", "Votes")}</th>
                    </tr>
                </thead>
                <tbody>
                    {rows.map((r) => {
                        const langData = r.languages[0];
                        if (!langData) return null;
                        return (
                            <tr>
                                <td>{langData.english_name}</td>
                                <td><code>{langData.bcp_47}</code></td>
                                <td><LevelBadge level={r.level} /></td>
                                <td style="text-align: center;">
                                    <CoverageBar pct={r.coverage?.pct || 0} />
                                </td>
                                <td>
                                    <a
                                        href={`/models/${modelId}/languages/${langData.id}`}
                                    >
                                        View ({r.coverage?.translated || 0})
                                    </a>
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}

app.get("/models", async (c) => {
    const modelId = parseInt(c.req.query("modelId") || "0", 10);
    const sort = c.req.query("sort") || "";
    const dir = c.req.query("dir") || "";
    const isHtmx = c.req.header("HX-Request") === "true";
    const models = await getModels();

    if (isHtmx) {
        const content = await renderProficiencyTableContent(modelId, sort, dir);
        return c.html(content);
    }

    return c.html(
        <Layout title="Models">
            <hgroup>
                <h1>Models</h1>
                <p>Model proficiency levels by language</p>
            </hgroup>

            <div class="grid">
                <select
                    name="modelId"
                    hx-get={`/models?sort=${sort}&dir=${dir}`}
                    hx-target="#prof-output"
                    hx-trigger="change"
                    hx-push-url="true"
                >
                    <option value="">— Select Model —</option>
                    {models.map((m) => (
                        <option value={m.id} selected={m.id === modelId}>
                            {m.name}
                        </option>
                    ))}
                </select>
            </div>

            <div id="prof-output">
                {modelId
                    ? await renderProficiencyTableContent(modelId, sort, dir)
                    : <p>Select a model above to see proficiency levels.</p>}
            </div>
        </Layout>,
    );
});

app.get("/models/:mid/languages/:lid", async (c) => {
    const mid = parseInt(c.req.param("mid"), 10);
    const lid = parseInt(c.req.param("lid"), 10);
    if (isNaN(mid) || isNaN(lid)) throw new HTTPException(400);

    const prisma = await getPrisma();
    const [model, lang, groups, coverage] = await Promise.all([
        prisma.llm_models.findUnique({ where: { id: mid } }),
        prisma.languages.findUnique({ where: { id: lid } }),
        getVotesGrouped(mid, lid),
        getCoverage(mid, lid),
    ]);

    if (!model || !lang) throw new HTTPException(404);

    return c.html(
        <Layout title={`${model.name} — ${lang.bcp_47}`}>
            <a href="/models">&larr; Back to Models</a>
            <hgroup>
                <h1><a href={`/models?modelId=${mid}`} style="text-decoration: none; color: inherit;">{model.name}</a></h1>
                <p>
                    {lang.english_name} ({lang.bcp_47})
                </p>
            </hgroup>

            <article>
                <header>Coverage</header>
                <p>
                    {coverage.translated} / {coverage.total} strings (
                    {coverage.coverage_pct}%)
                </p>
                <CoverageBar pct={coverage.coverage_pct} />
            </article>

            <article>
                <header>Translations ({groups.length} strings)</header>
                <table>
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>Master String</th>
                            <th>Translations</th>
                        </tr>
                    </thead>
                    <tbody>
                        {groups.map((g, i) => (
                            <tr>
                                <td>{i + 1}</td>
                                <td>{g.master_string}</td>
                                <td>
                                    {g.translations.map((t, j) => (
                                        <div>
                                            <code>{t}</code>
                                            {j < g.translations.length - 1
                                                ? <br />
                                                : ""}
                                        </div>
                                    ))}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </article>
        </Layout>,
    );
});

app.get("/strings", async (c) => {
    const search = c.req.query("search") || "";
    const sort = c.req.query("sort") || "";
    const dir = c.req.query("dir") || "";
    const isHtmx = c.req.header("HX-Request") === "true";

    const strings = await getStrings(search);

    if (sort && dir) {
        strings.sort((a, b) => {
            const getVal = (s: typeof strings[number]): string | number => {
                switch (sort) {
                    case "id": return s.id;
                    case "text": return s.text;
                    case "translations": return s.voteCount;
                    default: return 0;
                }
            };
            const va = getVal(a);
            const vb = getVal(b);
            if (typeof va === "string" && typeof vb === "string") {
                return dir === "asc" ? va.localeCompare(vb) : vb.localeCompare(va);
            }
            return dir === "asc" ? Number(va) - Number(vb) : Number(vb) - Number(va);
        });
    }

    const nextDir = dir === "asc" ? "desc" : "asc";

    function sortLink(field: string, label: string) {
        const indicator = sort === field ? (dir === "asc" ? "▲" : "▼") : "";
        return (
            <a
                href="#"
                hx-get={`/strings?search=${search}&sort=${field}&dir=${nextDir}`}
                hx-target="#string-table"
                hx-push-url="true"
                style="text-decoration: none; color: inherit;"
            >
                {label} {indicator}
            </a>
        );
    }

    const tableContent = strings.length === 0
        ? <p>No strings found.</p>
        : (
            <table>
                <thead>
                    <tr>
                        <th>{sortLink("id", "ID")}</th>
                        <th>{sortLink("text", "Text")}</th>
                        <th>{sortLink("translations", "Translations")}</th>
                        <th>Compare</th>
                    </tr>
                </thead>
                <tbody>
                    {strings.map((s) => (
                        <tr key={s.id}>
                            <td>{s.id}</td>
                            <td>{s.text}</td>
                            <td>{s.voteCount}</td>
                            <td>
                                <a href={`/strings/${s.id}/comparison`}>Compare</a>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        );

    if (isHtmx) return c.html(tableContent);

    return c.html(
        <Layout title="Strings">
            <hgroup>
                <h1>Master Strings</h1>
                <p>Search strings and compare translations across models</p>
            </hgroup>

            <input
                type="search"
                name="search"
                placeholder="Search strings..."
                value={search}
                hx-get={`/strings?sort=${sort}&dir=${dir}`}
                hx-target="#string-table"
                hx-trigger="keyup changed delay:300ms"
                hx-push-url="true"
            />

            <div id="string-table">
                {tableContent}
            </div>
        </Layout>,
    );
});

async function renderComparisonContent(sid: number, langId: number) {
    const [models, comparison] = await Promise.all([
        getModels(),
        getComparison(sid, langId),
    ]);

    return (
        <>
            <details>
                <summary>Model Details</summary>
                <table>
                    <thead>
                        <tr>
                            <th>Model</th>
                            <th>Translations</th>
                        </tr>
                    </thead>
                    <tbody>
                        {models.map((m) => {
                            const cmp = comparison.comparisons.find((c) =>
                                c.modelId === m.id
                            );
                            return (
                                <tr>
                                    <td>{m.name}</td>
                                    <td>
                                        {cmp
                                            ? cmp.translations.map((t) => (
                                                <code>{t}</code>
                                            ))
                                            : <em>No data</em>}
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </details>

            <ComparisonTableView
                comparisons={comparison.comparisons}
                masterString={comparison.master_string}
            />
        </>
    );
}

app.get("/strings/:sid/comparison", async (c) => {
    const sid = parseInt(c.req.param("sid"), 10);
    if (isNaN(sid)) throw new HTTPException(400, { message: "Invalid string id" });

    const prisma = await getPrisma();
    const masterString = await prisma.master_strings.findUnique({ where: { id: sid } });
    if (!masterString) throw new HTTPException(404, { message: "String not found" });

    const langIds = await prisma.votes.findMany({
        where: { master_stringsId: sid },
        select: { languagesId: true },
        distinct: ["languagesId"],
    });
    const languages = await prisma.languages.findMany({
        where: { id: { in: langIds.map((l) => l.languagesId) } },
        orderBy: { bcp_47: "asc" },
    });

    return c.html(
        <Layout title="String Comparison">
            <a href="/strings">&larr; Back to Strings</a>
            <hgroup>
                <h1>String Comparison</h1>
                <p>Master string: <code>{masterString.text}</code></p>
            </hgroup>

            <h3>Filter by Language</h3>
            <select
                name="langId"
                hx-get={`/strings/${sid}/comparison/table`}
                hx-target="#comparison-content"
                hx-trigger="change"
            >
                <option value="0">— All Languages —</option>
                {languages.map((l) => (
                    <option value={l.id}>
                        {l.english_name} ({l.bcp_47})
                    </option>
                ))}
            </select>

            <div id="comparison-content">
                {await renderComparisonContent(sid, 0)}
            </div>
        </Layout>,
    );
});

function ComparisonTableView(
    { comparisons, masterString: _masterString }: {
        comparisons: { model: string; translations: string[] }[];
        masterString: string;
    },
) {
    if (comparisons.length === 0) {
        return <p>No translations yet.</p>;
    }

    // For each model, find consensus:
    // A translation is "consensus" if 2+ models have it
    const allTranslations = comparisons.flatMap((c) => c.translations);
    const translationCounts: Record<string, number> = {};
    for (const t of allTranslations) {
        translationCounts[t] = (translationCounts[t] || 0) + 1;
    }

    return (
        <table>
            <thead>
                <tr>
                    <th>Model</th>
                    <th>Translations</th>
                    <th>Consensus</th>
                </tr>
            </thead>
            <tbody>
                {comparisons.map((c) => (
                    <tr>
                        <td><strong>{c.model}</strong></td>
                        <td>
                            {c.translations.map((t) => (
                                <div>
                                    <code>{t}</code>
                                </div>
                            ))}
                            {c.translations.length === 0
                                ? <em style="color: var(--pico-color-red);">No translation</em>
                                : ""}
                        </td>
                        <td>
                            {c.translations
                                .filter((t) => (translationCounts[t] || 0) >= 2)
                                .map((t) => (
                                    <div>
                                        <mark>{t}</mark>
                                    </div>
                                ))}
                            {c.translations.filter((t) => (translationCounts[t] || 0) >= 2)
                                    .length === 0
                                ? <span style="color: var(--pico-color-zinc-500);">—</span>
                                : ""}
                        </td>
                    </tr>
                ))}
            </tbody>
        </table>
    );
}

// ── htmx Fragment Routes ──────────────────────────────────────────────────────



app.get("/strings/:sid/comparison/table", async (c) => {
    const sid = parseInt(c.req.param("sid"), 10);
    const langId = parseInt(c.req.query("langId") || "0", 10);
    if (isNaN(sid)) return c.html(<p>Invalid string ID.</p>);

    return c.html(await renderComparisonContent(sid, langId));
});

// ── Languages Page ────────────────────────────────────────────────────────────

app.get("/languages", async (c) => {
    const search = c.req.query("search") || "";
    const sort = c.req.query("sort") || "";
    const dir = c.req.query("dir") || "";
    const isHtmx = c.req.header("HX-Request") === "true";

    const languages = await getLanguagesWithStats(search);

    if (sort && dir) {
        languages.sort((a, b) => {
            const va = (() => {
                switch (sort) {
                    case "bcp47": return a.bcp_47;
                    case "name": return a.english_name;
                    case "posix": return a.posix_code;
                    case "iso": return a.iso_639_3;
                    case "whisper": return a.whisper_response || "";
                    case "models": return a.modelCount;
                    case "votes": return a.voteCount;
                    default: return 0;
                }
            })();
            const vb = (() => {
                switch (sort) {
                    case "bcp47": return b.bcp_47;
                    case "name": return b.english_name;
                    case "posix": return b.posix_code;
                    case "iso": return b.iso_639_3;
                    case "whisper": return b.whisper_response || "";
                    case "models": return b.modelCount;
                    case "votes": return b.voteCount;
                    default: return 0;
                }
            })();
            if (typeof va === "string" && typeof vb === "string") {
                return dir === "asc" ? va.localeCompare(vb) : vb.localeCompare(va);
            }
            return dir === "asc" ? Number(va) - Number(vb) : Number(vb) - Number(va);
        });
    }

    const nextDir = dir === "asc" ? "desc" : "asc";

    function sortLink(field: string, label: string) {
        const indicator = sort === field ? (dir === "asc" ? "▲" : "▼") : "";
        return (
            <a
                href="#"
                hx-get={`/languages?search=${search}&sort=${field}&dir=${nextDir}`}
                hx-target="#language-table"
                hx-push-url="true"
                style="text-decoration: none; color: inherit;"
            >
                {label} {indicator}
            </a>
        );
    }

    const tableContent = languages.length === 0
        ? <p>No languages found.</p>
        : (
            <table>
                <thead>
                    <tr>
                        <th>{sortLink("bcp47", "BCP 47")}</th>
                        <th>{sortLink("name", "English Name")}</th>
                        <th>{sortLink("posix", "POSIX")}</th>
                        <th>{sortLink("iso", "ISO 639-3")}</th>
                        <th>{sortLink("whisper", "Whisper")}</th>
                        <th style="text-align: center;">{sortLink("models", "Models")}</th>
                        <th style="text-align: center;">{sortLink("votes", "Votes")}</th>
                    </tr>
                </thead>
                <tbody>
                    {languages.map((l) => (
                        <tr>
                            <td><code>{l.bcp_47}</code></td>
                            <td><a href={`/languages/${l.id}`}>{l.english_name}</a></td>
                            <td><code>{l.posix_code}</code></td>
                            <td><code>{l.iso_639_3}</code></td>
                            <td>{l.whisper_response || <em style="color: var(--pico-color-zinc-500);">—</em>}</td>
                            <td style="text-align: center;">{l.modelCount}</td>
                            <td style="text-align: center;">{l.voteCount}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        );

    if (isHtmx) return c.html(tableContent);

    return c.html(
        <Layout title="Languages">
            <hgroup>
                <h1>Languages</h1>
                <p>Browse {languages.length} locales</p>
            </hgroup>

            <input
                type="search"
                name="search"
                placeholder="Search by name or BCP 47..."
                value={search}
                hx-get={`/languages?sort=${sort}&dir=${dir}`}
                hx-target="#language-table"
                hx-trigger="keyup changed delay:300ms"
                hx-push-url="true"
            />

            <div id="language-table">
                {tableContent}
            </div>
        </Layout>,
    );
});

app.get("/languages/:lid", async (c) => {
    const lid = parseInt(c.req.param("lid"), 10);
    if (isNaN(lid)) throw new HTTPException(400, { message: "Invalid language id" });

    const prisma = await getPrisma();
    const language = await prisma.languages.findUnique({ where: { id: lid } });
    if (!language) throw new HTTPException(404, { message: "Language not found" });

    return c.html(
        <Layout title={language.english_name}>
            <a href="/languages">&larr; Back to Languages</a>
            <hgroup>
                <h1>{language.english_name}</h1>
                <p>
                    {language.bcp_47}
                    {" — "}
                    <code>{language.posix_code}</code>
                    {" — "}
                    <code>{language.iso_639_3}</code>
                </p>
            </hgroup>

            <article>
                <header>Details</header>
                {language.whisper_response
                    ? <p>Whisper response: <code>{language.whisper_response}</code></p>
                    : <p>Whisper: <em>Not supported</em></p>}
                <p>Directory: <code>{language.directory}</code></p>
            </article>
        </Layout>,
    );
});

// ── Evaluation Page ─────────────────────────────────────────────────────────

app.get("/evaluation", async (c) => {
    const langId = parseInt(c.req.query("langId") || "0", 10);
    const sort = c.req.query("sort") || "";
    const dir = c.req.query("dir") || "";
    const isHtmx = c.req.header("HX-Request") === "true";

    const prisma = await getPrisma();
    const langIds = await prisma.votes.findMany({
        select: { languagesId: true },
        distinct: ["languagesId"],
    });
    const languages = await prisma.languages.findMany({
        where: { id: { in: langIds.map((l) => l.languagesId) } },
        orderBy: { bcp_47: "asc" },
    });

    const data = langId ? await getEvaluation(langId) : [];

    // Default sort: models desc, then score desc (getEvaluation already does this)
    if (sort && dir) {
        data.sort((a, b) => {
            let va: string | number;
            let vb: string | number;
            switch (sort) {
                case "master_string": va = a.master_string; vb = b.master_string; break;
                case "translation": va = a.translation; vb = b.translation; break;
                case "score": va = a.score; vb = b.score; break;
                case "models": va = a.modelCount; vb = b.modelCount; break;
                case "settled": va = a.modelCount >= 3 ? 1 : 0; vb = b.modelCount >= 3 ? 1 : 0; break;
                default: return 0;
            }
            if (typeof va === "string") {
                return dir === "asc" ? va.localeCompare(vb as string) : (vb as string).localeCompare(va);
            }
            return dir === "asc" ? Number(va) - Number(vb) : Number(vb) - Number(va);
        });
    }

    const nextDir = dir === "asc" ? "desc" : "asc";

    function sortLink(field: string, label: string) {
        const indicator = sort === field ? (dir === "asc" ? "▲" : "▼") : "";
        return (
            <a
                href="#"
                hx-get={`/evaluation?langId=${langId}&sort=${field}&dir=${nextDir}`}
                hx-target="#eval-table"
                hx-push-url="true"
                style="text-decoration: none; color: inherit;"
            >
                {label} {indicator}
            </a>
        );
    }

    const tableContent = data.length === 0
        ? <p>{langId ? "No evaluation data found." : "Select a language to see evaluation results."}</p>
        : (
            <table>
                <thead>
                    <tr>
                        <th>{sortLink("master_string", "Master String")}</th>
                        <th>{sortLink("translation", "Translation")}</th>
                        <th style="text-align: center;">{sortLink("models", "Votes")}</th>
                        <th style="text-align: center;">{sortLink("score", "Confidence")}</th>
                        <th style="text-align: center;">{sortLink("settled", "Settled")}</th>
                        <th>Models</th>
                    </tr>
                </thead>
                <tbody>
                    {data.map((r) => {
                        const settled = r.modelCount >= 3;
                        return (
                            <tr>
                                <td>{r.master_string}</td>
                                <td>
                                    <strong>{r.translation}</strong>
                                </td>
                                <td style="text-align: center;">{r.modelCount}</td>
                                <td style="text-align: center;">
                                    <code>{r.score}</code>
                                </td>
                                <td style="text-align: center;">
                                    {settled
                                        ? <mark style="background: var(--pico-color-green); color: white; padding: 0.1rem 0.4rem; border-radius: var(--pico-border-radius);">✓</mark>
                                        : <span style="color: var(--pico-color-zinc-500);">—</span>}
                                </td>
                                <td style="font-size: 0.85rem;">{r.modelNames}</td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        );

    if (isHtmx) return c.html(tableContent);

    return c.html(
        <Layout title="Evaluation">
            <hgroup>
                <h1>Evaluation</h1>
                <p>Democratic vote: sorted by vote count, ties broken by confidence sum</p>
            </hgroup>

            <select
                name="langId"
                hx-get="/evaluation"
                hx-target="#eval-table"
                hx-trigger="change"
                hx-push-url="true"
            >
                <option value="">— Select Language —</option>
                {languages.map((l) => (
                    <option value={l.id} selected={l.id === langId}>
                        {l.english_name} ({l.bcp_47})
                    </option>
                ))}
            </select>

            <div id="eval-table">
                {tableContent}
            </div>
        </Layout>,
    );
});

// ── Error Handler ─────────────────────────────────────────────────────────────

app.onError((err, c) => {
    if (err instanceof HTTPException) return err.getResponse();
    if (err instanceof SyntaxError) return c.json({ error: "Invalid JSON body" }, 400);
    if (typeof err === "object" && err !== null && "code" in err) {
        const prismaErr = err as { code: string; message: string };
        if (prismaErr.code === "P2002") return c.json({ error: prismaErr.message }, 409);
        return c.json({ error: prismaErr.message }, 400);
    }
    console.error("Unhandled error:", err);
    return c.json({ error: "Internal server error" }, 500);
});

// ── Server ────────────────────────────────────────────────────────────────────

Deno.serve({ port: 8000 }, app.fetch);
