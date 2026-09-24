"""Build a complete, repeatable Windows download from an already built EXE."""

from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import tempfile
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo


ROOT = Path(__file__).resolve().parent
SUPPORT_FILES = (
    "README.md",
    "THIRD_PARTY_NOTICES.md",
    "licenses/go-LICENSE",
    "licenses/pdf-LICENSE",
    "licenses/robotstxt-LICENSE",
)


def package(executable: Path, destination: Path) -> None:
    binary = executable.read_bytes()
    if not binary.startswith(b"MZ"):
        raise ValueError(f"not a Windows executable: {executable}")

    files = {"Ladon.exe": binary}
    for name in SUPPORT_FILES:
        files[name] = (ROOT / name).read_bytes()
    checksums = "".join(
        f"{hashlib.sha256(data).hexdigest()}  {name}\n" for name, data in files.items()
    )
    files["SHA256SUMS.txt"] = checksums.encode("ascii")

    destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(
        dir=destination.parent, prefix=".ladon-package-", suffix=".zip", delete=False
    ) as temporary:
        temporary_path = Path(temporary.name)
    try:
        with ZipFile(temporary_path, "w") as archive:
            for name, data in files.items():
                info = ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
                info.compress_type = ZIP_DEFLATED
                info.external_attr = 0o644 << 16
                archive.writestr(info, data, compress_type=ZIP_DEFLATED, compresslevel=9)
        with ZipFile(temporary_path) as archive:
            if archive.testzip() is not None or any(archive.read(name) != data for name, data in files.items()):
                raise ValueError("release ZIP did not pass its integrity check")
        os.replace(temporary_path, destination)
    finally:
        temporary_path.unlink(missing_ok=True)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--exe", type=Path, default=ROOT / "Ladon.exe")
    parser.add_argument("--output", type=Path, default=ROOT / "Ladon-Windows.zip")
    arguments = parser.parse_args()
    package(arguments.exe, arguments.output)
    print(f"Packaged {arguments.output}")


if __name__ == "__main__":
    main()
