import json,os,shutil
from pathlib import Path
d=Path(os.environ['WQ_RELEASE_DIR'])
r=json.loads((d/'delta/delta-report.json').read_text())
assert r['eligible'], 'Delta too large; retain current public release'
m=json.loads((d/'signed/update.json').read_text())
m['deltas']=[r['delta']]
for folder in ('signed','delta'):
    (d/folder/'update.json').write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n')
shutil.copyfile(d/'delta/delta-report.json',d/'signed/delta-report.json')
