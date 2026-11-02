package com.gaspoweredcake.client33.utils.recon;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerTeam;
import static com.gaspoweredcake.client33.Client33.mc;

/** Only displayed tab/sidebar strings; no synthetic faction inference from team membership. */
public final class ServerTextSnapshot {
    public record Line(String text, ReconParser.Source source) {}
    private ServerTextSnapshot() {}
    public static List<Line> read() {
        List<Line> lines = new ArrayList<>();
        if (mc.level == null || mc.getConnection() == null) return lines;
        for (var player : mc.getConnection().getOnlinePlayers()) {
            Component name = player.getTabListDisplayName();
            if (name == null) name = PlayerTeam.formatNameForTeam(player.getTeam(), Component.literal(player.getProfile().name()));
            lines.add(new Line(name.getString(), ReconParser.Source.Tab));
            if (lines.size() >= 256) break;
        }
        var scoreboard = mc.level.getScoreboard();
        var objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective != null) {
            lines.add(new Line(objective.getDisplayName().getString(), ReconParser.Source.Scoreboard));
            scoreboard.listPlayerScores(objective).stream().filter(entry -> !entry.isHidden()).sorted(java.util.Comparator.comparingInt(net.minecraft.world.scores.PlayerScoreEntry::value).reversed()).limit(32).forEach(entry -> {
                Component name = entry.display() != null ? entry.display() : Component.literal(entry.owner());
                var team = scoreboard.getPlayersTeam(entry.owner());
                if (team != null) name = PlayerTeam.formatNameForTeam(team, name);
                lines.add(new Line(name.getString(), ReconParser.Source.Scoreboard));
            });
        }
        return lines;
    }
}
