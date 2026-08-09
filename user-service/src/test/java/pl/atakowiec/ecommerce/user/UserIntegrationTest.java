package pl.atakowiec.ecommerce.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import pl.atakowiec.ecommerce.user.account.UserAccountRepository;
import pl.atakowiec.ecommerce.user.account.dto.Role;
import pl.atakowiec.ecommerce.user.account.dto.UserAccount;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UserIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpUsers() {
        userAccountRepository.deleteAll();
        userAccountRepository.saveAll(List.of(
                user("alice", "alice@shop.com", Role.USER),
                user("alicia-admin", "owner@shop.com", Role.ADMIN),
                user("bob", "bob@another.com", Role.USER),
                user("root", "alice-admin@shop.com", Role.ADMIN)
        ));
    }

    @Test
    void adminCanSearchUsersByUsername() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .queryParam("query", "alicia")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("alicia-admin"));
    }

    @Test
    void adminCanSearchUsersByEmail() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .queryParam("query", "alice-admin@shop.com")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("root"));
    }

    @Test
    void adminCanFilterUsersByRole() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .queryParam("role", "ADMIN")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].username",
                        containsInAnyOrder("alicia-admin", "root")));
    }

    @Test
    void adminCanFilterRegularUsersByRole() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .queryParam("role", "USER")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].username",
                        containsInAnyOrder("alice", "bob")));
    }

    @Test
    void adminCanCombineSearchAndRoleFilter() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .queryParam("query", "ali")
                        .queryParam("role", "ADMIN")
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].username",
                        containsInAnyOrder("alicia-admin", "root")));
    }

    @Test
    void adminCanCreateAdminByProvidingValidDetails() throws Exception {
        long accountsBeforeRequest = userAccountRepository.count();

        mockMvc.perform(post("/admin/users")
                        .content("""
                                {
                                    "username": "new_user",
                                    "email": "new_user@cieszczyk.pl",
                                    "password": "password",
                                    "role": "ADMIN"
                                }""")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(adminJwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", equalTo("new_user")))
                .andExpect(jsonPath("$.email", equalTo("new_user@cieszczyk.pl")))
                .andExpect(jsonPath("$.role", equalTo("ADMIN")))
                .andExpect(jsonPath("$.id", notNullValue()));

        UserAccount persistedAccount = userAccountRepository
                .findByUsername("new_user")
                .orElseThrow();

        assertThat(userAccountRepository.count()).isEqualTo(accountsBeforeRequest + 1);

        assertThat(persistedAccount.getId()).isNotNull();
        assertThat(persistedAccount.getUsername()).isEqualTo("new_user");
        assertThat(persistedAccount.getEmail()).isEqualTo("new_user@cieszczyk.pl");
        assertThat(persistedAccount.getRole()).isEqualTo(Role.ADMIN);

        // Verify that the raw password was not persisted.
        assertThat(persistedAccount.getPasswordHash()).isNotEqualTo("password");
        assertThat(passwordEncoder.matches("password", persistedAccount.getPasswordHash())).isTrue();
    }

    @Test
    void unauthorizedUserCannotCreateUser() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .content("""
                                {
                                    "username": "new_user",
                                    "email": "new_user@cieszczyk.pl",
                                    "password": "password",
                                    "role": "ADMIN"
                                }""")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminUserCannotCreateUser() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .content("""
                                {
                                    "username": "new_user",
                                    "email": "new_user@cieszczyk.pl",
                                    "password": "password",
                                    "role": "ADMIN"
                                }""")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(userJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanEditUsers() throws Exception {
        long accountsBeforeRequest = userAccountRepository.count();
        UserAccount initialUser = userAccountRepository
                .findByUsername("alice")
                .orElseThrow();

        mockMvc.perform(patch("/admin/users")
                        .content(json(Map.of(
                                "id", initialUser.getId(),
                                "username", "alicia_new",
                                "email", "alicia_new@cieszczyk.pl",
                                "password", "new_password",
                                "role", "ADMIN"
                        )))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(adminJwt()))
                .andExpect(status().isOk());

        assertThat(userAccountRepository.findByUsername("alice")).isEmpty(); // previous user should not exist

        assertThat(userAccountRepository.count()).isEqualTo(accountsBeforeRequest); // no user should be created

        UserAccount changedUser = userAccountRepository
                .findByUsername("alicia_new")
                .orElseThrow();

        assertThat(changedUser.getUsername()).isEqualTo("alicia_new");
        assertThat(changedUser.getEmail()).isEqualTo("alicia_new@cieszczyk.pl");
        assertThat(changedUser.getRole()).isEqualTo(Role.ADMIN);

        // Verify that the raw password was not persisted.
        assertThat(changedUser.getPasswordHash()).isNotEqualTo("new_password");
        assertThat(passwordEncoder.matches("new_password", changedUser.getPasswordHash())).isTrue();
    }

    @Test
    void unauthorizedUserCantEditUsers() throws Exception {
        mockMvc.perform(patch("/admin/users")
                        .content(json(Map.of(
                                "id", 1,
                                "username", "alicia_new",
                                "email", "alicia_new@cieszczyk.pl",
                                "password", "new_password",
                                "role", "ADMIN"
                        )))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminUserCantEditUsers() throws Exception {
        mockMvc.perform(patch("/admin/users")
                        .content(json(Map.of(
                                "id", 1,
                                "username", "alicia_new",
                                "email", "alicia_new@cieszczyk.pl",
                                "password", "new_password",
                                "role", "ADMIN"
                        )))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(userJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDeleteUsers() throws Exception {
        long accountsBeforeRequest = userAccountRepository.count();
        UserAccount initialUser = userAccountRepository
                .findByUsername("alice")
                .orElseThrow();

        mockMvc.perform(delete("/admin/users?id=" + initialUser.getId())
                        .with(adminJwt()))
                .andExpect(status().isOk());

        assertThat(userAccountRepository.findByUsername("alice")).isEmpty(); // previous user should not exist
        assertThat(userAccountRepository.findById(initialUser.getId())).isEmpty();

        assertThat(userAccountRepository.count()).isEqualTo(accountsBeforeRequest - 1); // no user should be created
    }

    @Test
    void unauthorizedUserCantDeleteUsers() throws Exception {
        mockMvc.perform(delete("/admin/users?id=1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminUserCantDeleteUsers() throws Exception {
        mockMvc.perform(delete("/admin/users?id=1")
                        .with(userJwt()))
                .andExpect(status().isForbidden());
    }

    private static UserAccount user(String username, String email, Role role) {
        return new UserAccount(email, username, "irrelevant-password-hash", role);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_ADMIN"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor userJwt() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_USER"));
    }

    private static String json(Map<Object, Object> map) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(map);
    }
}
