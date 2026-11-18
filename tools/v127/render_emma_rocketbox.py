#!/usr/bin/env python3
"""Blender renders of a genuine rigged adult Emma candidate.

Four camera views: full front/rear, face close-up and 3/4 profile.
These *are candidate previews*, not app screenshots or release approval.
"""
from pathlib import Path
import os
import math
import json
import bpy
from mathutils import Vector
from bpy_extras.object_utils import world_to_camera_view

ROOT=Path(__file__).resolve().parents[2]
MODEL=Path(os.environ.get("WQ_EMMA_MODEL",str(ROOT/"candidate-emma-rocketbox/emma.glb")))
OUT=Path(os.environ.get("WQ_EMMA_OUTPUT",str(ROOT/"candidate-emma-rocketbox/previews")))
LABEL=os.environ.get("WQ_EMMA_LABEL","emma")
OUT.mkdir(parents=True,exist_ok=True)

bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)
bpy.ops.import_scene.gltf(filepath=str(MODEL))
sc=bpy.context.scene
meshes=[obj for obj in sc.objects if obj.type=="MESH"]
armatures=[obj for obj in sc.objects if obj.type=="ARMATURE"]
assert len(meshes)>0 and len(armatures)>0,"Missing rigged figure"
# The source GLB contains four mutually exclusive clothing configurations;
# the Avatar Studio viewer hides unused outfits. Blender does not honor that
# custom viewer logic, causing overlapping black/white/pink fragments.
# Incoming candidate GLB has already been normalized to its selected outfit.
# Never hide parts based on an outdated hard-coded 'original outfit' rule:
# doing so caused the white-blouse preview to display only a floating head.
for obj in meshes:
    chain=[]
    ancestor=obj
    while ancestor is not None:
        chain.append(ancestor.name)
        ancestor=ancestor.parent
    for mat in obj.data.materials:
        if mat is None:continue
        family=" ".join(chain+[mat.name]).lower()
        is_strand=any(n in family for n in ("hair","eyelash","eyebrow"))
        try:
            mat.blend_method="CLIP" if is_strand else "OPAQUE"
            mat.alpha_threshold=.38 if is_strand else .5
        except AttributeError:
            pass
        if not is_strand and mat.use_nodes:
            pbr=next((n for n in mat.node_tree.nodes if n.type=="BSDF_PRINCIPLED"),None)
            if pbr:
                if "Alpha" in pbr.inputs:
                    pbr.inputs["Alpha"].default_value=1.0
                if "Roughness" in pbr.inputs:
                    pbr.inputs["Roughness"].default_value=.8
print("EMMA_PREVIEW active mesh names", [o.name for o in meshes][:55])
assert len(meshes)>0
action_label=os.environ.get("WQ_EMMA_ACTION","idle").lower()
selected_actions=[a for a in bpy.data.actions if action_label in a.name.lower()]
print("EMMA_PREVIEW actions", [a.name for a in bpy.data.actions][:35])
assert selected_actions, ("Missing requested actual GLB action",action_label)
for rig in armatures:
    rig.animation_data_create()
    rig.animation_data.action=selected_actions[0]
sc.frame_set(int(os.environ.get("WQ_EMMA_FRAME","9")))
shape_values=json.loads(os.environ.get("WQ_EMMA_MORPHS","{}"))
if shape_values:
    changed=set()
    for mesh in meshes:
        keys=mesh.data.shape_keys
        if not keys:continue
        for target,value in shape_values.items():
            if target in keys.key_blocks:
                keys.key_blocks[target].value=float(value)
                changed.add(target)
    assert set(shape_values)==changed,("Missing ARKit face keys",set(shape_values)-changed)
    print("ARKit FACE ACTION",action_label,"morphs",shape_values)
bpy.context.view_layer.update()

points=[]
for obj in meshes:
    for v in obj.bound_box:points.append(obj.matrix_world @ Vector(v))
bottom=Vector((min(p.x for p in points),min(p.y for p in points),min(p.z for p in points)))
top=Vector((max(p.x for p in points),max(p.y for p in points),max(p.z for p in points)))
center=(bottom+top)/2
height=top.z-bottom.z
width=max(top.x-bottom.x,top.y-bottom.y)
assert 0.6<height<5, f"Unexpected height {height:.3f}"
print("Emma measurements",height,width,"midpoint",tuple(center))

sc.render.engine="CYCLES"
sc.cycles.samples=int(os.environ.get('WQ_EMMA_SAMPLES','16'))
sc.render.resolution_x=620
sc.render.resolution_y=900
sc.render.resolution_percentage=100
sc.render.image_settings.file_format="PNG"
sc.render.film_transparent=False
sc.world.color=(0.15,0.20,0.22)
sc.view_settings.view_transform="Standard"
sc.view_settings.look="Medium High Contrast"
sc.view_settings.exposure=-0.5
sc.view_settings.view_transform="Standard"

def create_light(name, location, power, size, color):
    light=bpy.data.lights.new(name,"AREA")
    light.energy=power
    light.shape="DISK";light.size=size;light.color=color
    ob=bpy.data.objects.new(name,light)
    sc.collection.objects.link(ob)
    ob.location=center+Vector(location)
    ob.rotation_euler=(center+Vector((0,0,height*.11))-ob.location).to_track_quat("-Z","Y").to_euler()

create_light("Warm softbox",(-2.4,-3.5,3.7),260,3.5,(1.0,.87,.78))
create_light("Cool fill",(2.0,-1.7,2.8),160,3.7,(.81,.90,1.0))
create_light("Rim",(1.3,2.6,3.0),220,2.0,(.89,1.0,1.0))

mat=bpy.data.materials.new("Studio floor")
mat.diffuse_color=(.10,.19,.21,1)
mat.use_nodes=True
floor_shader=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED')
floor_shader.inputs['Base Color'].default_value=(.10,.19,.21,1)
floor_shader.inputs['Roughness'].default_value=1.0
shoe_points=[]
for ob in meshes:
    if "shoe" in ob.name.lower() or any("shoe" in p.name.lower() for p in [ob.parent] if p):
        ob_eval=ob.evaluated_get(bpy.context.evaluated_depsgraph_get())
        geo=ob_eval.to_mesh()
        if geo:
            shoe_points.extend(ob.matrix_world @ vertex.co for vertex in geo.vertices)
            ob_eval.to_mesh_clear()
floor_height=(min(v.z for v in shoe_points) if shoe_points else bottom.z)-.018
print("FLOOR_FROM_SHOE_VERTICES",len(shoe_points),"z",round(floor_height,4))
bpy.ops.mesh.primitive_plane_add(size=7,location=(0,0,floor_height))
floor=bpy.context.object
floor.data.materials.append(mat)

bpy.ops.object.camera_add()
camera=bpy.context.object
sc.camera=camera
camera.data.type="ORTHO"


# Use skinned, *evaluated* mesh vertices for camera placement. Raw object
# bound_box can include unused authoring rig extents and produce tiny humans.
evaluated=bpy.context.evaluated_depsgraph_get()
visible_points=[]
for obj in meshes:
    copy=obj.evaluated_get(evaluated)
    geo=copy.to_mesh()
    if geo:
        step=max(1,len(geo.vertices)//15000)
        visible_points.extend((obj.matrix_world @ vertex.co)
                              for vertex in list(geo.vertices)[::step])
        copy.to_mesh_clear()
assert visible_points,"No drawable geometry after outfit filtering"
zlo=min(point.z for point in visible_points)
zhi=max(point.z for point in visible_points)
head_points=[p for p in visible_points if p.z>=zlo+.78*(zhi-zlo)]
assert len(head_points)>50,"Not enough head vertices for face camera"

def make_photo(label,angle,distance,look_z,framing):
    z=bottom.z+height*look_z
    target=Vector((center.x,center.y,z))
    camera.location=target+Vector((math.sin(angle)*distance, math.cos(angle)*distance, height*.09))
    camera.rotation_euler=(target-camera.location).to_track_quat("-Z","Y").to_euler()
    camera.data.ortho_scale=height*framing
    bpy.context.view_layer.update()
    subjects=head_points if label=="face" else visible_points

    def projected():
        return [world_to_camera_view(sc,camera,p) for p in subjects]

    coords=projected()
    lowx=min(v.x for v in coords);highx=max(v.x for v in coords)
    lowy=min(v.y for v in coords);highy=max(v.y for v in coords)
    bound_h=max(1e-5,highy-lowy)
    bound_w=max(1e-5,highx-lowx)
    desired=.77 if label=="face" else .84
    # Perspective-safe independent width/height fit for orthographic preview.
    factor=max(bound_h/desired, bound_w/.82)
    camera.data.ortho_scale*=factor
    bpy.context.view_layer.update()
    coords=projected()
    xmid=(min(v.x for v in coords)+max(v.x for v in coords))*.5
    ymid=(min(v.y for v in coords)+max(v.y for v in coords))*.5
    right=camera.rotation_euler.to_quaternion() @ Vector((1,0,0))
    up=camera.rotation_euler.to_quaternion() @ Vector((0,1,0))
    aspect=sc.render.resolution_x/sc.render.resolution_y
    camera.location+=right*((xmid-.5)*camera.data.ortho_scale*aspect)
    camera.location+=up*((ymid-.5)*camera.data.ortho_scale)
    # Raw GLB silhouette extents include hair cards and invisible authoring
    # triangles, which previously left the actual character occupying <50%
    # of the preview. Tighten to the painted visible shape and preserve room
    # for hands/feet. Full-body and face presets have different safe crops.
    camera.data.ortho_scale *= (.75 if label != "face" else .66)
    # Geometry bounds include long transparent hair cards whose center is not
    # the actual painted face/body center. Move the camera UP, so the visible
    # teacher (currently too high and clipped) lands inside the viewport.
    camera.location += up * camera.data.ortho_scale * (.18 if label != "face" else .13)
    bpy.context.view_layer.update()
    sc.render.filepath=str(OUT/(LABEL+"-"+label+".png"))
    bpy.ops.render.render(write_still=True)
    print("AUTO_FRAMED",LABEL,label,"z",round(z,3),"ortho",round(camera.data.ortho_scale,3))

viewsets={
    "full-front":(math.pi,max(2.8,height*2.1),.50,1.09),
    "full-back":(0,max(2.8,height*2.1),.50,1.09),
    "face":(math.pi,max(1.7,height*1.6),.85,.50),
    "profile":(math.pi*1.28,max(2.2,height*1.9),.63,.91),
}
requested=os.environ.get("WQ_EMMA_VIEWS","full-front,face,profile,full-back").split(",")
for label in requested:
    if label not in viewsets:raise ValueError("Unknown preview view: "+label)
    make_photo(label,*viewsets[label])
print("Preview completed:",",".join(requested),"for",LABEL,"VISUAL APPROVAL PENDING")
