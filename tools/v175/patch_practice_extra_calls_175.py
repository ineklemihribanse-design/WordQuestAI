#!/usr/bin/env python3
"""Safely replace two special Daily Plan MenuItems.

The existing onClick lambdas contain return@MenuItem statements. To preserve
their exact actions, label the same trailing lambda explicitly "MenuItem@"
when changing only its native composable presentation. This prevents the
Kotlin implicit-label compilation regression seen in CI #434.
"""

def replace_special_card_call(source: str, title: str) -> str:
    anchor = 'MenuItem("' + title + '"'
    if source.count(anchor) != 1:
        raise AssertionError(f"{title}: expected exactly one MenuItem, got {source.count(anchor)}")
    start = source.index(anchor)
    open_paren = start + len("MenuItem")
    assert source[open_paren] == "("
    depth = 0
    quote = None
    escaped = False
    closing = -1
    for i in range(open_paren, len(source)):
        ch = source[i]
        if quote is not None:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == quote:
                quote = None
            continue
        if ch in ("'", '"'):
            quote = ch
        elif ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                closing = i
                break
    if closing < 0:
        raise AssertionError(f"{title}: unclosed MenuItem arguments")
    block = closing + 1
    while block < len(source) and source[block].isspace():
        block += 1
    if block >= len(source) or source[block] != "{":
        raise AssertionError(f"{title}: expected trailing lambda (not an unsupported call shape)")
    return (
        source[:start] + "Wq175PremiumPracticeSpecialTile"
        + source[open_paren:block].rstrip()
        + " MenuItem@ " + source[block:]
    )
