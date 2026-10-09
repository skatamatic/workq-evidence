FormApp browser smoke prerequisite handoff

Inspected client 50e4df3ef50649e0e2b3d0eb56dc9838d6f4d789 and fetched paired server kwesley/workflow-scenario-port at 199397b95041f9987ac0bfe2b28cbc80dc4189fe. Legacy source is exactly 525716d6c337c5b007894e59cea555d7c051605c. No product files changed; no browser execution or CI verification claimed.

Blocking product seam (proposed owner: Insight client forms delivery):
- https://bitbucket.org/flatironssolutions/insight-client/src/50e4df3ef50649e0e2b3d0eb56dc9838d6f4d789/src/app/main/components/applications/task/task-form/task-form.component.html#lines-1 contains only a translated stub.
- TaskFormComponent has no form retrieval/renderer/submission implementation.
- applications-task.component.ts lines 150-154 implements Save as console.warn; lines 177-178 calls TaskService.completeTask.
- backend/api/task.service.ts lines 40-43 uses raw PUT /internal/task/:id/action/complete, forbidden as substitute for form-backed completion.
Required linked implementation work: own archive form JSON -> production reader -> renderer -> validated form submission bridge; wire task actions through it, retain refused task and assert no backend mutation. This is product host integration, not a test-only fixture.

Blocking runtime seam (proposed owner: paired server/browser acceptance maintainer):
- https://bitbucket.org/flatironssolutions/insight-server/src/199397b95041f9987ac0bfe2b28cbc80dc4189fe/insight-rest/src/test/java/com/flatirons/insight/scenario/CustomerWorkflowHttpSmokeTest.java#lines-354
  WebConfiguration imports archive, process-instance, forms, task actions and related-content controllers only. HttpRun at line384 owns one JUnit invocation, not an executable browser bootstrap.
- Browser auth uses GET /auth/encrypted and encrypted POST /login (client backend/api/user.service.ts lines35-46 and login/service/auth.service.ts lines18-30); ordinary login redirects to configured ACM. HTTP smoke verifies internally signed tokens through captured ACM final transport; it does not establish the browser login contract.
Required linked implementation work: long-lived bounded local real-server bootstrap with fresh schema, persisted synthetic seed, product UI query/config/user/login controllers, safe ACM login and final mail/content transports, external local configuration, startup receipt and deterministic teardown. Keep test-support out of shipped artifacts. Owner must choose supported local sign-in contract, without inventing publisher policy.

Journey contract:
- Legacy docs/scenario-harness/customers/form-app/README.md lines16-24 requires TEN parallel department forms; one completed task leaves nine active.
- https://bitbucket.org/flatironssolutions/insight-server/src/199397b95041f9987ac0bfe2b28cbc80dc4189fe/docs/form-app-full-journey.md
  Existing full journey/backend assertions are not browser proof. FormApp has an empty start schema and no declared mail/metadata listeners; start-page placeholder alone is not a required-field blocker.
- Read current workflow-scenario-port.md, workflow-scenario-seams.md, customer-parity-inventory.txt and workflow-http-smoke.md. Current FormApp full-journey inventory is ASSERTED at backend layer; do not revert it or promote browser parity from it.

Acceptance receipt for this item:
Authenticate: PENDING (browser-compatible isolated login runtime unproved).
Start: PENDING (UI start exists, not executed against isolated runtime).
Required-field task rendering: BLOCKED (placeholder).
Refusal on task/no mutation: BLOCKED (no validated UI submission bridge).
Valid form submission: BLOCKED (raw completion is only current action).
Visible process completion: BLOCKED (all ten form arms required).
Client smoke CI: PENDING (no runnable product journey yet; existing KCS config targets absent src/e2e tests).
Actual browser legs passed: zero. Test/Surefire counts for this run: zero; no remote CI/Sonar claim.

Routing decision:
Do not fabricate a skipped/always-green smoke or widen into renderer/auth/runtime architecture without an owner. Split and assign the two implementation seams above, then resume bounded Playwright/CI work with pinned archive provenance and all-ten-form completion. PR81 dependency: https://bitbucket.org/flatironssolutions/insight-server/pull-requests/81.
