# MDCR consult-syntax-v1: source-cited policy proposal

Repo: insight-server — https://bitbucket.org/flatironssolutions/insight-server
Inspected head: `2b785a9d3a670ce0028dc81f22638324dfc1fb7d`.
No product code, customer archive, form, definition or live instance was changed. No PR was opened.

## Exact proposed transformation

Accept only original archive SHA-256 `7ffe64c62463713346cf4c610235248c72c299a47c984da8b09cb3bb7ec0bf2b` and member `bpmn-models/MDCR-21.bpmn20.xml` SHA-256 `6063c14c2bf0c159a346599aec664168174a571dd00da97f632745c084e5c0a7`.
At process MDCR, user task supervisor-consult-sme, activiti executionListener event=start, remove only the trailing semicolon:

- Before: `${execution.setVariable("n_days_supervisor_consult_sme", 0)};`
- After: `${execution.setVariable("n_days_supervisor_consult_sme", 0)}`

The proposed one-byte BPMN repair hashes to `01f796d484062baf46236319e9fea974c80a62d42fbc63fb616e505790f2533d`. This is not a published artifact receipt. All unrelated expressions, sequence-flow order, timers and form bytes would remain unchanged. A builder should return deterministic separately named archive bytes and a sidecar manifest with source/artifact hashes, exact binding placement and before/after expression. Original archive acceptance must remain refused; unknown input hashes must refuse before mutation. Proposed app identity: MDCR-consult-syntax-v1; alias prefix must follow that derived app identity, while form bytes remain unchanged. No live cutover or latest-version start policy is approved here.

## Why exact syntax repair does not satisfy authenticated product execution

Source links below are pinned to the inspected head.

1. [ArchiveExecutionPolicy.java:95-99](https://bitbucket.org/flatironssolutions/insight-server/src/2b785a9d3a670ce0028dc81f22638324dfc1fb7d/insight-service/src/main/java/com/flatirons/insight/service/editor/ArchiveExecutionPolicy.java#lines-95) refuses unretained expressions and records every binding absent from the execution contract. The corrected expression is not retained. Adding that single expression could be part of the syntax repair, but does not make MDCR runnable.
2. [archive-feedback-execution-contract.txt](https://bitbucket.org/flatironssolutions/insight-server/src/2b785a9d3a670ce0028dc81f22638324dfc1fb7d/insight-service/src/main/resources/archive-feedback-execution-contract.txt) has zero MDCR rows. Even after retaining the repaired expression, the publisher would record MDCR execution diagnostics.
3. [AppArchiveServiceImpl.java:67-78](https://bitbucket.org/flatironssolutions/insight-server/src/2b785a9d3a670ce0028dc81f22638324dfc1fb7d/insight-service/src/main/java/com/flatirons/insight/service/editor/AppArchiveServiceImpl.java#lines-67) suspends the whole definition when any diagnostics remain and reports runnable=false.
4. [ArchiveProcessAvailability.java:16-33](https://bitbucket.org/flatironssolutions/insight-server/src/2b785a9d3a670ce0028dc81f22638324dfc1fb7d/insight-service/src/main/java/com/flatirons/insight/service/editor/ArchiveProcessAvailability.java#lines-16) refuses persisted diagnostics independently of mutable engine activation. [ProcessInstanceServiceImpl.java:169](https://bitbucket.org/flatironssolutions/insight-server/src/2b785a9d3a670ce0028dc81f22638324dfc1fb7d/insight-service/src/main/java/com/flatirons/insight/service/runtime/ProcessInstanceServiceImpl.java#lines-169) calls that guard. Unsuspending the definition would not satisfy the product start boundary.
5. [SafranAnalysisArtifactBuilder.java:51,72](https://bitbucket.org/flatironssolutions/insight-server/src/2b785a9d3a670ce0028dc81f22638324dfc1fb7d/insight-service/src/main/java/com/flatirons/insight/service/runtime/SafranAnalysisArtifactBuilder.java#lines-51) is a hash-pinned builder, not a product archive publication exception. Its [coexistence receipt](https://bitbucket.org/flatironssolutions/insight-server/src/2b785a9d3a670ce0028dc81f22638324dfc1fb7d/insight-service/src/test/java/com/flatirons/insight/scenario/SafranAnalysisArtifactCoexistenceScenarioTest.java) deploys directly with RepositoryService.

## Decision needed

Recommended bounded acceptance: explicitly allow the derived archive to publish through the authenticated SYSTEM product API as **published but non-runnable**, and require a separate, clearly labeled engine/form consult receipt using the same migrated bytes. That receipt would not certify product start, original customer parity, routing, mail or child flows. This changes the interpretation of the requested migrated product journey and needs its owner.

If the outcome instead requires starting this API-published archive through product services, an owner must define the MDCR execution enablement policy and its prerequisites. Clearing all MDCR diagnostics, deleting policy metadata, bypassing the product guard or inventing routing/mail bindings would exceed the exact syntax repair. This worker did none of those.

## Evidence and remaining work

Discovery evidence ev_9504ae37-c1f3-4f21-82b8-075f534c262b executes read-only hash, exact-placement and catalogue assertions. No Java/API/runtime test or local CI gate was run; this is static boundary characterization only.

Original publication: PENDING/refused, unchanged. Derived publication: NOT BUILT / NOT CERTIFIED. Two-day timer, comments and fresh Supervisor: existing original direct-engine form receipt only; no migrated receipt. Authenticated alias/resource consistency, malformed-variant atomic refusal, deterministic derived archive and configured gates remain unimplemented. Keep the downstream certification dependency blocked until the decision and implementation are complete.
