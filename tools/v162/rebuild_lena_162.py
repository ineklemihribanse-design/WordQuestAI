#!/usr/bin/env python3
"""Rebuild Lena's corrected 1.0.62 visual model from the UNMODIFIED v6 GLB.
Never color an already-colorized or morphed model; do not touch source asset.
"""
from pathlib import Path
import subprocess,sys,tempfile,hashlib,json

HERE=Path(__file__).resolve().parent

def main():
    if len(sys.argv)<3: sys.exit("usage: rebuild_lena_162.py original.glb corrected.glb [manifest.json]")
    original=Path(sys.argv[1]);dest=Path(sys.argv[2])
    manifest=Path(sys.argv[3]) if len(sys.argv)>3 else None
    baseline=original.read_bytes()
    with tempfile.TemporaryDirectory(prefix="wq-lena162-") as root:
        painted=Path(root)/"painted.glb"
        cmd=[sys.executable,str(HERE/"paint_lena_162.py"),str(original),str(painted)]
        if manifest:cmd+=["--manifest",str(manifest)]
        subprocess.run(cmd,check=True)
        cmd=[sys.executable,str(HERE/"face_lena_162.py"),str(painted),str(dest)]
        if manifest:cmd+=["--manifest",str(manifest)]
        subprocess.run(cmd,check=True)
    assert original.read_bytes()==baseline, "Original GLB must remain untouched"
    sys.path.insert(0,str(HERE))
    from paint_lena_162 import read_chunks,accessor_values
    source,_=read_chunks(baseline)
    data,blob=read_chunks(dest.read_bytes())
    prim=data["meshes"][0]["primitives"][0]
    assert len(data.get("animations",[]))==18
    assert len(source.get("animations",[]))==16
    assert len(data["skins"][0]["joints"])==len(source["skins"][0]["joints"])==201
    assert len(prim["targets"])==2
    assert "COLOR_0" in prim["attributes"]
    pts=accessor_values(data,blob,prim["attributes"]["POSITION"])
    for target,label,low,high in zip(prim["targets"],["mouth","blink"],[1.545,1.659],[1.582,1.687]):
        delta=accessor_values(data,blob,target["POSITION"])
        moving=[(p,d) for p,d in zip(pts,delta) if abs(d[0])+abs(d[1])+abs(d[2])>1e-7]
        ys=[p[1] for p,d in moving]
        assert len(moving)>30,(label,len(moving))
        assert min(ys)>=low and max(ys)<=high,(label,min(ys),max(ys))
        print("CORRECTED FACE",label,"vertices",len(moving),"y_bounds",min(ys),max(ys))
    print("CORRECTED GLB",len(dest.read_bytes()),"bytes; 201 joints; 18 clips; untouched baseline SHA",hashlib.sha256(baseline).hexdigest())
    if manifest:
        m=json.loads(manifest.read_text())
        assert m["baseAssetSha256"]==hashlib.sha256(baseline).hexdigest()
        assert m["sha256"]==hashlib.sha256(dest.read_bytes()).hexdigest()
        print("MANIFEST MATCHED",m["sha256"])
if __name__=="__main__":main()
