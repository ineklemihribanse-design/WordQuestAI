#!/usr/bin/env python3
"""No GitHub connection: pruning-policy regression tests."""
import unittest
from datetime import datetime, timezone, timedelta
from prune_actions_artifacts import choose, MAIN_AGE_HOURS, EXPERIMENT_AGE_HOURS

NOW = datetime(2026, 11, 18, 20, tzinfo=timezone.utc)

def a(num, branch, name="visual-qa", mb=32, hours_ago=72):
    return {"id":num,"created_at":(NOW-timedelta(hours=hours_ago)).isoformat(),
            "workflow_run":{"head_branch":branch},"name":name,
            "size_in_bytes":int(mb*1048576),"expired":False}

class CleanupSafety(unittest.TestCase):
    def test_protect_recent_three_main(self):
        d=[a(i,"main","WordQuestAI-Unsigned-APK",120,30+i*20) for i in range(1,7)]
        chosen=choose(d,NOW)
        self.assertEqual({x["id"] for x in chosen},{4,5,6})
    def test_protect_single_latest_per_experimental_branch(self):
        d=[a(i,"feature/teacher","preview",34,4+i*26) for i in range(1,6)]
        self.assertEqual({x["id"] for x in choose(d,NOW)},{2,3,4,5})
    def test_do_not_touch_recent_or_tiny_files(self):
        d=[a(1,"main",hours_ago=2), a(2,"feature/x",hours_ago=1),
           a(3,"feature/x",hours_ago=96,mb=.1),a(4,None,hours_ago=400)]
        self.assertEqual(choose(d,NOW),[])
    def test_keep_different_experiment_branch_previews(self):
        d=[a(1,"feature/a"),a(2,"feature/b"),a(3,"feature/c")]
        self.assertEqual(choose(d,NOW),[])
    def test_limit_newest_500_deletions(self):
        d=[a(i,"feature/mass",hours_ago=96+i) for i in range(700)]
        self.assertEqual(len(choose(d,NOW)),500)
        self.assertNotIn(0,{x["id"] for x in choose(d,NOW)})
    def test_empty_is_safe(self):
        self.assertEqual(choose([],NOW),[])
if __name__ == "__main__":
    unittest.main()
