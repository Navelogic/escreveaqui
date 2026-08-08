package br.com.escreveaqui.backend.repositories;

import br.com.escreveaqui.backend.models.Nota;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class NotaRepositoryConcurrencyIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private NotaRepository notaRepository;

    @Test
    void upsertConcorrenteNoMesmoSlugNaoLancaEExisteUmaUnicaLinha() throws InterruptedException {
        String slug = "concurrency-" + UUID.randomUUID();
        int threads = 20;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger failures = new AtomicInteger();
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();

        for (int i = 0; i < threads; i++) {
            int threadIndex = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    notaRepository.upsert(slug, "conteudo da thread " + threadIndex);
                } catch (Throwable t) {
                    failures.incrementAndGet();
                    firstFailure.compareAndSet(null, t);
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        if (firstFailure.get() != null) {
            throw new AssertionError("upsert concorrente lancou exceção", firstFailure.get());
        }
        assertThat(failures.get()).isZero();

        Nota nota = notaRepository.findBySlug(slug).orElseThrow();
        assertThat(nota.slug()).isEqualTo(slug);
    }
}
