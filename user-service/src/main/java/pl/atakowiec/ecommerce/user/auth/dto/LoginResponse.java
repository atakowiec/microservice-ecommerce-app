package pl.atakowiec.ecommerce.user.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String username,
        String role
) {
}
