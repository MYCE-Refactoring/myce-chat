package com.myce.api.auth.filter;

public final class InternalHeaderKey {
    public static final String INTERNAL_HEADER_PREFIX = "X-Internal-";
    public static final String INTERNAL_AUTH = INTERNAL_HEADER_PREFIX + "Auth";
    public static final String INTERNAL_MEMBER_ID = INTERNAL_HEADER_PREFIX + "Member-Id";
    public static final String INTERNAL_LOGIN_TYPE = INTERNAL_HEADER_PREFIX + "Login-Type";
    public static final String INTERNAL_ROLE = INTERNAL_HEADER_PREFIX + "Role";
}
