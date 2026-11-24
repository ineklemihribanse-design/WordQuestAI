#!/usr/bin/env python3
"""Extra real-device-key-event regressions on the existing disposable emulator."""
import sys
import time
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"v190"))
from qa_system_back_190 import adb, ui_nodes, find_label, click_node, wait_for, assert_process

def visible(token):
    for _ in range(5):
        node=find_label(ui_nodes(),token)
        if node is not None:
            return node
        adb("shell","input","swipe","520","1400","520","650","300")
        time.sleep(1)
    raise AssertionError("Navigation control not found: "+token)

def back():
    adb("shell","input","keyevent","KEYCODE_BACK")
    assert_process()

def main():
    # v190 baseline QA has already dismissed the true-root exit dialog.
    click_node(visible("OYUNLAR"))
    wait_for("Ana bölümlere dön")
    click_node(visible("Bulmacalarım"))
    # Ensure we actually entered the child rather than immediately backing.
    wait_for("Bulmaca ekle")
    back()
    wait_for("Ana bölümlere dön")
    wait_for("Bulmacalarım")
    back()
    wait_for("Beş öğrenme alanı")
    print("PASS: child page returns to OYUNLAR section before Home root")
    click_node(visible("ARENA"))
    click_node(visible("Bulmaca Düellosu"))
    wait_for("Oyun modlarına dön")
    back()
    wait_for("Maceranı seç")
    assert find_label(ui_nodes(),"Oyun modlarına dön") is None
    back()
    wait_for("Beş öğrenme alanı")
    print("PASS: Arena settings hardware Back returns to mode picker before Home")
    assert_process()

if __name__=="__main__":
    main()
