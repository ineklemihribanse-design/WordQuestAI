#!/usr/bin/env python3
"""Host-only regression tests for Android upgrade continuity gate."""
import unittest
from verify_upgrade_metadata_175 import package_snapshot, verify_upgrade

PKG = "com.kelimebulmacasi.a1.practice.fresh"


def snapshot(version, uid=10174, first="2026-11-19 12:30:00",
             data="/data/user/0/" + PKG, camera="true"):
    permission_line = (
        f"      android.permission.CAMERA: granted={camera}, flags=[ USER_SET ]\n"
        if camera is not None else ""
    )
    return (
        f"Package [{PKG}] (abc):\n"
        f"    versionCode={version} minSdk=24 targetSdk=35\n"
        f"    userId={uid}\n"
        f"    firstInstallTime={first}\n"
        f"    lastUpdateTime=2026-11-20 17:00:00\n"
        f"    dataDir={data}\n"
        f"    runtime permissions:\n{permission_line}"
    )


class VerifyUpgradeMetadataTest(unittest.TestCase):
    def test_normal_in_place_upgrade(self):
        result = verify_upgrade(snapshot(100), snapshot(101), PKG)
        self.assertTrue(result["same_uid"])
        self.assertTrue(result["same_initial_install"])
        self.assertTrue(result["same_data_dir"])
        self.assertTrue(result["camera_grant_unchanged"])

    def test_uninstall_reinstall_fails_even_with_same_uid(self):
        with self.assertRaisesRegex(ValueError, "firstInstallTime changed"):
            verify_upgrade(snapshot(100),
                           snapshot(101, first="2026-11-20 16:00:00"), PKG)

    def test_user_identity_change_fails(self):
        with self.assertRaisesRegex(ValueError, "UID changed"):
            verify_upgrade(snapshot(100), snapshot(101, uid=10201), PKG)

    def test_wrong_version_fails(self):
        with self.assertRaisesRegex(ValueError, "100 -> 101"):
            verify_upgrade(snapshot(100), snapshot(102), PKG)

    def test_changed_data_directory_fails(self):
        with self.assertRaisesRegex(ValueError, "data directory changed"):
            verify_upgrade(snapshot(100),
                           snapshot(101, data="/data/user/10/" + PKG), PKG)

    def test_lost_camera_permission_fails(self):
        with self.assertRaisesRegex(ValueError, "camera grant state changed"):
            verify_upgrade(snapshot(100), snapshot(101, camera="false"), PKG)

    def test_absent_camera_permission_is_not_faked(self):
        report = verify_upgrade(snapshot(100, camera=None),
                                snapshot(101, camera=None), PKG)
        self.assertIsNone(report["camera_grant_unchanged"])

    def test_missing_package_fails(self):
        with self.assertRaisesRegex(ValueError, "Package identity missing"):
            package_snapshot(snapshot(100), "invalid.example")


if __name__ == "__main__":
    unittest.main()
