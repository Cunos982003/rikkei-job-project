package com.example.profileservice.file;

import java.util.Optional;

/**
 * Identifies a file's real type from its leading bytes rather than its name,
 * so a renamed executable cannot pass as a document.
 */
public final class MagicBytesValidator {

    private MagicBytesValidator() {}

    public enum FileType {
        PDF("pdf"),
        JPG("jpg"),
        PNG("png");

        private final String extension;

        FileType(String extension) {
            this.extension = extension;
        }

        public String extension() {
            return extension;
        }
    }

    private static final byte[] PDF_SIGNATURE = {0x25, 0x50, 0x44, 0x46};
    private static final byte[] JPG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE =
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    public static Optional<FileType> detect(byte[] content) {
        if (content == null) {
            return Optional.empty();
        }
        if (startsWith(content, PDF_SIGNATURE)) {
            return Optional.of(FileType.PDF);
        }
        if (startsWith(content, JPG_SIGNATURE)) {
            return Optional.of(FileType.JPG);
        }
        if (startsWith(content, PNG_SIGNATURE)) {
            return Optional.of(FileType.PNG);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (content[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
