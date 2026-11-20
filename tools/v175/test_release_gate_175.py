#!/usr/bin/env python3
"""Host regression tests for release evidence requirements."""
import unittest
from release_gate_175 import REQUIRED, evaluate


def sample():
    return {
        "versionName": "1.0.75", "versionCode": 101,
        "checks": {
            key: {"status": "passed", "evidence_url": "https://github.com/example/evidence",
                  "tested_at": "2026-11-20", "tester": "QA"}
            for key in REQUIRED
        },
    }


class GateTests(unittest.TestCase):
    def test_all_evidence_present(self):
        self.assertEqual([], evaluate(sample()))

    def test_missing_hardware_evidence_blocks(self):
        item = sample()
        del item["checks"]["physical_phone_camera_ocr"]
        self.assertTrue(evaluate(item))

    def test_not_tested_is_not_passed(self):
        item = sample()
        item["checks"]["arena_two_accounts"]["status"] = "pending"
        self.assertTrue(evaluate(item))

    def test_empty_evidence_link_blocks(self):
        item = sample()
        item["checks"]["existing_174_user_data"]["evidence_url"] = ""
        self.assertTrue(evaluate(item))

    def test_wrong_version_blocks(self):
        item = sample()
        item["versionCode"] = 102
        with self.assertRaises(ValueError):
            evaluate(item)

    def test_missing_tester_blocks(self):
        item = sample()
        item["checks"]["responsive_manual_visual"]["tester"] = ""
        self.assertTrue(evaluate(item))


if __name__ == "__main__":
    unittest.main()
