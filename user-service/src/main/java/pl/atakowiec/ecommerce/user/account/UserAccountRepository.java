package pl.atakowiec.ecommerce.user.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.atakowiec.ecommerce.user.account.dto.Role;
import pl.atakowiec.ecommerce.user.account.dto.UserAccount;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("""
            SELECT u
            FROM UserAccount u
            WHERE (u.email LIKE %:email% OR u.username LIKE %:username%)
              AND :role = u.role
            """)
    List<UserAccount> findMatchingUsers(
            @Param("email") String email,
            @Param("username") String username,
            @Param("role") Role role
    );

    @Query("""
            SELECT u
            FROM UserAccount u
            WHERE u.email LIKE %:email% OR u.username LIKE %:username%
            """)
    List<UserAccount> findMatchingUsers(
            @Param("email") String email,
            @Param("username") String username
    );

    UserAccount getByEmail(String email);
    UserAccount getByUsername(String username);

}
