# Task 3 — Supply chain & the Definition of Done (PM, ~45 min)

*[← back to TODO.md](../TODO.md) · [diagrams](architecture.html) · [overview & cheatsheet](index.html) · [answers](../solution/) — struggle first*

### Why we're doing this

Your `pom.xml` names about half a dozen dependencies. Your built jar contains dozens. Every one
of those "transitive" dependencies is code you ship, execute with full trust, and never wrote —
written by people you'll never meet. When one of them turns out to have a remote-code-execution
bug — and one eventually will — you need to know three things fast: that you ship it, which of
*your* direct dependencies pulled it in, and whether a patched version exists. That inventory is
your software supply chain. Log4Shell in 2021 was the morning every engineering org discovered
they didn't have one.

Then the honesty half. Automation and green ticks are only worth anything if someone reads them.
This afternoon you'll wire up two safety tools **and** deliberately turn off MySQL and Docker to
watch which of your green ticks are telling the truth — because a suite that quietly needs a
database will lie to you, and a build that silently skips its best test will lie to you louder.
Naming exactly what your green means is the whole point of a **Definition of Done**, which you'll
write down today and live by for the rest of the capstone.

**Skills you're building**
- Reading `dependency:tree`: indentation is who-pulled-whom
- Looking a dependency up in the NVD / GitHub advisories by artifact + version
- Turning on Dependabot and adding a tolerated-failure OWASP scan to CI
- The two honesty checks — MySQL off (the fast tier) and Docker off (the silent skip)
- Writing a five-point Definition of Done and running your own PR through it

### What you're producing

`docs/day4-status.md` — a short status doc that names what's tested at which altitude, what CI
guards and doesn't, your Dependabot rule, and the **five-point Definition of Done**. Plus a
`dependency-check` job appended to `ci.yml`, Dependabot enabled, and a demonstrated honesty check.

---

### Step-by-step

**0. Branch first.**

```bash
cd fx-exchange
git switch main && git pull
git switch -c exercise-03
cd fx-app-spring
```

**1. Inventory what you ship.**

```bash
./mvnw dependency:tree
```

Skim it. Count the dependencies — every entry, direct and indented. That number is your attack
surface: it's what actually ends up in the jar. Note it down; it goes in the status doc.

**2. Trace two strangers.** Pick two dependencies you've never heard of. In the tree, an
*indented* line is a transitive dependency and the line one level *less* indented is what pulled
it in. Write both down as "X → pulled in by Y". (Your validation starter alone drags in a
Jakarta EL implementation you never asked for — that's a fine one to trace.)

**3. Look one up.** Take one dependency and its exact version from the tree and search
[the NVD](https://nvd.nist.gov/vuln/search) or [GitHub Advisories](https://github.com/advisories)
for that artifact + version. Note any advisories. **"No known vulnerabilities" is a valid
result** — write it down; the skill is doing the lookup, not finding a scandal.

**4. Turn on Dependabot.** Repo → **Settings → Code security** → enable **Dependabot alerts** and
**Dependabot security updates**. From now on, when a patched version of anything you ship lands,
GitHub opens a PR for it automatically. (If corporate GitHub policy blocks Dependabot, note that
in the status doc and keep the manual lookup from step 3 — the *rule* is what matters, not the
button.)

**5. Add a tolerated-failure scan to CI.** Append a third job to the `ci.yml` you wrote in Task 2
— it already has `build` and `lint`, so this sits beside them:

```yaml
  dependency-check:
    runs-on: ubuntu-latest
    continue-on-error: true
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
          cache-dependency-path: fx-app-spring/pom.xml
      - name: OWASP dependency check
        run: mvn -B org.owasp:dependency-check-maven:check
```

`continue-on-error: true` means a slow scan **surfaces** its report without blocking every PR.
In a real project you'd move this to a nightly schedule against `main` — it's the kind of thing
that should run while everyone's asleep, not in the way of a one-line fix.

**6. Now the honesty checks — turn things off and read the tea leaves.** Two of them, and they
teach opposite lessons.

**6a. MySQL off — is the fast tier honest?** Stop your local MySQL (`brew services stop mysql`,
or however you started it). Leave Docker running. Then:

```bash
./mvnw test
```

Every test should still be green — `29 run, 0 failures, 1 skipped`, exactly as in Task 1. Units
and slices mock everything below them, so your local database being down is *irrelevant* to them.
If one goes red with a "Communications link failure", you've found a test that secretly reaches a
real database it shouldn't: mock the missing collaborator, or — if it genuinely needs a database
— rename it `*IT` so it belongs to failsafe, not surefire.

**6b. Docker off — the silent skip, for real this time.** Now quit Docker Desktop entirely and
run the *full* command:

```bash
./mvnw verify
```

Read it carefully:

```
Tests run: 3, Failures: 0, Errors: 0, Skipped: 3 -- in com.fx.api.repo.RateRepositoryIT
...
BUILD SUCCESS
```

**BUILD SUCCESS — with your best test skipped.** This is the Task-1 trap sprung in the wild:
`disabledWithoutDocker = true` skips the IT when Docker is gone, and the build stays green over
zero database coverage. Nobody warns you. The only defence is a human who reads the summary line
— which is precisely why the Definition of Done you're about to write says *"nothing wrongly
skipped"* in point 1. Restart Docker, run `verify` again, confirm `Skipped: 0`.

> Bank the pairing: **MySQL off proves the fast tier doesn't lie about needing a database;
> Docker off proves a green `verify` can still be hiding a skipped test.** Same command, two
> failure modes, both invisible unless you look.

**7. Write `docs/day4-status.md`.** Short — three or four bullets a section. Create it with:

- **Tested at which altitude.** Unit: rounding, fees, unknown-pair throw. Slice: list, 404, valid
  POST, negative-amount → 400. Integration: real SQL on a real MySQL — `findLatest()` = 10,
  EUR/USD = 1.0818, unknown pair empty.
- **What CI guards.** `mvn verify` on every push and PR — all three altitudes, a real database
  included; branch protection blocks merging red.
- **What CI does *not* guard.** No full end-to-end run in CI: nothing there starts the app *and*
  a database *and* curls it as a user would — that's what `docker compose up` gives you locally,
  and it's in the Definition of Done below. No front end yet (Week 3).
- **Dependabot rule.** One sentence your team will keep, e.g. *"Dependabot PRs reviewed within
  one working day; security PRs merged same day if CI is green."*

Then paste the **Definition of Done** into the same file. From today it is the checklist a
reviewer runs on your PR, and it is the bar for the capstone:

> A change is **done** only when **all** of these hold:
>
> 1. From `fx-app-spring/`, **`./mvnw verify` is green** — unit, slice **and** integration, with
>    **nothing wrongly skipped** (read the summary line) — **and** `docker compose up` on a clean
>    machine serves `/api/rates` returning **10** rates.
> 2. **The new behaviour has tests at the right altitude**: a new endpoint → a slice test, happy
>    *and* failure path; a new calculation → a unit test with boundaries; new or changed SQL → an
>    `*IT`.
> 3. **A teammate reviewed the PR** — checked out the branch, ran `./mvnw verify` themselves, and
>    approved.
> 4. **Merged to `main` only through that PR** — never a direct push.
> 5. **`main` is still green after the merge**, and a **fresh clone** builds and verifies.
>
> Not done: a failing or wrongly-skipped test · "works on my laptop" but not on a fresh clone ·
> code merged without review.

**Checkpoint.** `docs/day4-status.md` holds the status **and** the five-point Definition of Done;
`ci.yml` has three jobs (`build`, `lint`, `dependency-check`); Dependabot is on; and you have seen
with your own eyes that `mvn test` stays `29 / 0 / 1` with MySQL off, while `mvn verify` reports
`Skipped: 3` — still "BUILD SUCCESS" — with Docker off.

**8. Ship it.**

```bash
git add -A
git commit -m "ci: OWASP scan; docs: day-4 status and Definition of Done"
git push -u origin exercise-03
```

Open the PR, wait for green, merge, then:

```bash
git switch main
git pull
```

> One branch per exercise, every exercise merged through a PR — and from now on, every PR judged
> against the Definition of Done you just wrote. It's your rule; hold your own work to it first.

<details>
<summary>Stuck?</summary>

**`./mvnw test` fails with "Communications link failure" (MySQL off).** A test is pulling the
real `DataSource`. A `@WebMvcTest` slice shouldn't; if it does, something in the web layer grabs
the repo directly — add `@MockBean RateRepository` to the slice. Last resort:
`spring.autoconfigure.exclude=…DataSourceAutoConfiguration` in a test properties file.

**`mvn verify` shows `Skipped: 3` and I didn't turn Docker off.** Then Docker isn't reachable, or
your `testcontainers.version` pin from Task 1 isn't in this branch. That's the whole lesson of
step 6b — a green build you didn't read.

**`dependency:tree` prints a wall of text.** That's normal — dozens of lines is the point.
Indentation is a strict hierarchy: the immediate parent is one level less indented.

**Org policy blocks Dependabot.** Some corporate GitHub Enterprise setups do. Do the manual CVE
lookup, note the block in the status doc, and write the rule anyway.
</details>
