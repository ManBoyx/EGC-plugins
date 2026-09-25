package fr.minebed.hub.ultimate.logic;

import fr.minebed.hub.common.time.DurationParser;
import java.util.Arrays;
import java.util.OptionalLong;

/** Lit les arguments « [durée] [motif…] » des commandes de modération, où la durée est facultative. */
public final class SanctionArgs {

    /** Durée « pour toujours » (aucune expiration). */
    public static final long PERMANENT = Long.MAX_VALUE;

    private final long durationMillis;
    private final String reason;

    private SanctionArgs(long durationMillis, String reason) {
        this.durationMillis = durationMillis;
        this.reason = reason;
    }

    /**
     * @param args  tous les arguments de la commande
     * @param from  index du premier argument à lire (après le pseudo)
     * @param noReason texte utilisé quand aucun motif n'est donné
     *
     * Si le premier argument lu est une durée valable (« 10m », « 2h », « 1d »…), il donne la durée et le reste est le motif ;
     * sinon la sanction est définitive et tout est le motif.
     */
    public static SanctionArgs parse(String[] args, int from, String noReason) {
        if (from >= args.length) {
            return new SanctionArgs(PERMANENT, noReason);
        }
        OptionalLong duration = DurationParser.parseMillis(args[from]);
        int reasonFrom = from;
        long millis = PERMANENT;
        if (duration.isPresent()) {
            millis = duration.getAsLong();
            reasonFrom = from + 1;
        }
        String reason = reasonFrom >= args.length ? noReason : String.join(" ", Arrays.copyOfRange(args, reasonFrom, args.length)).trim();
        return new SanctionArgs(millis, reason.isEmpty() ? noReason : reason);
    }

    public boolean permanent() {
        return durationMillis == PERMANENT;
    }

    public long durationMillis() {
        return durationMillis;
    }

    public String reason() {
        return reason;
    }

    /** Fin de la sanction à partir de {@code now} : {@link #PERMANENT} ou l'instant d'expiration (sans dépassement). */
    public long expiresAt(long now) {
        return permanent() || durationMillis > PERMANENT - now ? PERMANENT : now + durationMillis;
    }
}
