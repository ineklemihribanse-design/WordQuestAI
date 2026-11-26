from pathlib import Path
import json,re
root=Path(__file__).resolve().parents[2]
src=root/'source/app/src/main'
java=src/'java/com/kelimebulmacasi/a1'
data=(root/'tools/arena_v150_server/learning_content.mjs').read_text('utf-8')
words=json.loads(data.split('export const authoredWords = ',1)[1].split(';\nexport const authoredQuestions',1)[0])
qs=json.loads(data.split('export const authoredQuestions = ',1)[1].rstrip().removesuffix(';'))
more=(root/'tools/arena_v150_server/learning_more.mjs').read_text('utf-8')
qs += json.loads(more.split('export const extraQuestions = ',1)[1].split(';\nexport const grammarVariants = ',1)[0])
academic=(root/'tools/arena_v150_server/learning_academic.mjs').read_text('utf-8')
words += json.loads(academic.split('export const academicWords = ',1)[1].split(';\nexport const collocationQuestions = ',1)[0])
qs += json.loads(academic.split('export const collocationQuestions = ',1)[1].rstrip().removesuffix(';'))
reading=(root/'tools/arena_v150_server/learning_reading.mjs').read_text('utf-8')
qs += json.loads(reading.split('export const readingQuestions = ',1)[1].rstrip().removesuffix(';'))
coherence=(root/'tools/arena_v150_server/learning_coherence.mjs').read_text('utf-8')
qs += json.loads(coherence.split('export const coherenceQuestions = ',1)[1].rstrip().removesuffix(';'))
wordforms=(root/'tools/arena_v150_server/learning_wordforms.mjs').read_text('utf-8')
words += json.loads(wordforms.split('export const wordFormWords = ',1)[1].split(';\nexport const wordFormQuestions = ',1)[0])
qs += json.loads(wordforms.split('export const wordFormQuestions = ',1)[1].rstrip().removesuffix(';'))
relations=(root/'tools/arena_v150_server/learning_relations.mjs').read_text('utf-8')
words += json.loads(relations.split('export const lexicalRelationWords = ',1)[1].split(';\nexport const lexicalRelationQuestions = ',1)[0])
qs += json.loads(relations.split('export const lexicalRelationQuestions = ',1)[1].rstrip().removesuffix(';'))
domains=(root/'tools/arena_v150_server/learning_domains.mjs').read_text('utf-8')
words += json.loads(domains.split('export const domainWords = ',1)[1].split(';\nexport const domainQuestions = ',1)[0])
qs += json.loads(domains.split('export const domainQuestions = ',1)[1].rstrip().removesuffix(';'))
context=(root/'tools/arena_v150_server/learning_context_batch.mjs').read_text('utf-8')
qs += json.loads(context.split('export const contextQuestions = ',1)[1].rstrip().removesuffix(';'))
deep=(root/'tools/arena_v150_server/learning_deep_reading.mjs').read_text('utf-8')
qs += json.loads(deep.split('export const deepReadingQuestions = ',1)[1].rstrip().removesuffix(';'))
foundation=(root/'tools/arena_v150_server/learning_foundation.mjs').read_text('utf-8')
qs += json.loads(foundation.split('export const foundationQuestions = ',1)[1].rstrip().removesuffix(';'))
advanced=(root/'tools/arena_v150_server/learning_advanced.mjs').read_text('utf-8')
qs += json.loads(advanced.split('export const advancedQuestions = ',1)[1].rstrip().removesuffix(';'))
logic=(root/'tools/arena_v150_server/learning_logic.mjs').read_text('utf-8')
qs += json.loads(logic.split('export const logicQuestions = ',1)[1].rstrip().removesuffix(';'))
research=(root/'tools/arena_v150_server/learning_research.mjs').read_text('utf-8')
qs += json.loads(research.split('export const researchQuestions = ',1)[1].rstrip().removesuffix(';'))
cohesion_exp=(root/'tools/arena_v150_server/learning_cohesion_expansion.mjs').read_text('utf-8')
qs += json.loads(cohesion_exp.split('export const cohesionExpansion = ',1)[1].rstrip().removesuffix(';'))
lexfast=(root/'tools/arena_v150_server/learning_lexical_fast.mjs').read_text('utf-8')
qs += json.loads(lexfast.split('export const lexicalFastQuestions = ',1)[1].rstrip().removesuffix(';'))
bulkphrase=(root/'tools/arena_v150_server/learning_bulk_phrases.mjs').read_text('utf-8')
qs += json.loads(bulkphrase.split('export const bulkPhraseQuestions = ',1)[1].rstrip().removesuffix(';'))
prep_basic=(root/'tools/arena_v150_server/learning_preposition_basic.mjs').read_text('utf-8')
qs += json.loads(prep_basic.split('export const prepositionBasicQuestions = ',1)[1].rstrip().removesuffix(';'))
verbalnew=(root/'tools/arena_v150_server/learning_verbal_new.mjs').read_text('utf-8')
qs += json.loads(verbalnew.split('export const verbalNewQuestions = ',1)[1].rstrip().removesuffix(';'))
formext=(root/'tools/arena_v150_server/learning_form_extension.mjs').read_text('utf-8')
qs += json.loads(formext.split('export const formExtension = ',1)[1].rstrip().removesuffix(';'))
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
chunks=['    private fun batch'+str(i//30)+'() = listOf(\n'+',\n'.join(rows[i:i+30])+'\n    )' for i in range(0,len(rows),30)]
joined=' + '.join('batch'+str(i)+'()' for i in range(len(chunks)))
(java/'Wq150LearningQuestions.kt').write_text('package com.kelimebulmacasi.a1\n\ninternal object Wq150LearningQuestions {\n'+'\n'.join(chunks)+'\n    val questions by lazy { '+joined+' }\n}\n','utf-8')
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

# Local YDS crosswords previously read only the old GameMode.Yds boards.
# Add explicit academic records to this pool; general CEFR words are not
# automatically reclassified as YDS. Keep all existing games and senses.
levels={'easy':'Kolay','medium':'Orta','hard':'Zor'}
lex=[]
for w in words:
 lex.append('        WordSense(Entry('+k(w['answer'])+', '+k(w['meaning'])+'), '+k(levels[w['yds']])+', '+k(w['category'])+', '+k(w['wordType'])+')')
parts=['    private fun group'+str(i//60)+'() = listOf(\n'+',\n'.join(lex[i:i+60])+'\n    )' for i in range(0,len(lex),60)]
join=' + '.join('group'+str(i)+'()' for i in range(len(parts)))
(java/'Wq150AcademicWords.kt').write_text('package com.kelimebulmacasi.a1\n\ninternal object Wq150AcademicWords {\n'+'\n'.join(parts)+'\n    val senses by lazy { ('+join+').groupBy(WordSense::level) }\n}\n','utf-8')
bank=java/'VocabularyBank.kt'
original=bank.read_text('utf-8')
anchor='private fun senses(level: String) = levelBanks[level] ?: ydsBanks[level].orEmpty()'
assert original.count(anchor)==1, 'Unexpected VocabularyBank contract'
bank.write_text(original.replace(anchor,'private fun senses(level: String) = levelBanks[level] ?: (ydsBanks[level].orEmpty() + Wq150AcademicWords.senses[level].orEmpty()).distinct()',1),'utf-8')
print('Wq150 local YDS academic words:',len(words))
