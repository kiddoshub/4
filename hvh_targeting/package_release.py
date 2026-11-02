#!/usr/bin/env python3
"""Create a verified, repeatable Windows package from prebuilt EXEs."""

import argparse
import hashlib
import os
from pathlib import Path
import tempfile
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo


ROOT = Path(__file__).resolve().parent
SOURCE_FILES = (
    "README.md",
    "hvh_targeting.ini",
    "Run-HVH-Targeting.bat",
    "Check-Configuration.bat",
    "CMakeLists.txt",
    "src/main.cpp",
    "src/targeting_math.hpp",
    "tests/targeting_math_test.cpp",
    "package_release.py",
)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--win64", type=Path, required=True)
    parser.add_argument("--win32", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    files = {
        "HVH-Targeting-Win64.exe": args.win64,
        "HVH-Targeting-Win32.exe": args.win32,
        "README.md": ROOT / "README.md",
        "hvh_targeting.ini": ROOT / "hvh_targeting.ini",
        "Run-HVH-Targeting.bat": ROOT / "Run-HVH-Targeting.bat",
        "Check-Configuration.bat": ROOT / "Check-Configuration.bat",
    }
    files.update({f"source/{name}": ROOT / name for name in SOURCE_FILES})
    for name in ("HVH-Targeting-Win64.exe", "HVH-Targeting-Win32.exe"):
        if not files[name].read_bytes().startswith(b"MZ"):
            parser.error(f"{files[name]} is not a Windows executable")

    contents = {name: path.read_bytes() for name, path in files.items()}
    checksums = "".join(
        f"{hashlib.sha256(data).hexdigest()}  {name}\n"
        for name, data in contents.items()
    ).encode("ascii")
    contents["SHA256SUMS"] = checksums

    args.output.parent.mkdir(parents=True, exist_ok=True)
    fd, temp_name = tempfile.mkstemp(prefix=".hvh-package-",
                                      suffix=".zip", dir=args.output.parent)
    os.close(fd)
    try:
        with ZipFile(temp_name, "w", compression=ZIP_DEFLATED,
                     compresslevel=9) as archive:
            for name, data in contents.items():
                info = ZipInfo(name, (1980, 1, 1, 0, 0, 0))
                info.compress_type = ZIP_DEFLATED
                info.external_attr = 0o644 << 16
                archive.writestr(info, data, compress_type=ZIP_DEFLATED,
                                 compresslevel=9)
        with ZipFile(temp_name) as archive:
            if archive.testzip() is not None:
                raise RuntimeError("Archive checksum validation failed")
            for name, data in contents.items():
                if archive.read(name) != data:
                    raise RuntimeError(f"Archive content mismatch: {name}")
        os.replace(temp_name, args.output)
    finally:
        if os.path.exists(temp_name):
            os.unlink(temp_name)
    print(f"Packaged {len(contents)} files: {args.output}")
    print(f"SHA256 {hashlib.sha256(args.output.read_bytes()).hexdigest()}")


if __name__ == "__main__":
    main()
