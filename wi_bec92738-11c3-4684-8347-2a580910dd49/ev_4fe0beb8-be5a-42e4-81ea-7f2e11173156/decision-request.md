# Analysis journey prerequisite and ownership decision

Item: wi_bec92738-11c3-4684-8347-2a580910dd49
Repo: insight-server — https://bitbucket.org/flatironssolutions/insight-server
Branch: workq/wi_bec92738-11c3-4684-8347-2a580910dd49
Inspected base: e7d65ab8bd736b83a18c97db9457b4dd27f586c4 (declared kwesley/workflow-scenario-port)
Legacy source: 525716d6c337c5b007894e59cea555d7c051605c

## Decision needed

The mail prerequisite D has landed. The requested unchanged-form long and short completion journeys now expose another production prerequisite: persisted Inspector/Generic signatures. Assign an owner and approve the bounded signature delivery/ordering before resuming this journey. This is a new database/API/security/form lifecycle, not a script replacement. Replacing signoff with a string, Boolean or no-op would bypass the source contract. No production or fixture files changed, no PR opened, no parity dimensions promoted.

## Source-backed contract

All source paths below are relative to /Users/kylewesley/repos/insight-activiti-bpm-suite/activiti-bpm-suite at the pinned revision.

- activiti-app/src/test/java/com/flatironssolutions/insight/scenario/dsl/TaskFormSession.java:637-700: sign() reads the initialized signature ID/revision, invokes production saveSignature with SIGN, and submits the resulting persistent signature ID. It is expressly not a form-value fill.
- insight-app-ext/src/main/java/com/activiti/service/runtime/InsightFieldServiceImpl.java:266-274: initializes a signature record bound to form/process/task and populates the form field with its ID.
- insight-app-ext/src/main/java/com/activiti/service/runtime/InsightSignatureServiceImpl.java:148-200: authenticates, validates, persists and publishes signature effects; :203-224 validates task permission; :273-301 checks existence, stale revision, signer-only unsign and Inspector/Technician capabilities; :304-326 binds and records identity, timestamp, state and revision.
- activiti-app/src/test/java/com/flatironssolutions/insight/scenario/customers/safrananalysis/SafranAnalysisScenarioTestBase.java:59-100 grants Inspector capability to the real SAE/analyst groups used by the spine. The required signature is an authorization boundary.

Current ArchiveFormDefinition.java:30 omits signature/boolean from writable types; :159-163 refuses required unsupported fields. The original signoff customProperties declare signatureType=Inspector and requirePinAuthentication=true (confirmed for Launch); the original archive requires Inspector signoff on Conflict List, Launch, Incremental publishing and Finalize; AdHoc and Full publishing use Generic signatures. All these signoff fields declare requirePinAuthentication=true. Finalize also requires Boolean completemerge. Current signature-named files contain only a reporting handler; no signature entity/repository/service/API exists. See attached inspection for exact original form hashes and field inventory. This is static inspection, not a new runtime receipt.

## Resume plan after prerequisite delivery

Reuse WorkflowAnalysisOperationsTestConfiguration (real PostgreSQL/JPA/Flowable, deterministic engine jobs, real outbox, isolated mail/content transport), AnalystStartContentScenarioTest start upload/forms wiring, and SafranAnalysisArtifactBuilder operations-v1. Deploy exact original forms with ArchiveFormResources mappings. Keep original source archive/hash and original live definitions unchanged.

Task naming is also missing, but its contract is resolved: InsightRenameTaskListener currently delegates only to Feedback routing and lacks the two-argument overload. Legacy InsightRenameTaskListener generates the Analysis name from the configured prefix/package ID, actual BPMN task name (or explicit overload name), analyst_document and analyst_group_name. Resolve original numeric group identities against persisted configured groups; do not assume current database IDs equal archive IDs. Preserve refused actor/tenant no-mutation tests.

Long path: authenticated uploaded highlights -> acknowledgement as SAE R4 -> Conflict List -> Manager Triage -> Launch -> deterministic jobs -> AdHoc as SAE R3 plus two keyed analyst instances -> Finalize as SAE R4 -> Full publishing as SAE R5 -> finished mail/end. Two document keys are LEAP-1A-72-00-03-04A-522C-C (LEAP-SAESB-A) and LEAP-1A-72-58-00-01A-810A-C (LEAP-SAESQY-A); LEAP-1A-72-24-01-01A-663B-C is excluded. Partial completion must leave the keyed sibling active and Finalize absent; full join must reach Finalize. Use FormService submissions, never direct task completion or injected listener context.

No-highlights is a first acknowledgement wait with analysis still required, not automatic no-analysis completion. Acknowledgement analyst_required=false routes to R5 without Launch/MI/Finalize. Real CMM-ALL metadata also selects no-analysis; IC selects Incremental publishing. Assert history and branch-not-taken behavior. Source owners: SafranAnalysisSpineScenarioTest, SafranAnalysisShortRouteScenarioTest, SafranAnalysisMultiInstanceScenarioTest, README.

## Required signature delivery receipts

Specify bounded support for original Inspector and Generic fields with task/form/process-bound persistence, authenticated signer and capability checks, revision/state handling, form initialization/read/submission, and durable event effects at isolated capture boundaries. Include a named actor lacking Inspector, cross-task/tenant signatures, stale revision, non-signer unsign, completed task, rollback and concurrent signing refusal with no mutation. Resolve the original requirePinAuthentication=true flag against the source scenario helper, which passes null PIN to the production API: decide the authenticated no-PIN path versus PIN enforcement before delivery. Decide separately how impersonation and legacy signature-event rules are supported or remain explicitly pending; do not invent those semantics in the journey worker.

## Status and validation limits

All six Analysis product dimensions and archive-publication remain PENDING. No Maven run, Surefire counts, Jenkins or Sonar rerun in this discovery-only turn; previous D receipts are not evidence for these journeys. Exact-head gates remain required after implementation. updateGroupName/missing-group correction stays owned separately and must not be manufactured by variable injection. Dependent conditional-refusal/notification item wi_6ab73c34-a3f7-4717-9dcf-086398d876f4 has no new interface to build against until this prerequisite and the journey land.
