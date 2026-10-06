package ch.sectioninformatique.auth.support;

import static org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName;
import static org.springframework.restdocs.cookies.CookieDocumentation.requestCookies;
import static org.springframework.restdocs.cookies.CookieDocumentation.responseCookies;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;

import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

/**
 * Contracts of the documented requests and responses.
 *
 * REST Docs fails a test when a documented field, parameter, header or cookie is missing,
 * or when the payload contains an undocumented field. Adding, removing or renaming a field
 * therefore breaks the build until both the code and this contract agree, which keeps the
 * generated documentation accurate.
 */
public final class RestDocsSnippets {

    private static final String REFRESH_COOKIE = "refresh_token";

    private RestDocsSnippets() {
    }

    // Headers and cookies

    public static Snippet authorizationHeader() {
        return requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION)
                .description("`Bearer` followed by the access token"));
    }

    public static Snippet locationHeader() {
        return responseHeaders(headerWithName(HttpHeaders.LOCATION)
                .description("URL of the created resource"));
    }

    public static Snippet refreshCookieSet() {
        return responseCookies(cookieWithName(REFRESH_COOKIE)
                .description("New refresh token (HTTP-only, Secure, path `/auth/refresh`)"));
    }

    public static Snippet refreshCookieCleared() {
        return responseCookies(cookieWithName(REFRESH_COOKIE)
                .description("Emptied and expired, so that the browser deletes the refresh token"));
    }

    public static Snippet refreshCookieSent() {
        return requestCookies(cookieWithName(REFRESH_COOKIE)
                .description("Refresh token received at login or at the previous refresh"));
    }

    // Parameters

    public static Snippet loginPathParameter() {
        return pathParameters(parameterWithName("login").description("Login (email) of the user"));
    }

    public static Snippet userStatusQueryParameter() {
        return queryParameters(parameterWithName("status").optional()
                .description("`ACTIVE` (default), `DELETED` or `ALL`"));
    }

    public static Snippet permanentQueryParameter() {
        return queryParameters(parameterWithName("permanent").optional()
                .description("`true` to remove the user from the database; default `false` (restorable soft delete)"));
    }

    // Request bodies

    public static Snippet loginRequest() {
        return requestFields(
                fieldWithPath("login").description("Email used as login"),
                fieldWithPath("password").description("Password"));
    }

    public static Snippet authCodeExchangeRequest() {
        return requestFields(
                fieldWithPath("userId").description("`userId` received in the redirect URL"),
                fieldWithPath("code").description("`authCode` received in the redirect URL (single use, short-lived)"));
    }

    public static Snippet createUserRequest() {
        return requestFields(
                fieldWithPath("firstName").description("First name"),
                fieldWithPath("lastName").description("Last name"),
                fieldWithPath("login").description("Email used as login, must be unique"),
                fieldWithPath("password").description("Password, 8 to 72 characters"),
                fieldWithPath("mainRole").optional().type(JsonFieldType.STRING)
                        .description("`USER` (default), `MANAGER` or `ADMIN`. Only an admin can create an admin"));
    }

    public static Snippet updateUserRequest() {
        return requestFields(
                fieldWithPath("firstName").description("First name"),
                fieldWithPath("lastName").optional().type(JsonFieldType.STRING).description("Last name"),
                fieldWithPath("login").description("Email used as login, must be unique"));
    }

    public static Snippet roleUpdateRequest() {
        return requestFields(
                fieldWithPath("role").description("`USER`, `MANAGER` or `ADMIN`"));
    }

    public static Snippet passwordUpdateRequest() {
        return requestFields(
                fieldWithPath("oldPassword").description("Current password"),
                fieldWithPath("newPassword").description("New password, 8 to 72 characters, different from the current one"));
    }

    // Response bodies

    public static Snippet userResponse() {
        return responseFields(userFields(""));
    }

    public static Snippet userListResponse() {
        return responseFields(userFields("[]."));
    }

    public static Snippet tokenResponse() {
        return responseFields(fieldWithPath("accessToken").description("New JWT access token"));
    }

    public static Snippet messageResponse() {
        return responseFields(fieldWithPath("message").description("Localized message"));
    }

    public static Snippet errorResponse() {
        return responseFields(
                fieldWithPath("timestamp").description("When the error occurred (ISO-8601)"),
                fieldWithPath("status").description("HTTP status code"),
                fieldWithPath("error").description("HTTP reason phrase"),
                fieldWithPath("message").description("Localized error message"),
                subsectionWithPath("fieldErrors").optional().type(JsonFieldType.OBJECT)
                        .description("Validation errors only: localized message for each invalid field"));
    }

    private static FieldDescriptor[] userFields(String prefix) {
        return new FieldDescriptor[] {
                fieldWithPath(prefix + "id").description("User identifier"),
                fieldWithPath(prefix + "firstName").description("First name"),
                fieldWithPath(prefix + "lastName").optional().type(JsonFieldType.STRING).description("Last name"),
                fieldWithPath(prefix + "login").description("Email used as login"),
                fieldWithPath(prefix + "token").optional().type(JsonFieldType.STRING)
                        .description("JWT access token, only after login or code exchange"),
                fieldWithPath(prefix + "deleted").description("Whether the user is soft-deleted"),
                fieldWithPath(prefix + "mainRole").description("Role: `USER`, `MANAGER` or `ADMIN`"),
                fieldWithPath(prefix + "permissions").description("Authorities granted by the role")
        };
    }
}
