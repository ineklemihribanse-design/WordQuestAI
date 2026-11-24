#!/usr/bin/env python3
"""Repair verified navigation defects only; never change board or saved game data."""
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
def once(s,old,new,label):
    assert s.count(old)==1,(label,s.count(old))
    return s.replace(old,new,1)

home=SRC/"HomeScreen.kt"
s=home.read_text("utf-8")
assert "navigationTrail" in s and "confirmLeaveWork" in s and "confirmExitApp" in s
assert "PackageCatalog" in s
# Restore the real parent, not the historical special-case jump. Keep
# fallback destinations only for a restored route with no valid history.
s=once(s,"        val previous = navigationTrail.lastOrNull() ?: Page.Home.name",
'''        val previous = navigationTrail.lastOrNull()?.takeIf { candidate ->
            candidate != page && Page.entries.any { it.name == candidate }
        }''',"actual previous route")
s=once(s,"            Page.Topic -> Page.Packages.name",
'''            Page.Topic -> Page.PackageCatalog.name''',"catalogue fallback")
s=once(s,"            else -> previous\n        }",
'''            else -> Page.Home.name
        }
        val actualDestination = previous ?: destination''',"actual history before fallback")
s=once(s,"            page = destination.takeIf { candidate ->",
"            page = actualDestination.takeIf { candidate ->","restore actual parent")
# A successful editor save is a pop too, not an untracked route mutation.
s=once(s,"                        page = editorOrigin\n                    }\n                    saved",
"                        back()\n                    }\n                    saved","editor successful save")
# A SaveableStateHolder is required to preserve local subsection state
# when the entire HomeScreen leaves composition.
start="        Page.Home -> HomeScreen("
assert s.count(start)==1
a=s.index(start)
b=s.index("\n        Page.NewGame ->",a)
block=s[a:b]
assert block.rstrip().endswith(")"),block[-100:]
s=s[:a]+block.replace(start,'        Page.Home -> topicState.SaveableStateProvider("home:" + session.profileKey) {\n            HomeScreen(',1)+"\n        }"+s[b:]
assert "session.closeGame()" in s and "confirmLeaveWork = true" in s
assert 'title = { Text("Uygulamadan çıkılsın mı?") }' in s
assert "navigationTrail = navigationTrail.dropLast(1)" in s

grid=SRC/"Wq175PremiumHome.kt"
g=grid.read_text("utf-8")
g=once(g,"    var activeSection by remember { mutableStateOf<String?>(null) }",
'''    var activeSection by androidx.compose.runtime.saveable.rememberSaveable(session.profileKey) { mutableStateOf<String?>(null) }''',"retain section while away")
assert "BackHandler(enabled = activeSection != null)" in g

arena=SRC/"ArenaApprovedHome.kt"
r=arena.read_text("utf-8")
anchor="    var matchSettingsPage by rememberSaveable { mutableStateOf(false) }"
assert r.count(anchor)==1
assert 'TextButton(onClick={matchSettingsPage=false}' in r
assert not re.search(r"BackHandler[^\n]*matchSettingsPage",r), "unexpected existing local back handler"
r=once(r,anchor,anchor+'''
    // The mode picker and match settings share the parent route. Consume
    // hardware/gesture Back here, just like the visible return button.
    androidx.activity.compose.BackHandler(enabled = matchSettingsPage) {
        matchSettingsPage = false
    }''',"arena settings hardware back")

# Write only after every original source contract is checked.
for path, content in ((home,s),(grid,g),(arena,r)):
    path.write_text(content,"utf-8")
    print("PASS navigation-only patch:",path.name)
print("PASS actual route history, catalog return, saved home section, arena settings back; leave-work confirmation intact")
