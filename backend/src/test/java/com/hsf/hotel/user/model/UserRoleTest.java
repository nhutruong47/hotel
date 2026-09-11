package com.hsf.hotel.user.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRoleTest {

    @Test
    void parsesRolesCaseInsensitively() {
        assertEquals(UserRole.STAFF, UserRole.from("staff"));
        assertEquals(UserRole.MANAGER, UserRole.from(" MANAGER "));
        assertEquals(UserRole.USER, UserRole.from(null));
    }

    @Test
    void hierarchyMatchesBackOfficePolicy() {
        assertTrue(UserRole.ADMIN.includes(UserRole.MANAGER));
        assertTrue(UserRole.MANAGER.includes(UserRole.STAFF));
        assertTrue(UserRole.STAFF.includes(UserRole.USER));
        assertFalse(UserRole.STAFF.includes(UserRole.MANAGER));
        assertFalse(UserRole.USER.includes(UserRole.STAFF));
    }
}
