#!/usr/bin/env python3
"""Create an attributed, original YDS-style vocabulary/translation and grammar bank.
Runs AFTER all 1.0.75 overlays and BEFORE Gradle. Never copy publisher questions
from uploaded book PDFs. Those books inform the syllabus, not the question text.
Vocabulary expressions come from the existing CC BY-SA 4.0 credited word lists.
"""
from pathlib import Path
from collections import defaultdict
import hashlib
import random
import re
import shutil

ROOT = Path(__file__).resolve().parents[2]
HERE = ROOT / "tools/v176"
DATA = ROOT / "tools/v175/content"
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
TEST = ROOT / "source/app/src/test/java/com/kelimebulmacasi/a1"
ASSETS = ROOT / "source/app/src/main/assets"
TARGET = 1600
GRAMMAR = 240
FINAL_TOTAL = 198 + TARGET + GRAMMAR

def digest(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()

def safe(text):
    return text.replace("\t"," ").replace("\n"," ").replace("\r"," ").replace('"""',"'").strip()

def words():
    selected = []
    seen = set()
    for level in ("B1","B2","C1"):
        for line in (DATA/f"wordquest_en_tr_{level}.tsv").read_text("utf-8").splitlines():
            parts = line.split("\t")
            if len(parts) != 5:
                continue
            _,_,kind,word,meaning = parts
            sense = safe(meaning.split(",")[0].split(";")[0])
            if not re.fullmatch(r"[A-Z]{4,17}",word):
                continue
            if not (4 <= len(sense) <= 38) or not re.fullmatch(
                 r"[A-Za-zÇĞİÖŞÜçğıöşü0-9 \-'()]+",sense):
                continue
            if word in seen or sense.casefold() in {item[3].casefold() for item in selected}:
                continue
            if len(sense.split()) > 5:
                continue
            if kind not in ("Fiiller","İsimler","Sıfatlar","Zarflar"):
                continue
            seen.add(word)
            selected.append((level,kind,word,sense))
    selected.sort(key=lambda item: digest("wq176-content-v1:"+item[2]))
    assert len(selected)>=TARGET, ("Not enough suitable sourced words",len(selected))
    return selected

def lexical_questions(entries):
    bank=[]
    # Use different English stems for vocabulary and translation tasks.
    en_stems=(
        "In academic English, choose the closest Turkish meaning of '{word}'.",
        "Which Turkish option best conveys the word '{word}'?",
        "A YDS reading task contains '{word}'. What is its Turkish equivalent?",
        "Choose the Turkish meaning closest to '{word}'.",
    )
    tr_stems=(
        "'{sense}' ifadesinin İngilizce karşılığı hangisidir?",
        "In an English translation task, which term corresponds to '{sense}'?",
        "Select the best English equivalent of the Turkish expression '{sense}'.",
        "'{sense}' anlamına gelen İngilizce sözcüğü seçiniz.",
    )
    for i, (level,kind,word,sense) in enumerate(entries[:TARGET]):
        topic = "Vocabulary" if i%2 == 0 else "Translation"
        difficulty={"B1":"Easy","B2":"Medium","C1":"Hard"}[level]
        rng=random.Random(int(digest(word+":"+str(i))[:12],16))
        candidates=[w for w in entries if w[0]==level and w[1]==kind and w[2]!=word]
        rng.shuffle(candidates)
        incorrect=[]
        for other in candidates:
            proposed=other[3] if topic=="Vocabulary" else other[2].lower()
            if proposed.casefold() == (sense.casefold() if topic=="Vocabulary" else word.lower()):
                continue
            # Reject distractors with overlapping descriptive roots (near-synonyms).
            if topic=="Vocabulary":
                own={t for t in re.findall(r"\w+",sense.lower()) if len(t)>3}
                alt={t for t in re.findall(r"\w+",other[3].lower()) if len(t)>3}
                if own & alt: continue
            if proposed.casefold() in {x.casefold() for x in incorrect}:
                continue
            incorrect.append(proposed)
            if len(incorrect)==3: break
        if len(incorrect)!=3:
            raise AssertionError("No safe distinct distractors: "+word)
        if topic=="Vocabulary":
            prompt=en_stems[(i//2)%len(en_stems)].format(word=word.lower())
            correct=sense
            explanation=f"'{word.lower()}' sözcüğünün sözlükte verilen Türkçe karşılığı '{sense}' ifadesidir. Diğer seçenekler farklı anlamlar taşır."
        else:
            prompt=tr_stems[(i//2)%len(tr_stems)].format(sense=sense)
            correct=word.lower()
            explanation=f"'{sense}' ifadesinin İngilizce sözlük karşılığı '{word.lower()}' kelimesidir. Bu eşleştirme akademik kelime çalışması içindir."
        bank.append((f"wq176_lex_{i:04d}",topic,difficulty,prompt,
                     correct,*incorrect,explanation))
    return bank

# Original, rule-checked grammatical structures; contextual nouns vary by row.
GRAMMAR_RULES=[
 ("By the time the {actor} reached the {place}, the {event} ____.",
  ("had already begun","has already begun","will begin","begins"),
  "By the time ile iki geçmiş olayın daha önce gerçekleşeni past perfect ile kurulur."),
 ("The {thing} ____ by the committee last month.",
  ("was approved","is approving","will approve","has approve"),
  "Last month geçmiş zaman göstergesidir; edilgen past simple was approved olur."),
 ("If the {actor} ____ earlier, the outcome would have been different.",
  ("had arrived","arrives","will arrive","has arrive"),
  "Geçmişe karşıt koşulda if + past perfect kullanılır."),
 ("The {thing} must ____ before the final review.",
  ("be checked","checking","checked","be checking"),
  "Must + be + past participle edilgen zorunluluk yapısıdır."),
 ("Despite ____ limited resources, the {actor} completed the {thing}.",
  ("having","to have","have","had"),
  "Despite ardından isim ya da -ing biçimi gelir."),
 ("Hardly ____ the {event} started when the alarm rang.",
  ("had","has","is","did"),
  "Hardly had + özne + V3 kalıbı geçmişte art arda olan iki olayı gösterir."),
 ("No sooner ____ the {event} begun than the audience reacted.",
  ("had","has","was","did"),
  "No sooner had ... than kalıbı devrik past perfect yapısı gerektirir."),
 ("Unless the {thing} ____ carefully, the conclusions may be misleading.",
  ("is examined","examines","will examining","examined"),
  "Unless şart yan cümlesinde present simple passive kullanılır."),
 ("The {actor}, ____ research influenced the field, presented the results.",
  ("whose","who","which","where"),
  "Whose, kişinin araştırmasıyla arasındaki sahiplik ilişkisini belirtir."),
 ("Neither the {actor} nor the assistants ____ available yesterday.",
  ("were","was","is","be"),
  "Neither ... nor yapısında yakın çoğul özne assistants ile were uyumludur."),
 ("The {thing} is expected ____ next month.",
  ("to be completed","completing","to completed","be complete"),
  "Is expected to be completed gelecek için edilgen beklenti anlatır."),
 ("The more often the {thing} is tested, ____ its limits become.",
  ("the clearer","clearer","the clearest","the most clear"),
  "The more ..., the clearer ... çift karşılaştırma yapısıdır."),
 ("Not until the {thing} was verified ____ the researchers publish their conclusions.",
  ("did","had","were","have"),
  "Not until cümle başındaysa ana cümlede did + özne + yalın fiil ile inversion gerekir."),
 ("If the {actor}'s team had sufficient funding, it ____ the project now.",
  ("could expand","could have expanded","will expanded","expanding"),
  "Şimdiki gerçek dışı koşulda if + past simple, could + fiil kullanılır."),
 ("The {thing} should have ____ last week.",
  ("been repaired","be repaired","repair","repairing"),
  "Should have been + V3 geçmişte yapılması gereken edilgen eylemi belirtir."),
 ("The {actor}'s samples, ____ were stored at low temperatures, remained stable.",
  ("which","who","whose","where"),
  "Virgüllü açıklayıcı relative clause için cansız özneye which gelir."),
 ("It is essential that every participant in the {event} ____ informed.",
  ("be","is","was","being"),
  "Essential that + özne + yalın fiil, mandative subjunctive yapısıdır."),
 ("The {actor} suggested ____ the figures before submitting the {thing}.",
  ("reviewing","to review","review","reviewed"),
  "Suggest fiilinden sonra bu yapıda gerund (-ing) kullanılır."),
 ("The {thing} is believed ____ designed decades ago.",
  ("to have been","to be","having been","was"),
  "Geçmişte gerçekleşmiş edilgen eylem için to have been + V3 kullanılır."),
 ("The {actor} would rather ____ the {event} results until further checks were complete.",
  ("postpone","to postpone","postponing","postponed"),
  "Would rather ardından to almayan yalın fiil gelir."),
]
CONTEXTS=[
 {"actor":"researcher","place":"laboratory","event":"analysis","thing":"report"},
 {"actor":"analyst","place":"archive","event":"survey","thing":"proposal"},
 {"actor":"engineer","place":"workshop","event":"inspection","thing":"device"},
 {"actor":"instructor","place":"classroom","event":"lecture","thing":"assignment"},
 {"actor":"scientist","place":"observatory","event":"experiment","thing":"instrument"},
 {"actor":"editor","place":"office","event":"review","thing":"manuscript"},
 {"actor":"technician","place":"station","event":"assessment","thing":"equipment"},
 {"actor":"investigator","place":"museum","event":"investigation","thing":"document"},
 {"actor":"consultant","place":"conference hall","event":"presentation","thing":"contract"},
 {"actor":"supervisor","place":"facility","event":"audit","thing":"system"},
 {"actor":"scholar","place":"library","event":"seminar","thing":"collection"},
 {"actor":"director","place":"research centre","event":"workshop","thing":"framework"},
]

def grammar_questions():
    bank=[]
    for rule_index,(stem,options,why) in enumerate(GRAMMAR_RULES):
        for context_index,context in enumerate(CONTEXTS):
            prompt=stem.format(**context)
            bank.append((f"wq176_gr_{rule_index:02d}_{context_index:02d}",
                         "Grammar",("Easy","Medium","Hard")[rule_index%3],
                         prompt,*options,why))
    assert len(bank)==GRAMMAR
    return bank

def kotlin_source(items):
    # Each raw string stays <64k UTF-8 JVM constant-pool limit; rows are
    # parsed lazily and 4 choices are independently shuffled by the screen.
    chunks=[]
    for index in range(0,len(items),50):
        segment=items[index:index+50]
        data="\n".join("\t".join(safe(str(field)) for field in row) for row in segment)
        assert len(data.encode("utf-8"))<60000,(index,len(data))
        assert "$" not in data and '"""' not in data
        chunks.append('        """'+data+'"""')
    code='''package com.kelimebulmacasi.a1

/** Original supplementary YDS-style practice, not ÖSYM past questions.
 * Source vocabulary: attributed CC BY-SA CEFR wordlists (v1.0.75).
 * Literary reference PDFs are NOT copied or republished.
 */
internal object Wq176YdsGenerated {
    private val segments = listOf(
'''+",\n".join(chunks)+'''
    )
    val questions: List<YdsQuestion> by lazy {
        segments.flatMap { chunk ->
            chunk.trim().lines().filter { it.isNotBlank() }.map { raw ->
                val a=raw.split('\\t')
                require(a.size==9) { "Malformed original YDS practice row" }
                YdsQuestion(
                    id=a[0],topic=YdsTopic.valueOf(a[1]),
                    difficulty=YdsDifficulty.valueOf(a[2]),
                    question=a[3],options=a.subList(4,8),
                    answer=0,explanation=a[8]
                )
            }
        }
    }
}
'''
    return code

def main():
    all_entries=words()
    bank=lexical_questions(all_entries)+grammar_questions()
    assert len(bank)==TARGET+GRAMMAR
    assert len({q[0] for q in bank})==len(bank)
    assert len({q[3].casefold() for q in bank})==len(bank),"Repeated prompt"
    for q in bank:
        assert len(q)==9,q[0]
        assert len({x.casefold() for x in q[4:8]})==4,q[0]
        assert len(q[3])>=20 and len(q[8])>=35,q[0]
    generated=SRC/"Wq176YdsGenerated.kt"
    generated.write_text(kotlin_source(bank),"utf-8")
    shutil.copyfile(HERE/"Wq176DailyChallenge.kt",SRC/"Wq176DailyChallenge.kt")
    shutil.copyfile(HERE/"Wq176ContentRotationTest.kt",TEST/"Wq176ContentRotationTest.kt")

    yds=SRC/"YdsContent.kt"
    original=yds.read_text("utf-8")
    anchor="+ Wq175YdsQualityBatch.questions"
    assert original.count(anchor)==1,("YDS bank append",original.count(anchor))
    yds.write_text(original.replace(anchor,anchor+" + Wq176YdsGenerated.questions",1),"utf-8")

    daily=SRC/"PracticeHubScreen.kt"
    original=daily.read_text("utf-8")
    anchor="        Wq175PremiumPracticeGrid { kind ->"
    assert original.count(anchor)==1,("Daily goal source bank card",original.count(anchor))
    daily.write_text(original.replace(anchor,
        "        Wq176DailyPracticeEntry(session.profileKey)\n"+anchor,1),"utf-8")

    screen=SRC/"YdsCenterScreen.kt"
    original=screen.read_text("utf-8")
    anchor="    fun start(modeExam: Boolean, wrongOnly: Boolean = false, full: Boolean = false) {"
    assert original.count(anchor)==1,("YDS session composer",original.count(anchor))
    original=original.replace(anchor,
        "    val wq176Context = androidx.compose.ui.platform.LocalContext.current\n"+anchor,1)
    old="activeIds = selection.take(if (full) 80 else if (modeExam) 15 else 12).map { it.id }"
    assert original.count(old)==1,("YDS session order",original.count(old))
    new='''activeIds = Wq176QuestionRotation.pick(
            wq176Context, session.profileKey, selection,
            if (full) 80 else if (modeExam) 15 else 12
        ).map { it.id }'''
    screen.write_text(original.replace(old,new,1),"utf-8")

    for filename in ("YdsCenterTest.kt","Wq175FullYdsTest.kt","Wq175YdsBankQualityTest.kt"):
        p=TEST/filename
        text=p.read_text("utf-8")
        assert "198" in text,(filename,"expected 1.0.75 question count")
        text=text.replace("198",str(FINAL_TOTAL))
        p.write_text(text,"utf-8")
    license_dir=ASSETS/"licenses"
    license_dir.mkdir(exist_ok=True)
    (license_dir/"WQ176_CONTENT_SOURCES.md").write_text(
        "# WordQuest 1.0.76 original question content\n"
        "Additional lexical questions derive single-word English/Turkish mappings "
        "from EnglishPractice (CC BY-SA 4.0); see "
        "WORDQUEST_CEFR_175_ATTRIBUTION.md for attribution and ShareAlike terms.\n"
        "240 additional grammar questions are original generated study items.\n"
        "Reference syllabi: user-provided More to Read I/II and Test Attack books; "
        "no publisher book question/paragraph/image is copied into this app.\n"
        "Automated checks validate structure and deduplication, not human pedagogy.\n",
        "utf-8")
    gradle=ROOT/"source/app/build.gradle.kts"
    text=gradle.read_text("utf-8")
    assert text.count("versionCode = 101")==1
    assert text.count('versionName = "1.0.75"')==1
    text=text.replace("versionCode = 101","versionCode = 102",1)
    text=text.replace('versionName = "1.0.75"','versionName = "1.0.76"',1)
    gradle.write_text(text,"utf-8")
    print("PASS: YDS offline bank",FINAL_TOTAL,"total =",198,"existing +",TARGET,
          "lexical/translation +",GRAMMAR,"grammar.")
    print("PASS: daily source-bank drill, per-profile session rotation and Android versionCode 102.")

if __name__=="__main__":
    main()
