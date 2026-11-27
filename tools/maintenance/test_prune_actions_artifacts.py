#!/usr/bin/env python3
import unittest
from datetime import datetime, timezone, timedelta
from prune_actions_artifacts import choose,MAX_DELETE
NOW=datetime(2026,11,28,0,tzinfo=timezone.utc)
def a(i,branch="feature/arena",name="WordQuestAI-v1.0.158-release-diagnostics",hours=240,mb=2,expired=False):
    return {"id":i,"name":name,"created_at":(NOW-timedelta(hours=hours)).isoformat(),
            "workflow_run":{"head_branch":branch,"id":1000+i},"size_in_bytes":mb*1048576,"expired":expired}
class Safety(unittest.TestCase):
    def test_keep_three_per_branch_and_name(self):
        rows=[a(i,hours=240+i) for i in range(6)]
        self.assertEqual({r["id"] for r in choose(rows,NOW)},{3,4,5})
        self.assertEqual(choose([a(i,branch="branch"+str(i)) for i in range(10)],NOW),[])
    def test_never_touch_critical_files(self):
        for name in ["WordQuestAI-v1.0.158-verified-release","WordQuestAI-v1.0.158-device-test",
                     "WordQuestAI-full-assembled-source-audit","WordQuestAI-v150-verified-arena-bank",
                     "WordQuestAI.apk","update.json","WordQuestAI-171-to-172.wqd.gz","unknown"]:
            self.assertEqual(choose([a(i,name=name,hours=400+i) for i in range(10)],NOW),[])
    def test_recent_expired_tiny_missing(self):
        self.assertEqual(choose([a(i,hours=24+i) for i in range(8)],NOW),[])
        self.assertEqual(choose([a(i,expired=True) for i in range(8)],NOW),[])
        self.assertEqual(choose([a(i,mb=0.01) for i in range(8)],NOW),[])
        rows=[a(i) for i in range(8)]
        for r in rows:r["workflow_run"].pop("id")
        self.assertEqual(choose(rows,NOW),[])
    def test_limit(self):
        self.assertEqual(len(choose([a(i,hours=300+i) for i in range(80)],NOW)),MAX_DELETE)
if __name__=="__main__":unittest.main()
