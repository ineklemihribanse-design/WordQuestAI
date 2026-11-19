#!/usr/bin/env python3
"""Original WordQuest AI mint messenger icon family — pure vector drawables.

All silhouettes are authored specifically for Lena's light cream/mint UI.
Paths are editable Android VectorDrawable XML committed via this generator,
not external stock icon packages, fonts, emoji or heavyweight bitmap sheets.
"""
from pathlib import Path
import sys
from xml.etree import ElementTree

ICON_STROKES = {
  "chat": [
    "M4.8,5.1 C2.6,7 2.3,10.3 3.5,13.1 C4.0,14.3 4.9,15.2 6.1,16.0 L5.2,20.3 L9.0,18.2 C13.6,19.9 18.3,17.5 20.3,13.8 C22.8,9.1 18.4,3.4 12.5,3.4 C9.3,3.4 6.7,3.9 4.8,5.1",
    "M7.7,11.2 L7.8,11.2 M12,11.2 L12.1,11.2 M16.3,11.2 L16.4,11.2",
  ],
  "practice": [
    "M12,3.1 C16.9,3.1 20.9,7.1 20.9,12 C20.9,16.9 16.9,20.9 12,20.9 C7.1,20.9 3.1,16.9 3.1,12 C3.1,7.1 7.1,3.1 12,3.1",
    "M12,7.1 C14.7,7.1 16.9,9.3 16.9,12 C16.9,14.7 14.7,16.9 12,16.9 C9.3,16.9 7.1,14.7 7.1,12 C7.1,9.3 9.3,7.1 12,7.1",
    "M9.9,12.1 L11.4,13.7 L14.6,10.3",
  ],
  "grammar": [
    "M12,5.2 C9.9,3.8 7,3.4 3.1,4.2 L3.1,17.8 C6.9,17 10,17.6 12,19.1 C14.3,17.6 17.6,17 20.9,17.8 L20.9,4.2 C17.5,3.4 14.2,3.7 12,5.2 Z",
    "M12,5.2 L12,19.1 M5.6,8.1 C7.6,7.6 9,7.9 10,8.6 M14.3,8.6 C15.6,7.9 17,7.6 18.6,8.1 M5.6,11.6 C7.4,11.3 9,11.7 10,12.3 M14.3,12.3 C15.6,11.7 17,11.3 18.6,11.6",
  ],
  "word": [
    "M4.2,5.1 L17.1,5.1 C18.1,5.1 18.9,5.9 18.9,6.9 L18.9,17.1 C18.9,18.1 18.1,18.9 17.1,18.9 L4.2,18.9 C3.2,18.9 2.4,18.1 2.4,17.1 L2.4,6.9 C2.4,5.9 3.2,5.1 4.2,5.1 Z",
    "M7,14.5 L9.6,8.1 L12.2,14.5 M8,12.4 L11.2,12.4 M14.2,9.1 L16.7,9.1 L14.1,14.6 L16.7,14.6 M18.9,9.7 L21.4,9.7 M18.9,13.5 L21.4,13.5",
  ],
  "lesson": [
    "M2.2,9 L12,4.4 L21.8,9 L12,13.7 Z",
    "M6.5,11.4 L6.5,16.4 C9.5,19 14.7,19 17.5,16.4 L17.5,11.4",
    "M21,10.1 L21,16.9 L19.8,18.6",
  ],
  "correction": [
    "M12.3,2.8 L14.1,9.7 L21,11.5 L14.1,13.3 L12.3,20.2 L10.5,13.3 L3.6,11.5 L10.5,9.7 Z",
    "M5.7,4.1 L6.4,6.6 L8.9,7.3 L6.4,8 L5.7,10.5 L5,8 L2.5,7.3 L5,6.6 Z",
    "M17,17.7 L18,18.6 L20.7,15.6",
  ],
  "listen": [
    "M4,12.4 L4,11 C4,6.4 7.6,3.3 12,3.3 C16.4,3.3 20,6.4 20,11 L20,12.4",
    "M4.4,12.4 L6.4,12.4 C7.1,12.4 7.7,13 7.7,13.7 L7.7,17.4 C7.7,18.1 7.1,18.7 6.4,18.7 L4.6,18.7 C3.7,18.7 3,18 3,17.1 L3,14 C3,13.1 3.6,12.4 4.4,12.4 Z",
    "M19.6,12.4 L17.6,12.4 C16.9,12.4 16.3,13 16.3,13.7 L16.3,17.4 C16.3,18.1 16.9,18.7 17.6,18.7 L19.4,18.7 C20.3,18.7 21,18 21,17.1 L21,14 C21,13.1 20.4,12.4 19.6,12.4 Z",
  ],
  "audio": [
    "M3,10 L7,10 L11.5,6 L11.5,18 L7,14 L3,14 Z",
    "M14.5,9.1 C16.1,10.4 16.1,13.6 14.5,14.9",
    "M17.3,6.3 C20.5,9.2 20.5,14.8 17.3,17.7",
  ],
  "camera": [
    "M4.1,7.4 L7.7,7.4 L9.1,5.3 L14.9,5.3 L16.3,7.4 L19.9,7.4 C20.8,7.4 21.4,8.1 21.4,9 L21.4,18 C21.4,18.9 20.8,19.6 19.9,19.6 L4.1,19.6 C3.2,19.6 2.6,18.9 2.6,18 L2.6,9 C2.6,8.1 3.2,7.4 4.1,7.4 Z",
    "M12,9.8 C14.2,9.8 15.8,11.5 15.8,13.6 C15.8,15.7 14.2,17.3 12,17.3 C9.8,17.3 8.2,15.7 8.2,13.6 C8.2,11.5 9.8,9.8 12,9.8",
  ],
  "send": [
    "M2.6,11.4 L21.1,3.5 L16.3,20.8 L11,13.1 Z",
    "M11,13.1 L21.1,3.5 M11,13.1 L8.4,19.5",
  ],
  "cube": [
    "M12,2.5 L20.6,7.3 L20.6,16.8 L12,21.5 L3.4,16.8 L3.4,7.3 Z",
    "M12,12.1 L12,21.5 M3.4,7.3 L12,12.1 L20.6,7.3 M12,12.1 L16.2,9.8",
  ],
  "new": [
    "M12,3.2 C16.9,3.2 20.8,7.1 20.8,12 C20.8,16.9 16.9,20.8 12,20.8 C7.1,20.8 3.2,16.9 3.2,12 C3.2,7.1 7.1,3.2 12,3.2",
    "M12,7.3 L12,16.7 M7.3,12 L16.7,12",
  ],
  "mic": [
    "M9.1,5.3 C9.1,3.6 10.3,2.5 12,2.5 C13.7,2.5 14.9,3.6 14.9,5.3 L14.9,12.8 C14.9,14.6 13.7,15.8 12,15.8 C10.3,15.8 9.1,14.6 9.1,12.8 Z",
    "M6.5,10.9 L6.5,12.4 C6.5,15.8 8.9,18.3 12,18.3 C15.1,18.3 17.5,15.8 17.5,12.4 L17.5,10.9",
    "M12,18.3 L12,21.3 M8.8,21.3 L15.2,21.3",
  ],
  "play": ["M7,4.9 L19.2,12 L7,19.1 Z"],
  "pause": ["M7.3,5 L7.3,19 M16.7,5 L16.7,19"],
  "history": [
    "M12,3.2 C16.9,3.2 20.8,7.1 20.8,12 C20.8,16.9 16.9,20.8 12,20.8 C7.1,20.8 3.2,16.9 3.2,12 C3.2,7.1 7.1,3.2 12,3.2",
    "M12,6.8 L12,12.4 L15.8,14.8",
  ],
  "bookmark": [
    "M6.3,3.2 L17.7,3.2 C18.4,3.2 19,3.8 19,4.5 L19,21.1 L12,16.5 L5,21.1 L5,4.5 C5,3.8 5.6,3.2 6.3,3.2 Z",
  ],
}
ACCENT={"chat":"M12,1.6 L12.1,1.6","practice":"M19.4,4.7 L19.5,4.7",
        "grammar":"M21.6,3.3 L21.7,3.3","word":"M3.2,20.6 L3.3,20.6"}
GREEN="#195F47"
def render(name,paths):
    root=['<?xml version="1.0" encoding="utf-8"?>',
          '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
          '    android:width="24dp"',
          '    android:height="24dp"',
          '    android:viewportWidth="24" android:viewportHeight="24">']
    for i,path in enumerate(paths):
        root.extend([
            '    <path',
            '        android:fillColor="#00000000"',
            f'        android:strokeColor="{GREEN}"',
            '        android:strokeWidth="1.75"',
            '        android:strokeLineCap="round"',
            '        android:strokeLineJoin="round"',
            f'        android:pathData="{path}"/>',
        ])
    root.append('</vector>')
    data="\n".join(root)+"\n"
    ElementTree.fromstring(data)
    return data

def create(root):
    destination=Path(root)
    destination.mkdir(parents=True,exist_ok=True)
    for name,paths in ICON_STROKES.items():
        path=destination/f"wq167_{name}.xml"
        path.write_text(render(name,paths),"utf-8")
    print("CUSTOM_ICON_ASSETS",len(ICON_STROKES),"WordQuest original paths",
          [p.name for p in sorted(destination.glob("wq167_*.xml"))])
    assert len(list(destination.glob("wq167_*.xml")))==len(ICON_STROKES)

if __name__=="__main__":
    if len(sys.argv)!=2:sys.exit("usage: generate_messenger_icons.py res/drawable")
    create(sys.argv[1])
