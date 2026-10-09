const assert = require('node:assert/strict');
const path = require('node:path');
const { runBounded } = require(path.join(process.cwd(), 'tools/verification/bounded-process.cjs'));
(async () => {
  const result = await runBounded('npm', ['run', 'test:etc-client', '--', '--watch=false', '--skip-nx-cache', '--include=src/testing/forms-diagnostics.spec.ts'], {
    cwd: process.cwd(), env: { ...process.env, FORMS_DIAGNOSTIC_CANARY: 'allowance-leak', NX_DAEMON: 'false', NG_BUILD_MAX_WORKERS: '2' }, timeoutMs: 180000,
  });
  const output = result.output.replace(/\u001b\[[0-9;]*[a-zA-Z]/g, '');
  assert.equal(result.code, 1);
  assert.equal(result.timedOut, false);
  assert.equal(result.interrupted, false);
  assert.equal(result.signal, null);
  assert.match(output, /fails an unused allowance before a sibling starts FAILED/);
  assert.match(output, /must fail the owning test and command FAILED/);
  assert.match(output, /Expected forms console.error was not emitted: Leaked refusal/);
  assert.match(output, /Unexpected forms console.error: Leaked refusal/);
  assert.match(output, /TOTAL: 2 FAILED, 2 SUCCESS/);
  console.log('Allowance-leak canary verified: unused allowance and sibling leak both fail; TOTAL: 2 FAILED, 2 SUCCESS; exit 1.');
})().catch(error => { console.error(error); process.exitCode = 1; });
