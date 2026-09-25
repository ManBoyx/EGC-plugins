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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

/** Maisons personnelles : /sethome, /home, /delhome, /homes. Limite par permission « ultimatecore.homes.limit.N ». */
public final class HomesModule extends AbstractModule {

    private static final String LIMIT_PREFIX = "ultimatecore.homes.limit.";

    private final TeleportService teleports;
    private DataStore store;

    public HomesModule(Ctx ctx, TeleportService teleports) {
        super(ctx);
        this.teleports = teleports;
    }

    @Override
    public String id() {
        return "homes";
    }

    @Override
    public void enable() {
        store = new DataStore(ctx.plugin, ctx.scheduler, "homes.yml");
        command("sethome", new BaseCommand(ctx, "ultimatecore.homes") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (args.length > 1) {
                    return false;
                }
                String name = args.length == 0 ? "home" : args[0];
                if (!PermissionLimits.isValidName(name)) {
                    ctx.messages.send(player, Msg.HOME_INVALID_NAME);
                    return true;
                }
                String key = key(player, name);
                int limit = limitOf(player);
                if (!store.contains(key) && names(player).size() >= limit) {
                    ctx.messages.send(player, Msg.HOME_LIMIT, "limit", String.valueOf(limit));
                    return true;
                }
                store.set(key, Locations.save(player.getLocation()).format());
                ctx.messages.send(player, Msg.HOME_SET, "name", name);
                return true;
            }
        });
        command("home", new BaseCommand(ctx, "ultimatecore.homes") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (args.length > 1) {
                    return false;
                }
                Set<String> all = names(player);
                String name;
                if (args.length == 1) {
                    name = args[0];
                } else if (all.size() == 1) {
                    name = all.iterator().next();
                } else {
                    name = "home";
                }
                SavedLocation saved = PermissionLimits.isValidName(name) ? SavedLocation.parse(store.getString(key(player, name))) : null;
                if (saved == null) {
                    ctx.messages.send(player, Msg.HOME_NOT_FOUND, "name", name);
                    return true;
                }
                Location where = Locations.restore(saved);
                if (where == null) {
                    ctx.messages.send(player, Msg.TP_WORLD_MISSING, "world", saved.world());
                    return true;
                }
                teleports.teleport(player, where, true, Msg.HOME_TELEPORTED, "name", name.toLowerCase(Locale.ROOT));
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 && sender instanceof Player ? filter(names((Player) sender), args[0]) : Collections.<String>emptyList();
            }
        });
        command("delhome", new BaseCommand(ctx, "ultimatecore.homes") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (args.length != 1) {
                    return false;
                }
                String key = PermissionLimits.isValidName(args[0]) ? key(player, args[0]) : null;
                if (key == null || !store.contains(key)) {
                    ctx.messages.send(player, Msg.HOME_NOT_FOUND, "name", args[0]);
                    return true;
                }
                store.set(key, null);
                ctx.messages.send(player, Msg.HOME_DELETED, "name", args[0].toLowerCase(Locale.ROOT));
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 && sender instanceof Player ? filter(names((Player) sender), args[0]) : Collections.<String>emptyList();
            }
        });
        command("homes", new BaseCommand(ctx, "ultimatecore.homes") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                Set<String> all = names(player);
                if (all.isEmpty()) {
                    ctx.messages.send(player, Msg.HOME_NONE);
                } else {
                    int limit = limitOf(player);
                    ctx.messages.send(player, Msg.HOME_LIST, "homes", String.join(", ", all), "count", String.valueOf(all.size()),
                        "limit", limit == Integer.MAX_VALUE ? "∞" : String.valueOf(limit));
                }
                return true;
            }
        });
    }

    @Override
    protected void onDisable() {
        store.flush();
    }

    private static String key(Player player, String name) {
        return player.getUniqueId() + "." + name.toLowerCase(Locale.ROOT);
    }

    private Set<String> names(Player player) {
        return new java.util.TreeSet<String>(store.keys(player.getUniqueId().toString()));
    }

    private int limitOf(Player player) {
        List<String> nodes = new ArrayList<String>();
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (info.getValue()) {
                nodes.add(info.getPermission());
            }
        }
        return PermissionLimits.highest(nodes, LIMIT_PREFIX, ctx.config().getInt("homes.default-limit", 3));
    }
}
