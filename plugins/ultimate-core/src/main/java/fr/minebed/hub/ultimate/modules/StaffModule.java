package fr.minebed.hub.ultimate.modules;

import fr.minebed.hub.common.time.DurationParser;
import fr.minebed.hub.compat.PlayerCompat;
import fr.minebed.hub.compat.Sounds;
import fr.minebed.hub.ultimate.AbstractModule;
import fr.minebed.hub.ultimate.BaseCommand;
import fr.minebed.hub.ultimate.Ctx;
import fr.minebed.hub.ultimate.Msg;
import fr.minebed.hub.ultimate.TeleportService;
import fr.minebed.hub.ultimate.logic.SanctionArgs;
import fr.minebed.hub.ultimate.util.DataStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Outils du staff : /tphere (ramène un joueur auprès de soi), /mute et /unmute (avec durée et motif, conservés d'un redémarrage à l'autre), /warn et /warns
 * (avertissements comptés, avec des actions configurables à certains seuils), /alert (message à l'écran d'un joueur ou de tous).
 * Les autres membres du staff sont prévenus. Les sanctions visent des joueurs en ligne ; pour les bannissements, voir LibertyBans.
 */
public final class StaffModule extends AbstractModule implements Listener {

    private static final class Mute {
        final String name;
        final long until;
        final String reason;

        Mute(String name, long until, String reason) {
            this.name = name;
            this.until = until;
            this.reason = reason;
        }
    }

    private final Map<UUID, Mute> mutes = new ConcurrentHashMap<UUID, Mute>();
    private DataStore muteStore;
    private DataStore warnStore;
    private final Object warnLock = new Object();

    private final TeleportService teleports;

    public StaffModule(Ctx ctx, TeleportService teleports) {
        super(ctx);
        this.teleports = teleports;
    }

    @Override
    public String id() {
        return "staff";
    }

    @Override
    public void enable() {
        muteStore = new DataStore(ctx.plugin, ctx.scheduler, "mutes.yml");
        warnStore = new DataStore(ctx.plugin, ctx.scheduler, "warns.yml");
        for (String key : muteStore.keys("mutes")) {
            try {
                UUID id = UUID.fromString(key);
                mutes.put(id, new Mute(String.valueOf(muteStore.getString("mutes." + key + ".name")), muteStore.getLong("mutes." + key + ".until", SanctionArgs.PERMANENT),
                    String.valueOf(muteStore.getString("mutes." + key + ".reason"))));
            } catch (IllegalArgumentException e) {
                ctx.plugin.getLogger().warning("mutes.yml : entrée ignorée (identifiant invalide) : " + key);
            }
        }
        command("mute", new BaseCommand(ctx, "ultimatecore.mute") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length == 0) {
                    return false;
                }
                Player target = requireOnline(sender, args[0]);
                if (target == null) {
                    return true;
                }
                if (target.hasPermission("ultimatecore.mute.bypass")) {
                    ctx.messages.send(sender, Msg.MUTE_BYPASS, "player", target.getName());
                    return true;
                }
                SanctionArgs a = SanctionArgs.parse(args, 1, ctx.messages.raw(Msg.STAFF_REASON_NONE));
                long until = a.expiresAt(System.currentTimeMillis());
                mute(target, until, a.reason());
                String time = describe(until);
                ctx.messages.send(sender, Msg.MUTE_DONE, "player", target.getName(), "time", time);
                ctx.notifyStaff(ctx.messages.raw(Msg.STAFF_MUTED, "staff", sender.getName(), "player", target.getName(), "time", time));
                final Player t = target;
                final String reason = a.reason();
                ctx.runOnPlayer(t, new Runnable() {
                    @Override
                    public void run() {
                        ctx.messages.send(t, Msg.MUTE_TARGET, "time", describe(mutes.get(t.getUniqueId()).until), "reason", reason);
                    }
                });
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(args[0]) : Collections.<String>emptyList();
            }
        });
        command("unmute", new BaseCommand(ctx, "ultimatecore.mute") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length != 1) {
                    return false;
                }
                for (Map.Entry<UUID, Mute> e : mutes.entrySet()) {
                    if (e.getValue().name.equalsIgnoreCase(args[0])) {
                        unmute(e.getKey());
                        ctx.messages.send(sender, Msg.MUTE_UNDONE, "player", e.getValue().name);
                        ctx.notifyStaff(ctx.messages.raw(Msg.STAFF_UNMUTED, "staff", sender.getName(), "player", e.getValue().name));
                        final Player online = Bukkit.getPlayer(e.getKey());
                        if (online != null) {
                            ctx.runOnPlayer(online, new Runnable() {
                                @Override
                                public void run() {
                                    ctx.messages.send(online, Msg.MUTE_UNDONE_TARGET);
                                }
                            });
                        }
                        return true;
                    }
                }
                ctx.messages.send(sender, Msg.MUTE_NOT_MUTED, "name", args[0]);
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                List<String> names = new ArrayList<String>();
                for (Mute m : mutes.values()) {
                    names.add(m.name);
                }
                return args.length == 1 ? filter(names, args[0]) : Collections.<String>emptyList();
            }
        });
        command("warn", new BaseCommand(ctx, "ultimatecore.warn") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length < 2) {
                    return false;
                }
                Player target = requireOnline(sender, args[0]);
                if (target == null) {
                    return true;
                }
                String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                warn(sender, target, reason);
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(args[0]) : Collections.<String>emptyList();
            }
        });
        command("warns", new BaseCommand(ctx, "ultimatecore.warn") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length != 1) {
                    return false;
                }
                UUID id = null;
                String name = args[0];
                Player online = ctx.findOnlineExact(args[0]);
                if (online != null) {
                    id = online.getUniqueId();
                    name = online.getName();
                } else {
                    synchronized (warnLock) {
                        for (String key : warnStore.keys("warns")) {
                            if (args[0].equalsIgnoreCase(warnStore.getString("warns." + key + ".name"))) {
                                try {
                                    id = UUID.fromString(key);
                                    name = warnStore.getString("warns." + key + ".name");
                                } catch (IllegalArgumentException ignored) {
                                    // entrée abîmée : ignorée
                                }
                            }
                        }
                    }
                }
                if (id == null) {
                    ctx.messages.send(sender, Msg.PLAYER_NOT_FOUND, "name", args[0]);
                    return true;
                }
                ctx.messages.send(sender, Msg.WARN_COUNT, "player", name, "count", String.valueOf(warnStore.getLong("warns." + id + ".count", 0)));
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(args[0]) : Collections.<String>emptyList();
            }
        });
        command("tphere", new BaseCommand(ctx, "ultimatecore.tphere") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                Player me = requirePlayer(sender);
                if (me == null) {
                    return true;
                }
                if (args.length != 1) {
                    return false;
                }
                final Player target = requireOnline(me, args[0]);
                if (target == null) {
                    return true;
                }
                if (target.getUniqueId().equals(me.getUniqueId())) {
                    ctx.messages.send(me, Msg.TPA_SELF);
                    return true;
                }
                // Ordre du staff : ni attente, ni temps de recharge. La position est lue ici, dans le fil de celui qui donne l'ordre.
                final Location where = me.getLocation();
                final String staff = me.getName();
                ctx.runOnPlayer(target, new Runnable() {
                    @Override
                    public void run() {
                        teleports.teleport(target, where, false, Msg.TPHERE_TARGET, "player", staff);
                    }
                });
                ctx.messages.send(me, Msg.TPHERE_DONE, "player", target.getName());
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(args[0]) : Collections.<String>emptyList();
            }
        });
        command("alert", new BaseCommand(ctx, "ultimatecore.alert") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length < 2) {
                    return false;
                }
                List<Player> targets = new ArrayList<Player>();
                if (args[0].equals("*")) {
                    targets.addAll(Bukkit.getOnlinePlayers());
                } else {
                    Player p = requireOnline(sender, args[0]);
                    if (p == null) {
                        return true;
                    }
                    targets.add(p);
                }
                final String text = ctx.messages.format(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                for (final Player p : targets) {
                    ctx.runOnPlayer(p, new Runnable() {
                        @Override
                        public void run() {
                            p.sendMessage(ctx.messages.raw(Msg.ALERT_LINE, "message", text));
                            PlayerCompat.sendTitle(p, ctx.messages.raw(Msg.ALERT_TITLE), text, 10, 70, 20);
                            Sounds.play(p, "NOTE_PLING", 1f, 1f);
                        }
                    });
                }
                ctx.messages.send(sender, Msg.CORE_SENT, "count", String.valueOf(targets.size()));
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    List<String> names = onlineNames(args[0]);
                    if ("*".startsWith(args[0])) {
                        names.add("*");
                    }
                    return names;
                }
                return Collections.emptyList();
            }
        });
        listen(this);
    }

    @Override
    protected void onDisable() {
        muteStore.flush();
        warnStore.flush();
    }

    private void mute(Player target, long until, String reason) {
        mutes.put(target.getUniqueId(), new Mute(target.getName(), until, reason));
        String path = "mutes." + target.getUniqueId();
        muteStore.set(path + ".name", target.getName());
        muteStore.set(path + ".until", until);
        muteStore.set(path + ".reason", reason);
    }

    private void unmute(UUID id) {
        mutes.remove(id);
        muteStore.set("mutes." + id, null);
    }

    private String describe(long until) {
        if (until == SanctionArgs.PERMANENT) {
            return ctx.messages.raw(Msg.MUTE_PERMANENT);
        }
        return DurationParser.format(Math.max(0, until - System.currentTimeMillis()), "j", "h", "min", "s");
    }

    /** Le mute encore actif de ce joueur, ou {@code null} (un mute expiré est retiré au passage). */
    private Mute activeMute(UUID id) {
        Mute m = mutes.get(id);
        if (m == null) {
            return null;
        }
        if (m.until != SanctionArgs.PERMANENT && m.until <= System.currentTimeMillis()) {
            unmute(id);
            return null;
        }
        return m;
    }

    private void warn(CommandSender by, final Player target, final String reason) {
        final long count;
        synchronized (warnLock) {
            String path = "warns." + target.getUniqueId();
            count = warnStore.getLong(path + ".count", 0) + 1;
            warnStore.set(path + ".count", count);
            warnStore.set(path + ".name", target.getName());
            warnStore.set(path + ".last", reason);
        }
        ctx.runOnPlayer(target, new Runnable() {
            @Override
            public void run() {
                target.sendMessage(ctx.messages.raw(Msg.PREFIX) + ctx.messages.raw(Msg.WARN_TARGET, "count", String.valueOf(count), "reason", reason));
                PlayerCompat.sendTitle(target, ctx.messages.raw(Msg.WARN_TITLE), ctx.messages.format(reason), 10, 80, 20);
                Sounds.play(target, "ANVIL_LAND", 1f, 1f);
            }
        });
        ctx.messages.send(by, Msg.WARN_DONE, "player", target.getName(), "count", String.valueOf(count));
        ctx.notifyStaff(ctx.messages.raw(Msg.STAFF_WARNED, "staff", by.getName(), "player", target.getName(), "count", String.valueOf(count), "reason", reason));
        runWarnActions(target.getName(), count, reason);
    }

    /** Commandes de la console configurées pour ce nombre d'avertissements, par exemple {@code warn.actions.3: ["kick {player} …"]}. */
    private void runWarnActions(final String name, long count, final String reason) {
        ConfigurationSection actions = ctx.config().getConfigurationSection("warn.actions");
        if (actions == null) {
            return;
        }
        for (final String command : actions.getStringList(String.valueOf(count))) {
            ctx.scheduler.runGlobal(new Runnable() {
                @Override
                public void run() {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", name).replace("{reason}", reason));
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (mutes.isEmpty()) {
            return;
        }
        Mute m = activeMute(event.getPlayer().getUniqueId());
        if (m != null) {
            event.setCancelled(true);
            ctx.messages.send(event.getPlayer(), Msg.MUTE_CHAT, "time", describe(m.until), "reason", m.reason);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (mutes.isEmpty() || activeMute(event.getPlayer().getUniqueId()) == null) {
            return;
        }
        String first = event.getMessage().substring(1).split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        int colon = first.indexOf(':');
        if (colon >= 0) {
            first = first.substring(colon + 1);
        }
        for (String blocked : ctx.config().getStringList("mute.blocked-commands")) {
            if (blocked.toLowerCase(Locale.ROOT).replace("/", "").equals(first)) {
                event.setCancelled(true);
                ctx.messages.send(event.getPlayer(), Msg.MUTE_COMMAND);
                return;
            }
        }
    }
}
