#!/usr/bin/env python3
"""Screen-visibility QA must reject blank screens even when PNG is valid."""
import tempfile
import unittest
from pathlib import Path
from PIL import Image, ImageDraw
from verify_emulator_screenshot_175 import inspect_png, verify_ui_content

class ResponsiveContentTest(unittest.TestCase):
    def test_valid_but_blank_screenshot_is_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            file=Path(d)/"blank.png"
            img=Image.new("RGB",(720,1280),(248,247,240))
            img.save(file)
            assert inspect_png(file)[0] == (720,1280)
            with self.assertRaisesRegex(ValueError,"empty"):
                verify_ui_content(file,(720,1280))

    def test_real_colored_home_content_is_accepted(self):
        with tempfile.TemporaryDirectory() as d:
            file=Path(d)/"home.png"
            img=Image.new("RGB",(720,1280),(248,247,240))
            painter=ImageDraw.Draw(img)
            painter.rounded_rectangle((40,220,680,770),radius=40,fill=(26,66,54))
            painter.rectangle((90,820,610,990),fill=(35,89,77))
            img.save(file)
            assert inspect_png(file)[0] == (720,1280)
            self.assertGreater(verify_ui_content(file,(720,1280)),0.07)

if __name__=="__main__":
    unittest.main()
