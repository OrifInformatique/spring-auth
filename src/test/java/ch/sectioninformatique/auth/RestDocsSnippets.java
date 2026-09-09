package ch.sectioninformatique.auth;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

/**
 * Field contracts used by Spring REST Docs.
 * If a documented JSON field is added, removed or renamed, the corresponding
 * integration test fails instead of leaving stale documentation.
 */
public final class RestDocsSnippets {

        private RestDocsSnippets() {
        }

        public static Snippet loginRequest() {
                return requestFields(
                                fieldWithPath("login").description("Email used as login"),
                                fieldWithPath("password").description("User password"));
        }

        public static Snippet userResponse() {
                return responseFields(userFields("", false));
        }

        public static Snippet userListResponse() {
                return responseFields(userFields("[].", true));
        }

        public static Snippet registerRequest() {
                return requestFields(
                                fieldWithPath("firstName").description("First name"),
                                fieldWithPath("lastName").description("Last name"),
                                fieldWithPath("login").description("Email used as login"),
                                fieldWithPath("password").description("Password (8 to 72 characters)"),
                                fieldWithPath("mainRole").optional().description("Initial role, for example USER"));
        }

        public static Snippet refreshResponse() {
                return responseFields(
                                fieldWithPath("accessToken").description("New JWT access token"));
        }

        public static Snippet passwordUpdateRequest() {
                return requestFields(
                                fieldWithPath("oldPassword").description("Current password"),
                                fieldWithPath("newPassword").description("New password (8 to 72 characters)"));
        }

        public static Snippet messageResponse() {
                return responseFields(
                                fieldWithPath("message").description("Localized status message"));
        }

        public static Snippet oauth2TokenRequest() {
                return requestFields(
                                fieldWithPath("login").description("Email of the authenticated user"),
                                fieldWithPath("code").description("One-time authentication code issued after OAuth2 login"));
        }

        public static Snippet userUpdateRequest() {
                return requestFields(
                                fieldWithPath("firstName").description("Updated first name"),
                                fieldWithPath("lastName").description("Updated last name"),
                                fieldWithPath("login").description("Updated email login"),
                                fieldWithPath("mainRole").description("Updated main role"));
        }

        private static org.springframework.restdocs.payload.FieldDescriptor[] userFields(String prefix,
                        boolean optionalItems) {
                var id = fieldWithPath(prefix + "id").description("User identifier");
                var firstName = fieldWithPath(prefix + "firstName").description("First name");
                var lastName = fieldWithPath(prefix + "lastName").description("Last name");
                var login = fieldWithPath(prefix + "login").description("Email used as login");
                var token = fieldWithPath(prefix + "token").optional().type(JsonFieldType.VARIES)
                                .description("JWT access token, present after login or registration");
                var deleted = fieldWithPath(prefix + "deleted").description("Whether the user is soft-deleted");
                var mainRole = fieldWithPath(prefix + "mainRole").description("Main role (USER, MANAGER or ADMIN)");
                var permissions = fieldWithPath(prefix + "permissions").type(JsonFieldType.ARRAY)
                                .description("Authorities granted by the role");
                if (optionalItems) {
                        id = id.optional().type(JsonFieldType.NUMBER);
                        firstName = firstName.optional().type(JsonFieldType.STRING);
                        lastName = lastName.optional().type(JsonFieldType.STRING);
                        login = login.optional().type(JsonFieldType.STRING);
                        deleted = deleted.optional().type(JsonFieldType.BOOLEAN);
                        mainRole = mainRole.optional().type(JsonFieldType.STRING);
                        permissions = permissions.optional().type(JsonFieldType.ARRAY);
                }
                return new org.springframework.restdocs.payload.FieldDescriptor[] {
                                id, firstName, lastName, login, token, deleted, mainRole, permissions
                };
        }
}
