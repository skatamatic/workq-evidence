# Safran Analysis: ten-script compatibility decision request

Status: discovery complete for the pinned fixture; **all ten migration decisions remain unapproved**. Recommendation: replace the observed behavior with bounded Java domain operations after approval; no general JavaScript engine, handler, registry, target policy or runtime migration is authorized by this record. Retain original source bytes as provenance. No whole task has evidence sufficient for retirement; retaining native script execution would require an explicit engine/security decision that is not recommended.

## Provenance and citation key

Target inspected HEAD: `504121469225f36334aef82e238e5b91b518c26f`. Legacy HEAD: `525716d6c337c5b007894e59cea555d7c051605c` (verified). Target fixture: `insight-service/src/test/resources/scenarios/customers/safran-analysis/app.zip`, byte-identical to pinned legacy `activiti-bpm-suite/activiti-app/src/test/resources/scenarios/customers/safran-analysis/app.zip`.

Archive SHA-256: `2a8d390d112c97303d5916ad3a1d33257a188a7cf4e481466ba4305841546019`. BPMN member: `bpmn-models/Safran Analysis workflow-20.bpmn20.xml`; SHA-256 `7cdf3d79b584db1caadcea08557efcc536c6934d13f822ff0be0d08757922f6c`. Each script hash below is SHA-256 of its XML-parser-decoded script text encoded UTF-8, including whitespace; it is not a hash of escaped XML or editor JSON. Inventory attachment includes complete script text/attributes; XML line citations refer to the original BPMN member. Published v20 is the source, not draft v21 (legacy README:7-9).

Legacy citation root is `/Users/kylewesley/repos/insight-activiti-bpm-suite/activiti-bpm-suite/` at the pinned SHA:
- J: `insight-app-ext/src/main/java/com/activiti/extension/bean/InsightJsonUtilListener.java`.
- D: `insight-app-ext/src/main/java/com/activiti/extension/bean/safran/InsightSafranDispatchListener.java`.
- V: `insight-app-ext/src/main/java/com/activiti/extension/bean/InsightVariableUtilsListener.java`.
- C: `activiti-app-logic/src/main/java/com/activiti/service/runtime/RelatedContentService.java`.
- Legacy tests: `activiti-app/src/test/java/com/flatironssolutions/insight/scenario/customers/safrananalysis/`; shortened ConditionalFlows, NotificationContent and MultiInstance mean `SafranAnalysis<name>ScenarioTest.java`; SafranAnalysis means `SafranAnalysis.java`.
- Legacy README: `docs/scenario-harness/customers/safran-analysis/README.md` relative to legacy repository root.

## Per-script decision matrix

Every entry has current disposition **needs-product-decision**, with the proposed disposition and specific approvals below. This separates a recommendation from an approved contract.

### 1. `mail_script` — Mail Script

Source: BPMN member:352; helper citations: D:305-322,551-559. Script SHA-256: `e5dfaaf46fd6e587080c18289a59009b479f9fd74e849e85d953c185f6141ac4`.

Inputs/dependencies: table_of_impact JSON string of {groupName,key}; tenant-resolved group membership/email.

Outputs/effects: groupMapString (raw HTML), analysisMailList (comma-space addresses); no SMTP in script.

Disposition: **needs-product-decision**. Replace after approval: trusted tenant/group recipient authority, HTML escaping, prototype-safe grouping, deterministic order, invalid input and stale-output handling. Keep async=true/exclusive=false; changing ignored disable_mail requires separate route approval.

### 2. `reassign-group-script-multi` — Reassign Group

Source: BPMN member:662; helper citations: V:340-343; J:93-99; legacy ConditionalFlows:163-184. Script SHA-256: `fc0ad32d4a9c178492cfccbba5a9f1a96a22472d0131a16b87fd3283ba507a40`.

Inputs/dependencies: assign_to_group; groupServiceImpl.getGroup ID lookup; stringValueOf; JSON inspect.

Outputs/effects: analyst_group_id/name via setVariable; inspect logging; lookup exceptions precede writes, but a null returned group causes failure after analyst_group_id is written; transaction rollback is unproven.

Disposition: **needs-product-decision**. Replace after approval: null/empty/unknown/non-tenant group must refuse with no mutation. OR guard is always true. Preserve nearest existing MI scope: setVariable is not setVariableLocal, and cannot be assumed process-root. Prove selected sibling changes and other sibling does not.

### 3. `process-analyst-task-script` — Process Analyst task variables

Source: BPMN member:677; helper citations: J:703-750; legacy SafranAnalysis:804-814. Script SHA-256: `bdc6ad8fc68d728447d2d8a5c5d85e82ec335f011664f6faaf77733fc4fd3f60`.

Inputs/dependencies: document_rfus String/object array, index 0; document,title,hyperlink,numCOCChanges,numOEMChanges,date,status,group.name,group.id.

Outputs/effects: execution-local analyst_document/title/hyperlink/coc/oem/date/status/group_name/group_id; activity_log empty (twice); analyst_action/result = Choose one....

Disposition: **needs-product-decision**. Replace after approval; retain local scope, scalar conversion and defaults. Missing/invalid array maps GET_JSON_ARRAY_VALUE_ERROR with errorMessage. No source-backed reason to retire.

### 4. `preprocess-json-files-script` — Preprocess list of highlights and merge report

Source: BPMN member:789; helper citations: J:118-162,277-299,899-930,954-983,994-1011,1050-1134; D:156-169,260-301,384-393,469-559. Script SHA-256: `52d928ac9311442623d95c3b3698fac6b33dd331984f57161b562613287808e2`.

Inputs/dependencies: first active listOfHighlights/documentMergeReport attachments; highlights node; merge keys key/document; LEAP metadata icv_activities + dynamic model/system map; exclusions C,I,T,B,R,M,E,S,CI; tenant groups.

Outputs/effects: list_of_highlights,document_merge_report,master_list JSON strings; dispatch row expansion/ICV/ATA/groupName/analysis_required; groupAssignmentValidationLog,hasGroupAssignmentErrors; helper errorMessage/BPMN errors.

Disposition: **needs-product-decision**. Replace after approval of metadata/routing authority and missing-input policy. Preserve last duplicate merge source wins, source overwrites target fields, nontext document drops, excluded activity filtering and Hors_Capa no-analysis. ReferenceData is storage, not policy approval.

### 5. `check-missing-group-script` — Clean master list

Source: BPMN member:811; helper citations: J:568-637; D:260-301. Script SHA-256: `5aa4a1e57659b07e8e5ec96a2f18d409b3b8df419b520d8e1556c2264fe0bd1c`.

Inputs/dependencies: first active upload_override_csv attachment; fixed 16-column header mapping; tenant groups.

Outputs/effects: master_list JSON string; validation log/error flag; CSV_PARSE_ERROR on helper failure.

Disposition: **needs-product-decision**. Replace after approval. Trim CSV headers/values, ignore unknown columns; missing file currently leaves stale master_list. Unknown group is a data flag rather than thrown BPMN error; do not invent error injections.

### 6. `create-data-tables-and-csv-script` — Create data tables and CSVs

Source: BPMN member:855; helper citations: J:354-404,498-560,1155-1158. Script SHA-256: `ca772c25546e0287e9f7a18bf25f4a807a5ddcd745cdfdbb6e8fd2b7433a06b9`.

Inputs/dependencies: master_list JSON string; fixed ordered 16-column map; docManager numeric String user ID; clock; content storage.

Outputs/effects: table_of_impact exact analysis_required=yes; no_analyst_required exact no; three persisted CSV attachments under master_list_csv/table_of_impact_csv/no_analyst_required_csv with timestamped names; not process variables containing CSV IDs.

Disposition: **needs-product-decision**. Replace after approval of trusted creator authorization, transactional content and retry deduplication. Preserve exact yes/no partition (other values excluded from both), column order/quoted fields. urt01_list/no_capa_list declarations and commented filters are inactive, not output.

### 7. `override-script` — Override Dispatch

Source: BPMN member:921; helper citations: J:568-637; D:260-301,372-375; C:338-355. Script SHA-256: `755a9b076704a08420c22a29915a75352e6992b337ea20563b0aa40babdde5a2`.

Inputs/dependencies: first active manager_override_master_list_csv attachment; reverse header map; tenant groups.

Outputs/effects: master_list and validation outputs; deletes first override attachment (renditions/row, physical store deletion after commit); override_master_distribution=false; manager_action=Choose one....

Disposition: **needs-product-decision**. Needs product decision on destructive upload retention/audit, missing attachment and replay semantics. Proposed replacement only after approval; no cleanup shortcut. Unused CSV title-map/declarations are inert.

### 8. `create_missing_group_master_list_script` — Build Master List CSV

Source: BPMN member:1003; helper citations: J:498-560,1155-1158. Script SHA-256: `6cc0f83abaa5dffdfc014570e0d2524ad0b4a646f625b4b6e83d70278269ab60`.

Inputs/dependencies: master_list; ordered 16-column map; docManager numeric String; clock/content.

Outputs/effects: persisted timestamped master_list_missing CSV attachment field master_list_mising_csv (exact misspelling); no process CSV ID write.

Disposition: **needs-product-decision**. Replace after approval; preserve misspelled field alias for published forms. Rename only with explicit consumer/live-state migration. Repeated calls create new attachments today.

### 9. `post-process-script` — Post process

Source: BPMN member:1074; helper citations: D:183-228,325-369,551-559. Script SHA-256: `572fb28c98d0fdb9d0e3665e2387569a177664e31a575a79bcc34261460e65ab`.

Inputs/dependencies: table_of_impact JSON string; publication_url; row document/groupName; tenant group map.

Outputs/effects: table_of_impact with document_url={hyperlink,displayText}, hyperlink, resolved group objects; table_of_impact_unresolved written only if nonempty; errorMessage/errors.

Disposition: **needs-product-decision**. Replace after approval of URL construction/encoding and stale unresolved clearing. Legacy appends literal &documentID= without encoding; missing groups remain in main table. Decide rejected actor/tenant behavior explicitly.

### 10. `analysis-workflow-end-script` — Build Distribution List and Subject

Source: BPMN member:1129; helper citations: D:305-322,551-559; legacy NotificationContent:37-56; SafranAnalysis:861-882. Script SHA-256: `c945414dce1d17e9ad821c4cb3898999463c2779b595531f505c50a5a08d9d2a`.

Inputs/dependencies: fixed SAE R1,R2,R3,R4,R5; tenant-resolved group emails; manualName.

Outputs/effects: finalEmailList trimmed case-sensitive deduplication; finalSubject=Update of doc + JS-coerced manualName; no SMTP in script.

Disposition: **needs-product-decision**. Needs product decision: use fixed group distributions or task-based recipients. Legacy content test asserts R4/R5 task recipients, not script lists; it injects task IDs. Proposed replacement after authority/subject null policy approval.

## Shared contracts and existing target seams

All ten declare JavaScript and autoStoreVariables=false. Only mail_script declares async=true and exclusive=false; do not serialize it or move it inline silently. Script temporaries are not persisted through auto-storage. setVariable updates an existing visible scope or creates a root variable; setVariableLocal explicitly creates local state. Reassignment must be tested with actual parallel MI ancestry, not a map mock. Original parallel MI uses documents/document_rfus; scope and task identity are behavior, not implementation detail (legacy README:26-36,177-180; MultiInstance:25-49).

Ordered export fields/header labels (all exports share this map): key→Data Module Code; title→Title; issueNumber→Issue Number; date→Publication Date; status→Status; filterId→Filter ID; filterText→Filter Text; icStatus→IC Status; rfu_type→RFU Type; rfu_label→RFU Label; rfu_text→RFU Reference; document→Document; icv→ICV; ataCode→ATA Code; groupName→Group; analysis_required→Analysis Required. Imports reverse these pairs. Helpers select the first active attachment via page size 1; lookup errors may yield empty/partial results (J:1021-1037). These behaviors need explicit compatibility decisions, not silent catch-and-ignore in replacement code.

Target code root `insight-service/src/main/java/com/flatirons/insight/service/runtime/`:
- `listener/InsightJsonUtilListener.java:22-39`: only Feedback extractDisplayTextToColumn; not Analysis parsing/merging/CSV.
- `GroupServiceImpl.java:99-100,215-219,413-414`: persisted group/name/ID/membership reads. No Analysis dispatch or recipient contract.
- `ReferenceDataServiceImpl.java:44-54,67-70`: persisted lookup, writable metadata. Storage is not trusted routing/recipient authorization.
- `ContentStorageServiceImpl.java:57,120` and `RelatedContentServiceImpl.java:147-152,295-297`: real attachment storage/read seams; upload permission TODO at149 must not be treated as workflow authorization.
- `WorkflowNotificationOutboxService.java:36-56,59-71`: enqueue in mandatory workflow transaction, after-commit dispatch, locked delivery. Actual transport is WorkflowMailService. Analysis must use durable isolated capture boundaries; scripts themselves only prepare data.
- `WorkflowNotificationService.java:21` and `WorkflowTaskRoutingService.java:39-46`: deliberately bounded Feedback scope, not an Analysis authority to widen implicitly.

Flowable has native ScriptTask but Java21 test classpath lacks JavaScript (docs/workflow-scenario-seams.md:70-79). Existing engine services and DelegateExecution support variables/local variables, service tasks, async jobs and native parallel MI; they do not implement metadata dispatch, CSV content operations or approved compatibility mappings. PE-23 (docs/design/script-task-migration/PE-23-Script-Task-Migration-Legacy-Script-Task-Replacement.md:18-34) expressly requires approved decisions before handlers/registry/mappings.

## Smallest bounded recommendation requiring approval

For new deployments only, recommend explicit Java operations for the pinned ten identities, reusing existing service/storage/outbox seams. No arbitrary-code engine, general registry or new script authoring. A separately hashed/provenanced transformed BPMN artifact may be needed after the approved target form is known; unchanged originals remain retained. Preserve IDs, routing topology, async/exclusive, local scopes, archive forms and refusal boundaries. Do not invent Administration/PDC-005 authoring policy.

Approve the ten proposed replacements as a batch only with explicit answers to these decisions:
1. Routing and email authority: trusted tenant-bound persisted groups/reference data; define group membership ownership, metadata lookup provenance and whether launch uses impact groups and finished uses fixed R1–R5, or an approved task-based selector. Legacy current-user-first tenant fallback must not leak cross-tenant data or disappear for async jobs.
2. Safety corrections: reject null/empty/unknown or wrong-tenant reassignment with zero writes; escape summary HTML/prototype-safe grouping; define valid manualName/subject; choose URL encoding and stale table/unresolved clearing rather than preserving deterministic defects.
3. CSV compatibility: approve exact schema/yes-no partition, authenticated creator replacing caller-supplied docManager authority, upload retention versus deletion, missing-file refusal, retry idempotency and transactional content cleanup. Preserve misspelled alias unless consumers are explicitly migrated.
4. Live-instance policy and owner: recommend existing definitions/executions remain on their original runtime; apply new behavior only to new separately versioned deployments. This is a proposed default, not approved policy. Inventory actual deployed definition IDs/versions, process IDs, outstanding async mail jobs, MI-local fields, pending forms, attachments and notification events before any migration. No production/live-instance inventory was accessed here; fixture presence does not prove live usage. If live migration is requested, require a separate approved rollback/drain/cutover design.
5. Preserve ignored parallel-gateway disable_mail semantics or approve a separate routing change. Existing route evidence does not permit interpreting the condition as a mail suppression guard.

After approval, create linked implementation slices (not created now): (A) trusted metadata/group dispatch + first-wait prerequisite with authenticated JWT/legacy start and named refused actor/tenant no mutation; (B) JSON/CSV/content operations with real uploaded bytes, exact rows/columns, unknown-group flags, rollback and replay/retention assertions; (C) MI initialization/reassignment with two concrete documents and selected-only group swap, sibling isolation, invalid actions rollback; (D) launch/finished notifications through approved recipient rules/outbox with original async pump, exact content/addresses, rollback and retry isolation; (E) migration artifact/version policy only if approved. Link each slice to this decision record/PE-23 and its relevant dependent item. No handler signature or mapping is settled yet.

## Evidence and honest limits

Attached runnable Node harness executes all ten original bodies with call-recording helper stubs in isolated contexts. Twenty cases pass: helper call order/constants; execution-local initialization; OR guard null/empty behavior; mail trim/dedup; malformed JSON throw; stale mail on non-string/non-array/absent input; prototype collision; unescaped HTML; override resets; misspelled attachment field; fixed five groups/null subject. It executes no Java/Flowable helpers, database, SMTP, HTTP, LDAP or content writes. It is language-level characterization, not a customer runtime receipt or JSR-223 parity. Legacy helper source establishes indirect effects; those still require real-service tests after approval.

Legacy two-document journey expectations are source-backed (README:50-65; MultiInstance:25-49). Legacy reassignment route test does not assert group swap or sibling isolation (ConditionalFlows:180-184). Legacy notification test injects analysis_launch_task/final_task_id and asserts task-based recipients, not the computed script lists (NotificationContent:37-56; SafranAnalysis:861-882). No waiver or injected listener error is carried forward as an assertion.

No repository production/test/fixture change, Maven run, PR or CI landing claim is made for this question-phase investigation. Fresh unique Surefire count: **not measured**; predecessor reported 1218, not rerun here. Current inventory exactly: **4 DEPLOYED, 11 ASSERTED, 13 PENDING** product dimensions; Analysis is **1 DEPLOYED, 0 ASSERTED, 6 PENDING**. All Analysis dimensions and both dependent completion capabilities stay pending while approval/implementation is missing. No acceptance receipt was promoted.
