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

    # v184 already loads/crops the unscaled full-resolution mascot image.
    store=(SRC/"Wq183MascotStore.kt").read_text("utf-8")
    assert "inSampleSize = 1" in store and "Wq184BitmapTrim.crop(decoded)" in store
    friend=(SRC/"Wq185FriendMascot.kt").read_text("utf-8")
    assert "Wq188FriendPreview.encode(avatar, source)" in friend
    assert "Wq183MascotStore.picture(context, avatar, false)" in friend

    file=SRC/"Wq183ArenaShare.kt"
    s=file.read_text("utf-8")
    s=patch(s,
        """            val image = Wq183MascotStore.picture(context, selected, false)
            image?.let { Wq183ArenaShare.encodePreview(selected, it) }""",
        """            val image = Wq183MascotStore.picture(context, selected, false)
            try {
                image?.let { Wq183ArenaShare.encodePreview(selected, it) }
            } finally {
                image?.recycle()
            }""",
        "recycle full-resolution Arena source after encoding")
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

    # Both bundled mascots and remote Arena portraits use this shared
    # renderer. High-quality filtering avoids jagged/pixelated upscaling.
    file=SRC/"WordQuest2DCharacters.kt"
    s=file.read_text("utf-8")
    s=patch(s,
        """                contentScale=ContentScale.Fit,
                modifier=Modifier.fillMaxSize().scale(scale))""",
        """                contentScale=ContentScale.Fit,
                filterQuality=androidx.compose.ui.graphics.FilterQuality.High,
                modifier=Modifier.fillMaxSize().scale(scale))""",
        "smooth local and remote 2D portrait rendering")
    changes[file]=s

    for file,s in changes.items():
        file.write_text(s,encoding="utf-8")
        print("PASS v189 mascot quality:",file.name)
    print("PASS: old previews supported; Firebase path, consent and 18k limit unchanged")

if __name__=="__main__": apply()
