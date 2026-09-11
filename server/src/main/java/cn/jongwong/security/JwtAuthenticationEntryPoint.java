package cn.jongwong.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException ex)
            throws IOException {

        AuthenticationException failure = resolveFailure(request, ex);
        int code;
        String message;

        if (failure instanceof CredentialsExpiredException) {
            code = AuthErrorCode.TOKEN_EXPIRED;
            message = "TOKEN_EXPIRED";
        } else if (failure instanceof BadCredentialsException) {
            code = AuthErrorCode.INVALID_TOKEN;
            message = "INVALID_TOKEN";
        } else {
            code = AuthErrorCode.UNAUTHORIZED;
            message = "UNAUTHORIZED";
        }

        log.warn(
                "Unauthorized {} {} ({})",
                request.getMethod(),
                request.getRequestURI(),
                code
        );
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private static AuthenticationException resolveFailure(
            HttpServletRequest request,
            AuthenticationException ex
    ) {
        Object stored = request.getAttribute(AuthErrorCode.FAILURE_ATTRIBUTE);
        if (stored instanceof AuthenticationException authFailure) {
            return authFailure;
        }
        return ex;
    }
}
