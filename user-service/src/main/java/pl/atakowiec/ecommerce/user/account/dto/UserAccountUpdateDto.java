package pl.atakowiec.ecommerce.user.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountUpdateDto {
    private Long id;
    private String username;
    private String email;
    private String password;
    private String role;
}
