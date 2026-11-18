import sys
import unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from prune_public_releases import prune, REPOSITORY


def rel(version, rid, complete=True, draft=False):
    assets = [{"name": "WordQuestAI.apk", "state": "uploaded"}, {"name": "update.json", "state": "uploaded"}] if complete else []
    return {"tag_name": f"v1.0.{version}", "id": rid, "draft": draft, "prerelease": False, "assets": assets}

class CleanupRules(unittest.TestCase):
    def test_only_oldest_complete_releases_are_deleted(self):
        rows = [rel(15, 5), rel(18, 8), rel(16, 6), rel(19, 9), rel(17, 7), rel(20, 10, False)]
        seen = []
        def fetch(path):
            if path == f'/repos/{REPOSITORY}': return {'private': False}
            return rows
        results = prune('test', fetch=fetch, delete=seen.append)
        self.assertEqual(['v1.0.16', 'v1.0.15'], results)
        self.assertEqual(2, len(seen))
        self.assertTrue(all('/releases/' in v for v in seen))
    def test_no_prune_with_fewer_than_three_or_private_repository(self):
        def fetch(path): return {'private': False} if path.startswith('/repos/') and '?per_page' not in path else [rel(17,7)]
        self.assertEqual([], prune('test', fetch=fetch, delete=lambda _: self.fail('must not delete')))
        with self.assertRaises(RuntimeError):
            prune('test', fetch=lambda _: {'private': True})
    def test_dry_run_reports_without_deletion(self):
        rows=[rel(i,i) for i in range(10,16)]
        def fetch(path): return {'private': False} if '?' not in path else rows
        results=prune('test', dry_run=True, fetch=fetch, delete=lambda _: self.fail('dry run'))
        self.assertEqual(['v1.0.12','v1.0.11','v1.0.10'], results)

if __name__ == '__main__': unittest.main()