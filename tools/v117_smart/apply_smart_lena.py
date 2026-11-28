#!/usr/bin/env python3
"""Connect the actual spoken conversation reply to native V2 gesture selection."""
from pathlib import Path
import hashlib,json,struct,shutil
ROOT=Path(__file__).resolve().parents[2]
P=ROOT/'source/app/src/main/java/com/kelimebulmacasi/a1'
T=ROOT/'source/app/src/test/java/com/kelimebulmacasi/a1'
def once(s,a,b):
    assert s.count(a)==1,('anchor',a[:160],s.count(a))
    return s.replace(a,b,1)
p=P/'LenaLive3DStage.kt'; s=p.read_text()
s=once(s,'    speechRate: Float,\n    modifier:', '    speechRate: Float,\n    spokenReply: String? = null,\n    modifier:')
s=once(s,'    LaunchedEffect(node, autoMode, manualClip, manualReplay, seated) {',
    '    LaunchedEffect(node, autoMode, manualClip, manualReplay, seated, if (autoMode == AvatarMode.SPEAKING) spokenReply else null) {')
s=once(s,'        val choices = groups[group].orEmpty().filter { it in clips }',
'''        // One semantic opening per actual spoken turn; after the original-length
        // clip, normal native talking resumes. Manual previews remain independent.
        if (autoMode == AvatarMode.SPEAKING && !spokenReply.isNullOrBlank()) {
            val intentKey = "semantic_" + Wq117LenaMotion.classify(spokenReply).name + "_" + seated
            val cursor = autoCursor[intentKey] ?: 0
            val opening = Wq117LenaMotion.choose(spokenReply, seated, clips.keys, cursor)
            if (opening != null && playNative(opening, false)) {
                autoCursor[intentKey] = (cursor + 1) % 10000
                delay(clipDurationsMs[opening] ?: 4000L)
            }
        }
        val choices = groups[group].orEmpty().filter { it in clips }''')
assert s.count('model.playAnimation(')==1
assert 'speed = 1f' in s
assert 'LenaFaceAnimationClips.select' not in s
p.write_text(s)
p=P/'LiveTeacherScreen.kt';s=p.read_text()
assert 'ai.turns' in s and 'speaking' in s
s=once(s,'                    motion = avatarMotion.mode,',
'''                    motion = avatarMotion.mode,
                    spokenReply = ai.turns.firstOrNull { it.id == speaking }?.let {
                        Wq187LenaSpeech.forSpeech(it.answer, it.language)
                    },''')
p.write_text(s)
shutil.copyfile(ROOT/'tools/v117_smart/Wq117LenaMotion.kt',P/'Wq117LenaMotion.kt')
shutil.copyfile(ROOT/'tools/v117_smart/Wq117LenaMotionTest.kt',T/'Wq117LenaMotionTest.kt')
asset=ROOT/'source/app/src/main/assets/avatars/lena/Lena_Mixamo_38_Stable.glb'
b=asset.read_bytes()
assert hashlib.sha256(b).hexdigest()=='cb033dc3503f01e3fa64971c2bbe40b1260e7d9a96f7860796a32400328ad26f'
d=json.loads(b[20:20+struct.unpack_from('<I',b,12)[0]])
assert len(d['animations'])==38 and len(d['skins'][0]['joints'])==65 and len(d['materials'])==9
print('PASS: actual speaking turn linked to native motion selection; exact existing GLB preserved')
