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
        listen(this);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        String cause = event.getCause().name();
        // Seules les téléportations « voulues » comptent : pas les perles d'Ender, portails, fruits de Chorus…
        if ((cause.equals("COMMAND") || cause.equals("PLUGIN") || cause.equals("UNKNOWN")) && event.getFrom().getWorld() != null) {
            last.put(event.getPlayer().getUniqueId(), Locations.save(event.getFrom()));
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
