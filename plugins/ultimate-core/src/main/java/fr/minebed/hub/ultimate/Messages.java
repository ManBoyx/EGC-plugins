package fr.minebed.hub.ultimate;

import fr.minebed.hub.common.text.ColorCodes;
import fr.minebed.hub.common.text.Placeholders;
import fr.minebed.hub.compat.Platform;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Messages traduits. Ordre de recherche d'une clé : le fichier de l'administrateur (dossier {@code lang/}), puis le fichier
 * d'origine de la même langue dans le jar, puis le français du jar : une nouvelle version du plugin n'affiche donc jamais
 * de clé manquante, même si l'administrateur garde d'anciens fichiers.
 */
public final class Messages {

    private final Plugin plugin;
    private final Platform platform;
    private volatile YamlConfiguration user;
    private volatile YamlConfiguration bundled;
    private volatile YamlConfiguration french;

    public Messages(Plugin plugin, Platform platform) {
        this.plugin = plugin;
        this.platform = platform;
    }

    public void load(String requestedLanguage) {
        String code = requestedLanguage == null ? "fr" : requestedLanguage.toLowerCase(java.util.Locale.ROOT).trim();
        if (!code.matches("[a-z]{2,5}(_[a-z]{2,5})?")) {
            plugin.getLogger().warning("Langue invalide « " + requestedLanguage + " » : le français est utilisé.");
            code = "fr";
        }
        french = readBundled("fr");
        YamlConfiguration wanted = readBundled(code);
        if (wanted == null) {
            plugin.getLogger().warning("Aucun fichier de langue « " + code + " » dans le plugin : le français est utilisé.");
            code = "fr";
            wanted = french;
        }
        bundled = wanted;
        File file = new File(new File(plugin.getDataFolder(), "lang"), code + ".yml");
        if (!file.exists()) {
            plugin.saveResource("lang/" + code + ".yml", false);
        }
        user = readFile(file);
    }

    private YamlConfiguration readBundled(String code) {
        InputStream in = plugin.getResource("lang/" + code + ".yml");
        if (in == null) {
            return null;
        }
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(reader);
            return yaml;
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.SEVERE, "Fichier de langue interne illisible : " + code, e);
            return null;
        }
    }

    private YamlConfiguration readFile(File file) {
        try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(reader);
            return yaml;
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.WARNING, "Fichier de langue illisible (" + file.getName() + ") : les textes d'origine sont utilisés.", e);
            return null;
        }
    }

    private String lookup(String key) {
        String value = user == null ? null : user.getString(key);
        if (value == null && bundled != null) {
            value = bundled.getString(key);
        }
        if (value == null && french != null) {
            value = french.getString(key);
        }
        return value;
    }

    /** Le message, avec ses couleurs et jetons remplacés, sans préfixe. */
    public String raw(Msg msg, Object... pairs) {
        String template = lookup(msg.key());
        if (template == null) {
            return ColorCodes.translate("&c[message manquant : " + msg.key() + "]", false);
        }
        Map<String, String> values = Placeholders.of(pairs);
        return ColorCodes.translate(Placeholders.apply(template, values), platform.supportsHexColors());
    }

    /** Envoie le message précédé du préfixe ; un message vide dans le fichier de langue n'envoie rien. */
    public void send(CommandSender to, Msg msg, Object... pairs) {
        String text = raw(msg, pairs);
        if (text.isEmpty()) {
            return;
        }
        to.sendMessage(raw(Msg.PREFIX) + text);
    }

    /** Une chaîne de configuration quelconque, avec couleurs et jetons (annonces, sujets d'information…). */
    public String format(String template, Object... pairs) {
        return ColorCodes.translate(Placeholders.apply(template, Placeholders.of(pairs)), platform.supportsHexColors());
    }

    public List<String> formatAll(List<String> templates, Object... pairs) {
        java.util.ArrayList<String> out = new java.util.ArrayList<String>(templates.size());
        for (String t : templates) {
            out.add(format(t, pairs));
        }
        return out;
    }
}
