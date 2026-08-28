package pl.atakowiec.ecommerce.user.account;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.atakowiec.ecommerce.user.account.dto.UserAccountCreateDto;
import pl.atakowiec.ecommerce.user.account.dto.UserAccountDto;
import pl.atakowiec.ecommerce.user.account.dto.UserAccountUpdateDto;
import pl.atakowiec.ecommerce.shared.response.ApiResponse;

import java.util.List;

@RestController()
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAccountAdminController {
    private final UserAccountService userAccountService;

    @GetMapping()
    public ApiResponse<List<UserAccountDto>> getUsers(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "role", required = false) String role
    ) {
        return ApiResponse.ok("Users retrieved successfully", userAccountService.searchUsers(query, role));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserAccountDto> createUser(@RequestBody UserAccountCreateDto createDto) {
        return ApiResponse.created("User created successfully", userAccountService.createUser(createDto));
    }

    @PatchMapping
    public ApiResponse<UserAccountDto> updateUser(@RequestBody UserAccountUpdateDto updateDto) {
        return ApiResponse.ok("User updated successfully", userAccountService.updateUser(updateDto));
    }

    @DeleteMapping("")
    public ApiResponse<UserAccountDto> deleteUser(@RequestParam("id") Long userId) {
        return ApiResponse.ok("User deleted successfully", userAccountService.deleteUser(userId));
    }
}
