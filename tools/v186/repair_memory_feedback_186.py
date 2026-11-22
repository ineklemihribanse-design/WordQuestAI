#!/usr/bin/env python3
"""Correct Package 3 card-feedback lexical scope in cumulative v1.0.86 builds.

P3 added a nonblocking LaunchedEffect that reads 'reviewed', but its
source overlay placed the declaration below the effect. Move ONLY the
already-existing declaration. No UI behavior, database or user data change.
"""
from pathlib import Path

root=Path(__file__).resolve().parents[2]
path=root/"source/app/src/main/java/com/kelimebulmacasi/a1/VocabularyNeonPractice.kt"
src=path.read_text("utf-8")
declaration="        val reviewed = reviews.containsKey(position)\n"
anchor='        var briefFeedback by remember(card.key, position) { mutableStateOf("") }\n'
effect='        LaunchedEffect(reviewed, typeFeedback, position, card.key) {'
assert src.count(declaration)==1, "Expected precisely one reviewed declaration"
assert src.count(anchor)==1 and src.count(effect)==1, "Unexpected P3 feedback anchors"
if src.index(declaration)<src.index(anchor)<src.index(effect):
    print("PASS: Package 3 reviewed state already declared before effect")
else:
    assert src.index(anchor)<src.index(effect)<src.index(declaration), (
        "Unexpected reviewed lexical order; manual review required")
    src=src.replace(declaration,"",1).replace(anchor,declaration+anchor,1)
    path.write_text(src,encoding="utf-8")
    print("PASS: Package 3 feedback reads reviewed only after declaration")
assert src.index(declaration)<src.index(anchor)<src.index(effect)
