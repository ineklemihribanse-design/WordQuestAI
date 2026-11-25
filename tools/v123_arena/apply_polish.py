#!/usr/bin/env python3
"""Polish only the released 1.0.122 Arena entry/settings, no new art."""
from pathlib import Path
import hashlib,re
root=Path(__file__).resolve().parents[2]
main=root/'source/app/src/main'
p=main/'java/com/kelimebulmacasi/a1'
names=('ArenaApprovedHome.kt','PremiumOnlineScreen.kt','PremiumArenaScreen.kt','WordQuest174Arena.kt','Wq122Settings.kt','Wq186ArenaPresetControls.kt')
protected={f:hashlib.sha256(f.read_bytes()).hexdigest() for f in main.rglob('*') if f.is_file() and f.name not in names}
def once(s,a,b):
    assert s.count(a)==1,(a[:140],s.count(a))
    return s.replace(a,b,1)
out={}
# Both pages must receive the identical bounded full-screen constraints.
# Previously a child SideEffect told a parent to switch scroll/padding one
# composition later: on back the tablet briefly drew the portrait lobby.
s=(p/'PremiumOnlineScreen.kt').read_text()
s=once(s,'val wq121FullLobby = match == null && wq121LobbyVisible','val wq121FullLobby = match == null')
a=s.index('else if(match==null) Modifier.fillMaxSize()',s.index('val wq121FullLobby'))
b=s.index('else Modifier.widthIn(max = 960.dp)',a)
s=s[:a]+s[b:]
s=once(s,'if(!wq121FullLobby) Wq122SettingsBackdrop(Modifier.matchParentSize())','// Entry and settings now draw their own art within the stable full-screen parent.')
s=once(s,'wq121OnLayout={wq121LobbyVisible=it},','wq121OnLayout={},')
out['PremiumOnlineScreen.kt']=s
s=(p/'PremiumArenaScreen.kt').read_text()
s=once(s,'val wq121FullLobby=stage==PremiumStage.Lobby && wq121LobbyVisible','val wq121FullLobby=stage==PremiumStage.Lobby')
a=s.index('else if(stage==PremiumStage.Lobby) Modifier.fillMaxSize().then(wq122SettingsModifier())')
b=s.index('else Modifier.fillMaxSize().background(ArenaNight)',a)
s=s[:a]+s[b:]
s=once(s,'wq121OnLayout={wq121LobbyVisible=it},','wq121OnLayout={},')
out['PremiumArenaScreen.kt']=s
s=(p/'ArenaApprovedHome.kt').read_text()
s=once(s,'    SideEffect { wq121OnLayout(wq121Lobby) }\n','')
# The page-specific settings scroller lives INSIDE the child now. It cannot
# impose infinite height or a stale portrait measurement on the new lobby.
a=s.index('    MaterialTheme(colorScheme=Wq122Scheme) {',s.index('val wq121Lobby'))
b=s.index('    if(avatarPicker)',a)
part=s[a:b]
c=part.index('    Column(')
d=part.index('verticalArrangement=',c)
assert 'Modifier.fillMaxWidth()' in part[c:d]
part=part[:c]+'''    Box(Modifier.fillMaxSize().background(Color(0xFF150B3E))) {
        Wq122SettingsBackdrop(Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Color(0x55100832)))
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal=if(androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp>=650) 26.dp else 10.dp,vertical=10.dp),
        '''+part[d:]
part += '    }\n'
# The join action remains available, but not a blank low-contrast outline.
part=part.replace('color=ArenaApproved.plum','color=Wq122Gold')
part=part.replace('color=ArenaApproved.green','color=Wq122Ink')
part=part.replace('color=Color(0xFF174D43)','color=Wq122Ink')
part=part.replace('color=Color(0xFF145047)','color=Wq122Ink')
join='OutlinedButton(onClick={joinOpen=!joinOpen},'
assert part.count(join)==1, 'join control must remain reachable'
j=part.index(join)
e=part.index('Text("Oda Koduyla Katıl"',j)
# Keep the exact existing callback; remove the stretched border.
part=part[:j]+'''TextButton(onClick={joinOpen=!joinOpen},
                modifier=Modifier.fillMaxWidth().heightIn(min=44.dp)
                    .clip(RoundedCornerShape(15.dp)).background(Color(0xD9231348))) {
                Wq122Art("friends",Modifier.size(30.dp))
                Spacer(Modifier.width(8.dp))
                '''+part[e:]
part=part.replace('Text("Oda Koduyla Katıl",color=Wq122Gold','Text(if(joinOpen)"Oda kodunu gizle" else "Oda Koduyla Katıl",color=Wq122Gold')
part=part.replace('Text("Oda Koduyla Katıl",color=ArenaApproved.plum','Text(if(joinOpen)"Oda kodunu gizle" else "Oda Koduyla Katıl",color=Wq122Gold')
# Explicitly protect selected-mode text from bright stadium lights.
part=part.replace('Text("Seçilen mod: " + Wq190ArenaSoloPolicy.displayName(mode,solo),','Text("Seçilen mod: " + Wq190ArenaSoloPolicy.displayName(mode,solo),\n                modifier=Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xE51D103C)).padding(horizontal=12.dp,vertical=6.dp),')
s=s[:a]+part+s[b:]
out['ArenaApprovedHome.kt']=s
s=(p/'WordQuest174Arena.kt').read_text()
# Remove all preexisting decorative gear calls between the header's Row
# opening and the approved new artwork, rather than guessing an old icon ID.
a=s.index('internal fun Wq174UnifiedMatchSettings(')
b=s.index('Wq122Art("settings",Modifier.size(40.dp))',a)
r=s.rfind('verticalAlignment=Alignment.CenterVertically) {',a,b)
assert r!=-1
r+=len('verticalAlignment=Alignment.CenterVertically) {')
old=s[r:b]
assert len(old)<750 and ('Icon' in old or 'Text(' in old),old
s=s[:r]+'\n            '+s[b:]
s=once(s,'Wq122Art("settings",Modifier.size(40.dp))','Wq122Art("settings",Modifier.size(48.dp))')
s=once(s,'else->"hint"},Modifier.size(43.dp))','else->"hint"},Modifier.size(51.dp))')
# Scope the old shared info component replacement ONLY to this settings page.
s=once(s,'Wq187InfoTip(\n                "Maç kuralları",','Wq122Section("rules",\n                "Maç kuralları",')
# Keep native info action; replace the header's text glyph with actual artwork.
s=s.replace('Text("ⓘ",color=Color.White,fontSize=23.sp,\n                    fontWeight=FontWeight.Bold)','Wq122Art("info",Modifier.size(32.dp))')
out['WordQuest174Arena.kt']=s
s=(p/'Wq122Settings.kt').read_text()
# Enlarged artwork without scaling the underlying image files.
s=once(s,'if(icon!=null)Wq122Art(icon,Modifier.size(20.dp))','if(icon!=null)Wq122Art(icon,Modifier.size(25.dp))')
s=once(s,'Wq122Art(icon,Modifier.size(27.dp))','Wq122Art(icon,Modifier.size(33.dp))')
s=once(s,'Wq122Art("info",Modifier.size(24.dp))','Wq122Art("info",Modifier.size(29.dp))')
s=once(s,'Wq122Art("start",Modifier.size(38.dp))','Wq122Art("start",Modifier.size(46.dp))')
# Stretched image plate caused the long horizontal stripe; use the existing
# clean gold gradient and independent icon, not a flattened/stretched bitmap.
s=once(s,'        Wq122Art("start_plate",Modifier.matchParentSize(),ContentScale.FillBounds,.14f)\n','')
s=once(s,'modifier.fillMaxWidth().heightIn(min=52.dp).clip(shape)','modifier.fillMaxWidth().heightIn(min=58.dp).clip(shape)')
s=s.replace('Color(0xFFFFE9A2),Color(0xFFFFB82C),Color(0xFFE39A18)','Color(0xFFFFE7A1),Color(0xFFFFC34B),Color(0xFFE4A02A)')
out['Wq122Settings.kt']=s
s=(p/'Wq186ArenaPresetControls.kt').read_text()
s=once(s,'Wq122Art("info",Modifier.size(21.dp))','Wq122Art("info",Modifier.size(26.dp))')
out['Wq186ArenaPresetControls.kt']=s
for name,s in out.items():
    (p/name).write_text(s,'utf-8')
g=root/'source/app/build.gradle.kts'
s=once(g.read_text(),'versionCode = 148','versionCode = 149')
g.write_text(once(s,'versionName = "1.0.122"','versionName = "1.0.123"'))
assert all(hashlib.sha256(f.read_bytes()).hexdigest()==digest for f,digest in protected.items())
assert len(list((main/'assets/arena_v120').glob('*.webp')))==21
assert len(list((main/'assets/arena_v122').glob('*.webp')))==20
print('PASS: stable full-screen entrance/settings, single gear, larger art, readable join/rules; 41 original assets and other sources unchanged')
