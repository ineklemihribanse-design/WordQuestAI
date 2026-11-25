#!/usr/bin/env python3
"""Apply only after complete, verified 1.0.119 source has been assembled."""
from pathlib import Path
import hashlib,json,shutil
root=Path(__file__).resolve().parents[2]
pack=Path(__file__).resolve().parent
app=root/'source/app'
main=app/'src/main'
p=main/'java/com/kelimebulmacasi/a1'
file=p/'ArenaApprovedHome.kt'
version=app/'build.gradle.kts'
s=file.read_text('utf-8'); g=version.read_text('utf-8')
assert s.count('Wq175ArenaHeroAndModes(')==1 and 'matchSettingsPage' in s
assert 'onMode(nextMode)' in s and 'matchSettingsPage=true' in s.replace(' ','')
assert g.count('versionCode = 145')==1 and g.count('versionName = "1.0.119"')==1
assert 'applicationIdSuffix = ".fresh"' in g
manifest=json.loads((pack/'assets/manifest.json').read_text())
required={'phone','tablet','crest','duel','team','quiz','modes','ranking','friends','history','settings','info','mascot','book','back'}|{f'{d}_card_{k}' for d in ('phone','tablet') for k in ('duel','team','quiz')}
assert set(manifest)=={x+'.webp' for x in required}
for name,meta in manifest.items():
    b=(pack/'assets'/name).read_bytes()
    assert b[:4]==b'RIFF' and b[8:12]==b'WEBP' and hashlib.sha256(b).hexdigest()==meta['sha256'],name
protected={x:hashlib.sha256(x.read_bytes()).hexdigest() for x in main.rglob('*') if x.is_file() and x!=file}
out=main/'assets/arena_v120';out.mkdir(parents=True,exist_ok=True)
for name in manifest: shutil.copyfile(pack/'assets'/name,out/name)
shutil.copyfile(pack/'Wq120ArenaLobby.kt',p/'Wq120ArenaLobby.kt')
# Only hide the redundant Arena text chip; retain the existing dynamic mascot selector.
chip='''                Box(Modifier.clip(RoundedCornerShape(25.dp))
                    .background(Color(0xFFEBE0FF))
                    .padding(horizontal=17.dp,vertical=3.dp)) {
                    Text("Arena",color=ArenaApproved.ink,fontWeight=FontWeight.Bold,fontSize=12.sp)
                }'''
if s.count(chip)==1:
    s=s.replace(chip,'',1)
s=s.replace('Wq175ArenaHeroAndModes(','Wq120ArenaLobby(',1)
file.write_text(s,'utf-8')
version.write_text(g.replace('versionCode = 145','versionCode = 146',1).replace('versionName = "1.0.119"','versionName = "1.0.120"',1),'utf-8')
assert all(hashlib.sha256(x.read_bytes()).hexdigest()==digest for x,digest in protected.items())
print('PASS: Arena entry callback only, assets verified, 1.0.120/code 146')
