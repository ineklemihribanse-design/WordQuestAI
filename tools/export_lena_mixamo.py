import bpy
from pathlib import Path
from mathutils import Vector
source=Path("assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb").resolve()
output=Path("exports/Lena_Mixamo_AutoRig.fbx").resolve()
output.parent.mkdir(exist_ok=True)
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=str(source))
meshes=[o for o in bpy.context.scene.objects if o.type=="MESH"]
rigs=[o for o in bpy.context.scene.objects if o.type=="ARMATURE"]
assert meshes and rigs, "Source must contain mesh and skeleton"
# Work exclusively on an in-memory copy. Freeze the original neutral mesh
# geometry in world space before removing incompatible Prism3D armatures.
for o in bpy.context.scene.objects:
    if o.animation_data: o.animation_data_clear()
    if o.type=="ARMATURE":
        for bone in o.pose.bones: bone.matrix_basis.identity()
        o.data.pose_position="REST"
    if o.type=="MESH" and o.data.shape_keys:
        o.data.shape_keys.animation_data_clear()
        for key in o.data.shape_keys.key_blocks:
            if key != o.data.shape_keys.reference_key: key.value=0.0
bpy.context.view_layer.update()
deps=bpy.context.evaluated_depsgraph_get()
frozen=[]
for o in meshes:
    evaluated=o.evaluated_get(deps)
    geometry=bpy.data.meshes.new_from_object(evaluated,preserve_all_data_layers=True,depsgraph=deps)
    geometry.transform(evaluated.matrix_world)
    new=bpy.data.objects.new(o.name+"_neutral",geometry)
    frozen.append(new)
for o in list(bpy.context.scene.objects):
    bpy.data.objects.remove(o,do_unlink=True)
for o in frozen: bpy.context.collection.objects.link(o)
bpy.ops.object.select_all(action="DESELECT")
for o in frozen: o.select_set(True)
bpy.context.view_layer.objects.active=frozen[0]
bpy.ops.object.join()
body=bpy.context.object
body.name="Lena"
body.data.name="Lena_Neutral_Mesh"
body.vertex_groups.clear()
assert not body.parent and not body.modifiers and not body.data.shape_keys
# Center at feet, normalize to a 1.7m standing height. Uniform scale only.
lo=Vector(tuple(min(v.co[i] for v in body.data.vertices) for i in range(3)))
hi=Vector(tuple(max(v.co[i] for v in body.data.vertices) for i in range(3)))
height=hi.z-lo.z
assert height>0 and len(body.data.vertices)>100
offset=Vector(((lo.x+hi.x)/2,(lo.y+hi.y)/2,lo.z))
for v in body.data.vertices: v.co=(v.co-offset)*(1.7/height)
body.data.update()
print("Neutral mesh:",len(body.data.vertices),"vertices",len(body.data.polygons),"faces",len(body.data.materials),"materials")
assert len(bpy.context.scene.objects)==1
bpy.ops.file.pack_all()
bpy.ops.export_scene.fbx(filepath=str(output),use_selection=True,object_types={"MESH"},add_leaf_bones=False,bake_anim=False,path_mode="COPY",embed_textures=True)
assert output.stat().st_size>10000 and output.read_bytes()[:18]==b"Kaydara FBX Binary"
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.fbx(filepath=str(output))
assert not any(o.type=="ARMATURE" for o in bpy.context.scene.objects)
assert len([o for o in bpy.context.scene.objects if o.type=="MESH"])==1
print("Verified skeleton-free FBX:",output,output.stat().st_size,"bytes")
