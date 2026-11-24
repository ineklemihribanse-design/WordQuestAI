import bpy
from pathlib import Path
source=Path("assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb").resolve()
output=Path("exports/Lena_Mixamo.fbx").resolve()
output.parent.mkdir(exist_ok=True)
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=str(source))
meshes=[o for o in bpy.context.scene.objects if o.type=="MESH"]
rigs=[o for o in bpy.context.scene.objects if o.type=="ARMATURE"]
assert meshes and rigs, "Source must contain mesh and skeleton"
print("Source mesh count",len(meshes),"armatures",len(rigs),"bones",sum(len(o.data.bones) for o in rigs))
for o in bpy.context.scene.objects:
    if o.animation_data: o.animation_data_clear()
    if o.type=="ARMATURE":
        for bone in o.pose.bones: bone.matrix_basis.identity()
        o.data.pose_position="REST"
bpy.ops.file.pack_all()
bpy.ops.export_scene.fbx(filepath=str(output),object_types={"ARMATURE","MESH","EMPTY"},add_leaf_bones=False,bake_anim=False,path_mode="COPY",embed_textures=True)
assert output.stat().st_size>10000
assert output.read_bytes()[:18]==b"Kaydara FBX Binary"
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.fbx(filepath=str(output))
assert any(o.type=="MESH" for o in bpy.context.scene.objects)
assert any(o.type=="ARMATURE" for o in bpy.context.scene.objects)
print("Verified FBX reimport:",output,output.stat().st_size,"bytes")
