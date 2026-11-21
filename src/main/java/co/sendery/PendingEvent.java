package co.sendery;

public final class PendingEvent extends PendingRequest<EventReceipt> {
    PendingEvent(Sendery client, String body, String key) { super(client, body, key, "/api/v1/events", EventReceipt.class); }
    public PendingEvent retry() { super.retry(); return this; }
    public PendingEvent retry(int retries) { super.retry(retries); return this; }
}
