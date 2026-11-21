#!/usr/bin/env python3
"""Overlay after v181; preserve signed app identity, premium existing icons and saved data."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[2]
PKG=ROOT/'tools/v182'
SRC=ROOT/'source/app/src/main/java/com/kelimebulmacasi/a1'
ASSETS=ROOT/'source/app/src/main/assets'

def once(s,old,new,label):
    n=s.count(old)
    if n!=1:raise AssertionError(f'{label}: expected 1 anchor, got {n}')
    return s.replace(old,new,1)
def edit(name,transform):
    f=SRC/name;old=f.read_text('utf-8');new=transform(old)
    assert new!=old,name
    f.write_text(new,'utf-8')
    print('PASS v182',name)

for name in ('Wq182Icons.kt','Wq182QuizAndInfo.kt'):
    shutil.copyfile(PKG/name,SRC/name)
ASSETS.mkdir(parents=True,exist_ok=True)
shutil.copyfile(PKG/'wq182_approved_33_icons_atlas.webp',ASSETS/'wq182_approved_33_icons_atlas.webp')
raw=(ASSETS/'wq182_approved_33_icons_atlas.webp').read_bytes()
assert raw[:4]==b'RIFF' and raw[8:12]==b'WEBP' and len(raw)>150000

def icons(s):
    # v175 approved 176-file art ZIP is present in production: keep its
    # existing userIcon fallback, but selectively prefer this new 33-icon pack.
    intro='''    val selectedReplacement=Wq182Icons.preferred(name)
    if(selectedReplacement!=null && Wq182Artwork(selectedReplacement,modifier)) {
        // New approved icon drawn; no duplicate.
    } else '''
    if '    if (userIcon != null) {' in s:
        return once(s,'    if (userIcon != null) {',
            intro+'if (userIcon != null) {',
            'new replacement before existing 176-file approved user art')
    return once(s,'    if (approved != null) {',
        intro+'if (approved != null) {',
        'new replacement before existing dedicated approved drawables')
edit('WordQuest175Icons.kt',icons)

def home(s):
    return once(s,
        '''                Wq175GlossIcon(
                    if(tile.artSlug!=null) Wq175ThemeArt.glossName(tile.artSlug)
                    else tile.icon,Modifier.size(imageSize))''',
        '''                if(tile.artSlug=="bulmacalarim" &&
                    Wq182Artwork("wq_bulmacalarim",Modifier.size(imageSize))) {
                    // A unique puzzle illustration, never the Arena team symbol.
                } else Wq175GlossIcon(
                    if(tile.artSlug!=null) Wq175ThemeArt.glossName(tile.artSlug)
                    else tile.icon,Modifier.size(imageSize))''',
        'unique puzzles icon')
edit('Wq175PremiumHome.kt',home)

def quiz(s):
    before='''                        question.options.forEachIndexed { n, option ->
                            val answered = own[index]
                            val bg = when {
                                answered != null && n == question.correct -> Color(0xFF355E48)
                                answered == n -> Color(0xFF743B35)
                                else -> Color(0xFF294C3E)
                            }
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(bg).clickable(enabled = answered == null) {
                                        client.answer(index, n)
                                        if (n == question.correct) sounds.correct(session.preferences)
                                        else sounds.wrong(session.preferences)
                                    }
                                .padding(14.dp)) {
                                Text("${"ABCDE"[n]}. $option", color = OnlineCream,
                                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }'''
    after='''                        question.options.forEachIndexed { n, option ->
                            val answered=own[index]
                            Wq182QuizAnswer(
                                letter="ABCDE"[n],answer=option,
                                chosen=answered==n,correct=n==question.correct,
                                hasAnswered=answered!=null,
                                onPick={
                                    if(own[index]==null) {
                                        client.answer(index,n)
                                        if(n==question.correct) sounds.correct(session.preferences)
                                        else sounds.wrong(session.preferences)
                                    }
                                }
                            )
                        }'''
    s=once(s,before,after,'five-choice answer buttons')
    s=once(s,
        '                        Text(question.prompt, color = OnlineCream, fontSize = 20.sp, fontWeight = FontWeight.Bold)',
        '''                        Text(question.prompt, color = Color.White, fontSize = 21.sp,
                            lineHeight=28.sp, fontWeight = FontWeight.Bold)''',
        'prompt readability')
    s=once(s,
        '''                    ArenaScoreStrip(own.size, questions.size, myScore, null)
                    ArenaPanelBlock {''',
        '''                    ArenaScoreStrip(own.size, questions.size, myScore, null)
                    Wq182Info("Düello kuralları",
                        "Beş seçenekten birini seç. Doğru cevap puan kazandırır; yanlış cevap puan azaltabilir. Süre dolduğunda soru cevaplanmamış sayılır.")
                    ArenaPanelBlock {''',
        'small info button for quiz rules')
    return s
edit('PremiumOnlineScreen.kt',quiz)

def account(s):
    s=once(s,
        '''                Text(session.accountName?.takeIf { it.isNotBlank() } ?: "Oyuncu",
                    fontFamily=Serif,color=Ink,fontSize=25.sp,
                    fontWeight=FontWeight.ExtraBold)''',
        '''                Text(social.nickname.ifBlank { "Kullanıcı adını belirle" },
                    fontFamily=Serif,color=Ink,fontSize=22.sp,
                    fontWeight=FontWeight.ExtraBold,maxLines=2)''',
        'never show email-derived Google profile name')
    s=once(s,
        '''                TextButton(onClick={choosingMaskot=true}) {
                    Text("Karakteri değiştir",fontSize=13.sp)
                }''',
        '''                if(session.accountName!=null) {
                    OutlinedTextField(username,
                        onValueChange={username=it.take(ArenaNickname.maximumLength);
                            nameEditing=true},
                        modifier=Modifier.fillMaxWidth(),
                        label={Text("Kullanıcı adını yaz")},
                        singleLine=true,
                        enabled=social.enabled && social.nameLoaded && !social.nameSaving)
                    TextButton(onClick={
                        nameEditing=false
                        social.saveName(username)
                    },enabled=social.enabled && social.nameLoaded &&
                        !social.nameSaving && ArenaNickname.isValid(username) &&
                        ArenaNickname.normalize(username)!=social.nickname) {
                        Text(if(social.nameSaving)"Kaydediliyor…" else "İsmi kaydet",
                            fontSize=13.sp)
                    }
                }
                TextButton(onClick={choosingMaskot=true}) {
                    Text("Karakteri değiştir",fontSize=13.sp)
                }''',
        'editable username in avatar header')
    s=once(s,
        '''            BodyText("Oturum: $account")
            BodyText("Kullanıcı adı, sohbet ve yarışma odalarında ortak kullanılır; dilediğin zaman değiştirebilirsin.")
            OutlinedTextField(username, { username = it.take(ArenaNickname.maximumLength); nameEditing = true },
                modifier = Modifier.fillMaxWidth(), label = { Text("Kullanıcı adın") }, singleLine = true,
                enabled = social.enabled && social.nameLoaded && !social.nameSaving)
            PrimaryAction(if (social.nameSaving) "Kaydediliyor…" else "Kullanıcı adını kaydet", {
                nameEditing = false
                social.saveName(username)
            }, social.enabled && social.nameLoaded && !social.nameSaving &&
                ArenaNickname.isValid(username) && ArenaNickname.normalize(username) != social.nickname)
            if (social.nickname.isBlank()) BodyText("Arkadaşlık ve yarışma için önce kullanıcı adını belirle.")
            else BodyText("Görünecek adın: ${social.nickname}", Forest)''',
        '''            Wq182Info("Kullanıcı adı ve eşitleme",
                "Kullanıcı adını maskotunun yanında belirleyip kaydedebilirsin. E-posta adresin yalnızca giriş içindir. Seçtiğin ad sohbet, arkadaşlık ve Arena odalarında kullanılır; ilerlemen eşitlenmeye devam eder.")''',
        'remove duplicate editor and long account instructions')
    s=once(s,
        '        BodyText("Seçtiğin karakter Arena, Kelime Hafızam ve profilinde kullanılır.")',
        '''        Wq182Info("Maskot nerede görünür?",
            "Seçtiğin maskot profilinde, Arena'da ve Kelime Hafızam'da kullanılır.")''',
        'short mascot explanation through in-page info button')
    s=once(s,
        '            BodyText("E-posta ve şifreyle hesap oluşturabilir veya giriş yapabilirsin. Kullanıcı adın tüm oyun ve sohbet bölümlerinde gösterilir.")',
        '''            Wq182Info("Hesap oluşturma bilgisi",
                "E-posta ve şifreyle hesap oluşturabilir veya giriş yapabilirsin. Kullanıcı adını sen seçersin; sohbet ve oyun odalarında e-postan değil bu ad görünür.")''',
        'registration instructions behind info')
    s=once(s,
        '            BodyText("Misafir kayıtların cihazında saklanır. Hesap şifreleri Firebase Authentication tarafından yönetilir; uygulama içinde saklanmaz.")',
        '''            Wq182Info("Misafir ve şifre güvenliği",
                "Misafir kayıtların cihazında tutulur. Hesap şifreleri Firebase Authentication tarafından yönetilir; uygulama içinde saklanmaz.")''',
        'guest password explanation behind info')
    s=once(s,
        '            BodyText("Hesabını silmeden önce ilerlemeni yedekleyebilirsin.")',
        '''            Wq182Info("Hesabı silmeden önce",
                "Hesap silmeden önce ilerlemeyi yedekle. Hesabı silmek geri alınamaz.")''',
        'short context help on account deletion')
    s=once(s,
        'Text(if (session.accountName == null) "Hesap" else session.profileName.take(16), fontFamily = Sans, fontWeight = FontWeight.Bold)',
        '''Text(if(session.accountName==null)"Hesap" else
                        session.social.nickname.ifBlank {"Oyuncu"}.take(16),
                        fontFamily=Sans,fontWeight=FontWeight.Bold)''',
        'dashboard greeting shows nickname instead of email')
    return s
edit('HomeScreen.kt',account)

f=ROOT/'source/app/build.gradle.kts'
s=f.read_text('utf-8')
assert s.count('versionCode = 107')==1 and s.count('versionName = "1.0.81"')==1
assert 'applicationIdSuffix = ".fresh"' in s
f.write_text(s.replace('versionCode = 107','versionCode = 108',1)
    .replace('versionName = "1.0.81"','versionName = "1.0.82"',1),'utf-8')
print('PASS: WordQuest AI 1.0.82 code108; installed package and cloud data unchanged')