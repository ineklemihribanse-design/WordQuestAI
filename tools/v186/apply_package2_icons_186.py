#!/usr/bin/env python3
"""Package 2: retain existing approved icon artwork, add category and verb-form
resources, and route those specific semantic titles to their dedicated art.
Never replace original assets or user data.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[2]
main=root/"source/app/src/main/java/com/kelimebulmacasi/a1/WordQuest175Icons.kt"
res=root/"source/app/src/main/res/drawable"
assert main.is_file(), "Approved Wq175 icon layer must be assembled first"
s=main.read_text("utf-8")
def once(old,new,what):
    global s
    assert s.count(old)==1,(what,s.count(old))
    s=s.replace(old,new,1)
once('        "trophy" -> R.drawable.wq175_arena_icon',
     '        "categories" -> R.drawable.wq186_category_folder_icon\n'
     '        "verbs" -> R.drawable.wq186_verb_forms_icon\n'
     '        "trophy" -> R.drawable.wq175_arena_icon',
     "Package 2 dedicated category and verb resource priority")
once('            "paket" in s -> "packages"',
     '            "kategori" in s || "klasör" in s -> "categories"\n'
     '            "paket" in s -> "packages"',
     "Separate the category folder artwork from the package artwork")
once('            "arena" in s || "yarış" in s -> "trophy"',
     '            "5 şıklı" in s || "beş şıklı" in s -> "arena_quiz"\n'
     '            "bulmaca düellosu" in s -> "arena_duel"\n'
     '            "ortak bulmaca" in s || "takım oyunu" in s -> "arena_team"\n'
     '            "arena" in s || "yarış" in s -> "trophy"',
     "Differentiate approved game-mode icons")
assert '"fiil" in s -> "verbs"' in s, "Verb forms mapping lost"
# Historical Wq175GlossIcon only checked the old wq175_icons assets, so the
# 33 installed user-approved atlas icons were not actually rendered in most
# cards. This is the missing integration, not an icon-file download issue.
old_gloss='''    if (approved != null) {
        Image(painter = painterResource(approved), contentDescription = null,
            modifier = modifier, contentScale = ContentScale.Fit)
    } else if (image != null) {
        Image(bitmap = image, contentDescription = null,
            modifier = modifier, contentScale = ContentScale.Fit)
    } else {
        Box(modifier.background(Color(0xFFEAE5F8), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center) {
            Text("•", color = Color(0xFF6654A2))
        }
    }'''
new_gloss='''    if (approved != null) {
        Image(painter = painterResource(approved), contentDescription = null,
            modifier = modifier, contentScale = ContentScale.Fit)
    } else {
        // Package 2: route generic/repeated UI icon keys to the 33 approved
        // 512px source artworks, then to their verified 6x6 atlas, and only
        // then to the existing bitmap fallback. Premium room icons stay first.
        val atlasKey = Wq182Icons.preferred(name)
        val drawn = if (atlasKey != null) {
            Wq183HighResArtwork(atlasKey, modifier) ||
                Wq182Artwork(atlasKey, modifier)
        } else false
        if (!drawn && image != null) {
            Image(bitmap = image, contentDescription = null,
                modifier = modifier, contentScale = ContentScale.Fit)
        } else if (!drawn) {
            Box(modifier.background(Color(0xFFEAE5F8), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center) {
                Text("•", color = Color(0xFF6654A2))
            }
        }
    }'''
once(old_gloss,new_gloss,"Real 33-icon atlas routing in all Wq175GlossIcon cards")
assert s.count('Wq183HighResArtwork(atlasKey, modifier)')==1
assert s.count('Wq182Artwork(atlasKey, modifier)')==1
assert '"kategori" in s || "klasör" in s -> "categories"' in s
res.mkdir(parents=True,exist_ok=True)
category='''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="64dp" android:height="64dp" android:viewportWidth="64"
    android:viewportHeight="64">
    <path android:fillColor="#493184" android:pathData="M5,18 Q5,12 11,12 L25,12 L31,18 L53,18 Q59,18 59,24 L59,49 Q59,55 53,55 L11,55 Q5,55 5,49 Z"/>
    <path android:fillColor="#B48BEE" android:pathData="M7,27 Q7,23 12,23 L52,23 Q57,23 57,28 L57,47 Q57,52 52,52 L12,52 Q7,52 7,47 Z"/>
    <path android:fillColor="#F6E3A1" android:pathData="M27,30 L33,30 L33,35 Q38,33 40,38 Q40,42 34,41 L34,47 L27,47 L27,42 Q22,44 21,39 Q21,35 27,36 Z"/>
</vector>
'''
verbs='''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="64dp" android:height="64dp" android:viewportWidth="64"
    android:viewportHeight="64">
    <path android:fillColor="#5F3A9B" android:pathData="M3,16 Q3,12 8,12 L22,12 Q26,12 26,16 L26,49 Q26,53 22,53 L8,53 Q3,53 3,49 Z"/>
    <path android:fillColor="#8471DB" android:pathData="M21,11 Q21,7 26,7 L40,7 Q44,7 44,11 L44,50 Q44,54 40,54 L26,54 Q21,54 21,50 Z"/>
    <path android:fillColor="#45B5B2" android:pathData="M40,16 Q40,12 45,12 L57,12 Q62,12 62,16 L62,49 Q62,53 57,53 L45,53 Q40,53 40,49 Z"/>
    <path android:strokeColor="#FFFFFF" android:strokeWidth="2.3" android:strokeLineCap="round"
        android:pathData="M8,29 L12,39 L17,29 M10,45 L18,45 M27,25 L31,35 L36,25 M29,42 L37,42 M45,29 L49,39 L54,29 M47,45 L55,45"/>
</vector>
'''
(res/"wq186_category_folder_icon.xml").write_text(category,encoding="utf-8")
(res/"wq186_verb_forms_icon.xml").write_text(verbs,encoding="utf-8")
main.write_text(s,encoding="utf-8")
assert s.count("R.drawable.wq186_category_folder_icon")==1
assert s.count("R.drawable.wq186_verb_forms_icon")==1
assert 'Wq183HighResArtwork(atlasKey, modifier)' in s
assert 'Wq182Artwork(atlasKey, modifier)' in s
print("PASS Package2: original premium rooms, 33 atlas icons, category and verb art actively routed")
