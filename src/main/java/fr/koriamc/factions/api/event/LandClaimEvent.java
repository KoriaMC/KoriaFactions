package fr.koriamc.factions.api.event;

import fr.koriamc.factions.model.ChunkPos;
import fr.koriamc.factions.model.Faction;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Appelé avant qu'un chunk soit revendiqué (y compris par overclaim).
 */
public final class LandClaimEvent extends FactionEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ChunkPos chunk;
    private final Player player;
    private final Faction previousOwner;

    public LandClaimEvent(Faction faction, ChunkPos chunk, Player player, Faction previousOwner) {
        super(faction);
        this.chunk = chunk;
        this.player = player;
        this.previousOwner = previousOwner;
    }

    public ChunkPos getChunk() {
        return chunk;
    }

    /** @return le joueur à l'origine du claim, ou null (console / automatique). */
    public Player getPlayer() {
        return player;
    }

    public Faction getPreviousOwner() {
        return previousOwner;
    }

    public boolean isOverclaim() {
        return previousOwner != null && previousOwner.isNormal();
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
