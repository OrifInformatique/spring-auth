# AUTH PACKAGE
AUTH Documentation of Spring-Auth


## *Table of Contents*

* [AuthCode.java](#authcodejava)
* [AuthCodeDto.java](#authcodedtojava)
* [AuthCodeRepository.java](#authcoderepositoryjava)
* [AuthCodeRepositoryImpl.java](#authcoderepositoryimpljava)
* [AuthController.java](#authcontrollerjava)

  * [login()](#login)
  * [refreshLogin()](#refreshlogin)
  * [register()](#register)
  * [updatePassword()](#updatepassword)
  * [logout()](#logout)
  * [redirectAfterLogin()](#redirectafterlogin)
  * [Security](#security)
* [AuthExceptions.java](#authexceptionsjava)

  * [InvalidCredentialsException](#invalidcredentialsexception)
  * [InvalidRefreshTokenException](#invalidrefreshtokenexception)
  * [AuthCodeNotFoundException](#authcodenotfoundexception)
* [AuthService.java](#authservicejava)

  * [retrieveAndDeleteAuthCode()](#retrieveanddeleteauthcode)
  * [generateAndStoreAuthCode()](#generateandstoreauthcode)
* [CredentialsDto.java](#credentialsdtojava)
* [OAuth2Controller.java](#oauth2controllerjava)

  * [initiateAzureLogin()](#initiateazurelogin)
  * [oauth2Success()](#oauth2success)
  * [getToken()](#gettoken)
  * [OAuth2 flow](#oauth2-flow)
  * [Security](#security-1)
* [PasswordConfig.java](#passwordconfigjava)

  * [passwordEncoder()](#passwordencoder)
* [PasswordNotReused.java](#passwordnotreusedjava)

  * [PasswordNotReusedValidatorImpl](#passwordnotreusedvalidatorimpl)
  * [areCharArraysEqual()](#arechararraysequal)
* [PasswordUpdateDto.java](#passwordupdatedtojava)
* [RefreshToken.java](#refreshtokenjava)
* [RefreshTokenRepository.java](#refreshtokenrepositoryjava)

  * [findByUserLoginAndRevokedFalse()](#findbyuserloginandrevokedfalse)
  * [deleteByUserLogin()](#deletebyuserlogin)
* [SignUpDto.java](#signupdtojava)
* [TokenResponseDto.java](#tokenresponsedtojava)

---

## *AuthCode.java*

`AuthCode` is a JPA Entity class used to store temporary authentication codes in the `spring-auth` service.

It is used after a successful OAuth2 login to allow the client application to exchange the code for JWT access and refresh tokens.

It contains :

* `id`,
* `code`,
* `userLogin`,
* `redirectUrl`,
* `expiresAt`,
* `createdAt`

The `code` field stores a **hashed authentication code** rather than the original code. This prevents the raw code from being exposed if the database is compromised.

The `userLogin` identifies the user associated with the authentication code.

The `redirectUrl` stores the URL to which the client should be redirected after authentication.

The `expiresAt` field defines when the authentication code becomes invalid.

The `createdAt` field is automatically set by Hibernate when the authentication code is created.

Authentication codes are temporary and can only be used once before being removed from the database.


---

## *AuthCodeDto.java*

`AuthCodeDto` is a Data Transfer Object used to transfer a temporary authentication code between the `spring-auth` service and the client application.

It contains :

* `login`,
* `code`

The `login` identifies the user associated with the authentication code.

The `code` contains the temporary authentication code that can be exchanged for JWT access and refresh tokens.

Because `AuthCodeDto` is a Java `record`, it is immutable and automatically provides its constructor, accessors, `equals()`, `hashCode()`, and `toString()` methods.


---

## *AuthCodeRepository.java*

`AuthCodeRepository` is a repository interface used to access and manage `AuthCode` entities in the `spring-auth` database.

It extends `JpaRepository<AuthCode, Long>`, which provides standard CRUD operations.

It contains :

* `findByUserLogin()`,
* `deleteExpiredCodes()`

### `findByUserLogin()`

Retrieves all authentication codes associated with a specific user login.

### `deleteExpiredCodes()`

Deletes authentication codes that have expired.

This helps remove authentication codes that are no longer valid from the database.

Spring Data JPA automatically provides the repository implementation.


---

## *AuthCodeRepositoryImpl.java*

`AuthCodeRepositoryImpl` is the implementation class used to provide the custom `deleteExpiredCodes()` method for `AuthCodeRepository`.

It uses `EntityManager` to execute a JPQL query directly on the database.

It contains :

* `entityManager`,
* `deleteExpiredCodes()`

### `deleteExpiredCodes()`

Deletes all authentication codes whose `expiresAt` date is earlier than or equal to the current time.

The deletion is performed using a JPQL `DELETE` query.

`@Transactional` ensures that the database modification is executed within a transaction.

This removes expired authentication codes so they can no longer be used for authentication.


---

## *AuthController.java*

`AuthController` is a REST Controller responsible for authentication, token management, user registration, password updates, and logout in the `spring-auth` service.

All endpoints use the `/auth` path.

It contains :

* `login()`,
* `refreshLogin()`,
* `register()`,
* `updatePassword()`,
* `logout()`,
* `redirectAfterLogin()`

### `login()`

Authenticates a user using their login and password.

It :

* verifies the credentials using `UserService`,
* creates an access token,
* creates a refresh token,
* stores the refresh token in the database,
* sends the refresh token as a secure HTTP-only cookie,
* returns the user information and access token.

The refresh token cookie uses `Secure`, `HttpOnly`, and `SameSite=Strict` settings.

### `refreshLogin()`

Refreshes the user's access token using the `refresh_token` cookie.

It validates the refresh token and checks that it matches the stored token.

It then creates a new access token and refresh token and sends the new refresh token as an HTTP-only cookie.

### `register()`

Creates a new user account.

The endpoint requires the `user:write` permission.

After registration, it creates access and refresh tokens and returns the new user's information with the access token.

The refresh token is also stored in the database and sent as a secure HTTP-only cookie.

### `updatePassword()`

Allows an authenticated user to change their password.

It retrieves the current user from Spring Security's `SecurityContext`.

The old password is verified and the new password is securely stored through `UserService`.

A localized confirmation message is returned after a successful update.

### `logout()`

Logs out the currently authenticated user.

It :

* invalidates the HTTP session if one exists,
* retrieves the current user,
* deletes all stored refresh tokens for that user,
* creates an expired refresh token,
* sends an expired refresh-token cookie,
* clears the Spring Security context.

This prevents the deleted refresh tokens from being reused.

### `redirectAfterLogin()`

Provides a confirmation endpoint for a successful login.

It requires authentication and returns a localized success message.

### Security

The controller uses `@PreAuthorize` to protect authenticated endpoints.

The refresh token is stored in a secure HTTP-only cookie, while the access token is returned in the response body.

The refresh token lifetime is configured using the `SECURITY_JWT_TOKEN_REFRESH_TOKEN_LIFETIME` environment property.

`MessageSource` and `LocaleContextHolder` are used to return localized messages according to the current request's language.


---

## *AuthExceptions.java*

`AuthExceptions` is a container class for authentication-related exceptions in the `spring-auth` service.

It contains :

* `InvalidCredentialsException`,
* `InvalidRefreshTokenException`,
* `AuthCodeNotFoundException`

Each exception extends `AppException` and implements `MessageKeyProvider`, allowing it to define an HTTP status and a localized message key.

### `InvalidCredentialsException`

Thrown when the provided login credentials are invalid.

It returns HTTP `401 UNAUTHORIZED` and uses the message key `error.authorisation.invalid.credentials`.

### `InvalidRefreshTokenException`

Thrown when a refresh token is invalid.

It returns HTTP `401 UNAUTHORIZED` and uses the message key `error.security.refresh.token.invalid`.

### `AuthCodeNotFoundException`

Thrown when an authentication code cannot be found.

It returns HTTP `404 NOT_FOUND` and uses the message key `error.authcode.not.found`.


---

## *AuthService.java*

**##** **AuthService.java**

`AuthService` is a service class responsible for generating, storing, validating, and consuming temporary authentication codes in the `spring-auth` service.

It uses :

* `UserAuthenticationProvider`,
* `UserService`,
* `AuthCodeRepository`

The authentication code lifetime is configured using the `SECURITY_AUTHENTICATION_CODES_LIFETIME` environment property.

It contains :

* `retrieveAndDeleteAuthCode()`,
* `generateAndStoreAuthCode()`

### `retrieveAndDeleteAuthCode()`

Retrieves and consumes an authentication code.

It :

* deletes expired authentication codes,
* retrieves the codes associated with the user's login,
* hashes the provided code,
* compares it with the stored hashes,
* deletes the matching code,
* retrieves the user,
* creates a JWT token.

If no matching code is found, an `AuthCodeNotFoundException` is thrown.

The code is deleted after successful use, preventing it from being reused.

### `generateAndStoreAuthCode()`

Generates and stores a new temporary authentication code.

It :

* deletes expired authentication codes,
* generates a random UUID,
* hashes the generated code,
* creates an `AuthCode` entity,
* stores the user's login, redirect URL, hash, and expiration date,
* saves the code in the database.

The raw code is returned to the caller, while only its hash is stored in the database.

The expiration time is calculated using the configured authentication-code lifetime.


---

## *CredentialsDto.java*

`CredentialsDto` is a Data Transfer Object used to transfer the user's login credentials during authentication.

It contains :

* `login`,
* `password`

The `login` contains the user's email address and must be a valid email format.

The `password` contains the user's password as a character array and must be between **8 and 72 characters** long.

`@NotBlank` ensures that the login is not empty.

`@Email` validates the login as an email address.

`@NotNull` ensures that a password is provided.

`@Size` validates the password length.

Because `CredentialsDto` is a Java `record`, it is immutable and automatically provides its constructor, accessors, `equals()`, `hashCode()`, and `toString()` methods.


---

## *OAuth2Controller.java*

`OAuth2Controller` is a REST Controller responsible for handling the Azure OAuth2 authentication flow in the `spring-auth` service.

All endpoints use the `/oauth2` path.

It contains :

* `initiateAzureLogin()`,
* `oauth2Success()`,
* `getToken()`

### `initiateAzureLogin()`

Starts the Azure OAuth2 login process.

It determines where the user should be redirected after authentication using :

1. the `redirectUrl` request parameter,
2. the `Referer` header,
3. the default `/auth/redirect-after-login` URL.

The selected URL is stored in the HTTP session.

The user is then redirected to Spring Security's Azure OAuth2 authorization endpoint.

### `oauth2Success()`

Handles the callback after a successful Azure OAuth2 authentication.

It :

* retrieves the authenticated Azure user,
* extracts the user's email, first name, and last name,
* creates or retrieves the corresponding local user,
* retrieves the redirect URL from the HTTP session,
* adds the Azure login type to the URL,
* generates a temporary authentication code,
* redirects the user back to the client application.

The authentication code is stored through `AuthService` and is later exchanged for JWT tokens.

If the OAuth2 authentication, user, or required email attribute is missing, an HTTP `401 UNAUTHORIZED` response is returned with a localized error message.

### `getToken()`

Exchanges a temporary authentication code for JWT tokens.

It :

* retrieves the user using the login from `AuthCodeDto`,
* validates and consumes the authentication code,
* creates a JWT access token,
* creates a refresh token,
* places the refresh token in a secure HTTP-only cookie,
* returns the user information and access token.

The access token is also included in the `Authorization` response header as a Bearer token.

### OAuth2 flow

The complete flow is :

1. The client starts the Azure login using `/oauth2/login/azure`.
2. `OAuth2Controller` redirects the user to Azure.
3. Azure authenticates the user and redirects back to `/oauth2/success`.
4. The controller creates or retrieves the local user.
5. A temporary authentication code is generated and sent to the client application.
6. The client sends the code to `/oauth2/token`.
7. The code is validated and deleted.
8. JWT access and refresh tokens are created and returned.

### Security

Temporary authentication codes are handled through `AuthService` and are deleted after successful use.

Refresh tokens are stored in secure HTTP-only cookies.

The controller uses `MessageSource` and `LocaleContextHolder` to return localized authentication error messages.


---

## *PasswordConfig.java*

`PasswordConfig` is a configuration component responsible for providing the password encoder used by the `spring-auth` service.

It uses the **BCrypt** algorithm to securely hash passwords before they are stored in the database and to verify passwords during authentication.

It contains :

* `passwordEncoder()`

### `passwordEncoder()`

Creates and provides a `BCryptPasswordEncoder` as a Spring `PasswordEncoder` bean.

This encoder is used throughout the application to hash passwords and verify provided passwords against their stored hashes.


---

## *PasswordNotReused.java*

`PasswordNotReused` is a custom validation annotation used to ensure that a user's new password is different from their old password.

It is applied at the class level of `PasswordUpdateDto`.

It contains :

* `message`,
* `groups`,
* `payload`,
* `PasswordNotReusedValidatorImpl`

### `PasswordNotReusedValidatorImpl`

`PasswordNotReusedValidatorImpl` contains the validation logic for `@PasswordNotReused`.

It checks that :

* the DTO is not null,
* both password fields are present,
* the old and new passwords are not identical.

If the passwords are identical, the validation fails and uses the `validation.password.not.reused` message.

### `areCharArraysEqual()`

Compares the old and new passwords character by character.

The comparison checks every character instead of stopping at the first difference.

Overall, `PasswordNotReused` prevents a user from changing their password to the exact same password they were already using.


---

## *PasswordUpdateDto.java*

`PasswordUpdateDto` is a Data Transfer Object used to transfer the information required to update a user's password.

It contains :

* `oldPassword`,
* `newPassword`

Both passwords are stored as character arrays (`char[]`).

The `oldPassword` is required to verify the user's current password.

The `newPassword` is required and must contain between **8 and 72 characters**.

`@PasswordNotReused` ensures that the new password is not identical to the old password.

`@NotNull` ensures that both passwords are provided.

`@Size` validates the length of the new password.


---

## *RefreshToken.java*

`RefreshToken` is a JPA Entity class used to store refresh tokens in the `spring-auth` database.

Refresh tokens allow users to obtain new access tokens without logging in again.

It contains :

* `id`,
* `tokenHash`,
* `userLogin`,
* `expiresAt`,
* `revoked`,
* `createdAt`

The `tokenHash` field stores a **hash of the refresh token** instead of the original token. This prevents the raw token from being exposed if the database is compromised.

The `userLogin` identifies the user who owns the refresh token.

The `expiresAt` field defines when the refresh token becomes invalid.

The `revoked` field indicates whether the token has been manually or automatically revoked. It defaults to `false`.

The `createdAt` field stores the time when the refresh token record was created.

A refresh token cannot be used to obtain new access tokens if it is expired or revoked.


---

## *RefreshTokenRepository.java*

`RefreshTokenRepository` is a repository interface used to access and manage `RefreshToken` entities in the `spring-auth` database.

It extends `JpaRepository<RefreshToken, Long>`, which provides standard CRUD operations.

It contains :

* `findByUserLoginAndRevokedFalse()`,
* `deleteByUserLogin()`

### `findByUserLoginAndRevokedFalse()`

Finds the active, non-revoked refresh token associated with a specific user login.

It returns an `Optional`, which is empty if no active token is found.

### `deleteByUserLogin()`

Deletes all refresh tokens associated with a specific user.

It can be used when a user logs out, changes their password, or when their refresh tokens need to be invalidated.

`@Modifying` indicates that the method modifies the database, while `@Transactional` ensures that the operation is executed within a transaction.

Spring Data JPA automatically provides the repository implementation.


---

## *SignUpDto.java*

`SignUpDto` is a Data Transfer Object used to transfer the information required to create a new user account in the `spring-auth` service.

It contains :

* `firstName`,
* `lastName`,
* `login`,
* `password`,
* `mainRole`

The `firstName` and `lastName` contain the user's name information. Both fields must not be blank and must contain valid letters, spaces, hyphens, or apostrophes.

The `login` contains the user's email address and must be a valid email format.

The `password` contains the user's password as a character array and must be between **8 and 72 characters** long.

The `mainRole` contains the user's requested main role.

`@NotBlank` ensures that required text fields are not empty.

`@Pattern` validates the format of the first and last names.

`@Email` validates the login as an email address.

`@NotNull` ensures that a password is provided.

`@Size` validates the password length.

Because `SignUpDto` is a Java `record`, it is immutable and automatically provides its constructor, accessors, `equals()`, `hashCode()`, and `toString()` methods.

---

## *TokenResponseDto.java*

`TokenResponseDto` is a Data Transfer Object used to return an access token after successful authentication or token refresh.

It contains :

* `accessToken`

The `accessToken` contains the JWT used by the client to authenticate API requests.

`@Data` from Lombok automatically generates getters, setters, `toString()`, `equals()`, and `hashCode()`.

`@AllArgsConstructor` generates a constructor containing all fields, while `@NoArgsConstructor` generates an empty constructor.



