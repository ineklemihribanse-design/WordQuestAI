#!/usr/bin/env python3
"""Add only match-settings visuals to the published v121 source, preserving its lobby."""
from pathlib import Path
import hashlib,json,shutil,re
root=Path(__file__).resolve().parents[2]
pack=Path(__file__).resolve().parent
main=root/'source/app/src/main'; p=main/'java/com/kelimebulmacasi/a1'
def once(s,a,b):
    assert s.count(a)==1,(a[:160],s.count(a))
    return s.replace(a,b,1)
names=('ArenaApprovedHome.kt','PremiumOnlineScreen.kt','PremiumArenaScreen.kt','WordQuest174Arena.kt','Wq186ArenaPresetControls.kt')
protected={f:hashlib.sha256(f.read_bytes()).hexdigest() for f in main.rglob('*') if f.is_file() and f.name not in names}
changes={}
# Preserve the existing v121 lobby branch. Only its secondary page gets a dark theme.
s=(p/'ArenaApprovedHome.kt').read_text()
start=s.index('    } else {\n    Column(',s.index('val wq121Lobby'))
end=s.index('    if(avatarPicker)',start)
part=s[start:end]
assert 'Wq174UnifiedMatchSettings(' in part
part=part.replace('    } else {\n    Column(','    } else {\n    MaterialTheme(colorScheme=Wq122Scheme) {\n    Column(',1)
part=part.replace('.background(ArenaApproved.background)','.background(Color.Transparent)',1)
# Keep the current back action, but no redundant old light top header.
part=once(part,'        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {','        if(!matchSettingsPage) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {')
part=part.replace('color=ArenaApproved.ink','color=Wq122Ink').replace('color=ArenaApproved.sub','color=Color(0xFFE2CFFF)').replace('color=ArenaApproved.purple','color=Wq122Gold').replace('Color(0xFF6534A9)','Wq122Gold')
part=part.replace('ArenaApprovedPrimary(','Wq122Primary(')
# Existing second-page closing braces + new MaterialTheme closure.
part += '    }\n'
s=s[:start]+part+s[end:]
changes['ArenaApprovedHome.kt']=s
s=(p/'PremiumOnlineScreen.kt').read_text()
s=once(s,'if(!wq121FullLobby) Box(Modifier.matchParentSize().background(Color.White))','if(!wq121FullLobby) Wq122SettingsBackdrop(Modifier.matchParentSize())')
s=once(s,'else Modifier.widthIn(max = 960.dp).fillMaxWidth()','else if(match==null) Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding().verticalScroll(rememberScrollState()).padding(horizontal=if(androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp>=650) 26.dp else 8.dp,vertical=8.dp) else Modifier.widthIn(max = 960.dp).fillMaxWidth()')
s=once(s,'if(!wq121FullLobby) Row(Modifier.fillMaxWidth()','if(match!=null) Row(Modifier.fillMaxWidth()')
# Existing live match rendering and history stay identical below the lobby function.
changes['PremiumOnlineScreen.kt']=s
s=(p/'PremiumArenaScreen.kt').read_text()
s=once(s,'else Modifier.fillMaxSize().background(ArenaNight)','else if(stage==PremiumStage.Lobby) Modifier.fillMaxSize().then(wq122SettingsModifier()).windowInsetsPadding(WindowInsets.safeDrawing).imePadding().verticalScroll(rememberScrollState()).padding(horizontal=12.dp,vertical=8.dp) else Modifier.fillMaxSize().background(ArenaNight)')
s=once(s,'if(!wq121FullLobby) Row(Modifier.fillMaxWidth()','if(stage!=PremiumStage.Lobby) Row(Modifier.fillMaxWidth()')
# Existing local hint action uses a separate budget, reset at the actual start.
s=once(s,'    fun begin() {','''    var wq122HintBudget by remember { mutableIntStateOf(0) }
    var wq122HintsUsed by remember { mutableIntStateOf(0) }
    fun begin() {
        wq122HintBudget=Wq186ArenaPresetStore.read(context,session.profileKey,level).hints
        wq122HintsUsed=0''')
needle='hintLetter = "${index + 1}. harf ${word.entry.letters[index]}"'
if s.count(needle)==1:
    s=once(s,needle,'''if(wq122HintsUsed>=wq122HintBudget) {
                                    feedback="İpucu hakkın kalmadı."
                                } else {
                                    wq122HintsUsed++
                                    '''+needle)
    # score stays in the authorized reveal branch
    anchor='score = (score - 2).coerceAtLeast(0)'
    j=s.index(anchor,s.index('wq122HintsUsed++'))
    s=s[:j]+s[j:].replace(anchor,anchor+'\n                                }',1)
else:
    raise AssertionError('local hint action not found; do not publish a nonfunctional setting')
changes['PremiumArenaScreen.kt']=s
s=(p/'WordQuest174Arena.kt').read_text()
# The older hero/mode functions are outside the modified suffix.
a=s.index('@Composable\nprivate fun Wq174SettingIcon(')
b=s.index('@Composable\nprivate fun Wq174SettingRow(',a)
s=s[:a]+'''@Composable
private fun Wq174SettingIcon(kind:String) {
    Wq122Art(when(kind){"level"->"level";"topic"->"topic";"time"->"time";"count"->"count";else->"hint"},Modifier.size(43.dp))
}

'''+s[b:]
a=s.index('@Composable\nprivate fun Wq174SettingRow(')
head=s[:a];tail=s[a:]
tail=tail.replace('Ink174','Wq122Ink').replace('Royal174','Wq122Gold').replace('Border174','Wq122Border')
colors={'0xFFFFF9FC':'0xD9371B62','0xFFF8F3FF':'0xFF28164C','0xFFD6BBF3':'0xFFBA8EE5','0xFFFFF8FE':'0xE91B113C','0xFFD9B9FD':'0xFFBA8EE5','0xFFF3EAFE':'0xFF22143F','0xFFFFF5DB':'0xFF301C57','0xFF6B438D':'0xFFE2CFFF','0xFFEBD9FF':'0xFF342052','0xFFF7EFFF':'0xFF24133D','0xFF493568':'0xFFE2CFFF'}
for x,y in colors.items():tail=tail.replace(x,y)
tail=tail.replace('Color(0xFF4F149F),Color(0xFF872CDD)','Color(0xFF32115F),Color(0xFF8C29CE)')
# Native setting cards and selectors keep real callbacks and dropdown choices.
tail=tail.replace('Wq174SettingIcon(kind)\n        Text(label','Wq174SettingIcon(kind)\n        Text(label',1)
tail=tail.replace('Text(value,maxLines=1','Text(value,maxLines=1',1)
# Replace exact current play-type segmented row with reusable native 3D chips.
a=tail.index('            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp))',tail.index('internal fun Wq174UnifiedMatchSettings('))
b=tail.index('            Wq186ArenaPresetControls(',a)
old=tail[a:b]
assert 'onSolo(isSolo)' in old
tail=tail[:a]+'''            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Wq122Choice("Arkadaşınla Oyna",!solo,Modifier.weight(1f),"friends"){onSolo(false)}
                Wq122Choice("Tek Başına Oyna",solo,Modifier.weight(1f),"solo"){onSolo(true)}
            }
'''+tail[b:]
# Heading artwork is additive without changing info handler.
tail=tail.replace('Text("Maç Ayarları",color=Color.White,','Wq122Art("settings",Modifier.size(40.dp))\n            Spacer(Modifier.width(5.dp))\n            Text("Maç Ayarları",color=Wq122Gold,',1)
tail=tail.replace('Text("Oyun Biçimi",color=Wq122Ink,fontWeight=FontWeight.ExtraBold,\n                fontSize=13.sp)','Wq122Section("mode","Oyun Biçimi")',1)
# Replace the old rules control's glyph with the generated icon, preserving expanded real rules.
tail=tail.replace('Text(if(detailsOpen) "ⓘ Maç kurallarını gizle"','Wq122Art("rules",Modifier.size(28.dp))\n                Spacer(Modifier.width(5.dp))\n                Text(if(detailsOpen) "Maç kurallarını gizle"').replace('else "ⓘ Maç kuralları"','else "Maç kuralları"')
changes['WordQuest174Arena.kt']=head+tail
s=(p/'Wq186ArenaPresetControls.kt').read_text()
a=s.index('@Composable\ninternal fun Wq186ArenaPresetControls(')
assert 'fun sanitized' in s[:a] and 'hints.coerceIn(0,20)' in s[:a]
prefix=s[:a].replace('import androidx.compose.foundation.layout.*','import androidx.compose.foundation.layout.*\nimport androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.unit.sp')
changes['Wq186ArenaPresetControls.kt']=prefix+(pack/'PresetBody.kt.txt').read_text()
manifest=json.loads((pack/'assets/manifest.json').read_text());assert len(manifest)==20
for name,digest in manifest.items():
    f=pack/'assets'/name;assert f.suffix=='.webp' and hashlib.sha256(f.read_bytes()).hexdigest()==digest,name
out=main/'assets/arena_v122';out.mkdir(parents=True,exist_ok=True)
for name in manifest:shutil.copyfile(pack/'assets'/name,out/name)
for name,s in changes.items():(p/name).write_text(s,'utf-8')
shutil.copyfile(pack/'Wq122Settings.kt',p/'Wq122Settings.kt')
g=root/'source/app/build.gradle.kts'
s=once(g.read_text(),'versionCode = 147','versionCode = 148')
g.write_text(once(s,'versionName = "1.0.121"','versionName = "1.0.122"'))
assert all(hashlib.sha256(f.read_bytes()).hexdigest()==digest for f,digest in protected.items())
print("PASS: 20 verified assets; existing v121 lobby, game board, Firebase rules and Lena untouched")
