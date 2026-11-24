package ch.sectioninformatique.auth.auth.dto;

/**
 * Response of {@code POST /auth/refresh}.
 *
 * @param accessToken new short-lived JWT access token
 */
public record TokenResponseDto(String accessToken) {
}
