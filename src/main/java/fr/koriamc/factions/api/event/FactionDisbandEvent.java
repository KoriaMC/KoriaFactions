package fr.koriamc.factions.api.event;

import fr.koriamc.factions.model.Faction;
import org.bukkit.event.HandlerList;

/**
 * Appelé avant la dissolution d'une faction. L'annulation est ignorée lorsque la
 * dissolution est forcée (inactivité, faction vide, réinitialisation de saison).
 */
public final class FactionDisbandEvent extends FactionEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public enum Reason {
        COMMAND,
        ADMIN,
        EMPTY,
        INACTIVITY,
        SEASON
    }

    private final String actor;
    private final Reason reason;

    public FactionDisbandEvent(Faction faction, String actor, Reason reason) {
        super(faction);
        this.actor = actor;
        this.reason = reason;
    }

    public String getActor() {
        return actor;
    }

    public Reason getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
