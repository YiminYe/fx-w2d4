# FX exchange — Week 2 Day 3

**Name:** _<your name here>_

Three applications that run side by side as one stack.

| Folder | What it is | Built by |
|---|---|---|
| `fx-app-spring/` | the API and its database — my project | me |
| `fx-monitor/` | live web view of the rates | given |
| `fx-orchestrator/` | upstream rate feed | given |

Each folder owns its own `Dockerfile`. `docker-compose.yml` at this root wires them together.

```bash
docker compose up --build -d
docker compose ps
```

Then open <http://localhost:3000>.
