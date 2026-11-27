import hashlib,json,os
from pathlib import Path
d=Path(os.environ['WQ_RELEASE_DIR'])
old=json.loads((d/'base/update.json').read_text())
assert old['versionCode']==163 and old['versionName']=='1.0.148'
assert old['sha256']==os.environ['WQ_BASE_SHA']
p=d/'signed/WordQuestAI-v1.0.150-signed-qa.apk'
with p.open('rb') as f: digest=hashlib.file_digest(f,'sha256').hexdigest()
m=dict(schemaVersion=1,tag='v1.0.150',versionCode=164,versionName='1.0.150',
       packageName='com.kelimebulmacasi.a1.practice.fresh',apkFile='WordQuestAI.apk',
       sizeBytes=p.stat().st_size,sha256=digest,notes=os.environ['WQ_NOTES'])
(d/'signed/update.json').write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n')
