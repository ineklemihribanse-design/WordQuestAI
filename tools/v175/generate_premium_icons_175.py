#!/usr/bin/env python3
"""Create 19 original anti-aliased high-DPI glossy game icon assets.

No downloaded icons, fonts or emoji. Render at 3x, downsample Lanczos and
bundle 256px transparent WebP icons with the Android application.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
import math

ROOT=Path(__file__).resolve().parents[2]
DEST=ROOT/"source/app/src/main/assets/wq175_icons"
DEST.mkdir(parents=True,exist_ok=True)
S=3
N=256*S
PURPLE="#7744CE"; PURPLE_LIGHT="#C6A2FF"
GOLD="#FFC342"; GOLD_EDGE="#CA8318"
TEAL="#37C6C2"; BLUE="#418DDD"; CORAL="#F17675"
GREEN="#2D9C78"; PINK="#E685BA"

def box(a):
    return tuple(int(round(v*S)) for v in a)
def line(dr,coords,color,width=6):
    dr.line([(int(x*S),int(y*S)) for x,y in coords],fill=color,width=int(width*S),joint="curve")
def rr(dr,a,r,fill,outline=None,width=3):
    dr.rounded_rectangle(box(a),radius=int(r*S),fill=fill,outline=outline,width=int(width*S))
def ell(dr,a,fill,outline=None,width=3):
    dr.ellipse(box(a),fill=fill,outline=outline,width=int(width*S))
def poly(dr,coords,fill,outline=None,width=3):
    points=[(int(x*S),int(y*S)) for x,y in coords]
    dr.polygon(points,fill=fill)
    if outline:dr.line(points+[points[0]],fill=outline,width=int(width*S),joint="curve")

def glow_background(base):
    rim=Image.new("RGBA",(N,N));d=ImageDraw.Draw(rim)
    ell(d,(36,35,220,219),(179,120,255,48))
    return Image.alpha_composite(base,rim.filter(ImageFilter.GaussianBlur(21*S)))

def sword_layer(angle):
    a=Image.new("RGBA",(N,N)); d=ImageDraw.Draw(a)
    # Vertical polished steel blade with rim, pommel and gold crossguard.
    poly(d,[(121,26),(135,26),(143,124),(128,151),(113,124)],
         "#EAF2FF","#6476A4",3)
    poly(d,[(126,31),(135,32),(134,120),(128,141),(125,124)],
         "#FFFFFF")
    rr(d,(119,144,137,221),7,PURPLE,GOLD_EDGE,4)
    rr(d,(88,140,168,159),9,GOLD,GOLD_EDGE,4)
    ell(d,(120,208,136,224),GOLD,"#C08321",3)
    ell(d,(122,145,134,158),"#F6E7FF",PURPLE,2)
    return a.rotate(angle,Image.Resampling.BICUBIC,expand=False,center=(N/2,N/2))

def icon(name):
    im=Image.new("RGBA",(N,N))
    im=glow_background(im)
    d=ImageDraw.Draw(im)
    if name=="arena_duel":
        rr(d,(43,42,212,210),41,"#5732A6",GOLD,7)
        im=Image.alpha_composite(im,sword_layer(44))
        im=Image.alpha_composite(im,sword_layer(-44))
        d=ImageDraw.Draw(im)
        ell(d,(107,105,149,147),GOLD,GOLD_EDGE,5)
        ell(d,(119,114,137,132),"#FFEFAC")
    elif name=="arena_team":
        rr(d,(31,73,149,196),21,"#9F69E9","#572D9C",6)
        ell(d,(75,48,116,92),"#9F69E9","#572D9C",5)
        ell(d,(139,111,180,155),"#9F69E9","#572D9C",5)
        rr(d,(113,62,224,180),20,GOLD,GOLD_EDGE,5)
        ell(d,(93,108,141,152),GOLD,GOLD_EDGE,4)
        ell(d,(153,37,196,80),GOLD,GOLD_EDGE,4)
        line(d,[(121,71),(181,71)],"#FFEAB1",5)
        line(d,[(35,82),(93,82)],"#DBB9FF",5)
    elif name=="arena_quiz":
        poly(d,[(139,17),(67,135),(115,133),(92,238),(191,103),(142,108),(175,17)],
             GOLD,GOLD_EDGE,7)
        poly(d,[(145,26),(87,119),(129,117),(110,206),(178,97),(134,98),(162,26)],
             "#FFF3B2")
        line(d,[(45,81),(69,76)],"#C584FF",7)
        line(d,[(191,193),(215,178)],"#C584FF",7)
    elif name=="trophy":
        rr(d,(65,45,192,148),28,GOLD,GOLD_EDGE,7)
        d.arc(box((34,57,103,141)),75,285,fill=GOLD_EDGE,width=10*S)
        d.arc(box((154,57,222,141)),255,465,fill=GOLD_EDGE,width=10*S)
        rr(d,(118,143,140,203),6,GOLD,GOLD_EDGE,4)
        rr(d,(82,198,174,223),12,GOLD,GOLD_EDGE,4)
        poly(d,[(127,68),(136,93),(162,94),(142,111),
                (148,135),(126,121),(107,136),(113,110),
                (94,94),(119,91)],"#FFF2B5")
    elif name=="listen":
        d.arc(box((59,35,198,205)),180,355,fill="#227F91",width=20*S)
        rr(d,(34,103,81,188),23,TEAL,"#0E6879",6)
        rr(d,(177,103,223,188),23,TEAL,"#0E6879",6)
        rr(d,(46,118,68,177),11,"#A2FFF0")
        rr(d,(188,118,212,177),11,"#A2FFF0")
        for x,he in ((103,35),(127,68),(151,42)):
            rr(d,(x-7,157-he/2,x+7,157+he/2),7,CORAL,"#B33F58",2)
    elif name=="sentence":
        for x in (30,96,162):
            rr(d,(x,94,x+64,162),16,"#DDF8F0",GREEN,5)
            rr(d,(x+9,104,x+55,113),4,"#FFFFFF")
        line(d,[(49,132),(75,132)],GREEN,6)
        line(d,[(115,132),(142,132)],GREEN,6)
        line(d,[(181,132),(207,132)],GREEN,6)
        poly(d,[(107,74),(134,49),(160,74)],GOLD,GOLD_EDGE,4)
    elif name=="grammar":
        rr(d,(44,33,193,213),19,"#C9B2FD","#7045B6",6)
        rr(d,(61,50,179,197),14,"#FAF6FF")
        for y in (86,105,124,144):
            line(d,[(82,y),(162,y)],PURPLE_LIGHT,5)
        ell(d,(129,122,213,206),(240,251,255,180),GOLD_EDGE,10)
        line(d,[(194,190),(227,222)],PURPLE,17)
        ell(d,(142,133,198,189),None,"#4A339D",5)
    elif name=="story":
        poly(d,[(32,60),(122,68),(127,212),(37,200)],"#FFFBF1",GOLD_EDGE,5)
        poly(d,[(130,68),(222,57),(218,200),(127,212)],"#FFF0C2",GOLD_EDGE,5)
        line(d,[(128,75),(128,212)],PURPLE,5)
        for y in (104,128,152):
            line(d,[(52,y),(106,y)],"#B2A3DA",4)
            line(d,[(149,y),(202,y)],"#C9AE7B",4)
        poly(d,[(170,75),(175,87),(188,89),(178,98),(180,112),(169,105),
            (158,112),(161,98),(151,89),(165,87)],GOLD)
    elif name=="speak":
        rr(d,(83,32,174,159),46,PINK,"#A94882",7)
        for x in (104,122,140):
            line(d,[(x,60),(x,133)],"#F7C7DE",5)
        d.arc(box((59,100,197,204)),0,180,fill="#AD4984",width=10*S)
        line(d,[(128,200),(128,227)],"#AF4A89",9)
        rr(d,(89,219,169,231),6,PINK)
    elif name=="puzzle":
        rr(d,(48,44,207,211),23,"#205E51","#0B4838",7)
        for x,y in ((77,88),(117,88),(157,88),(117,128),(117,168)):
            rr(d,(x,y,x+35,y+35),4,"#FFF8EE",GOLD_EDGE,2)
        rr(d,(117,128,152,163),4,"#E5F38E","#9FB348",2)
    elif name=="yds":
        poly(d,[(24,106),(129,47),(234,106),(129,169)],"#234A89","#0C2F5F",6)
        poly(d,[(72,135),(72,191),(181,191),(181,134)],"#294E93",GOLD_EDGE,6)
        line(d,[(226,105),(226,188)],GOLD,7)
        ell(d,(216,181,237,206),GOLD,GOLD_EDGE,3)
        ell(d,(105,91,149,135),GOLD,GOLD_EDGE,4)
    elif name=="cards":
        rr(d,(31,65,157,213),21,"#B5E1FA",BLUE,5)
        rr(d,(68,48,190,199),19,"#A998EE",PURPLE,5)
        rr(d,(104,31,225,182),20,"#FCFAFF","#6D84C4",6)
        for y in (84,106,128):
            line(d,[(127,y),(198,y)],BLUE,5)
        ell(d,(147,145,175,173),GOLD,GOLD_EDGE,2)
    elif name=="plan":
        rr(d,(34,48,223,222),22,"#F7FFEA","#2B8864",7)
        rr(d,(34,48,223,96),19,GREEN,"#2B8864",5)
        for x in (83,173):
            rr(d,(x-10,30,x+10,69),9,GOLD,GOLD_EDGE,3)
        for x in (62,112,162):
            for y in (119,163):
                rr(d,(x,y,x+32,y+32),6,"#D7F0DD")
        poly(d,[(138,164),(155,183),(195,140),(207,150),(156,200),(127,177)],
             "#39B879")
    elif name=="camera":
        rr(d,(29,80,227,195),25,"#FFAD5E","#C66E2A",6)
        rr(d,(72,52,125,88),12,"#FFE9A8","#C66E2A",5)
        ell(d,(89,98,168,178),"#324D98",GOLD_EDGE,9)
        ell(d,(110,116,148,157),"#76C4FF","#273876",5)
        ell(d,(187,98,205,116),GOLD)
    elif name=="assistant":
        rr(d,(39,63,218,191),48,"#E3DBFF",PURPLE,6)
        rr(d,(56,78,204,166),36,"#283161","#4B4DBE",5)
        d.arc(box((79,107,128,140)),180,360,fill="#66F5F0",width=7*S)
        d.arc(box((134,107,185,140)),180,360,fill="#66F5F0",width=7*S)
        d.arc(box((110,121,154,152)),10,170,fill="#FF98D1",width=5*S)
        line(d,[(127,62),(127,40)],GOLD,7)
        ell(d,(116,26,138,48),GOLD,GOLD_EDGE,3)
    elif name=="mistakes":
        # Original revision-puzzle icon: two layered tiles, search lens,
        # corrective check and lively amber glints.
        rr(d,(41,70,190,205),27,"#CDB2FB","#6B40B7",6)
        ell(d,(82,49,131,92),"#CDB2FB","#6B40B7",5)
        rr(d,(117,88,226,192),23,"#FFD367","#D18C25",6)
        ell(d,(104,120,148,163),"#FFD367","#D18C25",4)
        line(d,[(157,140),(173,157),(208,115)],"#2C9569",12)
        line(d,[(50,191),(63,176)],"#FFF2A5",7)
        ell(d,(190,45,208,63),"#FFE89D")
        ell(d,(209,67,223,81),"#FFB44E")
    elif name=="ai_teacher":
        # Friendly original violet teaching bot with distinct open-book
        # companion silhouette, not a duplicate of the chat icon.
        rr(d,(38,63,220,185),47,"#A786EF","#673AB3",7)
        rr(d,(55,80,203,153),28,"#253265","#6458B5",5)
        ell(d,(78,108,100,131),"#55F3DF")
        ell(d,(156,108,178,131),"#55F3DF")
        d.arc(box((107,108,148,146)),12,164,fill="#FF9DCC",width=5*S)
        line(d,[(129,63),(129,38)],GOLD,7)
        ell(d,(115,24,143,49),GOLD,GOLD_EDGE,4)
        poly(d,[(55,169),(127,181),(128,236),(54,223)],"#EFFFFF","#7352BB",5)
        poly(d,[(128,181),(202,169),(202,223),(128,236)],"#FFF0BD","#7352BB",5)
        line(d,[(128,183),(128,233)],PURPLE,5)
        ell(d,(40,42,53,55),"#F8DE7C")
        ell(d,(204,46,219,61),"#F8DE7C")
    elif name=="packages":
        for x,y,c in ((32,73,"#B1C8FF"),(73,59,"#C9A6FF"),(108,46,"#7DDDD0")):
            rr(d,(x,y,x+110,y+154),17,c,PURPLE,5)
            line(d,[(x+22,y+40),(x+88,y+40)],"#FFFFFF",6)
            line(d,[(x+22,y+59),(x+80,y+59)],"#FFFFFF",4)
        poly(d,[(173,178),(188,163),(201,177),(216,161),(212,201),(177,207)],
             GOLD,GOLD_EDGE,3)
    elif name=="verbs":
        rr(d,(28,59,110,176),15,"#C7F4E7",GREEN,5)
        rr(d,(146,59,228,176),15,"#C3D8FF",BLUE,5)
        line(d,[(90,207),(170,207)],GOLD,8)
        poly(d,[(167,190),(188,207),(167,225)],GOLD,GOLD_EDGE,3)
        line(d,[(79,86),(79,147)],GREEN,7)
        line(d,[(185,86),(185,147)],BLUE,7)
    else:
        rr(d,(45,42,207,209),30,"#E4ECFF",BLUE,6)
    # Tiny polished reflection toward top-left, subtle depth.
    glint=Image.new("RGBA",(N,N));g=ImageDraw.Draw(glint)
    ell(g,(41,32,112,58),(255,255,255,35))
    im=Image.alpha_composite(im,glint.filter(ImageFilter.GaussianBlur(6*S)))
    im=im.resize((256,256),Image.Resampling.LANCZOS)
    return im

names=("arena_duel","arena_team","arena_quiz","trophy","listen","sentence",
       "grammar","story","speak","puzzle","yds","cards","plan",
       "camera","assistant","packages","verbs","mistakes","ai_teacher")
for name in names:
    path=DEST/(name+".webp")
    icon(name).save(path,format="WEBP",quality=92,method=4)
    assert path.stat().st_size>800, path
    print(name,path.stat().st_size)
print("Generated",len(names),"original high-DPI premium icons.")
