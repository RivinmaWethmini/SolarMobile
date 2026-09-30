package com.solarmicrogrid.mobile.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Domain model representing an authenticated user in the Solar Microgrid system.
 * Mirrors AuthUserDto from the C# Web API backend.
 */
public class AuthUser implements Serializable {
    private String id;
    private String email;
    private String username;
    private String role;
    private String fullName;
    private String nic;
    private String approvalStatus;
    private boolean isActive;
    private boolean isVerified;
    private String createdAt;

    public AuthUser() {
    }

    public static AuthUser fromJson(JSONObject json) {
        if (json == null) return null;
        AuthUser user = new AuthUser();
        user.id = json.optString("id", "");
        user.email = json.optString("email", "");
        user.username = json.optString("username", "");
        user.role = json.optString("role", "Consumer");
        user.fullName = json.optString("fullName", "");
        user.nic = json.optString("nic", "");
        user.approvalStatus = json.optString("approvalStatus", "Approved");
        user.isActive = json.optBoolean("isActive", true);
        user.isVerified = json.optBoolean("isVerified", false);
        user.createdAt = json.optString("createdAt", "");
        return user;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("email", email);
            json.put("username", username);
            json.put("role", role);
            json.put("fullName", fullName);
            json.put("nic", nic);
            json.put("approvalStatus", approvalStatus);
            json.put("isActive", isActive);
            json.put("isVerified", isVerified);
            json.put("createdAt", createdAt);
        } catch (JSONException ignored) {
        }
        return json;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAdmin() {
        return "Admin".equalsIgnoreCase(role) || "GridOperator".equalsIgnoreCase(role);
    }

    public boolean isProsumer() {
        return "Prosumer".equalsIgnoreCase(role);
    }

    public boolean isConsumer() {
        return "Consumer".equalsIgnoreCase(role);
    }

    public boolean isPendingApproval() {
        return "PendingApproval".equalsIgnoreCase(approvalStatus);
    }

    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        }
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }
        if (email != null && !email.trim().isEmpty()) {
            return email.split("@")[0];
        }
        return "Participant";
    }
}
