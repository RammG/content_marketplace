package com.tianzige.marketplace.ingest.storage;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorageService {

    String store(InputStream inputStream, String filename) throws IOException;

    InputStream retrieve(String fileReference) throws IOException;

    void delete(String fileReference) throws IOException;

    boolean exists(String fileReference);
}
