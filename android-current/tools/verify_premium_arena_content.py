"""Verify the bundled A1/A2 Premium Arena pack before compiling the APK."""
import collections
import json
from pathlib import Path

assets = Path(__file__).resolve().parents[1] / 'app/src/main/assets'
words = json.loads((assets / 'arena-vocabulary-v1.json').read_text(encoding='utf8'))
boards = json.loads((assets / 'arena-crosswords-v1.json').read_text(encoding='utf8'))
assert len(words) >= 750 and len(boards) >= 500
assert len({(w['level'], w['answer']) for w in words}) == len(words)
assert set(w['level'] for w in words) == {'A1','A2'}
assert all(w['answer'].isupper() and w['answer'].isalpha() and w['meaning'].strip() and w['category'] for w in words)
ids = set()
counts = collections.Counter()
for board in boards:
    assert board['id'] not in ids
    ids.add(board['id'])
    size = len(board['words'])
    level = board['level']
    assert size in (20,30,40,50)
    assert level in ('A1','A2','A1–A2')
    answers = [''.join(c for c in w[0] if c in 'ABCDEFGHIJKLMNOPQRSTUVWXYZ') for w in board['words']]
    assert len(set(answers)) == size, board['id']
    by_cell = {}
    direction_by_cell = collections.defaultdict(set)
    member_by_cell = collections.defaultdict(set)
    for i, ((answer, meaning, context, row, col, direction), normalized) in enumerate(zip(board['words'], answers)):
        assert normalized and meaning and row >= 0 and col >= 0 and direction in ('A','D')
        for offset, letter in enumerate(normalized):
            cell = (row + (offset if direction == 'D' else 0), col + (offset if direction == 'A' else 0))
            assert by_cell.get(cell, letter) == letter, (board['id'], cell)
            assert direction not in direction_by_cell[cell], board['id']
            direction_by_cell[cell].add(direction)
            by_cell[cell] = letter
            member_by_cell[cell].add(i)
    network = [set() for _ in board['words']]
    for ids_here in member_by_cell.values():
        for i in ids_here: network[i].update(ids_here - {i})
    reached = {0}
    while True:
        later = reached | set().union(*(network[i] for i in reached))
        if later == reached: break
        reached = later
    assert len(reached) == size, (board['id'],'not connected')
    counts[(level,size)] += 1
for level in ('A1','A2','A1–A2'):
    for size in (20,30,40,50): assert counts[(level,size)] >= 15, (level,size,counts[(level,size)])
print(f'Premium Arena content verified: {len(words)} A1/A2 word entries, {len(boards)} crossed boards')
print('Levels/sizes:', dict(sorted(counts.items())))
