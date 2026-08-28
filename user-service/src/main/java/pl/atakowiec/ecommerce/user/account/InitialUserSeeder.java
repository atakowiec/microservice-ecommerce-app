package pl.atakowiec.ecommerce.user.account;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.atakowiec.ecommerce.user.account.dto.Role;
import pl.atakowiec.ecommerce.user.account.dto.UserAccount;

import java.util.Locale;

@Component
@ConditionalOnProperty(name = "auth.bootstrap.enabled", havingValue = "true")
public class InitialUserSeeder implements ApplicationRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String userUsername;
    private final String userPassword;
    private final String adminUsername;
    private final String adminPassword;

    public InitialUserSeeder(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${auth.bootstrap.user.username}") String userUsername,
            @Value("${auth.bootstrap.user.password}") String userPassword,
            @Value("${auth.bootstrap.admin.username}") String adminUsername,
            @Value("${auth.bootstrap.admin.password}") String adminPassword
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.userUsername = userUsername;
        this.userPassword = userPassword;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        createIfMissing(userUsername, userPassword, Role.USER);
        createIfMissing(adminUsername, adminPassword, Role.ADMIN);
    }

    private void createIfMissing(String username, String password, Role role) {
        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);

        if (userAccountRepository.existsByUsername(normalizedUsername)) {
            return;
        }

        userAccountRepository.save(new UserAccount(
                normalizedUsername + "@cieszczyk.pl",
                normalizedUsername,
                passwordEncoder.encode(password),
                role
        ));
    }
}
