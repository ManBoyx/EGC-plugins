package fr.minebed.hub.ultimate.modules;

import fr.minebed.hub.common.util.SavedLocation;
import fr.minebed.hub.ultimate.AbstractModule;
import fr.minebed.hub.ultimate.BaseCommand;
import fr.minebed.hub.ultimate.Ctx;
import fr.minebed.hub.ultimate.Msg;
import fr.minebed.hub.ultimate.TeleportService;
import fr.minebed.hub.ultimate.util.DataStore;
import fr.minebed.hub.ultimate.util.Locations;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/** /spawn et /setspawn ; option : envoyer les nouveaux joueurs (et les ressuscités) au spawn. */
public final class SpawnModule extends AbstractModule implements Listener {

    private final TeleportService teleports;
    private DataStore store;

    public SpawnModule(Ctx ctx, TeleportService teleports) {
        super(ctx);
        this.teleports = teleports;
    }

    @Override
    public String id() {
        return "spawn";
    }

    @Override
    public void enable() {
        store = new DataStore(ctx.plugin, ctx.scheduler, "spawn.yml");
        command("spawn", new BaseCommand(ctx, "ultimatecore.spawn") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length > 1) {
                    return false;
                }
                Location spawn = spawnLocation();
                if (spawn == null) {
                    ctx.messages.send(sender, Msg.SPAWN_NOT_SET);
                    return true;
                }
                if (args.length == 1) {
                    if (!sender.hasPermission("ultimatecore.spawn.others")) {
                        ctx.messages.send(sender, Msg.NO_PERMISSION);
                        return true;
                    }
                    final Player target = requireOnline(sender, args[0]);
                    if (target == null) {
                        return true;
                    }
                    final Location where = spawn;
                    ctx.runOnPlayer(target, new Runnable() {
                        @Override
                        public void run() {
                            teleports.teleport(target, where, false, Msg.SPAWN_TELEPORTED);
                        }
                    });
                    ctx.messages.send(sender, Msg.SPAWN_SENT, "player", target.getName());
                    return true;
                }
                Player player = requirePlayer(sender);
                if (player != null) {
                    teleports.teleport(player, spawn, true, Msg.SPAWN_TELEPORTED);
                }
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 && sender.hasPermission("ultimatecore.spawn.others") ? onlineNames(args[0]) : Collections.<String>emptyList();
            }
        });
        command("setspawn", new BaseCommand(ctx, "ultimatecore.spawn.set") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                store.set("spawn", Locations.save(player.getLocation()).format());
                ctx.messages.send(player, Msg.SPAWN_SET);
                return true;
            }
        });
        listen(this);
    }

    @Override
    protected void onDisable() {
        store.flush();
    }

    private Location spawnLocation() {
        SavedLocation saved = SavedLocation.parse(store.getString("spawn"));
        return saved == null ? null : Locations.restore(saved);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (player.hasPlayedBefore() || !ctx.config().getBoolean("spawn.teleport-on-first-join", true)) {
            return;
        }
        final Location spawn = spawnLocation();
        if (spawn != null) {
            ctx.scheduler.runForEntityLater(player, new Runnable() {
                @Override
                public void run() {
                    teleports.teleport(player, spawn, false, null);
                }
            }, null, 2);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (ctx.config().getBoolean("spawn.teleport-on-respawn", false)) {
            Location spawn = spawnLocation();
            if (spawn != null) {
                event.setRespawnLocation(spawn);
            }
        }
    }
}
