"""Build a server-only SQL seed from the app's English/Turkish catalog."""
import argparse
import json
from collections import defaultdict
from pathlib import Path
import random
import subprocess
import tempfile
from concurrent.futures import ThreadPoolExecutor

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
LEVELS = ("A1", "A2", "B1", "B2", "C1", "C2")
import sys
sys.path.insert(0, str(ROOT / "content"))
from generate import normal, seed_for, valid_board


def item(mode, level, topic, kind, prompt, answer, options=(), explanation=""):
    return dict(mode=mode, level=level, topic=topic, kind=kind, prompt=prompt, answer=answer,
                options=list(options), explanation=explanation)


def build_items():
    fast, yds, tags = {}, {}, {}
    games = [game for name in ("puzzles.json", "expanded-puzzles.json")
             for game in json.loads((ASSETS / name).read_text(encoding="utf-8"))]
    for game in games:
        if game["mode"] == "kind":
            for answer, clue, *_ in game["words"]:
                tags.setdefault((game["level"], normal(answer), clue), game["category"])
    for game in games:
        level, topic = game["level"], game["category"]
        for answer, clue, *_ in game["words"]:
            if not 2 <= len(normal(answer)) <= 22 or not clue.strip():
                continue
            key = (level, topic, normal(answer))
            kind = tags.get((level, normal(answer), clue), "Sözcük bilgisi")
            if game["mode"] == "topic":
                fast.setdefault(key, item("fast", level, topic, kind, clue, answer))
            elif game["mode"] == "kind":
                key = (level, "Kelime Türleri", normal(answer))
                fast.setdefault(key, item("fast", level, "Kelime Türleri", game["category"], clue, answer))
            elif game["mode"] == "yds":
                if topic == "YDS Hazırlık":
                    topic = dict(Kolay="Akademik Kelimeler", Orta="Bağlam ve Bağlaçlar", Zor="İleri Akademik İngilizce")[level]
                yds.setdefault((level, normal(answer)), item("yds", level, topic, "Kelime", clue, answer))
    for line in (ASSETS / "vocabulary-extra.tsv").read_text(encoding="utf-8").splitlines():
        if line and not line.startswith("#"):
            level, topic, kind, answer, clue = line.split("\t")
            fast[level, topic, normal(answer)] = item("fast", level, topic, kind, clue, answer)
    other = json.loads((ROOT / "arena/content.json").read_text(encoding="utf-8"))
    for row in other["yds"]:
        level, kind, prompt, answer, options, explanation = row
        assert len(set(options)) == 4 and answer in options
        yds[level, prompt] = item("yds", level, "YDS Soru Türleri", kind, prompt, answer, options, explanation)
    oral = [item("oral", level, "Konuşma", "Sözlü anlatım", prompt, "")
            for level, prompts in other["oral"].items() for prompt in prompts]
    return [*fast.values(), *yds.values(), *oral]


def make_board(job, layout):
    level, topic, size, seq, pool = job
    ident = f"{level}-{seed_for(topic)}-{size}-{seq}"
    rng = random.Random(seed_for(ident))
    for attempt in range(9):
        subset = rng.sample(pool, size)
        words = [normal(row["answer"]) for row in subset]
        result = subprocess.run([layout, "12", str(rng.getrandbits(32))], input="\n".join(words),
                                text=True, capture_output=True, check=False)
        if result.returncode == 2:
            continue
        result.check_returncode()
        places = [(int(r), int(c), d) for r, c, d in map(str.split, result.stdout.splitlines())]
        valid_board(words, places)
        rows = max(r + (len(w) if d == "D" else 1) for (r, _, d), w in zip(places, words, strict=True))
        cols = max(c + (len(w) if d == "A" else 1) for (_, c, d), w in zip(places, words, strict=True))
        starts = sorted({(r, c) for r, c, _ in places})
        data = [dict(prompt=row["prompt"], answer=row["answer"], topic=row["topic"], kind=row["kind"],
                     length=len(w), row=r, col=c, direction=d, number=starts.index((r, c)) + 1)
                for row, w, (r, c, d) in zip(subset, words, places, strict=True)]
        return ident, level, topic, size, rows, cols, data
    raise RuntimeError(f"Tahta oluşturulamadı {ident} {level} {topic}")


def sql_literal(value):
    return "'" + str(value).replace("'", "''") + "'"


def as_json(value):
    return sql_literal(json.dumps(value, ensure_ascii=False, separators=(",", ":"))) + "::jsonb"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    items = build_items()
    groups = defaultdict(dict)
    for row in items:
        if row["mode"] == "fast":
            for topic in ("", row["topic"]):
                groups[row["level"], topic].setdefault(normal(row["answer"]), row)
    jobs = [(level, topic, size, seq, list(pool.values()))
            for (level, topic), pool in sorted(groups.items())
            for size in (8, 10, 15, 20) if len(pool) >= size
            for seq in range(4 if topic == "" else 2)]
    with tempfile.TemporaryDirectory() as work:
        layout = str(Path(work) / "layout")
        subprocess.run(["g++", "-O2", "-std=c++17", str(ROOT / "content/crossword-layout.cpp"), "-o", layout], check=True)
        with ThreadPoolExecutor(max_workers=5) as pool:
            boards = list(pool.map(lambda job: make_board(job, layout), jobs))

    with args.output.open("w", encoding="utf-8") as out:
        print("-- Generated by arena/seed.py. Server only.\nbegin;", file=out)
        print("truncate arena_private.items, arena_private.boards;", file=out)
        values = [
            "(" + ",".join([
                sql_literal(f"{row['mode']}_{index}"),
                *map(sql_literal, (row[k] for k in ("mode","level","topic","kind","prompt","answer"))),
                as_json(row["options"]), sql_literal(row["explanation"]),
            ]) + ")"
            for index, row in enumerate(items)
        ]
        for offset in range(0, len(values), 400):
            print("insert into arena_private.items(id,mode,level,topic,kind,prompt,answer,options,explanation) values\n"
                  + ",\n".join(values[offset:offset+400]) + ";", file=out)
        values.clear()
        for identity, level, topic, count, rows, cols, words in boards:
            values.append(f"({sql_literal(identity)},{sql_literal(level)},{sql_literal(topic)},{count},{rows},{cols},{as_json(words)})")
        for offset in range(0, len(values), 50):
            print("insert into arena_private.boards(id,level,topic,size,height,width,words) values\n"
                  + ",\n".join(values[offset:offset+50]) + ";", file=out)
        print("commit;", file=out)
    print(f"{len(items)} soru, {len(boards)} tahta")


if __name__ == "__main__":
    main()
