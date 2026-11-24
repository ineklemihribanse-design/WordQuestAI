#!/usr/bin/env python3
"""Only the approved New Game background and header badge change in v203."""
from pathlib import Path
import hashlib
import shutil
ROOT=Path(__file__).resolve().parents[2]
HERE=Path(__file__).resolve().parent
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"

def one(s,a,b):
    assert s.count(a)==1,(a[:160],s.count(a))
    return s.replace(a,b,1)

def main():
    target=SRC/"Wq198NewGameUi.kt"
    before=target.read_text("utf-8")
    protected={p:hashlib.sha256(p.read_bytes()).hexdigest() for p in SRC.glob("*.kt") if p!=target}
    old='''        Wq198Artwork(if (landscapeArtwork) "tablet_202" else "phone_202",
            Modifier.matchParentSize(), scale = ContentScale.Crop,
            alignment = Alignment.TopCenter)'''
    new='''        Wq203NewGameBackground(landscapeArtwork, Modifier.matchParentSize())'''
    s=one(before,old,new)
    imp="import androidx.compose.ui.graphics.Color"
    newimp=imp+"\nimport androidx.compose.ui.graphics.graphicsLayer"
    s=one(s,imp,newimp)
    old_badge='                    Wq198Artwork("level", Modifier.size(side))'
    new_badge='''                    Wq198Artwork("level", Modifier.size(side).graphicsLayer {
                        // Visual scale only: the header, title and cards keep their measurements.
                        scaleX = 1.18f
                        scaleY = 1.18f
                    })'''
    s=one(s,old_badge,new_badge)
    assert s.replace(new_badge,old_badge,1).replace(newimp,imp,1).replace(new,old,1)==before
    target.write_text(s,"utf-8")
    shutil.copyfile(HERE/"Wq203NewGameBackground.kt",SRC/"Wq203NewGameBackground.kt")
    for p,digest in protected.items():
        assert hashlib.sha256(p.read_bytes()).hexdigest()==digest,p
    print("PASS: all existing Kotlin files except New Game artwork host byte-identical")
    print("PASS: exact v1.0.102 New Game layout; vector background and 18% visually enlarged header badge only")
if __name__=="__main__":
    main()
