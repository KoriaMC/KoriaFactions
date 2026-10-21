package fr.koriamc.factions.api.event;

import fr.koriamc.factions.model.Faction;
import org.bukkit.event.HandlerList;

/**
 * Appelé avant que les points d'une faction changent (quêtes, commande admin, autre plugin).
 * Le gain (négatif pour un retrait) peut être modifié avec {@link #setAmount(long)}.
 */
public final class FactionPointsChangeEvent extends FactionEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private long amount;
    private final String reason;

    public FactionPointsChangeEvent(Faction faction, long amount, String reason) {
        super(faction);
        this.amount = amount;
        this.reason = reason;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    /** @return l'origine du changement (ex : "koriaquest", "admin"), jamais null. */
    public String getReason() {
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
