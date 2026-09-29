package co.sendery;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

public final class Sendery {
    private final String key;
    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
    private final Gson json = new Gson();
    public Sendery(String key) { this(key, "https://sendery.co"); }
    public Sendery(String key, String baseUrl) {
        URI uri = URI.create(baseUrl);
        if (key == null || key.isBlank() || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null || !("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme()) && java.util.List.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost()))))
            throw new IllegalArgumentException("Provide an API key and an HTTPS URL (HTTP allowed only on loopback).");
        this.key = key; this.baseUrl = baseUrl.replaceAll("/$", "");
    }
    public PendingEmail prepare(String to, String template, Map<String, ?> data) { return prepare(to, template, data, null, UUID.randomUUID().toString()); }
    public PendingEmail prepare(String to, String template, Map<String, ?> data, String locale, String idempotencyKey) {
        return prepare(to, template, data, locale, idempotencyKey, java.util.List.of());
    }
    public PendingEmail prepare(String to, String template, Map<String, ?> data, String locale, String idempotencyKey, java.util.List<Attachment> attachments) {
        if (attachments.size() > 10 || attachments.stream().mapToLong(Attachment::size).sum() > 5242880)
            throw new IllegalArgumentException("Use at most 10 attachments, up to 5 MB combined.");
        var payload = new JsonObject(); payload.addProperty("to", to); payload.addProperty("template", template); payload.add("data", json.toJsonTree(data));
        if (!attachments.isEmpty()) payload.add("attachments", json.toJsonTree(attachments));
        if (locale != null) payload.addProperty("locale", locale);
        return new PendingEmail(this, payload.toString(), idempotencyKey == null ? UUID.randomUUID().toString() : idempotencyKey);
    }
    public SendReceipt send(String to, String template, Map<String, ?> data) { return prepare(to, template, data).send(); }
    public SendReceipt get(String id) { return request("GET", "/api/v1/emails/" + URLEncoder.encode(id, StandardCharsets.UTF_8), null, null); }
    SendReceipt request(String method, String path, String body, String idempotencyKey) {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + key).header("Accept", "application/json").header("Content-Type", "application/json");
        if (idempotencyKey != null) builder.header("Idempotency-Key", idempotencyKey);
        var request = builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response;
        try { response = http.send(request, HttpResponse.BodyHandlers.ofString()); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new SenderyException(499, "interrupted", null); }
        catch (java.io.IOException exception) { throw new SenderyException(0, "connection_error", null); }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String code = "request_error";
            var errors = new JsonObject();
            try { var error = JsonParser.parseString(response.body()).getAsJsonObject(); if (error.has("errors") && error.get("errors").isJsonObject()) errors = error.getAsJsonObject("errors"); if (error.has("code")) code = error.get("code").getAsString(); } catch (RuntimeException ignored) {}
            Double delay = null;
            var retry = response.headers().firstValue("Retry-After");
            if (retry.isPresent()) {
                try { delay = Double.valueOf(retry.get()); } catch (NumberFormatException ignored) {
                    try { delay = (double) Duration.between(ZonedDateTime.now(), ZonedDateTime.parse(retry.get(), DateTimeFormatter.RFC_1123_DATE_TIME)).getSeconds(); } catch (RuntimeException ignoredDate) {}
                }
                if (delay != null && (!Double.isFinite(delay) || delay < 0)) delay = null;
            }
            throw new SenderyException(response.statusCode(), code, delay, errors);
        }
        try {
            var receipt = json.fromJson(response.body(), SendReceipt.class);
            if (receipt == null || receipt.id() == null || receipt.status() == null) throw new IllegalArgumentException();
            return receipt;
        } catch (RuntimeException exception) { throw new SenderyException(0, "invalid_response", null); }
    }
}
