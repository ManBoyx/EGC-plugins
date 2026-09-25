package fr.minebed.hub.ultimate.modules;

import fr.minebed.hub.ultimate.AbstractModule;
import fr.minebed.hub.ultimate.BaseCommand;
import fr.minebed.hub.ultimate.Ctx;
import fr.minebed.hub.ultimate.Msg;
import fr.minebed.hub.ultimate.logic.ChatGuard;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Modération du chat (anti-spam, répétitions, cris, mots interdits, liens), format optionnel et /clearchat. */
public final class ChatModule extends AbstractModule implements Listener {

    private static final String BYPASS = "ultimatecore.chat.bypass";

    private volatile ChatGuard guard;

    public ChatModule(Ctx ctx) {
        super(ctx);
    }

    @Override
    public String id() {
        return "chat";
    }

    @Override
    public void enable() {
        buildGuard();
        command("clearchat", new BaseCommand(ctx, "ultimatecore.chat.clear") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                String who = sender instanceof Player ? sender.getName() : "console";
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.hasPermission(BYPASS)) {
                        for (int i = 0; i < 100; i++) {
                            p.sendMessage(" ");
                        }
                    }
                    ctx.messages.send(p, Msg.CHAT_CLEARED, "player", who);
                }
                if (!(sender instanceof Player)) {
                    ctx.messages.send(sender, Msg.CHAT_CLEARED, "player", who);
                }
                return true;
            }
        });
        listen(this);
    }

    @Override
    public void reload() {
        buildGuard();
    }

    private void buildGuard() {
        ChatGuard.Settings s = new ChatGuard.Settings();
        s.maxMessages = Math.max(1, ctx.config().getInt("chat.anti-spam.max-messages", 4));
        s.windowMillis = Math.max(1, ctx.config().getLong("chat.anti-spam.seconds", 5)) * 1000L;
        s.repeatSimilarity = Math.min(1.0, Math.max(0.1, ctx.config().getDouble("chat.anti-repeat.similarity", 0.85)));
        s.capsMinLetters = ctx.config().getInt("chat.anti-caps.min-letters", 8);
        s.capsMaxPercent = ctx.config().getInt("chat.anti-caps.max-percent", 60);
        s.capsLowercase = "lowercase".equalsIgnoreCase(ctx.config().getString("chat.anti-caps.action", "lowercase"));
        s.forbiddenWords = new ArrayList<String>(ctx.config().getStringList("chat.forbidden-words"));
        s.blockLinks = ctx.config().getBoolean("chat.block-links", false);
        s.allowedDomains = new ArrayList<String>(ctx.config().getStringList("chat.allowed-domains"));
        guard = new ChatGuard(s, System::currentTimeMillis);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(BYPASS)) {
            ChatGuard.Verdict verdict = guard.check(player.getUniqueId(), event.getMessage());
            if (!verdict.allowed()) {
                event.setCancelled(true);
                switch (verdict.reason()) {
                    case SPAM: ctx.messages.send(player, Msg.CHAT_SPAM); break;
                    case REPEAT: ctx.messages.send(player, Msg.CHAT_REPEAT); break;
                    case CAPS: ctx.messages.send(player, Msg.CHAT_CAPS); break;
                    case WORD: ctx.messages.send(player, Msg.CHAT_WORD); break;
                    case LINK: ctx.messages.send(player, Msg.CHAT_LINK); break;
                    default: break;
                }
                return;
            }
            event.setMessage(verdict.message());
        }
        if (ctx.config().getBoolean("chat.format.enabled", false)) {
            String template = ctx.config().getString("chat.format.text", "&7{player}&8 : &f{message}");
            // Le format de Bukkit utilise « %1$s » (pseudo) et « %2$s » (message) : tout autre « % » doit être doublé.
            String safe = template.replace("%", "%%").replace("{player}", "%1$s").replace("{message}", "%2$s");
            event.setFormat(ctx.messages.format(safe));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        guard.forget(event.getPlayer().getUniqueId());
    }

    /** Pour l'auto-test. */
    public ChatGuard guard() {
        return guard;
    }
}
