#!/usr/bin/env python3
"""WordQuest AI 1.0.85: cumulative overlay AFTER verified 1.0.84.

Preserves user data, Firebase state, all Arena modes, locally stored ZIPs,
and the exact phone/tablet art. Fails closed if any prior source changes.
"""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
TOOLS = ROOT / "tools/v185"
pending = {}

def edit(name, changes):
    file = SRC / name
    original = pending.get(file, file.read_text(encoding="utf-8"))
    for label, old, new in changes:
        found = original.count(old)
        if found != 1:
            raise AssertionError(f"{name} {label}: expected exactly one anchor, found {found}")
        original = original.replace(old, new, 1)
    pending[file] = original
    print("PASS v185", name)

def edit_region(name, start_text, end_text, replacement, label):
    file = SRC / name
    content = pending.get(file, file.read_text("utf-8"))
    assert content.count(start_text) == 1, (label,"start anchor",content.count(start_text))
    start = content.index(start_text)
    finish = content.find(end_text, start + len(start_text))
    assert finish > start, (label,"end anchor")
    pending[file] = content[:start] + replacement + content[finish:]
    print("PASS v185",label)

def apply():
    assert SRC.is_dir(), "Must run after source and v1.0.84 overlay"
    assert (SRC / "Wq184BitmapTrim.kt").is_file(), "v184 has not been applied"
    assert (TOOLS / "Wq185FriendMascot.kt").is_file()

    edit("HomeScreen.kt", [
        ("edit icon import",
         "import androidx.compose.material.icons.filled.Settings",
         "import androidx.compose.material.icons.filled.Settings\nimport androidx.compose.material.icons.filled.Edit"),
        ("larger own maskot card",
         '''        Wq2DCompactSelector(chosenMaskot.value,
            onClick={choosingMaskot=true})''',
         '''        Wq185ProfileSelector(chosenMaskot.value,
            onClick={choosingMaskot=true})'''),
        ("profile name shows only a pencil control once saved",
         '''                Text(social.nickname.ifBlank { "Kullanıcı adını belirle" },
                    fontFamily=Serif,color=Ink,fontSize=22.sp,
                    fontWeight=FontWeight.ExtraBold,maxLines=2)''',
         '''                Row(Modifier.fillMaxWidth(),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    Text(social.nickname.ifBlank { "Kullanıcı adını belirle" },
                        modifier=Modifier.weight(1f),
                        fontFamily=Serif,color=Ink,fontSize=22.sp,
                        fontWeight=FontWeight.ExtraBold,maxLines=2)
                    if(session.accountName!=null && !nameEditing &&
                        social.nickname.isNotBlank()) {
                        IconButton(onClick={
                            username=social.nickname
                            nameEditing=true
                        },enabled=social.enabled && social.nameLoaded && !social.nameSaving) {
                            Icon(Icons.Default.Edit,
                                contentDescription="Kullanıcı adını düzenle")
                        }
                    }
                }'''),
        ("name editor closes only after persisted nickname matches",
         '''    LaunchedEffect(social.nickname) {
        if (!nameEditing) username = social.nickname
    }''',
         '''    LaunchedEffect(social.nickname, social.nameSaving) {
        if (nameEditing && social.nickname.isNotBlank() &&
            !social.nameSaving &&
            ArenaNickname.normalize(username) == social.nickname) {
            nameEditing = false
        }
        if (!nameEditing) username = social.nickname
    }'''),
        ("friend overview publisher reacts to personal maskot and opt-in",
         '''    // Only authenticated accounts publish minimal shared statistics.
    // Timestamp is server time; never infer that an absent heartbeat means "online".
    LaunchedEffect(session.accountName,session.profileKey) {
        while(session.accountName!=null && session.social.enabled) {
            session.social.publishOverview(
                Wq2DMascots.selected(appContext,session.profileKey),
                session.totalPoints,session.finishedCount)
            kotlinx.coroutines.delay(120000L)
        }
    }''',
         '''    // Friend-only previews use an independent consent; original ZIP stays local.
    Wq185FriendProfilePublisher(session)'''),
    ])

    edit_region("HomeScreen.kt",
        '''                if(session.accountName!=null) {
                    OutlinedTextField(username,''',
        '''                TextButton(onClick={choosingMaskot=true}) {''',
        '''                if(session.accountName!=null &&
                    (nameEditing || social.nickname.isBlank())) {
                    OutlinedTextField(username,
                        onValueChange={username=it.take(ArenaNickname.maximumLength);
                            nameEditing=true},
                        modifier=Modifier.fillMaxWidth(),
                        label={Text("Kullanıcı adı")},
                        singleLine=true,
                        enabled=social.enabled && social.nameLoaded && !social.nameSaving)
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Button(onClick={social.saveName(username)},
                            enabled=social.enabled && social.nameLoaded &&
                                !social.nameSaving && ArenaNickname.isValid(username) &&
                                ArenaNickname.normalize(username)!=social.nickname) {
                            Text(if(social.nameSaving)"Kaydediliyor…" else "İsmi kaydet",
                                fontSize=13.sp)
                        }
                        if(social.nickname.isNotBlank()) TextButton(onClick={
                            nameEditing=false
                            username=social.nickname
                        }) { Text("Vazgeç") }
                    }
                }
''',
        "inline profile username editor shown only on tap")

    # The existing profile owner is still rendered by HomeScreen.
    # Add an explicit friend-only setting after the custom ZIP importer.
    file = SRC / "HomeScreen.kt"
    content = pending[file]
    marker = "                Wq183MascotManager("
    assert content.count(marker) == 1, "Missing unique profile mascot manager"
    start = content.index(marker)
    end = content.index("\n                )",start) + len("\n                )")
    content = (content[:end] +
        '''\n                Wq185FriendSharingSetting(session.profileKey,
                    online=session.accountName!=null)''' +
        content[end:])
    pending[file] = content
    print("PASS v185 profile permission switch")

    edit("FriendsHub.kt", [
        ("friend entry may contain approved small personal preview",
         '''    val completedGames:Int?=null,val lastActiveAt:Long?=null)''',
         '''    val completedGames:Int?=null,val lastActiveAt:Long?=null,
    val avatarPreview:String?=null)'''),
        ("friend overview model preview",
         '''    val avatarId:String,val points:Int,val completedGames:Int,val lastActiveAt:Long?
)''',
         '''    val avatarId:String,val points:Int,val completedGames:Int,val lastActiveAt:Long?,
    val avatarPreview:String?=null
)'''),
        ("read optional friend-only preview",
         '''                    val active=snapshot.child("lastActiveAt").getValue(Long::class.java)''',
         '''                    val active=snapshot.child("lastActiveAt").getValue(Long::class.java)
                    val preview=snapshot.child("avatarPreview")
                        .getValue(String::class.java)?.takeIf {
                            Wq185FriendMascot.decode(it)!=null
                        }'''),
        ("store optional remote image without sharing originals",
         '''                            completed.coerceIn(0L,1000000L).toInt(),active)''',
         '''                            completed.coerceIn(0L,1000000L).toInt(),active,preview)'''),
        ("forward preview into rendered friend cards",
         '''                profile?.lastActiveAt)''',
         '''                profile?.lastActiveAt,profile?.avatarPreview)'''),
    ])

    edit_region("FriendsHub.kt",
        '''    fun publishOverview(avatar:String,points:Int,completed:Int){''',
        '''    fun searchName(input: String) {''',
        '''    fun publishOverview(
        avatar:String,points:Int,completed:Int,avatarPreview:String?=null
    ){
        val root=db?.reference?:return
        if(!enabled || nickname.isBlank())return
        // An owner's private custom ID must never be mistaken for a builtin
        // ID. The optional thumbnail is delivered only if explicitly allowed.
        val builtin=if(Wq183MascotStore.isCustomId(avatar)) "piko" else avatar
        if(!Wq2DMascots.valid(builtin))return
        val fields=mutableMapOf<String,Any>(
            "avatar" to builtin,
            "points" to points.coerceIn(0,1000000000),
            "completed" to completed.coerceIn(0,1000000),
            "lastActiveAt" to ServerValue.TIMESTAMP
        )
        if(Wq185FriendMascot.validPreview(avatar,avatarPreview))
            fields["avatarPreview"]=avatarPreview!!
        // Replacing the owner-controlled summary retracts the thumbnail
        // immediately when consent is disabled or avatar changes.
        root.child("friendProfiles").child(uid).setValue(fields)
            .addOnFailureListener {
                if(enabled) status="Arkadaş profili paylaşımı için Firebase Rules güncellenmeli."
            }
    }

''',
        "friend profile publisher retains safe builtin fallback")

    edit_region("Wq180FriendCards.kt",
        '''@Composable
private fun Wq180FriendFace(''',
        '''@Composable
private fun Wq180SmallAction(''',
        '''@Composable
private fun Wq180FriendFace(
    avatar:String?, preview:String?,
    size:androidx.compose.ui.unit.Dp=84.dp
) {
    val bitmap=remember(preview){Wq185FriendMascot.decode(preview)}
    Box(Modifier.width(size).height(size*1.30f)
        .background(Color(0xFFF2E6FF),RoundedCornerShape(17.dp)),
        contentAlignment=Alignment.Center){
        if(bitmap!=null)
            androidx.compose.foundation.Image(
                bitmap=bitmap.asImageBitmap(),
                contentDescription="Arkadaşın kişisel maskotu",
                modifier=Modifier.fillMaxSize(),
                contentScale=androidx.compose.ui.layout.ContentScale.Fit)
        else if(Wq2DMascots.valid(avatar) &&
                !Wq183MascotStore.isCustomId(avatar))
            Wq2DPortrait(avatar!!,Modifier.fillMaxSize())
        else Text("♧",fontSize=27.sp,color=Wq180Purple)
    }
}

''',
        "friend tile uses bounded real preview or safe builtin")

    edit("Wq180FriendCards.kt", [
        ("asImageBitmap import",
         "import androidx.compose.ui.graphics.Color",
         "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.asImageBitmap"),
        ("render correct friend's image",
         "            Wq180FriendFace(entry.avatarId)",
         "            Wq180FriendFace(entry.avatarId,entry.avatarPreview)"),
    ])

    gradle = ROOT / "source/app/build.gradle.kts"
    text = gradle.read_text("utf-8")
    assert text.count("versionCode = 110") == 1
    assert text.count('versionName = "1.0.84"') == 1
    pending[gradle] = text.replace("versionCode = 110","versionCode = 111",1) \
                          .replace('versionName = "1.0.84"',
                                   'versionName = "1.0.85"',1)
    for path, text in pending.items():
        path.write_text(text,encoding="utf-8")
    shutil.copyfile(TOOLS/"Wq185FriendMascot.kt",SRC/"Wq185FriendMascot.kt")
    print("PASS 1.0.85 cumulative code111; no Firebase or user data deleted")

if __name__=="__main__":
    apply()
