# Notification retry timing investigation

Base: 504121469225f36334aef82e238e5b91b518c26f.

The original scenario writes PostgreSQL CURRENT_TIMESTAMP into the original poison row, then immediately invokes retryUnsent twice and expects delivered=true (original line 470). The production selector compares next_attempt_at <= JVM Instant.now(). When the database clock is ahead, neither call selects that row. Earlier poison failures also rely on a real 30-second interval remaining unexpired throughout all subsequent assertions.

CustomerFirstWaitTestConfiguration imports WorkflowNotificationOutboxService but does not import WorkflowNotificationDeliveryConfiguration. Its Flowable async executor is disabled. No managed notification scheduler runs in this context. The dedicated WorkflowNotificationDeliveryConfigurationTest covers retry scheduling and shutdown independently.

Controlled reproduction: fixed application clock at wall time minus 60 seconds; z-later timestamp explicitly eligible to isolate the original poison assertion; original poison reset still uses CURRENT_TIMESTAMP. The focused test fails at the original delivered assertion (shifted to line 471), expected true but false, 1 failure / 0 errors / 0 skips. The failing command and setup patch are retained on Reproduce.

Searched retained recall, evidence indexes and substantive work transcripts of the first-wait item wi_2499aed6-b2c0-44df-8107-1dab057239f5 and later FormApp items wi_d3638df2-24ef-49f4-9c05-6b4ea84f0ba9 / wi_4b0988d9-a19a-4764-a82f-a608f989e73a. The historical raw line-470 failure was not found. The new controlled reproduction establishes the reported failure path; the exact historical trigger remains unverified. Existing historical records were not changed.

Repair: an internal package-private Clock constructor; unchanged public production constructor uses Clock.systemUTC(). Both due selection and attempt deadlines use the same clock. Scenario fixture timestamps share that domain and advance explicitly through 29 / 30 / 60 seconds. Real PostgreSQL repository, row locking, transaction boundaries, outbox service and mail rendering remain enabled; only time and the per-run safe transport are controlled. Original rollback / pending / delivered / concurrent dispatch checks remain, with additional deadline, attempt-state and full 52-event recovery assertions. Production scheduling and retry policy are unchanged.

Customer source: legacy snapshot 525716d6c docs/scenario-harness/customers/safran-feedback/README.md and SafranFeedbackNotificationContentScenarioTest establish notification content expectations; this ticket changes no customer routing, form or content assertions. Current inventory stays 4 DEPLOYED / 11 ASSERTED / 13 PENDING.
