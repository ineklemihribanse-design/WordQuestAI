#!/usr/bin/env python3
"""Create a lossless APK delta without re-signing or recompressing APK entries.
WQD1 gzip stream: magic, u64 output size, then COPY (1,u64 offset,u32 length),
LITERAL (2,u32 length, bytes), END (0). All integers big-endian.
Only exact matching raw compressed ZIP payloads are copied; all other bytes,
including Android's APK signing block and ZIP directory, are literals.
"""
import argparse, gzip, hashlib, json, mmap, os, struct, zipfile
from pathlib import Path

CHUNK = 1024 * 1024
LIMIT = 1024 * 1024 * 1024

def sha(path):
    h=hashlib.sha256()
    with open(path,'rb') as f:
        for b in iter(lambda:f.read(CHUNK),b''): h.update(b)
    return h.hexdigest()

def spans(path, data):
    with zipfile.ZipFile(path) as z:
        seen=set()
        for i in z.infolist():
            if i.filename in seen: raise ValueError('duplicate ZIP entry')
            seen.add(i.filename)
            off=i.header_offset
            if off<0 or off+30>len(data) or data[off:off+4]!=b'PK\x03\x04':
                raise ValueError('invalid local header')
            n,e=struct.unpack_from('<HH',data,off+26)
            start=off+30+n+e
            end=start+i.compress_size
            if start<off or end>len(data): raise ValueError('invalid ZIP span')
            if end-start>=64:
                yield i.filename,start,end

def build(old,new,patch):
    if not 0<old.stat().st_size<=LIMIT or not 0<new.stat().st_size<=LIMIT:
        raise ValueError('invalid APK size')
    with open(old,'rb') as a, open(new,'rb') as b:
        with mmap.mmap(a.fileno(),0,access=mmap.ACCESS_READ) as x, mmap.mmap(b.fileno(),0,access=mmap.ACCESS_READ) as y:
            indexed={name:(s,e) for name,s,e in spans(old,x)}
            copies=[]
            for name,s,e in spans(new,y):
                prev=indexed.get(name)
                if prev and prev[1]-prev[0]==e-s and x[prev[0]:prev[1]]==y[s:e]:
                    copies.append((s,e,prev[0]))
            copies.sort()
            copied=0
            def literal(out,start,end):
                for pos in range(start,end,CHUNK):
                    size=min(CHUNK,end-pos)
                    out.write(struct.pack('>BI',2,size))
                    out.write(y[pos:pos+size])
            with open(patch,'wb') as raw:
                with gzip.GzipFile(filename='',fileobj=raw,mode='wb',compresslevel=6,mtime=0) as out:
                    out.write(struct.pack('>4sQ',b'WQD1',len(y)))
                    pos=0
                    for s,e,off in copies:
                        if s<pos: raise ValueError('overlapping ZIP spans')
                        literal(out,pos,s)
                        out.write(struct.pack('>BQI',1,off,e-s))
                        copied+=e-s
                        pos=e
                    literal(out,pos,len(y))
                    out.write(b'\0')
    return copied

def verify(path, manifest):
    assert path.stat().st_size==manifest['sizeBytes']
    assert sha(path)==manifest['sha256'].lower()
    assert manifest['packageName']=='com.kelimebulmacasi.a1.practice.fresh'
    assert manifest['apkFile']=='WordQuestAI.apk'

def main():
    p=argparse.ArgumentParser()
    p.add_argument('old',type=Path);p.add_argument('new',type=Path)
    p.add_argument('old_manifest',type=Path);p.add_argument('new_manifest',type=Path)
    p.add_argument('out',type=Path); args=p.parse_args()
    old=json.loads(args.old_manifest.read_text());new=json.loads(args.new_manifest.read_text())
    verify(args.old,old);verify(args.new,new)
    assert 0<int(old['versionCode'])<int(new['versionCode'])
    args.out.mkdir(parents=True,exist_ok=True)
    name=f"WordQuestAI-{old['versionCode']}-to-{new['versionCode']}.wqd.gz"
    patch=args.out/name
    copied=build(args.old,args.new,patch)
    size=patch.stat().st_size
    info={'format':'WQD1-gzip','fromVersionCode':old['versionCode'],
          'baseSha256':old['sha256'],'baseSizeBytes':old['sizeBytes'],
          'file':name,'sizeBytes':size,'sha256':sha(patch),
          'toVersionCode':new['versionCode'],'targetSha256':new['sha256'],
          'targetSizeBytes':new['sizeBytes']}
    report={'delta':info,'copiedBytes':copied,'fullApkBytes':new['sizeBytes'],
            'savedBytes':new['sizeBytes']-size,
            'savedPercent':round(100*(1-size/new['sizeBytes']),2),
            'eligible':size < new['sizeBytes']*.85}
    (args.out/'delta-report.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report,indent=2))
if __name__=='__main__':main()
