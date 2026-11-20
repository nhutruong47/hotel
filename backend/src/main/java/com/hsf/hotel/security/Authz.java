package com.hsf.hotel.security;

import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.model.UserRole;

/**
 * Role checks that honour the hierarchy ADMIN &gt; MANAGER &gt; STAFF &gt; USER
 * ({@code docs/BOOKING_LIFECYCLE_AND_RBAC.md}). Replaces scattered
 * {@code "ADMIN".equals(user.getRole())} comparisons, which silently denied
 * MANAGER/STAFF capabilities granted by the RBAC matrix (R-14).
 */
public final class Authz {

    private Authz() {}

    /** True when the user's role includes {@code required}. Unknown/missing role → false. */
    public static boolean atLeast(User user, UserRole required) {
        if (user == null || required == null) {
            return false;
        }
        try {
            UserRole role = user.getRoleEnum();
            return role != null && role.includes(required);
        } catch (IllegalArgumentException unknownRole) {
            return false;
        }
    }

    /** True when the user owns the resource or holds at least {@code required}. */
    public static boolean ownerOrAtLeast(User user, Integer ownerId, UserRole required) {
        if (user == null) {
            return false;
        }
        boolean owner = ownerId != null && ownerId.equals(user.getId());
        return owner || atLeast(user, required);
    }
}
