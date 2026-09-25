package fr.minebed.hub.ultimate;

import fr.minebed.hub.compat.PlayerCompat;
import fr.minebed.hub.compat.Sounds;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * La commande /ultimatecore (/uc) : aide, rechargement, informations, auto-test et outils utilisables depuis la console
 * (annonce, titre, barre d'action, son), pratiques pour les récompenses du site.
 */
final class CoreModule extends AbstractModule {

    private static final List<String> SUBCOMMANDS = Arrays.asList("help", "version", "reload", "info", "selftest", "broadcast", "title", "actionbar", "sound");

    private final UltimatePlugin plugin;

    CoreModule(Ctx ctx, UltimatePlugin plugin) {
        super(ctx);
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "core";
    }

    @Override
    public void enable() {
        command("ultimatecore", new BaseCommand(ctx, null) {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
                boolean open = sub.equals("help") || sub.equals("version");
                if (!open && !sender.hasPermission("ultimatecore.admin")) {
                    ctx.messages.send(sender, Msg.NO_PERMISSION);
                    return true;
                }
                String[] rest = args.length == 0 ? args : Arrays.copyOfRange(args, 1, args.length);
                switch (sub) {
                    case "help":
                        help(sender);
                        return true;
                    case "version":
                        sender.sendMessage(ctx.messages.raw(Msg.PREFIX) + "UltimateCore " + plugin.getDescription().getVersion() + " (" + ctx.platform + ")");
                        return true;
                    case "reload":
                        plugin.reloadAll();
                        ctx.messages.send(sender, Msg.CORE_RELOADED);
                        return true;
                    case "info":
                        info(sender);
                        return true;
                    case "selftest":
                        new SelfTest(ctx, plugin).run(sender);
                        return true;
                    case "broadcast":
                        return broadcast(sender, rest);
                    case "title":
                        return title(sender, rest);
                    case "actionbar":
                        return actionBar(sender, rest);
                    case "sound":
                        return sound(sender, rest);
                    default:
                        return false;
                }
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    return filter(SUBCOMMANDS, args[0]);
                }
                String sub = args[0].toLowerCase(Locale.ROOT);
                if (args.length == 2 && (sub.equals("title") || sub.equals("actionbar") || sub.equals("sound"))) {
                    List<String> names = onlineNames(args[1]);
                    if ("*".startsWith(args[1])) {
                        names.add("*");
                    }
                    return names;
                }
                return Collections.emptyList();
            }
        });
    }

    private void help(CommandSender sender) {
        ctx.messages.send(sender, Msg.CORE_HELP_HEADER);
        Map<String, Map<String, Object>> commands = plugin.getDescription().getCommands();
        List<String> names = new ArrayList<String>(commands.keySet());
        Collections.sort(names);
        for (String name : names) {
            Map<String, Object> info = commands.get(name);
            Object usage = info.get("usage");
            Object description = info.get("description");
            ctx.messages.send(sender, Msg.CORE_HELP_LINE, "usage", usage == null ? "/" + name : String.valueOf(usage).replace("<command>", name),
                "description", description == null ? "" : String.valueOf(description));
        }
    }

    private void info(CommandSender sender) {
        line(sender, "Version", plugin.getDescription().getVersion());
        line(sender, "Plateforme", ctx.platform.toString());
        line(sender, "Java", System.getProperty("java.version"));
        line(sender, "Modules", plugin.activeModuleIds().isEmpty() ? "-" : String.join(", ", plugin.activeModuleIds()));
        line(sender, "Joueurs", String.valueOf(Bukkit.getOnlinePlayers().size()));
        line(sender, "Langue", ctx.config().getString("language", "fr"));
    }

    private void line(CommandSender sender, String label, String value) {
        ctx.messages.send(sender, Msg.CORE_INFO_LINE, "label", label, "value", value);
    }

    private boolean broadcast(CommandSender sender, String[] rest) {
        if (rest.length == 0) {
            return false;
        }
        String text = ctx.messages.format(String.join(" ", rest));
        String line = ctx.messages.raw(Msg.CORE_BROADCAST, "message", text);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(line);
        }
        sender.sendMessage(line);
        return true;
    }

    /** Les joueurs visés : un pseudo, ou « * » pour tous. {@code null} (et message d'erreur) si introuvable. */
    private List<Player> targets(CommandSender sender, String who) {
        List<Player> out = new ArrayList<Player>();
        if (who.equals("*")) {
            out.addAll(Bukkit.getOnlinePlayers());
            return out;
        }
        Player p = ctx.findOnline(who);
        if (p == null) {
            ctx.messages.send(sender, Msg.PLAYER_NOT_FOUND, "name", who);
            return null;
        }
        out.add(p);
        return out;
    }

    private boolean title(CommandSender sender, String[] rest) {
        if (rest.length < 2) {
            return false;
        }
        final List<Player> targets = targets(sender, rest[0]);
        if (targets == null) {
            return true;
        }
        String joined = String.join(" ", Arrays.copyOfRange(rest, 1, rest.length));
        int bar = joined.indexOf('|');
        final String title = ctx.messages.format(bar < 0 ? joined : joined.substring(0, bar).trim());
        final String subtitle = ctx.messages.format(bar < 0 ? "" : joined.substring(bar + 1).trim());
        for (final Player p : targets) {
            ctx.runOnPlayer(p, new Runnable() {
                @Override
                public void run() {
                    PlayerCompat.sendTitle(p, title, subtitle, 10, 60, 20);
                }
            });
        }
        ctx.messages.send(sender, Msg.CORE_SENT, "count", String.valueOf(targets.size()));
        return true;
    }

    private boolean actionBar(CommandSender sender, String[] rest) {
        if (rest.length < 2) {
            return false;
        }
        List<Player> targets = targets(sender, rest[0]);
        if (targets == null) {
            return true;
        }
        final String text = ctx.messages.format(String.join(" ", Arrays.copyOfRange(rest, 1, rest.length)));
        for (final Player p : targets) {
            ctx.runOnPlayer(p, new Runnable() {
                @Override
                public void run() {
                    PlayerCompat.sendActionBar(p, text);
                }
            });
        }
        ctx.messages.send(sender, Msg.CORE_SENT, "count", String.valueOf(targets.size()));
        return true;
    }

    private boolean sound(CommandSender sender, String[] rest) {
        if (rest.length < 2 || rest.length > 4) {
            return false;
        }
        List<Player> targets = targets(sender, rest[0]);
        if (targets == null) {
            return true;
        }
        final String name = rest[1];
        final float volume = number(rest, 2, 1f);
        final float pitch = number(rest, 3, 1f);
        int played = 0;
        for (final Player p : targets) {
            if (Sounds.resolve(name) != null) {
                played++;
                ctx.runOnPlayer(p, new Runnable() {
                    @Override
                    public void run() {
                        Sounds.play(p, name, volume, pitch);
                    }
                });
            }
        }
        ctx.messages.send(sender, Msg.CORE_SENT, "count", String.valueOf(played));
        return true;
    }

    private static float number(String[] args, int index, float fallback) {
        if (args.length <= index) {
            return fallback;
        }
        try {
            float value = Float.parseFloat(args[index]);
            return Float.isNaN(value) || Float.isInfinite(value) ? fallback : Math.max(0f, Math.min(value, 4f));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
