#include "targeting_math.hpp"

#include <cmath>
#include <cstdlib>
#include <iostream>
#include <limits>

namespace {
bool near(float a, float b) { return std::fabs(a - b) < 0.01f; }
void require(bool condition, const char* message) {
    if (!condition) {
        std::cerr << message << '\n';
        std::exit(1);
    }
}
}

int main() {
    using namespace hvh;
    const Vec3 origin{0, 0, 0};
    const auto east = aimAngles(origin, {10, 0, 0});
    const auto north = aimAngles(origin, {0, 0, 10});
    const auto west = aimAngles(origin, {-10, 0, 0});
    const auto south = aimAngles(origin, {0, 0, -10});
    const auto above = aimAngles(origin, {10, 10, 0});
    require(east && near(east->yaw, 0), "east yaw");
    require(north && near(north->yaw, 90), "north yaw");
    require(west && near(std::fabs(west->yaw), 180), "west yaw");
    require(south && near(south->yaw, -90), "south yaw");
    require(above && near(above->pitch, -45), "vertical pitch");
    require(!aimAngles(origin, {0, 10, 0}), "vertical-only target");
    require(!aimAngles(origin, {0, 0, 0}), "zero-distance target");
    require(!aimAngles(origin, {std::numeric_limits<float>::infinity(), 0, 0}),
            "nonfinite target");

    const auto projected = projectedAimPoint({1, 2, 3}, 90, 10, 2);
    require(projected && near(projected->x, 1) &&
            near(projected->y, 4) && near(projected->z, 13), "projected point");
    require(!projectedAimPoint(origin, 100000, 10, 0),
            "invalid target yaw rejected");
    require(near(angleDelta(-179, 179), 2), "wrapped yaw difference");

    const auto close = makeCandidate(1, origin, {10, 0, 0}, {0, 0});
    const auto far = makeCandidate(2, origin, {20, 0, 0}, {0, 0});
    const auto offAxis = makeCandidate(3, origin, {5, 0, 5}, {0, 0});
    require(close && far && offAxis, "valid candidates");
    require(!makeCandidate(4, origin, {10, 0, 0}, {100, 0}),
            "invalid current pitch rejected");
    require(isPreferred(*close, std::nullopt, Selection::distance, 100, 90),
            "first candidate selected");
    require(!isPreferred(*far, close, Selection::distance, 100, 90),
            "farther target rejected");
    require(!isPreferred(*offAxis, close, Selection::viewError, 100, 30),
            "off-axis target rejected");
    require(!isPreferred(*far, std::nullopt, Selection::distance, 15, 90),
            "distance limit");
    require(!isPreferred(*close, std::nullopt, Selection::distance, 100, -1),
            "view error limit");
}
