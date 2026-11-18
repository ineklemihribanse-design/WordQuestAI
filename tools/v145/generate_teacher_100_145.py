#!/usr/bin/env python3
"""Reproducible 100-frame teacher animation, source of truth is ONE portrait.

Face anchor points NEVER move: pixel-identical hair/skin/eyes outside the
actual lip and eyelid animation masks. This fixes expression-to-expression
face drift introduced by independently generated patches in 1.0.43.

The underlying master is genuinely full body (for future camera expansion).
Current Android surface uses its waist-up crop, with normally relaxed arms.
"""
from __future__ import annotations
import argparse, hashlib, io, json, math, zipfile
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageChops

W, H = 256, 384
COLUMNS, ROWS = 10, 10
N = COLUMNS * ROWS
MOUTH_X, MOUTH_Y = 121, 85
EYE_BOXES = [(100, 57, 124, 70), (132, 57, 156, 70)]


def breathing(base: Image.Image, phase: float) -> Image.Image:
    # A fraction of one pixel only on the upper torso. No head motion/jitter.
    drift = 0.31 * math.sin(phase)
    if abs(drift) < .03:
        return base.copy()
    moved = base.transform(base.size, Image.Transform.AFFINE,
                           (1, 0, 0, 0, 1, drift), resample=Image.Resampling.BICUBIC)
    mask = Image.new('L', (W,H), 0)
    d = ImageDraw.Draw(mask)
    d.ellipse((69, 136, 188, 295), fill=90)
    mask = mask.filter(ImageFilter.GaussianBlur(20))
    return Image.composite(moved, base, mask)


def hands_low(image: Image.Image, gesture: int, strength: float) -> Image.Image:
    # Hands at sides by DEFAULT. A gesture is a small movement of ONE wrist,
    # never both arms permanently up. No separate floating hand overlays.
    if not gesture or strength < .025:
        return image
    moved = image.transform(image.size, Image.Transform.AFFINE,
                            (1, 0, (-2.0 if gesture==1 else 1.5)*strength,
                             0, 1, 2.3*strength),resample=Image.Resampling.BICUBIC)
    mask = Image.new('L', (W,H), 0)
    d = ImageDraw.Draw(mask)
    # left arm x30–77, right arm x180–232
    d.ellipse((22, 243, 82, 389) if gesture==1 else (177, 243, 235, 389), fill=125)
    mask=mask.filter(ImageFilter.GaussianBlur(15))
    return Image.composite(moved, image, mask)


def blink(image: Image.Image, amount: float) -> Image.Image:
    """Soft same-face eyelid closure, with no pasted dislocated eye rectangles.

    Each eyelid is synthesized solely from neighboring skin colors, blended
    per pixel with an oval weight that is zero at the eye boundary. Critically,
    hairline, eyebrow, nose and cheek never shift, even during full closure.
    """
    if amount <= .01:
        return image
    original=image.convert('RGB')
    out=original.copy()
    orig=original.load(); dst=out.load()
    for left,top,right,bottom in ((100,59,121,69),(121,59,143,69)):
        cx=(left+right-1)/2.0
        rx=(right-left)/2.0
        cy=(top+bottom-1)/2.0
        ry=(bottom-top)/2.0
        for x in range(left,right):
            sx=abs(x-cx)/rx
            if sx>=1:continue
            horizontal=max(0.0,1-sx*sx)**1.0
            # Skin is from the EXACT same local portrait, just above and below the iris.
            upper=orig[x,bottom+1]
            lower=orig[x,bottom+4]
            for y in range(top,bottom):
                sy=abs(y-cy)/ry
                if sy>=1:continue
                # Inner part closes first; edge always matches original source.
                envelope=horizontal * max(0.0,1-sy*sy)**.75
                alpha=max(0.0,min(1.0, amount*envelope*1.7))
                if alpha<.001:continue
                t=(y-top)/max(1,(bottom-top-1))
                skin=[int(upper[c]*(1-t)+lower[c]*t) for c in range(3)]
                # Lowered upper eyelid has one thin, softly shaded crease.
                if abs(y-(cy+1.2))<.8:
                    shade=1-0.18*horizontal*amount
                    skin=[int(v*shade) for v in skin]
                old=orig[x,y]
                dst[x,y]=tuple(int(old[c]*(1-alpha)+skin[c]*alpha) for c in range(3))
    return out


def mouth(image: Image.Image, viseme: int, amount: float) -> Image.Image:
    """Animate the actual lips, never paste a new full-face rectangular patch.

    All frame changes are confined to a feathered 29x13-pixel lip zone at
    one fixed landmark. Eye, nose, chin, jawline and hair remain pixel-identical.
    Original upper/lower lip shading is preserved through alpha compositing.
    """
    if amount < .04 or viseme in (0, 6):
        return image
    specs = {1:(18.0,5.1),2:(23.0,2.6),3:(19.0,2.4),
             4:(11.0,4.4),5:(10.0,3.2)}
    width,height = specs.get(viseme,(17.0,3.2))
    # Realistic mouth is normally narrow and changes shape within its lips.
    op=max(0.0,min(1.0,amount))
    height = max(.2,height*(.24+.76*op))
    width *= .75+.25*op
    scale=8
    patchw,patchh=36,19
    overlay=Image.new('RGBA',(patchw*scale,patchh*scale),(0,0,0,0))
    d=ImageDraw.Draw(overlay)
    cx=(patchw/2)*scale
    cy=(patchh/2-1.3)*scale
    rx=width*scale/2
    ry=height*scale/2
    # Recessed cavity, softened on its boundary; dark natural mouth shading.
    # Unlike 1.0.44 prototype this has NO large opaque ellipse over cheek.
    d.ellipse((cx-rx,cy-ry,cx+rx,cy+ry), fill=(65,28,35,145))
    if viseme in (2,3) and op > .4:
        t=max(1,round(height*scale*.19))
        d.rounded_rectangle((cx-rx*.68,cy-ry*.73,cx+rx*.68,cy-ry*.73+t),
                            radius=1.3*scale, fill=(241,218,198,115))
    # Lower lip edge catches subtle light, matching the existing pink lip.
    d.arc((cx-rx,cy-ry,cx+rx,cy+ry+1.2*scale),start=10,end=168,
          fill=(187,110,112,100),width=scale//2)
    patch=overlay.resize((patchw,patchh),Image.Resampling.LANCZOS)
    mask=patch.getchannel('A')
    feather=Image.new('L',(patchw,patchh),0)
    md=ImageDraw.Draw(feather)
    md.ellipse((3,2,patchw-4,patchh-3),fill=255)
    feather=feather.filter(ImageFilter.GaussianBlur(1.2))
    patch.putalpha(ImageChops.multiply(mask,feather))
    out=image.convert('RGBA')
    out.alpha_composite(patch,(MOUTH_X-patchw//2,MOUTH_Y-patchh//2))
    return out.convert('RGB')


def params(i:int):
    """100 distinct reproducible frames: 10 idle, 60 speech, 10 listen,
    10 think, 10 gesture. One consistent human and stationary face geometry.
    """
    if i < 10:
        blink_level={4:.55,5:1.0,6:.5}.get(i,0)
        return dict(status='idle',viseme=0,aperture=0,blink=blink_level,
                    gesture=0,strength=0,breath=2*math.pi*i/10)
    if i < 70:
        group=(i-10)//10
        phase=(i-10)%10
        aperture=[.05,.16,.38,.58,.82,.99,.84,.61,.34,.14][phase]
        blink_level={24:.5,25:1.,26:.6,54:.6,55:1.,56:.5}.get(i,0)
        # 2/3 of talking frames have BOTH hands relaxed at sides.
        gesture=0 if group in (0,1,3,5) else (1 if group==2 else 2)
        return dict(status='speaking',viseme=(1,2,3,4,5,6)[group],aperture=aperture,
                    blink=blink_level,gesture=gesture,strength=.12+.18*math.sin(phase*math.pi/9),
                    breath=2*math.pi*phase/10)
    if i < 80:
        j=i-70
        return dict(status='listening',viseme=0,aperture=0,
                    blink={74:.45,75:1,76:.4}.get(i,0),gesture=0,strength=0,
                    breath=2*math.pi*j/10)
    if i < 90:
        j=i-80
        return dict(status='thinking',viseme=0,aperture=0,
                    blink={85:.6,86:1.,87:.4}.get(i,0),gesture=0,strength=0,
                    breath=2*math.pi*j/10)
    j=i-90
    return dict(status='gesture',viseme=(1,2,4,1,5,3,2,4,6,0)[j],
                aperture=[.15,.24,.38,.5,.65,.72,.61,.45,.23,.05][j],
                blink=0,gesture=1 if j<5 else 2,
                strength=.20*math.sin(math.pi*j/9),breath=2*math.pi*j/10)


def build(master:Image.Image, i:int)->Image.Image:
    p=params(i)
    # A single generated full-body source produces every image; crop includes
    # shoulders, waist and both fully LOWERED arms and hands.
    im=master.crop((205,0,819,921)).resize((W,H),Image.Resampling.LANCZOS)
    im=breathing(im,p['breath'])
    im=hands_low(im,p['gesture'],p['strength'])
    im=blink(im,p['blink'])
    im=mouth(im,p['viseme'],p['aperture'])
    return im


def run(args):
    master=Image.open(args.master).convert('RGB')
    assert master.size==(1024,1536), ('master dimensions',master.size)
    assets=Path(args.assets);assets.mkdir(parents=True,exist_ok=True)
    path=Path(args.zip);path.parent.mkdir(parents=True,exist_ok=True)
    atlas=Image.new('RGB',(W*COLUMNS,H*ROWS))
    record=[]
    frame_bytes=[]
    for i in range(N):
        img=build(master,i)
        atlas.paste(img,((i%COLUMNS)*W,(i//COLUMNS)*H))
        b=io.BytesIO();img.save(b,format='WEBP',lossless=True,method=4)
        raw=b.getvalue()
        frame_bytes.append(raw)
        record.append({'index':i,'file':f'frames/frame_{i:03d}.webp',
                       'state':params(i)['status'],
                       'sha256':hashlib.sha256(raw).hexdigest()})
    atlas_path=assets/'teacher2d_atlas_100.webp'
    atlas.save(atlas_path,'WEBP',lossless=True,method=4)
    # Master is FULL BODY even though current app crops to upper body.
    full_path=assets/'teacher2d_fullbody_master.webp'
    full_path.write_bytes(Path(args.master).read_bytes())
    manifest={
        'version':1,'frame_count':N,'format':'WEBP','tile_size':[W,H],
        'atlas_grid':[COLUMNS,ROWS], 'atlas_sha256':hashlib.sha256(atlas_path.read_bytes()).hexdigest(),
        'fullbody_master':'fullbody_master.webp', 'source_identity':'ONE image reused at stable eye and mouth anchors',
        'frame_groups':{'idle':[0,9],'speaking':[10,69],'listening':[70,79],
                        'thinking':[80,89],'gesture':[90,99]},
        'speech_note':'TTS word-range approximate, fallback driven only by actual audio onStart/onDone',
        'frames':record,
    }
    with zipfile.ZipFile(path,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        z.writestr('README.txt',
          'WordQuest AI Alex: 100 registered 2D animation frames.\n'
          'All frames use ONE full-body master face; face anchor never shifts.\n'
          'Only moving lips/eyelids and tiny torso/hand gestures vary.\n'
          'Android currently displays a waist-up crop. Lower legs are in master.\n'
          'Words are approximated using Android TTS callbacks, not phoneme-aligned.\n')
        z.writestr('manifest.json',json.dumps(manifest,indent=2,ensure_ascii=False))
        z.write(args.master,'fullbody_master.webp')
        for i,raw in enumerate(frame_bytes):
            z.writestr(f'frames/frame_{i:03d}.webp',raw)
    if args.preview:
        # Contact sheet shows 30 visually distinctive meaningful poses.
        indices=[0,3,4,5,6,9,10,13,15,17,20,24,25,29,
                 33,35,38,44,45,47,55,64,72,75,82,86,90,94,96,99]
        sheet=Image.new('RGB',(256*6,384*5))
        for n,i in enumerate(indices):
            sheet.paste(atlas.crop(((i%10)*W,(i//10)*H,(i%10+1)*W,(i//10+1)*H)),
                        ((n%6)*W,(n//6)*H))
        Path(args.preview).parent.mkdir(parents=True,exist_ok=True)
        sheet.save(args.preview)
    # Product acceptance checks: full-body master, zip includes EXACTLY 100
    # frames, each frame drawn from same base and face anchors pixel-stable.
    base=build(master,0)
    stable_checks=[(128,38),(112,44),(145,43),(91,98),(165,107),(125,118)]
    for i in range(N):
        fr=atlas.crop(((i%10)*W,(i//10)*H,(i%10+1)*W,(i//10+1)*H))
        assert all(fr.getpixel(point)==base.getpixel(point) for point in stable_checks),i
    with zipfile.ZipFile(path) as z:
        assert sum(name.startswith('frames/frame_') for name in z.namelist())==N
    print('PASS: 100 original WebP animation images, ONE registered face and master',
          'atlas',atlas_path.stat().st_size,'zip',path.stat().st_size,'bytes')

if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--master',required=True)
    parser.add_argument('--assets',required=True)
    parser.add_argument('--zip',required=True)
    parser.add_argument('--preview')
    run(parser.parse_args())