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
  <version>0.1.0</version>
</dependency>
```

## Set up

Publish a `welcome` template with `name` and `action_url` variables, and create a [project API key](https://sendery.co/en/docs/authentication). Store it as `SENDERY_API_KEY` on your server.

```bash
export SENDERY_API_KEY="your_project_api_key"
```

## Send an email

The response contains the accepted email’s `id` and `status`.

```java
import co.sendery.Sendery;
import java.util.Map;

public class SendWelcome {
    public static void main(String[] args) {
        var sendery = new Sendery(System.getenv("SENDERY_API_KEY"));
        var receipt = sendery.send("alex@example.com", "welcome", Map.of(
            "name", "Alex",
            "action_url", "https://example.com/start"
        ));
        System.out.println(receipt.id());
    }
}
```

## Retrieve an email

Use the returned ID to [check delivery status](https://sendery.co/en/docs/get-email). `SendReceipt` also provides `errorCode()`, `createdAt()`, and `submittedAt()`. Calls block until the request completes.

```java
var message = sendery.get(receipt.id());
System.out.println(message.status());
```

## Retry a send

Use a key such as `welcome-123` for one email, and [keep the payload unchanged on retries](https://sendery.co/en/docs/idempotency). `retry(3)` allows up to three additional attempts for temporary failures; `send()` alone makes one attempt.

```java
var email = sendery.prepare("alex@example.com", "welcome", Map.of(
    "name", "Alex", "action_url", "https://example.com/start"
), null, "welcome-123");
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

See [idempotency and retries](https://sendery.co/en/docs/idempotency) for retry conditions, delays, and reusing a key across attempts.

## License

[MIT](LICENSE).
