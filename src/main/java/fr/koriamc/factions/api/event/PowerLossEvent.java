package fr.koriamc.factions.api.event;

import fr.koriamc.factions.model.FPlayer;
import fr.koriamc.factions.model.Faction;
import org.bukkit.event.HandlerList;

/**
 * Appelé avant qu'un joueur perde de la puissance en mourant.
 * La perte peut être modifiée avec {@link #setLoss(double)}.
 */
public final class PowerLossEvent extends FactionEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final FPlayer player;
    private double loss;

    public PowerLossEvent(Faction faction, FPlayer player, double loss) {
        super(faction);
        this.player = player;
        this.loss = loss;
    }

    public FPlayer getFPlayer() {
        return player;
    }

    public double getLoss() {
        return loss;
    }

    public void setLoss(double loss) {
        this.loss = Math.max(0, loss);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
