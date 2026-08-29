package org.hashtagcms.workflows.security;

import jakarta.servlet.http.HttpServletRequest;
import org.hashtagcms.workflows.model.PersonalAccessToken;
import org.hashtagcms.workflows.model.User;
import org.hashtagcms.workflows.repository.PersonalAccessTokenRepository;
import org.hashtagcms.workflows.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Authenticates the {@code Authorization: Bearer {id}|{secret}} token against the
 * shared Laravel Sanctum tables — validating {@code sha256(secret)} against
 * `personal_access_tokens.token` and its expiry, then loading the `users` row.
 * These are the same tokens HashtagCMS issues; no PHP round-trip is needed.
 */
public class SanctumTokenUserResolver implements WorkflowUserResolver {

    private final UserRepository users;
    private final PersonalAccessTokenRepository tokens;

    public SanctumTokenUserResolver(UserRepository users, PersonalAccessTokenRepository tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    @Override
    public Map<String, Object> resolveUser(HttpServletRequest request) {
        if (request == null) return null;
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;

        String bearer = header.substring(7).trim();
        int pipe = bearer.indexOf('|');
        if (pipe < 1) return null;

        Long tokenId;
        try {
            tokenId = Long.parseLong(bearer.substring(0, pipe));
        } catch (NumberFormatException e) {
            return null;
        }
        String secret = bearer.substring(pipe + 1);

        PersonalAccessToken pat = tokens.findById(tokenId).orElse(null);
        if (pat == null) return null;
        if (!constantTimeEquals(sha256Hex(secret), pat.getToken())) return null;
        if (pat.getExpiresAt() != null && pat.getExpiresAt().isBefore(Instant.now())) return null;

        User user = pat.getTokenableId() == null ? null : users.findById(pat.getTokenableId()).orElse(null);
        if (user == null) return null;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        if (user.getEmail() != null) m.put("email", user.getEmail());
        if (user.getName() != null) m.put("name", user.getName());
        return m;
    }

    private static String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
