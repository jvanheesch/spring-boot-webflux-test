package com.github.jvanheesch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationTest {
    @LocalServerPort
    private int port;

    @Container
    private static final GenericContainer<?> AXON_CONTAINER = new GenericContainer<>("axoniq/axonserver:4.5.12")
            .withExposedPorts(8124)
            .waitingFor(new LogMessageWaitStrategy()
                    .withRegEx(".*Started AxonServer.*"));

    @DynamicPropertySource
    static void fixProperties(DynamicPropertyRegistry registry) {
        AXON_CONTAINER.start();
        registry.add("axon.axonserver.servers", () -> String.format("localhost:%d", AXON_CONTAINER.getMappedPort(8124)));
    }

    @Test
    void testServerSentEvents() {
        WebClient w = WebClient.create();

        Mono.delay(Duration.ofSeconds(2))
                .then(WebClient.create().get()
                        .uri(String.format("http://localhost:%d/emit", port))
                        .retrieve()
                        .toBodilessEntity())
                .subscribe();

        // dit doet de call somehow opnieuw, t lijkt dus dat de flux tot dan lazy is?
        List<DocumentStatus> block = w.get()
                .uri(String.format("http://localhost:%d/flux", port))
                .retrieve()
                .bodyToFlux(DocumentStatus.class)
                .collectList()
                .timeout(Duration.ofSeconds(5)) // make sure the test terminates
                .block();

        assertThat(block)
                .isEqualTo(Arrays.asList(DocumentStatus.IN_PROGRESS, DocumentStatus.SUCCESS));
    }
}
