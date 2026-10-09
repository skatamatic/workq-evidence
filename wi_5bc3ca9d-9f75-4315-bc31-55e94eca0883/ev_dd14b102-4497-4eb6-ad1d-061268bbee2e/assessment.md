# Forms pilot readiness assessment — 2026-10-09

Item: `wi_5bc3ca9d-9f75-4315-bc31-55e94eca0883`. Repository: flatironssolutions/etc-client. Declared base: `kwesley/insight-next`. Assessed merged head: `a5bb6e64c820e88da819edee5d0e12df500938e6`.

## Decision

The present synthetic Dropdown, embedded rich-text and bounded source-feedback pilot has strong executable proof. Broader parallel component dispatch remains **blocked pending captain acceptance and human review**, plus the explicit coordination/revision prerequisites below. This assessment does not certify every completed field, customer parity, database persistence, a gated library artifact or periodic product E2E. No component implementation, profile change, environment upgrade, PR or merge was performed.

Recommend a first wave of two disjoint completed-field certification tickets after review, with a named harness/integration owner. Keep component-specific fixtures and browser specs separate. Do not dispatch undefined contracts or shared renderer/rule/TinyMCE edits concurrently. The prepared backlog is `component-backlog.json` (31 ticket-sized briefs; no tickets created).

## Acceptance trace

1. **Current accumulated foundation:** concise delivery skill at `docs/design/form-builder/skills/form-field-refactor-skill/SKILL.md`; catalog generator at `tools/form-fixtures/catalog.mjs`; strict full/focused runner at `tools/forms-verify/run.cjs`; isolated host at `tools/forms-browser/host/main.ts`; Maven/Jenkins carrier at `pom.xml`, `Jenkinsfile` and `tools/verification`. Merged PR37/43/44/42 predecessor handoffs were read. Current head has exactly the approved PR42 source tree (`git diff 503f984... HEAD` is empty), but a different merge revision. Earlier 12-case evidence is historical and cannot certify the present 14-case registration.
2. **Every required registered case:** full runner requires three contract outcomes, one real Angular mount and one real browser save/reload/remount outcome per selected case. `receipt.cjs:6` rejects absent, skipped, duplicate, flaky/failed, empty, stale or changed-input evidence; artifact hashes are checked. Native tooling tests exercise these refusals. Full matrix includes all three authoring pilots. Exact case list and current measurements are in `assessment.json`; retained receipts/artifacts are under the linked benchmark directory.
3. **Actual authoring:** browser `pilots.spec.ts:84` uses production Dropdown editor and builder; `:153` uses local real TinyMCE, production metadata modal, shared scoped rules and production save bridge. Screenshots were visually inspected. Saved models and traces accompany them. Immediate saves freeze the audit timer rather than waiting out debounce; explicit null and fresh TinyMCE edits must survive. Reload destroys/recreates the actual renderer (`host/main.ts:367`), and the second rule scope remains unchanged. Screenshots contain untranslated harness labels; this is execution proof, not Justin UX acceptance.
4. **Negative proof:** missing catalog registration, real Angular bad binding, immediate saved-clear failure and shared-selection broadening probes require their owning failure, not arbitrary startup failure. An additional real TinyMCE stale-rule canary failed with `danglingTarget` in the embedded authoring assertion. Normal final execution follows canaries. Negative receipts are deliberately incomplete and must never be presented as green full proof.
5. **Test strength limits:** `negative.cjs:16` appends a comment to the shared engine and injects wrong-clear expectation. The lost-clear probe deliberately inverts the expected saved value (expects Closed where the actual correct result is null); it proves the owning live assertion fails. Shared-regression proves changed-shared-file broadening and failure propagation, **not behavioral mutation of the engine**. Immediate readonly transition is explicitly pending; current Dropdown tests let retained state catch up before toggling readonly. Current pilots do not certify every rename/delete/duplicate/import/revision lifecycle or every component described by the skill.
6. **Catalog extensibility:** recursive discovery generates registrations/showcase through production save (`catalog.mjs:13`, `catalog-entry.ts:10`, `catalog.ts:49`). Second-component source-contract regression proves independent IDs and tamper refusal (`catalog.spec.ts:94`). No hand-maintained giant registry is needed. However authoring pilots are currently explicit mappings (`selection.cjs:47`, `result-receipt.cjs:1`), and generic browser interactions only implement present component operations (`matrix.spec.ts:64`). A fixture alone cannot certify a new component's authoring/interactions. Assign one shared harness owner and one generated-output integration owner.
7. **Production exclusion:** production entry/output is `src/main.ts` → `dist/etc-client`; host entry/output is separate. `tsconfig.app.json` starts main/polyfills; Maven packages only `dist/etc-client` (`pom.xml:237`). No production app imports of host/catalog were found. Actual production artifact scan result is recorded separately in `assessment.json`; structural exclusion alone is not a byte-level claim.
8. **CI evidence:** authenticated PR42/13 readback is SUCCESS, 1,821 JUnit passes, zero failures/skips. Original full receipt executes 14 cases/70 assertions/26 supporting assertions, with all 120 artifact hashes remotely reverified. Source revision is `503f984740cc964996b558fae0cc083e2284b776`, not the merged head. Matrix elapsed 207.095s; build duration 2,118.233s includes provisioning/queue/other work. No post-merge integration CI result or remote deliberate canary run is claimed. CI Node24/Linux differs from local Node22/macOS and source-only CI base provenance is null.

## Data bridge, separately assessed

Source manifest `reference-feedback-action-v1` pins insight-server `62e10d72485215a3ba9151fac019d8813ea6bfef`. Original archive SHA256 `ce0ad91abf59caee9a714641d35ca94c193eb32e914f07e87d92998a3a0da2be`; original form SHA256 `ddf5ca5fb8bf483b16f780e61a5bb5fcb31ba6ac3f95a0217b74c34956e1924f`; sanitized projection SHA256 `b58baff5315c34d7576dcb89884bdf58c127e077acdff4645919e2f6176db318`.

Supported `static-action` preserves exactly Solved / Global Method Action / Reject, required=true and readOnly=false. Cases are `source-feedback/supported-selection` and `source-feedback/explicit-clear`. Independent literal reviewed pointers, target/type and complete config prevent jointly edited projection/fixture metadata from moving the approval boundary. Generated source-backed receipts retain distinct definition/input/expected hashes; these are derived ETC hashes, not original legacy bytes.

`legacy-empty-sentinel` and `site-reference` mappings are refused. Null is synthetic ETC input, not translated legacy Choose one.../empty default; the required source pilot has no UI clear affordance. Pending: sentinel/default semantics; readonly relationships/dynamic table; remaining controls/layout/workflows; embedded conversion; HTTP/database/full customer journey. Private original bytes were not re-read by this acceptance run: opaque committed provenance plus reviewed projection is the local contract, and a separate private verifier/locator is required to reprove original archive bytes.

Reuse named server dependencies from `docs/design/form-builder/source-backed-fixtures.md:23`: [start PR71](https://bitbucket.org/flatironssolutions/insight-server/pull-requests/71), [task forms PR72](https://bitbucket.org/flatironssolutions/insight-server/pull-requests/72), [FormApp importer PR59](https://bitbucket.org/flatironssolutions/insight-server/pull-requests/59), [journey PR75](https://bitbucket.org/flatironssolutions/insight-server/pull-requests/75), [MDCR PR74](https://bitbucket.org/flatironssolutions/insight-server/pull-requests/74), and branch/timer/subtask item `wi_ec434f93-7e1d-4b8e-96f6-a0d65c878bf9`. Their existing tests do not prove new ETC UI rules. Legacy referenceId4 and task formKey319 are distinct namespaces; start318/global-review320 are outside this projection.

## Dispatch boundaries and revision recommendations

- **Certification first:** checkbox/date/number/textarea/radio/people-group/people-user are marked completed activities but absent from the present catalog. Each owns its own fixture, adjacent contract spec and dedicated browser spec. Existing amount gets contract review first; no completed activity status is inferred. Current Dropdown/embedded proof is bounded; full activity closure is not granted here.
- **Component briefs:** Display Text; Display Value; File; Image; Link; Password; Audit Events; Barcode; Form Data; Form Save; Pinpoint; Related Documents; Single Signature; Signature Table; Sign-Off; Rich Text; Step Table each has a separate brief in the JSON backlog. Host/source/signature/upload contracts and shared-file dependencies are explicit, not invented implementation approval.
- **Hold contracts:** Part, Table, Button, dynamic value sets, Notification authoring and Number mask. Justin UX and unrelated work-order/library UI are excluded.
- **Shared owners:** form-container, adapter registry, configuration modal, save/reader, rule engine/catalog/registry, TinyMCE bridge and component-library are collision boundaries. FB-039/041/045/046 lifecycle work is sequenced before dependent field revalidation. Generated registry/showcase writes belong to one integration owner after fixtures merge.
- **Precise foundation revision:** declare component authoring/scenario requirements alongside registrations, or provide an explicit reviewed coordinator registration with fail-closed receipt enforcement. Prove a new component cannot pass full verification without its required authoring and applicable interaction scenario. Do not substitute generic mount/save proof. Strengthen the shared regression probe with a real behavioral mutation of shared production rules and an owning outside-selection failure, then restore green execution. Immediate readonly transition needs a named regression before claiming that dimension.
- **Library gating prerequisite:** consumed `@flatirons/component-library@20.3.16-feature-etc-insight.1791070420` has pinned bytes/integrity, but no proved build-info/gitHead chain to gated PR61. Require owner-supplied published tarball integrity + source SHA + successful lint/unit/build receipt mapping; the consumed artifact must match that mapping. Publishing skipped is not gated artifact delivery.
- **Product lane prerequisite:** reuse `wi_bbde85ce-d5ed-4a37-980e-5d178c1a1130`. Require landed command/config, paired isolated client/server/auth/disposable DB/synthetic seeds, required-field refusal with no mutation, submission/completion readback, captured effects, explicit Jenkins trigger/schedule, teardown and reviewed retention. Remote QC Playwright is not this lane. Product full E2E scheduling and persistence remain blocked.

## Workq commands and performance recommendation

No shared configuration was changed. Keep existing configured **Lint & format, full Unit tests and Build** gates. Recommend captain review/promotion of the observed additional full forms command only: `npm run forms:verify -- --full --base <actual-locally-resolvable-target-base>` for local component delivery. Jenkins already runs the identical shared full command through `npm run test:ci`, after full coverage; preserve its source-only null-base disclosure unless target ancestry is explicitly fetched and supplied through FORMS_VERIFY_BASE. Never replace full units with the matrix or promote focused execution as final full evidence.

Supported iteration: `npm run forms:verify -- --focus --base <actual-target-base> --component <id>` or explicit `--case <component>/<case>`. Shared/unknown changes broaden. Require receipt validation and retained artifacts. These are recommendations to the captain, not activated profile commands.

Goals: warm full <=120s, focused <=30s. Measured results and memory are in `assessment.json`. Fresh means fresh source/compiler/browser build after pinned dependencies were provisioned; install/checkout copy time is excluded. Host contention from independent negative/browser work affected part of this benchmark, so it is not a hardware-isolated performance guarantee. Per-command maximum RSS is not aggregate concurrent process-tree memory.

Focused clear exceeded30s: Angular15.9s + browser12.2s + tooling/catalog7s dominate. Preserve all assertions. A concrete next optimization is a dedicated scoped Angular entry that retains catalog-integrity/supporting contracts but reduces startup compilation; prove identical owning failures and receipt counts before adopting it. Until measured, advertise approximately40s focused iteration rather than a portable30s promise. Warm full meets its goal; CI Linux full207s does not support a cross-platform120s guarantee.

## Evidence and integrity

The machine-readable assessment links durable Workq uploads and exact Jenkins URLs. Benchmark artifacts remain beside their copied receipts; benchmark checker validated every receipt against its live owned checkout before cleanup. Old heads, initial dependency refusal, the provisioning-race failed copy, incomplete negative receipts and excluded scoped cases are explicitly separated from successful full evidence. No source files or lockfile were changed; injected AGENTS and skill/tool plumbing remain unrelated working-tree changes.

Current observed wall times: fresh97.2s; warm70.7/64.4s; focused37.2s; concurrent147.4/149.0s. Maximum command RSS respectively2.56/1.76/1.70/1.71/2.44/2.30GiB. Final normal post-canary full88.372s, validated current. Native tooling22 tests. Concurrent runs overlap independent canary and app builds, so their148s is a contention measurement, not two-run-only capacity.

Production scan: explicit production configuration passed;333 output files/15,420,019bytes hashed;0 host/catalog marker matches. Default build also passed but resolves development, so only the explicit production output supplies the shipment check.

Durable downloads: [six-run benchmark](http://127.0.0.1:9876/evidence/ev_85f3853f-a7b3-461c-b6d0-50957b442a9c), [31 component briefs](http://127.0.0.1:9876/evidence/ev_2f867cca-e4de-40b4-9a47-b98d3a199749), [complete final proof bundle](http://127.0.0.1:9876/evidence/ev_2e634769-b52b-470b-892a-7775a9e9c6da), [TinyMCE authoring screenshot](http://127.0.0.1:9876/evidence/ev_7c82448d-a8d2-4259-80e8-2b7ac5b8bd93), [remote120-hash check](http://127.0.0.1:9876/evidence/ev_4aa66eef-d2be-4d83-88e6-82191272a94d), [original Jenkins full receipt](https://build.flatirons.cloud/job/RDCA/job/etc-client/job/PR-42/13/artifact/test-results/forms-verify/run-BeIgxc/receipt.json). Workq URLs require the local worker host; the seven proof parts retain all original artifacts and have rehydration/hashing instructions in the manifest.

No repository lint/full-unit rerun was performed for this read-only investigation; existing source-head Jenkins proof was independently read back. Every final component implementation must still run all configured local gates before PR.

## Exact matrix identity

Selected and executed in each full run (14 required; none excluded/skipped):

- `dropdown/configured-disabled`
- `dropdown/default`
- `dropdown/explicit-null-clear`
- `dropdown/multiple-empty-clear`
- `dropdown/readonly`
- `dropdown/required`
- `dropdown/selection`
- `embedded/disable`
- `embedded/enable`
- `embedded/hide`
- `embedded/remount-clear`
- `embedded/remount-populated`
- `source-feedback/explicit-clear`
- `source-feedback/supported-selection`

Focused clear selected/executed only `dropdown/explicit-null-clear`; all13 other IDs above were disclosed exclusions. Pending/unsupported capability IDs have no fabricated registered success cases: `dropdown/dynamic-value-sets`, `dropdown/object-options-and-async-loaders`, `embedded/legacy-javascript-events`, `embedded/required-optional-rule-actions`, `source-feedback/legacy-parity`.

Local identity: Node22.17.0/macOS arm64; Angular20.3.16, component-library20.3.16-feature-etc-insight.1791070420, icon-font4.1.4, TinyMCE6.8.6, Playwright1.59.1, Chromium147.0.0.0, Nx21.6.10. Lockfile SHA256 `fd4c7d0694c7ddf4d7c667963ff3bec6289b0033bec1dbab0f5037137d2bf530`. Installed library/browser byte hashes and every source hash remain in the machine receipt.
