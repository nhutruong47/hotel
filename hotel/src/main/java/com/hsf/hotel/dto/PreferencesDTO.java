package com.hsf.hotel.dto;

/**
 * User-controlled UI / notification preferences. Stored on the
 * {@code users.preferences_json} column. All fields are optional so the
 * client may PATCH only the keys it cares about.
 *
 * <p>Stored as a JSON blob to avoid adding columns for every toggle the SPA
 * adds. The shape is intentionally permissive (String) so the backend does
 * not break when the SPA ships a new preference key.
 */
public class PreferencesDTO {

    /** "light" | "dark" | "system". */
    private String theme;

    /** "vi" | "en". */
    private String language;

    /** "VND" | "USD". */
    private String currency;

    private Boolean emailBooking;
    private Boolean emailReminders;
    private Boolean emailMarketing;
    private Boolean smsBooking;

    public PreferencesDTO() {}

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Boolean getEmailBooking() { return emailBooking; }
    public void setEmailBooking(Boolean emailBooking) { this.emailBooking = emailBooking; }
    public Boolean getEmailReminders() { return emailReminders; }
    public void setEmailReminders(Boolean emailReminders) { this.emailReminders = emailReminders; }
    public Boolean getEmailMarketing() { return emailMarketing; }
    public void setEmailMarketing(Boolean emailMarketing) { this.emailMarketing = emailMarketing; }
    public Boolean getSmsBooking() { return smsBooking; }
    public void setSmsBooking(Boolean smsBooking) { this.smsBooking = smsBooking; }
}
