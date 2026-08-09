package pl.atakowiec.ecommerce.user.account;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.atakowiec.ecommerce.user.account.dto.UserAccountCreateDto;
import pl.atakowiec.ecommerce.user.account.dto.UserAccountDto;
import pl.atakowiec.ecommerce.user.account.dto.UserAccountUpdateDto;

import java.util.List;

@RestController()
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAccountAdminController {
    private final UserAccountService userAccountService;

    @GetMapping()
    public List<UserAccountDto> getUsers(@RequestParam(value = "query", required = false) String query,
                                         @RequestParam(value = "role", required = false) String role) {
        return userAccountService.searchUsers(query, role);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserAccountDto createUser(@RequestBody UserAccountCreateDto createDto) {
        return userAccountService.createUser(createDto);
    }

    @PatchMapping
    public UserAccountDto updateUser(@RequestBody UserAccountUpdateDto updateDto) {
        return userAccountService.updateUser(updateDto);
    }

    @DeleteMapping("")
    public UserAccountDto deleteUser(@RequestParam("id") Long userId) {
        return userAccountService.deleteUser(userId);
    }
}
