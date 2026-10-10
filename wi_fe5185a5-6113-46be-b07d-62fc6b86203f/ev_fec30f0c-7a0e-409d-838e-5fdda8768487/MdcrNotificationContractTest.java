/** Copyright (c) 2026, Flatirons Solutions. All Rights Reserved. */
package com.flatirons.insight.service.runtime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.flatirons.insight.domain.UserEntity;
import com.flatirons.insight.domain.WorkflowNotificationRuleEntity;
import java.util.Date;
import java.util.HashMap;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Additional context and private binding boundaries; real delivery stays in the archive scenario suite. */
class MdcrNotificationContractTest {
    private final WorkflowInitiatorIdentityService identities = mock(WorkflowInitiatorIdentityService.class);
    private final HistoryService history = mock(HistoryService.class);
    private final RuntimeService runtime = mock(RuntimeService.class);
    private final DelegateExecution execution = mock(DelegateExecution.class);
    private final HistoricTaskInstance task = mock(HistoricTaskInstance.class);
    private final HistoricTaskInstance parent = mock(HistoricTaskInstance.class);
    private final HistoricTaskInstanceQuery taskQuery = mock(HistoricTaskInstanceQuery.class);
    private final HistoricProcessInstance starter = mock(HistoricProcessInstance.class);
    private final ProcessInstance instance = mock(ProcessInstance.class);
    private final MdcrNotificationContract contract = new MdcrNotificationContract(identities, history, runtime);

    MdcrNotificationContractTest() {
        when(task.getId()).thenReturn("task"); when(task.getProcessInstanceId()).thenReturn("process");
        when(task.getProcessDefinitionId()).thenReturn("definition"); when(task.getName()).thenReturn("Supervisor Review");
        when(task.getAssignee()).thenReturn("assignee"); when(task.getTaskDefinitionKey()).thenReturn("supervisor-review");
        when(execution.getProcessInstanceId()).thenReturn("process"); when(execution.getProcessDefinitionId()).thenReturn("definition");
        when(history.createHistoricTaskInstanceQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId("parent")).thenReturn(taskQuery); when(taskQuery.singleResult()).thenReturn(parent);
        when(parent.getProcessInstanceId()).thenReturn("process"); when(parent.getProcessDefinitionId()).thenReturn("definition");
        when(parent.getAssignee()).thenReturn("parent-actor");
        var historicQuery = mock(HistoricProcessInstanceQuery.class);
        when(history.createHistoricProcessInstanceQuery()).thenReturn(historicQuery);
        when(historicQuery.processInstanceId("process")).thenReturn(historicQuery); when(historicQuery.singleResult()).thenReturn(starter);
        when(starter.getProcessDefinitionId()).thenReturn("definition"); when(starter.getStartUserId()).thenReturn("starter");
        var runtimeQuery = mock(ProcessInstanceQuery.class);
        when(runtime.createProcessInstanceQuery()).thenReturn(runtimeQuery);
        when(runtimeQuery.processInstanceId("process")).thenReturn(runtimeQuery); when(runtimeQuery.singleResult()).thenReturn(instance);
        when(instance.getName()).thenReturn("MDCR 123");
        when(identities.requireReference("assignee")).thenReturn(user(1L, "assignee@example.invalid"));
        when(identities.requireReference("parent-actor")).thenReturn(user(2L, "parent@example.invalid"));
        when(identities.requireReference("starter")).thenReturn(user(3L, "starter@example.invalid"));
        when(execution.getVariable("n_days_supervisor_review")).thenReturn(10);
    }

    private static UserEntity user(Long id, String email) {
        var user = new UserEntity(); user.setId(id); user.setEmail(email); return user;
    }

    private WorkflowNotificationRuleEntity rule(String activity) {
        var rule = new WorkflowNotificationRuleEntity(); rule.setActivityId(activity); rule.setTaskKey("supervisor-review");
        rule.setFrom("sender@example.invalid"); rule.setTemplateName("mdcr-workflow-cancelled.ftl");
        rule.setSubject("${processInstanceName} - Workflow cancelled");
        rule.setRecipientSelector(WorkflowNotificationRuleEntity.RecipientSelector.MDCR_COMPLETED_ASSIGNEE_AND_STARTER);
        return rule;
    }

    @Test void cancellationPreservesDurableParentThenStarterCc() {
        when(task.getParentTaskId()).thenReturn("parent");
        var request = contract.request(execution, task, rule(MdcrNotificationContract.CANCEL), new HashMap<>());
        assertThat(request.to()).containsExactly("assignee@example.invalid");
        assertThat(request.cc()).containsExactly("parent@example.invalid", "starter@example.invalid");
        assertThat(request.bcc()).isEmpty();
        assertThat(request.variables()).containsEntry("sourceTaskId", "task").containsEntry("sourceProcessInstanceId", "process");
        verify(execution, never()).setVariable(anyString(), any());
    }

    @ParameterizedTest @ValueSource(strings = {"missing-parent", "completed-parent", "foreign-parent-process", "foreign-parent-definition", "missing-starter", "foreign-starter-definition", "missing-process", "missing-task-name", "blank-task-name", "structured-metadata", "numeric-non-id-metadata", "structured-process-id"})
    void invalidPersistedContextAndMetadataRefuseBeforeBusinessProjection(String fault) {
        String activity = MdcrNotificationContract.CANCEL;
        if (fault.contains("parent")) { when(task.getParentTaskId()).thenReturn("parent"); }
        switch (fault) {
            case "missing-parent" -> when(taskQuery.singleResult()).thenReturn(null);
            case "completed-parent" -> when(parent.getEndTime()).thenReturn(new Date());
            case "foreign-parent-process" -> when(parent.getProcessInstanceId()).thenReturn("foreign");
            case "foreign-parent-definition" -> when(parent.getProcessDefinitionId()).thenReturn("foreign");
            case "missing-starter" -> when(history.createHistoricProcessInstanceQuery().processInstanceId("process").singleResult()).thenReturn(null);
            case "foreign-starter-definition" -> when(starter.getProcessDefinitionId()).thenReturn("foreign");
            case "missing-process" -> when(runtime.createProcessInstanceQuery().processInstanceId("process").singleResult()).thenReturn(null);
            case "missing-task-name" -> when(task.getName()).thenReturn(null);
            case "blank-task-name" -> when(task.getName()).thenReturn(" ");
            case "structured-metadata" -> { activity = MdcrNotificationContract.NAG; when(execution.getVariable("nomenclature")).thenReturn(java.util.List.of("untrusted")); }
            case "numeric-non-id-metadata" -> { activity = MdcrNotificationContract.NAG; when(execution.getVariable("nomenclature")).thenReturn(42); }
            case "structured-process-id" -> { activity = MdcrNotificationContract.NAG; when(execution.getVariable("process_id")).thenReturn(java.util.Map.of("id", 42)); }
            default -> throw new IllegalArgumentException(fault);
        }
        var binding = rule(activity);
        assertThatThrownBy(() -> contract.request(execution, task, binding, new HashMap<>())).isInstanceOf(IllegalArgumentException.class);
        verify(execution, never()).setVariable(anyString(), any()); verify(execution, never()).setTransientVariableLocal(anyString(), any());
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" "})
    void missingCancellationNameUsesApprovedFallback(String name) {
        when(instance.getName()).thenReturn(name);
        var request = contract.request(execution, task, rule(MdcrNotificationContract.CANCEL), new HashMap<>());
        assertThat(request.subject()).isEqualTo("Workflow (unknown process name) - Workflow cancelled");
        assertThat(request.variables()).doesNotContainKey("processInstanceName");
    }

    @ParameterizedTest @ValueSource(strings = {"selector", "template", "subject", "cancel-task", "reject-task", "nag-task"})
    void malformedPrivateBindingRefuses(String fault) {
        var binding = rule(MdcrNotificationContract.CANCEL);
        switch (fault) {
            case "selector" -> binding.setRecipientSelector(null);
            case "template" -> binding.setTemplateName("test-resource.ftl");
            case "subject" -> binding.setSubject("invented subject");
            case "cancel-task" -> binding.setTaskKey("techlead-review");
            case "reject-task" -> { binding.setActivityId(MdcrNotificationContract.REJECT); binding.setTaskKey("originate-mdcr"); }
            case "nag-task" -> { binding.setActivityId(MdcrNotificationContract.NAG); binding.setTaskKey("originate-mdcr"); }
            default -> throw new IllegalArgumentException(fault);
        }
        assertThatThrownBy(() -> MdcrNotificationContract.validate(binding)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void differentCompletingActorRefusesBeforeRecipientRequest() {
        when(taskQuery.taskId("task")).thenReturn(taskQuery); when(taskQuery.singleResult()).thenReturn(task);
        when(task.getEndTime()).thenReturn(new Date()); when(execution.getCurrentActivityId()).thenReturn(MdcrNotificationContract.REJECT);
        when(identities.requireCurrent()).thenReturn(user(999L, "intruder@example.invalid"));
        assertThatThrownBy(() -> contract.task(execution, "task")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verifyNoInteractions(runtime); verify(execution, never()).setVariable(anyString(), any());
    }
}
