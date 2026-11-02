# HVH targeting sample

This is a configurable Windows console program based on the 32-bit memory
layout in the supplied C++ sample. It scans the configured entity list,
computes pitch and yaw, and writes the selected angles to the configured
view-angle fields. It starts paused; press **T** to toggle and **End** to exit.

The included `hvh_targeting.ini` contains the original, unverified offsets.
Update the window title, process name, modules, offsets, pointer width, and
position conventions for the exact build of your own game before using it.
The program cannot infer these values. Setting `team_offset=0` disables team
filtering and allows any live entity to be selected. A nonzero team offset
excludes entities whose team value equals the player's.

The EXE reads `hvh_targeting.ini` next to itself, or accepts a config path as
its sole argument. The file is UTF-8. Hex integers use a `0x` prefix. Unknown,
missing, duplicate, and malformed keys cause an error. A 64-bit EXE can read
4-byte or 8-byte target pointers; a 32-bit EXE supports only 4-byte pointers.
Run `hvh_targeting.exe --check-config` to validate the file without accessing
game memory.
In the packaged Windows ZIP, double-click `Check-Configuration.bat` to check
the file, then `Run-HVH-Targeting.bat` to launch the appropriate EXE and keep
the console open for status or error messages.
`selection=distance` chooses the nearest entity. `selection=view_error`
chooses the smallest angular difference from the current view. The distance
and view-error limits apply in both modes.

The process executable name and module names are checked before scanning.
Failed reads skip that frame or entity; failed writes stop the program. Status
is printed at most once every two seconds. The process handle closes on exit.

## Build and test

On Linux with GCC, run the math checks:

```sh
g++ -std=c++17 -Wall -Wextra -Wpedantic -Werror -Isrc tests/targeting_math_test.cpp -o /tmp/targeting_math_test
/tmp/targeting_math_test
```

On Windows with CMake and a C++17 compiler:

```sh
cmake -S . -B build
cmake --build build --config Release
ctest --test-dir build --output-on-failure
```

To cross-compile a 64-bit Windows EXE with MinGW-w64 on Linux:

```sh
x86_64-w64-mingw32-g++ -std=c++17 -O2 -Wall -Wextra -Wpedantic -Werror -municode -static -static-libgcc -static-libstdc++ -Isrc src/main.cpp -o hvh_targeting.exe
```

To package both architectures after building them, run
`python3 package_release.py --win64 <64-bit-exe> --win32 <32-bit-exe> --output <zip-path>`.
The ZIP includes the configuration, launcher scripts, source, and SHA-256 hashes.

The sample has no live game integration test because its memory layout is
unverified. A successful compile and math test do not establish that a given
game build uses these offsets or angle conventions.
