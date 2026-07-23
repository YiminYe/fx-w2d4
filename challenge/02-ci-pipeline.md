# Task 2 — The machine runs the tests (CI) (AM/PM, ~45 min)

*[← back to TODO.md](../TODO.md) · [diagrams](architecture.html) · [overview & cheatsheet](index.html) · [answers](../solution/) — struggle first*

### Why we're doing this

You have three altitudes of tests. They protect you exactly as often as you remember to run
them — which, honestly, is "before the interesting commits and not the boring ones". That gap
is where broken code reaches `main`. A CI pipeline closes it: no human runs the full suite
before every push, but a robot does, cheerfully, forever.

The moment you push, GitHub Actions receives the event, spins up a fresh Ubuntu VM, checks out
your code, installs Java, runs `mvn verify` — all three altitudes, including the Testcontainers
integration test, because `ubuntu-latest` ships a running Docker daemon — and reports green or
red back to the pull request. The value isn't the green. **The value is the red.** A pipeline
that has never gone red has never caught anything, so today you'll make it go red on purpose and
watch it stop a bad merge.

**Skills you're building**
- YAML workflow structure: `on:`, `jobs:`, `steps:`, `uses:` vs `run:`
- Pointing a workflow at a subfolder of a workspace with `defaults.run.working-directory`
- `actions/checkout@v4` + `actions/setup-java@v4` with a Maven cache keyed on the right pom
- Reading the Actions tab: the job graph, step logs, the first `[ERROR]` line
- Turning a deliberate failure into branch protection nobody can click past

### What you're producing

A `.github/workflows/ci.yml` at the **repo root** with two parallel jobs (`build` and `lint`);
one Actions run that goes red because you asked it to; and a branch-protection rule that greys
out the merge button until `build` is green.

---

### Step-by-step

**0. Branch first.** Clean `main`, new branch:

```bash
cd fx-exchange
git switch main && git pull
git switch -c exercise-02
```

**1. Create the workflow — and mind where it lives.** GitHub Actions only reads workflows from
the **repo root**, never from a subfolder. But your Maven project isn't at the root — it's in
`fx-app-spring/`. So the file goes at the root and *tells every job* to `cd` into the API first:

Create `.github/workflows/ci.yml` at the root of `fx-exchange` (beside `docker-compose.yml`,
**not** inside `fx-app-spring/`):

```yaml
name: CI
on:
  push:
    branches: [main]
  pull_request:

# The repo is the fx-exchange WORKSPACE; the API is one app inside it. Every `run:` step
# happens in fx-app-spring/ (where the pom is). `uses:` steps see the repo root, unaffected.
defaults:
  run:
    working-directory: fx-app-spring

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
          cache-dependency-path: fx-app-spring/pom.xml   # key the cache on OUR pom, not the root
      - name: Build and test (unit + slice + Testcontainers IT)
        run: mvn -B verify
```

Two lines carry the whole workspace nuance:

- `defaults.run.working-directory: fx-app-spring` — so `mvn` runs where your `pom.xml` is. Leave
  it out and the build fails with "no POM in this directory", because the root has none.
- `cache-dependency-path: fx-app-spring/pom.xml` — `setup-java`'s Maven cache needs to know which
  file's dependencies to key on. Point it at the root (there's no pom there) and caching silently
  does nothing.

And note the verb: `verify`, not `test`. That one word is what makes CI run **all three
altitudes**, the integration test included. `ubuntu-latest` has Docker, so the container boots
with zero extra wiring.

**2. Push it and watch the first run.**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: verify on every push and PR"
git push -u origin exercise-02
```

Open a PR from `exercise-02` into `main`, then open the **Actions** tab. The run should go green
because your suite is green. Click into the `build` job, expand *Build and test*, and confirm the
Maven output matches what you see locally — **including the Testcontainers lines pulling and
starting `mysql:8`**. Your IT runs in CI for free.

> **There's another way, and you should be able to say why you didn't use it.** GitHub Actions
> can start a MySQL *service container* alongside the job (a `services:` block). Both work. We use
> Testcontainers because the **test owns its database**, so the *same* `mvn verify` behaves
> identically on your laptop and on CI — one source of truth, instead of a CI-only MySQL block you
> have to keep in sync with your local setup.

**3. Add the parallel `lint` job.** Below `build:`, a second job:

```yaml
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
          cache-dependency-path: fx-app-spring/pom.xml
      - name: Validate
        run: mvn -B validate
```

Push. Refresh Actions and you'll see **two boxes side by side** in the job graph — jobs run in
parallel by default. (If one job needed another to finish first, it would say `needs:`. Neither
does, so they race, and your PR is ready sooner.)

**4. Now earn the pipeline's keep — go red on purpose.** From a scratch branch, break one
assertion in the **api** unit test (the one you wrote this morning, `com.fx.api.ConversionServiceTest`
— not the older `com.fx.core` one):

```bash
git switch -c break-a-test
```

Change the converted-amount assertion so it *must* fail — expect `999.99` instead of `133.55`:

```java
assertEquals(999.99, result.converted(), 1e-9);   // deliberately wrong
```

Push, open a PR. Within a minute a red X lands on it. Click through: expand the failing step,
find the first `[ERROR]` line, and confirm it names the wrong-assertion test with
`expected: <999.99> but was: <133.55>`. **That is the safety net catching a fault you invented.**

**5. Make red mean "you cannot merge".** A red check you can click past is decoration. In the repo:
**Settings → Branches → Add branch ruleset (or rule) for `main`** → require a pull request, and
require the status check **`build`** to pass before merging. Save.

Now go back to the red PR: the **Merge** button is greyed out. "Please don't merge red" just
became physics, not a plea. Fix the assertion back to `133.55`, push, watch the check flip green,
and confirm Merge lights up. You don't have to merge this throwaway branch — delete it — but you
have proved the gate holds.

**Checkpoint.** Your repo's Actions tab shows a **red run followed by a green run**. Branch
protection on `main` requires `build`, and a red PR cannot be merged. Your real `exercise-02`
PR — the one that adds `ci.yml` — is green and mergeable.

**6. Ship it.** Merge the real `exercise-02` PR (not the throwaway) into `main`, then:

```bash
git switch main
git pull
```

> `-B` is `--batch-mode`: no interactive progress bars, so CI logs read cleanly. `cache: maven`
> restores `~/.m2` between runs, turning a three-minute dependency download into a forty-second
> cache hit. Two flags, both earning their place — be able to say what each does in the PR body.

<details>
<summary>Stuck?</summary>

**"No POM in this directory" / can't find `pom.xml`.** The workflow is running `mvn` at the repo
root. Add `defaults: run: working-directory: fx-app-spring`, or the workflow file is inside
`fx-app-spring/` instead of at the root — it must be at `fx-exchange/.github/workflows/`.

**The cache never hits.** `cache-dependency-path` is missing or points at a non-existent root
`pom.xml`. It must be `fx-app-spring/pom.xml`.

**"Java not found."** `setup-java` must run *before* the `mvn` step — order matters in `steps:`.

**The red PR still shows a green "Merge" button.** The branch-protection rule didn't save, the
required check name doesn't match the job (`build`), or you added the rule after opening the PR —
push a new commit to re-evaluate.

**IT is skipped in CI.** Unlikely — `ubuntu-latest` has Docker — but check the `verify` log for
`Skipped: 3`. If so, your `testcontainers.version` pin from Task 1 didn't get committed.
</details>
