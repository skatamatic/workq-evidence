const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const inventory = JSON.parse(fs.readFileSync('/tmp/safran-analysis-scripts.json', 'utf8'));
function run(id, seed = {}) {
  const vars = {...seed}, local = {}, calls = [];
  const execution = {getVariable: key => vars[key] ?? null,
    setVariable: (key, value) => {vars[key] = value;},
    setVariableLocal: (key, value) => {local[key] = value;}};
  const helper = name => new Proxy({}, {get: (_, method) => (...args) => {
    calls.push([name, String(method), ...args.slice(1)]);
    if (method === 'getEmailDistributionListForGroup') return ' a@example.test, b@example.test,a@example.test ';
    if (method === 'stringValueOf') return String(args[0]);
    if (method === 'getValueFromJsonNodeArray') return args[2];
  }});
  const context = vm.createContext({execution,
    insightJsonUtilListener: helper('json'), insightSafranDispatchListener: helper('dispatch'),
    insightVariableUtilsListener: helper('variable'),
    groupServiceImpl: {getGroup: id => {calls.push(['group', 'getGroup', id]); return {getName: () => 'Chosen'};}}});
  let error;
  try {new vm.Script(inventory.scripts.find(s => s.id === id).script).runInContext(context, {timeout: 100});}
  catch (e) {error = e.name;}
  return {vars, local, calls, error};
}
let cases = 0;
function check(title, fn) {fn(); cases++; console.log('PASS', title);}
for (const s of inventory.scripts) check(`original script executes: ${s.id}`, () => {
  const r = run(s.id, {table_of_impact:'[{"groupName":"SAE R3","key":"D1"}]', assign_to_group:9, manualName:'SPM Full'});
  assert.equal(r.error, undefined);
  console.log(JSON.stringify({id:s.id, calls:r.calls, local:r.local, writes:Object.keys(r.vars).filter(k=> !['table_of_impact','assign_to_group','manualName'].includes(k))}));
});
check('launch mail trims and deduplicates addresses and row keys', () => {
  const r=run('mail_script',{table_of_impact:JSON.stringify([{groupName:'G',key:'D1'},{groupName:'G',key:'D1'}])});
  assert.equal(r.vars.analysisMailList,'a@example.test, b@example.test');
  assert.equal(r.calls.length,1); assert.equal(r.vars.groupMapString.match(/D1/g).length,1);
});
check('launch mail absent/non-string/non-array input leaves stale output', () => {
  for(const value of [null, [], '{}']) {const r=run('mail_script',{table_of_impact:value,analysisMailList:'stale'}); assert.equal(r.vars.analysisMailList,'stale'); assert.equal(r.calls.length,0);}
});
check('malformed launch JSON throws before helper calls', () => {const r=run('mail_script',{table_of_impact:'{'});assert.equal(r.error,'SyntaxError');assert.equal(r.calls.length,0);});
check('launch mail has prototype-key collision', () => {const r=run('mail_script',{table_of_impact:'[{"groupName":"__proto__","key":"D"}]'});assert.equal(r.error,'TypeError');});
check('launch summary embeds unescaped HTML', () => {const r=run('mail_script',{table_of_impact:'[{"groupName":"<img>","key":"<b>D</b>"}]'});assert.ok(r.vars.groupMapString.includes('<img>'));assert.ok(r.vars.groupMapString.includes('<b>D</b>'));});
check('null and empty reassignment both call getGroup (OR guard)', () => {for(const value of [null,'']) {const r=run('reassign-group-script-multi',{assign_to_group:value});assert.ok(r.calls.some(c=>c[1]==='getGroup'));}});
check('analyst initialization writes local scope only, duplicates activity_log', () => {const r=run('process-analyst-task-script');assert.equal(Object.keys(r.local).length,12);assert.equal(r.local.analyst_action,'Choose one...');assert.equal(Object.keys(r.vars).length,0);assert.equal(r.calls.length,9);});
check('override helper ordering and process resets', () => {const r=run('override-script');assert.deepEqual(r.calls.map(c=>c[1]),['createTableFieldFromCsvFile','validateGroupAssignment','removeCSVDocument']);assert.equal(r.vars.override_master_distribution,false);assert.equal(r.vars.manager_action,'Choose one...');});
check('missing-group output preserves misspelled variable', () => {const r=run('create_missing_group_master_list_script');assert.equal(r.calls[0].at(-1),'master_list_mising_csv');});
check('finished mail uses five fixed groups and JS null subject coercion', () => {const r=run('analysis-workflow-end-script');assert.deepEqual(r.calls.map(c=>c[2]),['SAE R1','SAE R2','SAE R3','SAE R4','SAE R5']);assert.equal(r.vars.finalSubject,'Update of doc null');});
console.log(`${cases} isolated characterization cases passed. Helpers are call-recording stubs; no Java, Flowable, SMTP, HTTP, LDAP, DB or content mutation executed. This proves script control flow only, not production parity or JSR-223 compatibility.`);
