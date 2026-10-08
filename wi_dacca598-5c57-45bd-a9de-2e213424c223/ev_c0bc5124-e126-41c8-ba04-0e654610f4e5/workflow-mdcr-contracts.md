# MDCR subtask and document execution contracts

This inventory characterizes legacy source at
`525716d6c337c5b007894e59cea555d7c051605c`. It defines the evidence the MDCR
ports must preserve; it does not introduce a production document system or
certify the full customer journey. Read it alongside
[execution seams](workflow-scenario-seams.md) and
[port receipts](workflow-scenario-port.md). The parity inventory remains
authoritative: MDCR authenticated start, first wait and wrong-user task refusal
are ASSERTED; form refusal, customer path and full journey remain PENDING.

## Source references

Paths below are relative to the legacy repository at the pinned snapshot.
The abbreviations are only for locating source, not runtime interfaces:

- `APP`: `activiti-bpm-suite/activiti-app/src/test/java/com/flatironssolutions/insight/scenario/`
- `EXT`: `activiti-bpm-suite/insight-app-ext/src/main/java/com/activiti/`
- `CONFIG`: `activiti-bpm-suite/insight-app-model/src/main/resources/insight-app/config/`

Customer expectations come from
`docs/scenario-harness/customers/mdcr/README.md`, its
`ASSERTION-INVENTORY-9075.md`, and `APP/customers/mdcr/` tests. No customer
bundle or fixture bytes are changed by this inventory.

## Parent and department children

The flagship creates Review, Other and Reliability as Insight child tasks under
TechLead Implementation, using separately deployed FormApp forms. These are
not BPMN call activities (`APP/customers/mdcr/Mdcr.java:249–263,2595–2669`).
Legacy `EXT/service/runtime/InsightSubtaskServiceImpl.java:1076–1106` creates
each child with parent task ID, current user's tenant, parent's process instance
and definition IDs, and deployment-derived app category. It does not give the
child a BPMN execution or task-definition ID. Parent linkage, process identity,
form/deployment identity and durable child order are distinct requirements.

Addition validates parent read permission and initializes form metadata and
durable order (`InsightSubtaskServiceImpl.java:359–405`). The fixture saves
assignee and due date through each child form, starts the ordered flow, then
claims, submits the form and invokes post-completion for each child. Due date
is engine-clock date plus seven days, rather than wall-clock time
(`Mdcr.java:2595–2685`). Parallel neighboring children form a group; all members
must complete before progression (service class contract at lines 113–130).

The `insight_task_form_key` metadata selects subtask configuration; absent or
empty selects `default` (`InsightSubtaskServiceImpl.java:1478–1488`). The fixture
parent and children omit it; a live `delta-mdcr` overlay is not this fixture's
policy (`README.md:94–98`). `CONFIG/config-form-subtask.json` names assignee,
due-date and action field metadata; its success actions are Approved, Completed,
Complete and Approve, and rejection actions are Rejected and Reject. Save updates
owner/due date; completion records action/rejection and notifies parent owner.

Parent completion eligibility is true with no children, or when the flow is
COMPLETED and the last group is not rejected
(`EXT/service/runtime/InsightSubtaskFlowHelperImpl.java:232–242`). A rejected
last group leaves Continue Pending (lines 210–225). Post-completion resolves
configuration from the **parent** form; when eligible and `completeParentTask`
is true, it notifies owner, marks local flow state Completed and completes
parent (`InsightSubtaskServiceImpl.java:799–832`). Both relevant default
completion events enable parent completion.

**Refusal evidence limit:** the legacy incomplete-child test checks eligibility
false and open task state; it does not submit parent completion
(`MdcrFlagshipSuiteScenarioTest.java:807–848`).
`APP/dsl/ProcessScenario.java:1855–1858` explicitly states the UI disables
Complete while the server form-complete path does not re-check eligibility.
A new server refusal/no-mutation receipt is required; this legacy receipt
cannot be promoted to an HTTP refusal guarantee.

## Document identities and effects

The flagship upload is synthetic UTF-8 text:
`scenario-upload:flagship-mdcr-attachment.txt`, filename
`flagship-mdcr-attachment.txt`, MIME `text/plain`, source `scenario-harness`.
Creation links task/process but leaves field null. It must be absent from
`mdcr_attachments` until Originate form submission binds its RelatedContent ID
through `fillUpload`; afterwards the field query must include that ID
(`MdcrFlagshipSuiteScenarioTest.java:886–949`). A content row ID, storage ID,
form field ID, task ID and process ID must never be treated as interchangeable.

Completion dispatches historic task, authenticated user and request/cookie
context to asynchronous HTML-to-PDF generation
(`EXT/extension/bean/InsightHtmlToPdfService.java:137`). Historic print-preview
HTML goes through the Node PDF service, then the stream goes to persisted PDF
content (`EXT/service/runtime/HtmlToPdfService.java:265–280`).
`InsightFormPdfServiceImpl.java:65–83` names output from task name (truncated
to 250 characters) plus `.pdf`, uses source `insight-form-pdf`, and creates
RelatedContent tied to task/process. A separate InsightFormPdf record holds
task ID, process ID, RelatedContent ID and timestamp. The stored RelatedContent
record in turn identifies the byte-store object.

Active/detail task rendering uses temporary streams; completed task rendering
retrieves persisted PDFs and generates missing ones. Refresh regenerates the
parent. Merge uses parent first, then durable child order only when parent
metadata `insight_task_pdf_include_subtasks=true`, honoring child metadata
`insight_task_pdf_exclude_subtask`
(`HtmlToPdfService.java:203–243,347–375,401–410`).

**PDF evidence limit:** `APP/seams/HtmlToPdfSeamConfiguration.java:29–31,56–106`
replaces rendering with a mock returning empty streams and recording calls/task
identity. Flagship counts are six L1 and nine L3 user-overload calls: each
main-path submitted form plus three children. Parent auto-completion adds no
call of that overload (`MdcrFlagshipSuiteScenarioTest.java:70–83`). Those
receipts prove invocation, not PDF bytes, storage correctness or merged output.

Analytics expects a process record in `insight_mdcr` and task records in
`insight_mdcr_task`: L1 Originate, Supervisor Review, TechLead Review and
Implementation; L3 additionally ORT, Mng Dir and RCB. Originate payload includes
`nomenclature=MDCR golden nomenclature` and `form_level=Level 1` or `Level 3`
(`Mdcr.java:297–300,532–565`). Process document ID is the actual process-instance
ID; task document ID is the historic task ID resolved by task name
(`APP/dsl/ScenarioBusinessAssertions.java:508–521`). Neither is the MDCR
reporting `process_id` variable or a RelatedContent ID.
The required set is **containment**, with extras allowed;
it does not assert child record absence or exact set equality
(`ASSERTION-INVENTORY-9075.md:26`).
`APP/seams/AnalyticsClientSeamConfiguration.java:25–30,56–105` captures real
processor update/delete payloads at transport and skips index bootstrap; it
does not write to live OpenSearch.

## Reusable Insight Next boundaries and missing ports

Current runtime classes below live in
`insight-service/src/main/java/com/flatirons/insight/service/runtime/`;
scenario tests live in
`insight-service/src/test/java/com/flatirons/insight/scenario/`.

- [TaskResponse](../insight-model/src/main/java/com/flatirons/insight/model/response/TaskResponse.java)
  preserves `parentTaskId`. Flowable provides task linkage, but `TaskServiceImpl`
  still returns null for creation. There is no equivalent ordered subtask service.
- `TaskActionServiceImpl.completeTask` authenticates assignee in an engine command
  but does not check children. `FormServiceImpl.submitForm` validates deployed
  fields and submits through the engine; it also lacks a child guard. Neither
  must be advertised as MDCR parent-completion certification.
- Current form resolution assumes process/deployment-linked tasks. A department
  port must preserve the child's parent process/deployment and FormApp form
  identity without manufacturing a BPMN execution. Archive upload binding is
  unsupported; a raw variable write is not an attachment form submission.
- `ContentStorageServiceImpl` persists mapped RelatedContent metadata, assigns
  store ID from row ID, writes through `IStorageClient`, closes input and supports
  read/delete. `CustomerContentCaptureScenarioTest` proves exact Analysis JSON
  bytes, identities, stream closure, defensive copies and two-run isolation with
  real JPA and production mapping. This is standalone storage coverage, not MDCR
  upload/PDF/analytics coverage or proof of atomic external-storage rollback.
- `RelatedContentServiceImpl` has explicit permission TODOs. Update, download,
  delete and export resolve content IDs without establishing their association
  with the requested task/process. Reuse storage internally with isolated
  transport; do not expose these facade methods as a certified authorization
  boundary. Missing actor/content-scope checks require a separate bounded port.
- `CustomerFirstWaitScenarioTest` already refuses named `refused@insight.test`
  on MDCR claim/complete under JWT and legacy principals and compares state,
  variables, identity links and mail count. It proves wrong-user refusal at first
  wait; it does not prove parent refusal with outstanding children.
- Current archive forms, persisted ACM identity/reference seed, workflow mail
  outbox and isolated mail/content transports remain the reuse points. They
  supply no PDF renderer, InsightFormPdf persistence or MDCR analytics pipeline.
- [PdfProperties](../insight-config/src/main/java/com/flatirons/insight/properties/PdfProperties.java)
  already declares the PDF service base, render endpoint, ready flag, timeout
  and ICC settings. No current production renderer consumes it; configuration
  presence is not an executable or certified document capability.

## Owning follow-ups and decision boundary

`wi_1ebe5367-ed7b-4014-a327-3c0b5ed01b21` owns required MDCR form refusals and
the Level1 main path. It must preserve archive/deployment-local form identities,
submit through product forms and prove no mutation on invalid fields; a Level1
path receipt cannot imply documents or full journey.

`wi_ec434f93-7e1d-4b8e-96f6-a0d65c878bf9` owns ordered department children,
branch/timer behavior and journey assembly. Required server refusal covers the
authorized parent assignee with outstanding/rejected children and a named
non-assignee, asserting task/variable/link/history state and outbound captures
unchanged. Guard both form submission and task action completion in the same
transaction as mutation. Test two final children racing, duplicate post-complete,
parent cancellation mid-flight and late work after parent state disappears;
parent progression and effects must happen once. A UI eligibility flag is
insufficient. Completion still traverses child forms, not direct engine calls.

Additional bounded owners before document certification are:

- `wi_28f5d47c-d255-41a1-b52f-bc368439889f`: task/process content authorization
  and association, including refused actors with no mutation.
- `wi_f829384c-b92d-411c-b700-2e2ac298ad95`: obtain and record the product
  document/reporting decision below; no implementation before that decision.
- `wi_0e7624d0-7930-4925-9aaa-8ca6e4e25728`: implement the approved PDF and
  reporting capture slice, gated on that decision and secured content boundary.

The document decision must settle whether to retain historical HTML/Node PDF
generation and InsightFormPdf records or define a supported replacement; which
trusted render context and PDF merge/refresh behavior are required; when durable
rendering runs relative to workflow commit; and which reporting destination,
document identity and payload semantics are supported. Legacy source gives the
behavior above, but cannot choose the new product architecture. Administration
or PDC-005 policy is not inferred here.

After that decision, the bounded implementation must drive real rendering,
content storage and analytics processors into per-run isolated transports. Assert
nonempty valid PDF bytes, task/process/content identities, ordered merge and
exclusion, captured reporting payload containment, failures/rollback and retry
without duplicate or stale effects. Use the synthetic upload above; preserve
original fixture bytes. No live SMTP, directory/ACM mutation, repository upload,
metadata writes or live analytics index changes are needed. Full journey stays
PENDING until the relevant concrete assertion receipts pass.
