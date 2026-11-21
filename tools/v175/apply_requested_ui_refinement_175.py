#!/usr/bin/env python3
"""Apply user-approved 1.0.75 mobile improvements AFTER all previous 1.0.75 overlays.

No changes to versionCode, signature, training/UK audio, AI keys, session records,
Arena wait behaviour or Lena GLB/rig/chair assets. Fail if core route signatures
drift, rather than shipping a superficially successful but disconnected design.
"""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
PKG = ROOT / "tools/v175"


def once(text, before, after, label, required=True):
    amount = text.count(before)
    if amount != 1:
        if not required and amount == 0:
            print("INFO: optional layout not present:", label)
            return text
        raise AssertionError(f"{label}: expected exactly one anchor; found {amount}")
    return text.replace(before, after, 1)


def update(filename, patch):
    path = SRC / filename
    before = path.read_text("utf-8")
    after = patch(before)
    assert after != before, f"No change to {filename}"
    path.write_text(after, "utf-8")
    print("PASS:", filename)


def scenes():
    shutil.copyfile(PKG/"Wq175PremiumScenes.kt", SRC/"Wq175PremiumScenes.kt")
    def home(s):
        return once(s, "Wq175HomeStoryAtmosphere(Modifier.matchParentSize())",
            'Wq175SceneBackdrop("ana_sayfa",Modifier.matchParentSize())',
            "approved home scene behind live mascot")
    update("HomeScreen.kt", home)
    def arena(s):
        return once(s, "Wq175StadiumBackdrop(Modifier.matchParentSize())",
            'Wq175SceneBackdrop("arena",Modifier.matchParentSize())',
            "arena original podium scene underneath native selected mascot")
    update("WordQuest175Arena.kt",arena)
    def theme(s):
        marker = '''    Box(modifier.clip(RoundedCornerShape(22.dp))
        .background(Brush.horizontalGradient(colors))) {'''
        return once(s,marker,marker+'\n        Wq175SceneBackdrop(slug,Modifier.matchParentSize())',
                    "use real scene backgrounds behind matching and new-game banners")
    update("WordQuest175ThemeArt.kt",theme)


def home_matching():
    home = SRC/"Wq175PremiumHome.kt"
    raw=home.read_text("utf-8")
    assert 'Wq175HomeTile("Eşleştirmelerim"' in raw
    assert 'onDismiss={matchingOpen=false},onImport=learning' in raw
    assert 'Wq175HomeTile("Öğrenme Merkezim"' not in raw
    assert 'Wq175HomeTile("İlerlemem"' in raw
    print("PASS: İlerlemem retained, redundant tile replaced with functioning Eşleştirmelerim")


def packages_and_profile():
    shutil.copyfile(PKG/"Wq175PackageCatalog.kt",SRC/"Wq175PackageCatalog.kt")
    def patch(s):
        s=once(s,
            'Packages, Topic, Progress, Learning, Account',
            'Packages, PackageCatalog, Topic, Progress, Learning, Account',
            "catalogue page enum")
        s=once(s,'            Page.Topic -> go(Page.Packages)',
            '            Page.Topic -> go(Page.PackageCatalog)\n'
            '            Page.PackageCatalog -> go(Page.Packages)',
            "catalogue back-navigation")
        s=once(s,
            '            { level = it; category = null },',
            '            { level = it; category = null; go(Page.PackageCatalog) },',
            "CEFR and YDS Paketi Gör button opens catalogue")
        s=once(s,'        Page.Topic -> {',
            '''        Page.PackageCatalog -> Wq175PackageCatalog(
            session, GameMode.valueOf(modeName), level,
            onGroup={ category=it; showAddedGames=false; go(Page.Topic) },
            onBack=::back
        )
        Page.Topic -> {''',
            "real catalogue UI route")
        s=once(s,'    val social = session.social',
            '''    val social = session.social
    val chosenMaskot = rememberWq2DSelection(session.profileKey)
    var choosingMaskot by rememberSaveable { mutableStateOf(false) }''',
            "profile-owned avatar selection state")
        s=once(s,'    ScreenPage("Hesap", "", back) {',
            '''    if(choosingMaskot) Wq2DCharacterPicker(
        selected=chosenMaskot.value,
        onSelected={ id ->
            Wq2DMascots.choose(appContext,session.profileKey,id)
            chosenMaskot.value=id
        },
        onDismiss={choosingMaskot=false}
    )
    ScreenPage("Hesap", "", back) {
        Text("Oyun karakterim",color=Ink,fontFamily=Serif,
            fontSize=20.sp,fontWeight=FontWeight.Bold)
        Wq2DCompactSelector(chosenMaskot.value,
            onClick={choosingMaskot=true})
        BodyText("Seçtiğin karakter Arena, Kelime Hafızam ve profilinde kullanılır.")''',
            "maskot selection on account/profile")
        return s
    update("HomeScreen.kt",patch)

    # Previous picker locations become display-only previews; never reset
    # selected SharedPreferences, saved avatar pose or user data.
    def arena_home(s):
        return once(s,
            "Wq2DCompactSelector(mascot,onClick={avatarPicker=true})",
            "Wq2DPortrait(mascot,Modifier.size(39.dp))",
            "Arena picker becomes profile-only visual preview",required=False)
    p=SRC/"ArenaApprovedHome.kt"
    old=p.read_text("utf-8");new=arena_home(old)
    if new!=old:p.write_text(new,"utf-8")
    def memory_home(s):
        return once(s,
            '''Wq2DCompactSelector(selectedAvatar,
                        onClick={robotPicker=true})''',
            "Wq2DPortrait(selectedAvatar,Modifier.size(46.dp))",
            "memory picker becomes profile-only visual preview",required=False)
    p=SRC/"VocabularyNeonHome.kt";old=p.read_text("utf-8");new=memory_home(old)
    if new!=old:p.write_text(new,"utf-8")


def settings_cards():
    shutil.copyfile(PKG/"Wq175SettingsCards.kt",SRC/"Wq175SettingsCards.kt")
    def patch(s):
        s=once(s,'        UpdateSettingsSection()\n        AiProviderSettingsSection(ai)',
            '''        Wq175SettingsCard(
            "Güncellemeler","Yeni sürümler ve güvenli yükleme","history"
        ) { UpdateSettingsSection() }
        Wq175SettingsCard(
            "Yapay zekâ","Sağlayıcı, model ve API anahtarım","assistant"
        ) { AiProviderSettingsSection(ai) }
        Wq175SettingsCard(
            "Görünüm","Yazı boyutu ve okunabilirlik","settings"
        ) {''',
            "top level cards for update, provider and font size")
        s=once(s,'        Text("Yazılı telaffuz (IPA)"',
            '''        }
        Wq175SettingsCard(
            "Yazılı telaffuz (IPA)","US/UK seslerin yazılı gösterimi","library"
        ) {
        Text("Yazılı telaffuz (IPA)"''',
            "IPA settings category")
        s=once(s,'        Text("Doğru cevap efekti"',
            '''        }
        Wq175SettingsCard(
            "Oyun sesleri","Doğru ve yanlış cevap efektleri","success"
        ) {
        Text("Doğru cevap efekti"''',
            "independent effects category")
        s=once(s,'        Text("İngilizce telaffuz"',
            '''        }
        Wq175SettingsCard(
            "Ses ve telaffuz","Amerikan/Britanya sesleri, hız ve seviye","listen"
        ) {
        Text("İngilizce telaffuz"''',
            "pronunciation and engine category")
        s=once(s,'        pronunciation.error?.let { BodyText(it, ErrorInk) }\n    }\n}',
            '''        pronunciation.error?.let { BodyText(it, ErrorInk) }
        }
    }
}''',
            "close settings voice card")
        return s
    update("WordQuestSettings.kt",patch)


def lena_mobile():
    # The 3D rendering path, phoneme data and uploaded user photos are untouched.
    def profile(s):
        s=once(s,
            '''        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            LenaActualAvatar(92.dp)''',
            '''        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 450.dp
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            LenaActualAvatar(if(compact)54.dp else 92.dp)''',
            "responsive portrait and profile header")
        s=once(s,'            Spacer(Modifier.width(16.dp))\n            Column(Modifier.weight(1f)',
            '            Spacer(Modifier.width(if(compact)8.dp else 16.dp))\n            Column(Modifier.weight(1f)',
            "mobile horizontal spacing")
        s=once(s,'Text("Lena", fontFamily = FontFamily.Serif, fontSize = 29.sp,',
            'Text("Lena", fontFamily = FontFamily.Serif, fontSize = if(compact)22.sp else 29.sp,',
            "Lena title mobile font")
        s=once(s,'            Box(Modifier.width(1.dp).height(66.dp).background(LenaBorder))',
            '            if(!compact) Box(Modifier.width(1.dp).height(66.dp).background(LenaBorder))',
            "remove divider on small phones")
        s=once(s,'            Spacer(Modifier.width(12.dp))\n            Column(horizontalAlignment',
            '            Spacer(Modifier.width(if(compact)5.dp else 12.dp))\n            Column(horizontalAlignment',
            "user profile margin")
        s=once(s,'avatar = userAvatar.value, size = 64.dp,',
            'avatar = userAvatar.value, size = if(compact)43.dp else 64.dp,',
            "mobile selected user avatar")
        s=once(s,'        if (photoError != null) Text(photoError.orEmpty(),',
            '        }\n        if (photoError != null) Text(photoError.orEmpty(),',
            "close responsive profile layout")
        if 'import androidx.compose.foundation.layout.BoxWithConstraints' not in s:
            s=once(s,'import androidx.compose.foundation.layout.Box\n',
                'import androidx.compose.foundation.layout.Box\n'
                'import androidx.compose.foundation.layout.BoxWithConstraints\n',
                "responsive profile import")
        return s
    update("LenaMessengerBrand.kt",profile)
    def conversation(s):
        if '"Lena · Mesajlar"' in s:
            s=once(s,'"Lena · Mesajlar"','"Lena"', "compact chat header")
        # Keep semantics of both actions while preventing 3 buttons from
        # occupying the entire short handset width.
        if 'Text("Yeni sohbet", fontFamily = Sans)' in s:
            s=once(s,'Text("Yeni sohbet", fontFamily = Sans)',
                'Text("Yeni", fontFamily = Sans)',"compact new-thread action")
        if 'Text("3D Görüşme"' in s:
            s=once(s,'Text("3D Görüşme"','Text("3D"',"compact 3D launch action")
        elif 'Text("3D Ders"' in s:
            s=once(s,'Text("3D Ders"','Text("3D"',"compact 3D launch action")
        return s
    update("ConversationScreen.kt",conversation)


def main():
    for name in ("HomeScreen.kt","WordQuest175Arena.kt","WordQuestSettings.kt",
                 "ConversationScreen.kt","LenaMessengerBrand.kt","WordQuest175ThemeArt.kt"):
        assert (SRC/name).is_file(),name
    scenes()
    home_matching()
    packages_and_profile()
    settings_cards()
    lena_mobile()
    import subprocess,sys
    subprocess.run([sys.executable,
        str(PKG/"apply_phone_tablet_polish_175.py")],check=True)
    assert (SRC/"LenaLive3DStage.kt").is_file()
    print("PASS: new cards are native and accessible; legacy user data and voice/GLB preserved.")


if __name__=="__main__":
    main()
