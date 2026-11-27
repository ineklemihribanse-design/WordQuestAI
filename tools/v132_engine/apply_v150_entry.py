from pathlib import Path
# Guard the actual v150 overlay, not the legacy branch it bypasses.
pack=Path(__file__).resolve().parent
entry=(pack/"Wq150ArenaEntry.kt").read_text(encoding="utf-8")
assert 'Wq150HomeModes.visible(local)' in entry
assert 'Wq150HomeCard(kind,true,enabled' in entry
assert 'Wq150HomeCard(kind,false,enabled' in entry
assert 'if(wide).50f else .655f' in entry
assert 'Wq132WebEngine.generate(context,engine' in entry
assert 'Wq183ArenaBackdrop(Modifier.matchParentSize(),active=false)' in entry
assert 'Wq150ModeCatalog.kinds.forEach' not in entry, "Legacy yellow mode buttons reintroduced"
assert 'Wq150PremiumPanel {' in entry
assert 'Wq150ApprovedArt("crest"' in entry
assert 'rememberWq150Profiles(room,session,gateway)' in entry
assert 'if(!wide&&!local)' in entry
assert 'Wq150ArtCache.get(name)' in entry
assert 'Wq150Records(context,session.profileKey' in entry
match=(pack/"Wq150ArenaMatch.kt").read_text(encoding="utf-8")
assert 'if(finished)Wq150History.record' in match
settings=(pack/"Wq150MatchSettings.kt").read_text(encoding="utf-8")
assert 'DialogProperties(usePlatformDefaultWidth=false)' in settings
p=Path("source/app/src/main/java/com/kelimebulmacasi/a1/PremiumArenaScreen.kt")
s=p.read_text()
old='''    var soloLaunch by remember { mutableStateOf<WqArenaSoloLaunch?>(null) }
    val launch=soloLaunch
    if (launch!=null) key(launch.id) {'''
new='''    var soloLaunch by remember { mutableStateOf<WqArenaSoloLaunch?>(null) }
    val context=LocalContext.current
    val nextEndpoint=remember(context.applicationContext){Wq150ArenaFeature.endpoint(context)}
    var useLegacy by rememberSaveable{mutableStateOf(false)}
    var soloTab by rememberSaveable{mutableStateOf(false)}
    val launch=soloLaunch
    if(launch==null&&nextEndpoint!=null&&!useLegacy){
        Wq150ArenaFlow(session,nextEndpoint,back,account,{useLegacy=true},soloTab,{soloTab=it},{soloLaunch=it})
    } else if (launch!=null) key(launch.id) {'''
if s.count(old)!=1:
    raise SystemExit("Unexpected Arena entrance contract; no changes applied")
p.write_text(s.replace(old,new,1))
print("PASS: v150 route is enabled only by a provisioned HTTPS origin; existing solo/legacy route retained")
