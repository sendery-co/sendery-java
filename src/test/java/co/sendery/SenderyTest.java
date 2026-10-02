package co.sendery;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SenderyTest {
    @Test void retriesFreezePayloadAndKey() throws Exception {
        var bodies = new ArrayList<String>();
        var keys = new ArrayList<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/emails", exchange -> {
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            keys.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            var response = (bodies.size() == 1 ? "{\"code\":\"server_error\"}" : "{\"id\":\"one\",\"status\":\"queued\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Retry-After", "0");
            exchange.sendResponseHeaders(bodies.size() == 1 ? 503 : 202, response.length);
            exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        try {
            var client = new Sendery("test", "http://127.0.0.1:" + server.getAddress().getPort());
            var data = new HashMap<String, String>(); data.put("name", "Original");
            var bytes = new byte[] {0, 1, (byte)255};
            var email = client.prepare("a@example.com", "welcome", data, null, "event-12", List.of(new Attachment("invoice.pdf", bytes, "application/pdf")));
            email.version(3);
            bytes[0] = 99;
            data.put("name", "Changed");
            assertEquals("one", email.retry().send().id());
            assertTrue(bodies.get(0).contains("AAH/"));
            assertTrue(bodies.get(0).contains("application/pdf"));
            assertThrows(IllegalArgumentException.class, () -> client.prepare("a@example.com", "receipt", Map.of(), null, null, List.of(new Attachment("a.pdf", new byte[5242880], "application/pdf"), new Attachment("b.pdf", new byte[1], "application/pdf"))));
            assertEquals(3, com.google.gson.JsonParser.parseString(bodies.get(0)).getAsJsonObject().get("version").getAsInt());
            assertThrows(IllegalArgumentException.class, () -> email.version(0));
            assertEquals(2, bodies.size()); assertEquals(bodies.get(0), bodies.get(1));
            assertTrue(bodies.get(0).contains("Original")); assertEquals(List.of("event-12", "event-12"), keys);
        } finally { server.stop(0); }
    }
    @Test void capacityAndCredentialsAreNotRetried() {
        assertFalse(new SenderyException(429, "email_capacity_exceeded", null).retryable());
        assertTrue(new SenderyException(429, "rate_limited", null).retryable());
        assertThrows(IllegalArgumentException.class, () -> new Sendery("test", "http://example.com"));
    }
}
