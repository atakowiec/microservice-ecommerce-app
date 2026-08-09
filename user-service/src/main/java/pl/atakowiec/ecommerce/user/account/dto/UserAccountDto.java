package pl.atakowiec.ecommerce.user.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAccountDto {
    private Long id;
    private String username;
    private String email;
    private Role role;

    public static UserAccountDto from(UserAccount userAccount) {
        return new UserAccountDto(userAccount.getId(), userAccount.getUsername(), userAccount.getEmail(), userAccount.getRole());
    }
}
