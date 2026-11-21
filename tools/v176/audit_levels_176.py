#!/usr/bin/env python3
"""Audit actual, puzzle-ready English words at all six CEFR learning tiers.

Counts shipped words only. C2 English headwords awaiting *Turkish definitions*
are candidates, never counted as playable words. Do not relabel B/C words as A1.
Approved expansion rows (optional) use:
  LEVEL<TAB>Genel İngilizce<TAB>PART_OF_SPEECH<TAB>UPPERCASE_WORD<TAB>TR_MEANING<TAB>SOURCE
SOURCE must cite a verified, redistributable source in the attribution file.
"""
from __future__ import annotations
from pathlib import Path
from collections import defaultdict
import argparse, csv, json, re

ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/"source/app/src/main/assets"
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
HERE=Path(__file__).resolve().parent
LEVELS=("A1","A2","B1","B2","C1","C2")
GOAL=2000
WORD=re.compile(r"^[A-Z]+(?:[ '-][A-Z]+){0,5}$")
KEY=lambda x: re.sub(r"[^A-Z]","",x.strip().upper())

def existing_words():
    pools=defaultdict(set)
    for filename in ("puzzles.json","expanded-puzzles.json"):
        for game in json.loads((ASSETS/filename).read_text("utf-8")):
            if game.get("level") in LEVELS and game.get("mode") in ("topic","kind"):
                for word in game.get("words",[]):
                    if word:
                        pools[game["level"]].add(KEY(word[0]))
    for line in (ASSETS/"vocabulary-extra.tsv").read_text("utf-8").splitlines():
        if not line or line.startswith("#"): continue
        cols=line.split("\t")
        assert len(cols)==5,("legacy malformed",line[:80])
        if cols[0] in LEVELS:
            pools[cols[0]].add(KEY(cols[3]))
    return pools

def add_tsv(pools,path,approved=False):
    additions=defaultdict(set)
    records=[]
    for index,line in enumerate(path.read_text("utf-8").splitlines(),1):
        if not line or line.startswith("#"): continue
        fields=line.split("\t")
        assert len(fields)==(6 if approved else 5),(path.name,index,"schema")
        level,topic,kind,word,meaning=fields[:5]
        assert level in LEVELS and topic=="Genel İngilizce",(path.name,index,"tier/topic")
        assert kind.strip() and WORD.fullmatch(word),(path.name,index,"word")
        assert 2<=len(KEY(word))<=22 and 2<=len(meaning.strip())<=125,(
            path.name,index,"translation length")
        if approved:
            assert fields[5].strip(),(path.name,index,"source citation required")
        key=KEY(word)
        assert key not in pools[level] and key not in additions[level],(
            path.name,index,"duplicate word",level,word)
        additions[level].add(key)
        records.append(fields[:5])
    for level in LEVELS:
        pools[level].update(additions[level])
    return additions,records

def candidates():
    source=HERE/"content/octanove_c2_source_candidates.csv"
    with source.open(encoding="utf-8",newline="") as fp:
        rows=list(csv.DictReader(fp))
    assert len(rows)==1025,("Octanove C2 source changed",len(rows))
    assert all(row["CEFR"]=="C2" and row["headword"].strip()
               for row in rows)
    return len(rows)

def maybe_install_expansion(records):
    # Optional reviewed, translated rows are only installed after passing
    # provenance/uniqueness checks. Never import untranslated candidate data.
    if not records:
        return
    asset="vocabulary-addendum-v176-approved.tsv"
    (ASSETS/asset).write_text(
        "\n".join("\t".join(x) for x in records)+"\n",encoding="utf-8")
    catalog=SRC/"Catalog.kt"
    s=catalog.read_text("utf-8")
    old="VocabularyBank(games, legacy + additional)"
    assert s.count(old)==1,"Catalog vocabulary construction changed"
    replacement='''val approved = context.assets.open("vocabulary-addendum-v176-approved.tsv")
            .bufferedReader(Charsets.UTF_8).use { it.readLines() }
        VocabularyBank(games, legacy + additional + approved)'''
    catalog.write_text(s.replace(old,replacement,1),"utf-8")

def run(require_complete=False):
    pools=existing_words()
    original={level:len(pools[level]) for level in LEVELS}
    base,ignored=add_tsv(pools,ASSETS/"vocabulary-addendum-v175.tsv")
    approved_path=HERE/"content/approved_en_tr_176.tsv"
    approved=defaultdict(set)
    rows=[]
    if approved_path.exists():
        approved,rows=add_tsv(pools,approved_path,approved=True)
    maybe_install_expansion(rows)
    report={
        "schemaVersion":1,
        "release":"1.0.76",
        "minimumUniqueWordsPerTier":GOAL,
        "counting":"unique normalized English headwords within each tier",
        "candidateC2Only_NotPlayable":candidates(),
        "c2CandidatesNeed":"licensed Turkish meaning, sense and CEFR/editorial review",
        "tiers":{level:{
            "existing":original[level],
            "added175":len(base[level]),
            "approved176":len(approved[level]),
            "playableUnique":len(pools[level]),
            "missing":max(0,GOAL-len(pools[level])),
            "meetsTarget":len(pools[level])>=GOAL,
        } for level in LEVELS},
        "allLevelsReady":all(len(pools[level])>=GOAL for level in LEVELS),
        "quality": "structure + provenance validated; independent language-teacher review pending",
    }
    output=ASSETS/"vocabulary-v176-coverage.json"
    output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    for tier in LEVELS:
        row=report["tiers"][tier]
        print(f"VOCAB 1.0.76 {tier}: {row['playableUnique']} / {GOAL}; "
              f"missing {row['missing']}; approved176={row['approved176']}")
    print("VOCAB 1.0.76 C2 staged candidates (NOT credited):",
          report["candidateC2Only_NotPlayable"])
    print("VOCAB 1.0.76 still needed",sum(x["missing"] for x in report["tiers"].values()))
    print("VOCAB 1.0.76 complete:",report["allLevelsReady"])
    if require_complete:
        assert report["allLevelsReady"],"Refusing a 12,000-word release claim; tiers are incomplete"
    return report

if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--require-2000-each",action="store_true")
    args=parser.parse_args()
    run(args.require_2000_each)
