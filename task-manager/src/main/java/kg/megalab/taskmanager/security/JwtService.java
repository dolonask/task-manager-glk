package kg.megalab.taskmanager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kg.megalab.taskmanager.domain.Role;
import kg.megalab.taskmanager.domain.User;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_DEPARTMENT_ID = "departmentId";
    private static final String CLAIM_CURATED_DEPARTMENT_IDS = "curatedDepartmentIds";
    private static final String CLAIM_LOGIN = "login";

    private final JwtProperties properties;
    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public long getAccessTokenTtlSeconds() {
        return properties.getAccessTokenTtlSeconds();
    }

    public long getRefreshTokenTtlSeconds() {
        return properties.getRefreshTokenTtlSeconds();
    }

    public String generateAccessToken(User user, Set<UUID> curatedDepartmentIds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_LOGIN, user.getLogin())
                .claim(CLAIM_ROLE, user.getRole().getValue())
                .claim(CLAIM_DEPARTMENT_ID, user.getDepartment() != null ? user.getDepartment().getId().toString() : null)
                .claim(CLAIM_CURATED_DEPARTMENT_IDS, curatedDepartmentIds.stream().map(UUID::toString).toList())
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plusSeconds(properties.getAccessTokenTtlSeconds())))
                .signWith(key)
                .compact();
    }

    public String generateOpaqueRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public Optional<UserPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            UUID id = UUID.fromString(claims.getSubject());
            Role role = Role.fromValue(claims.get(CLAIM_ROLE, String.class));
            String departmentIdStr = claims.get(CLAIM_DEPARTMENT_ID, String.class);
            UUID departmentId = departmentIdStr != null ? UUID.fromString(departmentIdStr) : null;
            @SuppressWarnings("unchecked")
            List<String> curatedRaw = claims.get(CLAIM_CURATED_DEPARTMENT_IDS, List.class);
            Set<UUID> curated = curatedRaw == null ? Set.of() :
                    curatedRaw.stream().map(UUID::fromString).collect(Collectors.toSet());
            String login = claims.get(CLAIM_LOGIN, String.class);
            return Optional.of(new UserPrincipal(id, login, role, departmentId, curated));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
