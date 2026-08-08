package br.com.escreveaqui.backend.performance;

import br.com.escreveaqui.backend.services.UpsertNotaService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongArray;


@SpringBootTest
@Testcontainers
@Tag("performance")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotaWritePerformanceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UpsertNotaService upsertNotaService;

    private static final int WARMUP_OPS = 200;
    private static final int MEASURED_OPS = 2000;
    private static final int CONCURRENT_THREADS = 16;
    private static final int CONCURRENT_OPS_PER_THREAD = 200;

    @Test
    @Order(1)
    void sequentialInserts() {
        String prefix = "perf-insert-" + UUID.randomUUID() + "-";

        warmup(i -> upsertNotaService.execute(prefix + "warmup-" + i, "conteudo de aquecimento"));

        long[] latenciesNanos = new long[MEASURED_OPS];
        long wallStart = System.nanoTime();
        for (int i = 0; i < MEASURED_OPS; i++) {
            long opStart = System.nanoTime();
            upsertNotaService.execute(prefix + i, "conteudo de teste para insercao " + i);
            latenciesNanos[i] = System.nanoTime() - opStart;
        }
        long wallElapsed = System.nanoTime() - wallStart;

        printReport("INSERT sequencial (slugs unicos)", latenciesNanos, wallElapsed);
    }

    @Test
    @Order(2)
    void sequentialUpdates() {
        String slug = "perf-update-" + UUID.randomUUID();

        warmup(i -> upsertNotaService.execute(slug, "conteudo de aquecimento " + i));

        long[] latenciesNanos = new long[MEASURED_OPS];
        long wallStart = System.nanoTime();
        for (int i = 0; i < MEASURED_OPS; i++) {
            long opStart = System.nanoTime();
            upsertNotaService.execute(slug, "conteudo atualizado " + i);
            latenciesNanos[i] = System.nanoTime() - opStart;
        }
        long wallElapsed = System.nanoTime() - wallStart;

        printReport("UPDATE sequencial (mesmo slug)", latenciesNanos, wallElapsed);
    }

    @Test
    @Order(3)
    void concurrentWrites() throws InterruptedException {
        String prefix = "perf-concurrent-" + UUID.randomUUID() + "-";

        warmup(i -> upsertNotaService.execute(prefix + "warmup-" + i, "conteudo de aquecimento"));

        int totalOps = CONCURRENT_THREADS * CONCURRENT_OPS_PER_THREAD;
        AtomicLongArray latenciesNanos = new AtomicLongArray(totalOps);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);

        long wallStart = System.nanoTime();
        for (int t = 0; t < CONCURRENT_THREADS; t++) {
            int threadIndex = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < CONCURRENT_OPS_PER_THREAD; i++) {
                        String slug = prefix + threadIndex + "-" + i;
                        long opStart = System.nanoTime();
                        upsertNotaService.execute(slug, "conteudo concorrente " + threadIndex + "-" + i);
                        latenciesNanos.set(threadIndex * CONCURRENT_OPS_PER_THREAD + i, System.nanoTime() - opStart);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(2, TimeUnit.MINUTES);
        long wallElapsed = System.nanoTime() - wallStart;
        executor.shutdown();

        long[] flattened = new long[totalOps];
        for (int i = 0; i < totalOps; i++) flattened[i] = latenciesNanos.get(i);

        printReport("Escrita concorrente (" + CONCURRENT_THREADS + " threads, slugs distintos)", flattened, wallElapsed);
    }

    private void warmup(java.util.function.IntConsumer op) {
        for (int i = 0; i < WARMUP_OPS; i++) op.accept(i);
    }

    private void printReport(String label, long[] latenciesNanos, long wallElapsedNanos) {
        long[] sorted = latenciesNanos.clone();
        Arrays.sort(sorted);

        int count = sorted.length;
        double totalMs = wallElapsedNanos / 1_000_000.0;
        double throughputOpsPerSec = count / (wallElapsedNanos / 1_000_000_000.0);
        double avgMs = Arrays.stream(sorted).average().orElse(0) / 1_000_000.0;
        double minMs = sorted[0] / 1_000_000.0;
        double maxMs = sorted[count - 1] / 1_000_000.0;
        double p50Ms = percentile(sorted, 0.50) / 1_000_000.0;
        double p95Ms = percentile(sorted, 0.95) / 1_000_000.0;
        double p99Ms = percentile(sorted, 0.99) / 1_000_000.0;

        System.out.println();
        System.out.println("==================================================================");
        System.out.println("BENCHMARK: " + label);
        System.out.println("------------------------------------------------------------------");
        System.out.printf("  operacoes:      %d%n", count);
        System.out.printf("  tempo total:    %.1f ms%n", totalMs);
        System.out.printf("  throughput:     %.1f ops/s%n", throughputOpsPerSec);
        System.out.printf("  latencia media: %.2f ms%n", avgMs);
        System.out.printf("  p50:            %.2f ms%n", p50Ms);
        System.out.printf("  p95:            %.2f ms%n", p95Ms);
        System.out.printf("  p99:            %.2f ms%n", p99Ms);
        System.out.printf("  min / max:      %.2f ms / %.2f ms%n", minMs, maxMs);
        System.out.println("==================================================================");
        System.out.println();
    }

    private long percentile(long[] sortedNanos, double percentile) {
        int index = (int) Math.ceil(percentile * sortedNanos.length) - 1;
        return sortedNanos[Math.max(0, Math.min(index, sortedNanos.length - 1))];
    }
}
