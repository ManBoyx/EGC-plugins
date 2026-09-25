package fr.minebed.hub.ultimate;

import fr.minebed.hub.common.text.ColorCodes;
import fr.minebed.hub.common.version.MinecraftVersion;
import fr.minebed.hub.compat.Enchants;
import fr.minebed.hub.compat.Materials;
import fr.minebed.hub.compat.Sounds;
import fr.minebed.hub.ultimate.util.DataStore;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Auto-test embarqué : « /uc selftest » vérifie sur le serveur réel tout ce qui dépend de la version du jeu (matériaux,
 * enchantements, sons, objets, planificateur, fichiers, messages, commandes). Il sert de test de fumée automatisé sur
 * chaque version (voir scripts/smoke-test.sh) et d'outil de diagnostic pour les administrateurs.
 * Ce qui demande un joueur connecté (titre, barre d'action, téléportation) n'est pas couvert ici.
 */
final class SelfTest {

    private final Ctx ctx;
    private final UltimatePlugin plugin;
    private final List<String> failures = new ArrayList<String>();
    private int total;

    SelfTest(Ctx ctx, UltimatePlugin plugin) {
        this.ctx = ctx;
        this.plugin = plugin;
    }

    void run(final CommandSender sender) {
        say(sender, "Auto-test sur " + ctx.platform + ", Java " + System.getProperty("java.version"));
        check(sender, "plateforme et version reconnues", ctx.platform.version() != null);
        check(sender, "version lue (" + ctx.platform.version() + ") cohérente avec Bukkit", MinecraftVersion.parse(Bukkit.getBukkitVersion()) != null);

        Material sword = Materials.resolve("WOOD_SWORD");
        Material sword2 = Materials.resolve("WOODEN_SWORD");
        check(sender, "matériau WOOD_SWORD / WOODEN_SWORD (ancien et nouveau nom)", sword != null && sword2 != null && sword == sword2);
        check(sender, "matériau DIAMOND_SWORD", Materials.resolve("diamond sword") != null);
        check(sender, "matériau inexistant refusé", Materials.resolve("PAS_UN_MATERIAU") == null);

        Enchantment sharp = Enchants.resolve("DAMAGE_ALL");
        Enchantment sharp2 = Enchants.resolve("sharpness");
        check(sender, "enchantement DAMAGE_ALL / SHARPNESS (ancien et nouveau nom)", sharp != null && sharp2 != null && sharp.equals(sharp2));
        check(sender, "enchantement DURABILITY / UNBREAKING", Enchants.resolve("DURABILITY") != null && Enchants.resolve("unbreaking") != null);

        check(sender, "son LEVEL_UP / ENTITY_PLAYER_LEVELUP", Sounds.resolve("LEVEL_UP") != null && Sounds.resolve("ENTITY_PLAYER_LEVELUP") != null);

        boolean itemOk = false;
        try {
            ItemStack stack = new ItemStack(sword == null ? Material.STONE : sword, 1);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(ctx.messages.format("&aTest"));
            if (sharp != null) {
                meta.addEnchant(sharp, 3, true);
            }
            stack.setItemMeta(meta);
            itemOk = stack.getItemMeta().getDisplayName().contains("Test") && (sharp == null || stack.getItemMeta().getEnchantLevel(sharp) == 3);
        } catch (Throwable t) {
            say(sender, "  exception : " + t);
        }
        check(sender, "objet avec nom et enchantement", itemOk);

        List<String> missing = new ArrayList<String>();
        for (Msg m : Msg.values()) {
            if (ctx.messages.raw(m).contains("message manquant")) {
                missing.add(m.key());
            }
        }
        check(sender, "tous les messages existent" + (missing.isEmpty() ? "" : " (manquants : " + missing + ")"), missing.isEmpty());

        String hex = ColorCodes.translate("&#ff8800Orange &aVert", ctx.platform.supportsHexColors());
        check(sender, "couleurs (hexadécimales " + (ctx.platform.supportsHexColors() ? "natives" : "converties") + ")", hex.indexOf('§') >= 0 && hex.indexOf('&') < 0);

        check(sender, "fichiers de données (écriture, relecture)", dataStoreRoundTrip());

        List<String> unbound = new ArrayList<String>();
        for (String name : plugin.getDescription().getCommands().keySet()) {
            PluginCommand c = plugin.getCommand(name);
            if (c == null || c.getExecutor() == plugin) {
                unbound.add(name);
            }
        }
        check(sender, "toutes les commandes de plugin.yml ont un exécuteur" + (unbound.isEmpty() ? "" : " (sans exécuteur : " + unbound + ")"), unbound.isEmpty());

        // Planificateur : fil principal (ou région globale), asynchrone, région d'un lieu, minuteur.
        final AtomicInteger sync = new AtomicInteger();
        final AtomicInteger async = new AtomicInteger();
        final AtomicInteger region = new AtomicInteger();
        final AtomicInteger timer = new AtomicInteger();
        ctx.scheduler.runGlobal(new Runnable() {
            @Override
            public void run() {
                sync.incrementAndGet();
            }
        });
        ctx.scheduler.runAsync(new Runnable() {
            @Override
            public void run() {
                async.incrementAndGet();
            }
        });
        if (!Bukkit.getWorlds().isEmpty()) {
            Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
            ctx.scheduler.runAtLocation(spawn, new Runnable() {
                @Override
                public void run() {
                    region.incrementAndGet();
                }
            });
        } else {
            region.incrementAndGet();
        }
        final fr.minebed.hub.compat.scheduler.Scheduler.Task[] holder = new fr.minebed.hub.compat.scheduler.Scheduler.Task[1];
        holder[0] = ctx.scheduler.runGlobalTimer(new Runnable() {
            @Override
            public void run() {
                if (timer.incrementAndGet() >= 2 && holder[0] != null) {
                    holder[0].cancel();
                }
            }
        }, 1, 1);
        ctx.scheduler.runGlobalLater(new Runnable() {
            @Override
            public void run() {
                check(sender, "planificateur : tâche principale", sync.get() == 1);
                check(sender, "planificateur : tâche asynchrone", async.get() == 1);
                check(sender, "planificateur : tâche de région", region.get() == 1);
                check(sender, "planificateur : minuteur répété puis annulé (" + timer.get() + " fois)", timer.get() == 2);
                finish(sender);
            }
        }, 20);
    }

    private boolean dataStoreRoundTrip() {
        try {
            DataStore store = new DataStore(plugin, ctx.scheduler, "selftest.yml");
            store.set("a.b", "valeur é");
            store.flush();
            DataStore again = new DataStore(plugin, ctx.scheduler, "selftest.yml");
            boolean same = "valeur é".equals(again.getString("a.b"));
            File file = new File(plugin.getDataFolder(), "selftest.yml");
            return same && (!file.exists() || file.delete());
        } catch (Throwable t) {
            return false;
        }
    }

    private void check(CommandSender sender, String label, boolean ok) {
        total++;
        if (!ok) {
            failures.add(label);
        }
        say(sender, (ok ? "OK    " : "ÉCHEC ") + label);
    }

    private void finish(CommandSender sender) {
        int failed = failures.size();
        String result = failed == 0 ? "PASS (" + total + "/" + total + ")" : "FAIL (" + failed + " échec(s) sur " + total + " : " + failures + ")";
        say(sender, "SELFTEST RESULT: " + result);
    }

    private void say(CommandSender sender, String text) {
        plugin.getLogger().info("[selftest] " + text);
        if (!(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            sender.sendMessage("§7[selftest] §f" + text);
        }
    }
}
