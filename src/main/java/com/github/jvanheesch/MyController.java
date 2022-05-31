package com.github.jvanheesch;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
public class MyController {
    @GetMapping(path = "/flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Integer> flux() {
        return Flux.just(1, 2, 3)
                .delayElements(Duration.ofSeconds(1));
    }
}
