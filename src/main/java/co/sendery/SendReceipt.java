package co.sendery;

import com.google.gson.annotations.SerializedName;

public record SendReceipt(String id, String status, @SerializedName("error_code") String errorCode,
                          @SerializedName("created_at") String createdAt, @SerializedName("submitted_at") String submittedAt) {}
