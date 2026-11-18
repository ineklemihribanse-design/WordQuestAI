#!/usr/bin/env python3
"""Blender CPU quality previews for 1.0.27 candidate human avatars.

The previews are CI artifacts, NOT a substitute for on-device visual QA.
Produces front and rear full-body PNGs to expose T-pose, silhouette, face
quality, incorrectly stripped hair/transparency and texture problems.
Run: blender -b -t 2 --python tools/v127/render_candidate_previews.py
"""
from pathlib import Path
import bpy
import os
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[2]
ASSETS=Path(os.environ.get("WQ_PREVIEW_ASSETS",str(ROOT/"source/app/src/main/assets/avatars")))
OUT=Path(os.environ.get("WQ_PREVIEW_OUTPUT",str(ROOT/"candidate-previews")))
OUT.mkdir(exist_ok=True)
scene=bpy.context.scene
scene.render.engine="CYCLES"
scene.cycles.samples=12
scene.render.resolution_x=480
scene.render.resolution_y=720
scene.render.resolution_percentage=100
scene.view_settings.view_transform="Standard"
scene.render.image_settings.file_format="PNG"
scene.render.film_transparent=False
scene.world.color=(0.08,0.15,0.18)

def clean():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)

def render_model(name,path):
    clean()
    bpy.ops.import_scene.gltf(filepath=str(path))
    meshes=[obj for obj in bpy.context.scene.objects if obj.type=="MESH"]
    if not meshes:raise RuntimeError(name+": Blender imported zero meshes")
    arms=[obj for obj in bpy.context.scene.objects if obj.type=="ARMATURE"]
    actions=[a for a in bpy.data.actions if "Teacher_Idle" in a.name]
    if arms and actions:
        for arm in arms:
            arm.animation_data_create()
            arm.animation_data.action=actions[0]
    scene.frame_set(6)
    coords=[]
    for obj in meshes:
        for v in obj.bound_box:
            coords.append(obj.matrix_world @ Vector(v))
    low=Vector((min(v.x for v in coords),min(v.y for v in coords),min(v.z for v in coords)))
    high=Vector((max(v.x for v in coords),max(v.y for v in coords),max(v.z for v in coords)))
    center=(low+high)/2
    height=max(1.0,high.z-low.z)
    extent=max(high.x-low.x,high.y-low.y)
    print(f"MODEL_PREVIEW {name}: mesh_count={len(meshes)} height={height:.3f} width={extent:.3f}")
    material=bpy.data.materials.new(name="PreviewFloor")
    material.diffuse_color=(.17,.23,.28,1)
    bpy.ops.mesh.primitive_plane_add(size=200,location=(0,0,low.z-.04))
    floor=bpy.context.object
    floor.data.materials.append(material)
    for pos,strength,size in [((-3,-4,6),800,4),((3,2,4),500,3)]:
        light=bpy.data.lights.new("Softbox","AREA")
        light.energy=strength;light.shape="DISK";light.size=size
        obj=bpy.data.objects.new("Softbox",light)
        bpy.context.collection.objects.link(obj)
        obj.location=center+Vector(pos)
        direction=center+Vector((0,0,.25))-obj.location
        obj.rotation_euler=direction.to_track_quat("-Z","Y").to_euler()
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    scene.camera=camera
    camera.data.type="ORTHO"
    camera.data.ortho_scale=max(height*1.20,extent*1.30)
    for direction,label in ((-1,"front"),(1,"reverse")):
        camera.location=center+Vector((0,direction*max(height*1.7,3),height*.20))
        direction_vector=center-camera.location
        camera.rotation_euler=direction_vector.to_track_quat("-Z","Y").to_euler()
        scene.render.filepath=str(OUT/(name+"-"+label+".png"))
        bpy.ops.render.render(write_still=True)

for name in os.environ.get("WQ_PREVIEW_NAMES","emma,sophia,ethan").split(","):
    render_model(name,ASSETS/(name+".glb"))
print("Rendered visual QA previews in",OUT,"; VISUAL APPROVAL STILL REQUIRED")
