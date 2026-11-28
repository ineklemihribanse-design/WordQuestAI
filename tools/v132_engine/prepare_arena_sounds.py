"""Create short, original, offline Arena sounds; no external audio or codecs."""
from pathlib import Path
import math, random, struct, wave
root=Path("source/app/src/main")
raw=root/"res/raw"
raw.mkdir(parents=True,exist_ok=True)
rate=24000
# (start, duration, pitch Hz, gain, timbre); zero pitch is a muted percussion hit.
packs={
 "crystal":{
 "correct":[(0,.23,659,.52,"bell"),(.09,.27,880,.45,"bell"),(.19,.31,1175,.35,"bell")],
 "wrong":[(0,.18,392,.38,"soft"),(.10,.25,294,.32,"soft")],
 "tick":[(0,.085,920,.20,"wood")],"urgent":[(0,.10,1100,.30,"wood"),(.11,.08,820,.22,"wood")],
 "ambient":[(0,.38,440,.12,"bell"),(.08,.33,660,.08,"bell")]},
 "stadium":{
 "correct":[(0,.20,392,.48,"brass"),(.10,.24,523,.42,"brass"),(.20,.32,784,.38,"brass")],
 "wrong":[(0,.20,220,.35,"brass"),(.13,.22,165,.28,"soft")],
 "tick":[(0,.09,0,.28,"wood")],"urgent":[(0,.12,0,.43,"wood"),(.13,.10,0,.32,"wood")],
 "ambient":[(0,.30,196,.14,"soft"),(.10,.25,294,.08,"brass")]},
 "digital":{
 "correct":[(0,.12,587,.38,"digital"),(.095,.12,784,.34,"digital"),(.19,.23,1047,.32,"digital")],
 "wrong":[(0,.13,349,.30,"digital"),(.13,.20,262,.27,"digital")],
 "tick":[(0,.065,720,.20,"digital")],"urgent":[(0,.09,960,.31,"digital"),(.10,.07,720,.21,"digital")],
 "ambient":[(0,.20,330,.11,"digital"),(.09,.20,495,.09,"digital")]},
 "soft":{
 "correct":[(0,.25,523,.40,"soft"),(.10,.30,659,.34,"soft"),(.20,.30,784,.30,"soft")],
 "wrong":[(0,.22,349,.25,"soft"),(.14,.25,294,.23,"soft")],
 "tick":[(0,.10,560,.15,"wood")],"urgent":[(0,.10,660,.22,"wood")],
 "ambient":[(0,.38,330,.08,"soft"),(.12,.32,440,.08,"soft")]}
}
for pack,events in packs.items():
 for event,notes in events.items():
  rng=random.Random(pack+"/"+event)
  duration=max(t+d for t,d,_,_,_ in notes)+.025
  buf=[0.0]*math.ceil(duration*rate)
  for start,dur,hz,gain,kind in notes:
   for n in range(int(dur*rate)):
    t=n/rate; u=t/dur; p=2*math.pi*(hz or 115)*t
    attack=min(1,t/.009); release=min(1,(dur-t)/.035)
    env=attack*release*math.exp(-u*(4.6 if kind=="wood" else 2.3))
    if kind=="bell": v=.72*math.sin(p)+.19*math.sin(2.01*p)+.09*math.sin(3.02*p)
    elif kind=="brass": v=.7*math.sin(p)+.19*math.sin(2*p)+.11*math.sin(3*p)
    elif kind=="digital": v=.75*math.sin(p+.55*math.sin(2*p))+.12*math.sin(3*p)
    elif kind=="wood": v=.7*math.sin(p)+.3*(rng.random()*2-1)*math.exp(-t*55)
    else: v=.92*math.sin(p)+.08*math.sin(2*p)
    i=int(start*rate)+n
    if i<len(buf):buf[i]+=gain*env*v
  peak=max(abs(v) for v in buf)
  assert .01<peak<1.0,(pack,event,peak)
  path=raw/f"wq150_{pack}_{event}.wav"
  with wave.open(str(path),"wb") as w:
   w.setparams((1,2,rate,0,"NONE","not compressed"))
   w.writeframes(b"".join(struct.pack("<h",round(v*32767*.82)) for v in buf))
  assert path.stat().st_size<40000
# Attach the selection to the existing Arena audio settings; source is reconstructed.
kt=root/"java/com/kelimebulmacasi/a1/Wq179ArenaExtras.kt"
s=kt.read_text()
anchor='    Text("Arena sesleri",fontWeight=FontWeight.Bold,color=Color(0xFF3C3258))'
assert s.count(anchor)==1
kt.write_text(s.replace(anchor,anchor+"\n    Wq150SoundPackSettings()"))
assert len(list(raw.glob("wq150_*_*.wav")))==20
print("PASS: 20 original PCM sound effects; 4 selectable packs.")
