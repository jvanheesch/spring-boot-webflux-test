package com.github.jvanheesch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {Application.class, ApplicationTest.DocumentTestConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ApplicationTest {
    @LocalServerPort
    private int port;

    @Container
    private static final GenericContainer<?> AXON_CONTAINER = new GenericContainer<>("axoniq/axonserver:4.5.12")
            .withExposedPorts(8124)
            .waitingFor(new LogMessageWaitStrategy()
                    .withRegEx(".*Started AxonServer.*"));
    @Container
    private static final GenericContainer<?> AMQ_CONTAINER = new GenericContainer<>("vromero/activemq-artemis:2.15.0-alpine")
            .withExposedPorts(61616, 8161)
            .waitingFor(new LogMessageWaitStrategy()
                    .withRegEx(".*AMQ221007.*"));  // INFO  [org.apache.activemq.artemis.core.server] AMQ221007: Server is now live

    @DynamicPropertySource
    static void fixProperties(DynamicPropertyRegistry registry) {
        AXON_CONTAINER.start();
        AMQ_CONTAINER.start();
        registry.add("axon.axonserver.servers", () -> String.format("localhost:%d", AXON_CONTAINER.getMappedPort(8124)));
        registry.add("spring.artemis.broker-url", () -> String.format("tcp://localhost:%d", AMQ_CONTAINER.getMappedPort(61616)));
    }

    @Test
    void testServerSentEvents() {
        WebClient w = WebClient.create();

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ClassPathResource("sample.pdf"));

        // https://hantsy.github.io/spring-reactive-sample/web/multipart.html
        List<DocumentStatus> block = w.post()
                .uri(String.format("http://localhost:%d/documents", port))
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(Long.class)
                .flatMap(documentId -> w.get().uri(String.format("http://localhost:%d/documents/1/status", port))
                        .retrieve()
                        .bodyToFlux(DocumentStatus.class)
                        .collectList())
                .timeout(Duration.ofSeconds(10))
                .block();

        assertThat(block)
                .isEqualTo(Arrays.asList(DocumentStatus.IN_PROGRESS, DocumentStatus.SUCCESS));
    }

    @Configuration
    public static class DocumentTestConfiguration {
        @Component
        static class DocumentUploadListener {
            private final JmsTemplate jmsTemplate;

            DocumentUploadListener(JmsTemplate jmsTemplate) {
                this.jmsTemplate = jmsTemplate;
            }

            // in reality we send a command to documentMailboxQueue instead of publishing an event to document.request.topic
            @JmsListener(destination = "document.request.topic", subscription = "documentMailboxQueue")
            void handle(String correlation) {
                System.out.println("Handling upload request for document with correlation " + correlation);
                Mono.delay(Duration.ofSeconds(2))
                        .doAfterTerminate(() -> {
                            System.out.println("DocumentUploadListener.handle");
                            jmsTemplate.convertAndSend("document.feedback.topic", correlation);
                        })
                        .subscribe();
            }
        }
    }
}
