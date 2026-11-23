#!/usr/bin/env python3
"""Android emulator hardware-Back smoke test for the real 1.0.90 Compose UI.

Checks that opening a HOME category and pressing device Back returns to the
five categories, and another Back displays exit confirmation without killing
the app. Runs only on disposable CI emulator, not a real user device.
"""
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def adb(*args, timeout=30):
    return subprocess.check_output(["adb", *args], stderr=subprocess.STDOUT, timeout=timeout).decode("utf-8", "replace")


def ui_nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/wq190-back.xml", timeout=45)
    tree = ET.fromstring(adb("exec-out", "cat", "/sdcard/wq190-back.xml"))
    return list(tree.iter("node"))


def label(node):
    return " ".join([node.attrib.get("text", ""), node.attrib.get("content-desc", "")]).casefold()


def find_label(nodes, token):
    found = [node for node in nodes if token.casefold() in label(node)]
    return found[-1] if found else None


def wait_for(token, attempts=5):
    for _ in range(attempts):
        nodes = ui_nodes()
        node = find_label(nodes, token)
        if node is not None:
            return node
        time.sleep(2)
    raise AssertionError(f"Home navigation UI element not found: {token}")


def click_node(node):
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
    if match is None:
        raise AssertionError("Missing Android UI bounds: " + str(node.attrib))
    x1, y1, x2, y2 = map(int, match.groups())
    assert x2 > x1 and y2 > y1
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))


def assert_process():
    assert adb("shell", "pidof", "-s", "com.kelimebulmacasi.a1.practice.fresh").strip(), (
        "Android app unexpectedly exited after system Back")


def main():
    # Emulator upgrade QA has already installed, upgraded and launched WordQuest.
    assert_process()
    target = None
    for _ in range(4):
        nodes = ui_nodes()
        target = find_label(nodes, "OYUNLAR")
        if target is not None:
            break
        adb("shell", "input", "swipe", "520", "1400", "520", "600", "300")
        time.sleep(1)
    if target is None:
        raise AssertionError("The OYUNLAR home category is not present on device")

    click_node(target)
    wait_for("Ana bölümlere dön")
    print("PASS: real Android home subsection OYUNLAR opened")

    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    wait_for("Beş öğrenme alanı")
    assert_process()
    print("PASS: Android hardware Back closes home subsection rather than the app")

    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    wait_for("Uygulamadan çıkılsın mı?")
    assert_process()
    print("PASS: Android Back on true Home requires explicit app exit confirmation")

    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    wait_for("Beş öğrenme alanı")
    assert_process()
    print("PASS: cancelled app exit retains running app and Home screen")


if __name__ == "__main__":
    main()
