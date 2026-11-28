from pathlib import Path
# Guard the actual v150 overlay, not the legacy branch it bypasses.
pack=Path(__file__).resolve().parent
entry=(pack/"Wq150ArenaEntry.kt").read_text(encoding="utf-8")
assert 'Wq150HomeModes.visible(local)' in entry
assert 'Wq150HomeCard(kind,true,enabled' in entry
assert 'Wq150HomeCard(kind,false,enabled' in entry
assert 'Wq150Podium.anchor(fullW.value,fullH.value,wide,newArt)' in entry
assert 'Box(Modifier.fillMaxSize().padding(safe))' in entry
assert 'Kişisel maskot' not in entry and 'if(picker)AlertDialog' not in entry
assert 'Wq150LevelRow(Wq150ModeCatalog.cefr,levels)' in entry
assert 'arena_v150_premium/$background.webp' in entry
assert 'Wq132WebEngine.generate(context,engine' in entry
assert 'Wq183ArenaBackdrop(Modifier.matchParentSize(),active=false)' in entry
assert 'if(lobby&&!active)Wq160LobbyBackdrop(Modifier.matchParentSize())' in entry
assert 'Wq160SettingsInsets()' in entry
assert 'Wq150ModeCatalog.kinds.forEach' not in entry, "Legacy yellow mode buttons reintroduced"
assert 'Wq150PremiumPanel {' in entry
assert 'Wq150ApprovedArt("crest"' in entry
assert 'rememberWq150Profiles(room,session,gateway)' in entry
assert 'if(!wide&&!local)' in entry
assert 'Wq150HomeLayout.measure(available.value,wide)' in entry
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
# Optional exact binary pack: supplied separately because connector writes text
# only. Never substitute a composite mock-up or a floating trophy backdrop.
import hashlib,json,zipfile
premium=Path("assets/user_uploads/arena_v150_premium.zip")
expected={
    "coop_plate.webp":"0829ddb4904e6ca1ee452b69fdefb6752c0bf2e957d8086c61fa6dfa40068436",
    "duel_plate.webp":"6a2fdc91c9158ef7f5ccdbd8217e03156c032d0ef18520606851a42acd81964b",
    "phone.webp":"943eba6fd1f34e6c6b9b179f6fc71187797bfcefcf8e0b607205b6940e9a0603",
    "quiz_plate.webp":"9ba550d88d64fc949e841e472c6ac77129416c0a09054231626fc0d818c464ff",
    "tablet.webp":"631912d7a7eec0890e9bf387f706e098dfe606dfa4b04f27513aa59dc7e6e6d5",
    "team_crest.webp":"78f842a57d8303d78debb7a8d8f491d261689e04499c41ea7c5bcae5ecea424e",
    "team_plate.webp":"988f061b801d5b5aa71dbe7032f97035acdf0b63dcc5e4d8f8955b2d740b264b",
}
if premium.is_file():
    with zipfile.ZipFile(premium) as z:
        assert len(z.namelist())==len(expected)+1 and set(z.namelist())==set(expected)|{"manifest.json"}
        assert sum(x.file_size for x in z.infolist())<12*1024*1024
        assert json.loads(z.read("manifest.json"))==expected
        for name,digest in expected.items():
            assert hashlib.sha256(z.read(name)).hexdigest()==digest,name
        target=assets/"arena_v150_premium"
        target.mkdir(parents=True,exist_ok=True)
        for name in (*expected,"manifest.json"):
            (target/name).write_bytes(z.read(name))
    print("PASS: exact premium art package staged")
else:
    print("WARNING: new premium ZIP not yet uploaded; using aligned original v120 art")
assert all((assets/"arena_v120"/name).is_file() for name in ("phone.webp","tablet.webp"))
# Keep all other portrait callers centered, and bottom-align only the Arena
# standing portrait so the fitted image never floats above the original podium.
portrait=Path("source/app/src/main/java/com/kelimebulmacasi/a1/WordQuest2DCharacters.kt")
ps=portrait.read_text()
old_signature='id:String,modifier:Modifier=Modifier,victory:Boolean=false'
new_signature=old_signature+',imageAlignment:Alignment=Alignment.Center'
old_image='contentDescription=Wq2DMascots.label(id),\n                contentScale=ContentScale.Fit,'
new_image=old_image+'\n                alignment=imageAlignment,'
assert ps.count(old_signature)==1 and ps.count(old_image)==1
ps=ps.replace(old_signature,new_signature,1).replace(old_image,new_image,1)
# Trim transparent space only for the standing arena portrait. Preserve every
# other portrait, selected custom ID, saved data and the underlying bitmap.
needle='    val scale by animateFloatAsState('
assert ps.count(needle)==1
ps=ps.replace(needle,'''    val displayState=remember(bitmap,imageAlignment){mutableStateOf(bitmap)}
    val displayBitmap by displayState
    LaunchedEffect(bitmap,imageAlignment) {
        if(bitmap!=null && imageAlignment==Alignment.BottomCenter && bitmap.hasAlpha()){
            displayState.value=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default){
                runCatching{
                    val row=IntArray(bitmap.width)
                    var left=bitmap.width;var right=-1;var top=bitmap.height;var bottom=-1
                    for(y in 0 until bitmap.height){
                        bitmap.getPixels(row,0,bitmap.width,0,y,bitmap.width,1)
                        for(x in row.indices)if((row[x] ushr 24)>24){
                            left=minOf(left,x);right=maxOf(right,x);top=minOf(top,y);bottom=maxOf(bottom,y)
                        }
                    }
                    if(right>=left&&bottom>=top)
                        android.graphics.Bitmap.createBitmap(bitmap,left,top,right-left+1,bottom-top+1)
                    else bitmap
                }.getOrDefault(bitmap)
            }
        }
    }
'''+needle,1)
assert ps.count('Image(bitmap.asImageBitmap(),\n                contentDescription=Wq2DMascots.label(id)')==1
ps=ps.replace('if(bitmap!=null) {\n            Image(bitmap.asImageBitmap(),','if(displayBitmap!=null) {\n            Image(displayBitmap!!.asImageBitmap(),',1)
portrait.write_text(ps)
assert 'Wq150ArtCache.get(name)' in entry
assert 'Wq150Records(context,session.profileKey' in entry
match=(pack/"Wq150ArenaMatch.kt").read_text(encoding="utf-8")
assert 'if(finished)Wq150History.record' in match
settings=(pack/"Wq150MatchSettings.kt").read_text(encoding="utf-8")
assert 'DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)' in settings
assert 'Wq150LevelRow(levels,s.levels)' in settings
assert 'Wq150SettingValue("İpucu hakkı"' in settings
assert 'Wq150SettingValue("Kelimeyi geçme hakkı"' in settings
assert 'window?.navigationBarColor=android.graphics.Color.TRANSPARENT' in settings
# Audio settings are shared with legacy light surfaces: honor the active theme.
audio=Path("source/app/src/main/java/com/kelimebulmacasi/a1/Wq179ArenaExtras.kt")
audio_text=audio.read_text()
a=audio_text.index("internal fun Wq179ArenaAudioSettings()")
b=audio_text.index("/** Stage backdrop",a)
segment=audio_text[a:b].replace("Color(0xFF3C3258)","MaterialTheme.colorScheme.onSurface").replace("Color(0xFF77708B)","MaterialTheme.colorScheme.onSurfaceVariant")
audio.write_text(audio_text[:a]+segment+audio_text[b:])
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

# Preload Arena's local artwork while the root navigation composition is alive.
# This runs after the v214 home transition patch; do not alter navigation or puzzle geometry.
home=Path("source/app/src/main/java/com/kelimebulmacasi/a1/HomeScreen.kt")
hs=home.read_text("utf-8")
anchor="    val appContext = LocalContext.current\n    // Warm the immutable local vocabulary while the user is on the home page."
assert hs.count(anchor)==1, "Unexpected root warmup anchor"
home.write_text(hs.replace(anchor,"    val appContext = LocalContext.current\n    Wq150WarmArenaArt(session)\n    // Warm the immutable local vocabulary while the user is on the home page.",1),"utf-8")

# v158: optional HD detail path, with independent writes so old rules/clients remain usable.
from shutil import copyfile
pkg=Path("source/app/src/main/java/com/kelimebulmacasi/a1")
copyfile(pack/"Wq158FriendHd.kt",pkg/"Wq158FriendHd.kt")
hub= pkg/"FriendsHub.kt"
hs=hub.read_text("utf-8")
anchor="    fun searchName(input: String) {"
assert hs.count(anchor)==1
hs=hs.replace(anchor,'''    // Separate node: old friendProfiles validation is untouched.
    fun publishFriendHd(preview:String?,hd:String?,done:()->Unit){
        val root=db?.reference?:return
        if(!enabled||nickname.isBlank())return
        val ref=root.child("friendMascotHd").child(uid)
        val task=if(preview!=null&&hd!=null&&hd.length<=Wq158FriendHd.MAX_TEXT&&
            hd.startsWith(preview.substringBefore(':')+":"))
            ref.setValue(mapOf("preview" to preview,"hd" to hd))
        else ref.removeValue()
        task.addOnSuccessListener{done()}
        // Older live rules can reject this optional path; never fail normal overview.
    }
    fun observeFriendHd(member:String,expected:String,callback:(String?)->Unit):()->Unit{
        val root=db?.reference
        if(root==null||!enabled||member !in friendIds){callback(null);return {}}
        val ref=root.child("friendMascotHd").child(member)
        var active=true
        val listener=object:ValueEventListener{
            override fun onDataChange(snapshot:DataSnapshot){
                if(!active)return
                val binding=snapshot.child("preview").getValue(String::class.java)
                val payload=snapshot.child("hd").getValue(String::class.java)
                callback(payload?.takeIf{enabled&&member in friendIds&&binding==expected&&
                    it.length<=Wq158FriendHd.MAX_TEXT&&it.startsWith(expected.substringBefore(':')+":")})
            }
            override fun onCancelled(error:DatabaseError){if(active)callback(null)}
        }
        ref.addValueEventListener(listener)
        return {active=false;ref.removeEventListener(listener)}
    }

'''+anchor,1)
hub.write_text(hs,"utf-8")
publisher=pkg/"Wq185FriendMascot.kt"
ps=publisher.read_text("utf-8")
start=ps.index("        while (session.accountName != null && session.social.enabled) {")
end=ps.index("            delay(120000L)",start)
ps=ps[:start]+'''        // Encode once per selected immutable custom pack / consent change.
        val detail=withContext(Dispatchers.Default){
            if(consent)Wq158FriendHd.prepare(context,session.profileKey,selected)
            else Wq158FriendHd.Pairing(null,null)
        }
        var hdPublished=false
        while (session.accountName != null && session.social.enabled) {
            session.social.publishOverview(
                selected, session.totalPoints, session.finishedCount, detail.preview)
            if(!hdPublished)session.social.publishFriendHd(detail.preview,detail.hd){hdPublished=true}
'''+ps[end:]
ps=ps.replace("Yalnızca karşılıklı arkadaşların küçük bir önizleme görür. ","Yalnızca karşılıklı arkadaşların maskot önizlemeni görür. ")
publisher.write_text(ps,"utf-8")
print("PASS: optional bounded HD friend detail; existing consent and low preview retained")

# v160: separate, device-specific route backgrounds. Use exact full-size existing
# stadium source, not a stretched/cropped image or a composite screenshot.
# Local rendering only; no external paid service or runtime download.
from PIL import Image, ImageEnhance, ImageDraw, ImageFilter, ImageOps
import math
for device,source_name,size in (
    ("tablet","arena_tablet_13_16x10.webp",(2560,1600)),
    ("phone","arena_telefon_9x19_5.webp",(1080,2340)),
):
    source=assets/"arena_v183"/source_name
    with Image.open(source) as f:
        original=f.convert("RGB")
    # Fit the entire original into the exact viewport, retaining all architecture.
    if original.size!=size:
        fitted=ImageOps.contain(original,size,Image.Resampling.LANCZOS)
        background=Image.new("RGB",size,(23,12,51))
        background.paste(fitted,((size[0]-fitted.width)//2,(size[1]-fitted.height)//2))
        original=background
    width,height=size
    for route in ("settings","lobby"):
        target=assets/"arena_v160"/route/(device+".webp")
        target.parent.mkdir(parents=True,exist_ok=True)
        if route=="settings":
            base=ImageEnhance.Color(original).enhance(.72)
            base=ImageEnhance.Brightness(base).enhance(.78)
            # Quiet centre, original gold rails still visible at edges.
            layer=Image.new("RGBA",size)
            d=ImageDraw.Draw(layer)
            d.ellipse((-width*.12,height*.07,width*1.12,height*.86),fill=(17,19,57,98))
            layer=layer.filter(ImageFilter.GaussianBlur(max(20,width//12)))
            rendered=Image.alpha_composite(base.convert("RGBA"),layer).convert("RGB")
        else:
            base=ImageEnhance.Color(original).enhance(1.15)
            base=ImageEnhance.Contrast(base).enhance(1.06)
            light=Image.new("RGBA",size)
            d=ImageDraw.Draw(light)
            # Energetic but unobtrusive peripheral gold and violet lighting.
            for side in (.11,.89):
                x=width*side
                d.polygon([(x-width*.035,height*.12),(x+width*.035,height*.12),
                    (width*.5+width*.18*(1 if side>.5 else -1),height*.65)],fill=(155,78,255,42))
                d.ellipse((x-width*.045,height*.25-width*.045,x+width*.045,height*.25+width*.045),
                    fill=(255,190,100,50))
            light=light.filter(ImageFilter.GaussianBlur(max(12,width//45)))
            rendered=Image.alpha_composite(base.convert("RGBA"),light).convert("RGB")
        rendered.save(target,"WEBP",quality=90,method=5)
        with Image.open(target) as check:
            assert check.size==size and check.format=="WEBP"
        print("PASS: distinct local route art",route,device,size,target.stat().st_size)
assert all((assets/"arena_v160"/r/(d+".webp")).is_file() for r in ("settings","lobby") for d in ("phone","tablet"))
