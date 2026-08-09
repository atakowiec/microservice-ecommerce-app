package pl.atakowiec.ecommerce.user.account;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pl.atakowiec.ecommerce.shared.exception.HttpException;
import pl.atakowiec.ecommerce.user.account.dto.*;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserAccountService {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserAccountDto> searchUsers(String query, String stringRole) {
        Role role = resolveRole(stringRole);

        return this.searchUsers(query, role)
                .stream()
                .map(UserAccount::toDto)
                .toList();
    }

    private List<UserAccount> searchUsers(String query, Role role) {
        if (query == null && role == null) {
            return userAccountRepository.findAll();
        }

        if (query == null)
            query = "";

        if (role != null) {
            return userAccountRepository.findMatchingUsers(query, query, role);
        }

        return userAccountRepository.findMatchingUsers(query, query);
    }

    private Role resolveRole(String role) {
        try {
            return role == null ? null : Role.valueOf(role);
        } catch (IllegalArgumentException ex) {
            throw new HttpException(400, "Role '%s' does not exist.".formatted(role));
        }
    }

    public UserAccountDto createUser(UserAccountCreateDto createDto) {
        if (userAccountRepository.existsByEmail(createDto.getEmail())) {
            throw new HttpException(409, "This email is taken.");
        }

        if (userAccountRepository.existsByUsername(createDto.getUsername())) {
            throw new HttpException(409, "This username is taken.");
        }

        Role role = resolveRole(createDto.getRole());

        UserAccount newUser = userAccountRepository.save(new UserAccount(
                createDto.getEmail(),
                createDto.getUsername(),
                passwordEncoder.encode(createDto.getPassword()),
                role
        ));

        return newUser.toDto();
    }

    public UserAccountDto updateUser(UserAccountUpdateDto updateDto) {
        UserAccount userByEmail = userAccountRepository.getByEmail(updateDto.getEmail());
        if (userByEmail != null && !Objects.equals(userByEmail.getId(), updateDto.getId())) {
            throw new HttpException(409, "This email is taken.");
        }

        UserAccount userByUsername = userAccountRepository.getByUsername(updateDto.getUsername());
        if (userByUsername != null && !Objects.equals(userByUsername.getId(), updateDto.getId())) {
            throw new HttpException(409, "This username is taken.");
        }

        UserAccount account = userAccountRepository.findById(updateDto.getId())
                .orElseThrow(() -> new HttpException(404, "User with id " + updateDto.getId() + " does not exists."));

        Role role = resolveRole(updateDto.getRole());

        account.setUsername(updateDto.getUsername());
        account.setEmail(updateDto.getEmail());
        account.setRole(role);

        if (updateDto.getPassword() != null && !updateDto.getPassword().isEmpty()) {
            String passwordHash = passwordEncoder.encode(updateDto.getPassword());
            account.setPasswordHash(passwordHash);
        }

        userAccountRepository.save(account);

        return account.toDto();
    }

    public UserAccountDto deleteUser(Long userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new HttpException(404, "User with id " + userId + " does not exist."));

        userAccountRepository.delete(account);

        return account.toDto();
    }
}
