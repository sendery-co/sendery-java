package co.sendery;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SenderyTest {
    @Test void rendersWithoutSendingAndPreservesSandboxKey() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var requests = new ArrayList<String>();
        var keys = new ArrayList<String>();
        server.createContext("/api/v1/emails/render", exchange -> {
            requests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            keys.add(exchange.getRequestHeaders().getFirst("Authorization"));
            var response = "{\"subject\":\"Hello\",\"html\":\"<p>Hello</p>\",\"text\":\"Hello\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        try {
            var client = new Sendery("snd_sandbox_test", "http://127.0.0.1:" + server.getAddress().getPort());
            assertEquals("Hello", client.render("welcome", Map.of("name", "Steven"), "ja", 2).subject());
            var body = com.google.gson.JsonParser.parseString(requests.get(0)).getAsJsonObject();
            assertEquals(2, body.get("version").getAsInt());
            assertFalse(body.has("to"));
            assertEquals(List.of("Bearer snd_sandbox_test"), keys);
            assertThrows(IllegalArgumentException.class, () -> client.render("welcome", Map.of(), null, 0));
        } finally { server.stop(0); }
    }

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
    @Test void eventsFreezeDataAndUseTypedReceipts() throws Exception {
        var bodies = new ArrayList<String>();
        var keys = new ArrayList<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/events", exchange -> {
            if (exchange.getRequestMethod().equals("GET")) {
                var response = """
                    {"id":"event","status":"accepted","name":"listing.created","received_at":"2026-11-21T00:00:00+00:00",
                     "environment":"sandbox","matched_workflows":1,"runs":[{"id":"run","workflow_id":4,"status":"completed",
                     "due_at":null,"error_code":null,"actions":[{"position":0,"type":"condition","status":"completed","email_id":null}]}]}
                    """.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response); exchange.close();
                return;
            }
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            keys.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            var response = (bodies.size() == 1 ? "{}" : "{\"id\":\"event\",\"status\":\"accepted\",\"matched_workflows\":2}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Retry-After", "0");
            exchange.sendResponseHeaders(bodies.size() == 1 ? 503 : 202, response.length);
            exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        try {
            var client = new Sendery("test", "http://127.0.0.1:" + server.getAddress().getPort());
            var owner = new HashMap<String, String>(); owner.put("email", "steven@example.com");
            var event = client.event("listing.created", "listing", "42", Map.of("owner", owner));
            owner.put("email", "changed@example.com");
            assertEquals(2, event.retry().send().matchedWorkflows());
            assertEquals(bodies.get(0), bodies.get(1));
            assertEquals(keys.get(0), keys.get(1));
            assertTrue(bodies.get(0).contains("steven@example.com"));
            var result = client.getEvent("event");
            assertEquals("event", result.id());
            assertEquals("listing.created", result.name());
            assertEquals("2026-11-21T00:00:00+00:00", result.receivedAt());
            assertEquals("condition", result.runs().get(0).actions().get(0).type());
        } finally { server.stop(0); }
    }
    @Test void capacityAndCredentialsAreNotRetried() {
        assertFalse(new SenderyException(429, "email_capacity_exceeded", null).retryable());
        assertTrue(new SenderyException(429, "rate_limited", null).retryable());
        assertThrows(IllegalArgumentException.class, () -> new Sendery("test", "http://example.com"));
    }
    @Test void eventDefinitionsUseTheirOwnResponseShape() throws Exception {
        var methods = new ArrayList<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/event-types", exchange -> {
            methods.add(exchange.getRequestMethod());
            var body = (exchange.getRequestMethod().equals("PUT") ? "{\"name\":\"document.signed\",\"verified\":false}" : "{\"events\":[]}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        try {
            var client = new Sendery("test", "http://127.0.0.1:" + server.getAddress().getPort());
            assertFalse(client.defineEvent("document.signed", Map.of("label", "Document signed")).verified());
            assertEquals(0, client.getEventTypes().size());
            assertEquals(java.util.List.of("PUT", "GET"), methods);
        } finally { server.stop(0); }
    }
}
