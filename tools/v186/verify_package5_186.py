#!/usr/bin/env python3
"""Source-level Package 5 regression contracts; these do not replace Android builds."""
from pathlib import Path
from collections import Counter

root=Path(__file__).resolve().parents[2]
p5=root/"tools/v186/package5"
matching=(p5/"WordQuest175Matching.kt").read_text("utf-8")
importer=(p5/"Wq186MatchingImporter.kt").read_text("utf-8")
folder=(p5/"Wq186MatchingFolders.kt").read_text("utf-8")
meaning=(p5/"Wq186MeaningBank.kt").read_text("utf-8")
voice=(p5/"Wq186LenaVoiceSettings.kt").read_text("utf-8")
overlay=(root/"tools/v186/apply_package5_to_186.py").read_text("utf-8")
assert 'listOf("6","10","15","20","30")' in matching
assert 'number*2' in matching
assert 'levels.toSet()' in matching
assert 'quickSeconds' in matching
assert 'Wq186MatchingImporter(' in matching
assert 'Wq186MatchingFolders(context,session.profileKey)' in matching
assert 'wordSource="Klasörlerim"' in matching
assert 'onDismiss();onImport()' not in matching
assert 'Wq186MeaningBank.load(levels)' in matching
assert 'if(!played) Box(' in matching
assert 'Wq186EnglishMeaningIcon(' in matching
for name in ('OpenDocument','TakeScanPhoto','scanPairs','suggestWords',
    'aiCount','Elle kelime ekle','onDone(selected.id)'):
    assert name in importer, f"Missing independent source: {name}"
assert "session.addManualLearning" not in importer
assert "session.searchLearning" not in importer
assert 'defineMatchingWords' in importer
assert (p5/"Wq186EnglishDefinitionsOnline.kt").is_file()
assert 'LenaRichComposer.kt' in overlay
assert 'translateDraftForLena' in overlay
assert (p5/"Wq186LenaTranslation.kt").is_file()
assert 'store.rename(' in importer
assert 'store.remove(' in importer
assert 'getSharedPreferences(' in folder
assert "wq186_match_folders_" in folder
assert 'commit()' in folder
assert 'wordSource=="Klasörlerim"' in matching
assert 'nativeVoiceUs' in voice and 'nativeVoiceUk' in voice
assert 'EnglishVoice.entries' in voice
assert 'PronunciationMode.entries' in voice
assert 'TextToSpeech' in voice
assert 'Wq186LenaVoiceSettings' in overlay
assert 'Apply staged Package 5 only' in overlay
rows=[line.strip() for line in meaning.splitlines() if line.strip().startswith(
    ('A1|','A2|','B1|','B2|','C1|','C2|'))]
levels=Counter(line.split('|',1)[0] for line in rows)
assert all(levels.get(x,0)>=30 for x in ('A1','A2','B1','B2','C1','C2')), levels
assert len({(line.split('|',2)[0],line.split('|',2)[1]) for line in rows})==len(rows)
print("PASS Package 5 source contracts:", dict(levels),len(rows),"verified definitions")
