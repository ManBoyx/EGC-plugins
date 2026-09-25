package fr.minebed.hub.ultimate.modules;

import fr.minebed.hub.common.time.DurationParser;
import fr.minebed.hub.compat.Enchants;
import fr.minebed.hub.compat.Materials;
import fr.minebed.hub.ultimate.AbstractModule;
import fr.minebed.hub.ultimate.BaseCommand;
import fr.minebed.hub.ultimate.Ctx;
import fr.minebed.hub.ultimate.Msg;
import fr.minebed.hub.ultimate.logic.PermissionLimits;
import fr.minebed.hub.ultimate.util.DataStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Kits définis dans {@code config.yml} : objets (noms de matériaux et d'enchantements valables avant comme après la 1.13),
 * temps de recharge conservé d'un redémarrage à l'autre, permission par kit. « /kit NOM JOUEUR » marche depuis la console
 * pour les récompenses du site.
 */
public final class KitsModule extends AbstractModule {

    private static final int MAX_STACKS_PER_ITEM = 36;

    private static final class Kit {
        final String name;
        final List<ItemStack> items = new ArrayList<ItemStack>();
        long cooldownMillis;
        boolean once;
        String permission;

        Kit(String name) {
            this.name = name;
        }
    }

    private final Map<String, Kit> kits = new LinkedHashMap<String, Kit>();
    private DataStore cooldowns;

    public KitsModule(Ctx ctx) {
        super(ctx);
    }

    @Override
    public String id() {
        return "kits";
    }

    @Override
    public void enable() {
        cooldowns = new DataStore(ctx.plugin, ctx.scheduler, "kit-cooldowns.yml");
        loadKits();
        BaseCommand kit = new BaseCommand(ctx, "ultimatecore.kit") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length == 0) {
                    listFor(sender);
                    return true;
                }
                if (args.length > 2) {
                    return false;
                }
                Kit found = kits.get(args[0].toLowerCase(Locale.ROOT));
                if (found == null) {
                    ctx.messages.send(sender, Msg.KIT_NOT_FOUND, "name", args[0]);
                    return true;
                }
                if (args.length == 2) {
                    if (!sender.hasPermission("ultimatecore.kit.give")) {
                        ctx.messages.send(sender, Msg.NO_PERMISSION);
                        return true;
                    }
                    final Player target = requireOnline(sender, args[1]);
                    if (target != null) {
                        final Kit kitToGive = found;
                        ctx.runOnPlayer(target, new Runnable() {
                            @Override
                            public void run() {
                                give(target, kitToGive);
                            }
                        });
                        ctx.messages.send(sender, Msg.KIT_GIVEN, "name", found.name, "player", target.getName());
                    }
                    return true;
                }
                Player player = requirePlayer(sender);
                if (player == null) {
                    return true;
                }
                if (!canUse(player, found)) {
                    ctx.messages.send(player, Msg.KIT_NO_PERMISSION, "name", found.name);
                    return true;
                }
                long left = remaining(player, found);
                if (left > 0) {
                    ctx.messages.send(player, Msg.KIT_COOLDOWN, "name", found.name, "time", left == Long.MAX_VALUE ? "∞" : DurationParser.format(left, "j", "h", "min", "s"));
                    return true;
                }
                give(player, found);
                start(player, found);
                ctx.messages.send(player, Msg.KIT_RECEIVED, "name", found.name);
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    return filter(usableNames(sender), args[0]);
                }
                return args.length == 2 && sender.hasPermission("ultimatecore.kit.give") ? onlineNames(args[1]) : Collections.<String>emptyList();
            }
        };
        command("kit", kit);
        command("kits", new BaseCommand(ctx, "ultimatecore.kit") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                listFor(sender);
                return true;
            }
        });
    }

    @Override
    protected void onDisable() {
        cooldowns.flush();
    }

    @Override
    public void reload() {
        loadKits();
    }

    /** Lit la section {@code kits} ; un objet invalide est signalé et ignoré, jamais fatal. */
    private void loadKits() {
        kits.clear();
        ConfigurationSection section = ctx.config().getConfigurationSection("kits");
        if (section == null) {
            return;
        }
        for (String name : section.getKeys(false)) {
            if (!PermissionLimits.isValidName(name)) {
                ctx.plugin.getLogger().warning("Kit ignoré : « " + name + " » n'est pas un nom valable (lettres, chiffres, _ et -).");
                continue;
            }
            ConfigurationSection s = section.getConfigurationSection(name);
            if (s == null) {
                continue;
            }
            Kit kit = new Kit(name.toLowerCase(Locale.ROOT));
            kit.once = s.getBoolean("once", false);
            kit.permission = s.getString("permission");
            String cooldown = s.getString("cooldown", "0");
            OptionalLong parsed = DurationParser.parseMillis(cooldown);
            kit.cooldownMillis = parsed.isPresent() ? parsed.getAsLong() : 0;
            if (!parsed.isPresent() && !"0".equals(cooldown.trim())) {
                ctx.plugin.getLogger().warning("Kit " + name + " : durée « " + cooldown + " » invalide, aucun temps de recharge.");
            }
            for (Map<?, ?> raw : s.getMapList("items")) {
                buildItems(kit, raw);
            }
            if (kit.items.isEmpty()) {
                ctx.plugin.getLogger().warning("Kit " + name + " : aucun objet valable, kit ignoré.");
                continue;
            }
            kits.put(kit.name, kit);
        }
    }

    private void buildItems(Kit kit, Map<?, ?> raw) {
        Object materialName = raw.get("material");
        Material material = materialName == null ? null : Materials.resolve(String.valueOf(materialName));
        if (material == null || material == Material.AIR) {
            ctx.plugin.getLogger().warning("Kit " + kit.name + " : matériau inconnu ou vide « " + materialName + " » sur cette version, objet ignoré.");
            return;
        }
        int amount = intOf(raw.get("amount"), 1);
        amount = Math.max(1, Math.min(amount, MAX_STACKS_PER_ITEM * 64));
        int stackSize = Math.max(1, Math.min(material.getMaxStackSize() <= 0 ? 1 : material.getMaxStackSize(), 64));
        while (amount > 0) {
            int part = Math.min(amount, stackSize);
            amount -= part;
            ItemStack stack = new ItemStack(material, part);
            applyMeta(kit, stack, raw);
            kit.items.add(stack);
            if (kit.items.size() >= MAX_STACKS_PER_ITEM * 4) {
                break;
            }
        }
    }

    private void applyMeta(Kit kit, ItemStack stack, Map<?, ?> raw) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        Object name = raw.get("name");
        if (name != null) {
            meta.setDisplayName(ctx.messages.format(String.valueOf(name)));
        }
        Object lore = raw.get("lore");
        if (lore instanceof List) {
            List<String> lines = new ArrayList<String>();
            for (Object line : (List<?>) lore) {
                lines.add(ctx.messages.format(String.valueOf(line)));
            }
            meta.setLore(lines);
        }
        Object enchants = raw.get("enchantments");
        if (enchants instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) enchants).entrySet()) {
                Enchantment enchantment = Enchants.resolve(String.valueOf(e.getKey()));
                if (enchantment == null) {
                    ctx.plugin.getLogger().warning("Kit " + kit.name + " : enchantement inconnu « " + e.getKey() + " » sur cette version, ignoré.");
                    continue;
                }
                meta.addEnchant(enchantment, Math.max(1, Math.min(intOf(e.getValue(), 1), 255)), true);
            }
        }
        stack.setItemMeta(meta);
    }

    private static int intOf(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean canUse(CommandSender who, Kit kit) {
        return who.hasPermission(kit.permission != null ? kit.permission : "ultimatecore.kit.use." + kit.name) || who.hasPermission("ultimatecore.kit.use.*");
    }

    private List<String> usableNames(CommandSender who) {
        List<String> out = new ArrayList<String>();
        for (Kit kit : kits.values()) {
            if (canUse(who, kit)) {
                out.add(kit.name);
            }
        }
        return out;
    }

    private void listFor(CommandSender sender) {
        List<String> names = usableNames(sender);
        if (names.isEmpty()) {
            ctx.messages.send(sender, Msg.KIT_NONE);
        } else {
            ctx.messages.send(sender, Msg.KIT_LIST, "kits", String.join(", ", names));
        }
    }

    /** Millisecondes avant de pouvoir reprendre le kit ; {@link Long#MAX_VALUE} pour un kit à usage unique déjà pris. */
    private long remaining(Player player, Kit kit) {
        long until = cooldowns.getLong(player.getUniqueId() + "." + kit.name, 0);
        if (until == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(0, until - System.currentTimeMillis());
    }

    private void start(Player player, Kit kit) {
        if (kit.once) {
            cooldowns.set(player.getUniqueId() + "." + kit.name, Long.MAX_VALUE);
        } else if (kit.cooldownMillis > 0) {
            cooldowns.set(player.getUniqueId() + "." + kit.name, System.currentTimeMillis() + kit.cooldownMillis);
        }
    }

    /** Donne les objets ; ce qui ne rentre pas dans l'inventaire tombe aux pieds du joueur. */
    private void give(Player player, Kit kit) {
        boolean dropped = false;
        for (ItemStack template : kit.items) {
            Map<Integer, ItemStack> left = player.getInventory().addItem(template.clone());
            for (ItemStack rest : left.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rest);
                dropped = true;
            }
        }
        if (dropped) {
            ctx.messages.send(player, Msg.KIT_DROPPED);
        }
    }

    /** Nombre de kits valables chargés (auto-test et journal de démarrage). */
    public int kitCount() {
        return kits.size();
    }
}
