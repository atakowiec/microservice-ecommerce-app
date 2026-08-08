package pl.atakowiec.ecommerce.user;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;
import pl.atakowiec.ecommerce.user.auth.AuthController;
import pl.atakowiec.ecommerce.user.auth.AuthExceptionHandler;
import pl.atakowiec.ecommerce.user.auth.AuthService;
import pl.atakowiec.ecommerce.user.auth.JwtService;

import java.time.Instant;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, TestController.class})
@Import({
        SecurityConfig.class,
        AuthService.class,
        JwtService.class,
        AuthExceptionHandler.class,
        AuthServiceTest.TestUsersConfiguration.class
})
class AuthServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void validAdminCredentialsReturnAdminToken() throws Exception {
        mockMvc.perform(loginRequest("admin", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles", hasItem("ADMIN")))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void invalidUsernameAndInvalidPasswordReturnTheSameGenericError() throws Exception {
        String wrongPasswordResponse = mockMvc.perform(loginRequest("admin", "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String unknownUserResponse = mockMvc.perform(loginRequest("unknown", "admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String wrongPasswordMessage = JsonPath.read(wrongPasswordResponse, "$.message");
        String unknownUserMessage = JsonPath.read(unknownUserResponse, "$.message");
        assertEquals(wrongPasswordMessage, unknownUserMessage);
    }

    @Test
    void expiredTokenRequiresReauthentication() throws Exception {
        mockMvc.perform(get("/test")
                        .header("Authorization", "Bearer " + expiredAdminToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void successfulLoginTokenCanAccessProtectedPageWithoutLoggingInAgain() throws Exception {
        String token = loginAndGetToken("admin", "admin");

        mockMvc.perform(get("/test").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/test").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest(
            String username,
            String password
    ) {
        return post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}
                        """.formatted(username, password));
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String response = mockMvc.perform(loginRequest(username, password))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private String expiredAdminToken() {
        Instant issuedAt = Instant.now().minusSeconds(7200);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ecommerce-user-service")
                .subject("admin")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(3600))
                .claim("scope", "USER ADMIN")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestUsersConfiguration {

        @Bean
        UserDetailsService testUserDetailsService(PasswordEncoder passwordEncoder) {
            return new InMemoryUserDetailsManager(
                    User.withUsername("admin")
                            .password(passwordEncoder.encode("admin"))
                            .roles("USER", "ADMIN")
                            .build()
            );
        }
    }
}
