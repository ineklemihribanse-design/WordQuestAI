from pathlib import Path
import json,re
root=Path(__file__).resolve().parents[2]
src=root/'source/app/src/main'
java=src/'java/com/kelimebulmacasi/a1'
data=(root/'tools/arena_v150_server/learning_content.mjs').read_text('utf-8')
words=json.loads(data.split('export const authoredWords = ',1)[1].split(';\nexport const authoredQuestions',1)[0])
qs=json.loads(data.split('export const authoredQuestions = ',1)[1].rstrip().removesuffix(';'))
# Existing YDS Academy has a strict four-choice contract; keep it unchanged.
# Arena uses all five authored choices.
qs=[q for q in qs if q['yds']]
def k(s):
 return json.dumps(s,ensure_ascii=False).replace('$','\\$')
rows=[]
for q in qs:
 opts=', '.join(k(o) for o in q['options'][:4])
 diff={'easy':'Easy','medium':'Medium','hard':'Hard'}[q['yds']]
 rows.append('        YdsQuestion('+', '.join([k(q['id']),'YdsTopic.'+q['topic'],'YdsDifficulty.'+diff,k(q['prompt']),'listOf('+opts+')','0',k(q['explanation'])])+')')
(java/'Wq150LearningQuestions.kt').write_text('package com.kelimebulmacasi.a1\n\ninternal object Wq150LearningQuestions {\n    val questions = listOf(\n'+',\n'.join(rows)+'\n    )\n}\n','utf-8')
yds=java/'YdsContent.kt'
s=yds.read_text('utf-8')
anchor='Wq176YdsGenerated.questions'
assert s.count(anchor)==1
yds.write_text(s.replace(anchor,anchor+' + Wq150LearningQuestions.questions',1),'utf-8')
tests=root/'source/app/src/test/java/com/kelimebulmacasi/a1'
for name in ('YdsCenterTest.kt','Wq175FullYdsTest.kt','Wq175YdsBankQualityTest.kt','Wq176ContentRotationTest.kt'):
 p=tests/name
 if p.exists():
  t=p.read_text('utf-8')
  p.write_text(re.sub(r'\b2818\b',str(2818+len(qs)),t),'utf-8')
# Add independently authored single-word senses to the existing vocabulary
# addendum without deleting old entries or overwriting a known CEFR headword.
asset=src/'assets/vocabulary-addendum-v175.tsv'
old=asset.read_text('utf-8')
seen={(a[0],a[3]) for line in old.splitlines() if len(a:=line.split('\t'))==5}
for line in (src/'assets/vocabulary-extra.tsv').read_text('utf-8').splitlines():
 a=line.split('\t')
 if len(a)==5:seen.add((a[0],a[3]))
new=[]
for w in words:
 key=(w['level'],w['answer'])
 if key in seen:continue
 seen.add(key);new.append('\t'.join([w['level'],w['category'],w['wordType'],w['answer'],w['meaning']]))
asset.write_text(old.rstrip('\n')+'\n'+'\n'.join(new)+'\n','utf-8')
print('Wq150 learning: added Academy questions:',len(qs),'additional CEFR rows:',len(new))
