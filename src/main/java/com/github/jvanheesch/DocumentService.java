package com.github.jvanheesch;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final JmsTemplate jmsTemplate;

    public DocumentService(DocumentRepository documentRepository, JmsTemplate jmsTemplate) {
        this.documentRepository = documentRepository;
        this.jmsTemplate = jmsTemplate;
    }

    // TODO_JORIS save content
    public Document saveDocument(String name, byte[] content) {
        UUID correlation = UUID.randomUUID();
        jmsTemplate.convertAndSend("document.request.topic", correlation);
        Document document = new Document();
        document.setName(name);
        document.setDocumentStatus(DocumentStatus.IN_PROGRESS);
        document.setCorrelation(correlation);
        return documentRepository.save(document);
    }

}
