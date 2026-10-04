package com.tip_video_ludoteca.users;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 500)
    private String description;

    @Column(name = "profile_image", columnDefinition = "bytea")
    private byte[] profileImage;

    @Column(name = "profile_image_content_type", length = 50)
    private String profileImageContentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getDescription() {
        return description;
    }

    public byte[] getProfileImage() {
        return profileImage;
    }

    public String getProfileImageContentType() {
        return profileImageContentType;
    }

    public boolean hasProfileImage() {
        return profileImage != null && profileImage.length > 0;
    }

    /**
     * Derived accessor, not a mapped column: the entity uses field access, so Hibernate
     * ignores it. Keeping the URL derived avoids a stored value drifting from the bytes.
     */
    public String getProfileImageUrl() {
        return hasProfileImage() ? "/api/users/" + username + "/avatar" : null;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateProfileImage(byte[] content, String contentType) {
        this.profileImage = content;
        this.profileImageContentType = contentType;
    }

    public void clearProfileImage() {
        this.profileImage = null;
        this.profileImageContentType = null;
    }
}