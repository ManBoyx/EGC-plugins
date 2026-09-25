package fr.minebed.hub.ultimate.modules;

import fr.minebed.hub.common.util.SavedLocation;
import fr.minebed.hub.ultimate.AbstractModule;
import fr.minebed.hub.ultimate.BaseCommand;
import fr.minebed.hub.ultimate.Ctx;
import fr.minebed.hub.ultimate.Msg;
import fr.minebed.hub.ultimate.TeleportService;
import fr.minebed.hub.ultimate.util.Locations;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/** /back : retour au dernier endroit quitté par téléportation, ou au lieu de la dernière mort. Gardé en mémoire seulement. */
public final class BackModule extends AbstractModule implements Listener {

    private final TeleportService teleports;
    private final Map<UUID, SavedLocation> last = new ConcurrentHashMap<UUID, SavedLocation>();

    public BackModule(Ctx ctx, TeleportService teleports) {
        super(ctx);
        this.teleports = teleports;
    }

    @Override
    public String id() {
        return "back";
    }

    @Override
    public void enable() {
        command("back", new BaseCommand(ctx, "ultimatecore.back") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                SavedLocation saved = last.get(player.getUniqueId());
                Location where = saved == null ? null : Locations.restore(saved);
                if (where == null) {
                    ctx.messages.send(player, Msg.BACK_NONE);
                    return true;
                }
                teleports.teleport(player, where, true, Msg.BACK_TELEPORTED);
                return true;
            }
        });
        teleports.addDepartureListener(new java.util.function.BiConsumer<Player, Location>() {
            @Override
            public void accept(Player player, Location from) {
                if (from.getWorld() != null) {
                    last.put(player.getUniqueId(), Locations.save(from));
                }
            }
        });
        listen(this);
    }

    /**
     * Complète les téléportations faites hors de ce plugin (commande « /tp » de la console ou d'un opérateur). Les nôtres
     * (cause « PLUGIN ») sont déjà enregistrées par le service de téléportation : sur Paper 1.16.5 l'événement de
     * {@code teleportAsync} annonçait l'endroit d'arrivée comme départ et écrasait le bon point (constaté avec les joueurs simulés).
     */
    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        String cause = event.getCause().name();
        Location from = event.getFrom();
        Location to = event.getTo();
        if ((cause.equals("COMMAND") || cause.equals("UNKNOWN")) && from.getWorld() != null && (to == null || !Locations.sameBlock(from, to))) {
            last.put(event.getPlayer().getUniqueId(), Locations.save(from));
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.hasPermission("ultimatecore.back.death")) {
            last.put(player.getUniqueId(), Locations.save(player.getLocation()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        last.remove(event.getPlayer().getUniqueId());
    }
}
