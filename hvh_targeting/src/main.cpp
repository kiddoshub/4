#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <tlhelp32.h>

#include "targeting_math.hpp"

#include <algorithm>
#include <chrono>
#include <cstdint>
#include <cwchar>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <iterator>
#include <limits>
#include <map>
#include <optional>
#include <stdexcept>
#include <string>

namespace {

using Clock = std::chrono::steady_clock;

struct Config {
    std::wstring windowTitle, processName, clientModule, engineModule;
    unsigned pointerWidth, entityCount, entityStride, pollMs;
    std::uintptr_t localPlayer, engineState, entityList, health, dormant;
    std::uintptr_t x, y, z, yaw, crouch, team, writePitch, writeYaw;
    int playerCrouchThreshold, targetCrouchThreshold;
    float playerCrouchYOffset, targetCrouchYOffset, targetStandYOffset;
    float targetForwardDistance, maxDistance, maxViewError;
    hvh::Selection selection;
};

class Handle {
public:
    explicit Handle(HANDLE value = nullptr) : value_(value) {}
    ~Handle() { if (value_ && value_ != INVALID_HANDLE_VALUE) CloseHandle(value_); }
    Handle(const Handle&) = delete;
    Handle& operator=(const Handle&) = delete;
    HANDLE get() const { return value_; }
    bool valid() const { return value_ && value_ != INVALID_HANDLE_VALUE; }
private:
    HANDLE value_;
};

std::wstring toWide(const std::string& utf8) {
    if (utf8.empty()) return {};
    const int size = MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS,
                                         utf8.data(), static_cast<int>(utf8.size()),
                                         nullptr, 0);
    if (!size) throw std::runtime_error("Configuration contains invalid UTF-8");
    std::wstring result(static_cast<std::size_t>(size), L'\0');
    if (!MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, utf8.data(),
                             static_cast<int>(utf8.size()), result.data(), size))
        throw std::runtime_error("Could not decode configuration text");
    return result;
}

std::string trim(std::string value) {
    const auto first = value.find_first_not_of(" \t\r\n");
    if (first == std::string::npos) return {};
    const auto last = value.find_last_not_of(" \t\r\n");
    return value.substr(first, last - first + 1);
}

Config loadConfig(const std::filesystem::path& path) {
    std::ifstream input(path);
    if (!input) throw std::runtime_error("Could not open configuration file");
    std::map<std::string, std::string> entries;
    std::string line;
    std::size_t lineNumber = 0;
    while (std::getline(input, line)) {
        ++lineNumber;
        if (lineNumber == 1 && line.compare(0, 3, "\xEF\xBB\xBF") == 0)
            line.erase(0, 3);
        line = trim(line);
        if (line.empty() || line[0] == '#' || line[0] == ';') continue;
        const auto separator = line.find('=');
        if (separator == std::string::npos || separator == 0)
            throw std::runtime_error("Invalid configuration line " +
                                     std::to_string(lineNumber));
        const std::string key = trim(line.substr(0, separator));
        const std::string value = trim(line.substr(separator + 1));
        if (key.empty() || !entries.emplace(key, value).second)
            throw std::runtime_error("Duplicate or empty key on line " +
                                     std::to_string(lineNumber));
    }
    if (!input.eof()) throw std::runtime_error("Could not finish reading configuration");

    const auto take = [&](const char* key) {
        const auto it = entries.find(key);
        if (it == entries.end())
            throw std::runtime_error(std::string("Missing setting: ") + key);
        const std::string value = it->second;
        entries.erase(it);
        return value;
    };
    const auto number = [&](const char* key, std::uint64_t minimum,
                            std::uint64_t maximum) {
        const std::string value = take(key);
        std::size_t used = 0;
        std::uint64_t parsed = 0;
        const int base = value.size() > 2 && value[0] == '0' &&
            (value[1] == 'x' || value[1] == 'X') ? 16 : 10;
        try { parsed = std::stoull(value, &used, base); }
        catch (const std::exception&) {
            throw std::runtime_error(std::string("Invalid number for ") + key);
        }
        if (used != value.size() || parsed < minimum || parsed > maximum ||
            (!value.empty() && value[0] == '-'))
            throw std::runtime_error(std::string("Out-of-range number for ") + key);
        return parsed;
    };
    const auto real = [&](const char* key, float minimum, float maximum) {
        const std::string value = take(key);
        std::size_t used = 0;
        float parsed = 0;
        try { parsed = std::stof(value, &used); }
        catch (const std::exception&) {
            throw std::runtime_error(std::string("Invalid decimal for ") + key);
        }
        if (used != value.size() || !std::isfinite(parsed) ||
            parsed < minimum || parsed > maximum)
            throw std::runtime_error(std::string("Out-of-range decimal for ") + key);
        return parsed;
    };
    Config cfg{};
    cfg.windowTitle = toWide(take("window_title"));
    cfg.processName = toWide(take("process_name"));
    cfg.clientModule = toWide(take("client_module"));
    cfg.engineModule = toWide(take("engine_module"));
    if (cfg.windowTitle.empty() || cfg.clientModule.empty() ||
        cfg.engineModule.empty())
        throw std::runtime_error("Window title and module names cannot be empty");
    cfg.pointerWidth = static_cast<unsigned>(number("pointer_width", 4, 8));
    if ((cfg.pointerWidth != 4 && cfg.pointerWidth != 8) ||
        cfg.pointerWidth > sizeof(void*))
        throw std::runtime_error("Pointer width must be 4 or 8 and fit this EXE");
    cfg.entityCount = static_cast<unsigned>(number("entity_count", 1, 4096));
    cfg.entityStride = static_cast<unsigned>(number("entity_stride", 1, 4096));
    cfg.pollMs = static_cast<unsigned>(number("poll_ms", 5, 1000));
    const auto address = [&](const char* key) {
        return static_cast<std::uintptr_t>(number(key, 0,
            std::numeric_limits<std::uintptr_t>::max()));
    };
    cfg.localPlayer = address("local_player_offset");
    cfg.engineState = address("engine_state_offset");
    cfg.entityList = address("entity_list_offset");
    cfg.health = address("health_offset");
    cfg.dormant = address("dormant_offset");
    cfg.x = address("x_offset");
    cfg.y = address("y_offset");
    cfg.z = address("z_offset");
    cfg.yaw = address("yaw_offset");
    cfg.crouch = address("crouch_offset");
    cfg.team = address("team_offset");
    cfg.writePitch = address("write_pitch_offset");
    cfg.writeYaw = address("write_yaw_offset");
    cfg.playerCrouchThreshold = static_cast<int>(number("player_crouch_threshold", 0, 1000000));
    cfg.targetCrouchThreshold = static_cast<int>(number("target_crouch_threshold", 0, 1000000));
    cfg.playerCrouchYOffset = real("player_crouch_y_offset", -1000, 1000);
    cfg.targetCrouchYOffset = real("target_crouch_y_offset", -1000, 1000);
    cfg.targetStandYOffset = real("target_stand_y_offset", -1000, 1000);
    cfg.targetForwardDistance = real("target_forward_distance", 0, 1000);
    cfg.maxDistance = real("max_distance", 0.001f, 1000000);
    cfg.maxViewError = real("max_view_error", 0, 360);
    const std::string selection = take("selection");
    if (selection == "distance") cfg.selection = hvh::Selection::distance;
    else if (selection == "view_error") cfg.selection = hvh::Selection::viewError;
    else throw std::runtime_error("selection must be distance or view_error");
    if (!entries.empty())
        throw std::runtime_error("Unknown setting: " + entries.begin()->first);
    return cfg;
}

std::uintptr_t moduleBase(DWORD pid, const std::wstring& name) {
    Handle snapshot(CreateToolhelp32Snapshot(TH32CS_SNAPMODULE |
                                               TH32CS_SNAPMODULE32, pid));
    if (!snapshot.valid()) return 0;
    MODULEENTRY32W entry{};
    entry.dwSize = sizeof(entry);
    if (!Module32FirstW(snapshot.get(), &entry)) return 0;
    do {
        if (_wcsicmp(entry.szModule, name.c_str()) == 0)
            return reinterpret_cast<std::uintptr_t>(entry.modBaseAddr);
    } while (Module32NextW(snapshot.get(), &entry));
    return 0;
}

template <typename T>
std::optional<T> read(HANDLE process, std::uintptr_t address) {
    T value{};
    SIZE_T bytes = 0;
    if (!ReadProcessMemory(process, reinterpret_cast<LPCVOID>(address),
                           &value, sizeof(value), &bytes) || bytes != sizeof(value))
        return std::nullopt;
    return value;
}

std::optional<std::uintptr_t> readPointer(HANDLE process, std::uintptr_t address,
                                           unsigned width) {
    if (width == 4) {
        const auto value = read<std::uint32_t>(process, address);
        if (value) return *value;
    } else {
        const auto value = read<std::uint64_t>(process, address);
        if (value && *value <= std::numeric_limits<std::uintptr_t>::max())
            return static_cast<std::uintptr_t>(*value);
    }
    return std::nullopt;
}

std::optional<hvh::Vec3> position(HANDLE process, std::uintptr_t base,
                                   const Config& cfg) {
    const auto x = read<float>(process, base + cfg.x);
    const auto y = read<float>(process, base + cfg.y);
    const auto z = read<float>(process, base + cfg.z);
    if (!x || !y || !z) return std::nullopt;
    const hvh::Vec3 point{*x, *y, *z};
    return hvh::finite(point) ? std::optional<hvh::Vec3>(point) : std::nullopt;
}

std::optional<hvh::Candidate> scan(HANDLE process, std::uintptr_t client,
                                    std::uintptr_t state, std::uintptr_t player,
                                    const Config& cfg) {
    const auto playerPosition = position(process, player, cfg);
    const auto playerCrouch = read<int>(process, player + cfg.crouch);
    const auto viewPitch = read<float>(process, state + cfg.writePitch);
    const auto viewYaw = read<float>(process, state + cfg.writeYaw);
    if (!playerPosition || !playerCrouch || !viewPitch || !viewYaw)
        return std::nullopt;
    auto eye = *playerPosition;
    if (*playerCrouch > cfg.playerCrouchThreshold)
        eye.y += cfg.playerCrouchYOffset;
    const hvh::Angles view{*viewPitch, *viewYaw};
    if (!hvh::finite(eye) || !std::isfinite(view.pitch) ||
        !std::isfinite(view.yaw)) return std::nullopt;
    std::optional<int> playerTeam;
    if (cfg.team != 0) {
        playerTeam = read<int>(process, player + cfg.team);
        if (!playerTeam) return std::nullopt;
    }

    std::optional<hvh::Candidate> best;
    for (unsigned index = 1; index < cfg.entityCount; ++index) {
        const auto entity = readPointer(process, client + cfg.entityList +
            static_cast<std::uintptr_t>(index) * cfg.entityStride,
            cfg.pointerWidth);
        if (!entity || !*entity || *entity == player) continue;
        const auto dormant = read<std::uint8_t>(process, *entity + cfg.dormant);
        const auto health = read<int>(process, *entity + cfg.health);
        if (!dormant || *dormant || !health || *health <= 0) continue;
        if (playerTeam) {
            const auto entityTeam = read<int>(process, *entity + cfg.team);
            if (!entityTeam || *entityTeam == *playerTeam) continue;
        }
        const auto targetPosition = position(process, *entity, cfg);
        const auto targetCrouch = read<int>(process, *entity + cfg.crouch);
        const auto targetYaw = read<float>(process, *entity + cfg.yaw);
        if (!targetPosition || !targetCrouch || !targetYaw) continue;
        const float yOffset = *targetCrouch > cfg.targetCrouchThreshold ?
            cfg.targetCrouchYOffset : cfg.targetStandYOffset;
        const auto aimPoint = hvh::projectedAimPoint(*targetPosition, *targetYaw,
                                                     cfg.targetForwardDistance, yOffset);
        if (!aimPoint) continue;
        const auto candidate = hvh::makeCandidate(*entity, eye, *aimPoint, view);
        if (candidate && hvh::isPreferred(*candidate, best, cfg.selection,
                                          cfg.maxDistance, cfg.maxViewError))
            best = candidate;
    }
    return best;
}

bool writeAngles(HANDLE process, std::uintptr_t state,
                 const Config& cfg, hvh::Angles angles) {
    SIZE_T bytes = 0;
    if (cfg.writeYaw == cfg.writePitch + sizeof(float)) {
        struct Pair { float pitch, yaw; } pair{angles.pitch, angles.yaw};
        if (!WriteProcessMemory(process,
            reinterpret_cast<LPVOID>(state + cfg.writePitch),
            &pair, sizeof(pair), &bytes)) return false;
        if (bytes == sizeof(pair)) return true;
        SetLastError(ERROR_PARTIAL_COPY);
        return false;
    }
    const auto writeOne = [&](std::uintptr_t offset, float value) {
        bytes = 0;
        if (!WriteProcessMemory(process,
            reinterpret_cast<LPVOID>(state + offset),
            &value, sizeof(value), &bytes)) return false;
        if (bytes == sizeof(value)) return true;
        SetLastError(ERROR_PARTIAL_COPY);
        return false;
    };
    return writeOne(cfg.writePitch, angles.pitch) &&
           writeOne(cfg.writeYaw, angles.yaw);
}

bool processNameMatches(HANDLE process, const std::wstring& expected) {
    if (expected.empty()) return true;
    wchar_t path[32768]{};
    DWORD length = static_cast<DWORD>(std::size(path));
    if (!QueryFullProcessImageNameW(process, 0, path, &length)) return false;
    return _wcsicmp(std::filesystem::path(path).filename().c_str(),
                    expected.c_str()) == 0;
}

} // namespace

int wmain(int argc, wchar_t** argv) {
    try {
        const bool checkOnly = argc > 1 &&
            std::wcscmp(argv[1], L"--check-config") == 0;
        if (argc > (checkOnly ? 3 : 2) ||
            (argc > 1 && std::wcscmp(argv[1], L"--help") == 0)) {
            std::cout << "Usage: hvh_targeting.exe [configuration.ini]\n"
                      << "       hvh_targeting.exe --check-config [configuration.ini]\n";
            if (argc == 2 && std::wcscmp(argv[1], L"--help") == 0) return 0;
            return 2;
        }
        std::filesystem::path configPath;
        if (argc == (checkOnly ? 3 : 2))
            configPath = argv[checkOnly ? 2 : 1];
        else {
            wchar_t exePath[32768]{};
            const DWORD length = GetModuleFileNameW(nullptr, exePath,
                                                     static_cast<DWORD>(std::size(exePath)));
            if (!length || length == std::size(exePath))
                throw std::runtime_error("Could not locate the executable directory");
            configPath = std::filesystem::path(exePath).parent_path() /
                         L"hvh_targeting.ini";
        }
        const Config cfg = loadConfig(configPath);
        if (checkOnly) {
            std::cout << "Configuration valid: " << cfg.entityCount
                      << " entity slots, " << cfg.pointerWidth
                      << "-byte pointers. No game memory was accessed.\n";
            return 0;
        }
        const HWND window = FindWindowW(nullptr, cfg.windowTitle.c_str());
        if (!window) throw std::runtime_error("Configured game window was not found");
        DWORD pid = 0;
        GetWindowThreadProcessId(window, &pid);
        if (!pid) throw std::runtime_error("Could not find the game process");
        Handle process(OpenProcess(PROCESS_VM_READ | PROCESS_VM_WRITE |
            PROCESS_VM_OPERATION | PROCESS_QUERY_LIMITED_INFORMATION |
            SYNCHRONIZE, FALSE, pid));
        if (!process.valid())
            throw std::runtime_error("OpenProcess failed (Windows error " +
                                     std::to_string(GetLastError()) + ")");
        if (!processNameMatches(process.get(), cfg.processName))
            throw std::runtime_error("Process executable does not match process_name");
        const auto client = moduleBase(pid, cfg.clientModule);
        const auto engine = moduleBase(pid, cfg.engineModule);
        if (!client || !engine)
            throw std::runtime_error("Configured client or engine module was not found");

        std::cout << "HVH targeting | PID " << pid << " | " << cfg.entityCount
                  << " entity slots | pointer width " << cfg.pointerWidth << "\n"
                  << "T: toggle targeting | End: exit | starts paused\n";
        if (!cfg.team)
            std::cout << "Team filtering is disabled (team_offset=0).\n";
        bool active = false;
        bool previousToggle = false;
        auto lastStatus = Clock::now() - std::chrono::seconds(3);
        while (IsWindow(window)) {
            const DWORD processState = WaitForSingleObject(process.get(), 0);
            if (processState == WAIT_OBJECT_0) break;
            if (processState == WAIT_FAILED)
                throw std::runtime_error("Process wait failed (Windows error " +
                                         std::to_string(GetLastError()) + ")");
            if (GetAsyncKeyState(VK_END) & 0x8000) break;
            const bool toggleDown = (GetAsyncKeyState('T') & 0x8000) != 0;
            if (toggleDown && !previousToggle) {
                active = !active;
                std::cout << (active ? "Targeting ON\n" : "Targeting OFF\n");
            }
            previousToggle = toggleDown;
            if (!active) { Sleep(cfg.pollMs); continue; }

            const auto player = readPointer(process.get(), client + cfg.localPlayer,
                                             cfg.pointerWidth);
            const auto state = readPointer(process.get(), engine + cfg.engineState,
                                            cfg.pointerWidth);
            const char* status = "waiting for player/engine state";
            if (player && *player && state && *state) {
                const auto target = scan(process.get(), client, *state, *player, cfg);
                status = target ? "target found" :
                    "no valid target (check entity and view offsets)";
                if (target && !writeAngles(process.get(), *state, cfg, target->aim))
                    throw std::runtime_error("View-angle write failed (Windows error " +
                                             std::to_string(GetLastError()) + ")");
            }
            const auto now = Clock::now();
            if (now - lastStatus >= std::chrono::seconds(2)) {
                std::cout << status << '\n';
                lastStatus = now;
            }
            Sleep(cfg.pollMs);
        }
        std::cout << "Stopped.\n";
        return 0;
    } catch (const std::exception& error) {
        std::cerr << "Error: " << error.what() << '\n';
        return 1;
    }
}
