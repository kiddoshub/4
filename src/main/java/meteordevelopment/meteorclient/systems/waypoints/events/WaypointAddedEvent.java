
package meteordevelopment.meteorclient.systems.waypoints.events;

import meteordevelopment.meteorclient.systems.waypoints.Waypoint;

@Deprecated()
public class WaypointAddedEvent {

    public final Waypoint waypoint;

    public WaypointAddedEvent(Waypoint waypoint) {
        this.waypoint = waypoint;
    }
}
