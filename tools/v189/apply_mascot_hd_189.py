#!/usr/bin/env python3
"""Quality-first, bounded Arena and friend mascot transport.

Run AFTER v188. Keeps existing private Firebase paths/consent and maximum
18,050-character preview payload. Never uploads source ZIPs or personal files.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"

def patch(s,old,new,label):
    count=s.count(old)
    if count!=1: raise AssertionError(f"{label}: expected 1 anchor, found {count}")
    return s.replace(old,new,1)

def apply():
    changes={}

    file=SRC/"Wq183MascotStore.kt"
    s=file.read_text("utf-8")
    s=patch(s,
        "fun picture(context: Context, id: String, victory: Boolean): Bitmap? {",
        "fun picture(context: Context, id: String, victory: Boolean, fullResolution: Boolean = false): Bitmap? {",
        "add opt-in full-res image decode")
    s=patch(s,
        "                inSampleSize = 2",
        "                inSampleSize = if (fullResolution) 1 else 2",
        "preserve originals for network HD; keep old lightweight in-app defaults")
    changes[file]=s

    file=SRC/"Wq185FriendMascot.kt"
    s=file.read_text("utf-8")
    s=patch(s,
        "Wq183MascotStore.picture(context, avatar, false) ?: return null",
        "Wq183MascotStore.picture(context, avatar, false, fullResolution = true) ?: return null",
        "full resolution friend preview source")
    assert "Wq188FriendPreview.encode(avatar, source)" in s
    changes[file]=s

    file=SRC/"Wq183ArenaShare.kt"
    s=file.read_text("utf-8")
    s=patch(s,
        """            val image = Wq183MascotStore.picture(context, selected, false)
            image?.let { Wq183ArenaShare.encodePreview(selected, it) }""",
        """            val image = Wq183MascotStore.picture(context, selected, false, fullResolution = true)
            try {
                image?.let { Wq183ArenaShare.encodePreview(selected, it) }
            } finally {
                image?.recycle()
            }""",
        "use full resolution, recycle image after Arena preview creation")
    start=s.index("    /** Only decode bounded, valid WebP content")
    end=s.index("    fun receive(id: String, data: String?) {",start)
    s=s[:start]+"""    /**
     * The same high-detail, resource-bounded WebP codec as mutual friends.
     * The original room field and 18,050-character rule are unchanged.
     * Older 160px previews remain decode-compatible.
     */
    fun decodePreview(avatarId: String, payload: String?): Bitmap? =
        Wq188FriendPreview.decode(avatarId, payload)

    fun encodePreview(id: String, bitmap: Bitmap): String? =
        Wq188FriendPreview.encode(id, bitmap)

"""+s[end:]
    assert "Wq188FriendPreview.encode(id, bitmap)" in s
    changes[file]=s

    file=SRC/"Wq186FriendProfileDetail.kt"
    s=file.read_text("utf-8")
    assert "filterQuality=FilterQuality.High" in s, "v188 friend image smoothing missing"
    s=patch(s,
        "val avatarWidth = if (tablet) 220.dp else 174.dp",
        "val avatarWidth = if (tablet) 260.dp else 202.dp",
        "larger remote portrait without stretching bitmap")
    s=patch(s,
        "val avatarHeight = if (tablet) 260.dp else 208.dp",
        "val avatarHeight = if (tablet) 320.dp else 252.dp",
        "taller portrait ensures entire mascot is visible")
    changes[file]=s

    for file,s in changes.items():
        file.write_text(s,encoding="utf-8")
        print("PASS v189 mascot quality:",file.name)
    print("PASS: old previews supported; Firebase path, consent and 18k limit unchanged")

if __name__=="__main__": apply()
