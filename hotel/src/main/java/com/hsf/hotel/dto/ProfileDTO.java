package com.hsf.hotel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileDTO {
    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 120, message = "Email tối đa 120 ký tự")
    private String email;

    @Size(max = 30, message = "Số điện thoại tối đa 30 ký tự")
    private String phone;

    // stored as ISO string yyyy-MM-dd
    @Size(max = 10, message = "Ngày sinh không hợp lệ")
    private String dateOfBirth;

    @Size(max = 20, message = "Giới tính không hợp lệ")
    private String gender;

    @Size(max = 80, message = "Quốc tịch tối đa 80 ký tự")
    private String nationality;

    public ProfileDTO() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    @Size(max = 100, message = "Tên người liên hệ tối đa 100 ký tự")
    private String emergencyContactName;
    @Size(max = 30, message = "Số điện thoại liên hệ tối đa 30 ký tự")
    private String emergencyContactPhone;
    @Size(max = 50, message = "Quan hệ tối đa 50 ký tự")
    private String emergencyContactRelation;

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

    public String getEmergencyContactRelation() { return emergencyContactRelation; }
    public void setEmergencyContactRelation(String emergencyContactRelation) { this.emergencyContactRelation = emergencyContactRelation; }
}