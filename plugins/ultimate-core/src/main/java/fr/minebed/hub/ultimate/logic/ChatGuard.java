package fr.minebed.hub.ultimate.logic;

import fr.minebed.hub.common.text.TextFilters;
import fr.minebed.hub.common.util.RateLimiter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/** Modération automatique du chat : trop de messages, répétition, cris, mots interdits, liens. Pur : aucune dépendance à Bukkit. */
public final class ChatGuard {

    public enum Reason { NONE, SPAM, REPEAT, CAPS, WORD, LINK }

    public static final class Verdict {
        private final Reason reason;
        private final String message;
        private final String detail;

        Verdict(Reason reason, String message, String detail) {
            this.reason = reason;
            this.message = message;
            this.detail = detail;
        }

        /** {@link Reason#NONE} si le message peut partir (éventuellement modifié : voir {@link #message()}). */
        public Reason reason() {
            return reason;
        }

        public boolean allowed() {
            return reason == Reason.NONE;
        }

        /** Le texte à publier (déjà corrigé si les majuscules ont été abaissées). */
        public String message() {
            return message;
        }

        /** Le mot interdit trouvé, ou le domaine du lien. */
        public String detail() {
            return detail;
        }
    }

    /** Réglages, avec des valeurs par défaut raisonnables. */
    public static final class Settings {
        public int maxMessages = 4;
        public long windowMillis = 5_000;
        public double repeatSimilarity = 0.85;
        public int capsMinLetters = 8;
        public int capsMaxPercent = 60;
        public boolean capsLowercase = true;
        public List<String> forbiddenWords = new ArrayList<String>();
        public boolean blockLinks = true;
        public List<String> allowedDomains = new ArrayList<String>();
    }

    private static final Pattern LINK = Pattern.compile(
        "(?i)\\b(?:https?://|www\\.)?((?:[a-z0-9-]+\\.)+(?:com|net|org|fr|io|gg|xyz|me|eu|info|club|fun|tk|ml|ga|cf|gq|to|ru|de|uk|us|co|tv|be|ch|ca))\\b");

    private final Settings settings;
    private final RateLimiter limiter;
    private final TextFilters.WordFilter words;
    private final Map<UUID, String> lastMessage = new HashMap<UUID, String>();

    public ChatGuard(Settings settings, LongSupplier clock) {
        this.settings = settings;
        this.limiter = new RateLimiter(Math.max(1, settings.maxMessages), Math.max(1, settings.windowMillis), clock);
        this.words = new TextFilters.WordFilter(settings.forbiddenWords);
    }

    public Verdict check(UUID player, String message) {
        String text = message == null ? "" : message.trim();
        if (text.isEmpty()) {
            return new Verdict(Reason.NONE, text, null);
        }
        String forbidden = words.find(text);
        if (forbidden != null) {
            return new Verdict(Reason.WORD, text, forbidden);
        }
        if (settings.blockLinks) {
            String domain = foreignDomain(text);
            if (domain != null) {
                return new Verdict(Reason.LINK, text, domain);
            }
        }
        if (!limiter.tryAcquire(player)) {
            return new Verdict(Reason.SPAM, text, null);
        }
        synchronized (lastMessage) {
            String previous = lastMessage.get(player);
            if (previous != null && text.length() >= 4 && TextFilters.similarity(previous, text) >= settings.repeatSimilarity) {
                return new Verdict(Reason.REPEAT, text, null);
            }
            lastMessage.put(player, text);
        }
        if (TextFilters.isShouting(text, settings.capsMinLetters, settings.capsMaxPercent)) {
            if (settings.capsLowercase) {
                return new Verdict(Reason.NONE, text.toLowerCase(Locale.ROOT), null);
            }
            return new Verdict(Reason.CAPS, text, null);
        }
        return new Verdict(Reason.NONE, text, null);
    }

    public void forget(UUID player) {
        limiter.reset(player);
        synchronized (lastMessage) {
            lastMessage.remove(player);
        }
    }

    private String foreignDomain(String text) {
        java.util.regex.Matcher m = LINK.matcher(text);
        while (m.find()) {
            String domain = m.group(1).toLowerCase(Locale.ROOT);
            boolean allowed = false;
            for (String ok : settings.allowedDomains) {
                String allowedDomain = ok.toLowerCase(Locale.ROOT).trim();
                if (!allowedDomain.isEmpty() && (domain.equals(allowedDomain) || domain.endsWith("." + allowedDomain))) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                return domain;
            }
        }
        return null;
    }
}
