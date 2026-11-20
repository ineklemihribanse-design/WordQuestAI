#!/usr/bin/env python3
"""Check working OCR/photo/camera/AI previews and existing scan components."""
from pathlib import Path
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
home=(src/"VocabularyNeonHome.kt").read_text("utf-8")
importer=(src/"WordQuest175ImportWords.kt").read_text("utf-8")
assert home.count("Wq175WordAddBar(")==1
assert home.count("Wq175ImportWordsDialog(")==1
assert home.count("Wq175AddWordDialog(")==1
assert "Wq174MemoryHero(" in home
for name in (
    "scanPairs(context,uri,document,selection,null)",
    "ActivityResultContracts.OpenDocument()",
    "TakeScanPhoto()",
    "scanPhotoUri(context,file)",
    'ai.suggestWords("",topic,level,10,used)',
    "session.addManualLearning(pair.answer,pair.clue,",
    "Seçilen Kelimeleri Kaydet",
    "WordPair"
):
    assert name in importer,name
assert importer.index("fun stage(found:List<WordPair>") < importer.index("Seçilen Kelimeleri Kaydet")
assert "if(!ai.hasKey)" in importer
assert "if(excluded[index]==true)continue" in importer
assert "wq175ImportPreviewPage(pairs,page).forEach" in importer
assert "pairs.forEachIndexed" not in importer
assert "previewPage=0" in importer
assert 'TEST' not in importer
assert (root/"source/app/src/test/java/com/kelimebulmacasi/a1/Wq175ImportPagingTest.kt").exists()
assert (src/"DocumentScan.kt").is_file()
assert (src/"ScanPhotoProvider.kt").is_file()
assert (src/"WordPairs.kt").is_file()
assert (src/"AiSession.kt").is_file()
print("PASS: existing OCR + photo + PDF + camera + AI suggestions use preview approval")
