package com.github.jvanheesch;

import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class DocumentUploadFeedbackListener {
    private final QueryUpdateEmitter queryUpdateEmitter;
    private final DocumentRepository documentRepository;

    DocumentUploadFeedbackListener(QueryUpdateEmitter queryUpdateEmitter, DocumentRepository documentRepository) {
        this.queryUpdateEmitter = queryUpdateEmitter;
        this.documentRepository = documentRepository;
    }

    @JmsListener(destination = "document.feedback.topic", subscription = "myappDocumentQueue")
    void handle(String correlation) {
        System.out.println("Handling feedback for upload of document with correlation " + correlation);
        Document document = documentRepository.findByCorrelation(UUID.fromString(correlation))
                .orElseThrow();
        document.setDocumentStatus(DocumentStatus.SUCCESS);
        documentRepository.save(document);
        queryUpdateEmitter.emit(DocumentStatusQuery.class, query -> query.getDocumentId() == document.getId(), DocumentStatus.SUCCESS);
    }
}
