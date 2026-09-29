package co.sendery;

import java.util.Base64;

/** An immutable snapshot of a per-send file. */
public final class Attachment {
    private final String filename;
    private final String content;
    private final String content_type;
    public Attachment(String filename, byte[] bytes, String contentType) {
        if (bytes.length == 0 || bytes.length > 5242880)
            throw new IllegalArgumentException("Attachments must contain 1 to 5,242,880 bytes.");
        this.filename = filename;
        this.content = Base64.getEncoder().encodeToString(bytes);
        this.content_type = contentType;
    }
    public long size() {
        return (content.length() / 4L) * 3 - (content.endsWith("==") ? 2 : content.endsWith("=") ? 1 : 0);
    }
}
