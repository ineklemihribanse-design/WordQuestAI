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
MOUTH_X, MOUTH_Y = 128, 85
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
    """Real eyelid closure synthesized from SAME face pixels, same landmarks."""
    if amount <= 0.01:
        return image
    out = image.copy().convert('RGBA')
    for left,top,right,bottom in EYE_BOXES:
        w=right-left; h=bottom-top
        # Clone the skin immediately below each eye as its own eyelid, rather
        # than substituting another independently generated person's eyes.
        patch=image.crop((left,top+11,right,top+11+h)).resize((w,h), Image.Resampling.BICUBIC)
        overlay=Image.new('RGBA',(w,h),(0,0,0,0))
        overlay.paste(patch,(0,0))
        alpha=Image.new('L',(w,h),0);a=ImageDraw.Draw(alpha)
        a.ellipse((1,1,w-2,h-1),fill=round(255*amount))
        alpha=alpha.filter(ImageFilter.GaussianBlur(1.7))
        overlay.putalpha(alpha)
        out.alpha_composite(overlay,(left,top))
        if amount > .62:
            # Thin lid crease only when the eyes actually CLOSE, never drawn
            # arbitrarily on top of an OPEN eye.
            lid=Image.new('RGBA',(w,h),(0,0,0,0))
            ld=ImageDraw.Draw(lid)
            ld.arc((3,3,w-3,h+2),start=192,end=346,fill=(112,70,59,round(85*amount)),width=1)
            out.alpha_composite(lid,(left,top))
    return out.convert('RGB')


def mouth(image: Image.Image, viseme: int, amount: float) -> Image.Image:
    """Subpixel jaw/lip opening without changing the portrait's face anchor.

    The original jaw/skin texture is kept at every frame. Only the inside of
    the person's actual lips changes; a fixed 34×19 feathered mask prevents
    cheek/nose drift or pasted rectangle seams.
    """
    if amount < .035 or viseme == 0 or viseme == 6:
        return image
    width,height = {1:(22,8),2:(28,4),3:(20,3.8),4:(14,8),5:(12,6)}.get(viseme,(18,5))
    height=max(1.0,height*amount)
    width=width*(.65+.35*amount)
    # Local supersampled lip patch. Feathered alpha stays within lips only.
    upscale=4;px=42;py=24
    overlay=Image.new('RGBA',(px*upscale,py*upscale),(0,0,0,0))
    d=ImageDraw.Draw(overlay)
    cx=(px/2)*upscale; cy=(py/2-1)*upscale
    halfw=width/2*upscale; halfh=height/2*upscale
    # Outer upper/lower lip follows existing neutral lip, not a face photo.
    lip=(165,92,89,115)
    d.ellipse((cx-halfw-1.3*upscale,cy-halfh-1*upscale,
               cx+halfw+1.3*upscale,cy+halfh+1*upscale),fill=lip)
    d.ellipse((cx-halfw*.72,cy-halfh*.67,cx+halfw*.72,cy+halfh*.67),fill=(75,34,43,212))
    if viseme in (2,3):
        # Teeth are visible only for spread vowels /E/ and /I/.
        teeth_h=max(1,round(height*.23*upscale))
        d.rounded_rectangle((cx-halfw*.51,cy-halfh*.50,
                             cx+halfw*.51,cy-halfh*.50+teeth_h),
                            radius=2*upscale,fill=(246,228,214,140))
    # Inner highlight catches light on lower lip at 20–35% aperture.
    d.arc((cx-halfw,cy-halfh,cx+halfw,cy+halfh+1.5*upscale),0,175,
          fill=(197,112,110,110),width=upscale)
    patch=overlay.resize((px,py),Image.Resampling.LANCZOS)
    # Restrict the actual patch to a soft oval; no 133px pasted facial strips.
    a=patch.getchannel('A')
    feather=Image.new('L',(px,py),0);fd=ImageDraw.Draw(feather)
    fd.ellipse((3,3,px-4,py-3),fill=255)
    feather=feather.filter(ImageFilter.GaussianBlur(1.5))
    from PIL import ImageChops
    patch.putalpha(ImageChops.multiply(a,feather))
    out=image.convert('RGBA')
    out.alpha_composite(patch,(MOUTH_X-px//2,MOUTH_Y-py//2))
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