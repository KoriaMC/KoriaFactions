package fr.koriamc.factions.api.event;

import fr.koriamc.factions.model.ChunkPos;
import fr.koriamc.factions.model.Faction;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Appelé avant qu'un chunk soit abandonné par un joueur.
 */
public final class LandUnclaimEvent extends FactionEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ChunkPos chunk;
    private final Player player;

    public LandUnclaimEvent(Faction faction, ChunkPos chunk, Player player) {
        super(faction);
        this.chunk = chunk;
        this.player = player;
    }

    public ChunkPos getChunk() {
        return chunk;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
