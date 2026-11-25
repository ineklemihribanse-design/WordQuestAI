#!/usr/bin/env python3
from pathlib import Path
import hashlib,shutil
main=Path("source/app/src/main")
p=main/"java/com/kelimebulmacasi/a1"
pack=Path(__file__).resolve().parent
protected={f:hashlib.sha256(f.read_bytes()).digest() for f in main.rglob("*") if f.is_file() and f.name!="PremiumArenaScreen.kt"}
f=p/"PremiumArenaScreen.kt";s=f.read_text()
def once(s,a,b):
    assert s.count(a)==1,(a[:110],s.count(a))
    return s.replace(a,b,1)
s=once(s,"    fun begin() {",'''    val webEngineScope=rememberCoroutineScope()
    var webEngineBusy by remember { mutableStateOf(false) }
    fun begin() {
        if(webEngineBusy)return''')
a=s.index('        if (selectedMode == "crossword") {',s.index('    fun begin()'))
b=s.index('    fun returnToLobby()',a)
old=s[a:b]
assert 'val chosen = chosenLevels.shuffled()' in old and 'stage = PremiumStage.FiveChoices' in old
# All solo modes now use the signed web engine. Native board rendering remains unchanged.
s=s[:a]+'''        webEngineBusy=true
        feedback="Oyun hazırlanıyor…"
        val requestMode=if(selectedMode=="quiz")"quiz" else soloArenaMode
        val requestId=roundId
        val requestSize=if(requestMode=="quiz")quizCount else size
        webEngineScope.launch {
            try {
                val result=Wq132WebEngine.generate(context,requestMode,chosenLevels,topic,requestSize,timeSeconds,preset.hints,preset.retries,requestId.hashCode())
                if(roundId!=requestId||stage!=PremiumStage.Lobby)return@launch
                if(requestMode!="quiz"){
                    board=result.board ?: error("Bulmaca üretilemedi")
                    resetBoard()
                    stage=PremiumStage.Crossword
                } else {
                    quiz=result.questions
                    quizIndex=0;quizChoice=-1;quizCorrect=0;quizAnswered=0;score=0
                    stage=PremiumStage.FiveChoices
                    remaining=timeSeconds
                }
                feedback=""
            } catch(e:kotlinx.coroutines.CancellationException) { throw e }
            catch(e:Exception) { feedback="Oyun hazırlanamadı: "+(e.message?:"Farklı ayarları deneyebilirsin.").take(200) }
            finally { webEngineBusy=false }
        }
    }
'''+s[b:]
if "import kotlinx.coroutines.launch\n" not in s and "import kotlinx.coroutines.*\n" not in s:
    s=once(s,"package com.kelimebulmacasi.a1\n","package com.kelimebulmacasi.a1\n\nimport kotlinx.coroutines.launch\n")
f.write_text(s)
shutil.copyfile(pack/"Wq132WebEngine.kt",p/"Wq132WebEngine.kt")
out=main/"assets/web";out.mkdir(parents=True,exist_ok=True)
shutil.copyfile(pack/"arena_engine.html",out/"arena_engine.html")
assert all(hashlib.sha256(f.read_bytes()).digest()==v for f,v in protected.items())
print("PASS: solo Arena executes signed web engine; board drawing and existing sources protected")
