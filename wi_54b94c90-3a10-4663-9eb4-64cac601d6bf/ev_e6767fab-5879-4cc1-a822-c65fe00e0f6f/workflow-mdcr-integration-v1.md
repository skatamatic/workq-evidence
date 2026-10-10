# MDCR integration contract v1 (experimental Next branch)

Engineering decision reviewed by **Root (captain), 2026-10-10**, under Kyle
Wesley's delegation to use best judgment and unblock
`wi_54b94c90-3a10-4663-9eb4-64cac601d6bf`. The updated durable request is the
policy authority. This is engineering-approved policy for
`kwesley/workflow-scenario-port`; implementation still requires normal PR review.
It is not a reviewed customer installation, membership receipt or customer journey
certification. No customer binding is bundled. Missing customer inputs refuse
installation rather than keeping the engineering decision unresolved.

## Version and source receipt

`MdcrIntegrationContract` is the machine-readable Java/JSON contract, independent
`contractVersion: 1`. Options retain existing runtime `version: 3` and resource
`insight-archive-task-options.json`. The immutable private deployment resource is
`insight-mdcr-integration-v1.json`. `mdcr/source-catalog-v1.json` pins only original
form identities, alias mapping and canonical flat choices, **not memberships**.
The disabled template is `mdcr-integration-v1.unbound.json`. Null means unknown;
turning its enabled flag on does not make it an installation.

Source revision: `525716d6c337c5b007894e59cea555d7c051605c` in the legacy suite,
MDCR README/ASSERTION-INVENTORY and `Mdcr.java`. Original customer archive SHA-256:
`7ffe64c62463713346cf4c610235248c72c299a47c984da8b09cb3bb7ec0bf2b`.
Original BPMN `bpmn-models/MDCR-21.bpmn20.xml` SHA-256:
`6063c14c2bf0c159a346599aec664168174a571dd00da97f632745c084e5c0a7`.
All nine original form hashes and stable aliases are in the source catalog;
original ZIP/form/seed bytes remain unchanged. Canonical choice vocabulary comes
from the existing bounded `mdcr-task-options-v2.json` receipt and seed manifest
(`0f4b3825644959d35379cd4d00a93831e1c81abc36fb7e8a3af87e1f18b116e4`);
it supplies no parent membership. Historical test SQL's 189 keys/567 values are
provenance only. Completed [packet v2](https://raw.githubusercontent.com/skatamatic/workq-evidence/main/wi_54b94c90-3a10-4663-9eb4-64cac601d6bf/ev_fda5eb67-062d-4ee7-a80b-a59ec9378dc8/contract-packet-v2.json)
remains the discovery receipt; no discovery conclusion has been relabelled as
customer approval.

## Private installation and scope

Only a trusted operator or deployment integration calls the Spring-managed
`MdcrIntegrationProvisioner.install(byte[] contractJson, Map<String, byte[]> sourceResources)`.
There is no HTTP endpoint, automatic bootstrap, public metadata fallback or
live directory/mail effect. The overload taking the typed record snapshots its
JSON and source bytes before validation. Strict file parsing rejects duplicate
JSON keys, unknown record properties/enums and trailing content. The caller owns
reviewing the actual private installation inputs; archive numeric pins are never
substituted for them.

The resource map contains exactly `mdcr.bpmn20.xml`,
`insight-form-mappings.json` and the nine `forms/MDCR/<form>.json` original
resources. All bytes and alias values must match the pinned catalog. v1's
compatibility limit is these source hashes; changed source/form vocabularies
require a deliberate contract/catalog version change. Child forms are separately
validated deployment resources and need not share this deployment.

Required `Scope` fields are an installation key, process key `MDCR`, and trusted
default engine tenant `""`. Every role and selection repeats that exact scope.
A new Flowable deployment ID namespaces both private resources and option rows;
installation keys are descriptive and do not serve as mutable global lookup keys.
Repeated installs create separate immutable deployments and separate row keys.
There is no update/reset/single-flight cache to poison newer state.

`Provenance` binds legacy revision/archive hashes, a private binding reference,
reviewer/date, `testOnly`, and `bindingSha256` (SHA-256 of the **ObjectMapper
serialized options node**, retaining field/array order). This receipt detects stale
options; it is not a signature or an external approval claim. The persisted
resource also contains all identity/route/child inputs. Review the whole contract,
not just the options digest. Test-only configurations are refused unless the
explicit isolated `mdcr-contract-test` profile is active. The profile is for test
harnesses only and must never be enabled for a customer installation.

Preflight validates the whole binding, original form/BPMN inventory, private active
identities, route vocabulary and actual child resource before creating a
DeploymentBuilder or writing option rows. Deployment creation, both resources,
six option sets and their choices use `insightTransactionManager`, shared by
Flowable and JPA. Existing `ArchiveTaskOptionService.resolve` verifies every
installed form against persisted choices before commit. Any materialization
failure rolls the deployment and partial rows back. A caller must use the managed
Spring bean so this transaction applies.

## Options

Every option-bearing form includes all six binding names and flat canonical choices for
`change_type`, `preprint_prime_shop`, `equipment`. Grouped fields are exactly
`request_type`, `priority_type`, `manual_document_type`, each containing explicit
ordered `{id,name}` arrays for both `Preprint` and `Publication`. Request and
priority arrays must be nonempty; optional manual arrays may deliberately be
`[]`. Missing/null is unknown and refuses. Manual remains visible for both
parents. Choices must preserve pinned canonical IDs, names and canonical relative
order. Duplicate, extra, unknown, relabelled or reordered choices refuse; all eight option-bearing
forms must agree on the single deployment-local membership document.

`parentSwitch: "clear-all-three"` preserves existing behavior in
`ArchiveFormDefinition`: changing parent clears all three values and labels;
same-request reselection works and unchanged-parent omission retains values.
Installation reuses the existing runtime source URL/form contract rather than
creating another options engine. Persisted sets use `<deploymentId>:mdcr:<field>`;
missing/ambiguous sets or rows that differ from the deployment resource refuse.
The existing synthetic v3 fixture remains explicitly test-only, including its
fabricated assignment of flat choices to both parents.

## Routing interface for the routing owner

`roles` is a list of `RoleBinding(role, scope, identity)`. Required source roles
are `OmtTechLead`, `OmtRcbSecretary`, `OmtEngineeringMD`; original pins
7/6/8 respectively are provenance, not current IDs. Each binding names a
positive durable `GROUP` ID plus the exact persisted name, type and active status.
`TechLeads` is a separate optional source-role binding, never an implicit alias
of `OmtTechLead`. v1 static routes consume the three original roles. A deployment
may explicitly map an original role to a durable group named TechLeads; the role
key remains OmtTechLead and the mapping must be supplied, never guessed.

`selections` lists exact selected-field lookup keys, scope and a nonempty ordered
allowlist of typed persisted identities (`PERSON` or `GROUP`, ID, exact username
or group name). Person IDs resolve through `WorkflowTaskIdentityService`, including
active status; group IDs resolve by ID and `GroupType.GROUP`, never `findByName`.
Missing, duplicate, ambiguous or mismatched entries refuse. GroupEntity has no
tenant field: explicit private deployment/tenant binding is required, and ordinary
membership is not tenant authority. Non-default execution remains unsupported
until trusted workflow principal propagation exists; caller tenants do not help.

All eight `routes` are required, with exact original task names (including source
newlines), unique task keys and explicit `fallback: REFUSE`:

- `supervisor-review`: PERSON_FIELD `user_originator_supervisor`.
- `supervisor-consult-sme`: PERSON_FIELD `user_sme_supervisor_review`.
- `techlead-consult-sme`: PERSON_FIELD `user_sme_techlead_review`.
- `techlead-implementation`: PERSON_FIELD `user_techlead_implementation_assignee`.
- `ort-review`: GROUP_FIELD `applicable_ort_group_techlead_review`.
- `techlead-review`: STATIC_ROLE `OmtTechLead`.
- `rcb-review`: STATIC_ROLE `OmtRcbSecretary`.
- `mng-dir-engr-review`: STATIC_ROLE `OmtEngineeringMD`.

Originate remains authenticated initiator assignment. Selected values are canonical
persisted IDs, not names or display labels. The routing implementation must load
only this process definition's deployment resource, re-resolve active identities
and exact scope on every execution, and refuse absent/ambiguous selection or
configuration **before** task, link, variable, history or outbox changes. There is
no default user, catch-all route or public-metadata lookup. Existing Feedback
`WorkflowTaskRoutingRuleEntity` is not a substitute: it lacks deployment scope.

## Child interface for the child owner

`ChildBinding` pins `techlead-implementation`,
`MDCR/FormTechLeadImplementation` and its original hash to an actual separate
child deployment ID, `FormApp/` form key and SHA-256. Child deployment must exist,
have default tenant, and contain parseable matching form bytes. v1 authority is
`CURRENT_ACTIVE_PARENT_ASSIGNEE`, dueDays 7 and durable order variable
`insight_subtask_position`. The installer validates linkage, not execution.

`MdcrChildAuthorityContract.requireCreator` is a pure boundary model: its
`TrustedActor` and `LockedParent` must be captured server-side from active
persisted identities and a locked/revision-checked transaction, never bound from
caller input. Capture real process/task/deployment IDs, process key and tenant,
current revision, current assignee, lifecycle/delegation/nesting and actual
resolved parent form key/hash. Only that active current assignee may create,
including a legitimately picker-assigned Supervisor. Owner, candidate,
participant, reader or generic admin has no implicit grant; an unassigned parent
must be legitimately claimed first. Wrong tenant, process/deployment, revision,
completed/suspended/delegated/cancelled parent, nesting or form mismatch refuses
before mutation. This intentionally narrows legacy parent-read permission and is
an explicit Next decision, not a claim about legacy behavior.

The child owner must lock and re-read active parent **and process**, load the
private binding from the actual parent deployment, revalidate child deployment
scope/form hash, and call this boundary while the lock is held. Keep separate
parent, child, process and form identities; record durable child order, use
engine-clock +7 days, and preserve source subtask metadata/action defaults from
`Mdcr.java` and `docs/workflow-mdcr-contracts.md`. A legitimate child assignee
completes its real form and triggers private persisted-state progression without
impersonating the parent assignee. This contract installs no child execution.

Merged PR108 (`84d010f7cf770d6da1f5eae8b20e8cede2778a16`) remains the ownership
metadata/raw-assignee dependency. The child owner must also refuse protected MDCR
unclaim and group-involvement mutation using the same persisted actor/tenant/
revision boundary. Caller IDs or labels never authorize ownership changes.
No broad Administration or permission redesign is part of this contract.

## Proof and downstream handoff

`MdcrIntegrationProvisionerTest` materializes the complete isolated
`mdcr-integration-test-only-v1.json` example with actual ephemeral deployment IDs,
active durable groups/people, a persisted-user DTO adapter and original FormApp
child bytes. The options receipt is computed over actual serialized input.
It proves exact resources/rows, two-deployment isolation, explicit optional
emptiness, deterministic preflight refusals and rollback after a real deployment
and partial row write. `MdcrChildAuthorityContractTest` proves named refused
actors/lifecycle/revision scope cannot pass the pure mutation boundary; this is
model proof, not child runtime execution or an HTTP-status receipt.
Existing archived task-form scenarios retain valid parent-switch/reselection and
persisted mismatch behavior. No image refresh: no UI change.

`wi_69c9d2bc` owns routing execution and live selected-field receipts after this
contract merges. `wi_70719746` owns child execution and protected ownership guards
after this contract and its merged identity dependencies. `wi_4bdec66b` still
requires actual reviewed customer installation bindings and concrete product
receipts. Neither the complete isolated example nor the private provisioning
proof certifies real customer membership, mail, analytics or a full MDCR journey.
Exact-head Jenkins/Sonar and independent Bitbucket approval remain landing gates;
no automatic merge is authorized.
