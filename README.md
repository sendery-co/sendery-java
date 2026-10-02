# Sendery for Java

Send published Sendery templates from Java.

[Documentation](https://sendery.co/en/docs/java) · [API reference](https://sendery.co/en/docs/send-email) · [Changelog](CHANGELOG.md)

## Requirements

Java 17+.

## Install

```xml
<dependency>
  <groupId>co.sendery</groupId>
  <artifactId>sendery-java</artifactId>
  <version>0.1.1</version>
</dependency>
```

## Set up

Choose a published template and create a [project API key](https://sendery.co/en/docs/authentication). Store the key as `SENDERY_API_KEY` on your server.

```bash
export SENDERY_API_KEY="your_project_api_key"
```

## Send an email

Replace `your-template` with your published template’s key and `data` with its variables.

The response contains the accepted email’s `id` and `status`.

```java
import co.sendery.Sendery;
import java.util.Map;

public class SendEmail {
    public static void main(String[] args) {
        var sendery = new Sendery(System.getenv("SENDERY_API_KEY"));
        var receipt = sendery.send("alex@example.com", "your-template", Map.of(
            "name", "Alex",
            "action_url", "https://example.com/start"
        ));
        System.out.println(receipt.id());
    }
}
```

## Send a specific version

Choose a [published template version](https://sendery.co/en/docs/send-email#section-5) to keep sending it after newer versions are published. By default, Sendery uses the latest version.

```java
import java.util.Map;

var receipt = sendery.prepare("alex@example.com", "your-template", Map.of(
    "name", "Alex",
    "action_url", "https://example.com/start"
)).version(3).send();
```

In Kotlin:

```kotlin
val receipt = sendery.prepare("alex@example.com", "your-template", mapOf(
    "name" to "Alex",
    "action_url" to "https://example.com/start"
)).version(3).send()
```

## Attachments

Pass a list of `Attachment` objects with the filename, file bytes, and MIME type. The SDK handles base64 encoding.

Send up to 10 files totaling 5 MB. See the [attachment reference](https://sendery.co/en/docs/send-email#section-6) for supported formats and limits.

```java
import co.sendery.Attachment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

var file = Files.readAllBytes(Path.of("document.pdf"));

sendery.prepare(
    "alex@example.com",
    "your-template",
    Map.of("name", "Alex", "action_url", "https://example.com/start"),
    null,
    "your-idempotency-key",
    List.of(new Attachment("document.pdf", file, "application/pdf"))
).retry().send();
```

In Kotlin:

```kotlin
import co.sendery.Attachment
import java.nio.file.Files
import java.nio.file.Path

val file = Files.readAllBytes(Path.of("document.pdf"))

sendery.prepare(
    "alex@example.com",
    "your-template",
    mapOf("name" to "Alex", "action_url" to "https://example.com/start"),
    null,
    "your-idempotency-key",
    listOf(Attachment("document.pdf", file, "application/pdf"))
).retry().send()
```

## Retrieve an email

Use the returned ID to [check delivery status](https://sendery.co/en/docs/get-email). `SendReceipt` also provides `errorCode()`, `createdAt()`, and `submittedAt()`. Calls block until the request completes.

```java
var message = sendery.get(receipt.id());
System.out.println(message.status());
```

## Retry a send

Use `retry(3)` for up to three extra attempts after temporary failures. Keep the same [idempotency key and email data](https://sendery.co/en/docs/idempotency) on every attempt.

```java
var email = sendery.prepare("alex@example.com", "your-template", Map.of(
    "name", "Alex", "action_url", "https://example.com/start"
), null, "your-idempotency-key");
var receipt = email.retry(3).send();
```

## Handle errors

Catch the SDK exception to inspect the [status and code](https://sendery.co/en/docs/errors). Retry delays are in seconds. The example uses the prepared `email` from the [retry example above](#retry-a-send).

```java
try {
    var receipt = email.retry(3).send();
    System.out.println(receipt.id());
} catch (co.sendery.SenderyException error) {
    System.err.println(error.status() + ": " + error.code());
    // error.errors() contains field-level validation errors.
    // error.retryAfter() is a delay in seconds, when provided.
    throw error;
}
```

## Kotlin

Use the same JVM package. See the [Kotlin guide](https://sendery.co/en/docs/kotlin) for Gradle setup and examples.

## More

Learn how to [retry emails without duplicate sends](https://sendery.co/en/docs/idempotency).

## License

[MIT](LICENSE).
