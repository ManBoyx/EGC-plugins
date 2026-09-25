package fr.minebed.hub.ultimate.modules;

import fr.minebed.hub.compat.PlayerCompat;
import fr.minebed.hub.ultimate.AbstractModule;
import fr.minebed.hub.ultimate.BaseCommand;
import fr.minebed.hub.ultimate.Ctx;
import fr.minebed.hub.ultimate.Msg;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Outils courants : /heal, /feed, /fly, /gamemode, /speed, /ping. Chacun accepte un joueur en argument (console comprise). */
public final class UtilityModule extends AbstractModule {

    public UtilityModule(Ctx ctx) {
        super(ctx);
    }

    @Override
    public String id() {
        return "utility";
    }

    /** Une action sur soi ou, avec permission « .others », sur un autre joueur. */
    private abstract class OnPlayer extends BaseCommand {
        private final String base;

        OnPlayer(String permission) {
            super(UtilityModule.this.ctx, permission);
            this.base = permission;
        }

        abstract void apply(CommandSender sender, Player target, boolean self, String[] rest);

        int firstArg() {
            return 0;
        }

        int maxArgs() {
            return 1;
        }

        @Override
        protected boolean execute(CommandSender sender, String label, String[] args) {
            if (args.length > maxArgs()) {
                return false;
            }
            final int at = firstArg();
            if (args.length > at) {
                if (!sender.hasPermission(base + ".others")) {
                    ctx.messages.send(sender, Msg.NO_PERMISSION);
                    return true;
                }
                final Player target = requireOnline(sender, args[at]);
                if (target != null) {
                    final String[] rest = args;
                    final CommandSender from = sender;
                    ctx.runOnPlayer(target, new Runnable() {
                        @Override
                        public void run() {
                            apply(from, target, false, rest);
                        }
                    });
                }
                return true;
            }
            Player me = requirePlayer(sender);
            if (me != null) {
                apply(sender, me, true, args);
            }
            return true;
        }

        @Override
        protected List<String> complete(CommandSender sender, String[] args) {
            return args.length == firstArg() + 1 && sender.hasPermission(base + ".others") ? onlineNames(args[args.length - 1]) : Collections.<String>emptyList();
        }
    }

    @Override
    public void enable() {
        command("heal", new OnPlayer("ultimatecore.heal") {
            @Override
            void apply(CommandSender sender, Player target, boolean self, String[] rest) {
                target.setHealth(Math.max(1.0, target.getMaxHealth()));
                target.setFoodLevel(20);
                target.setFireTicks(0);
                ctx.messages.send(target, Msg.UTIL_HEALED);
                if (!self) {
                    ctx.messages.send(sender, Msg.UTIL_DONE_FOR, "player", target.getName());
                }
            }
        });
        command("feed", new OnPlayer("ultimatecore.feed") {
            @Override
            void apply(CommandSender sender, Player target, boolean self, String[] rest) {
                target.setFoodLevel(20);
                target.setSaturation(20f);
                ctx.messages.send(target, Msg.UTIL_FED);
                if (!self) {
                    ctx.messages.send(sender, Msg.UTIL_DONE_FOR, "player", target.getName());
                }
            }
        });
        command("fly", new OnPlayer("ultimatecore.fly") {
            @Override
            void apply(CommandSender sender, Player target, boolean self, String[] rest) {
                boolean now = !target.getAllowFlight();
                target.setAllowFlight(now);
                if (!now) {
                    target.setFlying(false);
                }
                ctx.messages.send(target, now ? Msg.UTIL_FLY_ON : Msg.UTIL_FLY_OFF);
                if (!self) {
                    ctx.messages.send(sender, Msg.UTIL_DONE_FOR, "player", target.getName());
                }
            }
        });
        command("gamemode", new OnPlayer("ultimatecore.gamemode") {
            @Override
            int firstArg() {
                return 1;
            }

            @Override
            int maxArgs() {
                return 2;
            }

            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length == 0) {
                    return false;
                }
                if (parseMode(args[0]) == null) {
                    ctx.messages.send(sender, Msg.UTIL_GAMEMODE_INVALID);
                    return true;
                }
                return super.execute(sender, label, args);
            }

            @Override
            void apply(CommandSender sender, Player target, boolean self, String[] rest) {
                GameMode mode = parseMode(rest[0]);
                target.setGameMode(mode);
                ctx.messages.send(target, Msg.UTIL_GAMEMODE, "mode", mode.name().toLowerCase(Locale.ROOT));
                if (!self) {
                    ctx.messages.send(sender, Msg.UTIL_DONE_FOR, "player", target.getName());
                }
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    return filter(Arrays.asList("survival", "creative", "adventure", "spectator"), args[0]);
                }
                return super.complete(sender, args);
            }
        });
        command("speed", new OnPlayer("ultimatecore.speed") {
            @Override
            int firstArg() {
                return 1;
            }

            @Override
            int maxArgs() {
                return 2;
            }

            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length == 0) {
                    return false;
                }
                if (parseSpeed(args[0]) < 0) {
                    ctx.messages.send(sender, Msg.UTIL_SPEED_INVALID);
                    return true;
                }
                return super.execute(sender, label, args);
            }

            @Override
            void apply(CommandSender sender, Player target, boolean self, String[] rest) {
                int level = parseSpeed(rest[0]);
                float value = level / 10f;
                if (target.isFlying()) {
                    target.setFlySpeed(value);
                } else {
                    target.setWalkSpeed(value);
                }
                ctx.messages.send(target, Msg.UTIL_SPEED, "value", String.valueOf(level));
                if (!self) {
                    ctx.messages.send(sender, Msg.UTIL_DONE_FOR, "player", target.getName());
                }
            }
        });
        command("ping", new BaseCommand(ctx, "ultimatecore.ping") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length > 1) {
                    return false;
                }
                Player target;
                boolean self = args.length == 0;
                if (self) {
                    target = requirePlayer(sender);
                } else {
                    if (!sender.hasPermission("ultimatecore.ping.others")) {
                        ctx.messages.send(sender, Msg.NO_PERMISSION);
                        return true;
                    }
                    target = requireOnline(sender, args[0]);
                }
                if (target == null) {
                    return true;
                }
                int ping = PlayerCompat.ping(target);
                if (ping < 0) {
                    ctx.messages.send(sender, Msg.UTIL_PING_UNKNOWN);
                } else if (self) {
                    ctx.messages.send(sender, Msg.UTIL_PING, "ping", String.valueOf(ping));
                } else {
                    ctx.messages.send(sender, Msg.UTIL_PING_OTHER, "player", target.getName(), "ping", String.valueOf(ping));
                }
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 && sender.hasPermission("ultimatecore.ping.others") ? onlineNames(args[0]) : Collections.<String>emptyList();
            }
        });
    }

    /** « 0/s/survival », « 1/c/creative », « 2/a/adventure », « 3/sp/spectator ». */
    static GameMode parseMode(String text) {
        String t = text.toLowerCase(Locale.ROOT);
        if (t.equals("0") || t.equals("s") || t.equals("survival") || t.equals("survie")) {
            return GameMode.SURVIVAL;
        }
        if (t.equals("1") || t.equals("c") || t.equals("creative") || t.equals("créatif") || t.equals("creatif")) {
            return GameMode.CREATIVE;
        }
        if (t.equals("2") || t.equals("a") || t.equals("adventure") || t.equals("aventure")) {
            return GameMode.ADVENTURE;
        }
        if (t.equals("3") || t.equals("sp") || t.equals("spectator") || t.equals("spectateur")) {
            return GameMode.valueOf("SPECTATOR");
        }
        return null;
    }

    /** Niveau de 1 à 10 (vitesse de marche par défaut : 2 ; de vol : 1) ; -1 si invalide. */
    static int parseSpeed(String text) {
        try {
            int level = Integer.parseInt(text.trim());
            return level >= 1 && level <= 10 ? level : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
