from pathlib import Path
p=Path("source/app/src/main/java/com/kelimebulmacasi/a1/PremiumArenaScreen.kt")
s=p.read_text()
old='''    var soloLaunch by remember { mutableStateOf<WqArenaSoloLaunch?>(null) }
    val launch=soloLaunch
    if (launch!=null) key(launch.id) {'''
new='''    var soloLaunch by remember { mutableStateOf<WqArenaSoloLaunch?>(null) }
    val context=LocalContext.current
    val nextEndpoint=remember(context.applicationContext){Wq150ArenaFeature.endpoint(context)}
    var useLegacy by rememberSaveable{mutableStateOf(false)}
    val launch=soloLaunch
    if(launch==null&&nextEndpoint!=null&&!useLegacy){
        Wq150ArenaFlow(session,nextEndpoint,back,account,{useLegacy=true})
    } else if (launch!=null) key(launch.id) {'''
if s.count(old)!=1:
    raise SystemExit("Unexpected Arena entrance contract; no changes applied")
p.write_text(s.replace(old,new,1))
print("PASS: v150 route is enabled only by a provisioned HTTPS origin; existing solo/legacy route retained")
