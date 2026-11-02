
package meteordevelopment.meteorclient.systems.waypoints.events;

import meteordevelopment.meteorclient.systems.waypoints.Waypoint;

@Deprecated()
public class WaypointRemovedEvent {

    public final Waypoint waypoint;

    public WaypointRemovedEvent(Waypoint waypoint) {
        this.waypoint = waypoint;
    }
}
