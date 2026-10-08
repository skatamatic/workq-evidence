import sys,re
for line in sys.stdin:
 line=re.sub(r'\x1b\[[0-?]*[ -/]*[@-~]', '',line)
 if line.startswith('etc-client: '): line=line[len('etc-client: '):]
 if 'Executed ' in line and ' of 1642' in line and 'TOTAL:' not in line: continue
 sys.stdout.write(line)
 sys.stdout.flush()
