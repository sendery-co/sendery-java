package co.sendery;

public final class PendingEmail extends PendingRequest<SendReceipt> {
    PendingEmail(Sendery client, String body, String key) { super(client, body, key, "/api/v1/emails", SendReceipt.class); }
    public PendingEmail retry() { super.retry(); return this; }
    public PendingEmail retry(int retries) { super.retry(retries); return this; }
    public PendingEmail version(int version) {
        if (version < 1) throw new IllegalArgumentException("Version must be a positive integer.");
        var payload = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
        payload.addProperty("version", version);
        body = payload.toString();
        return this;
    }
}
