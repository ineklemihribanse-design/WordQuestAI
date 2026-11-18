#!/usr/bin/env python3
"""Fail-closed v0.7.5 -> v0.8.0 APK update gate.

Validates binary AXML package/version, APK v2/v3 signing certificate, and
(using the SDK's apksigner) the actual cryptographic signatures. No secrets
are extracted from the APK. A matching certificate alone is not a private key.
"""
from __future__ import annotations
import argparse
import hashlib
import os
from pathlib import Path
import shutil
import struct
import subprocess
import sys
import zipfile

EXPECTED = {
    "package": "com.kelimebulmacasi.a1.practice.test",
    "old_version": 12,
    "new_version": 13,
    "old_certificate_sha256": "88E25B3AD209D33F7052C29A939588C6C8E898D6398468C2A2E0BB78878BD19C",
}

def chunk(data: bytes, i: int) -> tuple[bytes, int]:
    if i + 4 > len(data): raise ValueError("Invalid APK signing data")
    length = struct.unpack_from("<I", data, i)[0]
    if i + 4 + length > len(data): raise ValueError("Invalid APK signing length")
    return data[i + 4:i + 4 + length], i + 4 + length

def certificate_hash(apk: Path) -> str:
    with apk.open("rb") as f:
        f.seek(0, 2)
        size = f.tell()
        f.seek(max(0, size - 65557))
        tail = f.read()
        eocd = tail.rfind(b"PK\x05\x06")
        if eocd < 0: raise ValueError("ZIP central directory missing")
        start = struct.unpack_from("<I", tail, eocd + 16)[0]
        f.seek(start - 24)
        block_size, magic = struct.unpack("<Q16s", f.read(24))
        if magic != b"APK Sig Block 42": raise ValueError("APK Signing Block v2/v3 missing")
        f.seek(start - 8 - block_size)
        signed = f.read(block_size + 8)
    pos = 8
    while pos < len(signed) - 24:
        pair_len = struct.unpack_from("<Q", signed, pos)[0]
        pair_type = struct.unpack_from("<I", signed, pos + 8)[0]
        if pair_len < 4 or pos + 8 + pair_len > len(signed):
            raise ValueError("Invalid APK signature section")
        payload = signed[pos + 12:pos + 8 + pair_len]
        if pair_type in (0x7109871a, 0xf05368c0):
            signers, _ = chunk(payload, 0)
            signer, _ = chunk(signers, 0)
            signed_data, _ = chunk(signer, 0)
            _, offset = chunk(signed_data, 0)  # digest records
            certificates, _ = chunk(signed_data, offset)
            certificate, _ = chunk(certificates, 0)
            return hashlib.sha256(certificate).hexdigest().upper()
        pos += 8 + pair_len
    raise ValueError("APK v2/v3 signer not found")

def pool_strings(data: bytes, start: int) -> list[str]:
    count, _, flags, string_start, _ = struct.unpack_from("<IIIII", data, start+8)
    utf8 = bool(flags & 0x100)
    base = start + string_start
    values: list[str] = []
    def variable_int(loc: int) -> tuple[int, int]:
        n = data[loc]
        if n & 0x80: return ((n & 0x7f) << 7) | data[loc + 1], loc + 2
        return n, loc + 1
    for n in range(count):
        offset = struct.unpack_from("<I", data, start + 28 + n * 4)[0]
        loc = base + offset
        if utf8:
            _, loc = variable_int(loc)
            length, loc = variable_int(loc)
            value = data[loc:loc+length].decode("utf-8")
        else:
            size = struct.unpack_from("<H", data, loc)[0]
            loc += 2
            if size & 0x8000:
                size = ((size & 0x7fff) << 16) | struct.unpack_from("<H", data, loc)[0]
                loc += 2
            value = data[loc:loc+2*size].decode("utf-16le")
        values.append(value)
    return values

def manifest_attrs(apk: Path) -> dict[str, str]:
    with zipfile.ZipFile(apk) as archive:
        data = archive.read("AndroidManifest.xml")
    strings: list[str] = []
    offset = 8
    while offset + 8 <= len(data):
        kind, _, size = struct.unpack_from("<HHI", data, offset)
        if size <= 0: break
        if kind == 0x0001:
            strings = pool_strings(data, offset)
        elif kind == 0x0102:
            name_index = struct.unpack_from("<I", data, offset + 20)[0]
            if strings[name_index] == "manifest":
                attr_start, attr_size, count = struct.unpack_from("<HHH", data, offset+24)
                result = {}
                for n in range(count):
                    pos = offset + 16 + attr_start + n * attr_size
                    _, attr_name, raw = struct.unpack_from("<III", data, pos)
                    _, _, dtype, val = struct.unpack_from("<HBBI", data, pos + 12)
                    value = strings[raw] if raw != 0xffffffff else (str(val) if dtype in (0x10, 0x11) else f"0x{val:x}")
                    result[strings[attr_name]] = value
                return result
        offset += size
    raise ValueError("AndroidManifest manifest node missing")

def find_apksigner() -> str | None:
    direct = shutil.which("apksigner") or shutil.which("apksigner.bat")
    if direct: return direct
    for var in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        sdk = os.getenv(var)
        if sdk:
            for path in sorted((Path(sdk)/"build-tools").glob("*/apksigner*"), reverse=True):
                if path.is_file() and path.suffix in ("", ".bat"): return str(path)
    return None

def check(old: Path, new: Path, *, metadata_only: bool) -> int:
    failures = []
    info_old, info_new = manifest_attrs(old), manifest_attrs(new)
    old_cert, new_cert = certificate_hash(old), certificate_hash(new)
    print(f"OLD: {info_old.get('package')} versionCode={info_old.get('versionCode')} cert={old_cert}")
    print(f"NEW: {info_new.get('package')} versionCode={info_new.get('versionCode')} cert={new_cert}")
    if info_old.get("package") != EXPECTED["package"]: failures.append("Provided old APK is not the intended v0.7.5 .test package")
    if info_new.get("package") != EXPECTED["package"]: failures.append("New package name differs: would not upgrade in place")
    if int(info_old.get("versionCode", "0")) != EXPECTED["old_version"]: failures.append("Unexpected old versionCode")
    if int(info_new.get("versionCode", "0")) < EXPECTED["new_version"]: failures.append("New versionCode must be at least 13")
    if int(info_new.get("versionCode", "0")) <= int(info_old.get("versionCode", "0")): failures.append("New versionCode must exceed old")
    if old_cert != EXPECTED["old_certificate_sha256"]: failures.append("Old APK certificate is not the supplied reference certificate")
    if old_cert != new_cert: failures.append("New APK uses a DIFFERENT signing key, update would fail")
    signer = find_apksigner()
    if metadata_only:
        print("NOT VERIFIED: --metadata-only skips cryptographic APK signature validation")
    elif not signer:
        failures.append("Android SDK apksigner missing; cannot cryptographically verify signed APKs")
    else:
        for path in (old, new):
            outcome = subprocess.run([signer, "verify", "--verbose", "--print-certs", str(path)], capture_output=True, text=True)
            if outcome.returncode != 0:
                failures.append(f"Cryptographic signature verification failed: {path.name}: {outcome.stderr.strip()}")
    for failure in failures: print("FAILED:", failure, file=sys.stderr)
    if failures: return 1
    print("METADATA MATCH (not a device upgrade test)" if metadata_only else "APK SIGNATURE/PACKAGE/VERSION MATCH (device upgrade still required)")
    return 0

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("old_apk", type=Path)
    parser.add_argument("new_apk", type=Path)
    parser.add_argument("--metadata-only", action="store_true", help="For diagnostics only; never call a passing result installation-safe")
    args = parser.parse_args()
    try: sys.exit(check(args.old_apk, args.new_apk, metadata_only=args.metadata_only))
    except Exception as problem:
        print(f"FAILED: {problem}", file=sys.stderr)
        sys.exit(2)
