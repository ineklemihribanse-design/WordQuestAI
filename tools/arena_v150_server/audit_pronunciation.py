#!/usr/bin/env python3
"""Read-only audit of the released APK and the exact expanded Arena bank.
Missing future entries are reported, not mistaken for defects in the old release.
"""
import argparse, collections, hashlib, json, re, zipfile
from pathlib import Path

def normal(s):
    return " ".join(s.strip().lower().split())
def plain_ipa(s):
    return re.sub(r"[\sˈˌ]", "", s)
def read_json(z, name):
    return json.loads(z.read(name).decode("utf-8"))
def inspect(apk, bank):
    with zipfile.ZipFile(apk) as z:
        names=set(z.namelist())
        base="assets/pronunciation/"
        manifest=read_json(z,base+"manifest.json")
        ipa_data=read_json(z,base+"ipa.json")
        words=manifest["words"]
        entries=ipa_data["entries"]
        unresolved=set(ipa_data["unresolved"])
        invalid_ids=[]; missing_clips=[]; invalid_clips=[]; invalid_ipa=[]
        for word, fileid in words.items():
            if word!=normal(word) or fileid!=hashlib.sha1(word.encode()).hexdigest()[:16]:
                invalid_ids.append(word)
            for region in ("en-us","en-gb"):
                path=base+region+"/"+fileid+".ogg"
                if path not in names:
                    missing_clips.append([word,region])
                elif z.getinfo(path).file_size<=1000:
                    invalid_clips.append([word,region,"size"])
                else:
                    with z.open(path) as f:
                        if f.read(4)!=b"OggS":
                            invalid_clips.append([word,region,"header"])
            row=entries.get(word,{})
            if not isinstance(row,dict) or any(not isinstance(row.get(a),str) or not row[a].strip() or any(c in row[a] for c in "<>/\r\n") for a in ("us","uk")):
                invalid_ipa.append(word)
        bad=set(invalid_ids+invalid_ipa)
        bad.update(w for w,*_ in missing_clips+invalid_clips)
        bad.update(unresolved)
        valid=set(words)-bad
        bundled=set()
        for name in ("puzzles.json","expanded-puzzles.json"):
            p="assets/"+name
            if p in names:
                bundled.update(normal(str(e[0])) for g in read_json(z,p) for e in g["words"])
        for p in sorted(n for n in names if n.startswith("assets/vocabulary") and n.endswith(".tsv")):
            for line in z.read(p).decode("utf-8").splitlines():
                if not line.strip() or line.startswith("#"):continue
                fields=line.split("\t")
                if len(fields)==5:bundled.add(normal(fields[3]))
        if "assets/verb-forms.txt" in names:
            for line in z.read("assets/verb-forms.txt").decode("utf-8").splitlines():
                if not line.strip() or line.startswith("#"):continue
                f=line.split("\t")
                if len(f)==7:bundled.update(normal(w) for field in f[1:4] for w in field.split("|"))
        bundled.discard("")
        bank_words={normal(w["answer"]) for w in bank if re.fullmatch(r"[A-Za-z]{2,24}",w.get("answer","")) and w.get("meaning","").strip()}
        by_level={}
        for level in ("A1","A2","B1","B2","C1","C2"):
            group={normal(w["answer"]) for w in bank if w.get("level")==level and re.fullmatch(r"[A-Za-z]{2,24}",w.get("answer","")) and w.get("meaning","").strip()}
            by_level[level]={"required":len(group),"covered":len(group&valid),"missing":len(group-valid)}
        expected={"do":"duː","hi":"haɪ","my":"maɪ","us":"ʌs"}
        short={w:{"ipa":entries.get(w),"valid":w in valid and all(plain_ipa(entries[w][a])==p for a in ("us","uk"))} for w,p in expected.items()}
        # A standalone recording cannot disambiguate grammatical or semantic heteronyms.
        heteronyms=["read","lead","live","wind","tear","bow","close","record","present","object","minute","refuse","produce","conduct","content","desert","invalid","wound","does","bass","use"]
        review={w:entries.get(w) for w in heteronyms if w in bank_words or w in bundled}
        report={"releasedAudioHeadwords":len(words),"releasedIpaEntries":len(entries),"releasedValidBothAccents":len(valid),"releasedBundledWords":len(bundled),"releasedBundledMissing":len(bundled-valid),"expandedArenaHeadwords":len(bank_words),"expandedCovered":len(bank_words&valid),"expandedMissing":len(bank_words-valid),"byLevel":by_level,"invalidIds":invalid_ids,"missingClips":missing_clips,"invalidClips":invalid_clips,"invalidIpa":invalid_ipa,"unresolved":sorted(unresolved),"orphanIpa":sorted(set(entries)-set(words)),"shortWords":short,"heteronymReview":review,"expandedMissingExamples":sorted(bank_words-valid)[:50],"releasedMissingExamples":sorted(bundled-valid)[:30],"limitation":"Header, size, manifest and IPA checks only; no listening test. Future content gaps are not a claim that the released APK contains those words."}
        return report
def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("apk",type=Path)
    parser.add_argument("bank",type=Path)
    parser.add_argument("--report",type=Path,required=True)
    args=parser.parse_args()
    report=inspect(args.apk,json.loads(args.bank.read_text("utf-8")))
    args.report.write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n","utf-8")
    print(json.dumps(report,ensure_ascii=False,indent=2))
    assert report["releasedAudioHeadwords"]>0
    assert report["expandedArenaHeadwords"]>0
if __name__=="__main__":
    main()
