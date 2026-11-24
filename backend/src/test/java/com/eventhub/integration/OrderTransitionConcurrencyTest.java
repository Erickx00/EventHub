package com.eventhub.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.eventhub.order.entity.Order;
import com.eventhub.order.entity.OrderStatus;
import com.eventhub.order.repository.OrderRepository;

/**
 * Verifies the order row lock and state re-read with PostgreSQL. This is gated
 * behind IT_POSTGRES so it never connects to an implicitly configured database.
 */
@SpringBootTest(properties = {
        "eventhub.orders.expiry-scan-ms=3600000",
        "eventhub.events.finish-scan-ms=3600000"
})
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "IT_POSTGRES", matches = "true")
class OrderTransitionConcurrencyTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void expirerRechecksStatusAfterWaitingForThePaymentLock() throws Exception {
        UUID userId = insertUser();
        UUID eventId = insertEvent(userId);
        UUID ticketTypeId = insertTicketType(eventId);
        Order order = Order.create(userId, eventId, Instant.now().plus(Duration.ofMinutes(5)));
        order.addItem(ticketTypeId, 1, new BigDecimal("12.00"));
        order = orders.saveAndFlush(order);
        UUID orderId = order.getId();
        jdbc.update("update orders set expires_at = now() - interval '1 minute' where id = ?", orderId);

        CountDownLatch paymentHasLock = new CountDownLatch(1);
        CountDownLatch allowPaymentCommit = new CountDownLatch(1);
        CountDownLatch expirerHasCandidate = new CountDownLatch(1);
        CountDownLatch allowExpirerToLock = new CountDownLatch(1);
        AtomicInteger expirerBackendId = new AtomicInteger();
        AtomicReference<OrderStatus> reloadedStatus = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> payment = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        Order locked = orders.findByIdForUpdate(orderId).orElseThrow();
                        locked.markPaid();
                        orders.flush();
                        paymentHasLock.countDown();
                        await(allowPaymentCommit);
                    }));

            assertThat(paymentHasLock.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> expiration = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        var candidates = orders.findIdsByStatusAndExpiresAtBefore(OrderStatus.PENDING, Instant.now());
                        assertThat(candidates).contains(orderId);
                        expirerBackendId.set(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                        expirerHasCandidate.countDown();
                        await(allowExpirerToLock);
                        Order reloaded = orders.findByIdForUpdate(orderId).orElseThrow();
                        reloadedStatus.set(reloaded.getStatus());
                    }));

            assertThat(expirerHasCandidate.await(5, TimeUnit.SECONDS)).isTrue();
            allowExpirerToLock.countDown();
            assertThat(awaitLockWait(expirerBackendId.get())).isTrue();
            allowPaymentCommit.countDown();

            payment.get(5, TimeUnit.SECONDS);
            expiration.get(5, TimeUnit.SECONDS);
        } finally {
            allowPaymentCommit.countDown();
            allowExpirerToLock.countDown();
            executor.shutdownNow();
        }

        assertThat(reloadedStatus.get()).isEqualTo(OrderStatus.PAID);
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
    }

    private UUID insertUser() {
        return jdbc.queryForObject("""
                insert into users (name, email, password_hash, role)
                values ('Concurrency test', ?, 'not-a-login-hash', 'ATTENDEE')
                returning id
                """, UUID.class, "concurrency-" + UUID.randomUUID() + "@example.test");
    }

    private UUID insertEvent(UUID organizerId) {
        Instant start = Instant.now().plus(Duration.ofDays(1));
        return jdbc.queryForObject("""
                insert into events (organizer_id, name, category, city, start_date, end_date, status)
                values (?, 'Concurrency test', 'TECHNOLOGY', 'Test city', ?, ?, 'PUBLISHED')
                returning id
                """, UUID.class, organizerId, Timestamp.from(start), Timestamp.from(start.plus(Duration.ofHours(2))));
    }

    private UUID insertTicketType(UUID eventId) {
        Instant start = Instant.now().minus(Duration.ofDays(1));
        return jdbc.queryForObject("""
                insert into ticket_types
                    (event_id, name, price, total_quantity, available_quantity, max_per_order, sales_start, sales_end)
                values (?, 'General', 12.00, 10, 9, 5, ?, ?)
                returning id
                """, UUID.class, eventId, Timestamp.from(start), Timestamp.from(start.plus(Duration.ofDays(30))));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out while coordinating concurrency test");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrency test interrupted", ex);
        }
    }

    private boolean awaitLockWait(int backendId) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            Integer waiting = jdbc.queryForObject("""
                    select count(*) from pg_stat_activity
                     where pid = ? and wait_event_type = 'Lock'
                    """, Integer.class, backendId);
            if (waiting != null && waiting > 0) {
                return true;
            }
            Thread.sleep(25);
        }
        return false;
    }
}
