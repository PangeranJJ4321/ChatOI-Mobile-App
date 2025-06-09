package com.example.mychat.models_user;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.Objects;

public class UserResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("username")
    private String username;
    @SerializedName("email")
    private String email;
    @SerializedName("profile_picture")
    private String profilePicture;
    @SerializedName("role")
    private String role; // Corresponds to UserRole enum in Python
    @SerializedName("is_online")
    private boolean isOnline;
    @SerializedName("last_active")
    private Date lastActive;
    @SerializedName("created_at")
    private Date createdAt;
    @SerializedName("updated_at")
    private Date updatedAt;

    // Transient field for UI selection state
    private transient boolean isSelected = false;


    // Constructor
    public UserResponse(String id, String username, String email, String profilePicture, String role, boolean isOnline, Date lastActive, Date createdAt, Date updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.profilePicture = profilePicture;
        this.role = role;
        this.isOnline = isOnline;
        this.lastActive = lastActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UserResponse(String id, String username, String profilePicture, boolean isOnline) {
        this.id = id;
        this.username = username;
        this.profilePicture = profilePicture;
        this.isOnline = isOnline;
    }

    public UserResponse() {

    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public String getRole() {
        return role;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public Date getLastActive() {
        return lastActive;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public void setLastActive(Date lastActive) {
        this.lastActive = lastActive;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Getter and Setter for transient field
    public void setSelected(boolean selected) { //
        isSelected = selected;
    }

    public boolean isSelected() { //
        return isSelected;
    }

    // Override equals and hashCode to correctly compare UserResponse objects based on their ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserResponse that = (UserResponse) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}