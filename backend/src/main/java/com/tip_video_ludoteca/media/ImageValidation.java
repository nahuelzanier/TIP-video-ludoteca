package com.tip_video_ludoteca.media;

/**
 * Content sniffing for the untrusted image uploads the app accepts. A declared
 * Content-Type is never trusted; the bytes themselves decide.
 */
public final class ImageValidation {

    private ImageValidation() {
    }

    public static final class RejectedImageException extends RuntimeException {

        public RejectedImageException(String message) {
            super(message);
        }
    }

    /**
     * @return the canonical content type for the image
     * @throws RejectedImageException if the bytes are neither PNG nor JPEG
     */
    public static String detectContentType(byte[] content) {
        if (isPng(content)) {
            return "image/png";
        }

        if (isJpeg(content)) {
            return "image/jpeg";
        }

        throw new RejectedImageException("The image must be a PNG or JPEG file.");
    }

    private static boolean isPng(byte[] content) {
        return content != null
                && content.length >= 8
                && content[0] == (byte) 0x89
                && content[1] == 0x50
                && content[2] == 0x4E
                && content[3] == 0x47
                && content[4] == 0x0D
                && content[5] == 0x0A
                && content[6] == 0x1A
                && content[7] == 0x0A;
    }

    private static boolean isJpeg(byte[] content) {
        return content != null
                && content.length >= 3
                && content[0] == (byte) 0xFF
                && content[1] == (byte) 0xD8
                && content[2] == (byte) 0xFF;
    }
}