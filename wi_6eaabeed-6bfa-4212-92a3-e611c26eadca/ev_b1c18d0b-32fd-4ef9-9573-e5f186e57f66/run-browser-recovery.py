from pathlib import Path
import subprocess
jdk=Path('/Users/kylewesley/opt/jdk-21/Contents/Home/bin')
launcher=Path('.workq/maven-repository/org/junit/platform/junit-platform-launcher/1.12.2/junit-platform-launcher-1.12.2.jar').resolve()
cp=Path('/tmp/browser-test-classpath.txt').read_text()+':'+str(launcher)+':/tmp/browser-timeout-runner'
Path('/tmp/browser-timeout-runner').mkdir(exist_ok=True)
sources=['insight-rest/src/test/java/com/flatirons/insight/scenario/CustomerWorkflowHttpRuntime.java','insight-rest/src/test/java/com/flatirons/insight/scenario/InsightBrowserRuntimeTest.java']
subprocess.run([str(jdk/'javac'),'-cp',cp,'-d','insight-rest/target/test-classes',*sources],check=True)
subprocess.run([str(jdk/'javac'),'-cp',cp,'-d','/tmp/browser-timeout-runner','/tmp/RunBrowserRecovery.java'],check=True)
raise SystemExit(subprocess.run([str(jdk/'java'),'-Djava.awt.headless=true','-cp',cp,'RunBrowserRecovery']).returncode)
