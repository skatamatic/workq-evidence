# MDCR option dependency decision required

Item: wi_1ebe5367-ed7b-4014-a327-3c0b5ed01b21
Reviewed base: af9a93f (merged task-form validation PR72).
No runtime, fixture, or parity edits have been made. No tests or CI gates are claimed.

## Concrete missing contract

The unchanged `scenarios/customers/mdcr/app.zip`, entry
`form-models/FormOriginateMDCR-18.json`, declares `change_type` with
`child_id=request_type,priority_type,manual_document_type`. The three children
declare `/group?type=<field>&groupName=` option URLs. Required request and
priority inputs cannot be submitted through the current product service:
`ArchiveFormDefinition.java:145` explicitly refuses `Pending groupName/child_id
option rules`. This is deliberate, not an accidental validation regression.

`docs/workflow-task-form-contracts.md`, section Six explicit MDCR option mappings,
documents that the actual required Originate form cannot complete until
production dependency rules are supplied. The deployment artifact
`scenarios/customer-adapters/mdcr-task-options-v2.json` pins flat options and
does not declare parent-to-child membership. The supplied
`scenarios/customers/mdcr/seed-manifest.yaml:99-124` has flat choices only:
Preprint/Publication; Economic/Operational/Safety; Critical/Routine; Maintenance
Programs. It has no group membership. Assigning every child choice to both
parents would invent a relationship and defeat the intentional fail-closed rule.

Legacy snapshot: 525716d6c337c5b007894e59cea555d7c051605c.
`docs/scenario-harness/customers/mdcr/README.md:70-79` describes fixture
optionSources for placeholder-only fields. Its seed-manifest comment says
TaskFormSession falls through placeholder-only options to the flat lists.
That is a harness fallback, not a production group relationship receipt.
`customers/mdcr/Mdcr.java:1177-1190` requires actual Originate form completion;
the flagship then passes through Supervisor Review and TechLead Review.

## Decision requested

Supply the reviewed mapping from `change_type` (Preprint and Publication) to
the allowed request_type, priority_type and manual_document_type choices,
including behavior when the parent changes and an existing child selection is
no longer permitted. Alternatively identify the authoritative persisted source
of those relationships. This is required before defining the production
dependency adapter and its refusal tests; flat fallback cannot certify it.

## Other discovered constraints for the resumed implementation

The archive contains a trailing semicolon in the execution listener
`${execution.setVariable("n_days_supervisor_consult_sme", 0)};` in
`bpmn-models/MDCR-21.bpmn20.xml`. `ArchiveExecutionPolicy.java:83-97` rejects it
as malformed. A source-backed, separately hashed migration artifact and explicit
publication support are required; the original archive must remain unchanged.
This is distinct from the missing option membership decision.

The legacy flagship TechLead helper (`Mdcr.java:1290-1310`) injects process_id
as string `1` to disclose an implementation assignee. That helper cannot be
copied under this item's prohibition on raw variable injection; a resumed
implementation must establish the visibility contract through actual form and
process state.

The flagship (`MdcrFlagshipSuiteScenarioTest.java:90-173`) includes department
children and document receipts. Those remain separate named dependencies:
wi_ec434f93-7e1d-4b8e-96f6-a0d65c878bf9 (children/timers/journey assembly) and
wi_0e7624d0-7930-4925-9aaa-8ca6e4e25728 (documents). Neither may be bypassed
or advertised as proved by a main-path receipt.

The parity inventory remains unchanged: 4 DEPLOYED, 11 ASSERTED, 13 PENDING;
MDCR form-refusal, customer-path and full-journey remain PENDING.
