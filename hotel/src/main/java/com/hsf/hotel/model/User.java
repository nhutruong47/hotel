package com.hsf.hotel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_username", columnList = "username", unique = true),
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_reset_token", columnList = "resetToken"),
        @Index(name = "idx_users_verification_token", columnList = "verificationToken"),
        @Index(name = "idx_users_role", columnList = "role"),
        @Index(name = "idx_users_disabled", columnList = "disabled")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    private String fullName;
    private String email;
    private String role = "USER";

    // Email verification fields
    private Boolean emailVerified = false;

    @JsonIgnore
    private String verificationToken;

    @JsonIgnore
    private LocalDateTime tokenExpiry;

    // Password reset fields (separate from email verification)
    @JsonIgnore
    private String resetToken;

    @JsonIgnore
    private LocalDateTime resetTokenExpiry;

    // Avatar filename stored in uploads/ (optional)
    private String avatarFilename;

    // Extended profile fields (all optional)
    private String phone;
    private String dateOfBirth;
    private String gender;
    private String nationality;

    // Emergency contact (required for luxury stays)
    @JsonIgnore
    private String emergencyContactName;

    @JsonIgnore
    private String emergencyContactPhone;

    @JsonIgnore
    private String emergencyContactRelation;

    // Identity document (for check-in verification) — sensitive PII; never serialize.
    @JsonIgnore
    private String identityType;    // PASSPORT, ID_CARD, DRIVER_LICENSE

    @JsonIgnore
    private String identityNumber;

    // Address
    @JsonIgnore
    private String address;

    private String city;
    private String country;

    @JsonIgnore
    private String postalCode;

    // Account management
    @JsonIgnore
    private Boolean deleteRequested = false;

    @JsonIgnore
    private LocalDateTime deleteRequestedAt;

    @JsonIgnore
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * When true, the account is administratively disabled: the user can no
     * longer log in or use authenticated endpoints. Existing bookings,
     * reviews, and audit rows are preserved. The flag defaults to false so
     * existing rows are unaffected by the column being added mid-life.
     */
    @Column(nullable = false)
    private Boolean disabled = false;

    private LocalDateTime disabledAt;

    private String disabledReason;

    /**
     * JSON blob for UI / notification preferences. The shape is owned by the
     * SPA and treated as opaque by the backend; see {@link com.hsf.hotel.dto.PreferencesDTO}.
     * Stored as TEXT (LONGTEXT-friendly) so adding new preference keys does
     * not require a schema migration.
     */
    @Column(columnDefinition = "TEXT")
    private String preferencesJson;

    // --- Getters and Setters ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
    public LocalDateTime getTokenExpiry() { return tokenExpiry; }
    public void setTokenExpiry(LocalDateTime tokenExpiry) { this.tokenExpiry = tokenExpiry; }
    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }
    public LocalDateTime getResetTokenExpiry() { return resetTokenExpiry; }
    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) { this.resetTokenExpiry = resetTokenExpiry; }
    public String getAvatarFilename() { return avatarFilename; }
    public void setAvatarFilename(String avatarFilename) { this.avatarFilename = avatarFilename; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Emergency contact
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
    public String getEmergencyContactRelation() { return emergencyContactRelation; }
    public void setEmergencyContactRelation(String emergencyContactRelation) { this.emergencyContactRelation = emergencyContactRelation; }

    // Identity
    public String getIdentityType() { return identityType; }
    public void setIdentityType(String identityType) { this.identityType = identityType; }
    public String getIdentityNumber() { return identityNumber; }
    public void setIdentityNumber(String identityNumber) { this.identityNumber = identityNumber; }

    // Address
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    // Account management
    public Boolean getDeleteRequested() { return deleteRequested; }
    public void setDeleteRequested(Boolean deleteRequested) { this.deleteRequested = deleteRequested; }
    public LocalDateTime getDeleteRequestedAt() { return deleteRequestedAt; }
    public void setDeleteRequestedAt(LocalDateTime deleteRequestedAt) { this.deleteRequestedAt = deleteRequestedAt; }
    public Boolean getDisabled() { return disabled; }
    public void setDisabled(Boolean disabled) { this.disabled = disabled; }
    public LocalDateTime getDisabledAt() { return disabledAt; }
    public void setDisabledAt(LocalDateTime disabledAt) { this.disabledAt = disabledAt; }
    public String getDisabledReason() { return disabledReason; }
    public void setDisabledReason(String disabledReason) { this.disabledReason = disabledReason; }
    public String getPreferencesJson() { return preferencesJson; }
    public void setPreferencesJson(String preferencesJson) { this.preferencesJson = preferencesJson; }
}
