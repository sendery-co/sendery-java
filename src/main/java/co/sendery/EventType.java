package co.sendery;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public record EventType(String name, String label, RecordType object, Map<String, Field> fields,
                        Map<String, Recipient> recipients, boolean verified,
                        @SerializedName("last_received_at") String lastReceivedAt) {
    public record RecordType(String type, String label) {}
    public record Field(String label, String path, String type, boolean required, Object example) {}
    public record Recipient(String label, String email, String name, String locale) {}
}
