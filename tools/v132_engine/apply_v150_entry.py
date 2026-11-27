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
assert 'Wq150HomeLayout.measure(maxHeight.value,wide)' in entry
assert 'imageAlignment=Alignment.BottomCenter' in entry
assets=Path("source/app/src/main/assets")
required=[
    *["arena_v120/"+name+".webp" for name in ("back","book","crest","duel","team","quiz","friends","history","info","modes","ranking","settings")],
    *["arena_v120/"+device+"_card_"+kind+".webp" for device in ("phone","tablet") for kind in ("duel","team","quiz")],
    "arena_v122/solo.webp","arena_v122/rules.webp",
    "wq175_icons/arena_team.webp","wq175_icons/puzzle.webp","wq175_icons/timer.webp",
    "arena_v183/arena_tablet_13_16x10.webp","arena_v183/arena_telefon_9x19_5.webp",
]
missing=[name for name in required if not (assets/name).is_file()]
assert not missing, "Missing approved Arena art: "+str(missing)
# Keep all other portrait callers centered, and bottom-align only the Arena
# standing portrait so the fitted image never floats above the original podium.
portrait=Path("source/app/src/main/java/com/kelimebulmacasi/a1/WordQuest2DCharacters.kt")
ps=portrait.read_text()
old_signature='id:String,modifier:Modifier=Modifier,victory:Boolean=false'
new_signature=old_signature+',imageAlignment:Alignment=Alignment.Center'
old_image='contentDescription=Wq2DMascots.label(id),\n                contentScale=ContentScale.Fit,'
new_image=old_image+'\n                alignment=imageAlignment,'
assert ps.count(old_signature)==1 and ps.count(old_image)==1
portrait.write_text(ps.replace(old_signature,new_signature,1).replace(old_image,new_image,1))
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
