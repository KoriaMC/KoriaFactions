package fr.koriamc.factions.manager;

import fr.koriamc.factions.KoriaFactions;
import fr.koriamc.factions.config.Messages;
import fr.koriamc.factions.config.Settings;
import fr.koriamc.factions.model.ChunkPos;
import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import fr.koriamc.factions.util.Text;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rendu de /f map : grille de symboles colorés, boussole, survol et claim au clic.
 */
public final class MapManager {

    private static final char[][] COMPASS = {
            {'\\', 'N', '/'},
            {'W', '+', 'E'},
            {'/', 'S', '\\'}
    };
    /** Position (ligne, colonne) de chaque direction : N, NE, E, SE, S, SO, O, NO. */
    private static final int[][] DIRECTIONS = {
            {0, 1}, {0, 2}, {1, 2}, {2, 2}, {2, 1}, {2, 0}, {1, 0}, {0, 0}
    };

    private final KoriaFactions plugin;

    public MapManager(KoriaFactions plugin) {
        this.plugin = plugin;
    }

    public void send(Player player, FPlayer fp) {
        Settings s = plugin.settings();
        Messages msg = plugin.msg();
        ChunkPos center = ChunkPos.of(player.getLocation());
        Faction here = plugin.claims().getOwner(center);
        Faction own = plugin.factions().getFaction(fp);
        msg.send(player, "map.header", "x", center.getX(), "z", center.getZ(),
                "faction", plugin.factions().getColor(fp, here) + here.getTag());

        int halfW = s.mapWidth / 2;
        int halfH = s.mapHeight / 2;
        int[] facing = DIRECTIONS[direction(player.getLocation().getYaw())];
        String compassColor = msg.text("map.compass-color");
        String compassHighlight = msg.text("map.compass-highlight");
        String label = plugin.commands().getLabel();
        Map<String, String> legend = new LinkedHashMap<String, String>();
        int symbolIndex = 0;

        for (int dz = -halfH; dz <= halfH; dz++) {
            List<BaseComponent> row = new ArrayList<BaseComponent>();
            int rowIndex = dz + halfH;
            for (int dx = -halfW; dx <= halfW; dx++) {
                int colIndex = dx + halfW;
                if (s.mapCompass && rowIndex < 3 && colIndex < 3) {
                    boolean highlighted = facing[0] == rowIndex && facing[1] == colIndex;
                    row.add(new TextComponent((highlighted ? compassHighlight : compassColor) + COMPASS[rowIndex][colIndex]));
                    continue;
                }
                ChunkPos pos = center.relative(dx, dz);
                Faction owner = plugin.claims().getOwner(pos);
                String symbol;
                String color;
                if (dx == 0 && dz == 0) {
                    symbol = s.mapSymbolYou;
                    color = s.mapColorYou;
                } else if (owner.isWilderness()) {
                    symbol = s.mapSymbolWilderness;
                    color = s.wildernessColor;
                } else if (owner.isSafeZone()) {
                    symbol = s.mapSymbolSafezone;
                    color = s.safezoneColor;
                } else if (owner.isWarZone()) {
                    symbol = s.mapSymbolWarzone;
                    color = s.warzoneColor;
                } else {
                    color = plugin.factions().getColor(fp, owner);
                    String known = legend.get(owner.getId());
                    if (known == null) {
                        known = String.valueOf(s.mapSymbols.charAt(symbolIndex++ % s.mapSymbols.length()));
                        legend.put(owner.getId(), known);
                    }
                    symbol = known;
                }
                TextComponent component = new TextComponent(Text.color(color + symbol));
                boolean clickable = s.mapClickToClaim && own != null && !owner.isSafeZone() && !owner.isWarZone()
                        && owner != own && Math.max(Math.abs(dx), Math.abs(dz)) <= s.mapClickMaxDistance;
                if (s.mapHover) {
                    String hover = msg.format(msg.config().getString("map.hover", ""),
                            "color", plugin.factions().getColor(fp, owner), "faction", owner.getTag(),
                            "x", pos.getX(), "z", pos.getZ());
                    if (clickable) {
                        hover += msg.format(msg.config().getString("map.hover-claim", ""));
                    }
                    component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hover)));
                }
                if (clickable) {
                    component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            "/" + label + " claimat " + pos.getWorldName() + " " + pos.getX() + " " + pos.getZ()));
                }
                row.add(component);
            }
            player.spigot().sendMessage(row.toArray(new BaseComponent[row.size()]));
        }

        StringBuilder line = new StringBuilder(msg.format(msg.config().getString("map.legend-you", ""),
                "color", s.mapColorYou, "symbol", s.mapSymbolYou));
        String separator = msg.format(msg.config().getString("map.legend-separator", ", "));
        int shown = 0;
        for (Map.Entry<String, String> entry : legend.entrySet()) {
            if (s.mapLegendMax > 0 && shown >= s.mapLegendMax) {
                break;
            }
            Faction f = plugin.factions().getById(entry.getKey());
            if (f == null) {
                continue;
            }
            line.append(separator).append(msg.format(msg.config().getString("map.legend-entry", ""),
                    "color", plugin.factions().getColor(fp, f), "symbol", entry.getValue(), "faction", f.getTag()));
            shown++;
        }
        player.sendMessage(line.toString());
    }

    /** Direction regardée (0 = nord, puis sens horaire par pas de 45°). */
    private static int direction(float yaw) {
        double rotation = (yaw - 180) % 360;
        if (rotation < 0) {
            rotation += 360;
        }
        return (int) Math.round(rotation / 45.0) % 8;
    }
}
