package fr.minebed.hub.ultimate.modules;

import fr.minebed.hub.compat.PlayerCompat;
import fr.minebed.hub.ultimate.AbstractModule;
import fr.minebed.hub.ultimate.Ctx;
import fr.minebed.hub.ultimate.Msg;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Messages de connexion et de départ, annonce de la première visite, message du jour et titre de bienvenue. */
public final class JoinModule extends AbstractModule implements Listener {

    public JoinModule(Ctx ctx) {
        super(ctx);
    }

    @Override
    public String id() {
        return "join";
    }

    @Override
    public void enable() {
        listen(this);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (!player.hasPlayedBefore() && ctx.config().getBoolean("join.announce-first-join", true)) {
            Bukkit.broadcastMessage(ctx.messages.raw(Msg.PREFIX) + ctx.messages.raw(Msg.JOIN_FIRST, "player", player.getName()));
        }
        if (ctx.config().getBoolean("join.custom-messages", true)) {
            String text = ctx.messages.raw(Msg.JOIN_MESSAGE, "player", player.getName());
            event.setJoinMessage(text.isEmpty() ? null : text);
        }
        final List<String> motd = ctx.config().getStringList("join.motd");
        final boolean title = ctx.config().getBoolean("join.welcome-title", true);
        if (!motd.isEmpty() || title) {
            ctx.scheduler.runForEntityLater(player, new Runnable() {
                @Override
                public void run() {
                    for (String line : ctx.messages.formatAll(motd, "player", player.getName())) {
                        player.sendMessage(line);
                    }
                    if (title) {
                        PlayerCompat.sendTitle(player, ctx.messages.raw(Msg.JOIN_TITLE, "player", player.getName()),
                            ctx.messages.raw(Msg.JOIN_SUBTITLE, "player", player.getName()), 10, 50, 10);
                    }
                }
            }, null, 20);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        if (ctx.config().getBoolean("join.custom-messages", true)) {
            String text = ctx.messages.raw(Msg.JOIN_QUIT, "player", event.getPlayer().getName());
            event.setQuitMessage(text.isEmpty() ? null : text);
        }
    }
}
