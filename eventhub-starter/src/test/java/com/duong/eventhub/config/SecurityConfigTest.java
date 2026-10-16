package com.duong.eventhub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.duong.eventhub.security.SecurityConfig;
import com.duong.eventhub.user.AppUser;
import com.duong.eventhub.user.AppUserRepository;
import com.duong.eventhub.user.Role;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {
    private static final UUID USER_ID = UUID.fromString("44c0d94a-9b24-4b33-a5c7-3de42d0b8f7a");

    @Mock
    private AppUserRepository users;

    private JwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
     //   converter = new SecurityConfig().jwtAuthenticationConverter(users);
    }

    @Test
    void usesCurrentDatabaseRolesInsteadOfStaleTokenRoles() {
        AppUser user = enabledUser();
        user.setRoles(Set.of(Role.CUSTOMER, Role.STAFF));
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));

        var authentication = converter.convert(token(USER_ID.toString(), 3L));

        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(USER_ID.toString());
        assertThat(authentication.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .filteredOn(authority -> authority.startsWith("ROLE_"))
            .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_STAFF");
    }

    @Test
    void rejectsUnknownUser() {
        when(users.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> converter.convert(token(USER_ID.toString(), 3)))
            .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void rejectsDisabledUser() {
        AppUser user = enabledUser();
        user.setEnabled(false);
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> converter.convert(token(USER_ID.toString(), 3)))
            .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void rejectsRevokedTokenVersion() {
        AppUser user = enabledUser();
        user.setTokenVersion(4);
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> converter.convert(token(USER_ID.toString(), 3)))
            .isInstanceOf(InvalidBearerTokenException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedClaims")
    void rejectsMalformedClaimsBeforeDatabaseLookup(String description, Object subject, Object version) {
        assertThatThrownBy(() -> converter.convert(token(subject, version)))
            .isInstanceOf(InvalidBearerTokenException.class);
        verifyNoInteractions(users);
    }

    private static Stream<Arguments> malformedClaims() {
        String subject = USER_ID.toString();
        return Stream.of(
            Arguments.of("missing subject", null, 3),
            Arguments.of("non-string subject", 42, 3),
            Arguments.of("invalid UUID", "not-a-uuid", 3),
            Arguments.of("abbreviated UUID", "1-1-1-1-1", 3),
            Arguments.of("missing token version", subject, null),
            Arguments.of("string token version", subject, "3"),
            Arguments.of("nonnumeric token version", subject, "invalid"),
            Arguments.of("fractional token version", subject, new BigDecimal("3.5")),
            Arguments.of("overflowing token version", subject, 2147483648L),
            Arguments.of("non-finite token version", subject, Double.NaN)
        );
    }

    private static AppUser enabledUser() {
        AppUser user = new AppUser();
        user.setId(USER_ID);
        user.setEnabled(true);
        user.setTokenVersion(3);
        return user;
    }

    private static Jwt token(Object subject, Object version) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
            .header("alg", "HS256")
            .claim("roles", List.of("ADMIN"));
        if (subject != null) {
            builder.claim("sub", subject);
        }
        if (version != null) {
            builder.claim("token_version", version);
        }
        return builder.build();
    }
}
