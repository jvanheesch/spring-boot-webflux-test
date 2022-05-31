package com.github.jvanheesch;

public class DocumentStatusQuery {
    private final long documentId;

    public DocumentStatusQuery(long documentId) {
        this.documentId = documentId;
    }

    public long getDocumentId() {
        return documentId;
    }
}
