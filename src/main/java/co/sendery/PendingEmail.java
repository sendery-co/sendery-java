package co.sendery;

import java.util.concurrent.ThreadLocalRandom;

public final class PendingEmail {
    private final Sendery client;
    private String body;
    private final String key;
    private int retries;
    PendingEmail(Sendery client, String body, String key) {
        if (!key.matches("[a-zA-Z0-9_.:-]{1,128}")) throw new IllegalArgumentException("Invalid idempotency key.");
        this.client = client; this.body = body; this.key = key;
    }
    public String idempotencyKey() { return key; }
    public PendingEmail version(int version) {
        if (version < 1) throw new IllegalArgumentException("Version must be a positive integer.");
        var payload = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
        payload.addProperty("version", version);
        body = payload.toString();
        return this;
    }
    public PendingEmail retry() { return retry(3); }
    public PendingEmail retry(int retries) {
        if (retries < 0 || retries > 5) throw new IllegalArgumentException("Choose 0 to 5 retries.");
        this.retries = retries; return this;
    }
    public SendReceipt send() {
        for (int attempt = 0; ; attempt++) {
            try { return client.request("POST", "/api/v1/emails", body, key); }
            catch (SenderyException exception) {
                if (attempt >= retries || !exception.retryable()) throw exception;
                double delay = exception.retryAfter() == null ? Math.min(8, .25 * Math.pow(2, attempt) + ThreadLocalRandom.current().nextDouble(.1)) : exception.retryAfter();
                if (delay > 30) throw exception;
                try { Thread.sleep((long) (delay * 1000)); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new SenderyException(499, "interrupted", null); }
            }
        }
    }
}
