package cn.jongwong.security;

public final class AuthErrorCode {

    public static final int UNAUTHORIZED = 40100;
    public static final int TOKEN_EXPIRED = 40101;
    public static final int INVALID_TOKEN = 40102;

    public static final String FAILURE_ATTRIBUTE = "cn.jongwong.security.AUTH_FAILURE";

    private AuthErrorCode() {}
}
