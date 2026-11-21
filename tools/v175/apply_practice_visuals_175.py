#!/usr/bin/env python3
"""Visual upgrade for real Daily Plan and its existing playable exercises."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
PKG=ROOT/"tools/v175"
shutil.copyfile(PKG/"WordQuest175PracticeVisuals.kt",
    SRC/"WordQuest175PracticeVisuals.kt")
p=SRC/"PracticeHubScreen.kt"
s=p.read_text("utf-8")
start=s.index('        Text("Günlük çalışma hedefi", fontFamily = Serif, color = Ink)')
end=s.index('        for (kind in PracticeKind.entries) {',start)
old=s[start:end]
assert 'LinearProgressIndicator' in old
assert 'store.minutes = option' in old
s=s[:start]+'''        val fraction = (store.seconds.toFloat() / (mins * 60f)).coerceIn(0f, 1f)
        Wq175DailyPlanBanner(
            minutes=mins,done=done,activeMinutes=doneMinutes,
            remaining=remaining,due=due,fraction=fraction,
            onMinutes={value->store.minutes=value;refresh++}
        )
'''+s[end:]
original='''            MenuItem(kind.title, kind.detail, "✦") { selected = kind; questionIndex = 0 }'''
assert s.count(original)==1
s=s.replace(original,
    '''            Wq175PracticeTile(kind) { selected = kind; questionIndex = 0 }''',1)
start=s.index('    ScreenPage(kind.title,')
end=s.index('\n',start)
assert s[start:end].endswith('back) {')
s=s[:end+1]+'''        Wq175PracticeQuestionHeader(kind,number)
'''+s[end+1:]
p.write_text(s,"utf-8")
assert s.count("Wq175DailyPlanBanner(")==1
assert s.count("Wq175PracticeTile(")==1
assert s.count("Wq175PracticeQuestionHeader(")==1
print("PASS: airy Daily Plan and 5 individual exercise room identities")
