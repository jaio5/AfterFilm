package alicanteweb.pelisapp.service.image;

import java.io.IOException;
import java.io.InputStream;

interface ImageStorageBackend extends AutoCloseable {

    String saveImage(InputStream inputStream, String filename, String subfolder) throws IOException;

    boolean exists(String filename, String subfolder);

    String resolveStoredPath(String filename, String subfolder);

    int deleteDuplicates(String subfolder);

    @Override
    default void close() {
        // Default no-op.
    }
}