package cn.jongwong.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Getter
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties implements RequestMatcher {

    private List<String> permitAll = new ArrayList<>();
    private RequestMatcher delegate = request -> false;

    public void setPermitAll(List<String> permitAll) {
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        if (permitAll != null) {
            for (String path : permitAll) {
                if (path != null && (path.contains("*") || path.contains("{"))) {
                    throw new IllegalArgumentException(
                            "app.security.permit-all must list exact paths, not wildcards: " + path);
                }
                if (path != null && !path.isBlank()) {
                    paths.add(path.trim());
                }
            }
        }
        this.permitAll = new ArrayList<>(paths);
        this.delegate = buildDelegate(this.permitAll);
    }

    public String[] permitAllPaths() {
        return effectivePaths().toArray(String[]::new);
    }

    List<String> effectivePaths() {
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        for (String path : permitAll) {
            String normalized = normalize(path);
            if (normalized.isEmpty()) {
                continue;
            }
            paths.add(normalized);
            if (normalized.startsWith("/api/") && normalized.length() > 5) {
                paths.add(normalized.substring(4));
            }
        }
        return new ArrayList<>(paths);
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        boolean allowed = delegate.matches(request) || matchesAnyCandidate(request);
        if (!allowed && containsOauth(request)) {
            log.warn(
                    "OAuth request was not anonymous: method={} uri={} servlet={} context={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getServletPath(),
                    request.getContextPath()
            );
        }
        return allowed;
    }

    public boolean isPermitAll(String uri) {
        return effectivePaths().contains(normalize(uri));
    }

    private boolean matchesAnyCandidate(HttpServletRequest request) {
        Set<String> allowed = new LinkedHashSet<>(effectivePaths());
        for (String candidate : pathCandidates(request)) {
            if (allowed.contains(candidate)) {
                return true;
            }
            for (String path : allowed) {
                if (candidate.endsWith(path)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsOauth(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.contains("/oauth/");
    }

    private static RequestMatcher buildDelegate(List<String> permitAll) {
        List<RequestMatcher> matchers = new ArrayList<>();
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        for (String path : permitAll) {
            String normalized = normalize(path);
            if (normalized.isEmpty()) {
                continue;
            }
            paths.add(normalized);
            if (normalized.startsWith("/api/") && normalized.length() > 5) {
                paths.add(normalized.substring(4));
            }
        }
        for (String path : paths) {
            matchers.add(AntPathRequestMatcher.antMatcher(path));
        }
        return matchers.isEmpty() ? request -> false : new OrRequestMatcher(matchers);
    }

    static Set<String> pathCandidates(HttpServletRequest request) {
        Set<String> paths = new LinkedHashSet<>();
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        addPath(paths, request.getRequestURI(), context);
        addPath(paths, request.getServletPath(), context);
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && !pathInfo.isBlank()) {
            addPath(paths, request.getServletPath() + pathInfo, context);
        }
        return paths;
    }

    private static void addPath(Set<String> paths, String raw, String context) {
        String path = normalize(raw);
        if (path.isEmpty()) {
            return;
        }
        if (!context.isEmpty() && path.startsWith(context)) {
            path = normalize(path.substring(context.length()));
        }
        if (path.isEmpty()) {
            return;
        }
        paths.add(path);
        if (path.startsWith("/api/") || path.equals("/api")) {
            return;
        }
        paths.add(normalize("/api" + (path.startsWith("/") ? path : "/" + path)));
    }

    private static String normalize(String uri) {
        if (uri == null || uri.isBlank()) {
            return "";
        }
        String path = uri;
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        int semicolon = path.indexOf(';');
        if (semicolon >= 0) {
            path = path.substring(0, semicolon);
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
