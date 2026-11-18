#!/usr/bin/env python3
"""Fix invisible WebGL avatars after restoring the v1.0.21 source overlay.

The Android WebView currently shows CSS scenery but can fail to composite the
transparent WebGL canvas. Switch to an opaque accelerated canvas, avoid
unnecessary face culling, and keep a local stand-in visible until rendering
actually succeeds. No user messages or API keys enter this scene.
"""
from pathlib import Path

WORKSPACE = Path(__file__).resolve().parent.parent
ROOT = WORKSPACE / "source" if (WORKSPACE / "source").is_dir() else WORKSPACE

def once(text: str, before: str, after: str) -> str:
    count = text.count(before)
    if count != 1:
        raise RuntimeError(f"Expected one replacement, got {count}: {before[:100]!r}")
    return text.replace(before, after, 1)

html_file = ROOT / "app/src/main/assets/teacher_3d_stage.html"
html = html_file.read_text(encoding="utf-8")
html = once(html, '</style></head>', """
/* Shown until a successfully drawn WebGL frame replaces the stand-in. */
#gl{position:relative;z-index:1}
.light,#floor{z-index:2}
#standin{position:absolute;inset:0;z-index:3;display:flex;flex-direction:column;align-items:center;justify-content:center;pointer-events:none;overflow:hidden;opacity:.98}
#standin .body{position:relative;top:130px;width:210px;height:215px;background:linear-gradient(115deg,#167a75,#084343);border-radius:90px 90px 20px 20px;box-shadow:inset -18px -10px 30px #052a34,0 18px 40px #061f2666}
#standin .neck{position:absolute;bottom:calc(50% - 119px);width:45px;height:80px;background:#e8b8a0;border-radius:22px;z-index:2}
#standin .face{position:absolute;z-index:4;top:calc(50% - 105px);width:154px;height:183px;border-radius:46% 46% 44% 44%;background:linear-gradient(110deg,#f4d0b4,#db9c86);box-shadow:inset -15px -11px 20px #9c605255,2px 8px 30px #081e2244}
#standin .hair{position:absolute;top:calc(50% - 125px);z-index:5;width:173px;height:67px;border-radius:80% 80% 34% 23%;background:linear-gradient(120deg,#704b3d,#352321);box-shadow:0 8px 12px #0b202555}
#standin .eye{position:absolute;top:73px;width:17px;height:11px;border-radius:50%;background:#fff4e7;box-shadow:inset 0 -2px 2px #755853}
#standin .eye:after{content:'';display:block;width:8px;height:9px;margin:1px auto;background:#47362e;border-radius:50%}
#standin .eye.left{left:31px}#standin .eye.right{right:31px}
#standin .nose{position:absolute;top:91px;left:70px;width:14px;height:32px;border-radius:45%;box-shadow:3px 1px 3px #a7696055}
#standin .mouth{position:absolute;left:56px;top:133px;width:42px;height:5px;border-radius:0 0 40px 40px;background:#8a4b50;transition:height .12s}
#standin.speaking .mouth{animation:avatar-mouth .25s infinite alternate}
#standin[data-actor="Deniz"] .body{background:linear-gradient(110deg,#5584b9,#263c73)}
#standin[data-actor="Deniz"] .hair{background:linear-gradient(120deg,#453b42,#231c24)}
#standin[data-actor="Mert"] .body{background:linear-gradient(110deg,#78658e,#322946)}
#standin[data-actor="Mert"] .hair{height:42px;background:linear-gradient(120deg,#53565a,#23252b)}
@keyframes avatar-mouth{from{height:5px}to{height:22px}}
#fallback{inset:auto 7% 5%;z-index:4;padding:10px 14px;background:#08242ad9;border:1px solid #d6b88a75;border-radius:12px;font:500 13px system-ui}
</style></head>""")
html = once(html, '</canvas><div id="floor">', """</canvas>
<div id="standin" data-actor="Elif" aria-label="3D yüklenene kadar görünen öğretmen">
<div class="body"></div><div class="neck"></div><div class="face"><div class="eye left"></div><div class="eye right"></div><div class="nose"></div><div class="mouth"></div></div><div class="hair"></div></div>
<div id="floor">""")
html = once(html, "'use strict';", """'use strict';
// Avoid an empty room if the WebGL context, GPU shader, or JS frame fails.
window.__teacherRenderReady = false;
window.__teacherShowError = function (error) {
  const message = document.getElementById('fallback');
  message.textContent = '3D öğretmen bu cihazda açılamadı. Geçici öğretmen görünümü gösteriliyor.';
  message.style.display = 'flex';
  console.warn('Teacher WebGL:', error);
};
window.teacherState = function (value) {
  if (typeof value === 'string') { try { value = JSON.parse(value); } catch (_) { return; } }
  if (!value || typeof value !== 'object') return;
  const standin = document.getElementById('standin');
  if (['Elif','Deniz','Mert'].includes(value.actor)) standin.dataset.actor = value.actor;
  standin.classList.toggle('speaking', !!value.speaking);
};
window.addEventListener('error', event => window.__teacherShowError(event.message || 'unknown JS error'));
window.addEventListener('unhandledrejection', event => window.__teacherShowError(String(event.reason)));
""")
html = once(html,
    "const canvas=document.getElementById('gl'),gl=canvas.getContext('webgl',{alpha:true,antialias:true,premultipliedAlpha:false,depth:true,preserveDrawingBuffer:false,powerPreference:'low-power'});",
    "const canvas=document.getElementById('gl'),gl=canvas.getContext('webgl',{alpha:false,antialias:true,depth:true,preserveDrawingBuffer:false,powerPreference:'default'}) || canvas.getContext('experimental-webgl',{alpha:false,antialias:true,depth:true});")
html = once(html,
    "if(!gl){document.getElementById('fallback').style.display='flex';return;}",
    "if(!gl){window.__teacherShowError('WebGL context unavailable');return;}\ncanvas.addEventListener('webglcontextlost', event => {event.preventDefault();window.__teacherShowError('GPU context lost');});")
html = once(html,
    "actor(o.actor||state.actor);for(const k of ['speaking','listening','thinking'])",
    "actor(o.actor||state.actor);document.getElementById('standin').dataset.actor=state.actor;document.getElementById('standin').classList.toggle('speaking',!!o.speaking);for(const k of ['speaking','listening','thinking'])")
html = once(html, "gl.clearColor(0,0,0,0);", "gl.clearColor(.065,.165,.19,1.0);")
html = once(html, "gl.enable(gl.CULL_FACE);gl.cullFace(gl.BACK);",
    "gl.disable(gl.CULL_FACE); // Avoid vendor-specific winding/culling visibility failures.")
html = once(html, "\n}\nrequestAnimationFrame(frame);\n})();", """
if (!window.__teacherRenderReady) {
  const error = gl.getError();
  if (error === gl.NO_ERROR) {
    window.__teacherRenderReady = true;
    document.getElementById('standin').style.display = 'none';
  } else window.__teacherShowError('GL error ' + error);
}
}
requestAnimationFrame(frame);
})();""")
assert 'alpha:false' in html and 'window.__teacherRenderReady' in html
html_file.write_text(html, encoding="utf-8")

webview_file = ROOT / "app/src/main/java/com/kelimebulmacasi/a1/TeacherStage3D.kt"
kt = webview_file.read_text(encoding="utf-8")
kt = once(kt, 'import android.graphics.Color\n',
    'import android.graphics.Color\nimport android.view.View\n')
kt = once(kt, 'WebView(context).apply {\n                setBackgroundColor(Color.TRANSPARENT)',
    """WebView(context).apply {
                // A hardware-composited WebView is required for reliable WebGL.
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                setBackgroundColor(Color.rgb(16, 42, 50))""")
webview_file.write_text(kt, encoding="utf-8")

gradle_file = ROOT / "app/build.gradle.kts"
gradle = gradle_file.read_text(encoding='utf-8')
gradle = once(gradle, 'versionCode = 44', 'versionCode = 45')
gradle = once(gradle, 'versionName = "1.0.21-3d-live-teacher"',
              'versionName = "1.0.22-3d-render-fix"')
gradle_file.write_text(gradle, encoding='utf-8')
print('Applied v1.0.22 teacher WebGL compositor and stand-in fix')
