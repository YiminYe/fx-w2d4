# fx-monitor — given to you complete

A read-only live view of **your** `fx-app-spring` API. Plain HTML/CSS/JS served by nginx.
**You do not write any code in this folder** — you containerise it and wire it into compose
(Activity 4), and it becomes your window onto the feed in Activity 5.

> This is not `fx-dashboard`. On Friday you build that one yourself, by hand, and it is the
> real user-facing app. `fx-monitor` is an ops view someone else already wrote — exactly the
> situation you meet on a real team, where most of the containers you run are not yours.

## What it does

| It calls | When | Exists since |
|---|---|---|
| `GET /health` | every second | W2D2 |
| `GET /api/rates` | every second — draws the table, flashes changed values | W2D2 |
| `GET /api/admin/accepting` | every 2s — drives the toggle | **you build it, Activity 5** |
| `POST /api/admin/accepting` | when you click the toggle | **you build it, Activity 5** |

Until Activity 5 the two admin calls return 404. That is expected: the toggle stays greyed out
and a banner says so. The rates table works from Activity 4 onwards regardless.

## Why nginx, and why there's no CORS here

The page never calls `http://localhost:8080` directly. nginx serves the page **and** proxies
`/api` and `/health` onward to `fx-app-spring:8080` inside the compose network, so as far as the
browser is concerned there is exactly one origin. No CORS headers, no preflight, nothing to
configure.

That proxy target is a **compose service name**, not an IP and not `localhost` — see
`nginx.conf.template`. Inside a container `localhost` is the container itself, which is the same
lesson the API learned the hard way in Activity 1.

## What you see per pair

| Column | Meaning |
|---|---|
| **Rate** | the latest value. On a change **the number itself** lights up green or red and fades back — nothing else moves: no background, no weight, no reflow. |
| **Last change** | the most recent move, **and it stays there**. It is a status, not a flash: between updates it keeps showing the last one rather than blanking out. |
| **Trend** | a live line chart of roughly the last 90 seconds this page has seen, scaled to that pair's own min/max (so 0.66 and 147.6 both read clearly). |
| **As of** | `capturedAt` once you build it in Activity 5; `rateDate` before that. |

The trend history lives in the **browser**, not the database — reload the page and it starts
again from empty. That is deliberate: it keeps `fx-monitor` a pure read-only view with no state
of its own.

## The Feed panel

| Stat | What it means |
|---|---|
| **Updates received** | polls in which the numbers actually moved — *not* the poll count |
| **Feed interval** | gap between the last two batches that landed, taken from the server's own `captured_at`. ~**2.0 s** while ACCEPTING is ON. |
| **Last update** | climbs, and goes amber past 5s. This is what shows the feed being declined. |

The page polls once a second, but the orchestrator only pushes every ~2s, so **about half of all
polls find nothing new** — which is why the raw poll count is demoted to a footnote.

**Switch ACCEPTING off and the interval does *not* rise to 10s.** A declined batch is never
stored, so it leaves no trace in the data at all: updates simply stop, and *Last update* climbs.
The 10-second backoff is real but only visible in `docker compose logs -f fx-orchestrator`.

### Why it doesn't flicker

Rows are created **once** and then mutated in place: text is written to a persistent text node,
the sparkline only has its `points` attribute re-pointed, and the flash is a CSS class toggled on
and off. Nothing in the table is ever destroyed and rebuilt. (Measured: across nine polls with
live data, zero DOM nodes added or removed.)

The naive version of this page rebuilt `<tbody>.innerHTML` every second, which throws away and
re-creates every cell — that is what made it strobe.

## Files

```
fx-monitor/
├── index.html          the page
├── styles.css          dark, projector-legible
├── app.js              polling + rendering (read the comments at the top)
├── nginx.conf.template rendered by envsubst at container start; ${API_HOST} is injected
├── Dockerfile          5 lines — no build stage, because there is nothing to build
└── .dockerignore
```

## Running it

Inside compose (the normal way — Activity 4 wires this up):

```bash
docker compose up --build fx-monitor
# then open http://localhost:3000
```

On its own, pointing at an API running elsewhere:

```bash
docker build -t fx-monitor .
docker run -p 3000:80 -e API_HOST=host.docker.internal fx-monitor
```
