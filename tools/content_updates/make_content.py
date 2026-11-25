#!/usr/bin/env python3
"""Build cumulative data-only content snapshot; sign using an existing release key.
The private key is supplied outside the repository; never commit it.
Publish objects before index.json, and never replace hash-named objects.
"""
import argparse,base64,hashlib,json,re,subprocess,tempfile
from pathlib import Path
PATH=re.compile(r'(arena_v120|arena_v122|new_game_198)/[a-zA-Z0-9_-]{1,80}\.webp')
def make(source,out,core,version,sequence,key):
    assert re.fullmatch(r'1\.0\.\d{1,8}',version) and sequence>0 and core>0
    entries=[]; total=0
    (out/'objects').mkdir(parents=True,exist_ok=True)
    for p in sorted(source.rglob('*')):
        if p.is_dir():continue
        path=p.relative_to(source).as_posix()
        assert not p.is_symlink() and PATH.fullmatch(path),path
        data=p.read_bytes()
        assert 12<=len(data)<=12*1024*1024 and data[:4]==b'RIFF' and data[8:12]==b'WEBP',path
        total+=len(data)
        digest=hashlib.sha256(data).hexdigest()
        (out/'objects'/f'{digest}.webp').write_bytes(data)
        entries.append(dict(path=path,sha256=digest,size=len(data)))
    assert 1<=len(entries)<=100 and total<=96*1024*1024
    manifest=dict(schema=1,packageName='com.kelimebulmacasi.a1.practice.fresh',coreCode=core,version=version,sequence=sequence,files=entries)
    payload=json.dumps(manifest,separators=(',',':'),sort_keys=True).encode()
    signature=subprocess.run(['openssl','dgst','-sha256','-sign',str(key)],input=payload,stdout=subprocess.PIPE,check=True).stdout
    (out/'index.json').write_text(json.dumps(dict(payload=base64.b64encode(payload).decode(),signature=base64.b64encode(signature).decode())))
if __name__=='__main__':
    p=argparse.ArgumentParser()
    p.add_argument('source',type=Path);p.add_argument('out',type=Path)
    p.add_argument('--core',type=int,required=True);p.add_argument('--version',required=True)
    p.add_argument('--sequence',type=int,required=True);p.add_argument('--key',type=Path,required=True)
    a=p.parse_args();make(a.source,a.out,a.core,a.version,a.sequence,a.key)
