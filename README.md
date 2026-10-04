# Academic System Environment

A unified Java application: the Java runtime serves the web frontend and its API from the same process.

## Run
Open PowerShell in this folder and run:

```powershell
.un.ps1
```

Then open the URL printed by the application (normally `http://localhost:8080`).

## Architecture

Browser UI -> Java HttpServer -> application/domain services -> EventBus -> persistent embedded HSQLDB -> audit/events -> SSE -> browser.

The Live Codespace reads the actual `src/` directory on disk. The Live Database reads the persistent `data/academic` database. Student actions are real API calls rather than simulated frontend animations.

AI Development is intentionally controlled: observe -> propose -> compile/test -> approval -> apply. The current build action compiles the actual source tree.
