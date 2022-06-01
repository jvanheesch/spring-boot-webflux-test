package com.github.jvanheesch;

import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequestMapping(path = "/documents")
@RestController
public class DocumentController {
    private final QueryGateway queryGateway;
    private final DocumentService documentService;

    public DocumentController(QueryGateway queryGateway, DocumentService documentService) {
        this.queryGateway = queryGateway;
        this.documentService = documentService;
    }

    // multipart/form-data;boundary=-Y_Ks1PYCTWsllonOnI2V6IBsoHlZkSY8N
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = "*/*")
    public Mono<ResponseEntity<Long>> upload(@RequestPart("file") Mono<FilePart> file) {
        return file
                .flatMap(f -> DataBufferUtils.join(f.content())
                        .map(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer);
                            return bytes;
                        })
                        .map(bytes -> documentService.saveDocument(
                                f.filename(),
                                bytes
                        )))
                .map(document -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(document.getId()));
    }

    @GetMapping(path = "/{documentId}/status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<DocumentStatus> getDocumentStatus(@PathVariable Long documentId) {
        SubscriptionQueryResult<DocumentStatus, DocumentStatus> statusQueryResult = queryGateway.subscriptionQuery(
                new DocumentStatusQuery(documentId),
                DocumentStatus.class,
                DocumentStatus.class
        );

        return Flux.concat(statusQueryResult.initialResult().flux(), statusQueryResult.updates())
                .takeUntil(status -> status != DocumentStatus.IN_PROGRESS);
    }
}
