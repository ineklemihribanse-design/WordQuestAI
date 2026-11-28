#!/usr/bin/env python3
"""Bounded foreground readiness gate for the actual Android package.
Require three successive checks of the same live process, resumed activity,
and no app-specific ANR/crash record. No fixed startup sleep.
"""
import argparse
import re
import subprocess
import time

def ready(pid, activities, package):
    pids=pid.strip().split()
    if len(pids)!=1 or not pids[0].isdigit():
        return None
    pattern=r"(?:topResumedActivity|mResumedActivity|ResumedActivity)\s*[:=].*?\b"+re.escape(package)+r"/"
    return pids[0] if re.search(pattern,activities) else None

def app_failure(log,package):
    # Match the target package, not arbitrary system services or launcher errors.
    p=re.escape(package)
    return bool(re.search(r"\b(?:Process:\s*"+p+r"(?=,|\s|$)|ANR in\s+"+p+r"(?=\s|$))",log))

def adb(*args):
    try:
        return subprocess.check_output(["adb",*args],stderr=subprocess.STDOUT,timeout=8,text=True)
    except (subprocess.SubprocessError,OSError):
        return ""

def wait(package,timeout=60):
    deadline=time.monotonic()+timeout
    previous=None
    streak=0
    while time.monotonic()<deadline:
        pid=ready(adb("shell","pidof","-s",package),
                  adb("shell","dumpsys","activity","activities"),package)
        if pid and pid==previous:
            streak+=1
        else:
            streak=1 if pid else 0
        previous=pid
        if streak>=3:
            if app_failure(adb("logcat","-d","-s","AndroidRuntime:E","ActivityManager:E"),package):
                raise RuntimeError("Target app crash or ANR in startup log")
            print("PASS: stable foreground process",package,pid,flush=True)
            return
        time.sleep(1)
    raise RuntimeError("Target app did not reach stable foreground: "+package)

if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("package")
    args=p.parse_args()
    wait(args.package)
