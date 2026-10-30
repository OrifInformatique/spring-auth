package ch.sectioninformatique.auth.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ch.sectioninformatique.auth.role.RoleEnum;
import ch.sectioninformatique.auth.support.AbstractIntegrationTest;
import ch.sectioninformatique.auth.support.RestDocsSnippets;
import ch.sectioninformatique.auth.support.TestUsers;
import ch.sectioninformatique.auth.user.UserExceptions.UserNotFoundException;

/**
 * HTTP tests of the users resource under {@code /users}.
 */
class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Adds the bearer token of the given user and a JSON body to a request. */
    private MockHttpServletRequestBuilder as(String login, MockHttpServletRequestBuilder request, String json) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(login))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }

    private MockHttpServletRequestBuilder as(String login, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(login));
    }

    private static String newUser(String login, String role) {
        String roleField = role == null ? "" : ", \"mainRole\": \"%s\"".formatted(role);
        return """
                {"firstName": "New", "lastName": "User", "login": "%s", "password": "NewUser123!"%s}"""
                .formatted(login, roleField);
    }

    private static String roleBody(String role) {
        return "{\"role\": \"%s\"}".formatted(role);
    }

    @Nested
    class CurrentUser {

        @Test
        void returnsAuthenticatedUser() throws Exception {
            mockMvc.perform(as(TestUsers.MANAGER, get("/users/me")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.login").value(TestUsers.MANAGER))
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.mainRole").value("MANAGER"))
                    .andExpect(jsonPath("$.token").doesNotExist())
                    .andDo(document("users/me",
                            RestDocsSnippets.authorizationHeader(),
                            RestDocsSnippets.userResponse()));
        }

        @Test
        void withoutToken_returns401() throws Exception {
            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message")
                            .value(message("error.security.authentication.token.invalid.or.missing")));
        }

        @Test
        void changePassword_returns204AndNewPasswordWorks() throws Exception {
            mockMvc.perform(as(TestUsers.USER, put("/users/me/password"),
                    "{\"oldPassword\": \"%s\", \"newPassword\": \"BrandNew123!\"}".formatted(TestUsers.USER_PASSWORD)))
                    .andExpect(status().isNoContent())
                    .andDo(document("users/update-password",
                            RestDocsSnippets.authorizationHeader(),
                            RestDocsSnippets.passwordUpdateRequest()));

            String storedHash = userRepository.findByLogin(TestUsers.USER).orElseThrow().getPassword();
            assertThat(passwordEncoder.matches("BrandNew123!", storedHash)).isTrue();
        }

        @Test
        void changePassword_withWrongCurrentPassword_returns400() throws Exception {
            mockMvc.perform(as(TestUsers.USER, put("/users/me/password"),
                    "{\"oldPassword\": \"WrongPassword1!\", \"newPassword\": \"BrandNew123!\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(message("error.user.current.password.invalid")))
                    .andDo(document("users/update-password-wrong-current", RestDocsSnippets.errorResponse()));
        }

        @Test
        void changePassword_toSamePassword_returns400OnNewPasswordField() throws Exception {
            mockMvc.perform(as(TestUsers.USER, put("/users/me/password"),
                    "{\"oldPassword\": \"%1$s\", \"newPassword\": \"%1$s\"}".formatted(TestUsers.USER_PASSWORD)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.newPassword").value(message("validation.password.not.reused")))
                    .andDo(document("users/update-password-validation-error", RestDocsSnippets.errorResponse()));
        }

        @Test
        void changePassword_withoutBody_returns400() throws Exception {
            mockMvc.perform(as(TestUsers.USER, put("/users/me/password")).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(message("error.request.body.unreadable")));
        }
    }

    @Nested
    class ListAndGet {

        @Test
        void list_returnsActiveUsersByDefault() throws Exception {
            mockMvc.perform(as(TestUsers.USER, get("/users")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].login").value(hasItem(TestUsers.ADMIN)))
                    .andExpect(jsonPath("$[*].login").value(not(hasItem(TestUsers.DELETED))))
                    .andDo(document("users/list",
                            RestDocsSnippets.authorizationHeader(),
                            RestDocsSnippets.userStatusQueryParameter(),
                            RestDocsSnippets.userListResponse()));
        }

        @Test
        void list_withDeletedStatus_returnsOnlySoftDeletedUsers() throws Exception {
            mockMvc.perform(as(TestUsers.MANAGER, get("/users").param("status", "DELETED")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].login").value(hasItem(TestUsers.DELETED)))
                    .andExpect(jsonPath("$[*].deleted").value(not(hasItem(false))))
                    .andDo(document("users/list-deleted",
                            RestDocsSnippets.userStatusQueryParameter(),
                            RestDocsSnippets.userListResponse()));
        }

        @Test
        void list_withAllStatus_returnsActiveAndDeletedUsers() throws Exception {
            mockMvc.perform(as(TestUsers.MANAGER, get("/users").param("status", "ALL")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].login").value(hasItem(TestUsers.DELETED)))
                    .andExpect(jsonPath("$[*].login").value(hasItem(TestUsers.ADMIN)));
        }

        @Test
        void list_withUnknownStatus_returns400() throws Exception {
            mockMvc.perform(as(TestUsers.MANAGER, get("/users").param("status", "SOMETHING")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(message("error.request.parameter.invalid", "status")));
        }

        @Test
        void get_returnsUser() throws Exception {
            mockMvc.perform(as(TestUsers.USER, get("/users/{login}", TestUsers.MANAGER)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.login").value(TestUsers.MANAGER))
                    .andExpect(jsonPath("$.mainRole").value("MANAGER"))
                    .andDo(document("users/get",
                            RestDocsSnippets.loginPathParameter(),
                            RestDocsSnippets.userResponse()));
        }

        @Test
        void get_unknownUser_returns404() throws Exception {
            mockMvc.perform(as(TestUsers.USER, get("/users/{login}", "unknown@test.com")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(message("error.user.not.found", "unknown@test.com")))
                    .andDo(document("users/get-not-found", RestDocsSnippets.errorResponse()));
        }

        @Test
        void get_softDeletedUser_returns404() throws Exception {
            mockMvc.perform(as(TestUsers.USER, get("/users/{login}", TestUsers.DELETED)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Create {

        @Test
        void managerCreatesUser_returns201WithLocation() throws Exception {
            mockMvc.perform(as(TestUsers.MANAGER, post("/users"), newUser("created@test.com", "USER")))
                    .andExpect(status().isCreated())
                    .andExpect(header().string(HttpHeaders.LOCATION, "/users/created@test.com"))
                    .andExpect(jsonPath("$.login").value("created@test.com"))
                    .andExpect(jsonPath("$.mainRole").value("USER"))
                    .andExpect(jsonPath("$.token").doesNotExist())
                    .andDo(document("users/create",
                            RestDocsSnippets.authorizationHeader(),
                            RestDocsSnippets.createUserRequest(),
                            RestDocsSnippets.locationHeader(),
                            RestDocsSnippets.userResponse()));

            String storedHash = userRepository.findByLogin("created@test.com").orElseThrow().getPassword();
            assertThat(storedHash).isNotEqualTo("NewUser123!");
            assertThat(passwordEncoder.matches("NewUser123!", storedHash)).isTrue();
        }

        @Test
        void withoutRole_createsRegularUser() throws Exception {
            mockMvc.perform(as(TestUsers.MANAGER, post("/users"), newUser("no.role@test.com", null)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mainRole").value("USER"));
        }

        @Test
        void adminCreatesAdmin_returns201() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, post("/users"), newUser("new.admin@test.com", "ADMIN")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mainRole").value("ADMIN"));
        }

        @Test
        void managerCannotCreateAdmin() throws Exception {
            mockMvc.perform(as(TestUsers.MANAGER, post("/users"), newUser("new.admin@test.com", "ADMIN")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(message("error.security.insufficient.rights", TestUsers.MANAGER)))
                    .andDo(document("users/create-admin-forbidden", RestDocsSnippets.errorResponse()));

            assertThat(userRepository.existsByLogin("new.admin@test.com")).isFalse();
        }

        @Test
        void regularUserCannotCreateUsers() throws Exception {
            mockMvc.perform(as(TestUsers.USER, post("/users"), newUser("created@test.com", "USER")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(message("error.security.access.denied")))
                    .andDo(document("users/access-denied", RestDocsSnippets.errorResponse()));
        }

        @Test
        void existingLogin_returns409() throws Exception {
            mockMvc.perform(as(TestUsers.MANAGER, post("/users"), newUser(TestUsers.USER, "USER")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(message("error.user.already.exists", TestUsers.USER)))
                    .andDo(document("users/create-conflict", RestDocsSnippets.errorResponse()));
        }

        @Test
        void loginOfSoftDeletedUser_returns409() throws Exception {
            mockMvc.perform(as(TestUsers.MANAGER, post("/users"), newUser(TestUsers.DELETED, "USER")))
                    .andExpect(status().isConflict());
        }

        @Test
        void invalidFields_return400WithFieldErrors() throws Exception {
            String body = """
                    {"firstName": "R2D2", "lastName": "", "login": "not-an-email", "password": "short"}""";

            mockMvc.perform(as(TestUsers.MANAGER, post("/users"), body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.firstName").isNotEmpty())
                    .andExpect(jsonPath("$.fieldErrors.lastName").isNotEmpty())
                    .andExpect(jsonPath("$.fieldErrors.login").isNotEmpty())
                    .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty())
                    .andDo(document("users/create-validation-error", RestDocsSnippets.errorResponse()));
        }

        @Test
        void unknownRole_returns400() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, post("/users"), newUser("created@test.com", "SUPERUSER")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(message("error.request.body.unreadable")));
        }
    }

    @Nested
    class Update {

        private static final String UPDATE_BODY = """
                {"firstName": "Updated", "lastName": "Name", "login": "updated@test.com"}""";

        @Test
        void managerUpdatesUser_returnsUpdatedUser() throws Exception {
            mockMvc.perform(as(TestUsers.MANAGER, put("/users/{login}", TestUsers.USER), UPDATE_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Updated"))
                    .andExpect(jsonPath("$.login").value("updated@test.com"))
                    .andExpect(jsonPath("$.mainRole").value("USER"))
                    .andDo(document("users/update",
                            RestDocsSnippets.loginPathParameter(),
                            RestDocsSnippets.updateUserRequest(),
                            RestDocsSnippets.userResponse()));
        }

        @Test
        void roleInBody_isIgnoredAndCannotEscalatePrivileges() throws Exception {
            String body = """
                    {"firstName": "Test", "lastName": "User", "login": "%s", "mainRole": "ADMIN"}"""
                    .formatted(TestUsers.USER);

            mockMvc.perform(as(TestUsers.MANAGER, put("/users/{login}", TestUsers.USER), body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mainRole").value("USER"));
        }

        @Test
        void managerCannotUpdateAdmin() throws Exception {
            mockMvc.perform(as(TestUsers.MANAGER, put("/users/{login}", TestUsers.ADMIN), UPDATE_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        void loginUsedByAnotherUser_returns409() throws Exception {
            String body = """
                    {"firstName": "Test", "lastName": "User", "login": "%s"}""".formatted(TestUsers.MANAGER);

            mockMvc.perform(as(TestUsers.ADMIN, put("/users/{login}", TestUsers.USER), body))
                    .andExpect(status().isConflict());
        }

        @Test
        void unknownUser_returns404() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, put("/users/{login}", "unknown@test.com"), UPDATE_BODY))
                    .andExpect(status().isNotFound());
        }

        @Test
        void invalidLogin_returns400() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, put("/users/{login}", TestUsers.USER),
                    "{\"firstName\": \"Test\", \"login\": \"not-an-email\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.login").isNotEmpty());
        }

        @Test
        void regularUserCannotUpdateUsers() throws Exception {
            mockMvc.perform(as(TestUsers.USER, put("/users/{login}", TestUsers.MANAGER), UPDATE_BODY))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class ChangeRole {

        @Test
        void managerPromotesUserToManager() throws Exception {
            mockMvc.perform(as(TestUsers.MANAGER, put("/users/{login}/role", TestUsers.USER), roleBody("MANAGER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mainRole").value("MANAGER"))
                    .andExpect(jsonPath("$.permissions").value(hasItem("user:update")))
                    .andDo(document("users/update-role",
                            RestDocsSnippets.loginPathParameter(),
                            RestDocsSnippets.roleUpdateRequest(),
                            RestDocsSnippets.userResponse()));
        }

        @Test
        void adminPromotesUserToAdmin() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, put("/users/{login}/role", TestUsers.USER), roleBody("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mainRole").value("ADMIN"));
        }

        @Test
        void adminDemotesAnotherAdmin() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, put("/users/{login}/role", TestUsers.OTHER_ADMIN), roleBody("MANAGER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mainRole").value("MANAGER"));
        }

        @Test
        void managerCannotGrantAdminRole() throws Exception {
            mockMvc.perform(as(TestUsers.MANAGER, put("/users/{login}/role", TestUsers.USER), roleBody("ADMIN")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(message("error.security.insufficient.rights", TestUsers.MANAGER)))
                    .andDo(document("users/update-role-forbidden", RestDocsSnippets.errorResponse()));
        }

        @Test
        void managerCannotDemoteAdmin() throws Exception {
            mockMvc.perform(as(TestUsers.MANAGER, put("/users/{login}/role", TestUsers.ADMIN), roleBody("USER")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void adminCannotChangeOwnRole() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, put("/users/{login}/role", TestUsers.ADMIN), roleBody("USER")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(message("error.user.self.modification.forbidden")))
                    .andDo(document("users/update-role-self", RestDocsSnippets.errorResponse()));
        }

        @Test
        void sameRole_returns409() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, put("/users/{login}/role", TestUsers.MANAGER), roleBody("MANAGER")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message")
                            .value(message("error.user.already.has.role", TestUsers.MANAGER, "MANAGER")))
                    .andDo(document("users/update-role-conflict", RestDocsSnippets.errorResponse()));
        }

        @Test
        void missingRole_returns400() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, put("/users/{login}/role", TestUsers.USER), "{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.role").isNotEmpty());
        }

        @Test
        void unknownUser_returns404() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, put("/users/{login}/role", "unknown@test.com"), roleBody("USER")))
                    .andExpect(status().isNotFound());
        }

        @Test
        void regularUserCannotChangeRoles() throws Exception {
            mockMvc.perform(as(TestUsers.USER, put("/users/{login}/role", TestUsers.MANAGER), roleBody("USER")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void me_reflectsRoleChangedAfterTheTokenWasIssued() throws Exception {
            // The token still says MANAGER, but /users/me reads the current role from the database
            String managerToken = bearer(TestUsers.MANAGER);
            userService.changeRole(TestUsers.MANAGER, RoleEnum.USER, TestUsers.ADMIN);

            mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, managerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mainRole").value("USER"));
        }
    }

    @Nested
    class DeleteAndRestore {

        @Test
        void softDelete_returns204AndUserCanBeRestored() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, delete("/users/{login}", TestUsers.USER)))
                    .andExpect(status().isNoContent())
                    .andDo(document("users/delete",
                            RestDocsSnippets.loginPathParameter(),
                            RestDocsSnippets.permanentQueryParameter()));

            mockMvc.perform(as(TestUsers.ADMIN, get("/users/{login}", TestUsers.USER)))
                    .andExpect(status().isNotFound());
            assertThat(userRepository.findByLoginAndDeletedTrue(TestUsers.USER)).isPresent();

            mockMvc.perform(as(TestUsers.MANAGER, post("/users/{login}/restore", TestUsers.USER)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.login").value(TestUsers.USER))
                    .andExpect(jsonPath("$.deleted").value(false))
                    .andDo(document("users/restore",
                            RestDocsSnippets.loginPathParameter(),
                            RestDocsSnippets.userResponse()));
        }

        @Test
        void permanentDelete_removesUserFromDatabase() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, delete("/users/{login}", TestUsers.USER).param("permanent", "true")))
                    .andExpect(status().isNoContent())
                    .andDo(document("users/delete-permanent",
                            RestDocsSnippets.loginPathParameter(),
                            RestDocsSnippets.permanentQueryParameter()));

            assertThat(userRepository.findByLogin(TestUsers.USER)).isEmpty();
        }

        @Test
        void permanentDelete_ofSoftDeletedUser_isAllowed() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, delete("/users/{login}", TestUsers.DELETED).param("permanent", "true")))
                    .andExpect(status().isNoContent());

            assertThat(userRepository.findByLogin(TestUsers.DELETED)).isEmpty();
        }

        @Test
        void adminCannotDeleteOwnAccount() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, delete("/users/{login}", TestUsers.ADMIN)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(message("error.user.self.modification.forbidden")));
        }

        @Test
        void managerCannotDelete() throws Exception {
            mockMvc.perform(as(TestUsers.MANAGER, delete("/users/{login}", TestUsers.USER)))
                    .andExpect(status().isForbidden());

            assertThat(userService.findByLogin(TestUsers.USER)).isNotNull();
        }

        @Test
        void unknownUser_returns404() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, delete("/users/{login}", "unknown@test.com")))
                    .andExpect(status().isNotFound());
        }

        @Test
        void invalidPermanentParameter_returns400() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, delete("/users/{login}", TestUsers.USER).param("permanent", "maybe")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void restoreActiveUser_returns404() throws Exception {
            mockMvc.perform(as(TestUsers.MANAGER, post("/users/{login}/restore", TestUsers.USER)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deletedUser_isNoLongerFoundByService() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, delete("/users/{login}", TestUsers.MANAGER)))
                    .andExpect(status().isNoContent());

            assertThatThrownBy(() -> userService.findByLogin(TestUsers.MANAGER))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    class GenericErrors {

        @Test
        void unsupportedMethod_returns405() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, post("/users/me")))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.message").value(message("error.request.method.unsupported", "POST")))
                    .andDo(document("users/method-not-allowed", RestDocsSnippets.errorResponse()));
        }

        @Test
        void unknownPath_returns404() throws Exception {
            mockMvc.perform(as(TestUsers.ADMIN, get("/users/{login}/unknown", TestUsers.USER)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(message("error.request.resource.not.found")));
        }
    }
}
