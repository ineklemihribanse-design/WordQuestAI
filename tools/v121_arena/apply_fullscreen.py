#!/usr/bin/env python3
"""Apply after the exact released v120 patch. No graphics generation or board changes."""
from pathlib import Path
import hashlib,shutil,re
ROOT=Path(__file__).resolve().parents[2]
MAIN=ROOT/'source/app/src/main'
P=MAIN/'java/com/kelimebulmacasi/a1'
PKG=Path(__file__).resolve().parent
G=ROOT/'source/app/build.gradle.kts'
def once(s,a,b):
    assert s.count(a)==1,(a[:120],s.count(a))
    return s.replace(a,b,1)
files={name:P/name for name in ('ArenaApprovedHome.kt','PremiumOnlineScreen.kt','PremiumArenaScreen.kt')}
protected={f:hashlib.sha256(f.read_bytes()).hexdigest() for f in MAIN.rglob('*') if f.is_file() and f not in files.values()}
home=files['ArenaApprovedHome.kt'].read_text()
# Add optional callbacks to the existing native landing composable; no stored state reset.
a=home.index('internal fun ArenaApprovedHome(')
b=home.index(') {',a)
signature=home[a:b]
assert 'onFriends:' in signature and 'soloInitially:' in signature
home=home[:b].rstrip().rstrip(',')+',\n    wq121FullScreen: Boolean = false,\n    wq121Back: () -> Unit = {},\n    wq121OnLayout: (Boolean) -> Unit = {}\n'+home[b:]
assert 'var matchSettingsPage by rememberSaveable' in home and 'var avatarPicker' in home
anchor='    Column('
start=home.index(anchor,home.index('var matchSettingsPage'))
# The actual old page remains unchanged for match settings.
assert 'Wq120ArenaLobby(' in home[start:]
home=home[:start]+'''    val wq121Lobby = wq121FullScreen && !matchSettingsPage
    SideEffect { wq121OnLayout(wq121Lobby) }
    var wq121FriendsOpen by remember { mutableStateOf(false) }
    if(wq121Lobby) {
        Wq121ArenaLobby(mascot,mode,solo,
            onMode={nextMode->onMode(nextMode);matchSettingsPage=true},
            onBack=wq121Back,onMascot={avatarPicker=true},
            onFriends={if(soloInitially)wq121Back() else wq121FriendsOpen=true},
            profile=avatarProfileKey)
        if(wq121FriendsOpen) AlertDialog(
            onDismissRequest={wq121FriendsOpen=false},
            title={Text("Arkadaşlarım")},
            text={Column(Modifier.heightIn(max=460.dp).verticalScroll(rememberScrollState())) {onFriends()}},
            confirmButton={TextButton(onClick={wq121FriendsOpen=false}){Text("Kapat")}})
    } else {
'''+home[start:]
# Close the conditional before the real mascot picker, which must also work in the full-screen branch.
picker=re.search(r'(?m)^    if\s*\(\s*avatarPicker\s*\)',home)
assert picker
home=home[:picker.start()]+'    }\n'+home[picker.start():]
assert 'avatarProfileKey' in signature
online=files['PremiumOnlineScreen.kt'].read_text()
# Actual outer container: remove max width, scroll, padding and duplicate header ONLY on the mode page.
root='    Box(Modifier.fillMaxSize().background(OnlineNight)) {'
assert online.count(root)==1
online=once(online,root,'''    var wq121LobbyVisible by rememberSaveable { mutableStateOf(true) }
    val wq121FullLobby = match == null && wq121LobbyVisible
    Box(Modifier.fillMaxSize().background(if(wq121FullLobby) Color(0xFF150B3E) else OnlineNight)) {''')
j=online.index('    Column(Modifier.widthIn',online.index('val wq121FullLobby'))
k=online.index('verticalArrangement',j)
old=online[j:k]
assert 'widthIn' in old and 'verticalScroll' in old and 'padding' in old
online=online[:j]+old.replace('Column(Modifier','Column(if(wq121FullLobby) Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing) else Modifier',1)+online[k:]
j=online.index('        Row(Modifier.fillMaxWidth()',j)
online=online[:j]+online[j:].replace('        Row(Modifier.fillMaxWidth()', '        if(!wq121FullLobby) Row(Modifier.fillMaxWidth()',1)
online=once(online,'            ArenaApprovedHome(','''            ArenaApprovedHome(
                wq121FullScreen=true,wq121Back=back,
                wq121OnLayout={wq121LobbyVisible=it},''')
# Existing light stage background otherwise paints white over the new root; hide it only in full-screen lobby.
# v183 replaced Wq179ArenaStageBackground with Wq183ArenaBackdrop.
# Only inspect the root between the outer Box and its first Column, never the
# board/playing composables below it. Preserve the exact current call.
outer_start=online.index('val wq121FullLobby')
outer_end=online.index('    Column(if(wq121FullLobby)',outer_start)
outer=online[outer_start:outer_end]
background_calls=list(re.finditer(r'(?m)^(\s*)(Wq183ArenaBackdrop|Wq181ArenaHeroStage|Wq179ArenaStageBackground)\(',outer))
assert len(background_calls)==1,('unexpected outer Arena backdrop',outer)
call=background_calls[0]
j=outer_start+call.start(2)
online=online[:j]+'if(!wq121FullLobby) '+online[j:]
print('PASS: current root backdrop gated:',call.group(2))
# Read actual scoring from the existing match result calculation. New isolated
# history is populated only after a real online match finishes.
m=re.search(r'(?m)^    val guestName\s*=.*$',online)
assert m and 'fun arenaScore(player: String)' in online[:m.start()]
online=online[:m.end()]+'''
    val wq121HostPoints = if(match.phase=="finished") arenaScore(match.hostUid) else 0
    val wq121GuestPoints = if(match.phase=="finished") arenaScore(match.guestUid) else 0
    LaunchedEffect(match.code,match.phase,wq121HostPoints,wq121GuestPoints) {
        if(match.phase=="finished") Wq121ArenaHistory.record(context,session.profileKey,match.code,match.mode,
            match.hostUid,match.guestUid,hostName,guestName,wq121HostPoints,wq121GuestPoints)
    }
'''+online[m.end():]
offline=files['PremiumArenaScreen.kt'].read_text()
# Same exclusive full-screen policy for local Arena entrance; no change when playing.
j=offline.index('    Column(\n        Modifier.fillMaxSize().background(ArenaNight)')
k=offline.index('verticalArrangement',j)
old=offline[j:k]
assert 'verticalScroll' in old
offline=offline[:j]+'''    var wq121LobbyVisible by rememberSaveable { mutableStateOf(true) }
    val wq121FullLobby=stage==PremiumStage.Lobby && wq121LobbyVisible
'''+old.replace('Modifier.fillMaxSize().background(ArenaNight)','if(wq121FullLobby) Modifier.fillMaxSize().background(Color(0xFF150B3E)).windowInsetsPadding(WindowInsets.safeDrawing) else Modifier.fillMaxSize().background(ArenaNight)',1)+offline[k:]
j=offline.index('        Row(Modifier.fillMaxWidth()',j)
offline=offline[:j]+offline[j:].replace('        Row(Modifier.fillMaxWidth()', '        if(!wq121FullLobby) Row(Modifier.fillMaxWidth()',1)
offline=once(offline,'                ArenaApprovedHome(','''                ArenaApprovedHome(
                    wq121FullScreen=true,wq121Back=back,
                    wq121OnLayout={wq121LobbyVisible=it},''')
# Check original art byte-for-byte and leave every non-Arena source intact.
for name,data in [('ArenaApprovedHome.kt',home),('PremiumOnlineScreen.kt',online),('PremiumArenaScreen.kt',offline)]:
    files[name].write_text(data,'utf-8')
for name in ('Wq121ArenaLobby.kt','Wq121ArenaHistory.kt'):
    shutil.copyfile(PKG/name,P/name)
g=G.read_text();g=once(g,'versionCode = 146','versionCode = 147');g=once(g,'versionName = "1.0.120"','versionName = "1.0.121"');G.write_text(g,'utf-8')
assert all(hashlib.sha256(f.read_bytes()).hexdigest()==h for f,h in protected.items())
assert len(list((MAIN/'assets/arena_v120').glob('*.webp')))==21
T=ROOT/'source/app/src/test/java/com/kelimebulmacasi/a1'
shutil.copyfile(PKG/'Wq121ArenaLayoutTest.kt',T/'Wq121ArenaLayoutTest.kt')
print("PASS: native full screen parent/child; all 21 approved images and non-Arena source bytes unchanged")
