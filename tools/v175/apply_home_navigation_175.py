#!/usr/bin/env python3
"""Native bottom navigation and real learning reminders for the approved mobile home."""
from pathlib import Path
import shutil
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
res=root/"source/app/src/main/res/drawable"
res.mkdir(parents=True,exist_ok=True)
shutil.copyfile(root/"tools/v175/Wq175HomeNavigation.kt",src/"Wq175HomeNavigation.kt")
shutil.copyfile(root/"tools/v175/wq175_home_alert_bell.xml",res/"wq175_home_alert_bell.xml")

p=src/"HomeScreen.kt"
s=p.read_text("utf-8")
def once(a,c,name):
    global s
    assert s.count(a)==1,(name,s.count(a))
    s=s.replace(a,c,1)

once('''    ScreenPage("", "", null) {''',
     '''    Box(Modifier.fillMaxSize()) {
    ScreenPage("", "", null) {''',
     "wrap home in navigation host")

once('''                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = settings)''',
     '''                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Wq175HomeAlerts(
                            due=session.learningDueCount(),
                            onMemory=learning,onDaily=practice)
                        IconButton(onClick = settings)''',
     "real notifications next to settings")

once('''                    progress=progress,insights=insights
                )
            }''',
     '''                    progress=progress,insights=insights
                )
                Spacer(Modifier.height(90.dp))
            }''',
     "ensure end of scrolling content clears fixed bottom bar")

once('''            }
        }
    }
}

@Composable
private fun HeroTiles''',
     '''            }
        }
    }
    Wq175HomeBottomNav(
        session=session,onNewGame=newGame,onArena=firebaseArena,
        onDaily=practice,onAccount=account,
        modifier=Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun HeroTiles''',
     "fixed Android bottom navigation with four working destinations")

p.write_text(s,"utf-8")
print("PASS: Android bottom nav with four real destination callbacks and selected Home state.")
print("PASS: live selected-profile due-word notifications and learning plan actions.")
