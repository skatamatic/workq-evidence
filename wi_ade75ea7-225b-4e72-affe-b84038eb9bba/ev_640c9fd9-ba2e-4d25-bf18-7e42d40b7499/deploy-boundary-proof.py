import os, subprocess, sys, json, tempfile, shutil
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path.cwd(); ns={'m':'http://maven.apache.org/POM/4.0.0'}
version=ET.parse(root/'pom.xml').find('m:properties/m:maven-deploy-plugin.version',ns).text
tail_versions=[]
for pom in ['container/docker/pom.xml','container/podman/pom.xml']:
    plugins=ET.parse(root/pom).findall('m:build/m:plugins/m:plugin',ns)
    declared=next(p.find('m:version',ns).text for p in plugins if p.find('m:artifactId',ns).text=='maven-deploy-plugin')
    tail_versions.append(version if declared=='${maven-deploy-plugin.version}' else declared)
base=root/'.workq/deploy-boundary-proof-real-gate';base.mkdir(exist_ok=True)
results=[]
for name,deferred,fault in [('immediate-late-failure',False,'gate'),('deferred-late-failure',True,'gate'),('deferred-skipped-tail-failure',True,'tail'),('deferred-success',True,'none')]:
    run=Path(tempfile.mkdtemp(prefix='workq-deploy-proof-',dir='/tmp'));repo=run/'published'; record=base/name;record.mkdir(exist_ok=True)
    parent=f'<project><modelVersion>4.0.0</modelVersion><groupId>local.proof</groupId><artifactId>root</artifactId><version>1.0</version><packaging>pom</packaging><modules><module>before</module><module>gate</module><module>docker-tail</module><module>podman-tail</module></modules><build><plugins><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-deploy-plugin</artifactId><version>{version}</version></plugin><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-install-plugin</artifactId><version>3.1.4</version></plugin></plugins></build></project>'
    (run/'pom.xml').write_text(parent)
    for module in ['before','gate','docker-tail','podman-tail']:
        path=run/module;path.mkdir(exist_ok=True)
        failure='<fail message="deliberate late validation refusal"/>' if fault=='tail' and module=='podman-tail' else '<echo>validation passed</echo>'
        if module=='gate':
            failure=f'<java classname="com.flatirons.insight.scenario.CustomerWorkflowHttpGate" fork="true" failonerror="true"><classpath><pathelement location="{root}/insight-rest/target/test-classes"/></classpath><arg value="{path}"/></java>'
            if fault!='gate':
                reports=path/'target/surefire-reports';reports.mkdir(parents=True)
                shutil.copyfile(root/'insight-rest/target/surefire-reports/TEST-com.flatirons.insight.scenario.CustomerWorkflowHttpSmokeTest.xml',reports/'TEST-com.flatirons.insight.scenario.CustomerWorkflowHttpSmokeTest.xml')
        plugins=f'<plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-antrun-plugin</artifactId><version>3.1.0</version><executions><execution><phase>verify</phase><goals><goal>run</goal></goals><configuration><target>{failure}</target></configuration></execution></executions></plugin>'
        if module.endswith('-tail'): plugins+=f'<plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-deploy-plugin</artifactId><version>{tail_versions[0 if module=='docker-tail' else 1]}</version><configuration><skip>true</skip></configuration></plugin>'
        (path/'pom.xml').write_text(f'<project><modelVersion>4.0.0</modelVersion><parent><groupId>local.proof</groupId><artifactId>root</artifactId><version>1.0</version></parent><artifactId>{module}</artifactId><packaging>pom</packaging><build><plugins>{plugins}</plugins></build></project>')
    cmd=['mvn','-s','/Users/kylewesley/repos/insight-next/.bootstrap/maven-settings.xml','-Dmaven.repo.local='+str(root/'.workq/maven-repository'),'-f',str(run/'pom.xml'),'deploy','-fae','-Dmaven.install.skip=true','-DdeployAtEnd='+str(deferred).lower(),'-DaltDeploymentRepository=proof::'+repo.as_uri()]
    result=subprocess.run(cmd,cwd=run,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    (record/'maven.log').write_text(result.stdout)
    published=sorted(str(p.relative_to(repo)) for p in repo.glob('**/*.pom')) if repo.exists() else []; all_files=sorted(str(p.relative_to(repo)) for p in repo.glob('**/*') if p.is_file()) if repo.exists() else []
    data={'case':name,'pluginVersion':version,'containerPluginVersions':tail_versions,'deployAtEnd':deferred,'exit':result.returncode,'publishedPoms':published,'publishedFiles':all_files};results.append(data);print(json.dumps(data),flush=True); shutil.rmtree(run)
(base/'results.json').write_text(json.dumps(results,indent=2)+'\n')
assert results[0]['exit']!=0 and results[0]['publishedPoms'], 'control must prove early publication'
assert results[1]['exit']!=0 and not results[1]['publishedFiles'], 'gate failure must publish nothing'
assert results[2]['exit']!=0 and not results[2]['publishedFiles'], 'skipped-tail validation failure must publish nothing'
assert results[3]['exit']==0 and len(results[3]['publishedPoms'])==3, 'successful build must actually publish all eligible projects'
