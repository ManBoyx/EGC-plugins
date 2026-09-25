package fr.minebed.hub.ultimate.logic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Comptes de l'économie interne. Les montants sont comptés en centimes entiers : jamais d'erreur d'arrondi de flottants,
 * et un plafond empêche tout dépassement. Toutes les opérations sont atomiques.
 */
public final class Ledger {

    /** Plafond d'un solde : 10^13 unités, soit 10^15 centimes (loin sous la limite d'un long). */
    public static final long MAX_CENTS = 1_000_000_000_000_000L;

    public enum Result { OK, INSUFFICIENT_FUNDS, TOO_RICH, INVALID_AMOUNT, SAME_ACCOUNT }

    private final Map<UUID, Long> balances = new HashMap<UUID, Long>();

    public static long toCents(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    public static double fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2).doubleValue();
    }

    public synchronized long balance(UUID account) {
        Long value = balances.get(account);
        return value == null ? 0 : value;
    }

    public synchronized boolean has(UUID account, long cents) {
        return balance(account) >= cents;
    }

    public synchronized Result deposit(UUID account, long cents) {
        if (cents <= 0) {
            return Result.INVALID_AMOUNT;
        }
        long now = balance(account);
        if (now > MAX_CENTS - cents) {
            return Result.TOO_RICH;
        }
        balances.put(account, now + cents);
        return Result.OK;
    }

    public synchronized Result withdraw(UUID account, long cents) {
        if (cents <= 0) {
            return Result.INVALID_AMOUNT;
        }
        long now = balance(account);
        if (now < cents) {
            return Result.INSUFFICIENT_FUNDS;
        }
        balances.put(account, now - cents);
        return Result.OK;
    }

    /** Tout ou rien : si le destinataire ne peut pas recevoir, l'expéditeur n'est pas débité. */
    public synchronized Result transfer(UUID from, UUID to, long cents) {
        if (from.equals(to)) {
            return Result.SAME_ACCOUNT;
        }
        if (cents <= 0) {
            return Result.INVALID_AMOUNT;
        }
        if (balance(from) < cents) {
            return Result.INSUFFICIENT_FUNDS;
        }
        if (balance(to) > MAX_CENTS - cents) {
            return Result.TOO_RICH;
        }
        balances.put(from, balance(from) - cents);
        balances.put(to, balance(to) + cents);
        return Result.OK;
    }

    /** Fixe un solde (administrateur) ; refuse un montant négatif ou au-dessus du plafond. */
    public synchronized Result set(UUID account, long cents) {
        if (cents < 0) {
            return Result.INVALID_AMOUNT;
        }
        if (cents > MAX_CENTS) {
            return Result.TOO_RICH;
        }
        if (cents == 0) {
            balances.remove(account);
        } else {
            balances.put(account, cents);
        }
        return Result.OK;
    }

    public synchronized Map<UUID, Long> snapshot() {
        return new HashMap<UUID, Long>(balances);
    }

    public synchronized void load(Map<UUID, Long> saved) {
        balances.clear();
        for (Map.Entry<UUID, Long> e : saved.entrySet()) {
            if (e.getValue() != null && e.getValue() > 0 && e.getValue() <= MAX_CENTS) {
                balances.put(e.getKey(), e.getValue());
            }
        }
    }
}
