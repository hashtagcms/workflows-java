package org.hashtagcms.workflows;

import org.hashtagcms.workflows.model.PersonalAccessToken;
import org.hashtagcms.workflows.model.User;
import org.hashtagcms.workflows.repository.PersonalAccessTokenRepository;
import org.hashtagcms.workflows.repository.UserRepository;
import org.hashtagcms.workflows.security.SanctumTokenUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Verifies Laravel Sanctum bearer-token validation ({id}|{secret} → sha256 → user). */
class SanctumTokenUserResolverTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PersonalAccessTokenRepository tokens = mock(PersonalAccessTokenRepository.class);
    private final SanctumTokenUserResolver resolver = new SanctumTokenUserResolver(users, tokens);

    private MockHttpServletRequest bearer(String token) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.addHeader("Authorization", "Bearer " + token);
        return r;
    }

    private PersonalAccessToken token(String secretHash, Instant expiresAt, Long userId) {
        PersonalAccessToken pat = mock(PersonalAccessToken.class);
        when(pat.getToken()).thenReturn(secretHash);
        when(pat.getExpiresAt()).thenReturn(expiresAt);
        when(pat.getTokenableId()).thenReturn(userId);
        return pat;
    }

    private User user(long id, String email, String name) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        when(u.getEmail()).thenReturn(email);
        when(u.getName()).thenReturn(name);
        return u;
    }

    @Test
    void resolvesValidToken() {
        PersonalAccessToken pat = token(sha256("s3cret"), null, 42L);
        User sam = user(42L, "sam@example.com", "Sam");
        when(tokens.findById(7L)).thenReturn(Optional.of(pat));
        when(users.findById(42L)).thenReturn(Optional.of(sam));

        Map<String, Object> u = resolver.resolveUser(bearer("7|s3cret"));

        assertThat(u).isNotNull();
        assertThat(u.get("id")).isEqualTo(42L);
        assertThat(u.get("email")).isEqualTo("sam@example.com");
        assertThat(u.get("name")).isEqualTo("Sam");
    }

    @Test
    void rejectsWrongSecret() {
        PersonalAccessToken pat = token(sha256("s3cret"), null, 42L);
        when(tokens.findById(7L)).thenReturn(Optional.of(pat));
        assertThat(resolver.resolveUser(bearer("7|wrong"))).isNull();
        verify(users, never()).findById(anyLong());
    }

    @Test
    void rejectsExpiredToken() {
        PersonalAccessToken pat = token(sha256("s3cret"), Instant.now().minusSeconds(60), 42L);
        when(tokens.findById(7L)).thenReturn(Optional.of(pat));
        assertThat(resolver.resolveUser(bearer("7|s3cret"))).isNull();
    }

    @Test
    void rejectsUnknownTokenId() {
        when(tokens.findById(9L)).thenReturn(Optional.empty());
        assertThat(resolver.resolveUser(bearer("9|whatever"))).isNull();
    }

    @Test
    void rejectsMalformedHeaders() {
        assertThat(resolver.resolveUser(bearer("no-pipe-here"))).isNull();     // no pipe
        assertThat(resolver.resolveUser(bearer("abc|secret"))).isNull();       // non-numeric id
        assertThat(resolver.resolveUser(bearer("|secret"))).isNull();          // empty id
        MockHttpServletRequest basic = new MockHttpServletRequest();
        basic.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        assertThat(resolver.resolveUser(basic)).isNull();                      // not Bearer
        assertThat(resolver.resolveUser(new MockHttpServletRequest())).isNull(); // no header
        assertThat(resolver.resolveUser(null)).isNull();                       // no request
    }

    private static String sha256(String in) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(in.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(h.length * 2);
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
