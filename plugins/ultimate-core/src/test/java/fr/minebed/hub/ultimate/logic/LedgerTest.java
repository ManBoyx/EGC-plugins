package fr.minebed.hub.ultimate.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LedgerTest {

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    @Test
    void centsConversionIsExact() {
        assertEquals(1999, Ledger.toCents(19.99));
        assertEquals(30, Ledger.toCents(0.1 + 0.2));
        assertEquals(19.99, Ledger.fromCents(1999), 1e-12);
    }

    @Test
    void depositAndWithdraw() {
        Ledger l = new Ledger();
        assertEquals(Ledger.Result.OK, l.deposit(a, 1000));
        assertEquals(Ledger.Result.OK, l.withdraw(a, 400));
        assertEquals(600, l.balance(a));
        assertEquals(Ledger.Result.INSUFFICIENT_FUNDS, l.withdraw(a, 601));
        assertEquals(600, l.balance(a));
        assertEquals(Ledger.Result.INVALID_AMOUNT, l.deposit(a, 0));
        assertEquals(Ledger.Result.INVALID_AMOUNT, l.withdraw(a, -5));
    }

    @Test
    void transferIsAllOrNothing() {
        Ledger l = new Ledger();
        l.deposit(a, 1000);
        l.set(b, Ledger.MAX_CENTS - 10);
        assertEquals(Ledger.Result.TOO_RICH, l.transfer(a, b, 500));
        assertEquals(1000, l.balance(a));
        assertEquals(Ledger.Result.INSUFFICIENT_FUNDS, l.transfer(a, b, 5000));
        assertEquals(Ledger.Result.SAME_ACCOUNT, l.transfer(a, a, 1));
        assertEquals(Ledger.Result.OK, l.transfer(a, b, 10));
        assertEquals(990, l.balance(a));
        assertEquals(Ledger.MAX_CENTS, l.balance(b));
    }

    @Test
    void depositCannotOverflow() {
        Ledger l = new Ledger();
        l.set(a, Ledger.MAX_CENTS);
        assertEquals(Ledger.Result.TOO_RICH, l.deposit(a, 1));
        assertEquals(Ledger.Result.TOO_RICH, l.deposit(a, Long.MAX_VALUE));
        assertEquals(Ledger.MAX_CENTS, l.balance(a));
    }

    @Test
    void setAndReset() {
        Ledger l = new Ledger();
        assertEquals(Ledger.Result.OK, l.set(a, 500));
        assertTrue(l.has(a, 500));
        assertFalse(l.has(a, 501));
        assertEquals(Ledger.Result.INVALID_AMOUNT, l.set(a, -1));
        assertEquals(Ledger.Result.OK, l.set(a, 0));
        assertEquals(0, l.snapshot().size());
    }

    @Test
    void loadIgnoresGarbageEntries() {
        Ledger l = new Ledger();
        Map<UUID, Long> saved = new HashMap<UUID, Long>();
        saved.put(a, 250L);
        saved.put(b, -3L);
        saved.put(UUID.randomUUID(), Long.MAX_VALUE);
        l.load(saved);
        assertEquals(250, l.balance(a));
        assertEquals(0, l.balance(b));
        assertEquals(1, l.snapshot().size());
    }
}
