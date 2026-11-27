from pathlib import Path
import json,re,hashlib
root=Path(__file__).resolve().parents[2]
src=root/'source/app/src/main'
java=src/'java/com/kelimebulmacasi/a1'
# Read the named JSON array independently of whitespace between JS exports.
# Reject malformed arrays, duplicate declarations and unexpected trailing data.
def read_array(source, name):
 pattern=r'^export[ \t]+const[ \t]+'+re.escape(name)+r'[ \t]*=[ \t]*'
 matches=list(re.finditer(pattern,source,re.M))
 if len(matches)!=1:
  raise ValueError(f'Expected one export for {name}, got {len(matches)}')
 tail=source[matches[0].end():].lstrip()
 value,end=json.JSONDecoder().raw_decode(tail)
 remainder=tail[end:].lstrip()
 if not isinstance(value,list) or not remainder.startswith(';'):
  raise ValueError(f'Invalid JSON array export: {name}')
 remainder=remainder[1:].lstrip()
 if remainder and not re.match(r'export\s+const\s+\w+\s*=',remainder):
  raise ValueError(f'Unexpected data after {name}')
 return value

data=(root/'tools/arena_v150_server/learning_content.mjs').read_text('utf-8')
words=read_array(data, 'authoredWords')
qs=read_array(data, 'authoredQuestions')
more=(root/'tools/arena_v150_server/learning_more.mjs').read_text('utf-8')
qs += read_array(more, 'extraQuestions')
academic=(root/'tools/arena_v150_server/learning_academic.mjs').read_text('utf-8')
words += read_array(academic, 'academicWords')
qs += read_array(academic, 'collocationQuestions')
reading=(root/'tools/arena_v150_server/learning_reading.mjs').read_text('utf-8')
qs += read_array(reading, 'readingQuestions')
coherence=(root/'tools/arena_v150_server/learning_coherence.mjs').read_text('utf-8')
qs += read_array(coherence, 'coherenceQuestions')
wordforms=(root/'tools/arena_v150_server/learning_wordforms.mjs').read_text('utf-8')
words += read_array(wordforms, 'wordFormWords')
qs += read_array(wordforms, 'wordFormQuestions')
relations=(root/'tools/arena_v150_server/learning_relations.mjs').read_text('utf-8')
words += read_array(relations, 'lexicalRelationWords')
qs += read_array(relations, 'lexicalRelationQuestions')
domains=(root/'tools/arena_v150_server/learning_domains.mjs').read_text('utf-8')
words += read_array(domains, 'domainWords')
qs += read_array(domains, 'domainQuestions')
context=(root/'tools/arena_v150_server/learning_context_batch.mjs').read_text('utf-8')
qs += read_array(context, 'contextQuestions')
deep=(root/'tools/arena_v150_server/learning_deep_reading.mjs').read_text('utf-8')
qs += read_array(deep, 'deepReadingQuestions')
foundation=(root/'tools/arena_v150_server/learning_foundation.mjs').read_text('utf-8')
qs += read_array(foundation, 'foundationQuestions')
advanced=(root/'tools/arena_v150_server/learning_advanced.mjs').read_text('utf-8')
qs += read_array(advanced, 'advancedQuestions')
logic=(root/'tools/arena_v150_server/learning_logic.mjs').read_text('utf-8')
qs += read_array(logic, 'logicQuestions')
research=(root/'tools/arena_v150_server/learning_research.mjs').read_text('utf-8')
qs += read_array(research, 'researchQuestions')
cohesion_exp=(root/'tools/arena_v150_server/learning_cohesion_expansion.mjs').read_text('utf-8')
qs += read_array(cohesion_exp, 'cohesionExpansion')
lexfast=(root/'tools/arena_v150_server/learning_lexical_fast.mjs').read_text('utf-8')
qs += read_array(lexfast, 'lexicalFastQuestions')
bulkphrase=(root/'tools/arena_v150_server/learning_bulk_phrases.mjs').read_text('utf-8')
qs += read_array(bulkphrase, 'bulkPhraseQuestions')
prep_basic=(root/'tools/arena_v150_server/learning_preposition_basic.mjs').read_text('utf-8')
qs += read_array(prep_basic, 'prepositionBasicQuestions')
verbalnew=(root/'tools/arena_v150_server/learning_verbal_new.mjs').read_text('utf-8')
qs += read_array(verbalnew, 'verbalNewQuestions')
formext=(root/'tools/arena_v150_server/learning_form_extension.mjs').read_text('utf-8')
qs += read_array(formext, 'formExtension')
cloze_exp=(root/'tools/arena_v150_server/learning_cloze_expansion.mjs').read_text('utf-8')
qs += read_array(cloze_exp, 'clozeExpansion')
# Existing YDS Academy has a strict four-choice contract; keep it unchanged.
# Arena uses all five authored choices.
qs=[q for q in qs if q['yds']]
def k(s):
 return json.dumps(s,ensure_ascii=False).replace('$','\\$')
rows=[]
for q in qs:
 # Keep the actual correct option even if it was originally fifth.
 correct=q['correct']
 assert isinstance(correct,int) and 0<=correct<len(q['options']), q['id']
 answer=q['options'][correct]
 selected=[answer]+[o for i,o in enumerate(q['options']) if i!=correct][:3]
 # Stable per-ID rotation avoids placing every imported correct answer first.
 offset=int(hashlib.sha256(q['id'].encode('utf-8')).hexdigest()[:8],16)%4
 selected=selected[-offset:]+selected[:-offset] if offset else selected
 answer_index=selected.index(answer)
 assert selected[answer_index]==answer
 assert len(selected)==4 and len({o.casefold() for o in selected})==4, q['id']
 opts=', '.join(k(o) for o in selected)
 diff={'easy':'Easy','medium':'Medium','hard':'Hard'}[q['yds']]
 rows.append('        YdsQuestion('+', '.join([k(q['id']),'YdsTopic.'+q['topic'],'YdsDifficulty.'+diff,k(q['prompt']),'listOf('+opts+')',str(answer_index),k(q['explanation'])])+')')
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
# Record the complete set of authored YDS headwords, including senses skipped
# by the CEFR addendum deduplication. The pronunciation generator consumes it.
pronunciation_words=sorted({w['answer'].strip().lower() for w in words})
assert all(re.fullmatch(r'[a-z]{2,24}',w) for w in pronunciation_words)
(src/'assets/learning-pronunciation-words.json').write_text(json.dumps(pronunciation_words,ensure_ascii=False)+'\n','utf-8')
print('Wq150 local YDS academic words:',len(words))
