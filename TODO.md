# fx-w2d4 — The machines take over: three altitudes, CI & CD

*Decks: JUnit & Mockito · Test strategy · CI/CD with GitHub Actions · Supply chain · ~4h hands-on*

> **This file is the day's entry point.** The tasks themselves are individual sheets under
> [`challenge/`](challenge/) — see the table below. Overview & cheatsheet:
> [`challenge/index.html`](challenge/index.html). Answers: [`solution/`](solution/), one runnable
> workspace per phase — struggle first, peek only when properly stuck.
>
> **See it before you build it:** [`challenge/architecture.html`](challenge/architecture.html) —
> the test pyramid and the CI→CD pipeline, drawn. Five minutes before Task 1.

---

## The story so far

Yesterday you put the exchange in a box. A multi-stage image, `docker compose up`, and the whole
stack — app, a Liquibase-owned MySQL, a monitor, an upstream feed — rises on a laptop with nothing
installed. It is reproducible. Anyone can pull it and run it.

But "it runs" is not "it is correct." Right now the only thing standing between a broken
`findLatest()` query and production is *you*, remembering to `curl` the endpoint after every
change and eyeballing the number. That is not a safety net. It is a person being careful, which is
the thing that fails at 5pm on a Friday.

Today the machines take over the checking. You'll write tests at **three altitudes** — a unit test
that runs in milliseconds with everything mocked, a web-slice test that exercises the controller
and its validation, and an **integration test that boots a real MySQL** and runs your actual SQL
against it. Then you'll hand those tests to a **CI pipeline** that runs them on every push and
*blocks the merge* when they go red, so a broken build physically cannot reach `main`. You'll look
at what your dependencies drag in behind them. And then you'll turn yesterday's image into a
**CD pipeline**: verify once, publish to a registry, and wait for a human before anything is called
"deployed" — because that pause is the whole difference between automation and abdication.

**Why today matters.** Everything you shipped this fortnight is only as trustworthy as the last
time you checked it by hand. Today the checking becomes automatic, repeatable, and enforced — and
a change you can't ship without a green suite and a human nod is the only kind you can make at
speed without fear.

## What's in this folder

```
fx-w2d4/
├── TODO.md            ← you are here: the day's entry point
├── AGENTS.md          ← house rules for your AI assistant
├── challenge/         ← everything you need today
│   ├── 01…05-*.md         the task sheets, in order
│   ├── index.html         overview, cheatsheet, extra challenges
│   └── architecture.html  the test pyramid + the CI/CD pipeline, drawn
├── fx-exchange/       ← CATCH-UP COPY ONLY — your whole repo at the `w2d3-done` end state
└── solution/          ← one runnable workspace per phase, 01–05
```

## Before you start

**You are not starting a new repo today.** You already own `fx-exchange` from yesterday. Pick up
exactly where `w2d3-done` left off:

```bash
docker --version                # Docker Desktop up? The integration test needs it.
cd fx-exchange                  # your own repo from Day 3
git switch main && git pull
git log --oneline -1            # you should be on (or just after) tag w2d3-done
cd fx-app-spring && ./mvnw -q clean package -DskipTests && cd ..   # yesterday's app still builds
```

> **`fx-exchange/` in this folder is a parachute, not your workspace.** It is a copy of the
> `w2d3-done` end state — three sibling apps, the compose file, Liquibase-owned `fxdb`. Use it only
> if your own repo is broken or missing the tag: recover from **your** `git checkout w2d3-done`
> first, and copy from here only as a last resort.

**All of today's Java work is in `fx-app-spring/`** — the API, one app inside the workspace. The
tests live there; the pipelines that run them live at the **repo root** (`.github/workflows/`),
because that is the only place GitHub Actions looks.

**One branch per exercise**, each merged to `main` through a reviewed pull request:

| Task | Branch |
|---|---|
| 1 | `exercise-01` |
| 2 | `exercise-02` |
| 3 | `exercise-03` |
| 4 | `exercise-04` |

Nobody commits straight to `main`; today that habit gets *enforced* by branch protection, not just
encouraged. Task 1 step 0 walks the setup.

**Two terminals again:** one for `mvn`/`git`, one to watch `docker` when the integration test and
the CD build boot containers.

---

## Today's tasks

Work them in order.

| # | Task | When |
|---|---|---|
| 1 | [The three altitudes](challenge/01-three-altitudes.md) | AM, ~90 min |
| 2 | [The machine runs the tests (CI)](challenge/02-ci-pipeline.md) | AM/PM, ~45 min |
| 3 | [Supply chain & the Definition of Done](challenge/03-supply-chain-and-dod.md) | PM, ~45 min |
| 4 | [From green build to blessed deploy (CD)](challenge/04-cd-pipeline.md) | PM, ~45 min |
| closer | [GenAI closer — build the habit that saves you](challenge/05-genai-closer.md) | ~10 min |

Task 1 is the dense one — the three altitudes take the whole first morning block. Budget accordingly.

---

## End-of-day: are you done?

You should be able to say yes to all of these:

- [ ] Three test altitudes live in `com.fx.api`: a unit test (mocked), a `@WebMvcTest` slice, and a
      Testcontainers `*IT`. From `fx-app-spring/`, `./mvnw test` is **29** green (1 pre-existing
      skip) and `./mvnw verify` adds the **3** integration tests.
- [ ] The integration test boots a **real MySQL** and gets its schema + data from **Liquibase on
      startup** — no `.sql` file mounted anywhere. `findLatest()` returns **10**; EUR/USD = **1.0818**.
- [ ] You proved the silent-skip: with the wrong `testcontainers.version`, `./mvnw verify` is a
      green BUILD SUCCESS that **ran zero database tests** — and you know `1.21.4` is the fix.
- [ ] `.github/workflows/ci.yml` sits at the **repo root**, uses `working-directory: fx-app-spring`,
      and a **red PR is physically blocked** from merging by branch protection.
- [ ] `mvn dependency:tree` read; one transitive dependency traced to why it's there; Dependabot
      enabled on the repo.
- [ ] `docs/day4-status.md` written, with the **5-point Definition of Done** (point 1 includes both
      `./mvnw verify` green **and** `docker compose up` serving 10 rates).
- [ ] CD works: `ci.yml` is narrowed to `pull_request`, `cd.yml` runs on push to `main`, and
      Actions shows **build → docker → (approved) deploy**. No job runs the build twice.
- [ ] ghcr.io shows **two tags** on the `fx-app-spring` package: `sha-<short>` and `latest`.
- [ ] The `staging` deploy **paused for a human** and only ran after approval — you can say why
      that pause is the line between continuous *delivery* and continuous *deployment*.
- [ ] `docs/genai-cicd-review.md` committed.
- [ ] `main` is green and current; tag **`w2d4-done`** pushed from it.

Tomorrow: the dashboard — HTML/CSS/JS in front of your API, and the full loop closes.
**Do not delete anything you built today.** The tests, the pipelines and the image are all
tomorrow's starting material.
