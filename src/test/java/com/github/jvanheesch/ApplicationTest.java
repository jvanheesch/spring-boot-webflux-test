package com.github.jvanheesch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationTest {
    @LocalServerPort
    private int port;

    @Test
    void testServerSentEvents() {
        WebClient w = WebClient.create();

        Flux<Integer> integerFlux = w.get()
                .uri(String.format("http://localhost:%d/flux", port))
                .retrieve()
                .bodyToFlux(Integer.class);

        List<Integer> block = integerFlux.collectList().block();

        assertThat(block)
                .isEqualTo(Arrays.asList(1, 2, 3));
    }

}
