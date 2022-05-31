package com.github.jvanheesch;

import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class DocumentProjection {
    @QueryHandler
    public DocumentStatus handle(DocumentStatusQuery query) {
        // check status of document with id == query.documentId
        return DocumentStatus.IN_PROGRESS;
    }
}
