#pragma once

#include <cmath>
#include <cstdint>
#include <optional>

namespace hvh {

struct Vec3 {
    float x;
    float y;
    float z;
};

struct Angles {
    float pitch;
    float yaw;
};

struct Candidate {
    std::uint64_t address;
    Angles aim;
    float distance;
    float viewError;
};

enum class Selection { distance, viewError };

inline constexpr float pi = 3.14159265358979323846f;

inline bool finite(Vec3 value) {
    return std::isfinite(value.x) && std::isfinite(value.y) &&
           std::isfinite(value.z);
}

inline float angleDelta(float a, float b) {
    return std::remainder(a - b, 360.0f);
}

inline std::optional<Angles> aimAngles(Vec3 from, Vec3 to) {
    if (!finite(from) || !finite(to)) return std::nullopt;
    const float dx = to.x - from.x;
    const float dy = to.y - from.y;
    const float dz = to.z - from.z;
    const float horizontal = std::hypot(dx, dz);
    if (!std::isfinite(horizontal) || horizontal < 0.001f)
        return std::nullopt;
    const float yaw = std::atan2(dz, dx) * 180.0f / pi;
    const float pitch = std::atan2(-dy, horizontal) * 180.0f / pi;
    if (!std::isfinite(yaw) || !std::isfinite(pitch)) return std::nullopt;
    return Angles{pitch, yaw};
}

inline std::optional<Vec3> projectedAimPoint(Vec3 position, float yaw,
                                              float forward, float yOffset) {
    if (!finite(position) || !std::isfinite(yaw) || std::fabs(yaw) > 36000.0f ||
        !std::isfinite(forward) || !std::isfinite(yOffset))
        return std::nullopt;
    const float radians = std::remainder(yaw, 360.0f) * pi / 180.0f;
    const Vec3 point{position.x + std::cos(radians) * forward,
                     position.y + yOffset,
                     position.z + std::sin(radians) * forward};
    if (!finite(point)) return std::nullopt;
    return point;
}

inline std::optional<Candidate> makeCandidate(std::uint64_t address,
                                               Vec3 playerEye, Vec3 aimPoint,
                                               Angles currentView) {
    const auto aim = aimAngles(playerEye, aimPoint);
    if (!aim || !std::isfinite(currentView.pitch) ||
        !std::isfinite(currentView.yaw) ||
        std::fabs(currentView.pitch) > 90.0f ||
        std::fabs(currentView.yaw) > 36000.0f)
        return std::nullopt;
    const float distance = std::hypot(std::hypot(aimPoint.x - playerEye.x,
                                                 aimPoint.z - playerEye.z),
                                      aimPoint.y - playerEye.y);
    const float viewError = std::hypot(angleDelta(aim->yaw, currentView.yaw),
                                       aim->pitch - currentView.pitch);
    if (!std::isfinite(distance) || !std::isfinite(viewError))
        return std::nullopt;
    return Candidate{address, *aim, distance, viewError};
}

inline bool isPreferred(const Candidate& next,
                        const std::optional<Candidate>& current,
                        Selection selection, float maxDistance,
                        float maxViewError) {
    if (next.distance <= 0 || next.distance > maxDistance ||
        next.viewError > maxViewError)
        return false;
    if (!current) return true;
    const float nextScore = selection == Selection::distance ?
        next.distance : next.viewError;
    const float oldScore = selection == Selection::distance ?
        current->distance : current->viewError;
    return nextScore < oldScore;
}

} // namespace hvh
