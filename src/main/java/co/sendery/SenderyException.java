package co.sendery;

public final class SenderyException extends RuntimeException {
    private final com.google.gson.JsonObject errors;
    private final int status;
    private final String code;
    private final Double retryAfter;
    public SenderyException(int status, String code, Double retryAfter) {
        this(status, code, retryAfter, new com.google.gson.JsonObject());
    }
    public SenderyException(int status, String code, Double retryAfter, com.google.gson.JsonObject errors) {
        super("Sendery API error: " + code); this.errors = errors.deepCopy(); this.status = status; this.code = code; this.retryAfter = retryAfter;
    }
    public com.google.gson.JsonObject errors() { return errors.deepCopy(); }
    public int status() { return status; }
    public String code() { return code; }
    public Double retryAfter() { return retryAfter; }
    public boolean retryable() { return status == 0 || status == 500 || status == 502 || status == 503 || status == 504 || (status == 429 && code.equals("rate_limited")); }
}
