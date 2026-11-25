#!/usr/bin/env python3
"""Strictly validate matching, eligible delta artifacts and exact release base."""
import hashlib,json,os,re,shutil,sys,zipfile
from pathlib import Path
d=Path(os.environ['WQ_RELEASE_DIR'])
base=int(os.environ['WQ_BASE_CODE']);target=int(os.environ['WQ_TARGET_CODE'])
version=os.environ['WQ_VERSION'];previous=os.environ['WQ_BASE_VERSION']
assert re.fullmatch(r'\d+\.\d+\.\d+',version) and re.fullmatch(r'\d+\.\d+\.\d+',previous)
assert tuple(map(int,version.split('.')))>tuple(map(int,previous.split('.'))) and target>base
def read(p):return json.loads(p.read_text())
def one(root,name):
    candidates=list(root.rglob(name));assert len(candidates)==1,(root,name,candidates)
    return candidates[0]
def sha(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def verify(p,size,digest):
    assert p.stat().st_size==size and sha(p)==digest,(p,'size/hash mismatch')
m=read(one(d/'delta','update.json'))
r=read(one(d/'delta','delta-report.json'))
old=read(d/'base'/'update.json')
assert r['eligible'] and m['deltas']==[r['delta']]
p=r['delta'];pkg='com.kelimebulmacasi.a1.practice.fresh'
assert m['packageName']==old['packageName']==pkg
assert m['schemaVersion']==old['schemaVersion']==1
assert (old['versionCode'],m['versionCode'])==(base,target)
assert (old['versionName'],m['versionName'])==(previous,version)
assert (old['tag'],m['tag'])==('v'+previous,'v'+version)
assert m['apkFile']==old['apkFile']=='WordQuestAI.apk'
assert (p['fromVersionCode'],p['toVersionCode'])==(base,target)
assert (p['baseSizeBytes'],p['baseSha256'])==(old['sizeBytes'],old['sha256'])
assert (p['targetSizeBytes'],p['targetSha256'])==(m['sizeBytes'],m['sha256'])
assert p['file']==f'WordQuestAI-{base}-to-{target}.wqd.gz'
assert 0<p['sizeBytes']<m['sizeBytes']*.85 and m['sizeBytes']<=512*1024*1024
verify(d/'base'/'WordQuestAI.apk',old['sizeBytes'],old['sha256'])
assert old['sha256']==os.environ['WQ_BASE_SHA']
apk=one(d/'signed',f'WordQuestAI-v{version}-signed-qa.apk')
verify(apk,m['sizeBytes'],m['sha256'])
delta=one(d/'delta',p['file']);verify(delta,p['sizeBytes'],p['sha256'])
assert read(one(d/'signed','update.json'))==m and read(one(d/'signed','delta-report.json'))==r
with zipfile.ZipFile(apk) as z:
    endpoint=('https://api.github.com/repos/'+os.environ['GITHUB_REPOSITORY']+'/releases/latest').encode()
    assert any(endpoint in z.read(n) for n in z.namelist() if n.startswith('classes') and n.endswith('.dex'))
shutil.copyfile(d/'base'/'WordQuestAI.apk',d/'old.apk')
shutil.copyfile(apk,d/'target.apk')
shutil.copyfile(delta,d/'patch.gz')
shutil.copyfile(one(d/'delta','delta-report.json'),d/'report.json')
out=d/'dist';out.mkdir(exist_ok=True)
shutil.copyfile(apk,out/'WordQuestAI.apk');shutil.copyfile(delta,out/p['file'])
m['notes']=os.environ['WQ_NOTES']
(out/'update.json').write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n')
print('PASS: release-ready exact APK, base and eligible delta verified',version,p['sizeBytes'])
