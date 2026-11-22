#!/usr/bin/env python3
"""P7 after P4-P6 assembly: Lena, speech, animated face, settings/Arena icons."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
TEST=ROOT/"source/app/src/test/java/com/kelimebulmacasi/a1"
RES=ROOT/"source/app/src/main/res/drawable"
P7=ROOT/"tools/v187"
GRADLE=ROOT/"source/app/build.gradle.kts"
planned={}

def load(name):
    p=APP/name
    if not p.is_file():raise AssertionError("P7 source unavailable: "+name)
    return planned.get(p,p.read_text("utf-8"))
def store(name,s):planned[APP/name]=s
def once(s,old,new,label):
    n=s.count(old)
    if n!=1:raise AssertionError(f"P7 {label}: expected 1 match, got {n}")
    return s.replace(old,new,1)

def conversation():
    s=load("Conversation.kt")
    s=once(s,'    val instructions = """',
      '''    val characterRole=if(avatarPrompt.contains("Lena",ignoreCase=true)) {
        "You are Lena, the in-app English tutor. Speak warmly, naturally, " +
        "and conversationally about everyday topics, hobbies, friends, travel, " +
        "culture, food or whatever the learner actually asks. Never force " +
        "an English exercise during ordinary conversation. Explain or correct " +
        "like a patient teacher when it is useful or requested. Never " +
        "volunteer robotic introductions such as 'as an AI'. When asked " +
        "directly whether you are human, answer honestly that you are a " +
        "virtual tutor, not a real person. Keep Turkish parenthetical " +
        "translations short and useful on screen; the voice player will " +
        "skip them without reading Turkish aloud."
    } else {
        "You are WordQuest AI, a patient personal English teacher for a Turkish-speaking learner."
    }
    val instructions = """''',"select Lena natural conversation role")
    s=once(s,
      '        You are WordQuest AI, a patient personal English teacher for a Turkish-speaking learner.',
      '        $characterRole',"identity remains accurate")
    s=once(s,
      '        Use short, natural replies suitable for speaking aloud. In practice conversations ask one follow-up question at a time.',
      '''        Do not repeat scripted openings or answer an earlier question instead of the newest message.
        Use short, natural replies suitable for speaking aloud. In practice conversations ask one follow-up question at a time.''',
      "reply to the actual last message")
    store("Conversation.kt",s)

    s=load("ConversationScreen.kt")
    # Earlier Lena attachment composer revisions change the number and names
    # of ai.reply parameters. Preserve each original argument exactly.
    matches=list(re.finditer(r'\bai\.reply\(',s))
    if len(matches)!=1:
        raise AssertionError("P7 Lena messenger expected one AI reply, found "+str(len(matches)))
    a=matches[0].start()
    j=matches[0].end()
    depth=1
    while j<len(s) and depth:
        if s[j]=="(":depth+=1
        elif s[j]==")":depth-=1
        j+=1
    if depth!=0:raise AssertionError("P7 unbalanced ai.reply call")
    b=j-1
    prior=s[a:j]
    if "personaOverride" not in prior:
        revised=prior[:-1]+(
            ', personaOverride="You are Lena, the warm and friendly '
            'English conversation tutor.")')
        s=s[:a]+revised+s[b+1:]
    store("ConversationScreen.kt",s)
    s=load("LenaIsolatedVoiceTeacherActivity.kt")
    s=once(s,
      'You are Lena, a warm and encouraging English conversation tutor. Speak naturally, correct gently, and ask one clear question at a time.',
      'You are Lena, a friendly everyday conversation partner and English tutor. Chat naturally about the current topic, help with lessons when useful and do not repeat scripted introductions. If asked whether you are a real human, respond truthfully.',
      "Lena 3D activity persona")
    store("LenaIsolatedVoiceTeacherActivity.kt",s)

    s=load("ConversationAudio.kt")
    mark='val text = turn.answer.replace(Regex("[*_'+chr(96)+'#]"), "").trim()'
    s=once(s,mark,
      'val text = Wq187LenaSpeech.forSpeech(turn.answer,turn.language)',
      "do not TTS-read written parenthetical Turkish explanations")
    store("ConversationAudio.kt",s)

def scene():
    s=load("LenaLive3DStage.kt")
    marker='''        val index = clips[clip] ?: clips["01_Idle_Natural_Breathing"]
            ?: clips.values.firstOrNull() ?: return@LaunchedEffect'''
    s=once(s,marker,marker+'''
        // Separate glTF morph weight animation: lips + natural eye blinks,
        // simultaneously with the existing hand/arm/shoulder/body clips.
        val faceIndex=LenaFaceAnimationClips.select(motion,clips.keys)
            ?.let { clips[it] }''',"Lena facial morph selection")
    marker='''                loop = true, // breathing must loop during entry greeting too
            )'''
    s=once(s,marker,marker+'''
            if(faceIndex!=null && faceIndex!=index) {
                model.playAnimation(faceIndex,
                    speed=if(motion==AvatarMode.SPEAKING)
                        speechRate.coerceIn(.7f,1.3f) else 1f,
                    loop=true,
                )
            }''',"play face morph simultaneously with body")
    store("LenaLive3DStage.kt",s)

def layout():
    s=load("Wq175SettingsCards.kt")
    s=once(s,'Wq175GlossIcon(iconName,Modifier.size(47.dp))',
        'Wq187SettingsArtwork(title,iconName,Modifier.size(47.dp))',
        "unique icon routing on settings cards")
    store("Wq175SettingsCards.kt",s)

    s=load("WordQuest174Arena.kt")
    start=s.index("@Composable\nprivate fun Wq174SettingIcon(kind:String) {")
    end=s.index("\n@Composable",start+14)
    prior=s[start:end]
    if "when(kind)" not in prior or len(prior)>4000:
        raise AssertionError("P7 cannot validate old Arena icon renderer")
    s=s[:start]+'''@Composable
private fun Wq174SettingIcon(kind:String) {
    Wq187ArenaSettingIcon(kind,Modifier.size(39.dp))
}
'''+s[end:]
    s=once(s,'Text("⚙",color=Color.White,fontSize=24.sp)',
        'Wq187ArenaSettingIcon("settings",Modifier.size(32.dp))',
        "Arena header original settings art")
    begin=s.find('            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))\n                .background(Brush.verticalGradient(listOf(')
    if begin<0:raise AssertionError("P7 expected old long Arena rule box")
    end=s.index('\n            }\n',begin)+len('\n            }')
    area=s[begin:end]
    if 'Text("Maç Kuralları"' not in area or "puanlama" not in area:
        raise AssertionError("P7 unsafe Arena rule replacement")
    replacement='''            Wq187InfoTip(
                "Maç kuralları",
                "Seçtiğin seviyeye ve konuya uygun sorular gelir. " +
                "Süre dolduğunda yanıtlar doğrulanır. İpucu ve tekrar " +
                "haklarını maç öncesinde seçebilirsin; çözülemeyen " +
                "kelimeler son tura ertelenir."
            )'''
    s=s[:begin]+replacement+s[end:]
    store("WordQuest174Arena.kt",s)

def audio_diagnostics():
    s=load("WordQuestSettings.kt")
    old='''        Text("Cihaz / çevrimdışı ses durumu", color = Ink, fontFamily = Serif, fontSize = 19.sp)
        Text(pronunciation.accentDiagnostics, fontFamily = Sans)
        Text(pronunciation.voiceDetails, fontFamily = Sans)
        Text("Eski yedek kayıtlar eSpeak ile sentezlenmiştir ve robotiktir. " +
            "Cihaz sesinin de doğal duyulması garanti edilemez. " +
            "Amerikan/Britanya farkını Tomato, Can't, Schedule ile karşılaştırın.",
            fontFamily = Sans, fontSize = 12.sp)'''
    replacement='''        Wq187InfoTip(
            "Cihaz / çevrimdışı ses durumu",
            pronunciation.accentDiagnostics + " — " +
            pronunciation.voiceDetails +
            " — Eski yedek eSpeak kayıtları robotik duyulabilir. " +
            "Cihaz sesi de her zaman doğal olmayabilir. " +
            "US/UK farkını Tomato, Can't ve Schedule ile karşılaştırabilirsin."
        )'''
    s=once(s,old,replacement,
        "move dynamic voice/region diagnostics behind scrollable info without hiding controls")
    store("WordQuestSettings.kt",s)

def privacy():
    s=load("AiProviderSettings.kt")
    old='''        Text("Anahtar yalnızca seçili cihazdaki bu hesaba aittir; Firebase'e veya GitHub'a kaydedilmez. " +
            "Bir API anahtarının cihazda bulunması riski tamamen ortadan kaldırmaz. Öğretmene gönderdiğiniz içerik " +
            "seçili sağlayıcıya iletilir. Sohbet ve mikrofon işlemleri veri kullanımına neden olabilir.",
            fontFamily = Sans)'''
    new='''        Text("Sohbet içerikleri seçtiğin AI sağlayıcısına iletilir.",fontFamily=Sans)
        Wq187InfoTip("Veri ve API anahtarı güvenliği",
            "Anahtar yalnızca seçili cihazdaki bu hesaba aittir; Firebase'e veya GitHub'a kaydedilmez. " +
            "Bir API anahtarının cihazda bulunması riski tamamen ortadan kaldırmaz. " +
            "Öğretmene gönderdiğiniz içerik seçili sağlayıcıya iletilir. " +
            "Sohbet ve mikrofon işlemleri veri kullanımına neden olabilir.")'''
    s=once(s,old,new,"privacy: retain short always-visible disclosure")
    store("AiProviderSettings.kt",s)

def main():
    for name in ("Wq186ArenaFairPlay.kt","LenaFaceAnimationClips.kt",
                 "LenaLive3DStage.kt","Wq175SettingsCards.kt"):
        assert (APP/name).exists(),name
    for name in ("Wq187InfoTip.kt","Wq187Icons.kt",
                 "Wq187LenaSpeech.kt","Wq187LenaSpeechTest.kt",
                 "wq187_settings_icon.xml","wq187_update_icon.xml"):
        assert (P7/name).exists(),name
    assert not (APP/"Wq187LenaSpeech.kt").exists()
    conversation()
    scene()
    layout()
    audio_diagnostics()
    privacy()
    g=GRADLE.read_text("utf-8")
    g=once(g,"versionCode = 112","versionCode = 113","versionCode")
    g=once(g,'versionName = "1.0.86"','versionName = "1.0.87"',"versionName")
    planned[GRADLE]=g
    # Every source anchor verified before any writes to assembled source.
    for p,s in planned.items():
        p.write_text(s,"utf-8")
        print("PASS P7",p.relative_to(ROOT))
    for name in ("Wq187LenaSpeech.kt","Wq187Icons.kt"):
        shutil.copyfile(P7/name,APP/name)
    TEST.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(P7/"Wq187LenaSpeechTest.kt",TEST/"Wq187LenaSpeechTest.kt")
    RES.mkdir(parents=True,exist_ok=True)
    for name in ("wq187_settings_icon.xml","wq187_update_icon.xml"):
        shutil.copyfile(P7/name,RES/name)
    print("P7 Lena chat/3D/voice and icon routing integrated. Existing app user data untouched.")

if __name__=="__main__":
    main()
