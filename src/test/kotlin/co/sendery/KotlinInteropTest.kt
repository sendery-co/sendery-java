package co.sendery
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
class KotlinInteropTest {
    @Test fun acceptsKotlinMapsAndExplicitKeys() {
        val email = Sendery("test").prepare("a@example.com", "welcome", mapOf("name" to "Alex"), "ja", "event-12", listOf(Attachment("invoice.pdf", byteArrayOf(0, 1, -1), "application/pdf")))
        email.version(3)
        assertEquals("event-12", email.idempotencyKey())
    }
}
