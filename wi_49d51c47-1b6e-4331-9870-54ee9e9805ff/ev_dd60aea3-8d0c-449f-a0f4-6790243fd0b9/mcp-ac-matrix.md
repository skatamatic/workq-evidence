# Hosted no-repo MCP acceptance (independent checks only)

Campaign tip: 03081308ef on WC20-7375-Productize-Create-MCP (includes #4397-#4403).
Client package version on branch: 2.50.10.
Generated worklink-api.json locally (379957 bytes, gitignored). write-server-tools failed: common/lib missing in this worktree (did not npm install into linked node_modules).

## Served environment

- Create Dev1 RELEASE_VERSION=2a0925d.develop.4221 (develop, not campaign tip)
- AI websocket: wss://worklink-ai-dev1.scopeartest.com
- MCP URL: https://worklink-ai-dev1.scopeartest.com/mcp — live (invalid Bearer → 401 api_key_invalid)
- Picker: GET /mcp/picker → 200
- MCP health: GET /mcp/health → 404 (Express /health is on the MCP app root, not under /mcp)
- Public /health: 200 empty body (ai-server, unauthenticated)
- stg1 /mcp: 404
- Local :3020: connection refused
- Codex CLI: not installed
- Cursor.app / ChatGPT.app: present on this Mac
- CMS MCP keys: none in worker env

Did not run Codex/Cursor attach, authoring, or key lifecycle against Dev1 develop. Ticket forbids treating helper tests or the wrong SHA as pass.

## AC

- Codex hosted no-clone connect: blocked (no CMS key; Dev1 not campaign SHA)
- Picker attach eligible tabs: blocked
- [MCP] title + MCP Connected: blocked
- Read / reversible edit / restore / disconnect: blocked
- Key not in chat/URLs/history/VCS/logs: live transcript checks blocked (VCS/docs already honest on branch)
- Two clients / two users / rotate / revoke / invalid / no sessions: blocked
- Image evidence of browser + redacted clients: blocked
- Confluence publish: captain handoff (dry-run 404s CT root)
