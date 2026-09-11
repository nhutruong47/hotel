package com.hsf.hotel.user.model;

public enum UserRole {
    USER(0),
    STAFF(1),
    MANAGER(2),
    ADMIN(3);

    private final int permissionLevel;

    UserRole(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public static UserRole from(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }
        return UserRole.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
    }

    /**
     * Returns whether this role includes the permissions of {@code required}.
     * The ordering is deliberate: ADMIN > MANAGER > STAFF > USER.
     */
    public boolean includes(UserRole required) {
        return required != null && permissionLevel >= required.permissionLevel;
    }
}
