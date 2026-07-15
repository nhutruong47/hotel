package com.hsf.hotel.dto;

import com.hsf.hotel.model.User;

import java.time.LocalDateTime;

/**
 * Public profile projection — exposes non-sensitive profile information
 * (no identity documents, no internal-only fields). Used by the profile
 * endpoints.
 */
public final class UserProfileDTO {

    private final Integer id;
    private final String username;
    private final String email;
    private final String fullName;
    private final String role;
    private final String avatarFilename;
    private final String avatarUrl;
    private final Boolean emailVerified;
    private final String phone;
    private final String dateOfBirth;
    private final String gender;
    private final String nationality;
    private final String emergencyContactName;
    private final String emergencyContactPhone;
    private final String emergencyContactRelation;
    private final String city;
    private final String country;
    private final LocalDateTime createdAt;

    private UserProfileDTO(User u) {
        this.id = u.getId();
        this.username = u.getUsername();
        this.email = u.getEmail();
        this.fullName = u.getFullName();
        this.role = u.getRole();
        this.avatarFilename = u.getAvatarFilename();
        this.avatarUrl = u.getAvatarFilename() != null ? "/uploads/" + u.getAvatarFilename() : null;
        this.emailVerified = u.getEmailVerified();
        this.phone = u.getPhone();
        this.dateOfBirth = u.getDateOfBirth();
        this.gender = u.getGender();
        this.nationality = u.getNationality();
        this.emergencyContactName = u.getEmergencyContactName();
        this.emergencyContactPhone = u.getEmergencyContactPhone();
        this.emergencyContactRelation = u.getEmergencyContactRelation();
        this.city = u.getCity();
        this.country = u.getCountry();
        this.createdAt = u.getCreatedAt();
    }

    public static UserProfileDTO from(User u) {
        return new UserProfileDTO(u);
    }

    public Integer getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public String getAvatarFilename() { return avatarFilename; }
    public String getAvatarUrl() { return avatarUrl; }
    public Boolean getEmailVerified() { return emailVerified; }
    public String getPhone() { return phone; }
    public String getDateOfBirth() { return dateOfBirth; }
    public String getGender() { return gender; }
    public String getNationality() { return nationality; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public String getEmergencyContactRelation() { return emergencyContactRelation; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}