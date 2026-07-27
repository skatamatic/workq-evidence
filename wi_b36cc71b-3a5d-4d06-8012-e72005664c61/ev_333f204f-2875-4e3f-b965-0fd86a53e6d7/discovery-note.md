# Discovery: split plan for service.ts / cli.ts / orchestrator.ts

**Item:** wi_b36cc71b-3a5d-4d06-8012-e72005664c61  
**Date:** 2026-07-27  
**Scope:** discovery + filing only — no extractions land here.  
**Base:** `origin/main` already includes WaitEngine landing/auto-merge (#60), WaitService units (#48), live API Playwright journeys (#72).

## Shared green gates (every follow-up ticket)

Do not merge a split PR unless all of these stay green:

| Gate | Command |
| --- | --- |
| Unit / integration | `npm test` (or focused files named in the ticket) |
| App service | `npx vitest run packages/app/src/service.spec.ts` |
| WaitEngine e2e | `npx vitest run apps/server/src/workers/wait-engine.e2e.spec.ts` |
| Orchestrator unit + e2e | `npx vitest run apps/pool/src/orchestrator/orchestrator.spec.ts apps/pool/src/orchestrator/orchestrator.e2e.spec.ts` |
| Live API journeys | `npm run -w apps/dashboard test:harness:live-api` |
| Types | `npm run typecheck` |

Pattern to copy: `packages/app/src/wait-service.ts` + `packages/app/src/waits/*` (lifecycle extracted; public API re-exported from `@workq/app`). Worker already has partial CLI extracts (`depends-cli`, `history-cli`, `watch`, `evidence-*`).

---

## 1. `packages/app/src/service.ts` (~2300 lines)

### Import / call-site graph

**Defined here:** `WorkQueueService`, `WorkQueueServiceDeps`, and ~25 input/DTO interfaces.

**Importers (production):**
- `apps/server/src/composition.ts` — constructs the singleton
- `apps/server/src/http/router.ts`, `server.ts` — HTTP edge
- `apps/server/src/workers/{wait-engine,lease-reaper,archive-sweeper}.ts`
- `packages/app/src/waits/{pull-request,human}.ts` — type-only `WorkQueueService`

**Tests:** `packages/app/src/service.spec.ts` (~1509 lines) is the primary safety net; also presence / lease-reaper / notifier specs.

**Already extracted nearby:** `wait-service.ts`, `presence-service.ts`, `evidence-service.ts`, `infra-service.ts`, `identity-service.ts`, `run-log-service.ts`.

### Natural sections (approx.)

| Region | Lines | Contents |
| --- | --- | --- |
| Public DTOs | ~75–405 | Capture/Lease/Transition/Pipeline/Review inputs |
| Reads / archive | ~438–660 | get/list/timeline/archive/delete |
| Notes / pipeline API | ~663–825 | notes + set/append/patch pipeline |
| Capture / lease / assign | ~828–1015 | claim path |
| Report / triage / transition / messages | ~1018–1365 | lifecycle verbs |
| Review / checks | ~1368–1588 | reviewing, revise, request-changes |
| **Dependencies** | ~1590–1935 | graph, gates, cascade |
| **Private internals** | ~1940–2300 | `applySystem`, `commit`, `decideOrThrow`, pipeline helpers |

### Proposed extract modules (3)

| # | Module | What moves | Risk | Notes |
| --- | --- | --- | --- | --- |
| S1 | `service-dependencies.ts` | `addDependency`…`dependencyGraph` + private `link` / `dependentsOf` / `dependencyVerdict` / `cascadeToDependents` / `repointDependency` | **Medium** | Clearest seam; cascade + gate semantics. Keep methods on `WorkQueueService` via composition/delegation so HTTP/router signatures unchanged. |
| S2 | `service-pipeline.ts` | Public `setPipeline` / `appendPipelineSteps` / `patchStep` / `getPipeline` + private `normalizeSteps` / `findStepIndex` / `applyStepPatch` / `markStepDone` / `sealPipeline` | **Medium** | Touches every agent run’s dashboard card. Spec coverage is good; watch nested step ids. |
| S3 | `service-review.ts` | `setReviewing`…`rejectFromReview`, `recordChecks`, `recordReview`, revise/request-changes paths | **High** | Couples to WaitEngine (`onEnteredReview`), auto-merge, revise/landing. Run wait-engine e2e + live-api journeys. Prefer last in the service chain. |

**Defer (not filing):** splitting lease/transition/`commit` core, or moving DTOs alone — low payoff vs merge conflict cost.

**Ordering:** S1 → S2 → S3 (`depends-on` prior `:merged`) to avoid thrashing the same file.

---

## 2. `packages/worker/src/cli.ts` (~2515 lines)

### Import / call-site graph

**Entry:** package bin → `cli.ts` `main()` switch (~50 commands).

**Already extracted:** `args`, `announce`, `classification`, `cli-config`, `attribution`, `depends-cli`, `history-cli`, `watch`, `when`, `fleet`, `help`, `evidence-*`.

**Callers of printers:** only `cli.ts` itself (plus tests that exercise CLI via process spawn in `runner.e2e.spec.ts` / command specs).

### Natural sections

| Region | Lines | Contents |
| --- | --- | --- |
| Helpers + printers | ~80–778 | printDetail/Watch/Wait/Checks/Review, whoAmI, captions |
| Switch: register→create | ~821–1050 | registration / create |
| watch / blocked / list / history | ~1052–1330 | mostly delegated |
| show → depends / handoff | ~1334–1540 | inspection |
| claim → feedback | ~1544–2075 | lease lifecycle commands |
| **waits / await** + notes / pipeline / evidence | ~2078–end | hot wait loop |

### Proposed extract modules (3)

| # | Module | What moves | Risk | Notes |
| --- | --- | --- | --- | --- |
| C1 | `cli-print.ts` | `printItemLine`, `printDetail`, `printWatch`, `printWait`, `printChecks`, `printReview`, `printReviewing`, `printSource`, `printGreetingPrompt`, `printTriage`, `printNotes`, `inWords` / `duration` helpers used only by printers | **Low** | Pure presentation; no API behavior change. Easy first cut. |
| C2 | `cli-await.ts` | `waits` + `await` cases, `HOLD_MS`, `DEFAULT_AWAIT_TOTAL_S`, loop against `client.awaitWait` | **Medium** | Hot path for parked agents; wrong exit code wakes wrongly. Keep exit semantics: satisfied→0, ended badly→1, budget exhausted→0. |
| C3 | `cli-helpers.ts` | Shared non-print helpers still inlined: `parsePriority`, `maybeNote`, `announce` wrapper, `whoAmI` / `filedByMe` / `activeItemIds`, `resolveStepId`, `requireCaption`, `peekPrHead` / `peekGitHead`, meta/mime | **Low–Medium** | Shrinks prelude so later command-group extracts are easier. Do not move command cases yet. |

**Defer:** carving the giant `switch` into per-domain command modules (create/lease/review) until C1–C3 land — otherwise every PR fights the same case block.

**Ordering:** C1 → C2 → C3.

---

## 3. `apps/pool/src/orchestrator/orchestrator.ts` (~2510 lines)

### Import / call-site graph

**Defined here:** `Orchestrator`, `OrchestratorDeps`, `STOP_REASON_STEERED`, exported `reviewResume`, `greeting`.

**Importers:** `apps/pool/src/runtime.ts`, `daemon.ts`; tests `orchestrator.spec.ts` (~997), `orchestrator.e2e.spec.ts` (~1820).

**Already extracted nearby:** `slot.ts` (agent run), `routing.ts` (slot pick/preempt), `triage.ts`, `enrichment.ts`, `resolve.ts`, `wrap-up.ts`, `reaper.ts`, `state.ts`, `phase-pipeline.ts`, `handoff.ts`.

### Natural sections

| Region | Lines | Contents |
| --- | --- | --- |
| Init / stop / preempt | ~162–525 | slots maps, reconcile, drain, soft-stop |
| **Triage / route** | ~528–1155 | evaluate → handleItem → claim → greet → route → triage/enrich/terminate/resolve |
| **Slot lifecycle** | ~1159–1700 | tryAssign, assignToSlot, onSlotDone/Error, finishStopped/Steered, gap heartbeats |
| **Park / resume / steer** | ~1703–2305 | refreshParked, reconcileParked, resume*, flushSteerResume, adoptOrphan, wrapUp, sweep |

### Proposed extract modules (3)

| # | Module | What moves | Risk | Notes |
| --- | --- | --- | --- | --- |
| O1 | `park-resume.ts` | `refreshParkedUnits`, `reconcileParked`, `adoptOrphanLeases`, `resumeAfterFeedback`, `resumeFromReview`, `resumePark`/`resumeParkBody`, `wrapUpReview`, `consumePendingSteers`, `flushSteerResume`, `failSteerResume`, related maps (`pendingSteerResume`, `resuming`, gap heartbeat helpers if only used here) | **High** | Recent bugs (#71 feedback resume, #70 flushSteerResume). Landing/revision resumes. Must keep orchestrator e2e green (landing-before-agent included). |
| O2 | `slot-lifecycle.ts` | `tryAssign`, `assignToSlot`, `onSlotDone`, `onSlotError`, `finishStoppedRun`, `finishSteeredRun`, `start/stopGapHeartbeat`, `releaseItem`, `ingestReportFor`, `maybeReap` | **High** | Lease continuity across soft-stop; wrong release orphans worktrees. Depends on Slot + routing already extracted. |
| O3 | `triage-route.ts` | `evaluate`, `handleItem`, `claim`, `greet`, `refreshThinking`, `route`, `publishTriage`, `maybeEnrich`, `terminate*`, `resolveDirectly`, `withCanonicalRepo`, cooldown helpers | **Medium–High** | Intake path; mis-route sends work to wrong repo. Triage agents stay injected via deps. |

**Defer:** further splitting `init`/`drain`/`sweep` until O1–O3 prove the “helpers module holding Orchestrator private state” pattern (likely a collaborator class taking `OrchestratorHost` / shared maps).

**Ordering:** O1 → O2 → O3 (park/resume is the hottest post-WaitEngine surface; do it first while e2e memory is fresh).

### Implementation pattern (all three files)

Prefer **collaborator modules** that receive the host’s deps + private maps, with `Orchestrator` / `WorkQueueService` / `main()` remaining the façade — do **not** change public package exports or HTTP routes in the first PR of each seam. Re-export types from the same places `@workq/app` / pool already export.

---

## Follow-up tickets filed

Nine implementation tickets (one seam each), chained `:merged` within each file family. Briefs name the shared green gates.

### service.ts

| Seam | Id | Title | Depends |
| --- | --- | --- | --- |
| S1 | `wi_f8fc2fd9-c614-4bb8-9e77-89e282c9b35f` | Extract service-dependencies from WorkQueueService | — (ready) |
| S2 | `wi_66f34b75-6b82-45ed-9be9-15f9cc1bf829` | Extract service-pipeline from WorkQueueService | S1:merged |
| S3 | `wi_5ebff506-1041-4e04-81bc-71b8f5f9e309` | Extract service-review from WorkQueueService | S2:merged |

### cli.ts

| Seam | Id | Title | Depends |
| --- | --- | --- | --- |
| C1 | `wi_248687d1-2b46-4707-8c4f-a0dba96f8f1d` | Extract cli-print printers from worker CLI | — (ready) |
| C2 | `wi_14d181f3-2022-4abc-9944-78e19b0b8e84` | Extract cli-await (waits/await) from worker CLI | C1:merged |
| C3 | `wi_f16cbc3e-b1a8-45b8-825a-f949299d9835` | Extract cli-helpers from worker CLI | C2:merged |

### orchestrator.ts

| Seam | Id | Title | Depends |
| --- | --- | --- | --- |
| O1 | `wi_da9aaac4-baa8-4a09-83df-159044c5d278` | Extract park-resume from pool Orchestrator | — (ready, high) |
| O2 | `wi_46352b71-fa9e-4ffb-aeb1-2125f678f4ae` | Extract slot-lifecycle from pool Orchestrator | O1:merged |
| O3 | `wi_24649ebf-b6f6-41dc-9c21-3bdf88c0288a` | Extract triage-route from pool Orchestrator | O2:merged |

**Ready now (3):** S1, C1, O1. The rest unblock as each predecessor merges.

## Out of scope (confirmed)

- Performing any of the extractions in this discovery item.
- Redesigning WaitEngine / auto-merge product rules.
- Playwright chrome beyond existing live-api harness.
