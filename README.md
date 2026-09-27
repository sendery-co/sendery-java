# Sendery — Java integration

Send template emails from Java with the Sendery SDK.

MIT licensed. Repository: https://github.com/sendery-co/sendery-java

Documentation: https://sendery.co/en/docs/java

## Install

```
<dependency>
  <groupId>co.sendery</groupId>
  <artifactId>sendery-java</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Install

Java 17+

## Configure the client

Create a Sendery client using SENDERY_API_KEY. The client uses Java HttpClient and Gson, with redirects disabled and bounded timeouts.

## Use the response

SendReceipt provides id() and status(). Call get(id) for status. SenderyException exposes status(), code(), and retryAfter(). Requests are synchronous; run them on an appropriate worker thread.

## Configuration example

```
<dependency>
  <groupId>co.sendery</groupId>
  <artifactId>sendery-java</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Example

```
import co.sendery.Sendery;
import java.util.Map;

var sendery = new Sendery(System.getenv("SENDERY_API_KEY"));
var email = sendery.prepare("alex@example.com", "welcome", Map.of("name", "Alex"));
var receipt = email.retry(3).send();
```

## Retries and queues

Reuse a prepared email for retries. New requests receive new keys; when reconstructing a request in another process, supply the original key and unchanged data. Keep API keys server-side. Framework mailers send Sendery templates, not arbitrary HTML or attachments.
