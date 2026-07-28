# Nested Task capability under headless cursor-agent

Date: 2026-07-28
cursor-agent: 2026.07.23-e383d2b
Flags: -p --output-format stream-json --force --trust --model composer-2.5-fast

## Result: nesting WORKS to at least depth 3 (4 agent levels)

Probe 1 (depth 2 Tasks / 3 agents): root → Task → Task wrote marker NEST_OK_DEPTH2.
Transcripts under ~/.cursor/projects/tmp-workq-nest-probe-97923/agent-transcripts/:
- root 78f18bff… (stream Task → mid)
- mid fd62a750… (chat tool_use Task → grandchild)
- grandchild 2a122e68… (Write marker)

Probe 2 (requested depth 3 Tasks / 4 agents): marker NEST_OK_DEPTH3 written; 4 transcript dirs.
Root and depth-1 used Task; one mid level used Shell `agent --print` instead of Task (agent cheating), so depth-3 Task purity is softer — but Task→Task nesting itself is confirmed by probe 1, and four concurrent agent sessions completed.

## Ceiling / flags
- cursor-agent --help shows no subagent-depth / max-nesting flag.
- Pool already passes --force / --trust (apps/pool/src/cursor/cli.ts).

## Harvest implication
Parent stream-json emits taskToolCall with args.agentId / result.success.agentId.
Child chat JSONL emits tool_use name=Task WITHOUT agentId (and without tool_result).
Recursive harvest must parse chat Task tool_use and resolve the child disk id by matching Task prompt to another transcript's user_query (verified on probe 1).
