/** Copyright (c) 2026, Flatirons Solutions. All Rights Reserved. */
package com.flatirons.insight.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flatirons.insight.conf.security.SpringSecurityAuthenticationContext;
import com.flatirons.insight.domain.UserEntity;
import com.flatirons.insight.model.component.SimpleContentTypeMapper;
import com.flatirons.insight.model.records.security.InsightUserDetails;
import com.flatirons.insight.model.request.FormSubmitRequest;
import com.flatirons.insight.model.request.ProcessInstanceStartRequest;
import com.flatirons.insight.repository.FormFieldOptionRepository;
import com.flatirons.insight.repository.FormOptionSetRepository;
import com.flatirons.insight.repository.GroupRepository;
import com.flatirons.insight.repository.RelatedContentRepository;
import com.flatirons.insight.repository.TaskRepository;
import com.flatirons.insight.repository.UserRepository;
import com.flatirons.insight.service.editor.ArchiveFormResources;
import com.flatirons.insight.service.exception.BadRequestException;
import com.flatirons.insight.service.mapper.RelatedContentMapper;
import com.flatirons.insight.service.mapper.UserMapper;
import com.flatirons.insight.service.runtime.ArchiveTaskOptionService;
import com.flatirons.insight.service.runtime.ContentStorageServiceImpl;
import com.flatirons.insight.service.runtime.FormService;
import com.flatirons.insight.service.runtime.FormServiceImpl;
import com.flatirons.insight.service.runtime.ProcessInstanceServiceImpl;
import com.flatirons.insight.service.runtime.RelatedContentService;
import com.flatirons.insight.service.runtime.RelatedContentServiceImpl;
import com.flatirons.insight.service.runtime.UserCache;
import com.flatirons.insight.service.runtime.UserCacheImpl;
import com.flatirons.insight.service.runtime.UserServiceImpl;
import com.flatirons.insight.service.runtime.WorkflowTaskIdentityService;
import com.flatirons.insight.service.runtime.WorkflowDocumentScenarioSupport;
import com.flatirons.insight.service.runtime.listener.InsightIncrementTaskDateListener;
import com.flatirons.insight.service.runtime.listener.TaskAssignmentBean;
import com.flatirons.insight.service.runtime.listener.TaskDueDateBean;
import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import org.flowable.common.engine.api.identity.AuthenticationContext;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.ProcessEngine;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/** Original BPMN/forms plus synthetic membership. Retains every original handler, including unsupported scopes. */
@org.testcontainers.junit.jupiter.Testcontainers
class MdcrSyntheticArchiveBoundaryScenarioTest {
    @org.testcontainers.junit.jupiter.Container
    private static final org.testcontainers.containers.PostgreSQLContainer<?> POSTGRES =
            new org.testcontainers.containers.PostgreSQLContainer<>("postgres:16-alpine");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String OWNER = "admin@app.insight.com";
    private static final String SUBMITTED_EDGE = "sid-6E85BAFA-608C-46D5-B343-355E50A532C2";
    private static final String CANCELLED_EDGE = "sid-FEECC59B-BD1E-4808-A518-822D1A5BBFB3";
    private AuthenticationContext previous;
    private AnnotationConfigApplicationContext context;
    private ProcessEngine engine;
    private FormService forms;
    private UserEntity originator;
    private UserEntity supervisorActor;
    private CustomerContentCapture storageCapture;
    private RelatedContentService content;
    private WorkflowDocumentScenarioSupport documentSupport;
    private Task originate;
    private java.time.Clock notificationClock;
    private static final Instant MAIL_NOW = Instant.parse("2030-01-01T00:00:00Z");

    @BeforeEach void open(org.junit.jupiter.api.TestInfo testInfo) throws Exception {
        previous = Authentication.getAuthenticationContext();
        Authentication.setAuthenticationContext(new SpringSecurityAuthenticationContext());
        boolean documents = testInfo.getTags().contains("document-receipt");
        notificationClock = org.mockito.Mockito.mock(java.time.Clock.class);
        org.mockito.Mockito.when(notificationClock.instant()).thenReturn(MAIL_NOW);
        context = CustomerFirstWaitScenarioTest.open(true, notificationClock, true, POSTGRES, documents);
        if (documents) { documentSupport = new WorkflowDocumentScenarioSupport(JSON); }
        engine = context.getBean(ProcessEngine.class);
        engine.getProcessEngineConfiguration().getClock().setCurrentTime(Date.from(Instant.parse("2030-01-01T00:00:00Z")));
        String deploymentId = deployOriginal();
        var repositories = repositories();
        try (var em = context.getBean(EntityManagerFactory.class).createEntityManager()) {
            em.getTransaction().begin(); CustomerGoldenSeed.load("mdcr").seed(em); em.getTransaction().commit();
        }
        var sets = repositories.getRepository(FormOptionSetRepository.class);
        var options = repositories.getRepository(FormFieldOptionRepository.class);
        MdcrSyntheticBinding.seed(deploymentId, sets, options);
        installForms(repositories, sets, options);
        var users = installAssignments();
        originator = context.getBean(UserRepository.class).findByUserName(OWNER).orElseThrow();
        originator.setUpdatedDate(java.time.ZonedDateTime.now());
        originator = context.getBean(UserRepository.class).saveAndFlush(originator);
        supervisorActor = context.getBean(UserRepository.class).findByUserName("supervisor@insight.test").orElseThrow();
        authenticate(originator);
        var cache = new UserCacheImpl(context.getEnvironment(), users);
        context.getAutowireCapableBeanFactory().initializeBean(cache, "syntheticMdcrUserCache");
        cache.putUser(originator.getId(), new UserCache.CachedUser(users.findUserById(originator.getId()), List.of()));
        var starter = new ProcessInstanceServiceImpl(engine.getRuntimeService(), engine.getHistoryService(), cache, users,
                null, null, null, null, engine.getRepositoryService());
        var request = new ProcessInstanceStartRequest(); request.setProcessDefinitionKey("MDCR"); starter.startProcessInstance(request);
        originate = engine.getTaskService().createTaskQuery().singleResult();
        assertThat(originate.getTaskDefinitionKey()).isEqualTo("originate-mdcr");
    }

    @AfterEach void close() {
        try {
            try {
                try { if (engine != null) { engine.getProcessEngineConfiguration().getClock().reset(); } }
                finally { if (documentSupport != null) { documentSupport.close(); } }
            } finally { if (context != null) { context.close(); } }
        } finally {
            SecurityContextHolder.clearContext(); Authentication.setAuthenticationContext(previous);
        }
    }

    private String deployOriginal() throws Exception {
        byte[] archive;
        try (var input = getClass().getResourceAsStream("/scenarios/customers/mdcr/app.zip")) { archive = input.readAllBytes(); }
        assertThat(MdcrSyntheticBinding.sha256(archive)).isEqualTo("7ffe64c62463713346cf4c610235248c72c299a47c984da8b09cb3bb7ec0bf2b");
        byte[] binding = MdcrSyntheticBinding.bytes();
        var contracts = JSON.readTree(binding).path("forms");
        var deployment = engine.getRepositoryService().createDeployment();
        var aliases = new LinkedHashMap<String, String>();
        try (var zip = new ZipInputStream(new java.io.ByteArrayInputStream(archive))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals("bpmn-models/MDCR-21.bpmn20.xml")) {
                    byte[] bytes = zip.readAllBytes();
                    assertThat(MdcrSyntheticBinding.sha256(bytes)).isEqualTo("6063c14c2bf0c159a346599aec664168174a571dd00da97f632745c084e5c0a7");
                    deployment.addBytes("mdcr.bpmn20.xml", bytes);
                }
                if (entry.getName().startsWith("form-models/") && entry.getName().endsWith(".json")) {
                    String name = entry.getName().substring("form-models/".length(), entry.getName().length() - ".json".length());
                    int separator = name.lastIndexOf('-'); String key = "MDCR/" + name.substring(0, separator);
                    byte[] bytes = zip.readAllBytes(); aliases.put(name.substring(separator + 1), key);
                    assertThat(MdcrSyntheticBinding.sha256(bytes)).isEqualTo(contracts.path(key).path("sha256").asText());
                    deployment.addBytes(ArchiveFormResources.formResource(key), bytes);
                }
            }
        }
        assertThat(aliases).hasSize(9);
        return deployment.addString(ArchiveFormResources.MAPPINGS_RESOURCE, JSON.writeValueAsString(aliases))
                .addBytes(ArchiveTaskOptionService.RESOURCE, binding).deploy().getId();
    }

    private JpaRepositoryFactory repositories() {
        var repositories = new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(context.getBean(EntityManagerFactory.class)));
        var transactions = context.getBean(PlatformTransactionManager.class);
        repositories.addRepositoryProxyPostProcessor((proxy, information) -> {
            var attributes = new org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource();
            attributes.addTransactionalMethod("*", new org.springframework.transaction.interceptor.RuleBasedTransactionAttribute());
            proxy.addAdvice(new TransactionInterceptor(transactions, attributes));
        });
        return repositories;
    }
    private void installForms(JpaRepositoryFactory repositories, FormOptionSetRepository sets, FormFieldOptionRepository options) {
        var contents = repositories.getRepository(RelatedContentRepository.class);
        storageCapture = documentSupport == null ? new CustomerContentCapture() : documentSupport.content();
        var storage = new ContentStorageServiceImpl(context.getEnvironment(), contents,
                documentSupport == null ? storageCapture : documentSupport.storage(), Mappers.getMapper(RelatedContentMapper.class));
        content = transactional(new RelatedContentServiceImpl(storage, new SimpleContentTypeMapper(), contents,
                engine.getTaskService(), engine.getHistoryService(), repositories.getRepository(TaskRepository.class)), RelatedContentService.class);
        forms = transactional(new FormServiceImpl(engine.getTaskService(), engine.getRepositoryService(), engine.getRuntimeService(),
                engine.getHistoryService(), JSON, new ArchiveTaskOptionService(sets, options, engine.getRepositoryService(), JSON), content), FormService.class);
        if (documentSupport != null) {
            try { documentSupport.enable(context, repositories, contents, storage); }
            catch (Exception failure) { throw new IllegalStateException("Document fixture setup failed", failure); }
        }
    }
    private UserServiceImpl installAssignments() {
        var users = new UserServiceImpl(context.getBean(UserRepository.class), Mappers.getMapper(UserMapper.class),
                null, null, null, context.getBean(GroupRepository.class), context.getBean(EntityManagerFactory.class));
        // Only user selection is exercised here; group routing is still the real, refusing production listener.
        context.getBeanFactory().registerSingleton("taskAssignmentBean", new TaskAssignmentBean(new WorkflowTaskIdentityService(users, null), engine.getHistoryService()));
        context.getBeanFactory().registerSingleton("insightIncrementTaskDateListener", new InsightIncrementTaskDateListener());
        context.getBeanFactory().registerSingleton("taskDueDateBean", new TaskDueDateBean());
        return users;
    }
    private void authenticate(UserEntity user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new InsightUserDetails(user.getUserName(), user.getId(), List.of(), List.of()), null, List.of()));
    }
    private Map<String, Object> originateValues(String action) {
        return new LinkedHashMap<>(Map.ofEntries(Map.entry("action_originate_mdcr", action), Map.entry("form_level", "Level 1"),
                Map.entry("user_originator_supervisor", Map.of("id", supervisorActor.getId(), "name", "Spoofed display name")),
                Map.entry("safety_risk_mgmt_applied", "Yes"), Map.entry("change_type", "Preprint"), Map.entry("request_type", "Safety"),
                Map.entry("related_to_aa", "No"), Map.entry("priority_type", "Routine"), Map.entry("nomenclature", "MDCR golden nomenclature"),
                Map.entry("equipment_types", "CF34-10E"), Map.entry("requested_change", "Test change"), Map.entry("reason_for_change", "Test reason")));
    }
    private void submit(Task task, Map<String, Object> values) throws Exception {
        var request = new FormSubmitRequest(); request.setTaskId(task.getId()); request.setValues(values);
        forms.submitForm(JSON.readValue(JSON.writeValueAsBytes(request), FormSubmitRequest.class));
    }
    private Task reachSupervisor() throws Exception {
        submit(originate, originateValues("Submitted"));
        var task = engine.getTaskService().createTaskQuery().singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("supervisor-review");
        assertThat(task.getAssignee()).isEqualTo(com.flatirons.insight.conf.security.WorkflowUserIdentity.reference(supervisorActor.getId()));
        var finished = engine.getHistoryService().createHistoricActivityInstanceQuery().finished().list();
        assertThat(finished).extracting(org.flowable.engine.history.HistoricActivityInstance::getActivityId)
                .contains(SUBMITTED_EDGE).doesNotContain(CANCELLED_EDGE, "notify-mdcr-cancel", "reject-notification-to-originator");
        return task;
    }

    @ParameterizedTest @ValueSource(strings = {"Submitted", "Cancelled"})
    void originalSubmittedTimerAndCancelledRefusalRemainDistinct(String action) throws Exception {
        if ("Cancelled".equals(action)) {
            var before = snapshot();
            assertThatThrownBy(() -> submit(originate, originateValues(action)))
                    .hasStackTraceContaining("Missing typed notification rule MDCR/notify-mdcr-cancel/" + originate.getTaskDefinitionKey());
            assertUnchanged(before); assertNoOutbound();
            return;
        }
        Task supervisor = reachSupervisor();
        var timer = engine.getManagementService().createTimerJobQuery().singleResult();
        assertThat(Duration.between(supervisor.getCreateTime().toInstant(), timer.getDuedate().toInstant())).isEqualTo(Duration.ofDays(10));
        assertThat(new JdbcTemplate(context.getBean(javax.sql.DataSource.class)).queryForObject(
                "select repeat_ from act_ru_timer_job where id_=?", String.class, timer.getId())).isEqualTo("R20/2030-01-01T00:00:00Z/P10D");
        engine.getProcessEngineConfiguration().getClock().setCurrentTime(new Date(timer.getDuedate().getTime() - 1));
        assertThat(engine.getManagementService().createTimerJobQuery().duedateLowerThan(engine.getProcessEngineConfiguration().getClock().getCurrentTime()).count()).isZero();
        engine.getProcessEngineConfiguration().getClock().setCurrentTime(timer.getDuedate());
        var executable = engine.getManagementService().moveTimerToExecutableJob(timer.getId());
        var beforeNag = snapshot();
        assertThatThrownBy(() -> engine.getManagementService().executeJob(executable.getId()))
                .hasStackTraceContaining("Missing typed notification rule MDCR/no-supervisor-review-n-days/" + supervisor.getTaskDefinitionKey());
        // Native management execution records failed-job retry bookkeeping before failed delivery; this is NOT full rollback.
        String exceptionId = assertTimerBookkeeping(timer, "Missing typed notification rule MDCR/no-supervisor-review-n-days/");
        assertTimerBusinessUnchanged(beforeNag, timer.getExecutionId(), exceptionId);
        assertThat(engine.getTaskService().createTaskQuery().taskId(supervisor.getId()).count()).isEqualTo(1);
        assertThat(engine.getRuntimeService().getVariable(supervisor.getProcessInstanceId(), "n_days_supervisor_review")).isEqualTo(0L);
        assertNoOutbound();
    }
    private void assertTimerBusinessUnchanged(Map<String, List<Map<String, Object>>> before, String timerScopeId, String exceptionId) {
        var after = snapshot();
        var expectedScope = new LinkedHashMap<>(before.get("act_ru_execution").stream().filter(row -> timerScopeId.equals(row.get("id_"))).findFirst().orElseThrow());
        expectedScope.put("is_active_", false); expectedScope.put("job_count_", 0); expectedScope.put("timer_job_count_", 1);
        expectedScope.put("rev_", ((Number) expectedScope.get("rev_")).intValue() + 1);
        assertThat(after.get("act_ru_execution").stream().filter(row -> timerScopeId.equals(row.get("id_"))).findFirst().orElseThrow()).isEqualTo(expectedScope);
        assertThat(after.get("act_ge_bytearray").stream().filter(row -> exceptionId.equals(row.get("id_"))).toList()).hasSize(1);
        after.put("act_ge_bytearray", after.get("act_ge_bytearray").stream().filter(row -> !exceptionId.equals(row.get("id_"))).toList());
        for (var state : List.of(before, after)) {
            var executions = state.get("act_ru_execution");
            state.put("act_ru_execution", executions.stream().filter(row -> !timerScopeId.equals(row.get("id_"))).toList());
            state.remove("act_ru_timer_job");
        }
        before.forEach((table, rows) -> assertThat(after.get(table)).as(table).usingRecursiveComparison().isEqualTo(rows));
    }
    private String assertTimerBookkeeping(org.flowable.job.api.Job original, String expectedFailure) {
        var next = engine.getManagementService().createTimerJobQuery().singleResult();
        assertThat(next.getExecutionId()).isEqualTo(original.getExecutionId());
        assertThat(new JdbcTemplate(context.getBean(javax.sql.DataSource.class)).queryForObject(
                "select repeat_ from act_ru_timer_job where id_=?", String.class, next.getId())).isEqualTo("R20/2030-01-01T00:00:00Z/P10D");
        assertThat(Duration.between(original.getDuedate().toInstant(), next.getDuedate().toInstant())).isEqualTo(Duration.ofSeconds(10));
        assertThat(next.getRetries()).isEqualTo(original.getRetries() - 1);
        assertThat(engine.getManagementService().getTimerJobExceptionStacktrace(next.getId())).contains(expectedFailure);
        var jdbc = new JdbcTemplate(context.getBean(javax.sql.DataSource.class));
        assertThat(jdbc.queryForObject("select is_active_ from act_ru_execution where id_=?", Boolean.class, original.getExecutionId())).isFalse();
        return jdbc.queryForObject("select exception_stack_id_ from act_ru_timer_job where id_=?", String.class, next.getId());
    }

    @ParameterizedTest @ValueSource(strings = {"invalid-action", "unknown-person", "inactive-person", "intruder"})
    void actualOriginateRefusesInvalidSelectionsAndActorWithoutMutation(String fault) throws Exception {
        var values = originateValues("Submitted");
        if ("invalid-action".equals(fault)) { values.put("action_originate_mdcr", "No matching action"); }
        if ("unknown-person".equals(fault)) { values.put("user_originator_supervisor", Map.of("id", 999999)); }
        if ("inactive-person".equals(fault)) {
            supervisorActor.setAccountStatus(com.flatirons.insight.model.enums.status.UserAccountStatus.INACTIVE);
            context.getBean(UserRepository.class).saveAndFlush(supervisorActor);
        }
        if ("intruder".equals(fault)) { authenticate(supervisorActor); }
        var before = snapshot();
        var expected = "intruder".equals(fault) ? AccessDeniedException.class : "invalid-action".equals(fault) ? BadRequestException.class : org.flowable.common.engine.api.FlowableException.class;
        assertThatThrownBy(() -> submit(originate, values)).isInstanceOf(expected);
        assertUnchanged(before); assertNoOutbound();
    }

    @ParameterizedTest @ValueSource(strings = {"Approved", "Rejected", "Cancelled"})
    void actualSupervisorEdgesRefuseMissingRoutingOrNotificationWithoutMutation(String action) throws Exception {
        Task supervisor = reachSupervisor(); authenticate(supervisorActor);
        var before = snapshot();
        String expected = "Approved".equals(action) ? "Unsupported routing at MDCR/techlead-review"
                : "Missing typed notification rule MDCR/" + ("Rejected".equals(action) ? "reject-notification-to-originator/" : "notify-mdcr-cancel/") + supervisor.getTaskDefinitionKey();
        var values = supervisorValues(action);
        assertThatThrownBy(() -> submit(supervisor, values))
                .hasStackTraceContaining(expected);
        assertUnchanged(before); assertNoOutbound();
    }

    @ParameterizedTest @ValueSource(strings = {"Routine", "Critical"})
    void sourcePrioritySetsOnlyItsEstablishedSupervisorDueDate(String priority) throws Exception {
        var values = originateValues("Submitted"); values.put("priority_type", priority);
        submit(originate, values);
        var supervisor = engine.getTaskService().createTaskQuery().singleResult();
        assertThat(supervisor.getTaskDefinitionKey()).isEqualTo("supervisor-review");
        if ("Critical".equals(priority)) {
            assertThat(Duration.between(supervisor.getCreateTime().toInstant(), supervisor.getDueDate().toInstant())).isEqualTo(Duration.ofHours(48));
        } else { assertThat(supervisor.getDueDate()).isNull(); }
        assertNoOutbound();
    }

    @org.junit.jupiter.api.Test
    void sourceSupervisorConsultUsesSelectedSmeAndReturnsThroughRealForms() throws Exception {
        Task supervisor = reachSupervisor(); authenticate(supervisorActor);
        var sme = context.getBean(UserRepository.class).findByUserName("sme@insight.test").orElseThrow();
        var values = supervisorValues("Consult SME"); values.put("user_sme_supervisor_review", Map.of("id", sme.getId()));
        submit(supervisor, values);
        Task consult = engine.getTaskService().createTaskQuery().singleResult();
        assertThat(consult.getTaskDefinitionKey()).isEqualTo("supervisor-consult-sme");
        assertThat(consult.getAssignee()).isEqualTo(com.flatirons.insight.conf.security.WorkflowUserIdentity.reference(sme.getId()));
        var timer = engine.getManagementService().createTimerJobQuery().singleResult();
        assertThat(Duration.between(consult.getCreateTime().toInstant(), timer.getDuedate().toInstant())).isEqualTo(Duration.ofDays(2));
        authenticate(sme);
        values = supervisorValues("Consult SME"); values.remove("action_supervisor_review"); values.remove("supervisor_comments");
        values.put("action_supervisor_consult_sme", "Completed"); values.put("sme_comments_supervisor_consult_sme", "Source-backed SME response");
        submit(consult, values);
        var returned = engine.getTaskService().createTaskQuery().singleResult();
        assertThat(returned.getTaskDefinitionKey()).isEqualTo("supervisor-review");
        assertThat(returned.getAssignee()).isEqualTo(com.flatirons.insight.conf.security.WorkflowUserIdentity.reference(supervisorActor.getId()));
        assertThat(returned.getId()).isNotEqualTo(supervisor.getId());
        assertThat(engine.getRuntimeService().getVariable(returned.getProcessInstanceId(), "sme_comments_supervisor_consult_sme"))
                .isEqualTo("Source-backed SME response");
        assertThat(engine.getManagementService().createTimerJobQuery().list()).singleElement().satisfies(next ->
                assertThat(Duration.between(returned.getCreateTime().toInstant(), next.getDuedate().toInstant())).isEqualTo(Duration.ofDays(10)));
        assertThat(engine.getHistoryService().createHistoricTaskInstanceQuery().taskId(consult.getId()).singleResult().getEndTime()).isNotNull();
        assertNoOutbound();
    }

    private Map<String, Object> supervisorValues(String action) {
        var values = originateValues("Submitted"); values.remove("action_originate_mdcr"); values.remove("user_originator_supervisor");
        values.put("action_supervisor_review", action); values.put("supervisor_comments", "Source-backed branch test");
        return values;
    }

    @org.junit.jupiter.api.Test @org.junit.jupiter.api.Tag("document-receipt")
    void actualOriginateCompletionCapturesPdfReportsAndNextStepRefusalRollsBack() throws Exception {
        String filename = "flagship-mdcr-attachment.txt";
        byte[] bytes = ("scenario-upload:" + filename).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var attachment = new com.flatirons.insight.model.request.AttachmentRequest();
        attachment.setDisplayName(filename); attachment.setRelatedContent(true);
        long upload = content.createTaskRelatedContent(originate.getId(),
                new org.springframework.mock.web.MockMultipartFile("file", filename, "text/plain", bytes), attachment).getId();
        var contents = repositories().getRepository(RelatedContentRepository.class);
        assertThat(contents.findById(upload).orElseThrow().getFieldId()).isNull();
        var values = originateValues("Submitted"); values.put("mdcr_attachments", List.of(upload));
        var before = snapshot(); var writes = List.copyOf(storageCapture.writes());
        authenticate(supervisorActor);
        assertThatThrownBy(() -> submit(originate, values)).isInstanceOf(AccessDeniedException.class);
        assertUnchanged(before); assertThat(storageCapture.writes()).isEqualTo(writes);
        assertThat(documentSupport.renders()).isEmpty(); assertThat(documentSupport.reports()).isEmpty();
        authenticate(originator); submit(originate, values);
        assertThat(engine.getTaskService().createTaskQuery().singleResult().getTaskDefinitionKey()).isEqualTo("supervisor-review");
        assertThat(engine.getHistoryService().createHistoricTaskInstanceQuery().taskId(originate.getId()).singleResult().getEndTime()).isNotNull();
        var bound = contents.findById(upload).orElseThrow();
        assertThat(bound.getFieldId()).isEqualTo("mdcr_attachments");
        assertThat(bound.getTaskId()).isEqualTo(originate.getId()); assertThat(bound.getProcessId()).isEqualTo(originate.getProcessInstanceId());
        assertThat(bound.getName()).isEqualTo(filename); assertThat(bound.getMimeType()).isEqualTo("text/plain");
        try (var input = storageCapture.getObjectByStoreId(bound.getStoreId()).getContent()) { assertThat(input.readAllBytes()).isEqualTo(bytes); }
        assertThat(documentSupport.renders()).isEmpty(); assertThat(documentSupport.reports()).isEmpty();
        documentSupport.dispatch(originate.getId()); documentSupport.dispatch(originate.getId());
        assertThat(documentSupport.renders()).hasSize(1); assertThat(documentSupport.pdfs().count()).isEqualTo(1);
        var pdf = documentSupport.pdfs().findById(originate.getId()).orElseThrow();
        var stored = contents.findById(pdf.getRelatedContentId()).orElseThrow();
        assertThat(pdf.getTaskId()).isEqualTo(originate.getId()); assertThat(pdf.getGeneration()).isEqualTo(1);
        assertThat(pdf.getRelatedContentId()).isNotEqualTo(upload); assertThat(pdf.getCreatedAt()).isNotNull();
        assertThat(pdf.getProcessId()).isEqualTo(originate.getProcessInstanceId());
        assertThat(stored.getTaskId()).isEqualTo(originate.getId()); assertThat(stored.getProcessId()).isEqualTo(originate.getProcessInstanceId());
        assertThat(stored.getContentSource()).isEqualTo("insight-form-pdf");
        assertThat(stored.getName()).isEqualTo(originate.getName() + ".pdf");
        try (var input = storageCapture.getObjectByStoreId(stored.getStoreId()).getContent();
                var parsed = org.apache.pdfbox.Loader.loadPDF(input.readAllBytes())) {
            assertThat(parsed.getNumberOfPages()).isEqualTo(1);
            assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(parsed)).contains(originate.getName());
        }
        assertThat(documentSupport.indexed()).containsKeys("insight_mdcr:" + originate.getProcessInstanceId(), "insight_mdcr_task:" + originate.getId());
        var report = documentSupport.indexed().get("insight_mdcr_task:" + originate.getId()).path("payload");
        assertThat(report.path("taskId").asText()).isEqualTo(originate.getId());
        assertThat(report.path("assigneeFirstName").asText()).isEqualTo(originator.getFirstName());
        assertThat(report.path("assigneeLastName").asText()).isEqualTo(originator.getLastName());
        assertThat(report.path("processInstanceId").asText()).isEqualTo(originate.getProcessInstanceId())
                .isNotEqualTo(stored.getId().toString()).isNotEqualTo(engine.getRuntimeService().getVariable(originate.getProcessInstanceId(), "process_id").toString());
        assertThat(report.path("formFields").path("id_nomenclature").path("Nomenclature").asText()).isEqualTo("MDCR golden nomenclature");
        assertThat(documentSupport.reports()).hasSize(2);
        var render = documentSupport.renders().getFirst();
        assertThat(render.path("url").asText()).endsWith("/insightclient/print/task/" + originate.getId());
        assertThat(render.path("readyFlag").asText()).isEqualTo("__PRINT_READY__");
        assertThat(render.path("preload").path("task").path("id").asText()).isEqualTo(originate.getId());
        assertThat(render.path("preload").path("attachments").get(0).path("name").asText()).isEqualTo(filename);
        assertThat(render.path("preload").toString()).doesNotContain("actorId", "cookie", "storeId");
        before = snapshot(); writes = List.copyOf(storageCapture.writes());
        var reports = documentSupport.reports(); var renders = documentSupport.renders();
        authenticate(supervisorActor); Task supervisor = engine.getTaskService().createTaskQuery().singleResult();
        assertThatThrownBy(() -> submit(supervisor, supervisorValues("Approved"))).hasStackTraceContaining("Unsupported routing at MDCR/techlead-review");
        assertUnchanged(before); assertThat(storageCapture.writes()).isEqualTo(writes);
        assertThat(documentSupport.reports()).isEqualTo(reports); assertThat(documentSupport.renders()).isEqualTo(renders);
        assertThat(context.getBean(CustomerMailCapture.class).networkAttempts()).isZero();
    }

    private void notificationRules() {
        for (String[] binding : List.of(
                new String[]{"notify-mdcr-cancel", "originate-mdcr", "mdcr-workflow-cancelled.ftl", "${processInstanceName} - Workflow cancelled"},
                new String[]{"notify-mdcr-cancel", "supervisor-review", "mdcr-workflow-cancelled.ftl", "${processInstanceName} - Workflow cancelled"},
                new String[]{"reject-notification-to-originator", "supervisor-review", "mdcr-task-rejected.ftl", "${taskName} - Task rejected"},
                new String[]{"no-supervisor-review-n-days", "supervisor-review", "mdcr-supervisor-not-started.ftl", "REMINDER: No activity on Supervisor Review task for ${n_days_supervisor_review} days"})) {
            var rule = new com.flatirons.insight.domain.WorkflowNotificationRuleEntity();
            rule.setProcessDefinitionKey("MDCR"); rule.setActivityId(binding[0]); rule.setTaskKey(binding[1]);
            rule.setTemplateName(binding[2]); rule.setSubject(binding[3]); rule.setFrom("insight@example.invalid");
            rule.setRecipientSelector(binding[0].equals("no-supervisor-review-n-days")
                    ? com.flatirons.insight.domain.WorkflowNotificationRuleEntity.RecipientSelector.MDCR_ACTIVE_SUPERVISOR
                    : com.flatirons.insight.domain.WorkflowNotificationRuleEntity.RecipientSelector.MDCR_COMPLETED_ASSIGNEE_AND_STARTER);
            context.getBean(com.flatirons.insight.service.runtime.WorkflowNotificationService.class).saveRule(rule);
        }
    }

    @org.junit.jupiter.api.Test
    void productOriginateCancellationCommitsSourceRecipientsAndEscapedBoundedCopy() throws Exception {
        notificationRules();
        var maliciousName = "MDCR <img src=x onerror=alert(1)> & \"quoted\"";
        engine.getRuntimeService().setProcessInstanceName(originate.getProcessInstanceId(), maliciousName);
        var capture = context.getBean(CustomerMailCapture.class);
        capture.beforeDelivery(() -> {
            assertThat(engine.getHistoryService().createHistoricTaskInstanceQuery().taskId(originate.getId()).singleResult().getEndTime()).isNotNull();
            assertThat(engine.getTaskService().createTaskQuery().taskId(originate.getId()).count()).isZero();
            assertThat(context.getBean(com.flatirons.insight.repository.WorkflowNotificationOutboxRepository.class).count()).isEqualTo(1);
        });
        submit(originate, originateValues("Cancelled"));
        assertMail(originate, "notify-mdcr-cancel", originator.getEmail(), List.of(originator.getEmail()),
                maliciousName + " - Workflow cancelled", "Workflow \"MDCR &lt;img src=x onerror=alert(1)&gt; &amp; &quot;");
        assertThat(engine.getRuntimeService().createProcessInstanceQuery().count()).isZero();
        assertThat(engine.getHistoryService().createHistoricActivityInstanceQuery().finished().list())
                .extracting(org.flowable.engine.history.HistoricActivityInstance::getActivityId)
                .contains("notify-mdcr-cancel").doesNotContain("reject-notification-to-originator", "no-supervisor-review-n-days");
    }

    @org.junit.jupiter.api.Test
    void productSupervisorRejectionPreservesCommentsWithoutMailDisclosure() throws Exception {
        notificationRules(); Task supervisor = reachSupervisor(); authenticate(supervisorActor);
        var values = supervisorValues("Rejected"); values.put("supervisor_comments", "Private <script>alert(1)</script> supervisor comment");
        String name = "Serial# 1; MDCR golden nomenclature; CF34-10E";
        submit(supervisor, values);
        assertMail(supervisor, "reject-notification-to-originator", supervisorActor.getEmail(), List.of(originator.getEmail()),
                supervisor.getName() + " - Task rejected", "The supervisor of the workflow \"" + name + "\" rejected the MDCR.");
        assertThat(context.getBean(CustomerMailCapture.class).messages().getFirst().getContent().toString()).doesNotContain("Private", "script", "supervisor comment");
        assertThat(engine.getRuntimeService().getVariable(supervisor.getProcessInstanceId(), "supervisor_comments")).isEqualTo(values.get("supervisor_comments"));
        assertThat(engine.getTaskService().createTaskQuery().singleResult().getTaskDefinitionKey()).isEqualTo("originate-mdcr");
    }

    @org.junit.jupiter.api.Test
    void productSupervisorTimerUsesExactTenDayDeadlineAndDurableRetry() throws Exception {
        notificationRules(); Task supervisor = reachSupervisor();
        SecurityContextHolder.clearContext(); // Native timer delivery has no completing human actor.
        var timer = engine.getManagementService().createTimerJobQuery().singleResult();
        assertThat(Duration.between(supervisor.getCreateTime().toInstant(), timer.getDuedate().toInstant())).isEqualTo(Duration.ofDays(10));
        engine.getProcessEngineConfiguration().getClock().setCurrentTime(new Date(timer.getDuedate().getTime() - 1));
        assertThat(engine.getManagementService().createTimerJobQuery().duedateLowerThan(engine.getProcessEngineConfiguration().getClock().getCurrentTime()).count()).isZero();
        assertThat(context.getBean(CustomerMailCapture.class).messages()).isEmpty();
        engine.getProcessEngineConfiguration().getClock().setCurrentTime(timer.getDuedate());
        var executable = engine.getManagementService().moveTimerToExecutableJob(timer.getId());
        var capture = context.getBean(CustomerMailCapture.class); capture.failNext(1);
        engine.getManagementService().executeJob(executable.getId());
        var events = context.getBean(com.flatirons.insight.repository.WorkflowNotificationOutboxRepository.class);
        var event = events.findAll().getFirst();
        assertThat(event.isDelivered()).isFalse(); assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(capture.messages()).isEmpty();
        assertThat(engine.getTaskService().createTaskQuery().taskId(supervisor.getId()).count()).isEqualTo(1);
        assertThat(engine.getRuntimeService().getVariable(supervisor.getProcessInstanceId(), "n_days_supervisor_review")).isEqualTo(10L);
        var delivery = context.getBean(com.flatirons.insight.service.runtime.WorkflowNotificationDelivery.class);
        assertThat(event.getNextAttemptAt()).isEqualTo(MAIL_NOW.plusSeconds(30));
        org.mockito.Mockito.when(notificationClock.instant()).thenReturn(MAIL_NOW.plusSeconds(30).minusMillis(1));
        delivery.retryUnsent(); assertThat(capture.messages()).isEmpty();
        assertThat(events.findById(event.getEventKey()).orElseThrow().getAttempts()).isEqualTo(1);
        org.mockito.Mockito.when(notificationClock.instant()).thenReturn(MAIL_NOW.plusSeconds(30));
        delivery.retryUnsent(); delivery.dispatch(event.getEventKey());
        assertMail(supervisor, "no-supervisor-review-n-days", supervisorActor.getEmail(), List.of(),
                "REMINDER: No activity on Supervisor Review task for 10 days", "has not approved or rejected the MDCR in 10 days.");
        assertThat(events.findById(event.getEventKey()).orElseThrow().getAttempts()).isEqualTo(2);
        var next = engine.getManagementService().createTimerJobQuery().singleResult();
        assertThat(Duration.between(timer.getDuedate().toInstant(), next.getDuedate().toInstant())).isEqualTo(Duration.ofDays(10));
        engine.getProcessEngineConfiguration().getClock().setCurrentTime(next.getDuedate());
        var repeated = engine.getManagementService().moveTimerToExecutableJob(next.getId());
        engine.getManagementService().executeJob(repeated.getId());
        assertThat(engine.getRuntimeService().getVariable(supervisor.getProcessInstanceId(), "n_days_supervisor_review")).isEqualTo(20L);
        assertThat(capture.messages()).hasSize(2);
        var reminder = capture.messages().get(1);
        assertThat(reminder.getSubject()).isEqualTo("REMINDER: No activity on Supervisor Review task for 20 days");
        assertThat(reminder.getContent().toString()).contains("has not approved or rejected the MDCR in 10 days.");
        assertThat(reminder.getRecipients(jakarta.mail.Message.RecipientType.TO)).extracting(Object::toString).containsExactly(supervisorActor.getEmail());
        assertThat(reminder.getRecipients(jakarta.mail.Message.RecipientType.CC)).isNull();
        assertThat(reminder.getRecipients(jakarta.mail.Message.RecipientType.BCC)).isNull();
        String occurrence = supervisor.getProcessInstanceId() + "/no-supervisor-review-n-days/" + supervisor.getId();
        assertThat(events.findAll()).extracting(com.flatirons.insight.domain.WorkflowNotificationOutboxEntity::getEventKey)
                .containsExactlyInAnyOrder(occurrence + "/10", occurrence + "/20");
        for (var receipt : events.findAll()) {
            assertThat(receipt.isDelivered()).isTrue();
            var model = JSON.readTree(receipt.getPayload()).path("variables");
            assertThat(model.path("sourceTaskId").asText()).isEqualTo(supervisor.getId());
            assertThat(model.path("sourceProcessInstanceId").asText()).isEqualTo(supervisor.getProcessInstanceId());
            delivery.dispatch(receipt.getEventKey());
        }
        assertThat(capture.messages()).hasSize(2);
        assertThat(capture.networkAttempts()).isZero();
    }

    @ParameterizedTest @ValueSource(strings = {"invalid-recipient", "disabled-recipient"})
    void productBackgroundSupervisorTimerRefusesInvalidPersistedRecipientWithoutBusinessMutation(String fault) throws Exception {
        notificationRules(); Task supervisor = reachSupervisor();
        if (fault.equals("invalid-recipient")) { supervisorActor.setEmail("not a mailbox"); }
        else { supervisorActor.setAccountStatus(com.flatirons.insight.model.enums.status.UserAccountStatus.INACTIVE); }
        context.getBean(UserRepository.class).saveAndFlush(supervisorActor);
        SecurityContextHolder.clearContext();
        var timer = engine.getManagementService().createTimerJobQuery().singleResult();
        engine.getProcessEngineConfiguration().getClock().setCurrentTime(timer.getDuedate());
        var executable = engine.getManagementService().moveTimerToExecutableJob(timer.getId());
        var before = snapshot();
        String failure = fault.equals("invalid-recipient") ? "Notification requires a valid email" : "Unknown or inactive notification user";
        assertThatThrownBy(() -> engine.getManagementService().executeJob(executable.getId())).hasStackTraceContaining(failure);
        String exceptionId = assertTimerBookkeeping(timer, failure);
        assertTimerBusinessUnchanged(before, timer.getExecutionId(), exceptionId);
        assertThat(engine.getRuntimeService().getVariable(supervisor.getProcessInstanceId(), "n_days_supervisor_review")).isEqualTo(0L);
        assertThat(engine.getTaskService().createTaskQuery().taskId(supervisor.getId()).count()).isEqualTo(1);
        assertNoOutbound();
    }

    @ParameterizedTest @ValueSource(strings = {"missing-rule", "invalid-recipient", "disabled-recipient", "invalid-starter", "disabled-starter", "bad-template", "intruder"})
    void productSupervisorMailRefusesNamedInvalidActorsAndBindingsWithoutBusinessMutation(String fault) throws Exception {
        notificationRules(); Task supervisor = reachSupervisor(); authenticate(supervisorActor);
        var users = context.getBean(UserRepository.class);
        if (fault.equals("invalid-recipient")) { supervisorActor.setEmail("not a mailbox"); users.saveAndFlush(supervisorActor); }
        if (fault.equals("disabled-recipient")) { supervisorActor.setAccountStatus(com.flatirons.insight.model.enums.status.UserAccountStatus.INACTIVE); users.saveAndFlush(supervisorActor); }
        if (fault.equals("invalid-starter")) { originator.setEmail("starter@example.invalid\r\nBcc: intruder@example.invalid"); users.saveAndFlush(originator); }
        if (fault.equals("disabled-starter")) { originator.setAccountStatus(com.flatirons.insight.model.enums.status.UserAccountStatus.INACTIVE); users.saveAndFlush(originator); }
        var rules = context.getBean(com.flatirons.insight.repository.WorkflowNotificationRuleRepository.class);
        var rule = rules.findByProcessDefinitionKeyAndActivityIdAndTaskKey("MDCR", "reject-notification-to-originator", "supervisor-review").orElseThrow();
        if (fault.equals("missing-rule")) { rules.delete(rule); }
        if (fault.equals("bad-template")) { rule.setTemplateName("task-rejected.ftl"); rules.saveAndFlush(rule); }
        if (fault.equals("intruder")) { authenticate(originator); }
        var before = snapshot();
        assertThatThrownBy(() -> submit(supervisor, supervisorValues("Rejected")))
                .isInstanceOf(fault.equals("intruder") ? AccessDeniedException.class : RuntimeException.class);
        assertUnchanged(before); assertNoOutbound();
    }

    @org.junit.jupiter.api.Test @org.junit.jupiter.api.Tag("document-receipt")
    void productCancellationOuterRollbackDropsMailFormAndDocumentEffects() throws Exception {
        notificationRules(); var before = snapshot();
        var transaction = new org.springframework.transaction.support.TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        transaction.executeWithoutResult(status -> {
            try { submit(originate, originateValues("Cancelled")); }
            catch (Exception failure) { throw new IllegalStateException(failure); }
            assertThat(context.getBean(com.flatirons.insight.repository.WorkflowNotificationOutboxRepository.class).count()).isEqualTo(1);
            assertThat(context.getBean(CustomerMailCapture.class).messages()).isEmpty();
            status.setRollbackOnly();
        });
        assertUnchanged(before); assertNoOutbound();
        assertThat(documentSupport.renders()).isEmpty(); assertThat(documentSupport.reports()).isEmpty();
    }

    @org.junit.jupiter.api.Test
    void productSupervisorCancellationUsesCompletingAssigneeAndPersistedStarter() throws Exception {
        notificationRules(); Task supervisor = reachSupervisor(); authenticate(supervisorActor);
        String name = "Serial# 1; MDCR golden nomenclature; CF34-10E";
        submit(supervisor, supervisorValues("Cancelled"));
        assertMail(supervisor, "notify-mdcr-cancel", supervisorActor.getEmail(), List.of(originator.getEmail()),
                name + " - Workflow cancelled", "Workflow \"" + name + "\" cancelled.");
        assertThat(engine.getRuntimeService().createProcessInstanceQuery().count()).isZero();
    }

    private void assertMail(Task source, String activity, String to, List<String> cc, String subject, String body) throws Exception {
        var capture = context.getBean(CustomerMailCapture.class);
        assertThat(capture.messages()).hasSize(1);
        var message = capture.messages().getFirst();
        assertThat(message.getRecipients(jakarta.mail.Message.RecipientType.TO)).extracting(Object::toString).containsExactly(to);
        if (cc.isEmpty()) { assertThat(message.getRecipients(jakarta.mail.Message.RecipientType.CC)).isNull(); }
        else { assertThat(message.getRecipients(jakarta.mail.Message.RecipientType.CC)).extracting(Object::toString).containsExactlyElementsOf(cc); }
        assertThat(message.getRecipients(jakarta.mail.Message.RecipientType.BCC)).isNull();
        assertThat(message.getFrom()).extracting(Object::toString).containsExactly("insight@example.invalid");
        assertThat(message.getSubject()).isEqualTo(subject);
        assertThat(message.getContent().toString()).contains(body).doesNotContain("<img", "<script");
        assertThat(capture.networkAttempts()).isZero();
        var events = context.getBean(com.flatirons.insight.repository.WorkflowNotificationOutboxRepository.class);
        assertThat(events.findAll()).singleElement().satisfies(event -> {
            assertThat(event.getEventKey()).startsWith(source.getProcessInstanceId() + "/" + activity + "/" + source.getId());
            assertThat(event.isDelivered()).isTrue();
        });
        var event = events.findAll().getFirst();
        var model = JSON.readTree(event.getPayload()).path("variables");
        assertThat(model.path("sourceTaskId").asText()).isEqualTo(source.getId());
        assertThat(model.path("sourceProcessInstanceId").asText()).isEqualTo(source.getProcessInstanceId());
        context.getBean(com.flatirons.insight.service.runtime.WorkflowNotificationDelivery.class).dispatch(event.getEventKey());
        assertThat(capture.messages()).hasSize(1);
    }

    private <T> T transactional(T service, Class<T> type) {
        var proxy = new ProxyFactory(service);
        proxy.addAdvice(new TransactionInterceptor(context.getBean(PlatformTransactionManager.class), new AnnotationTransactionAttributeSource()));
        return type.cast(proxy.getProxy());
    }
    private void assertNoOutbound() {
        assertThat(storageCapture.writes()).isEmpty();
        assertThat(context.getBean(CustomerMailCapture.class).messages()).isEmpty();
        assertThat(context.getBean(CustomerMailCapture.class).networkAttempts()).isZero();
    }
    private void assertUnchanged(Map<String, List<Map<String, Object>>> before) {
        var after = snapshot();
        before.forEach((table, rows) -> assertThat(after.get(table)).as(table).usingRecursiveComparison().isEqualTo(rows));
    }
    private Map<String, List<Map<String, Object>>> snapshot() {
        var jdbc = new JdbcTemplate(context.getBean(javax.sql.DataSource.class));
        var result = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (String table : List.of("act_ru_task", "act_ru_variable", "act_ru_execution", "act_ru_timer_job", "act_ge_bytearray", "act_ru_identitylink", "act_hi_taskinst", "act_hi_varinst",
                "act_hi_actinst", "act_hi_procinst", "insight_related_content", "workflow_notification_outbox")) {
            var rows = jdbc.queryForList("select * from " + table + " order by 1");
            rows.forEach(row -> row.replaceAll((column, value) -> value instanceof byte[] bytes ? MdcrSyntheticBinding.sha256(bytes) : value));
            result.put(table, rows);
        }
        if (documentSupport != null) {
            for (String table : List.of("workflow_document_process", "workflow_document_job", "workflow_document_resource", "insight_form_pdf")) {
                result.put(table, jdbc.queryForList("select * from " + table + " order by 1"));
            }
        }
        return result;
    }
}
