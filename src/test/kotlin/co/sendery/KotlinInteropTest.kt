package co.sendery
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
class KotlinInteropTest {
    @Test fun rendersFromKotlinWithStagingCredentials() {
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        val requests = java.util.concurrent.CopyOnWriteArrayList<String>()
        server.createContext("/api/v1/emails/render") { exchange ->
            requests.add(exchange.requestHeaders.getFirst("Authorization"))
            requests.add(String(exchange.requestBody.readAllBytes(), Charsets.UTF_8))
            val body = """{"subject":"Hello","html":"<p>Hello</p>","text":"Hello"}""".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.write(body)
            exchange.close()
        }
        server.start()
        try {
            val result = Sendery("snd_sandbox_test", "http://127.0.0.1:${server.address.port}")
                .render("welcome", mapOf("name" to "Steven"), "ja", 2)
            assertEquals("Hello", result.subject())
            assertEquals("Bearer snd_sandbox_test", requests[0])
            assertEquals(2, com.google.gson.JsonParser.parseString(requests[1]).asJsonObject.get("version").asInt)
        } finally { server.stop(0) }
    }

    @Test fun acceptsKotlinMapsAndExplicitKeys() {
        val email = Sendery("test").prepare("a@example.com", "welcome", mapOf("name" to "Steven"), "ja", "event-12", listOf(Attachment("invoice.pdf", byteArrayOf(0, 1, -1), "application/pdf")))
        email.version(3)
        assertEquals("event-12", email.idempotencyKey())
    }
}
