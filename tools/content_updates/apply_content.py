#!/usr/bin/env python3
"""Integrate after the complete verified v123 source is assembled."""
from pathlib import Path
import hashlib, re, shutil
pack=Path(__file__).resolve().parent
root=pack.parents[1]
main=root/'source/app/src/main'
src=main/'java/com/kelimebulmacasi/a1'
board=src/'PuzzleScreen.kt'
before=hashlib.sha256(board.read_bytes()).digest()
targets=('Wq122Settings.kt','Wq198NewGameUi.kt')
for name in targets:
    f=src/name
    s=f.read_text()
    old='context.assets.open('
    assert s.count(old)==1,(name,s.count(old))
    f.write_text(s.replace(old,'WqContentUpdates.open(context, ',1))
# Other Arena and New Game background asset calls only when they have an
# explicit allowlisted path; no broad replacement of unrelated asset code.
for f in src.glob('*.kt'):
    if f.name in targets or f==board: continue
    s=f.read_text()
    out=re.sub(r'\b(context|ctx)\.assets\.open\(("(?:arena_v120|arena_v122|new_game_198)/)',r'WqContentUpdates.open(\1, \2',s)
    if out!=s: f.write_text(out)
manifest=main/'AndroidManifest.xml'
s=manifest.read_text()
assert s.count('</application>')==1 and 'WqContentInitProvider' not in s
s=s.replace('</application>', '''    <provider
        android:name="com.kelimebulmacasi.a1.WqContentInitProvider"
        android:authorities="${applicationId}.signedcontent"
        android:exported="false" />
    </application>''')
manifest.write_text(s)
shutil.copyfile(pack/'WqContentUpdates.kt',src/'WqContentUpdates.kt')
ui=src/'UpdateUi.kt'
s=ui.read_text()
anchor='Text("İndirme boyutu: ${offer.sizeLabel}")'
assert s.count(anchor)==1
s=s.replace(anchor,'Text("İçerik sürümü: ${WqContentUpdates.version(androidx.compose.ui.platform.LocalContext.current)}")\n                    '+anchor)
ui.write_text(s)
assert hashlib.sha256(board.read_bytes()).digest()==before
print('Content updater integrated; phone puzzle source unchanged. APK version is controlled by release workflow.')
