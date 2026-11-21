package co.sendery;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public record EventReceipt(String id, String status, @SerializedName("matched_triggers") Integer matchedTriggers,
                           String environment, String name, @SerializedName("received_at") String receivedAt, List<Run> runs) {
    public record Run(String id, @SerializedName("trigger_id") long triggerId, String status,
                      @SerializedName("due_at") String dueAt, @SerializedName("error_code") String errorCode, List<Action> actions) {}
    public record Action(int position, String type, String status, @SerializedName("email_id") String emailId) {}
}
