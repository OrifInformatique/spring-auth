package ch.sectioninformatique.auth.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request of {@code POST /oauth2/token}: the one-time code and user id received by the
 * client application in the redirect URL after an Azure login.
 *
 * @param userId identifier of the user who logged in
 * @param code   one-time authentication code
 */
public record AuthCodeExchangeDto(
        @NotNull Long userId,
        @NotBlank String code) {
}
