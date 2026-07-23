# Task 4 — From green build to blessed deploy (CD) (PM, ~45 min)

*[← back to TODO.md](../TODO.md) · [diagrams](architecture.html) · [overview & cheatsheet](index.html) · [answers](../solution/) — struggle first*

### Why we're doing this

CI proves the code isn't broken. **CD** — continuous *delivery* — takes the thing you already
built and moves it toward users. The jump is small in code and enormous in mindset. You are about
to push an image to a registry the world can pull from, and — the part that matters — put a
**human gate** in front of the deploy. Automation ships the artifact; a named person answers for
it going live. That pause is the entire difference between continuous *delivery* (an image is
ready) and continuous *deployment* (it's live), and it's why sensible teams sleep at night.

Note what you are *not* doing: building anything new. The image already exists — you wrote its
Dockerfile on Day 3 and it already runs under compose. Today's CD is **promotion**: verify once,
publish with two tags, wait for a blessing. You'll also fix a subtle waste in your own pipeline —
right now, merging a PR runs the whole build *twice*.

**Skills you're building**
- Splitting CI and CD by trigger so nothing builds twice
- Job chaining with `needs:` — a red build never produces an image
- Registry auth: `docker/login-action` with `GITHUB_TOKEN` and `permissions: packages: write`
- Immutable vs movable tags — `sha-<short>` and `latest` on one build
- GitHub **environments** as an approval gate (`environment: staging`, required reviewer)

### What you're producing

`ci.yml` narrowed to run **only on pull requests**, a new `.github/workflows/cd.yml` that runs on
every push to `main` with three chained jobs (`build` → `docker` → `deploy`), a `staging`
environment with you as required reviewer, two tags on GHCR, and one run in the Actions tab that
visibly pauses at the gate until you approve it.

---

### Step-by-step

**0. Branch first.**

```bash
cd fx-exchange
git switch main && git pull
git switch -c exercise-04
```

**1. Stop building twice.** Right now `ci.yml` triggers on `push: [main]` *and* `pull_request`.
Once CD exists, every merge to `main` will trigger CD's build too — so `main` would build twice on
every merge. Give each pipeline one job: **CI gates the PR, CD ships `main`.** Narrow the top of
`.github/workflows/ci.yml` to pull requests only:

```yaml
name: CI
on:
  pull_request:
```

Delete the `push: branches: [main]` lines. Leave the rest of `ci.yml` — the three jobs, the
`defaults`, the caches — exactly as they are.

**2. Create `.github/workflows/cd.yml`.** Same root folder, same workspace nuance as CI
(`working-directory: fx-app-spring`), plus one thing CI never needed — permission to publish a
package:

```yaml
name: CD
on:
  push:
    branches: [main]

# GITHUB_TOKEN is read-only for contents by default; publishing a package needs this.
permissions:
  contents: read
  packages: write

defaults:
  run:
    working-directory: fx-app-spring
```

**3. Job 1 — `build`.** Nothing ships from a red build, so verify first, exactly as CI does:

```yaml
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
          cache-dependency-path: fx-app-spring/pom.xml
      - run: mvn -B verify
```

**4. Job 2 — `docker`.** `needs: build`, so a red verify never reaches it. Log in to GHCR with
the token GitHub already gives every workflow, then build and push:

```yaml
  docker:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - name: Build and push
        run: |
          SHORT=${GITHUB_SHA::7}
          IMAGE=ghcr.io/${{ github.repository_owner }}/fx-app-spring
          docker build -t $IMAGE:sha-$SHORT -t $IMAGE:latest .
          docker push $IMAGE:sha-$SHORT
          docker push $IMAGE:latest
```

Two tags on **one** build. `sha-<short>` is **immutable** — it pins exactly this commit, forever,
so a rollback is unambiguous. `latest` is a **movable pointer** — convenient for "whatever's
newest". That's release management without a release-management product. And because
`working-directory` is `fx-app-spring`, the `.` build context is your API folder — the exact
place the Dockerfile and `.dockerignore` already assume.

> **GHCR names must be lowercase.** The image path is
> `ghcr.io/<owner>/fx-app-spring`, and `<owner>` comes from `github.repository_owner`. If your
> GitHub username or org has capital letters, the `docker push` fails with a `400` — Docker
> registries reject uppercase in a repository name. If that bites you, lowercase the owner
> segment (e.g. hard-code the lowercased name) and push again.

**5. Job 3 — `deploy`.** `needs: docker`, and the gate itself: `environment: staging`. The step
is a stand-in — a real deploy would SSH to a host and `docker compose pull && up -d`, or run
`kubectl set image`, or hit a platform webhook — but the *gate* is completely real:

```yaml
  deploy:
    needs: docker
    runs-on: ubuntu-latest
    environment: staging
    steps:
      - uses: actions/checkout@v4
      - name: Deploy (stand-in)
        run: echo "Would pull ghcr.io/${{ github.repository_owner }}/fx-app-spring:latest on the staging host"
```

**6. Configure the gate.** In the repo: **Settings → Environments → New environment → `staging`**.
Add **yourself as a required reviewer**. Save. The workflow only *names* `staging`; the reviewer
requirement lives in the environment, and that's what makes the job pause.

**7. Push to `main` and watch it stop.** Merge your `exercise-04` PR (the one narrowing `ci.yml`
and adding `cd.yml`). On the push to `main`, open Actions: `build` runs, `docker` builds and
pushes, then `deploy` sits in **Waiting** with a "Review pending deployments" prompt. **That pause
is the whole point of the day.**

**8. Approve it, then verify the artifacts.** Approve the deployment and let the echo run. Then
open the repo's **Packages** and confirm the `fx-app-spring` package shows **both** tags —
`sha-<short>` and `latest`.

**Checkpoint.** Merging a PR now runs **CI only** (no more double build); a push to `main` runs
**CD** — `build → docker → (paused) deploy` — and the run halts at the `staging` gate until you
approve. GHCR shows the `fx-app-spring` package with two tags, `sha-<short>` and `latest`.

**9. Ship it.** This task's own work — the narrowed `ci.yml` and the new `cd.yml` — merged as the
`exercise-04` PR in step 7. Confirm `main` is clean:

```bash
git switch main
git pull
```

> Split by trigger, chained by `needs:`, gated by an environment. Three small ideas, and together
> they are the difference between "the build is green" and "a human decided this goes live."

### Sharing the image with your team

You pushed to *your* namespace — `ghcr.io/<owner>/fx-app-spring`. Another person — a teammate, or
another machine — needs to pull it. Two ways:

- **Public (simplest).** In **Packages → fx-app-spring → Package settings → Change visibility →
  Public**. Now anyone pulls it with **no login and no token**:
  ```bash
  docker pull ghcr.io/<owner>/fx-app-spring:latest
  ```
  and a teammate's `docker-compose.yml` can name it directly:
  `image: ghcr.io/<owner>/fx-app-spring:latest` (no `build:` — they don't need your source).
- **Private.** Leave it private; because the package is **linked to this repo**, anyone you add as a
  **repo collaborator** inherits read access. Each teammate runs `docker login ghcr.io` once with
  their own token (`read:packages`), then pulls the same way.

> The image isn't a secret, so in a class or shared setup, **public** is the friction-free choice. In a real
> company you'd instead own the repo under a shared **org**, and the image would be
> `ghcr.io/<org>/fx-app-spring` — team membership, not per-person grants, controls the pull.

### One image, every environment — profiles & injected secrets

The image you just pushed is **built once**. It should run in dev, staging and prod **without
rebuilding** — only its *config* changes. Spring does this with **profiles**.

`application.properties` is your dev default (localhost, `appuser`/`apppass`). Add a production
overlay — it overrides *only* what differs:

```properties
# src/main/resources/application-prod.properties
spring.datasource.url=jdbc:mysql://${DB_HOST:prod-db}:3306/fxdb
spring.datasource.username=${DB_USER:fxprod}
spring.datasource.password=${DB_PASSWORD}
```

The **shape** (host, user, database) lives in the file; the **password does not** — it's
`${DB_PASSWORD}`, supplied by the environment at deploy time. A real password here would ship inside
the image *and* the git history — the exact leak the supply-chain task hunts for. **Profiles for
structure, env for secrets.**

Pick the environment **at run time**, on the *same image* — nothing is rebuilt:

```bash
# dev — the default, no profile needed
docker run -p 8080:8080 ghcr.io/<owner>/fx-app-spring:latest

# prod — same image, different config, secret injected from outside
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=db.internal -e DB_PASSWORD='••••' \
  ghcr.io/<owner>/fx-app-spring:latest
```

**Precedence, so nothing surprises you:** an env var (`SPRING_DATASOURCE_PASSWORD`) beats
`application-prod.properties` beats `application.properties`. That's *why* your compose file's
`SPRING_DATASOURCE_*` values already win over the file — same rule, you've been using it since Day 2.

**Try it now — two instances, side by side.** A profile is just a property bundle, and one jar can
run as many times as you have ports. From `fx-app-spring/`, in two terminals:

```bash
mvn -q -DskipTests package
java -jar target/fx-app-spring-*.jar --server.port=8080     # instance A
java -jar target/fx-app-spring-*.jar --server.port=8081     # instance B — the SAME jar
curl -s localhost:8080/api/rates | head -c 80; echo
curl -s localhost:8081/api/rates | head -c 80; echo         # both serve, from one artifact
```

*(Both use your local DB from `application.properties`; if yours is the compose MySQL on 3307, add
`--spring.datasource.url=jdbc:mysql://localhost:3307/fxdb`.)* Bundle a port + datasource into
`application-staging.properties` and pick it with `--spring.profiles.active=staging` — the named
version of the same thing.

The password is real, not decoration — start one with the wrong one and watch it refuse to boot:

```bash
java -jar target/fx-app-spring-*.jar --server.port=8082 --spring.datasource.password=WRONGPASS
# → Access denied for user 'appuser' ... Application run failed
```

Want two genuinely different *credentials*? Make a second DB user once, then point an instance at it:

```sql
CREATE USER 'fxstaging'@'%' IDENTIFIED BY 'stagingpass';
GRANT ALL PRIVILEGES ON fxdb.* TO 'fxstaging'@'%';
```

Run one instance with `--spring.datasource.username=fxstaging --spring.datasource.password=stagingpass`
— same jar, different identity, both connect. (A *read-only* `GRANT SELECT` user won't work here:
Liquibase needs write on startup to manage `DATABASECHANGELOG` — a fair reminder that the app's own
migrations need privileges too, not just your queries.)

**Checkpoint (verified).** Boot the *same jar/image* two ways and read the first log lines: no
profile → `No active profile set, falling back to 1 default profile: "default"` (the dev
datasource); `SPRING_PROFILES_ACTIVE=prod` → `The following 1 profile is active: "prod"`, and it now
aims at the **prod** datasource — off-network that fails fast (~2s) with `Communications link
failure` / `UnknownHostException: prod-db`. Same artifact; one env var changed the target. The
password lives in neither the file nor the image — it arrives as `DB_PASSWORD` at deploy.

<details>
<summary>Stuck?</summary>

**`docker push` returns `403`.** `GITHUB_TOKEN` logged in but can't publish — you're missing
`permissions: packages: write` at the top of `cd.yml`.

**`docker push` returns `400 / invalid reference format`.** Uppercase in the owner segment. GHCR
repository names must be lowercase; lowercase `<owner>` and retry.

**The `deploy` job runs without pausing.** The `staging` environment has no required reviewer, or
you never created it. It's a repo-Settings thing, not a workflow-file one — the YAML only says
`environment: staging`.

**Every merge still builds twice.** You added `cd.yml` but didn't narrow `ci.yml` — it still has
`push: [main]`. Remove those lines.

**`docker` runs even though `build` failed.** `needs: build` is missing from the `docker` job.

**A teammate's `docker pull` returns `denied` / `not found`.** The package is still private and they
aren't a collaborator — make it public (Package settings), or add them to the repo.

**App won't start under `prod`: `Communications link failure` / `UnknownHostException: prod-db`.**
Expected off-network — the prod profile points at the *production* datasource, not localhost. Set
`DB_HOST`/`DB_USER`/`DB_PASSWORD` to a reachable database, or simply don't activate `prod` locally.
</details>
