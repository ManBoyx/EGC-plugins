package fr.minebed.hub.ultimate.modules;

import fr.minebed.hub.ultimate.AbstractModule;
import fr.minebed.hub.ultimate.BaseCommand;
import fr.minebed.hub.ultimate.Ctx;
import fr.minebed.hub.ultimate.Msg;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

/**
 * /info [sujet] et ses raccourcis (/rules, /discord, /vote, /store…) : le texte de chaque sujet est dans
 * {@code info-topics} de la configuration. Le raccourci utilisé donne le sujet.
 */
public final class InfoModule extends AbstractModule {

    public InfoModule(Ctx ctx) {
        super(ctx);
    }

    @Override
    public String id() {
        return "info";
    }

    @Override
    public void enable() {
        command("info", new BaseCommand(ctx, "ultimatecore.info") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                String topic = args.length > 0 ? args[0] : label;
                if (args.length > 1) {
                    return false;
                }
                ConfigurationSection topics = ctx.config().getConfigurationSection("info-topics");
                List<String> lines = topics == null ? Collections.<String>emptyList() : topics.getStringList(alias(topic));
                if (lines.isEmpty()) {
                    Set<String> names = topics == null ? Collections.<String>emptySet() : topics.getKeys(false);
                    ctx.messages.send(sender, Msg.INFO_UNKNOWN, "topic", topic);
                    ctx.messages.send(sender, Msg.INFO_TOPICS, "topics", String.join(", ", names));
                    return true;
                }
                for (String line : ctx.messages.formatAll(lines, "player", sender.getName())) {
                    sender.sendMessage(line);
                }
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                ConfigurationSection topics = ctx.config().getConfigurationSection("info-topics");
                return args.length == 1 && topics != null ? filter(topics.getKeys(false), args[0]) : Collections.<String>emptyList();
            }
        });
    }

    /** Les raccourcis français et anglais désignent le même sujet. */
    private static String alias(String label) {
        String l = label.toLowerCase(Locale.ROOT);
        switch (l) {
            case "regles": case "règles": return "rules";
            case "boutique": return "store";
            case "site": return "website";
            default: return l;
        }
    }
}
