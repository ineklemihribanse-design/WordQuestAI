#!/usr/bin/env python3
"""Audit the actual assembled app, never publish or expose signing material.
Every file gets a hash. Text sources get structural checks and review markers.
A marker is a review candidate, not a confirmed bug. Lint runs separately."""
from pathlib import Path
import hashlib,json,re,sys,zipfile,xml.etree.ElementTree as ET
from collections import Counter
root=Path(sys.argv[1]).resolve()
out=Path(sys.argv[2]).resolve()
out.mkdir(parents=True,exist_ok=True)
app=root/"app"
src=app/"src"
assert src.is_dir()
inventory=[];markers=[];errors=[];stats=Counter();definitions={}
patterns={
 "force_unwrap": r"(?<![=!])!!(?![=])",
 "blocking_coroutine": r"\brunBlocking\s*[\({]",
 "global_coroutine": r"\bGlobalScope\b",
 "webview_execution": r"\b(?:evaluateJavascript|addJavascriptInterface|loadDataWithBaseURL)\s*\(",
 "firebase_mutation": r"\.(?:setValue|updateChildren|runTransaction|removeValue)\s*\(",
 "persistent_data_clear": r"\b(?:clearApplicationUserData|deleteDatabase|deleteSharedPreferences)\s*\(",
 "unchecked_json": r"\.(?:getJSONObject|getJSONArray|getString|getInt)\s*\(",
 "empty_catch": r"catch\s*\([^)]*\)\s*\{\s*\}",
 "layout_scroll": r"\.(?:verticalScroll|horizontalScroll)\s*\(",
 "fixed_screen_height": r"\bscreenHeightDp\b",
 "native_library": r"\b(?:System\.loadLibrary|external fun)\b",
 "network_cleartext": r"http://(?!schemas\.android\.com|www\.w3\.org)",
}
text_ext={".kt",".java",".kts",".xml",".json",".properties",".pro",".gradle",".js",".html",".css",".py",".txt",".md"}
code_ext={".kt",".java",".kts",".xml",".pro",".gradle",".js",".html",".css"}
files=sorted(f for f in src.rglob("*") if f.is_file())
files+=sorted(f for f in (root,app) for f in f.iterdir() if f.is_file() and f.suffix in {".gradle",".kts",".pro"} and f.name!="local.properties")
with zipfile.ZipFile(out/"assembled-code.zip","w",zipfile.ZIP_DEFLATED) as z:
    for f in files:
        rel=f.relative_to(root).as_posix()
        raw=f.read_bytes()
        inventory.append({"path":rel,"bytes":len(raw),"sha256":hashlib.sha256(raw).hexdigest()})
        stats[f.suffix.lower() or "(none)"]+=1
        if f.suffix.lower() not in text_ext: continue
        try:s=raw.decode("utf-8-sig")
        except UnicodeDecodeError:
            errors.append({"file":rel,"kind":"invalid_utf8"});continue
        if f.suffix.lower()==".json":
            try:json.loads(s)
            except ValueError as e:errors.append({"file":rel,"kind":"invalid_json","message":str(e)})
        if f.suffix.lower()==".xml":
            try:ET.fromstring(s)
            except ET.ParseError as e:errors.append({"file":rel,"kind":"invalid_xml","message":str(e)})
        if f.suffix.lower() in code_ext:
            # Exclude credential/config files and test fixtures containing secrets.
            if not re.search(r"(?i)(?:google-services|secrets?|keystore|credentials|signing)",f.name):
                z.writestr(rel,raw)
        if f.suffix.lower() in {".kt",".java",".kts"}:
            stats["source_lines"]+=len(s.splitlines())
            for i,line in enumerate(s.splitlines(),1):
                for key,pattern in patterns.items():
                    if re.search(pattern,line):
                        markers.append({"file":rel,"line":i,"kind":key})
    z.writestr("audit-source-sha.json",json.dumps(inventory,ensure_ascii=False,indent=2))
(out/"inventory.json").write_text(json.dumps(inventory,ensure_ascii=False,indent=2))
(out/"review-markers.json").write_text(json.dumps(markers,ensure_ascii=False,indent=2))
p=src/"main/java/com/kelimebulmacasi/a1"
def read(name):return (p/name).read_text()
room=read("PremiumOnlineRoom.kt");online=read("PremiumOnlineScreen.kt");solo=read("PremiumArenaScreen.kt");board=read("ArenaBoardV2.kt")
create=room[room.index("    fun create("):room.index("    private fun reserve(",room.index("    fun create("))]
contracts={
 "room_payload_is_string": 'roundPayload: String?' in create and 'mapOf("roundData" to roundPayload)' in create and 'mapOf("roundData" to snapshot)' not in create,
 "ready_revision_guard": "current.configRevision!=m.configRevision" in room and "current.roundData!=m.roundData" in room,
 "lobby_can_validate_before_ready": online.index("Wq132WebEngine.decodeShared(")<online.index("Wq128PremiumRoomLobby("),
 "native_game_generator": "WebView(" not in read("Wq132WebEngine.kt"),
 "solo_single_question_count": 'Text("SORU SAYISI"' not in solo,
 "solo_actual_level_summary": 'activeRoundLevels.joinToString(" + ")' in solo,
 "ordinary_board_present": "private fun BoardFrame(" in read("PuzzleScreen.kt"),
 "tablet_uses_measured_not_config_height": "(arenaConfig.screenHeightDp-175f)" not in solo,
}
report={"counts":dict(stats),"total_files":len(inventory),"markers_by_kind":dict(Counter(m["kind"] for m in markers)),"structural_errors":errors,"contracts":contracts}
(out/"summary.json").write_text(json.dumps(report,ensure_ascii=False,indent=2))
print(json.dumps(report,ensure_ascii=False,indent=2))
# Findings are preserved even if lint/contract checks find an existing defect.
# Do not pass off review candidates as proven bugs or bypass release QA.
