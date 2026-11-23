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
# The v1.0.82 overlay already inserted an approved 33-icon atlas route
# before the legacy glossy bitmap fallback. Earlier P2 tried replacing the
# untouched v175 if/else chain and failed (the chain is no longer identical).
# Assert the REAL assembled route, then extend its semantic coverage.
assert "val selectedReplacement=Wq182Icons.preferred(name)" in s, (
    "P2 expected installed 33-icon atlas router")
assert "Wq182Artwork(selectedReplacement,modifier)" in s, (
    "P2 icon provider was not connected to generic card renderer")
atlas=root/"source/app/src/main/java/com/kelimebulmacasi/a1/Wq182Icons.kt"
assert atlas.is_file(), "P2 approved 33-icon atlas not assembled"
a=atlas.read_text(encoding="utf-8")
needle='''        "level_a1" to "wq_seviye_a1",'''
assert a.count(needle)==1,"P2 approved atlas replacement table changed"
a=a.replace(needle,'''        "library" to "wq_kelime_hafizam",
        "notebook" to "wq_kelime_hafizam",
        "review" to "wq_yanlislarimdan_bulmaca",
        "exam" to "wq_yds_hazirlik",
        "level" to "wq_seviye_rozeti_genel",
        "completion" to "wq_cumle_olustur",
        "level_a1" to "wq_seviye_a1",''',1)
for old,new in (("library","wq_kelime_hafizam"),
                ("notebook","wq_kelime_hafizam"),
                ("review","wq_yanlislarimdan_bulmaca"),
                ("exam","wq_yds_hazirlik"),
                ("level","wq_seviye_rozeti_genel"),
                ("completion","wq_cumle_olustur")):
    assert a.count('"'+old+'" to "'+new+'"')==1
atlas.write_text(a,encoding="utf-8")
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
assert "Wq182Artwork(selectedReplacement,modifier)" in s
print("PASS Package2: original premium rooms, verified 33-icon atlas routes, category/verb artwork and six additional semantic routes")
