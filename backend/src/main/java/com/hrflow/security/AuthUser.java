package com.hrflow.security;

/** Người dùng đã xác thực. employeeId = null với Admin kỹ thuật (không thuộc 40 nhân sự). */
public record AuthUser(Long userId, Long employeeId, String email, String role) {

    public boolean is(String... roles) {
        for (String r : roles) {
            if (r.equals(role)) {
                return true;
            }
        }
        return false;
    }
}
