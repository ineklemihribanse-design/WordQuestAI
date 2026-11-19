#!/usr/bin/env python3
"""Lena v1.0.64: reconstruct original Prisma3D OBJ-defined mesh parts.

The user's Prisma3D 'zip' is actually the raw .obj, whose vertices AND
all 47,904 triangle indices match our stored original v6 GLB exactly.
The old white one-primitive mesh lost the nine OBJ material boundaries.
Previous face-coordinate repainting and floating false eyes/mouth were wrong.
Use original per-part index ranges; preserve original 201-joint rig and
16 clips, adding two subtle real-mesh facial-weight clips without fake eyes.
No source GLB, Alex, chat, API key or other app feature is changed.
"""
import hashlib,json,math,struct
from collections import Counter,defaultdict
from pathlib import Path

SOURCE_SHA="548d70117df4599300c77dc3fde3c72a2111f78ff582e741862c25fcdf364617"
OBJ_SHA="9ac3147dcbd838eb562e47a1ad580af59603124b3cdfd0a168cf36eac57786f1"
# (name, first vertex, vertex count, first original OBJ triangle, triangles)
PARTS=[
 ("Body",0,18192,0,9096),("Tshirt",18192,1530,9096,766),
 ("Sneakers",19722,2400,9862,1200),("Pants",22122,1640,11062,820),
 ("Hair",23762,5240,11882,2608),("Eyes",29002,640,14490,320),
 ("Eyelash",29642,860,14810,428),("Eyebrow",30502,288,15238,144),
 ("Belt",30790,1168,15382,586)]
BASE={
 "Body":(142,85,65),"Tshirt":(245,242,237),"Sneakers":(238,237,230),
 "Pants":(43,47,66),"Hair":(47,31,27),"Eyes":(239,235,227),
 "Eyelash":(31,23,22),"Eyebrow":(46,30,25),"Belt":(78,56,44)}
ROUGH={"Body":.82,"Tshirt":.91,"Sneakers":.72,"Pants":.92,"Hair":.65,
 "Eyes":.29,"Eyelash":.82,"Eyebrow":.86,"Belt":.73}

def srgb(x):
 v=x/255
 return v/12.92 if v<=.04045 else ((v+.055)/1.055)**2.4

def rgba(color):
 return bytes([min(255,max(0,round(srgb(v)*255))) for v in color]+[255])

def read_glb(raw):
 magic,version,total=struct.unpack_from("<4sII",raw)
 assert magic==b"glTF" and version==2 and len(raw)==total
 at=12;parts=[]
 while at<len(raw):
  n,typ=struct.unpack_from("<I4s",raw,at)
  parts.append((typ,raw[at+8:at+8+n]));at+=8+n
 assert len(parts)==2 and parts[0][0]==b"JSON" and parts[1][0]==b"BIN\x00"
 return json.loads(parts[0][1]),parts[1][1]

def encode(doc,blob):
 j=json.dumps(doc,ensure_ascii=False,separators=(",",":")).encode()
 j+=b" "*((-len(j))%4);blob+=b"\0"*((-len(blob))%4)
 length=12+8+len(j)+8+len(blob)
 return (struct.pack("<4sII",b"glTF",2,length)+
  struct.pack("<I4s",len(j),b"JSON")+j+
  struct.pack("<I4s",len(blob),b"BIN\x00")+blob)

def getacc(doc,blob,idx):
 a=doc["accessors"][idx];v=doc["bufferViews"][a["bufferView"]]
 fmt,sz={5121:("B",1),5123:("H",2),5125:("I",4),5126:("f",4)}[a["componentType"]]
 n={"SCALAR":1,"VEC2":2,"VEC3":3,"VEC4":4}[a["type"]]
 unpack=struct.Struct("<"+fmt*n).unpack_from
 off=v.get("byteOffset",0)+a.get("byteOffset",0);stride=v.get("byteStride",sz*n)
 return [unpack(blob,off+i*stride) for i in range(a["count"])]

def add(doc,blob,raw,typ,component,count,target=None,normalized=False,bounds=None):
 blob+=b"\0"*((-len(blob))%4)
 view=len(doc["bufferViews"])
 d={"buffer":0,"byteOffset":len(blob),"byteLength":len(raw)}
 if target is not None:d["target"]=target
 doc["bufferViews"].append(d)
 idx=len(doc["accessors"])
 a={"bufferView":view,"componentType":component,"count":count,"type":typ}
 if normalized:a["normalized"]=True
 if bounds is not None:a["min"],a["max"]=bounds
 doc["accessors"].append(a)
 return blob+raw,idx

def face_color(part,x,y,z):
 if part=="Hair":
  t=(math.sin(x*205+y*251+z*183)+math.cos(y*193+x*112))*.5
  return (int(49+7*t),int(30+5*t),int(27+4*t))
 if part=="Eyes":
  cx=.034 if x>0 else -.034
  dx=(x-cx)/.0096;dy=(y-1.6465)/.0101
  r=math.hypot(dx,dy)
  if r<.31:return (19,16,14)
  if r<.78:
   f=.5+.5*math.sin(math.atan2(dy,dx)*21+int(r*14)*.71)
   return (int(159+39*f),int(113+22*f),int(32+12*f))
  if r<.95:return (69,51,32)
  return (241,235,225)
 if part=="Body" and abs(x)<.037 and z>.104 and 1.549<y<1.576:
  r=math.hypot(x/.041,(y-1.563)/.015)
  if r<1:
   w=.40*(1-r)
   return (round(142*(1-w)+134*w),round(85*(1-w)+65*w),65)
 return BASE[part]

def mouth(part,x,y,z):
 # v1.0.64 used a maximum lower-lip offset of only 4.8 mm, damped by a
 # falloff and voice cue. It was effectively invisible in the actual camera.
 # Gently open the original lips in 3D while preserving the user's approved
 # facial sculpt: lower lip 10 mm down, upper lip 3.5 mm up. No chin/cheek
 # movement, no fake black patch, and no new mesh.
 if part!="Body" or z<.099 or abs(x)>.041 or not 1.545<y<1.580:
  return (0.,0.,0.)
 radial=max(0.,1-(x/.041)**2-((y-1.562)/.019)**2)
 if radial<.09:return (0.,0.,0.)
 if y<=1.563:
  return (0.,-.0105*radial,-.001*radial)
 return (0.,.0035*radial,.0004*radial)

def blink(part,x,y,z):
 if part not in ("Eyes","Eyelash"):return (0.,0.,0.)
 cx=.034 if x>0 else -.034
 r=max(0.,1-((x-cx)/.03)**2)
 return (0.,(1.6465-y)*.93*r,-.0018*r)

def smooth_normals(doc,blob,src,coords):
 normals=getacc(doc,blob,src["attributes"]["NORMAL"])
 result=list(normals);changed=0
 for name,start,count,_,_ in PARTS:
  if name not in ("Body","Eyes","Tshirt","Sneakers","Pants"):continue
  groups=defaultdict(list)
  for i in range(start,start+count):
   groups[tuple(round(v,6) for v in coords[i])].append(i)
  for indices in groups.values():
   if len(indices)<2:continue
   for i in indices:
    x,y,z=normals[i]
    compatible=[normals[j] for j in indices if x*normals[j][0]+y*normals[j][1]+z*normals[j][2]>.62]
    if len(compatible)<2:continue
    xx=sum(v[0] for v in compatible);yy=sum(v[1] for v in compatible);zz=sum(v[2] for v in compatible)
    norm=math.sqrt(xx*xx+yy*yy+zz*zz)
    if norm>.001:result[i]=(xx/norm,yy/norm,zz/norm);changed+=1
 a=doc["accessors"][src["attributes"]["NORMAL"]];v=doc["bufferViews"][a["bufferView"]]
 assert a["componentType"]==5126 and a["count"]==31958 and "byteStride" not in v
 offset=v.get("byteOffset",0)+a.get("byteOffset",0)
 data=bytearray(blob)
 for i,n in enumerate(result):struct.pack_into("<fff",data,offset+i*12,*n)
 print("smoothed original face/body normals",changed)
 return bytes(data)

def animation(doc,blob,node,name,frames):
 times=[float(t) for t,m,b in frames]
 weights=[float(w) for t,m,b in frames for w in (m,b)]
 assert all(a<b for a,b in zip(times,times[1:]))
 blob,it=add(doc,blob,struct.pack("<"+"f"*len(times),*times),
             "SCALAR",5126,len(times),bounds=([times[0]],[times[-1]]))
 blob,iw=add(doc,blob,struct.pack("<"+"f"*len(weights),*weights),
             "SCALAR",5126,len(weights))
 doc["animations"].append({"name":name,"samplers":[
  {"input":it,"output":iw,"interpolation":"LINEAR"}],
  "channels":[{"sampler":0,"target":{"node":node,"path":"weights"}}]})
 return blob

def build(original,output,manifest=None):
 raw=Path(original).read_bytes()
 assert hashlib.sha256(raw).hexdigest()==SOURCE_SHA,"Original source GLB differs"
 doc,blob=read_glb(raw)
 assert len(doc["meshes"])==1 and len(doc["meshes"][0]["primitives"])==1
 assert len(doc["animations"])==16 and len(doc["skins"][0]["joints"])==201
 src=doc["meshes"][0]["primitives"][0]
 coords=getacc(doc,blob,src["attributes"]["POSITION"])
 idx=[v[0] for v in getacc(doc,blob,src["indices"])]
 assert len(coords)==31958 and len(idx)==47904
 blob=smooth_normals(doc,blob,src,coords)
 colors=bytearray();mm=[];bb=[];counts=Counter()
 for part,start,n,first,nfaces in PARTS:
  for x,y,z in coords[start:start+n]:
   colors.extend(rgba(face_color(part,x,y,z)))
   mv=mouth(part,x,y,z);bv=blink(part,x,y,z)
   mm.extend(mv);bb.extend(bv)
   if any(abs(q)>1e-8 for q in mv):counts["mouth"]+=1
   if any(abs(q)>1e-8 for q in bv):counts["blink"]+=1
 assert len(colors)==127832 and 30<counts["mouth"]<700 and 200<counts["blink"]<1600,counts
 blob,ca=add(doc,blob,bytes(colors),"VEC4",5121,31958,34962,normalized=True)
 blob,ma=add(doc,blob,struct.pack("<"+"f"*len(mm),*mm),"VEC3",5126,31958,34962)
 blob,ba=add(doc,blob,struct.pack("<"+"f"*len(bb),*bb),"VEC3",5126,31958,34962)
 newparts=[];materials=[]
 for part,start,n,first,nfaces in PARTS:
  indices=idx[first*3:(first+nfaces)*3]
  assert len(indices)==nfaces*3 and min(indices)>=start and max(indices)<start+n,part
  blob,ia=add(doc,blob,struct.pack("<"+"I"*len(indices),*indices),
             "SCALAR",5125,len(indices),34963)
  material=len(materials)
  materials.append({"name":"OriginalPrisma3D_"+part,"doubleSided":True,
   "pbrMetallicRoughness":{"baseColorFactor":[1,1,1,1],
   "metallicFactor":.02 if part=="Hair" else 0,"roughnessFactor":ROUGH[part]}})
  attrs=dict(src["attributes"]);attrs["COLOR_0"]=ca
  newparts.append({"attributes":attrs,"indices":ia,"material":material,
   "mode":4,"targets":[{"POSITION":ma},{"POSITION":ba}],
   "extras":{"originalPrisma3DObject":part}})
 doc["meshes"][0]["primitives"]=newparts
 doc["meshes"][0]["weights"]=[0,0]
 doc["meshes"][0]["extras"]={"targetNames":["MouthOpen","EyelidBlink"]}
 doc["materials"]=materials
 node=next(i for i,n in enumerate(doc["nodes"]) if n.get("mesh")==0)
 idle=[(0,0,0),(1.38,0,0),(1.48,0,1),(1.58,0,0),
       (3.80,0,0),(3.90,0,1),(4.0,0,0),(4.8,0,0)]
 talk=[(0,0,0),(.13,.82,0),(.29,.12,0),(.46,.90,0),(.62,.2,0),
       (.78,.73,0),(.96,.22,0),(1.14,.89,0),(1.32,.07,0),
       (1.48,.78,0),(1.63,.19,0),(1.76,.79,0),(1.94,.03,0),
       (2.10,.65,0),(2.27,.15,0),(2.43,.76,0),(2.60,.10,1),
       (2.76,.85,0),(2.95,.13,0),(3.12,.8,0),(3.30,.25,0),
       (3.49,.82,0),(3.68,.19,0),(3.85,.91,0),(4.02,.14,0),
       (4.25,.72,0),(4.45,.06,0),(4.65,.89,0),(4.8,0,0)]
 blob=animation(doc,blob,node,"Lena_V165_Face_IdleBlink",idle)
 blob=animation(doc,blob,node,"Lena_V165_Face_TalkBlink",talk)
 doc["buffers"][0]["byteLength"]=len(blob)
 doc["extras"]={"originalPrismaObjSha256":OBJ_SHA,"materialAssignment":"9 exact OBJ component ranges"}
 out=encode(doc,blob)
 test,_=read_glb(out)
 assert len(test["materials"])==9 and len(test["meshes"][0]["primitives"])==9
 assert len(test["skins"][0]["joints"])==201 and len(test["animations"])==18
 assert sum(test["accessors"][p["indices"]]["count"] for p in test["meshes"][0]["primitives"])==47904
 dest=Path(output);dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(out)
 print("v164 source-part GLB saved:",len(out),"bytes; 9 original materials, 201 bones, 18 clips; morph counts",dict(counts))
 if manifest:
  f=Path(manifest);m=json.loads(f.read_text())
  m.update({"baseAssetSha256":SOURCE_SHA,"sha256":hashlib.sha256(out).hexdigest(),
   "version":"1.0.65","originalPrismaObjSha256":OBJ_SHA,
   "appearance":"Dark brown braided hair, warm brown skin, gold eyes, ivory outfit from exact original Prisma3D groups",
   "facialAnimations":["Lena_V165_Face_IdleBlink","Lena_V165_Face_TalkBlink"],
   "facialMorphTargets":["MouthOpen","EyelidBlink"],
   "note":"Original face unchanged; subtle upper/lower lip opening during actual TTS, not phoneme-accurate."})
  f.write_text(json.dumps(m,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")

if __name__=="__main__":
 import sys
 assert 3<=len(sys.argv)<=4
 build(sys.argv[1],sys.argv[2],sys.argv[3] if len(sys.argv)==4 else None)
