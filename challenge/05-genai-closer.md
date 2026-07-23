# GenAI closer (~10 min) — build the habit that saves you

*[← back to TODO.md](../TODO.md) · [diagrams](architecture.html) · [overview & cheatsheet](index.html)*

Every day this fortnight ends the same way: generate something, then **verify it before you
believe it**. Pipelines are unusually good at punishing the unverified answer, because a workflow
that's valid YAML and runs green can still be quietly wrong — building twice, skipping your best
test, or "optimising" away the exact thing that protects you. Today you have the one instrument
that settles it: a suite and two pipelines you can actually run.

**1. Ask for a review of your own workflows.** Paste **only** your `ci.yml` and `cd.yml` — not the
pom, not your source — into your assistant with exactly this:

> Here are my CI and CD workflows: `<paste>`. Review them for correctness, speed, and cost.

**2. Read what comes back, and price every suggestion.** They're all real; none is free:

| Suggestion | What it actually costs you |
|---|---|
| Add a Java version matrix (`21, 22`) | Every push now runs the whole suite N times — CI minutes and wall-clock, to test a JDK you don't ship on. |
| Upload test reports as artifacts | Handy on a failure; storage and a slower job on every green run you'll never open. |
| Cache the Docker layers in CI | Real speed-up — and a new class of "stale cache" bug that's miserable to debug on a Friday. |
| Split each test tier into its own job | Prettier graph; the Testcontainers image now gets pulled in several jobs instead of one. |
| Add a nightly schedule | Correct home for the OWASP scan — but it's a *third* pipeline to keep in sync with two you already have. |
| Combine steps to "simplify" | Fewer lines, worse logs — a failure now points at a block, not a line. |

**3. Adopt exactly one, and be able to defend the ones you rejected.** Pick the single suggestion
whose value clearly beats its cost for a four-person capstone. Apply it, push, and confirm the PR
still goes green and `main` still ships. If it broke something, you just learned more from the
adoption than you would have from the acceptance.

**4. Now the harder ask — where the AI is most likely to be confidently wrong.** In a **fresh**
conversation:

> My CI takes too long. How do I make it faster?

You will very likely be offered `-DskipTests`, `-DskipITs`, splitting the IT out to "run less
often", or dropping to `mvn test` instead of `verify`. **You already know what every one of those
costs**: it's the Docker-off silent skip from Task 3, except now you'd be *choosing* it. Faster CI
that no longer runs your only real-database test isn't faster — it's blind. Score the answer
honestly:

- Did it suggest skipping or demoting the integration test to save time?
- Did it distinguish "the suite is slow" from "the suite is thorough", or treat every test as pure cost?
- Would its advice have survived your own `Skipped: 3 … BUILD SUCCESS` moment this afternoon?

**5. Write it down.** Four or five lines in `docs/genai-cicd-review.md`, committed with today's
work:

- the one suggestion you took, and why it beat its cost
- one you rejected, and what it would have cost you
- whether the "make CI faster" answer tried to sacrifice the IT — and how you'd have caught it

> **The habit is the deliverable.** An assistant that has never run your pipeline will hand you
> advice that is valid YAML, goes green, and still leaves you unprotected — and today you have the
> one thing that settles the argument: a `verify` you can read closely enough to know whether it
> actually ran.
