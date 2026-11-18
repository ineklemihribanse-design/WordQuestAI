"""Generate the bundled crosswords while retaining previously released puzzle IDs and grids."""
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import argparse
import hashlib
import json
import random
import re
import subprocess

ROOT = Path(__file__).resolve().parent
ASSETS = ROOT.parent / "app/src/main/assets"
FILES = sorted(ROOT.glob("topics-*.txt")) + [ROOT / "word-kinds.txt", ROOT / "yds.txt"]


def normal(answer):
    return re.sub(r"[^A-Z]", "", answer)


def unique_words(entries):
    return list({normal(entry[0]): entry for entry in reversed(entries)}.values())[::-1]


def seed_for(value):
    return int.from_bytes(hashlib.sha256(value.encode()).digest()[:4], "big")


def read_topics():
    result = []
    for path in FILES:
        mode = "yds" if path.stem == "yds" else "kind" if path.stem == "word-kinds" else "topic"
        for line in path.read_text(encoding="utf-8").splitlines():
            if not line.strip():
                continue
            if line.startswith("# "):
                level, key, title = line[2:].split("|")
                result.append((mode, level, key, title, []))
            elif line.startswith("> "):
                result[-1][-1].append((line[2:], []))
            else:
                for item in re.split(r"(?<!\|)\|(?!\|)", line):
                    answer, definition = item.split("=", 1)
                    clue, _, context = definition.partition("||")
                    assert re.fullmatch(r"[A-Z]+(?:[ '-][A-Z]+)*", answer), (path, answer)
                    assert len(normal(answer)) <= 23 and len(normal(answer)) >= 2, (path, answer)
                    assert clue.strip(), (path, answer)
                    assert not context or context.count("_____") == 1, (path, answer, context)
                    result[-1][-1][-1][1].append((answer, clue.strip(), context.strip()))
    return result


def valid_board(words, placements):
    board, directions = {}, {}
    edges = Counter()
    for idx, (word, (row, column, direction)) in enumerate(zip(words, placements, strict=True)):
        assert direction in {"A", "D"}
        dr, dc = (0, 1) if direction == "A" else (1, 0)
        for offset, char in enumerate(word):
            cell = row + dr * offset, column + dc * offset
            assert min(cell) >= 0 and max(cell) < 31
            assert board.get(cell, char) == char
            assert direction not in directions.get(cell, {})
            board[cell] = char
            directions.setdefault(cell, {})[direction] = idx
    for (row, column), values in directions.items():
        if len(values) == 2:
            edges.update(values.values())
        assert (row, column + 1) not in board or (
            "A" in values and directions[row, column + 1].get("A") == values["A"]
        )
        assert (row + 1, column) not in board or (
            "D" in values and directions[row + 1, column].get("D") == values["D"]
        )
    assert len(edges) == len(words)
    discovered = {0}
    while True:
        expanded = discovered.copy()
        for overlap in directions.values():
            if discovered.intersection(overlap.values()):
                expanded.update(overlap.values())
        if expanded == discovered:
            break
        discovered = expanded
    assert len(discovered) == len(words)


def make_game(spec, executable):
    header, candidates, sections = spec
    assert all(len(section) >= 9 for section in sections)
    assert len(candidates) >= 20
    seed = seed_for(header["id"])
    rng = random.Random(seed)
    for attempt in range(20):
        favored = []
        for section in sections:
            keys = {normal(row[0]) for row in favored}
            choices = [row for row in section if normal(row[0]) not in keys]
            count = min(len(choices), 12 if len(sections) == 1 else 6)
            favored.extend(rng.sample(choices, count))
        chosen = {normal(row[0]) for row in favored}
        remainder = [row for row in candidates if normal(row[0]) not in chosen]
        items = favored + rng.sample(remainder, 20 - len(favored))
        rng.shuffle(items)
        words = [normal(row[0]) for row in items]
        result = subprocess.run(
            [executable, "28" if attempt < 3 else "60", str((seed + attempt) % (2**32))],
            input="\n".join(words), text=True, capture_output=True, check=False,
        )
        if result.returncode == 2:
            continue
        result.check_returncode()
        places = [(int(r), int(c), d) for r, c, d in map(str.split, result.stdout.splitlines())]
        valid_board(words, places)
        return header | {"words": [list(row) + list(place) for row, place in zip(items, places, strict=True)]}
    raise RuntimeError("Could not create " + header["id"])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("layout", help="Compiled crossword-layout executable")
    parser.add_argument("--workers", type=int, default=7)
    parser.add_argument("--output", type=Path, default=ASSETS / "expanded-puzzles.json")
    args = parser.parse_args()
    executable = str(Path(args.layout).resolve())
    old = json.loads((ASSETS / "puzzles.json").read_text(encoding="utf-8"))
    specs = []
    groups = read_topics()
    for mode, level, key, category, sections in groups:
        assert len(sections) >= 4
        pool = unique_words([row for _, rows in sections for row in rows])
        indices = [(i, (i,)) for i in range(len(sections))]
        indices += [
            (len(sections) + index, (i, j))
            for index, (i, j) in enumerate(
                [(i, (i + 1) % len(sections)) for i in range(len(sections))]
                + [(i, (i + 2) % len(sections)) for i in range(len(sections) - 1)]
            )
        ]
        if len(indices) < 15:
            indices.extend((len(indices) + i, (i % len(sections),)) for i in range(15 - len(indices)))
        if mode != "yds":
            indices = indices[:15]
        for idx, section_indices in indices:
            themes = [sections[i] for i in section_indices]
            title = " ve ".join(x[0] for x in themes)
            if idx >= len(sections) and len(themes) == 1:
                title += " · Alıştırma"
            anchors = [unique_words(rows) for _, rows in themes]
            if len(themes) > 1:
                # Both named subtopics contribute to the candidate pool.
                candidates = unique_words([row for _, rows in themes for row in rows] + pool)
            else:
                candidates = pool
            specs.append((
                dict(id=f"exp-{key}-{idx+1:02}", mode=mode, level=level, category=category, title=title),
                candidates, anchors,
            ))
        for idx in range(5):
            specs.append((
                dict(id=f"mix-{key}-{idx+1}", mode=mode, level=level, category=category, title=f"Karışık {idx+1}", mixed=True),
                pool, [pool],
            ))
    ids = [header["id"] for header, _, _ in specs]
    assert len(set(ids)) == len(ids) and not set(ids).intersection(x["id"] for x in old)
    with ThreadPoolExecutor(max_workers=args.workers) as workers:
        result = []
        for game in workers.map(lambda spec: make_game(spec, executable), specs):
            result.append(game)
            if len(result) % 50 == 0:
                print(f"{len(result)}/{len(specs)} crosswords", flush=True)
    combinations = {(x["mode"], x["level"], x["category"]) for x in result}
    for key in combinations:
        package = [x for x in result if tuple(x[k] for k in ("mode", "level", "category")) == key]
        assert sum(not x.get("mixed", False) for x in package) >= 15
        assert sum(x.get("mixed", False) for x in package) >= 5
        assert len({frozenset(normal(w[0]) for w in b["words"]) for b in package}) == len(package)
    args.output.write_text(json.dumps(result, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"{len(result)} boards, {len(combinations)} packages, {len(set(normal(w[0]) for b in result for w in b['words']))} distinct answers")


if __name__ == "__main__":
    main()
