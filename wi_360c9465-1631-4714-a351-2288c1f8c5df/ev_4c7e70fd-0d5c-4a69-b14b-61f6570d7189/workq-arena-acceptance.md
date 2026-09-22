# Workq Arena — campaign acceptance evidence

Durable pass / fail / **not-run** report for campaign acceptance on
`codex/workq-arena`. Does **not** promote the branch. Criteria map to
[`workq-arena.md`](./workq-arena.md) § Acceptance.

| Field | Value |
| --- | --- |
| Item | `wi_360c9465-1631-4714-a351-2288c1f8c5df` |
| Tip SHA | `b058128ba52b6663f629210be8137df1edb899ef` (`docs(arena): setup, judging, and agent runbooks (#1527)`) |
| Host | pool `mac-studio` (worker `mac-studio-w4`) |
| Runtime | Darwin; directory sandbox hermetic + int isolation; `WORKQ_ARENA_RUNTIME` unset on seat |
| Budgets | No paid live-model sweep; synthetic judge smoke only |
| Date (UTC) | 2026-09-22 |

Statuses: **pass** (green evidence on tip), **fail** (broken — must fix), **not-run**
(blocked by missing auth/runtime; never treated as pass).

## Summary

| Area | Result |
| --- | --- |
| Fairness (paired / shared instance / counterbalance) | **pass** |
| Fixtures + catalog presets (blank, both toys, fake VCS, campaign DAG) | **pass** |
| Isolation (forbidden paths/home/endpoints, cancel tree, canary) | **pass** (hermetic + int; not full container/VM sales claim) |
| External judging (await, claim, package, submit, actor refuse, reclaim) | **pass** (synthetic plumbing) |
| Usage reconciliation / unknown ≠ zero / filter score invariance | **pass** |
| UX harness (runner + history shots + STE composition) | **pass** |
| Live-model / real adapter smoke | **not-run** |
| Live fleet mutation check against a real fleet canary object | **not-run** (unit canary injection **pass**) |

## Provenance frozen for this report

- **Workq tip:** `b058128b` on `codex/workq-arena`
- **Harness for UI proof:** dashboard Vite harness-runner + Playwright shots
  (`arena-runner-shots.mjs`, `arena-history-shots.mjs`)
- **Judge smoke label:** `provider: synthetic` / `model: workq-arena-judge-smoke`
- **Fixtures:** `blank`, `toy-ts-service`, `toy-react-api` under
  `apps/pool/src/arena/fixtures/`
- **Catalog presets:** `atr_onboarding_blank`, `atr_onboarding_existing`,
  `atr_integration_fake`, `atr_code_feature_health`, `atr_code_bug_billable`,
  `atr_campaign_dependency`

## Acceptance matrix (¶184)

| Criterion | Status | Evidence |
| --- | --- | --- |
| Paired workload + selectable config | **pass** | `packages/app/src/arena-trial-service.spec.ts`; `apps/server/src/http/arena-api.e2e.spec.ts` (import → paired → judge → history/compare) |
| Both repo fixtures | **pass** | `fixtures.spec.ts` / `fixtures.int.spec.ts` (git rematerialize digests); `drivers.int.spec.ts` |
| Blank onboarding path | **pass** | catalog `atr_onboarding_blank`; runner harness blank-mode + setup asserts |
| Fake integration | **pass** | catalog + `drivers.int.spec.ts` fake VCS holdout |
| Dependency campaign | **pass** | catalog + drivers int campaign order proof |
| Identical resolved hashes A/B + replay | **pass** | `catalog.spec.ts`, `arena-recipe-service.spec.ts`; e2e paired shared instance |
| External judge wait / claim / submit / reclaim | **pass** | `arena-assessment-service.spec.ts`, `arena-assessment.e2e.spec.ts`, `arena-judge-skill-smoke.e2e.spec.ts`; await-judge UX shot |
| Repeat / replay / rejudge | **pass** | trial service + assessment unit; runner task-version-replay shot |
| Exact usage reconciliation (resume/retry/caches) | **pass** | `arena-trial-evidence.spec.ts` |
| Unknown usage ≠ zero | **pass** | evidence + trial seal specs |
| Scoring invariant under history filters | **pass** | `arena-scoring.spec.ts`, `arena-history.spec.ts`; history shots prep asserts tokensScore `7.5` + fingerprint after filter |
| Incompatible comparison refusal | **pass** | `arena-api-service.spec.ts`; e2e compare path |
| Actor failure vs judge failure | **pass** | assessment e2e actor 403; domain abandon ≠ quality zero |
| Cancel/timeout, no late writers | **pass** | trial service unit; `local-runner.int.spec.ts` process tree |
| Crash/restart recovery | **pass** | trial service `reconcileAfterRestart` |
| Bounded artifacts | **pass** | evidence unit |
| Live-fleet canary unchanged | **pass** (unit) / **not-run** (live object) | trial service canary injection; seat did not touch live fleet |
| Deterministic failure matrix | **pass** | isolation refusals, actor refuse, foreign pool, malformed paths in unit/e2e |
| Budgeted live-model adapter smoke | **not-run** | No `WORKQ_ARENA_JUDGE_TOKEN` / `arena-judge.secret.json`; `WORKQ_ARENA_RUNTIME` unset — synthetic smoke is plumbing only |

## Commands run (this seat)

```bash
# Unit (acceptance slice) — 15 files / 153 tests + 3 files / 14 tests
npx vitest run packages/domain/src/arena.spec.ts \
  packages/domain/src/arena-trial-evidence.spec.ts \
  packages/domain/src/arena-scoring.spec.ts \
  packages/app/src/arena-trial-service.spec.ts \
  packages/app/src/arena-assessment-service.spec.ts \
  packages/app/src/arena-api-service.spec.ts \
  packages/app/src/arena-recipe-service.spec.ts \
  packages/app/src/arena/workloads/catalog.spec.ts \
  packages/app/src/arena/drivers.spec.ts \
  apps/pool/src/arena/local-runner.spec.ts \
  apps/pool/src/arena/fixtures/fixtures.spec.ts \
  apps/pool/src/provision/arena-judge-skill.spec.ts \
  apps/dashboard/src/lib/arena-history.spec.ts \
  apps/dashboard/src/lib/arena-history-enrich.spec.ts \
  apps/dashboard/src/lib/arena-runner.spec.ts \
  apps/dashboard/src/lib/arena-workloads.spec.ts \
  packages/protocol/src/schemas/arena.spec.ts \
  packages/protocol/src/schemas/arena-parity.spec.ts

# Integration — 3 files / 8 tests
npm run test:integration -- \
  apps/pool/src/arena/local-runner.int.spec.ts \
  apps/pool/src/arena/fixtures/fixtures.int.spec.ts \
  apps/pool/src/arena/drivers.int.spec.ts

# Server e2e (after `npx tsc --build --force` for stale sqlite dist) — 3 files / 9 tests
npm run test:e2e -- \
  apps/server/src/http/arena-judge-skill-smoke.e2e.spec.ts \
  apps/server/src/http/arena-assessment.e2e.spec.ts \
  apps/server/src/http/arena-api.e2e.spec.ts

# UX harness shots (*.harness.e2e.spec.ts stays out of WORKQ_E2E)
npm run -w @workq/dashboard test:harness:arena-runner
npm run -w @workq/dashboard test:harness:arena-history
```

## UX / STE fidelity (vision)

Reviewed harness stills under `apps/dashboard/tmp/shots/` (not committed; attached
on the work item):

- **STE reference-before** vs **Arena History populated:** same composition —
  trend chart, trial list, radar detail, accessible table; Arena adds Runner /
  Workloads / History chrome and N/A-as-dash copy (not zero imputation).
- **Await-judge:** sealed evidence, actor slot not held, copyable external claim
  workflow (no in-dashboard model invoke).
- **Compare:** dual-color radar + baseline/challenger banner; failed trial
  retained with low quality (does not win rankings by omission).
- Interaction asserts in shot scripts: setup A/B + blank fixture, invalid
  preflight refuses execute, editing-only save-without-run, filter score
  fingerprint invariance, keyboard/mobile/reduced-motion scenes.

## Within-scope fixes on this item

| Change | Why |
| --- | --- |
| `apps/dashboard/package.json` — `test:harness:arena-runner` / `test:harness:arena-history` | Shot headers documented these scripts; they were missing while every other harness scene had an npm alias |

No product defects found in the acceptance matrix on this tip.

## Narrow follow-ups (blocked externals — do not waive as pass)

1. **Live-model / real judge adapter smoke** — materialize pool
   `arena-judge.secret.json` / `WORKQ_ARENA_JUDGE_TOKEN` and run one budgeted
   live assessment; keep synthetic smoke labeled plumbing-only.
2. **Real sandbox runtime enablement** — seat had `WORKQ_ARENA_RUNTIME` unset;
   directory isolation int coverage ≠ container/VM arbitrary-branch sales claim
   (already documented in campaign + user guide).
3. **Live-fleet canary against a real fleet object** — unit injection passed;
   optional operator canary outside this worker seat.
4. **Confluence PNG capture** from suite screenshot placeholders — left from
   docs item `wi_d44397d0` (not required for tip acceptance).

## Cleanup

No live fleet homes, Docker sockets, or shared pool configs were pointed at.
Synthetic e2e servers were in-process only. Shot Vite ports were ephemeral.
