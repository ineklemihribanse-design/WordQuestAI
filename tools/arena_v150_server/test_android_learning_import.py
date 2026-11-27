#!/usr/bin/env python3
"""Check the real Android content importer against a disposable source-contract fixture.
This does not substitute for Android compilation or device tests.
"""
import hashlib, json, re, shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
HERE=Path(__file__).resolve().parent

def export_arrays(path):
    text=path.read_text("utf-8")
    for match in re.finditer(r"^export\s+const\s+(\w+)\s*=\s*",text,re.M):
        array,_=json.JSONDecoder().raw_decode(text[match.end():])
        yield match.group(1),array

def kotlin_string(text):
    return json.loads(text.replace(r"\$", "$"))
QSTRING=r'"(?:[^"\\]|\\.)*"'
QUESTION=re.compile(r"^\s*YdsQuestion\(("+QSTRING+r"), YdsTopic\.(\w+), YdsDifficulty\.(\w+), ("+QSTRING+r"), listOf\((.*)\), (\d+), ("+QSTRING+r")\),?$")
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()

def main():
    # Read expected answer identities independently from the generated Kotlin.
    source={}
    for path in HERE.glob("learning_*.mjs"):
        if path.name in ("learning_mix.mjs","learning_mix.test.mjs"):continue
        for name,rows in export_arrays(path):
            if name=="grammarVariants":continue # already represented in base Academy
            for q in rows:
                if isinstance(q,dict) and "prompt" in q and q.get("yds"):
                    assert q["id"] not in source,q["id"]
                    source[q["id"]]=q
    assert len(source)==5288,len(source)
    with tempfile.TemporaryDirectory() as d:
        root=Path(d)
        tools=root/"tools/arena_v150_server"
        shutil.copytree(HERE,tools,ignore=shutil.ignore_patterns("node_modules","*.json","__pycache__"))
        assets=root/"source/app/src/main/assets"
        assets.mkdir(parents=True)
        java=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
        java.mkdir(parents=True)
        (java/"YdsContent.kt").write_text("val questions = Wq176YdsGenerated.questions\n","utf-8")
        (java/"VocabularyBank.kt").write_text("private fun senses(level: String) = levelBanks[level] ?: ydsBanks[level].orEmpty()\n","utf-8")
        files=sorted((ROOT/"tools/v175/content").glob("wordquest_en_tr_*.tsv"))
        original="".join(p.read_text("utf-8").rstrip("\n")+"\n" for p in files)
        (assets/"vocabulary-addendum-v175.tsv").write_text(original,"utf-8")
        legacy="A1\tGenel İngilizce\tİsimler\tAPPLE\telma\n"
        (assets/"vocabulary-extra.tsv").write_text(legacy,"utf-8")
        installer=tools/"install_android_learning.py"
        subprocess.run([sys.executable,str(installer)],check=True,cwd=root)
        generated=java/"Wq150LearningQuestions.kt"
        observed=set()
        for line in generated.read_text("utf-8").splitlines():
            if not line.strip().startswith("YdsQuestion("):continue
            m=QUESTION.fullmatch(line)
            assert m,line[:200]
            qid,topic,diff,prompt,raw_options,correct,explanation=m.groups()
            qid=kotlin_string(qid)
            q=source[qid]
            assert qid not in observed
            observed.add(qid)
            opts=[kotlin_string(x) for x in re.findall(QSTRING,raw_options)]
            assert len(opts)==4 and len(set(opts))==4,qid
            assert opts[int(correct)]==q["options"][q["correct"]],qid
            assert set(opts)<=set(q["options"]),qid
            assert kotlin_string(prompt)==q["prompt"] and kotlin_string(explanation)==q["explanation"],qid
            assert topic==q["topic"] and diff=={"easy":"Easy","medium":"Medium","hard":"Hard"}[q["yds"]],qid
        assert observed==set(source),(len(observed),len(source))
        assert (assets/"vocabulary-extra.tsv").read_text("utf-8")==legacy
        assert (assets/"vocabulary-addendum-v175.tsv").read_text("utf-8").startswith(original)
        words=json.loads((assets/"learning-pronunciation-words.json").read_text("utf-8"))
        assert len(words)==1186,len(words)
        assert (java/"Wq150AcademicWords.kt").read_text("utf-8").count("WordSense(Entry(")==1231
        assert (java/"YdsContent.kt").read_text("utf-8").count("Wq150LearningQuestions.questions")==1
        assert (java/"VocabularyBank.kt").read_text("utf-8").count("Wq150AcademicWords.senses")==1
        snapshot={p:digest(p) for p in (root/"source").rglob("*") if p.is_file()}
        duplicate=subprocess.run([sys.executable,str(installer)],capture_output=True,cwd=root)
        assert duplicate.returncode!=0,"Repeated install must fail closed"
        assert {p:digest(p) for p in (root/"source").rglob("*") if p.is_file()}==snapshot
        print("ANDROID_CONTENT_IMPORT",json.dumps({"newYdsQuestions":len(observed),"allCorrectAnswersPreserved":True,"optionsPerYdsQuestion":4,"authoredAcademicRecords":1231,"pronunciationHeadwords":len(words),"originalVocabularyPreserved":True,"duplicateInstallBlocked":True,"fixtureOnly":True}))
if __name__=="__main__":main()
