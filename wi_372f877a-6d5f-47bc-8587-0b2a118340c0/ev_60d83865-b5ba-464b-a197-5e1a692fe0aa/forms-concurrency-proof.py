import subprocess, os, sys, time, pathlib, re, json, tempfile, signal
roots=[pathlib.Path(p).resolve() for p in sys.argv[1:]]
phase=os.environ.get('FORMS_PROOF_PHASE','cold')
receipt_root=pathlib.Path('/tmp/forms-concurrency-receipts');receipt_root.mkdir(exist_ok=True)
processes=[]
for label,root in zip(['a','b'],roots):
 if phase=='cold' and (root/'.angular/cache').exists():
  backup=pathlib.Path(tempfile.mkdtemp(prefix='forms-cache-backup-'))/'cache'
  (root/'.angular/cache').rename(backup)
 config=receipt_root/f'{phase}-{label}.karma.cjs'
 config.write_text('module.exports = config => { require('+json.dumps(str(root/'karma.conf.js'))+')(config); config.set({logLevel:config.LOG_DEBUG,reporters:["dots"]}); };')
 command=['/usr/bin/arch','-arm64','/usr/bin/time','-l','npm','run','test-coverage','--','--watch=false','--skip-nx-cache','--include=src/app/main/components/forms/components/embedded-rich-text-field-host.component.spec.ts','--include=src/app/main/components/forms/components/rich-text-runtime-field.component.spec.ts','--include=src/testing/forms-diagnostics.spec.ts']
 env=dict(os.environ,NX_DAEMON='false',NG_BUILD_MAX_WORKERS='2',KARMA_LOG_LEVEL='debug');env.pop('FORMS_DIAGNOSTIC_CANARY',None)
 log=receipt_root/f'{phase}-{label}.log'; output=log.open('w')
 started=time.time();proc=subprocess.Popen(command,cwd=root,env=env,stdout=output,stderr=subprocess.STDOUT,start_new_session=True)
 processes.append((label,root,proc,output,log,started,command))
results=[]
for label,root,proc,output,log,started,command in processes:
 try: code=proc.wait(timeout=max(1,300-(time.time()-started)))
 except subprocess.TimeoutExpired:
  os.killpg(proc.pid,signal.SIGKILL);code=proc.wait()
 output.close();text=re.sub(r'\x1b\[[0-9;]*[A-Za-z]','',log.read_text())
 elapsed=re.search(r'([0-9.]+) real',text);elapsed=float(elapsed.group(1)) if elapsed else time.time()-started
 port=re.search(r'server started at http://localhost:(\d+)/',text)
 profile=re.search(r'--user-data-dir=([^\s]+)',text)
 count=re.search(r'TOTAL: (\d+) SUCCESS',text)
 if not count: count=re.search(r'Executed (\d+) of \d+ SUCCESS',text)
 reports=[str(p.resolve()) for p in (root/'coverage/etc-client').glob('run-*/lcov.info') if p.stat().st_mtime>=started]
 memory=re.search(r'(\d+)\s+maximum resident set size',text)
 results.append(dict(label=label,phase=phase,workspace=str(root),started=started,ended=started+elapsed,seconds=elapsed,exit=code,testCount=int(count.group(1)) if count else 0,port=port.group(1) if port else None,profile=profile.group(1) if profile else None,profileRemoved=not pathlib.Path(profile.group(1)).exists() if profile else False,reports=reports,angularCache=str((root/'.angular/cache').resolve()),nxCache=str((root/'.nx/cache').resolve()),maxResidentBytes=int(memory.group(1)) if memory else None,command=command,log=str(log)))
print(json.dumps(results,indent=2))
(receipt_root/f'{phase}.json').write_text(json.dumps(results,indent=2))
assert all(r['exit']==0 and r['testCount']==17 and r['reports'] and r['profileRemoved'] for r in results), 'Each run must execute 17 tests, write own report and release browser profile'
assert results[0]['port']!=results[1]['port'] and results[0]['profile']!=results[1]['profile'], 'Port/profile collision'
assert max(r['started'] for r in results)<min(r['ended'] for r in results), 'Runs did not overlap'
assert results[0]['angularCache']!=results[1]['angularCache'] and results[0]['nxCache']!=results[1]['nxCache'], 'Shared cache'
print('PROOF PASSED: overlapping real Nx/Karma runs, independent ports/profiles/reports/caches')
