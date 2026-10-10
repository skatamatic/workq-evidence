> **Completed autonomously by workq.** An agent interpreted the request, implemented this change and verified it; no human wrote the code.
> Review accordingly — the evidence below is what it produced to show the change is correct.
>
> worker `mac-studio-w1` · model `gpt-6.1-sol` · workq item `wi_6eaabeed-6bfa-4212-92a3-e611c26eadca`

## Ticket

Provide an isolated browser-reachable Insight test runtime for the dependent FormApp smoke. Target `kwesley/workflow-scenario-port` only. Owner navigation must use real authenticated workflow APIs; anonymous, invalid and nonassignee callers must remain refused on the owner's protected task/process reads and actions, with no refused mutation. A valid nonassignee retains access to their own user info. Real credential login UI and the ten client task forms remain outside this foundation.

## Story

The standalone test-classpath launcher reuses the existing socket harness, production security and workflow/user/app/task/process services. It publishes the pinned FormApp archive into a fresh PostgreSQL schema/container, issues signed per-run synthetic identities through an owner-readable loopback bootstrap, and emits a complete public readiness receipt. Only external ACM, mail and content transports are captured.

Captain direction kept production read authorization in prerequisite PR85, which is merged in the feature base. This change adds no production authorization or login contract. The existing customer HTTP and signature scenarios consume the extracted runtime too. The final branch is rebased onto feature target 0ed7016b; newly merged comment/preferences authorization coverage and its production controller/service wiring are preserved in the shared test harness.

## Evidence

### Before

Regression spec for startup token expiry: valid one-second duration must publish real authenticated readiness and then shut down/clean files normally.


<details><summary><b>Spec: shortest bounded launcher</b></summary>

```
InsightBrowserLauncherTest.shortestBoundedRunPublishesReadinessThenCleansUpOnTimeout executes the standalone JVM with duration=1, checks exit 0 and INSIGHT_BROWSER_READY, and checks ready/control cleanup. The before run reverts only test-runtime token startup headroom; the after run restores it. Same JUnit selector and command; real test-classpath server/filter/publication run. No production authorization change. Direct JUnit runner invokes the checked-in test and propagates its result; it does not echo or duplicate assertions.
```

</details>

Same new launcher JUnit test with the startup-headroom fix reverted; demonstrates real readiness failure on an otherwise valid one-second launch.


<details><summary><b>Shortest duration before startup headroom</b> — <code>python3 /tmp/run-browser-timeout.py</code> · <strong>FAILED</strong> · exited 1</summary>

```text
INFO: Pausing ProtocolHandler ["http-nio-127.0.0.1-auto-1-59138"]
Oct 09, 2026 7:04:26 P.M. org.apache.catalina.core.StandardService stopInternal
INFO: Stopping service [Tomcat]
Oct 09, 2026 7:04:26 P.M. org.apache.catalina.core.ApplicationContext log
INFO: Destroying Spring FrameworkServlet 'dispatcher'
Oct 09, 2026 7:04:26 P.M. org.apache.catalina.loader.WebappClassLoaderBase checkThreadLocalsForLeaks
WARNING: You need to add "--add-opens=java.base/java.lang=ALL-UNNAMED" to the JVM command line arguments to enable ThreadLocal memory leak detection. Alternatively, you can suppress this warning by disabling ThreadLocal memory leak detection.
Oct 09, 2026 7:04:26 P.M. org.apache.catalina.loader.WebappClassLoaderBase clearReferencesRmiTargets
WARNING: You need to add "--add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED" to the JVM command line arguments to enable RMI Target memory leak detection. Alternatively, you can suppress this warning by disabling RMI Target memory leak detection.
Oct 09, 2026 7:04:26 P.M. org.apache.coyote.AbstractProtocol stop
INFO: Stopping ProtocolHandler ["http-nio-127.0.0.1-auto-1-59138"]
Oct 09, 2026 7:04:26 P.M. org.apache.coyote.AbstractProtocol destroy
INFO: Destroying ProtocolHandler ["http-nio-127.0.0.1-auto-1-59138"]
19:04:26.189 [main] INFO org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean -- Closing JPA EntityManagerFactory for persistence unit 'default'
Exception in thread "main" org.opentest4j.AssertionFailedError: [POST /api/internal/workflow-archives => <!doctype html><html lang="en"><head><title>HTTP Status 401 – Unauthorized</title><style type="text/css">body {font-family:Tahoma,Arial,sans-serif;} h1, h2, h3, b {color:white;background-color:#525D76;} h1 {font-size:22px;} h2 {font-size:16px;} h3 {font-size:14px;} p {font-size:12px;} a {color:black;} .line {height:1px;background-color:#525D76;border:none;}</style></head><body><h1>HTTP Status 401 – Unauthorized</h1><hr class="line" /><p><b>Type</b> Status Report</p><p><b>Message</b> Invalid JWT token</p><p><b>Description</b> The request has not been applied to the target resource because it lacks valid authentication credentials for that resource.</p><hr class="line" /><h3>Apache Tomcat/10.1.50</h3></body></html>] 
expected: 200
 but was: 401
	at com.flatirons.insight.scenario.CustomerWorkflowHttpRuntime$HttpRun.request(CustomerWorkflowHttpRuntime.java:269)
	at com.flatirons.insight.scenario.InsightBrowserRuntime.publish(InsightBrowserRuntime.java:84)
	at com.flatirons.insight.scenario.InsightBrowserRuntime.main(InsightBrowserRuntime.java:48)
] 
expected: 0
 but was: 1
       com.flatirons.insight.scenario.InsightBrowserLauncherTest.shortestBoundedRunPublishesReadinessThenCleansUpOnTimeout(InsightBrowserLauncherTest.java:74)
       java.base/java.lang.reflect.Method.invoke(Method.java:580)
       java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
       java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
```

</details>

Real JUnit/HTTP regression fails on anonymous recovery204 versus actual401 after invalid cookie, before test control servlet filter mapping fix.


<details><summary><b>Invalid-cookie recovery regression before fix</b> — <code>python3 /tmp/run-browser-recovery.py</code> · <strong>FAILED</strong> · exited 1</summary>

```text
INFO: Pausing ProtocolHandler ["http-nio-127.0.0.1-auto-1-60097"]
Oct 09, 2026 7:44:03 P.M. org.apache.catalina.core.StandardService stopInternal
INFO: Stopping service [Tomcat]
Oct 09, 2026 7:44:03 P.M. org.apache.catalina.core.ApplicationContext log
INFO: Destroying Spring FrameworkServlet 'dispatcher'
Oct 09, 2026 7:44:03 P.M. org.apache.catalina.loader.WebappClassLoaderBase checkThreadLocalsForLeaks
WARNING: You need to add "--add-opens=java.base/java.lang=ALL-UNNAMED" to the JVM command line arguments to enable ThreadLocal memory leak detection. Alternatively, you can suppress this warning by disabling ThreadLocal memory leak detection.
Oct 09, 2026 7:44:03 P.M. org.apache.catalina.loader.WebappClassLoaderBase clearReferencesRmiTargets
WARNING: You need to add "--add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED" to the JVM command line arguments to enable RMI Target memory leak detection. Alternatively, you can suppress this warning by disabling RMI Target memory leak detection.
Oct 09, 2026 7:44:03 P.M. org.apache.coyote.AbstractProtocol stop
INFO: Stopping ProtocolHandler ["http-nio-127.0.0.1-auto-1-60097"]
Oct 09, 2026 7:44:03 P.M. org.apache.coyote.AbstractProtocol destroy
INFO: Destroying ProtocolHandler ["http-nio-127.0.0.1-auto-1-60097"]
19:44:03.773 [main] INFO org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean -- Closing JPA EntityManagerFactory for persistence unit 'default'

Test run finished after 23304 ms
[         2 containers found      ]
[         0 containers skipped    ]
[         2 containers started    ]
[         0 containers aborted    ]
[         2 containers successful ]
[         0 containers failed     ]
[         1 tests found           ]
[         0 tests skipped         ]
[         1 tests started         ]
[         0 tests aborted         ]
[         0 tests successful      ]
[         1 tests failed          ]


Failures (1):
  JUnit Jupiter:InsightBrowserRuntimeTest:cookieIdentityRealQueriesRefusalsAndFreshSchemaCleanup()
    MethodSource [className = 'com.flatirons.insight.scenario.InsightBrowserRuntimeTest', methodName = 'cookieIdentityRealQueriesRefusalsAndFreshSchemaCleanup', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: 
expected: 204
 but was: 401
       com.flatirons.insight.scenario.InsightBrowserRuntimeTest.cookieIdentityRealQueriesRefusalsAndFreshSchemaCleanup(InsightBrowserRuntimeTest.java:89)
       java.base/java.lang.reflect.Method.invoke(Method.java:580)
       java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
       java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
```

</details>

### After

Same checked-in launcher JUnit selector and command, with per-run startup headroom restored; one-second run must publish authenticated readiness then cleanly expire and remove receipts.


<details><summary><b>Shortest duration after startup headroom</b> — <code>python3 /tmp/run-browser-timeout.py</code> · exited 0 (passed)</summary>

```text
Note: insight-rest/src/test/java/com/flatirons/insight/scenario/CustomerWorkflowHttpRuntime.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.

Test run finished after 24042 ms
[         2 containers found      ]
[         0 containers skipped    ]
[         2 containers started    ]
[         0 containers aborted    ]
[         2 containers successful ]
[         0 containers failed     ]
[         1 tests found           ]
[         0 tests skipped         ]
[         1 tests started         ]
[         0 tests aborted         ]
[         1 tests successful      ]
[         0 tests failed          ]
```

</details>

Same actual JUnit/HTTP command: invalid API401 remains, capability clears cookie and restores owner200; bad-cap/origin403 do not signal shutdown; valid-cap shutdown204 succeeds with invalid cookie.


<details><summary><b>Invalid-cookie recovery regression after fix</b> — <code>python3 /tmp/run-browser-recovery.py</code> · exited 0 (passed)</summary>

```text
	at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:72)
	at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:342)
	at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:399)
	at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:63)
	at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:903)
	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1774)
	at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:52)
	at org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:973)
	at org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:491)
	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:63)
	at java.base/java.lang.Thread.run(Thread.java:1583)
Oct 09, 2026 7:44:49 P.M. org.apache.coyote.AbstractProtocol pause
INFO: Pausing ProtocolHandler ["http-nio-127.0.0.1-auto-2-62494"]
Oct 09, 2026 7:44:49 P.M. org.apache.catalina.core.StandardService stopInternal
INFO: Stopping service [Tomcat]
Oct 09, 2026 7:44:49 P.M. org.apache.catalina.core.ApplicationContext log
INFO: Destroying Spring FrameworkServlet 'dispatcher'
Oct 09, 2026 7:44:49 P.M. org.apache.catalina.loader.WebappClassLoaderBase checkThreadLocalsForLeaks
WARNING: You need to add "--add-opens=java.base/java.lang=ALL-UNNAMED" to the JVM command line arguments to enable ThreadLocal memory leak detection. Alternatively, you can suppress this warning by disabling ThreadLocal memory leak detection.
Oct 09, 2026 7:44:49 P.M. org.apache.catalina.loader.WebappClassLoaderBase clearReferencesRmiTargets
WARNING: You need to add "--add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED" to the JVM command line arguments to enable RMI Target memory leak detection. Alternatively, you can suppress this warning by disabling RMI Target memory leak detection.
Oct 09, 2026 7:44:49 P.M. org.apache.coyote.AbstractProtocol stop
INFO: Stopping ProtocolHandler ["http-nio-127.0.0.1-auto-2-62494"]
Oct 09, 2026 7:44:49 P.M. org.apache.coyote.AbstractProtocol destroy
INFO: Destroying ProtocolHandler ["http-nio-127.0.0.1-auto-2-62494"]
19:44:49.866 [main] INFO org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean -- Closing JPA EntityManagerFactory for persistence unit 'default'

Test run finished after 24380 ms
[         2 containers found      ]
[         0 containers skipped    ]
[         2 containers started    ]
[         0 containers aborted    ]
[         2 containers successful ]
[         0 containers failed     ]
[         1 tests found           ]
[         0 tests skipped         ]
[         1 tests started         ]
[         0 tests aborted         ]
[         1 tests successful      ]
[         0 tests failed          ]
```

</details>


Full test-enabled Maven install on pushed **347186a7f24049c607a86bf1d686eec96a5dff27** passed **2,222 tests, zero failures/errors/skips**, in 38m32s: [full command receipt](http://127.0.0.1:9876/evidence/ev_52318225-552d-423b-a238-78d0006c49fe), [actual Surefire totals](http://127.0.0.1:9876/evidence/ev_e07922fc-58a6-44d0-8134-c1211e0a4725). All 13 preserved customer HTTP, three launcher, two cookie runtime and 28 signature cases passed. [Production WAR inspection](http://127.0.0.1:9876/evidence/ev_d00cd6cf-ae43-4d04-be2a-bc8bdf62cac2) scanned 98,791 entries including nested jars and found no test runtime/capture classes or session route.

Mandatory correctness, pattern and tests review plus two focused follow-ups converged with no open findings; [final reconciliation review](http://127.0.0.1:9876/evidence/ev_d954b458-6e2c-4988-80e3-f8fec7f34f3b). **Exact-head Jenkins/Sonar receipts remain pending with the captain; this prepared body is not an opened PR.**

### Tests added

Cookie owner user/app/process-definition/task/process query and form/detail navigation; nonassignee hidden queries/refused details; anonymous/invalid/nonassignee action refusals with full persistence/capture snapshots unchanged; accepted/refused browser origins; invalid-cookie reset/owner recovery and capability-protected shutdown; repeated fresh schemas and normal/error cleanup; standalone readiness, private control permissions, shutdown, receipt-write failure and shortest supported timeout.

### Vision verification

This is test infrastructure. No client UI journey or credential login UI was exercised. The Vite proxy example is an integration recipe for the dependent smoke.

## Implementation notes

Resolved RepoPolicy gate: **Build and tests**, using a full test-enabled Maven install. Separate Typecheck, Lint & format, Integration, Build and E2E gate labels are omitted by policy; the composite gate still builds, tests and packages the reactor. No client browser/vision tier is claimed.

- `CustomerWorkflowHttpRuntime` retains the merged authorization and signature wiring while sharing the real socket runtime.
- `InsightBrowserRuntime` accepts readiness/control paths and a bounded duration, binds only to `127.0.0.1`, publishes readiness atomically without replacement, and removes only owned files.
- Every production controller retains the real security chain. The separate test control servlet checks loopback/host/Origin/capability independently, allowing invalid-cookie recovery without weakening API authentication.
- The private control file carries the session capability. Bootstrap before trace capture; retain neither the capability nor Set-Cookie in traces.
- The launch guide supplies the test classpath command and explicit same-origin client proxy configuration.
- Production WAR inspection recursively checks class names and bytes in all nested jars for the runtime, captures and session route.

## Risk

**3/10** — test infrastructure only; failure can mislead browser acceptance or leak a synthetic session capability. Production Java/dependencies are unchanged, and WAR absence is asserted. Real ACM/SMTP/content services, real credential login UI and all ten client task forms remain untested here. SIGKILL cannot run Java cleanup. The readiness source HEAD is not a clean-worktree certificate. Landing still requires independent human approval and exact-head Jenkins/Sonar.
