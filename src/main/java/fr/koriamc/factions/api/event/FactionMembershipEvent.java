package fr.koriamc.factions.api.event;

import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import org.bukkit.event.HandlerList;

/**
 * Appelé avant qu'un joueur rejoigne ou quitte une faction.
 * L'annulation est ignorée pour les actions forcées (admin, dissolution, inactivité).
 */
public final class FactionMembershipEvent extends FactionEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public enum Action {
        JOIN,
        LEAVE,
        KICK,
        FORCED_JOIN,
        FORCED_LEAVE
    }

    private final FPlayer player;
    private final Action action;

    public FactionMembershipEvent(Faction faction, FPlayer player, Action action) {
        super(faction);
        this.player = player;
        this.action = action;
    }

    public FPlayer getFPlayer() {
        return player;
    }

    public Action getAction() {
        return action;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
