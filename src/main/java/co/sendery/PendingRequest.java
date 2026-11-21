package co.sendery;

import java.util.concurrent.ThreadLocalRandom;

public class PendingRequest<T> {
    private final Sendery client;
    protected String body;
    private final String endpoint;
    private final Class<T> receiptType;
    private final String key;
    private int retries;
    PendingRequest(Sendery client, String body, String key, String endpoint, Class<T> receiptType) {
        if (!key.matches("[a-zA-Z0-9_.:-]{1,128}")) throw new IllegalArgumentException("Invalid idempotency key.");
        this.client = client; this.body = body; this.key = key; this.endpoint = endpoint; this.receiptType = receiptType;
    }
    public String idempotencyKey() { return key; }
    public PendingRequest<T> retry() { return retry(3); }
    public PendingRequest<T> retry(int retries) {
        if (retries < 0 || retries > 5) throw new IllegalArgumentException("Choose 0 to 5 retries.");
        this.retries = retries; return this;
    }
    public T send() {
        for (int attempt = 0; ; attempt++) {
            try { return client.request("POST", endpoint, body, key, receiptType); }
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
