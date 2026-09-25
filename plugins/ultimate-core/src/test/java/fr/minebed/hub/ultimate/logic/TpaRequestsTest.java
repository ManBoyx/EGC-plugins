package fr.minebed.hub.ultimate.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class TpaRequestsTest {

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final UUID carl = UUID.randomUUID();
    private final AtomicLong now = new AtomicLong(0);
    private final TpaRequests requests = new TpaRequests(60_000, now::get);

    @Test
    void takeReturnsLatestAndRemoves() {
        requests.create(alice, bob, TpaRequests.Direction.REQUESTER_TO_TARGET);
        requests.create(carl, bob, TpaRequests.Direction.TARGET_TO_REQUESTER);
        TpaRequests.Request r = requests.take(bob, null);
        assertEquals(carl, r.from());
        assertEquals(TpaRequests.Direction.TARGET_TO_REQUESTER, r.direction());
        assertEquals(alice, requests.take(bob, null).from());
        assertNull(requests.take(bob, null));
    }

    @Test
    void takeCanTargetASender() {
        requests.create(alice, bob, TpaRequests.Direction.REQUESTER_TO_TARGET);
        requests.create(carl, bob, TpaRequests.Direction.REQUESTER_TO_TARGET);
        assertEquals(alice, requests.take(bob, alice).from());
        assertNull(requests.take(bob, alice));
        assertNotNull(requests.take(bob, carl));
    }

    @Test
    void requestsExpire() {
        requests.create(alice, bob, TpaRequests.Direction.REQUESTER_TO_TARGET);
        now.set(59_999);
        assertEquals(1, requests.pendingCount());
        now.set(60_000);
        assertEquals(0, requests.pendingCount());
        assertNull(requests.take(bob, null));
    }

    @Test
    void newRequestReplacesOldOneToSameTarget() {
        requests.create(alice, bob, TpaRequests.Direction.REQUESTER_TO_TARGET);
        requests.create(alice, bob, TpaRequests.Direction.TARGET_TO_REQUESTER);
        assertEquals(1, requests.pendingCount());
        assertEquals(TpaRequests.Direction.TARGET_TO_REQUESTER, requests.take(bob, alice).direction());
    }

    @Test
    void cancelAndForget() {
        requests.create(alice, bob, TpaRequests.Direction.REQUESTER_TO_TARGET);
        requests.create(alice, carl, TpaRequests.Direction.REQUESTER_TO_TARGET);
        assertEquals(2, requests.cancelFrom(alice));
        assertEquals(0, requests.cancelFrom(alice));
        requests.create(bob, carl, TpaRequests.Direction.REQUESTER_TO_TARGET);
        requests.forget(bob);
        assertEquals(0, requests.pendingCount());
    }
}
