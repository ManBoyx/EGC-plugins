package fr.minebed.hub.ultimate.modules;

import fr.minebed.hub.common.util.SavedLocation;
import fr.minebed.hub.ultimate.AbstractModule;
import fr.minebed.hub.ultimate.BaseCommand;
import fr.minebed.hub.ultimate.Ctx;
import fr.minebed.hub.ultimate.Msg;
import fr.minebed.hub.ultimate.TeleportService;
import fr.minebed.hub.ultimate.logic.PermissionLimits;
import fr.minebed.hub.ultimate.util.DataStore;
import fr.minebed.hub.ultimate.util.Locations;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Points de passage communs : /warp, /warps, /setwarp, /delwarp. */
public final class WarpsModule extends AbstractModule {

    private final TeleportService teleports;
    private DataStore store;

    public WarpsModule(Ctx ctx, TeleportService teleports) {
        super(ctx);
        this.teleports = teleports;
    }

    @Override
    public String id() {
        return "warps";
    }

    @Override
    public void enable() {
        store = new DataStore(ctx.plugin, ctx.scheduler, "warps.yml");
        command("warp", new BaseCommand(ctx, "ultimatecore.warp") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length == 0 || args.length > 2) {
                    return false;
                }
                String name = args[0].toLowerCase(Locale.ROOT);
                SavedLocation saved = PermissionLimits.isValidName(name) ? SavedLocation.parse(store.getString("warps." + name)) : null;
                if (saved == null) {
                    ctx.messages.send(sender, Msg.WARP_NOT_FOUND, "name", name);
                    return true;
                }
                Location where = Locations.restore(saved);
                if (where == null) {
                    ctx.messages.send(sender, Msg.TP_WORLD_MISSING, "world", saved.world());
                    return true;
                }
                if (args.length == 2) {
                    if (!sender.hasPermission("ultimatecore.warp.others")) {
                        ctx.messages.send(sender, Msg.NO_PERMISSION);
                        return true;
                    }
                    final Player target = requireOnline(sender, args[1]);
                    if (target != null) {
                        final Location destination = where;
                        final String warpName = name;
                        ctx.runOnPlayer(target, new Runnable() {
                            @Override
                            public void run() {
                                teleports.teleport(target, destination, false, Msg.WARP_TELEPORTED, "name", warpName);
                            }
                        });
                    }
                    return true;
                }
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (!canUse(player, name)) {
                    ctx.messages.send(player, Msg.WARP_NO_PERMISSION, "name", name);
                    return true;
                }
                teleports.teleport(player, where, true, Msg.WARP_TELEPORTED, "name", name);
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? filter(usable(sender), args[0]) : args.length == 2 ? onlineNames(args[1]) : Collections.<String>emptyList();
            }
        });
        command("warps", new BaseCommand(ctx, "ultimatecore.warp") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                Set<String> list = usable(sender);
                if (list.isEmpty()) {
                    ctx.messages.send(sender, Msg.WARP_NONE);
                } else {
                    ctx.messages.send(sender, Msg.WARP_LIST, "warps", String.join(", ", list));
                }
                return true;
            }
        });
        command("setwarp", new BaseCommand(ctx, "ultimatecore.warp.set") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (args.length != 1) {
                    return false;
                }
                if (!PermissionLimits.isValidName(args[0])) {
                    ctx.messages.send(player, Msg.WARP_INVALID_NAME);
                    return true;
                }
                String name = args[0].toLowerCase(Locale.ROOT);
                store.set("warps." + name, Locations.save(player.getLocation()).format());
                ctx.messages.send(player, Msg.WARP_SET, "name", name);
                return true;
            }
        });
        command("delwarp", new BaseCommand(ctx, "ultimatecore.warp.set") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length != 1) {
                    return false;
                }
                String name = args[0].toLowerCase(Locale.ROOT);
                if (!PermissionLimits.isValidName(name) || !store.contains("warps." + name)) {
                    ctx.messages.send(sender, Msg.WARP_NOT_FOUND, "name", name);
                    return true;
                }
                store.set("warps." + name, null);
                ctx.messages.send(sender, Msg.WARP_DELETED, "name", name);
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? filter(store.keys("warps"), args[0]) : Collections.<String>emptyList();
            }
        });
    }

    @Override
    protected void onDisable() {
        store.flush();
    }

    /** Par défaut tout le monde peut utiliser tous les points ; avec {@code warps.per-warp-permission} il faut « ultimatecore.warp.NOM ». */
    private boolean canUse(CommandSender who, String name) {
        return !ctx.config().getBoolean("warps.per-warp-permission", false) || who.hasPermission("ultimatecore.warp.use.*") || who.hasPermission("ultimatecore.warp.use." + name);
    }

    private Set<String> usable(CommandSender who) {
        Set<String> out = new TreeSet<String>();
        for (String name : store.keys("warps")) {
            if (canUse(who, name)) {
                out.add(name);
            }
        }
        return out;
    }
}
