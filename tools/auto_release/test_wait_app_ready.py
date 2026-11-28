import unittest
from wait_app_ready import ready,app_failure
P="com.kelimebulmacasi.a1.practice.fresh"
class ReadyTests(unittest.TestCase):
    def test_target(self):
        for prefix in ("topResumedActivity=","mResumedActivity: ","ResumedActivity: "):
            self.assertEqual(ready("123\n",prefix+"ActivityRecord{abc u0 "+P+"/.MainActivity t1}",P),"123")
    def test_other_app(self):
        self.assertIsNone(ready("123","mResumedActivity: ActivityRecord{abc u0 com.android.launcher3/.Launcher}",P))
        self.assertIsNone(ready("123","mResumedActivity: ActivityRecord{abc u0 "+P+".other/.MainActivity}",P))
    def test_invalid_pid(self):
        self.assertIsNone(ready("","mResumedActivity: "+P+"/.MainActivity",P))
        self.assertIsNone(ready("abc","mResumedActivity: "+P+"/.MainActivity",P))
    def test_crash_and_anr(self):
        self.assertTrue(app_failure("Process: "+P+", PID: 123",P))
        self.assertTrue(app_failure("ANR in "+P+" (activity)",P))
        self.assertFalse(app_failure("ANR in com.android.launcher3",P))
if __name__=="__main__":unittest.main()
