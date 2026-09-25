package fr.minebed.hub.ultimate.modules;

import fr.minebed.hub.compat.PlayerCompat;
import fr.minebed.hub.compat.scheduler.Scheduler;
import fr.minebed.hub.ultimate.AbstractModule;
import fr.minebed.hub.ultimate.BaseCommand;
import fr.minebed.hub.ultimate.Ctx;
import fr.minebed.hub.ultimate.Msg;
import fr.minebed.hub.ultimate.util.DataStore;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * /vanish : le joueur devient invisible pour les autres. On cache le joueur à chaque spectateur (sur Folia, dans la région de ce
 * spectateur), sauf à ceux qui ont « ultimatecore.vanish.see ». L'état est conservé d'une connexion à l'autre. Un joueur invisible
 * n'est plus ciblé par les monstres et ne déclenche plus de plaque de pression.
 * Limites : il reste compté par le serveur (liste des serveurs, autres plugins), et la liste des joueurs dépend de la version.
 */
public final class VanishModule extends AbstractModule implements Listener {

    private static final String SEE = "ultimatecore.vanish.see";

    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();
    private DataStore store;
    private Scheduler.Task reminder;

    public VanishModule(Ctx ctx) {
        super(ctx);
    }

    @Override
    public String id() {
        return "vanish";
    }

    @Override
    public void enable() {
        if (!PlayerCompat.canHidePlayers()) {
            throw new IllegalStateException("ce serveur ne permet pas de cacher un joueur (hidePlayer absent)");
        }
        store = new DataStore(ctx.plugin, ctx.scheduler, "vanish.yml");
        for (String key : store.keys("vanished")) {
            try {
                vanished.add(UUID.fromString(key));
            } catch (IllegalArgumentException e) {
                ctx.plugin.getLogger().warning("vanish.yml : entrée ignorée (identifiant invalide) : " + key);
            }
        }
        command("vanish", new BaseCommand(ctx, "ultimatecore.vanish") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length > 1) {
                    return false;
                }
                if (args.length == 1) {
                    if (!sender.hasPermission("ultimatecore.vanish.others")) {
                        ctx.messages.send(sender, Msg.NO_PERMISSION);
                        return true;
                    }
                    Player target = requireOnline(sender, args[0]);
                    if (target != null) {
                        toggle(target);
                        ctx.messages.send(sender, Msg.UTIL_DONE_FOR, "player", target.getName());
                    }
                    return true;
                }
                Player me = requirePlayer(sender);
                if (me != null) {
                    toggle(me);
                }
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 && sender.hasPermission("ultimatecore.vanish.others") ? onlineNames(args[0]) : Collections.<String>emptyList();
            }
        });
        listen(this);
        // Après un rechargement : les joueurs déjà invisibles le restent aux yeux de ceux qui sont là.
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (vanished.contains(p.getUniqueId())) {
                hideFromEveryone(p);
            }
        }
        long seconds = Math.max(0, ctx.config().getLong("vanish.remind-seconds", 5));
        if (seconds > 0) {
            reminder = ctx.scheduler.runGlobalTimer(new Runnable() {
                @Override
                public void run() {
                    remindAll();
                }
            }, seconds * 20, seconds * 20);
        }
    }

    @Override
    protected void onDisable() {
        if (reminder != null) {
            reminder.cancel();
            reminder = null;
        }
        // Module coupé ou rechargé alors que le plugin tourne : on remontre les joueurs. À l'arrêt du serveur, rien à faire.
        if (ctx.plugin.isEnabled()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (vanished.contains(p.getUniqueId())) {
                    showToEveryone(p);
                }
            }
        }
        store.flush();
    }

    private void toggle(Player target) {
        UUID id = target.getUniqueId();
        boolean fake = ctx.config().getBoolean("vanish.fake-messages", false);
        if (vanished.remove(id)) {
            store.set("vanished." + id, null);
            showToEveryone(target);
            tell(target, Msg.VANISH_OFF);
            if (fake) {
                Bukkit.broadcastMessage(ctx.messages.raw(Msg.JOIN_MESSAGE, "player", target.getName()));
            }
        } else {
            vanished.add(id);
            store.set("vanished." + id, target.getName());
            hideFromEveryone(target);
            tell(target, Msg.VANISH_ON);
            if (fake) {
                Bukkit.broadcastMessage(ctx.messages.raw(Msg.JOIN_QUIT, "player", target.getName()));
            }
        }
    }

    private void tell(final Player player, final Msg message) {
        ctx.runOnPlayer(player, new Runnable() {
            @Override
            public void run() {
                ctx.messages.send(player, message);
            }
        });
    }

    private void hideFromEveryone(final Player target) {
        for (final Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(target) && !viewer.hasPermission(SEE)) {
                ctx.runOnPlayer(viewer, new Runnable() {
                    @Override
                    public void run() {
                        PlayerCompat.hide(viewer, target, ctx.plugin);
                    }
                });
            }
        }
    }

    private void showToEveryone(final Player target) {
        for (final Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(target)) {
                ctx.runOnPlayer(viewer, new Runnable() {
                    @Override
                    public void run() {
                        PlayerCompat.show(viewer, target, ctx.plugin);
                    }
                });
            }
        }
    }

    private void remindAll() {
        for (UUID id : vanished) {
            final Player p = Bukkit.getPlayer(id);
            if (p != null) {
                ctx.runOnPlayer(p, new Runnable() {
                    @Override
                    public void run() {
                        PlayerCompat.sendActionBar(p, ctx.messages.raw(Msg.VANISH_REMINDER));
                    }
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        final Player joining = event.getPlayer();
        // Le nouvel arrivant ne doit pas voir les joueurs invisibles (sauf s'il a la permission de les voir).
        if (!joining.hasPermission(SEE)) {
            for (UUID id : vanished) {
                Player v = Bukkit.getPlayer(id);
                if (v != null && !v.equals(joining)) {
                    PlayerCompat.hide(joining, v, ctx.plugin);
                }
            }
        }
        if (vanished.contains(joining.getUniqueId())) {
            event.setJoinMessage(null);
            hideFromEveryone(joining);
            ctx.scheduler.runForEntityLater(joining, new Runnable() {
                @Override
                public void run() {
                    ctx.messages.send(joining, Msg.VANISH_STILL);
                }
            }, null, 20);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        if (vanished.contains(event.getPlayer().getUniqueId())) {
            event.setQuitMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player && vanished.contains(event.getTarget().getUniqueId()) && ctx.config().getBoolean("vanish.protect", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPhysical(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL && vanished.contains(event.getPlayer().getUniqueId()) && ctx.config().getBoolean("vanish.protect", true)) {
            event.setCancelled(true);
        }
    }
}
