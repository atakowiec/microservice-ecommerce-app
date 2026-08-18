package pl.atakowiec.ecommerce.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.atakowiec.ecommerce.user.account.UserAccountRepository;
import pl.atakowiec.ecommerce.user.account.UserAccountService;
import pl.atakowiec.ecommerce.user.account.dto.Role;
import pl.atakowiec.ecommerce.user.account.dto.UserAccount;
import pl.atakowiec.ecommerce.user.auth.AuthService;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({
        SecurityConfig.class,
        UserAccountService.class,
        UserServiceTest.TestUsersConfiguration.class
})
class UserServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    @MockitoBean
    private AuthService authService;

    @Test
    void adminCanViewAListOfAllRegisteredUsers() throws Exception {
        UserAccount regularUser = new UserAccount(
                "user@cieszczyk.pl",
                "user",
                "encoded-user-password",
                Role.USER
        );
        UserAccount admin = new UserAccount(
                "admin@cieszczyk.pl",
                "admin",
                "encoded-admin-password",
                Role.ADMIN
        );
        when(userAccountRepository.findAll()).thenReturn(List.of(regularUser, admin));

        mockMvc.perform(get("/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Users retrieved successfully"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].username").value("user"))
                .andExpect(jsonPath("$.data[1].username").value("admin"))
                .andExpect(jsonPath("$.data[0].email").value("user@cieszczyk.pl"))
                .andExpect(jsonPath("$.data[1].email").value("admin@cieszczyk.pl"));
    }

    @Test
    void adminCanFilterAndSearchInRegisteredUsers() {
        UserAccount regularUser = new UserAccount(
                "user@cieszczyk.pl",
                "user",
                "encoded-user-password",
                Role.USER
        );
        UserAccount admin = new UserAccount(
                "admin@cieszczyk.pl",
                "admin",
                "encoded-admin-password",
                Role.ADMIN
        );
        when(userAccountRepository.findAll()).thenReturn(List.of(regularUser, admin));
    }

    @Test
    void regularUserReceivesUnifiedForbiddenResponseForAdminEndpoint() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access is denied"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestUsersConfiguration {

        @Bean
        UserDetailsService testUserDetailsService(PasswordEncoder passwordEncoder) {
            return new InMemoryUserDetailsManager(
                    User.withUsername("admin")
                            .password(passwordEncoder.encode("admin"))
                            .roles("ADMIN")
                            .build()
            );
        }
    }
}
