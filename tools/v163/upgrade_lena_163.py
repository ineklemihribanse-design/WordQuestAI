#!/usr/bin/env python3
"""Lena v1.0.63: real skinned sclera, iris, pupils, eyelids, brown hair and mouth.

The original rig has no iris/pupils and its eyelid motion was incorrectly
centered on the BROWS (y~1.673); real eyelids are y~1.645. No painted dots
can replace missing eyeball geometry. Add small PBR mesh primitives to the
existing rigged head, including separate blue irises, dark pupils, catchlights
and blinking morph targets. Keep original 201 joints/16 legacy clips and the
2 TTS/idle morph animation clips. Never alter the original v6 source asset.
"""
import argparse,hashlib,json,math,struct,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"v162"))
from paint_lena_162 import read_chunks,encode_glb,accessor_values,srgb_linear_byte

def linear(rgb):
    return [((v/255)/12.92 if v/255<=.04045 else ((v/255+.055)/1.055)**2.4) for v in rgb]

def color_bytes(rgb):
    return tuple(max(0,min(255,srgb_linear_byte(int(v)))) for v in rgb)+(255,)

def append(doc,blob,raw,kind,component,count,target=None,normalized=False,bounds=None):
    blob+=b"\0"*((-len(blob))%4)
    offset=len(blob)
    view=len(doc["bufferViews"])
    entry={"buffer":0,"byteOffset":offset,"byteLength":len(raw)}
    if target is not None:entry["target"]=target
    doc["bufferViews"].append(entry)
    acc=len(doc["accessors"])
    obj={"bufferView":view,"componentType":component,"type":kind,"count":count}
    if normalized:obj["normalized"]=True
    if bounds is not None:obj.update({"min":bounds[0],"max":bounds[1]})
    doc["accessors"].append(obj)
    return blob+raw,acc

def add_material(doc,name,rgb,rough=.82):
    idx=len(doc.setdefault("materials",[]))
    doc["materials"].append({
        "name":name,"doubleSided":True,
        "pbrMetallicRoughness":{
            "baseColorFactor":linear(rgb)+[1.0],
            "metallicFactor":0.0,"roughnessFactor":rough}
    })
    return idx

def write_blob_array(blob,doc,index,raw):
    a=doc["accessors"][index]
    v=doc["bufferViews"][a["bufferView"]]
    offset=v.get("byteOffset",0)+a.get("byteOffset",0)
    assert len(raw)==v["byteLength"],(index,len(raw),v["byteLength"])
    bb=bytearray(blob)
    bb[offset:offset+len(raw)]=raw
    return bytes(bb)

def fpack(values):
    vals=[v for item in values for v in item]
    return struct.pack("<"+"f"*len(vals),*vals)

def upack(values):
    vals=[v for item in values for v in item]
    return struct.pack("<"+"H"*len(vals),*vals)

def eye_surface(p,x,y):
    # The measured Lena face surface is ~0.085–0.095m at eye level. We
    # weight nearby front-facing head vertices instead of a flat eye sticker.
    best=[]
    for px,py,pz in p:
        dx=px-x;dy=py-y
        dist2=dx*dx+dy*dy
        if dist2<.000065 and pz>.025 and pz<.150:
            best.append((dist2,pz))
    if not best:
        return .087
    best.sort(key=lambda item:item[0])
    near=[z for _,z in best[:10]]
    front=sorted(near,reverse=True)[:4]
    return sum(front)/len(front)

def procedural_mesh(doc,blob,meshes,skin_head,kind,material,verts,triangles,
                    speaking=None,blinking=None,colors=None):
    assert len(verts)>3
    speaking=speaking or [(0.,0.,0.)]*len(verts)
    blinking=blinking or [(0.,0.,0.)]*len(verts)
    assert len(speaking)==len(blinking)==len(verts)
    n=len(verts)
    normals=[(0.,0.,1.)]*n
    joints=[(skin_head,0,0,0)]*n
    weights=[(1.,0.,0.,0.)]*n
    mins=[min(p[i] for p in verts) for i in range(3)]
    maxs=[max(p[i] for p in verts) for i in range(3)]
    blob,pos=append(doc,blob,fpack(verts),"VEC3",5126,n,34962,bounds=(mins,maxs))
    blob,norm=append(doc,blob,fpack(normals),"VEC3",5126,n,34962)
    blob,joint=append(doc,blob,upack(joints),"VEC4",5123,n,34962)
    blob,weight=append(doc,blob,fpack(weights),"VEC4",5126,n,34962)
    blob,mi=append(doc,blob,fpack(speaking),"VEC3",5126,n,34962)
    blob,bi=append(doc,blob,fpack(blinking),"VEC3",5126,n,34962)
    flat=[i for face in triangles for i in face]
    assert min(flat)>=0 and max(flat)<n
    blob,idx=append(doc,blob,struct.pack("<"+"H"*len(flat),*flat),"SCALAR",5123,len(flat),34963)
    attrs={"POSITION":pos,"NORMAL":norm,"JOINTS_0":joint,"WEIGHTS_0":weight}
    if colors is not None:
        assert len(colors)==n
        raw=bytes(v for c in colors for v in c)
        blob,ci=append(doc,blob,raw,"VEC4",5121,n,34962,normalized=True)
        attrs["COLOR_0"]=ci
    meshes.append({"attributes":attrs,"indices":idx,"material":material,
                   "mode":4,"targets":[{"POSITION":mi},{"POSITION":bi}],
                   "extras":{"name":kind}})
    return blob

def build(source):
    doc,blob=read_chunks(source)
    assert len(doc.get("skins",[]))==1 and len(doc["skins"][0]["joints"])==201
    assert len(doc["animations"])==18
    mesh=doc["meshes"][0];prims=mesh["primitives"]
    assert len(prims)==1 and len(prims[0].get("targets",[]))==2
    body=prims[0]
    coords=accessor_values(doc,blob,body["attributes"]["POSITION"])
    skin_names=[doc["nodes"][i].get("name","") for i in doc["skins"][0]["joints"]]
    assert skin_names.count("head")==1
    head_bone=skin_names.index("head")
    mouth_bone=skin_names.index("neck_02")  # The original lower face is weighted to neck_02
    print("Head joint index:",head_bone)
    joints=accessor_values(doc,blob,body["attributes"]["JOINTS_0"])
    weights=accessor_values(doc,blob,body["attributes"]["WEIGHTS_0"])
    col_acc=body["attributes"]["COLOR_0"]
    colors=accessor_values(doc,blob,col_acc)
    assert len(coords)==len(colors)
    counts={"hair":0,"skin_face":0,"eyebrow":0,"lips":0}
    new_colors=bytearray()
    for (x,y,z),joint,weight,orig in zip(coords,joints,weights,colors):
        # Preserve shirt, hands and trousers while repainting the head.
        idx=max(range(4),key=lambda k:weight[k])
        bone=skin_names[joint[idx]]
        if bone in ("head","neck_02") and y>1.50:
            ax=abs(x)
            # Brown side braids/cornrows as well as hairline and crown.
            hair=(y>1.714 or
                (ax>.108 and y>1.52 and z<.087) or
                (ax>.087 and y>1.675 and z<.080))
            if hair:
                strand=.5+.5*math.sin(x*231+y*389+z*413)
                brown=(90+int(24*strand),48+int(15*strand),32+int(12*strand))
                rgb=color_bytes(brown);counts["hair"]+=1
            elif 1.53<y<1.710:
                # Replace overbroad eye/lip paints with warm even skin.
                rgb=color_bytes((218,164,137));counts["skin_face"]+=1
                # Only eyebrow ridge, not pupil location or lids.
                if .030<ax<.080 and 1.679<y<1.690 and z>.067:
                    rgb=color_bytes((92,55,42));counts["eyebrow"]+=1
                # Natural muted rose, centered on actual lip line, not cheeks.
                elif ax<.040 and z>.092 and ((x/.040)**2+((y-1.563)/.010)**2)<1:
                    rgb=color_bytes((169,93,99));counts["lips"]+=1
                elif .055<ax<.094 and 1.589<y<1.613 and z>.055:
                    rgb=color_bytes((218,154,139))
            else:rgb=tuple(orig)
        else:rgb=tuple(orig)
        new_colors.extend(rgb)
    print("v163 base paint:",counts)
    if counts["hair"]<150 or counts["skin_face"]<250 or counts["lips"]<20:
        raise ValueError("Model paint masks do not match original Lena geometry")
    blob=write_blob_array(blob,doc,col_acc,bytes(new_colors))

    # The V162 blink moved eyebrows (~y1.67), not eyelids (~y1.645).
    # Clear the entire old facial morph deltas to avoid cheek distortion.
    # Real eye/blink/mouth geometry is provided below on this same skin.
    for target in body["targets"]:
        accessor=target["POSITION"]
        blank=b"\0"*(len(coords)*3*4)
        blob=write_blob_array(blob,doc,accessor,blank)

    mats={}
    for name,rgb,rough in [
        ("sclera",(238,235,228),.32),
        ("iris",(255,255,255),.34),
        ("pupil",(19,19,25),.22),
        ("glint",(255,252,245),.17),
        ("eyelash",(92,56,44),.83),
        ("mouth",(62,24,31),.92)]:
        mats[name]=add_material(doc,"Lena163_"+name,rgb,rough)

    # Eye positions measured from actual source GLB, NOT guessed from brow.
    # Sculpted eyes are blank/closed shells; these are new 3D eyeball features.
    face=[p for p in coords if 1.620<p[1]<1.665 and abs(p[0])<.105 and p[2]>.025]
    assert len(face)>500
    def depth(x,y):
        return eye_surface(face,x,y)
    eye_count=0
    for direction in (-1,1):
        x0=direction*.0445;y0=1.646
        rx=.025;ry=.0105
        # Front-facing almond-shaped warm white sclera patch.
        verts=[(x0,y0,depth(x0,y0)+.0055)]
        blink=[(0.,0.,-.008)]
        for k in range(32):
            a=k*math.tau/32
            dx=rx*math.cos(a);dy=ry*math.sin(a)*(1.-.28*abs(math.cos(a)))
            x=x0+dx;y=y0+dy
            verts.append((x,y,depth(x,y)+.0050))
            blink.append((0.,y0-y,-.008))
        tris=[(0,k+1,((k+1)%32)+1) for k in range(32)]
        blob=procedural_mesh(doc,blob,prims,head_bone,"sclera",mats["sclera"],
            verts,tris,blinking=blink)
        # Natural blue iris with darker limbal ring + radial fiber shading.
        irisx=x0+direction*.001;irisy=y0
        verts=[];cols=[];bm=[]
        verts.append((irisx,irisy,depth(irisx,irisy)+.0087))
        cols.append(color_bytes((59,115,148)));bm.append((0.,0.,-.011))
        rings=[(.36,(59,133,172)),(.68,(47,109,151)),(1.0,(29,67,107))]
        for radius,base_rgb in rings:
            for k in range(32):
                a=math.tau*k/32
                x=irisx+(.0081*radius)*math.cos(a)
                y=irisy+(.0083*radius)*math.sin(a)
                verts.append((x,y,depth(x,y)+.0080))
                fiber=.5+.5*math.sin(k*3.6+direction*.9)
                cols.append(color_bytes(tuple(min(255,int(c*(.89+.18*fiber))) for c in base_rgb)))
                bm.append((0.,irisy-y,-.012))
        tris=[(0,k+1,((k+1)%32)+1) for k in range(32)]
        for r in range(2):
            start1=1+r*32;start2=start1+32
            for k in range(32):
                a=start1+k;b=start1+(k+1)%32;c=start2+k;d=start2+(k+1)%32
                tris.extend([(a,c,b),(b,c,d)])
        blob=procedural_mesh(doc,blob,prims,head_bone,"iris",mats["iris"],
            verts,tris,blinking=bm,colors=cols)
        # Black pupils and small catchlights to restore gaze/eye life.
        for layer,cx,cy,rad,material,offset in [
            ("pupil",irisx,irisy,.0037,mats["pupil"],.0101),
            ("glint",irisx-.002,irisy+.002,.0013,mats["glint"],.0120)]:
            verts=[(cx,cy,depth(cx,cy)+offset)]
            bm=[(0.,0.,-.015)]
            for k in range(24):
                a=math.tau*k/24
                x=cx+rad*math.cos(a);y=cy+rad*math.sin(a)
                verts.append((x,y,depth(x,y)+offset))
                bm.append((0.,irisy-y,-.015))
            tris=[(0,k+1,(k+1)%24+1) for k in range(24)]
            blob=procedural_mesh(doc,blob,prims,head_bone,layer,material,
                verts,tris,blinking=bm)
        # Soft upper eyelash contour; makes blink look like closed eyes.
        verts=[];bm=[];tris=[]
        for k in range(21):
            a=math.pi*k/20
            x=x0+rx*math.cos(a);y=y0+(ry+.0015)*math.sin(a)
            for extra in (0.,-.0013):
                verts.append((x,y+extra,depth(x,y)+.007))
                bm.append((0.,y0-(y+extra),-.005))
            if k<20:
                t=2*k;tris.extend([(t,t+1,t+2),(t+1,t+3,t+2)])
        blob=procedural_mesh(doc,blob,prims,head_bone,"eyelash",mats["eyelash"],
             verts,tris,blinking=bm)
        eye_count+=1

    # The TTS mouth morph is a small dark INNER opening between actual lips;
    # opening only during the SPEAKING animation, not the cheek or nose.
    cx=0.;cy=1.563
    def mouth_depth(x,y):
        face=[p[2] for p in coords if abs(p[0]-x)<.008 and abs(p[1]-y)<.006 and p[2]>.08]
        return max(face) if face else .112
    verts=[(cx,cy,mouth_depth(cx,cy)+.003)]
    moving=[(0.,0.,.003)]
    for k in range(32):
        a=k*math.tau/32
        x=cx+.021*math.cos(a)
        y=cy
        verts.append((x,y,mouth_depth(x,y)+.003))
        moving.append((0.,.0062*math.sin(a),.003))
    tris=[(0,k+1,(k+1)%32+1) for k in range(32)]
    blob=procedural_mesh(doc,blob,prims,mouth_bone,"inner_mouth",mats["mouth"],
        verts,tris,speaking=moving)
    doc["buffers"][0]["byteLength"]=len(blob)
    painted=encode_glb(doc,blob)
    valid,_=read_chunks(painted)
    assert len(valid["meshes"][0]["primitives"])>=12
    assert all(len(p.get("targets",[]))==2 for p in valid["meshes"][0]["primitives"])
    assert len(valid["skins"][0]["joints"])==201 and len(valid["animations"])==18
    assert eye_count==2
    return painted,counts,len(valid["meshes"][0]["primitives"])

def main():
    p=argparse.ArgumentParser()
    p.add_argument("source")
    p.add_argument("output")
    p.add_argument("--manifest")
    a=p.parse_args()
    original=Path(a.source).read_bytes()
    result,counts,prims=build(original)
    dest=Path(a.output)
    dest.parent.mkdir(parents=True,exist_ok=True)
    dest.write_bytes(result)
    print("Lena163:",prims,"skinned PBR primitives",len(result),
          "bytes, increase",len(result)-len(original),
          "201 bones/18 animation clips, new blue irises/dark pupils/brown hair.")
    if a.manifest:
        f=Path(a.manifest);m=json.loads(f.read_text("utf-8"))
        m.update({"sha256":hashlib.sha256(result).hexdigest(),
            "version":"1.0.63",
            "appearance":"Chestnut brown hair, realistic blue irises, black pupils, eye catchlights and warm skin",
            "facialGeometry":"Sculpted original lid shell + skinned blink-shrinking real eyeball primitives; dark opening at real mouth",
            "faceFix":"Old brow and cheek morph offsets removed; speech opens only new mouth geometry",
            "originalSourceUntouched":True})
        f.write_text(json.dumps(m,ensure_ascii=False,indent=2)+"\n","utf-8")

if __name__=="__main__":
    main()
