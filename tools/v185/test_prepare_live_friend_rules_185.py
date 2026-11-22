#!/usr/bin/env python3
"""Offline regression tests for the explicit-consent RTDB preview patch."""
import copy
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))
import prepare_live_friend_rules_185 as patch

def baseline():
    return {"rules": {
        "rooms": {"$room": {".read": False, ".write": False}},
        "users": {"$user": {".read": "auth.uid === $user"}},
        "friendProfiles": {"$uid": {
            ".read": patch.FRIEND_READ,
            ".write": "auth != null && auth.uid === $uid",
            ".validate": "newData.hasChildren(['avatar','points','completed','lastActiveAt'])",
            "$other": {".validate": False},
            "avatar": {".validate": "newData.isString()"},
        }},
    }}

class FriendPreviewOfflineRulesTests(unittest.TestCase):
    def test_only_one_preview_child_changes(self):
        old = baseline()
        old_copy = copy.deepcopy(old)
        new = patch.merged_live_rules(old)
        self.assertEqual(old, old_copy)
        self.assertEqual(new['rules']['friendProfiles']['$uid']['avatarPreview'],
                         patch.PREVIEW_RULE)
        stripped = copy.deepcopy(new)
        del stripped['rules']['friendProfiles']['$uid']['avatarPreview']
        self.assertEqual(stripped, old)

    def test_idempotent(self):
        new = patch.merged_live_rules(baseline())
        self.assertEqual(patch.merged_live_rules(new), new)

    def test_fail_closed_on_broad_read(self):
        old = baseline()
        old['rules']['friendProfiles']['$uid']['.read'] = 'true'
        with self.assertRaises(ValueError):
            patch.merged_live_rules(old)

    def test_fail_closed_on_writable_by_anyone(self):
        old = baseline()
        old['rules']['friendProfiles']['$uid']['.write'] = 'true'
        with self.assertRaises(ValueError):
            patch.merged_live_rules(old)

    def test_fail_closed_on_unknown_validation(self):
        old = baseline()
        old['rules']['friendProfiles']['$uid']['$other'] = {'.validate': True}
        with self.assertRaises(ValueError):
            patch.merged_live_rules(old)

    def test_fail_closed_on_changed_preview_policy(self):
        old = baseline()
        old['rules']['friendProfiles']['$uid']['avatarPreview'] = {'.validate': 'true'}
        with self.assertRaises(ValueError):
            patch.merged_live_rules(old)

    def test_cli_creates_new_file_without_touching_input(self):
        with tempfile.TemporaryDirectory() as d:
            source = Path(d) / 'live.json'
            output = Path(d) / 'safe.json'
            original = json.dumps(baseline(), ensure_ascii=False)
            source.write_text(original, encoding='utf8')
            cmd = [sys.executable, str(Path(__file__).with_name(
                'prepare_live_friend_rules_185.py')), '--live',
                str(source), '--output', str(output)]
            result = subprocess.run(cmd, check=True, capture_output=True, text=True)
            self.assertIn('No live Firebase deployment performed', result.stdout)
            self.assertEqual(source.read_text(encoding='utf8'), original)
            self.assertEqual(json.loads(output.read_text(encoding='utf8')),
                             patch.merged_live_rules(baseline()))
            second = subprocess.run(cmd, capture_output=True, text=True)
            self.assertNotEqual(second.returncode, 0)

if __name__ == '__main__':
    unittest.main()
