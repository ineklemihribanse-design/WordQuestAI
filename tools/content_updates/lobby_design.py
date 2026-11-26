import json
from pathlib import Path
PATH="design/arena_lobby.json"
RANGES={"schema":(1,1),"phoneCardPercent":(40,60),"tabletCardPercent":(40,65),"cardPadding":(4,14),
"portraitBottom":(0,16),"nameSp":(13,20),"actionHeight":(44,58),"gap":(4,12),"iconSize":(24,44),"radius":(12,28)}
CHOICES={"palette":{"royal","midnight","amethyst"},"sectionOrder":{"code_settings_players","players_code_settings","settings_code_players"}}
def validate(data):
    assert 2<=len(data)<=4096
    def pairs(items):
        obj=dict(items)
        assert len(obj)==len(items),"duplicate field"
        return obj
    o=json.loads(data,object_pairs_hook=pairs)
    assert type(o) is dict and set(o)==set(RANGES)|set(CHOICES)
    for k,(low,high) in RANGES.items(): assert type(o[k]) is int and low<=o[k]<=high,k
    for k,allowed in CHOICES.items(): assert type(o[k]) is str and o[k] in allowed,k
    return o
if __name__=="__main__":
    import sys
    validate(Path(sys.argv[1]).read_bytes())
