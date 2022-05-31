package com.github.jvanheesch;

import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class DocumentStatusController {
    private final QueryGateway queryGateway;
    private final QueryUpdateEmitter queryUpdateEmitter;

    public DocumentStatusController(QueryGateway queryGateway, QueryUpdateEmitter queryUpdateEmitter) {
        this.queryGateway = queryGateway;
        this.queryUpdateEmitter = queryUpdateEmitter;
    }

    @GetMapping(path = "/flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<DocumentStatus> flux() {
        SubscriptionQueryResult<DocumentStatus, DocumentStatus> statusQueryResult = queryGateway.subscriptionQuery(
                new DocumentStatusQuery(1L),
                DocumentStatus.class,
                DocumentStatus.class
        );

        return Flux.concat(statusQueryResult.initialResult().flux(), statusQueryResult.updates())
                .takeUntil(status -> status != DocumentStatus.IN_PROGRESS);
    }

    @GetMapping(path = "/emit")
    public void emit() {
        queryUpdateEmitter.emit(DocumentStatusQuery.class, query -> true, DocumentStatus.SUCCESS);
    }
}
