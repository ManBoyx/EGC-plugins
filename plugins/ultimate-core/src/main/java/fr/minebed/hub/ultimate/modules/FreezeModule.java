package fr.minebed.hub.ultimate.modules;

import fr.minebed.hub.compat.PlayerCompat;
import fr.minebed.hub.compat.scheduler.Scheduler;
import fr.minebed.hub.ultimate.AbstractModule;
import fr.minebed.hub.ultimate.BaseCommand;
import fr.minebed.hub.ultimate.Ctx;
import fr.minebed.hub.ultimate.Msg;
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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * /freeze et /unfreeze : immobilise un joueur pour un contrôle du staff. Le joueur gelé ne bouge plus, ne casse ni ne pose rien,
 * n'interagit plus, ne lâche rien, ne frappe personne, n'utilise plus de commandes, et reçoit titre, message et rappel dans la barre
 * d'action. L'état est enregistré : reconnecté, il reste gelé. Ce qui arrive s'il se déconnecte gelé se règle dans
 * {@code freeze.quit-commands} (vide par défaut : le plugin ne punit rien de lui-même).
 */
public final class FreezeModule extends AbstractModule implements Listener {

    private final Map<UUID, String> frozen = new ConcurrentHashMap<UUID, String>();
    private DataStore store;
    private Scheduler.Task reminder;

    public FreezeModule(Ctx ctx) {
        super(ctx);
    }

    @Override
    public String id() {
        return "freeze";
    }

    @Override
    public void enable() {
        store = new DataStore(ctx.plugin, ctx.scheduler, "freeze.yml");
        for (String key : store.keys("frozen")) {
            try {
                frozen.put(UUID.fromString(key), String.valueOf(store.getString("frozen." + key)));
            } catch (IllegalArgumentException e) {
                ctx.plugin.getLogger().warning("freeze.yml : entrée ignorée (identifiant invalide) : " + key);
            }
        }
        command("freeze", new BaseCommand(ctx, "ultimatecore.freeze") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length == 0) {
                    return false;
                }
                Player target = requireOnline(sender, args[0]);
                if (target == null) {
                    return true;
                }
                if (frozen.containsKey(target.getUniqueId())) {
                    release(sender, target.getUniqueId(), target.getName());
                    return true;
                }
                if (target.hasPermission("ultimatecore.freeze.bypass")) {
                    ctx.messages.send(sender, Msg.FREEZE_BYPASS, "player", target.getName());
                    return true;
                }
                String custom = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
                freeze(sender, target, custom);
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(args[0]) : Collections.<String>emptyList();
            }
        });
        command("unfreeze", new BaseCommand(ctx, "ultimatecore.freeze") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length != 1) {
                    return false;
                }
                for (Map.Entry<UUID, String> e : frozen.entrySet()) {
                    if (e.getValue().equalsIgnoreCase(args[0])) {
                        release(sender, e.getKey(), e.getValue());
                        return true;
                    }
                }
                ctx.messages.send(sender, Msg.FREEZE_NOT_FROZEN, "name", args[0]);
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? filter(new ArrayList<String>(frozen.values()), args[0]) : Collections.<String>emptyList();
            }
        });
        listen(this);
        long seconds = Math.max(2, ctx.config().getLong("freeze.remind-seconds", 5));
        reminder = ctx.scheduler.runGlobalTimer(new Runnable() {
            @Override
            public void run() {
                remindAll();
            }
        }, seconds * 20, seconds * 20);
    }

    @Override
    protected void onDisable() {
        if (reminder != null) {
            reminder.cancel();
            reminder = null;
        }
        store.flush();
    }

    private void freeze(CommandSender by, final Player target, String custom) {
        frozen.put(target.getUniqueId(), target.getName());
        store.set("frozen." + target.getUniqueId(), target.getName());
        announce(target, custom);
        ctx.messages.send(by, Msg.FREEZE_DONE, "player", target.getName());
        ctx.notifyStaff(ctx.messages.raw(Msg.STAFF_FROZE, "staff", by.getName(), "player", target.getName()));
    }

    /** Titre, message : le texte personnalisé du staff remplace le sous-titre standard. */
    private void announce(final Player target, final String custom) {
        ctx.runOnPlayer(target, new Runnable() {
            @Override
            public void run() {
                String subtitle = custom != null ? ctx.messages.format(custom) : ctx.messages.raw(Msg.FREEZE_SUBTITLE);
                PlayerCompat.sendTitle(target, ctx.messages.raw(Msg.FREEZE_TITLE), subtitle, 10, 100, 20);
                ctx.messages.send(target, Msg.FREEZE_FROZEN);
                if (custom != null) {
                    target.sendMessage(ctx.messages.raw(Msg.PREFIX) + ctx.messages.format(custom));
                }
            }
        });
    }

    private void release(CommandSender by, UUID id, String name) {
        frozen.remove(id);
        store.set("frozen." + id, null);
        final Player online = Bukkit.getPlayer(id);
        if (online != null) {
            ctx.runOnPlayer(online, new Runnable() {
                @Override
                public void run() {
                    ctx.messages.send(online, Msg.FREEZE_RELEASED);
                }
            });
        }
        ctx.messages.send(by, Msg.FREEZE_UNDONE, "player", name);
        ctx.notifyStaff(ctx.messages.raw(Msg.STAFF_UNFROZE, "staff", by.getName(), "player", name));
    }

    private void remindAll() {
        for (UUID id : frozen.keySet()) {
            final Player p = Bukkit.getPlayer(id);
            if (p != null) {
                ctx.runOnPlayer(p, new Runnable() {
                    @Override
                    public void run() {
                        PlayerCompat.sendActionBar(p, ctx.messages.raw(Msg.FREEZE_REMINDER));
                    }
                });
            }
        }
    }

    private boolean isFrozen(Player p) {
        return !frozen.isEmpty() && frozen.containsKey(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!isFrozen(event.getPlayer())) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        // Regarder autour reste possible ; changer de position, non.
        if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && isFrozen((Player) event.getDamager())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && isFrozen((Player) event.getEntity()) && ctx.config().getBoolean("freeze.protect", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!isFrozen(event.getPlayer()) || !ctx.config().getBoolean("freeze.block-commands", true)) {
            return;
        }
        String first = event.getMessage().substring(1).split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        int colon = first.indexOf(':');
        if (colon >= 0) {
            first = first.substring(colon + 1);
        }
        for (String allowed : ctx.config().getStringList("freeze.allowed-commands")) {
            if (allowed.toLowerCase(Locale.ROOT).replace("/", "").equals(first)) {
                return;
            }
        }
        event.setCancelled(true);
        ctx.messages.send(event.getPlayer(), Msg.FREEZE_COMMAND_BLOCKED);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (frozen.containsKey(player.getUniqueId())) {
            frozen.put(player.getUniqueId(), player.getName());
            ctx.scheduler.runForEntityLater(player, new Runnable() {
                @Override
                public void run() {
                    announce(player, null);
                }
            }, null, 20);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!frozen.containsKey(player.getUniqueId())) {
            return;
        }
        final String name = player.getName();
        ctx.notifyStaff(ctx.messages.raw(Msg.STAFF_FROZEN_QUIT, "player", name));
        for (final String command : ctx.config().getStringList("freeze.quit-commands")) {
            ctx.scheduler.runGlobal(new Runnable() {
                @Override
                public void run() {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", name));
                }
            });
        }
    }
}
