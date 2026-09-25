package fr.minebed.hub.ultimate.modules;

import fr.minebed.hub.common.money.Amounts;
import fr.minebed.hub.ultimate.AbstractModule;
import fr.minebed.hub.ultimate.BaseCommand;
import fr.minebed.hub.ultimate.Ctx;
import fr.minebed.hub.ultimate.Msg;
import fr.minebed.hub.ultimate.logic.Ledger;
import fr.minebed.hub.ultimate.util.DataStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Économie interne : /balance, /pay, /baltop et /eco (donner, retirer, fixer, remettre à zéro : utilisable depuis la
 * console, y compris pour un joueur hors ligne connu). Les soldes sont des centimes entiers, enregistrés à chaque changement.
 */
public final class EconomyModule extends AbstractModule implements Listener {

    private final Ledger ledger = new Ledger();
    private DataStore store;

    public EconomyModule(Ctx ctx) {
        super(ctx);
    }

    @Override
    public String id() {
        return "economy";
    }

    /** Pour les autres plugins et modules. */
    public Ledger ledger() {
        return ledger;
    }

    @Override
    public void enable() {
        store = new DataStore(ctx.plugin, ctx.scheduler, "economy.yml");
        Map<UUID, Long> saved = new java.util.HashMap<UUID, Long>();
        for (String key : store.keys("balances")) {
            try {
                saved.put(UUID.fromString(key), store.getLong("balances." + key, 0));
            } catch (IllegalArgumentException e) {
                ctx.plugin.getLogger().warning("economy.yml : compte ignoré (identifiant invalide) : " + key);
            }
        }
        ledger.load(saved);

        command("balance", new BaseCommand(ctx, "ultimatecore.balance") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length > 1) {
                    return false;
                }
                if (args.length == 1) {
                    if (!sender.hasPermission("ultimatecore.balance.others")) {
                        ctx.messages.send(sender, Msg.NO_PERMISSION);
                        return true;
                    }
                    UUID account = resolve(args[0]);
                    if (account == null) {
                        ctx.messages.send(sender, Msg.PLAYER_NOT_FOUND, "name", args[0]);
                        return true;
                    }
                    ctx.messages.send(sender, Msg.ECO_BALANCE_OTHER, "player", nameOf(account, args[0]), "amount", money(ledger.balance(account)));
                    return true;
                }
                Player player = requirePlayer(sender);
                if (player != null) {
                    ctx.messages.send(player, Msg.ECO_BALANCE, "amount", money(ledger.balance(player.getUniqueId())));
                }
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 && sender.hasPermission("ultimatecore.balance.others") ? onlineNames(args[0]) : Collections.<String>emptyList();
            }
        });
        command("pay", new BaseCommand(ctx, "ultimatecore.pay") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (args.length != 2) {
                    return false;
                }
                UUID to = resolve(args[0]);
                if (to == null) {
                    ctx.messages.send(player, Msg.PLAYER_NOT_FOUND, "name", args[0]);
                    return true;
                }
                OptionalDouble amount = Amounts.parsePositive(args[1]);
                if (!amount.isPresent() || amount.getAsDouble() < ctx.config().getDouble("economy.min-payment", 0.01)) {
                    ctx.messages.send(player, Msg.ECO_INVALID_AMOUNT);
                    return true;
                }
                long cents = Ledger.toCents(amount.getAsDouble());
                Ledger.Result result = ledger.transfer(player.getUniqueId(), to, cents);
                switch (result) {
                    case OK:
                        save(player.getUniqueId());
                        save(to);
                        String toName = nameOf(to, args[0]);
                        ctx.messages.send(player, Msg.ECO_PAID, "player", toName, "amount", money(cents));
                        Player online = Bukkit.getPlayer(to);
                        if (online != null) {
                            ctx.messages.send(online, Msg.ECO_RECEIVED, "player", player.getName(), "amount", money(cents));
                        }
                        break;
                    case INSUFFICIENT_FUNDS:
                        ctx.messages.send(player, Msg.ECO_NOT_ENOUGH);
                        break;
                    case SAME_ACCOUNT:
                        ctx.messages.send(player, Msg.ECO_SELF);
                        break;
                    case TOO_RICH:
                        ctx.messages.send(player, Msg.ECO_TOO_RICH, "player", nameOf(to, args[0]));
                        break;
                    default:
                        ctx.messages.send(player, Msg.ECO_INVALID_AMOUNT);
                        break;
                }
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(args[0]) : Collections.<String>emptyList();
            }
        });
        command("eco", new BaseCommand(ctx, "ultimatecore.eco") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length < 2 || args.length > 3) {
                    return false;
                }
                String action = args[0].toLowerCase(Locale.ROOT);
                if (!Arrays.asList("give", "take", "set", "reset").contains(action)) {
                    return false;
                }
                boolean needsAmount = !action.equals("reset");
                if (needsAmount != (args.length == 3)) {
                    return false;
                }
                UUID account = resolve(args[1]);
                if (account == null) {
                    ctx.messages.send(sender, Msg.PLAYER_NOT_FOUND, "name", args[1]);
                    return true;
                }
                long cents = 0;
                if (needsAmount) {
                    OptionalDouble amount = action.equals("set") ? parseNonNegative(args[2]) : Amounts.parsePositive(args[2]);
                    if (!amount.isPresent()) {
                        ctx.messages.send(sender, Msg.ECO_INVALID_AMOUNT);
                        return true;
                    }
                    cents = Ledger.toCents(amount.getAsDouble());
                }
                Ledger.Result result;
                if (action.equals("give")) {
                    result = ledger.deposit(account, cents);
                } else if (action.equals("take")) {
                    result = ledger.withdraw(account, cents);
                } else {
                    result = ledger.set(account, cents);
                }
                if (result == Ledger.Result.OK) {
                    save(account);
                    ctx.messages.send(sender, Msg.ECO_ADMIN_DONE, "player", nameOf(account, args[1]), "amount", money(ledger.balance(account)));
                } else if (result == Ledger.Result.INSUFFICIENT_FUNDS) {
                    ctx.messages.send(sender, Msg.ECO_NOT_ENOUGH);
                } else if (result == Ledger.Result.TOO_RICH) {
                    ctx.messages.send(sender, Msg.ECO_TOO_RICH, "player", nameOf(account, args[1]));
                } else {
                    ctx.messages.send(sender, Msg.ECO_INVALID_AMOUNT);
                }
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    return filter(Arrays.asList("give", "take", "set", "reset"), args[0]);
                }
                return args.length == 2 ? onlineNames(args[1]) : Collections.<String>emptyList();
            }
        });
        command("baltop", new BaseCommand(ctx, "ultimatecore.baltop") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                List<Map.Entry<UUID, Long>> all = new ArrayList<Map.Entry<UUID, Long>>(ledger.snapshot().entrySet());
                if (all.isEmpty()) {
                    ctx.messages.send(sender, Msg.ECO_TOP_EMPTY);
                    return true;
                }
                Collections.sort(all, new Comparator<Map.Entry<UUID, Long>>() {
                    @Override
                    public int compare(Map.Entry<UUID, Long> a, Map.Entry<UUID, Long> b) {
                        return Long.compare(b.getValue(), a.getValue());
                    }
                });
                ctx.messages.send(sender, Msg.ECO_TOP_HEADER);
                for (int i = 0; i < Math.min(10, all.size()); i++) {
                    UUID id = all.get(i).getKey();
                    ctx.messages.send(sender, Msg.ECO_TOP_LINE, "rank", String.valueOf(i + 1), "player", nameOf(id, "?"), "amount", money(all.get(i).getValue()));
                }
                return true;
            }
        });
        listen(this);
    }

    @Override
    protected void onDisable() {
        store.flush();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        store.set("names." + player.getName().toLowerCase(Locale.ROOT), player.getUniqueId().toString());
        store.set("last-name." + player.getUniqueId(), player.getName());
        if (!store.contains("balances." + player.getUniqueId()) && !player.hasPlayedBefore()) {
            double start = ctx.config().getDouble("economy.starting-balance", 0);
            if (start > 0) {
                ledger.deposit(player.getUniqueId(), Ledger.toCents(start));
                save(player.getUniqueId());
            }
        }
    }

    private static OptionalDouble parseNonNegative(String text) {
        return "0".equals(text.trim()) ? OptionalDouble.of(0) : Amounts.parsePositive(text);
    }

    private void save(UUID account) {
        long cents = ledger.balance(account);
        store.set("balances." + account, cents == 0 ? null : cents);
    }

    /**
     * Compte d'un joueur en ligne, sinon d'un joueur déjà vu (pseudo enregistré) ; jamais de requête réseau vers Mojang.
     * Le pseudo doit être exact : pour de l'argent, pas de « début de pseudo » qui pourrait viser la mauvaise personne.
     */
    private UUID resolve(String name) {
        Player online = ctx.findOnlineExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        String id = store.getString("names." + name.toLowerCase(Locale.ROOT));
        try {
            return id == null ? null : UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String nameOf(UUID account, String fallback) {
        Player online = Bukkit.getPlayer(account);
        if (online != null) {
            return online.getName();
        }
        String saved = store.getString("last-name." + account);
        return saved == null ? fallback : saved;
    }

    private String money(long cents) {
        return Amounts.format(Ledger.fromCents(cents)) + ctx.config().getString("economy.currency", " $");
    }
}
