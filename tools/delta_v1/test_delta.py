#!/usr/bin/env python3
"""Exercise identical reconstruction and safe rejection using the Java core."""
import gzip,hashlib,json,struct,subprocess,sys,tempfile,zipfile
from pathlib import Path
from make_delta import build,sha
classes=Path(sys.argv[1]).resolve()
root=Path(sys.argv[2])
report=json.loads((root/'patch/delta-report.json').read_text())
d=report['delta']
def call(base,patch,out,bs,ds,ts,bh,dh,th,ok):
    cmd=['java','-cp',str(classes),'com.kelimebulmacasi.a1.WqDeltaApplier',
         str(base),str(patch),str(out),str(bs),str(ds),str(ts),bh,dh,th]
    r=subprocess.run(cmd,capture_output=True,text=True)
    assert (r.returncode==0)==ok,(r.returncode,r.stderr)
    if not ok:assert not out.exists(),'failed output must not be committed'
def run(base,patch,out,info,ok=True):
    call(base,patch,out,info['baseSizeBytes'],info['sizeBytes'],info['targetSizeBytes'],
         info['baseSha256'],info['sha256'],info['targetSha256'],ok)
with tempfile.TemporaryDirectory() as tmp:
    t=Path(tmp)
    old=t/'old.apk';new=t/'new.apk';patch=t/'valid.gz'
    payload=hashlib.shake_256(b'fixed test').digest(1024*1024)
    for f,change in [(old,b'old'),(new,b'new')]:
        with zipfile.ZipFile(f,'w',compression=zipfile.ZIP_STORED) as z:
            z.writestr('assets/common.bin',payload)
            z.writestr('classes.dex',change)
    build(old,new,patch)
    info={'baseSizeBytes':old.stat().st_size,'sizeBytes':patch.stat().st_size,
          'targetSizeBytes':new.stat().st_size,'baseSha256':sha(old),'sha256':sha(patch),'targetSha256':sha(new)}
    run(old,patch,t/'good.apk',info)
    assert (t/'good.apk').read_bytes()==new.read_bytes()
    run(new,patch,t/'wrong-base.apk',info,False)
    run(old,patch,t/'wrong-hash.apk',dict(info,targetSha256='0'*64),False)
    bad=t/'bad.gz'
    bad.write_bytes(gzip.compress(struct.pack('>4sQBQI',b'WQD1',info['targetSizeBytes'],1,info['baseSizeBytes']+1,16)+b'\0'))
    run(old,bad,t/'bad-copy.apk',dict(info,sizeBytes=bad.stat().st_size,sha256=sha(bad)),False)
    trunc=t/'truncated.gz';trunc.write_bytes(patch.read_bytes()[:-5])
    run(old,trunc,t/'truncated.apk',dict(info,sizeBytes=trunc.stat().st_size,sha256=sha(trunc)),False)
    assert not list(t.glob('*.apk.part'))
print('PASS synthetic: lossless, wrong base, invalid target hash, out-of-range copy, truncated patch, cleanup')
if report['eligible']:
    run(root/'old/WordQuestAI.apk',root/'patch'/d['file'],root/'reconstructed.apk',d)
    assert sha(root/'reconstructed.apk')==sha(root/'new/WordQuestAI.apk')
    print('PASS real APK byte-for-byte SHA-256 match')
else:
    print('Not sufficiently smaller: use full APK instead')
