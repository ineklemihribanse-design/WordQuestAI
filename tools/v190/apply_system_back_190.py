#!/usr/bin/env python3
"""Repair Android hardware Back for home subcategories and top-level exit.

Runs after Packages 1–7 and all 1.0.90 visual overlays. All anchors are checked
before writing; no scores, preferences, games, Firebase, or signing changes.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"


def replace_one(source: str, original: str, replacement: str, label: str) -> str:
    count = source.count(original)
    if count != 1:
        raise AssertionError(f"v190 Android back {label}: expected one anchor, found {count}")
    return source.replace(original, replacement, 1)


def apply() -> None:
    pending = {}

    # These five category hubs are local state within the Home page. The
    # parent page-level BackHandler is disabled on Home in the old build.
    home = SRC / "Wq175PremiumHome.kt"
    source = home.read_text("utf-8")
    source = replace_one(
        source, "import androidx.compose.foundation.BorderStroke",
        "import androidx.activity.compose.BackHandler\nimport androidx.compose.foundation.BorderStroke",
        "category BackHandler import")
    source = replace_one(
        source,
        "    var activeSection by remember { mutableStateOf<String?>(null) }",
        """    var activeSection by remember { mutableStateOf<String?>(null) }
    // The parent route is still Page.Home while an ÖĞREN/OYUNLAR/YDS
    // section is visible. Consume the physical Back event HERE so it
    // returns to the five category cards rather than closing Android.
    BackHandler(enabled = activeSection != null) {
        activeSection = null
    }""",
        "home subcategory Back priority")
    pending[home] = source

    # Never exit immediately on a Back press at the actual Home root:
    # require an explicit confirmation. Non-Home destinations keep the
    # Package 6 navigation trail and in-progress work exit confirmation.
    app = SRC / "HomeScreen.kt"
    source = app.read_text("utf-8")
    source = replace_one(
        source,
        "    var confirmLeaveWork by rememberSaveable { mutableStateOf(false) }",
        """    var confirmLeaveWork by rememberSaveable { mutableStateOf(false) }
    var confirmExitApp by rememberSaveable { mutableStateOf(false) }""",
        "saved exit confirmation state")
    source = replace_one(
        source,
        "    BackHandler(page != Page.Home.name) { requestBack() }",
        """    // Home's category BackHandler is registered inside the content below,
    // so it has priority over this top-level fallback.
    BackHandler {
        if (page == Page.Home.name) confirmExitApp = true
        else requestBack()
    }
    if (confirmExitApp && page == Page.Home.name) AlertDialog(
        onDismissRequest = { confirmExitApp = false },
        title = { Text("Uygulamadan çıkılsın mı?") },
        text = { Text("Uygulamayı kapatmak istiyor musun?") },
        confirmButton = {
            TextButton(onClick = {
                confirmExitApp = false
                (appContext as? android.app.Activity)?.finish()
            }) { Text("Çık") }
        },
        dismissButton = {
            TextButton(onClick = { confirmExitApp = false }) { Text("İptal") }
        }
    )""",
        "home exit requires explicit confirmation")
    assert "fun requestBack()" in source and "navigationTrail" in source
    assert 'Page.Game.name, Page.Editor.name, Page.VerbGame.name' in source
    assert 'session.closeGame()' in source
    pending[app] = source

    for path, updated in pending.items():
        path.write_text(updated, "utf-8")
        print(f"PASS Android system Back navigation patch: {path.name}")
    print("PASS: nested sections return to category list; root app exit requires confirmation")
    print("PASS: existing saved games, work-exit confirmation, app identity untouched")


if __name__ == "__main__":
    apply()
