# Captain review findings

Reviewed source5d166356 against target3f04cd4a.

## 881259347

[insight-service/src/main/java/com/flatirons/insight/service/runtime/TaskActionServiceImpl.java:322](https://bitbucket.org/flatironssolutions/insight-server/pull-requests/101/_/diff#comment-881259347)

[HIGH] The private Analysis check here reaches claim, assign, and complete, but the REST-exposed `involveUser`, `unInvolveUser`, `involveGroup`, and `unInvolveGroup` methods still use `fetchTask()`, which only loads by ID. The group route can add a participant to a foreign Analysis task without an actor or tenant check, and an uninvolved member can add their own participant link before passing the task detail/comment involvement predicate. Please apply the persisted Analysis authority policy before these identity-link mutations and cover refusal with unchanged links.

## 881259348

[insight-service/src/main/java/com/flatirons/insight/service/runtime/RelatedContentServiceImpl.java:324](https://bitbucket.org/flatironssolutions/insight-server/pull-requests/101/_/diff#comment-881259348)

[MEDIUM] `isPrivateAnalysis(task)` becomes false as soon as the task completes because the active task query returns null. The historic attachment read path then checks only assignee and principal/engine tenant; a former assignee with a matching engine tenant can read or download private Analysis content after their persisted membership is disabled, while the task/comment readers revalidate membership. Please classify historic versioned Analysis tasks from immutable definition/process context and require current persisted membership before serving completed-task content.

## 881259349

[insight-service/src/test/java/com/flatirons/insight/scenario/WorkflowAnalysisOperationsScenarioTest.java:164](https://bitbucket.org/flatironssolutions/insight-server/pull-requests/101/_/diff#comment-881259349)

[MEDIUM] Please use at least two distinct document/group keys in a deliberate order and assert the resulting order and later sequence numbers. The current duplicate-row input yields one group and only `seq_number=1`, so this test remains green if grouping order or subsequent numbering regresses.

## 881259351

[insight-service/src/test/java/com/flatirons/insight/scenario/WorkflowAnalysisOperationsScenarioTest.java:62](https://bitbucket.org/flatironssolutions/insight-server/pull-requests/101/_/diff#comment-881259351)

[MEDIUM] Please exercise both projection and grouping expressions with a syntactically valid group ID that is not bound to this private Analysis tenant, and assert refusal with unchanged state. The existing absent/null/authorized and malformed-ID cases do not prove that `requireGroup` blocks a valid foreign group; removing that check would leave these carriers green.

