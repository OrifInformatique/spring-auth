# TEST PACKAGE
TEST Documentation of Spring-Auth


## *Table of Contents*

* [LocaleConfig.java](#localeconfigjava)

  * [messageSource()](#messagesource)
  * [resolveMessageBasenames()](#resolvemessagebasenames)

* [TemplateApplicationTests.java](#templateapplicationtestsjava)

  * [@SpringBootTest](#springboottest)
  * [@ActiveProfiles("test")](#activeprofilestest)
  * [contextLoads()](#contextloads)

* [TestConfigurationDebug.java](#testconfigurationdebugjava)

  * [@TestPropertySource](#testpropertysource)
  * [debugConfiguration()](#debugconfiguration)

* [Auth](#auth)

  * [AuthCodeTest.java](#authcodetestjava)

    * [createAuthCode()](#createauthcode)
    * [getCodeByUserLogin()](#getcodebyuserlogin)
    * [getAuthCodeFromUserWithNoCode()](#getauthcodefromuserwithnocode)
    * [deleteExpiredCodes()](#deleteexpiredcodes)
    * [Test data](#test-data)

  * [AuthControllerIntegrationTest.java](#authcontrollerintegrationtestjava)

    * [Test configuration](#test-configuration)
    * [performRequest()](#performrequest)
    * [Test setup](#test-setup)
    * [Login tests](#login-tests)
    * [Registration tests](#registration-tests)
    * [Refresh-token tests](#refresh-token-tests)
    * [Password update tests](#password-update-tests)
    * [Logout tests](#logout-tests)
    * [OAuth2 tests](#oauth2-tests)
    * [REST documentation](#rest-documentation)
    * [Main dependencies](#main-dependencies)

  * [AuthServiceTest.java](#authservicetestjava)

    * [Test configuration](#test-configuration-1)
    * [setUp()](#setup)
    * [generateAndStoreAuthCode_Success()](#generateandstoreauthcodesuccess)
    * [retrieveAndDelete_Success()](#retrieveanddeletesuccess)
    * [Main dependencies](#main-dependencies-1)

  * [CredentialsDtoTest.java](#credentialsdtotestjava)

    * [Test configuration](#test-configuration-2)
    * [hasViolation()](#hasviolation)
    * [Validation tests](#validation-tests)
    * [Password length boundaries](#password-length-boundaries)
    * [credentialsDto_recordMethods_shouldWorkCorrectly()](#credentialsdtorecordmethodsshouldworkcorrectly)

  * [SignUpDtoTest.java](#signupdtotestjava)

    * [Test configuration](#test-configuration-3)
    * [hasViolation()](#hasviolation-1)
    * [Name validation](#name-validation)
    * [Login validation](#login-validation)
    * [Password validation](#password-validation)
    * [signUpDto_withMultipleViolations_shouldReturnAllViolations()](#signupdtowithmultipleviolationsshouldreturnallviolations)
    * [signUpDto_recordMethods_shouldWorkCorrectly()](#signupdtorecordmethodsshouldworkcorrectly)

* [Security](#security)

  * [CustomAccessDeniedHandlerTest.java](#customaccessdeniedhandlertestjava)

    * [Test configuration](#test-configuration-4)
    * [message()](#message)
    * [Access denied tests](#access-denied-tests)
    * [Response format tests](#response-format-tests)

  * [PermissionEnumTest.java](#permissionenumtestjava)

    * [Permission values](#permission-values)
    * [valueOf()](#valueof)
    * [toString()](#tostring)
    * [Permission naming convention](#permission-naming-convention)
    * [Enum comparison](#enum-comparison)
    * [containsPermission()](#containspermission)

  * [RoleEnumTest.java](#roleenumtestjava)

  * [RoleTest.java](#roletestjava)

    * [Role creation](#role-creation)
    * [Field tests](#field-tests)
    * [Role values](#role-values)
    * [Multiple properties](#multiple-properties)
    * [Description handling](#description-handling)

  * [UserAuthenticationEntryPointTest.java](#userauthenticationentrypointtestjava)

    * [Test configuration](#test-configuration-5)
    * [commence()](#commence)
    * [Authentication exception tests](#authentication-exception-tests)
    * [Null and empty messages](#null-and-empty-messages)
    * [Response format](#response-format)

  * [UserAuthenticationProviderTest.java](#userauthenticationprovidertestjava)

    * [Test configuration](#test-configuration-6)
    * [testCreateToken()](#testcreatetoken)
    * [testValidateTokenStrongly_ExistingUser()](#testvalidatetokenstronglyexistinguser)
    * [testValidateTokenStrongly_NewUser()](#testvalidatetokenstronglynewuser)
    * [testBuildAuthorities()](#testbuildauthorities)

* [User](#user)

  * [TestUserSeeder.java](#testuserseederjava)

    * [run()](#run)
    * [loadUserData()](#loaduserdata)
    * [Test users](#test-users)
    * [Seeding conditions](#seeding-conditions)

  * [UserControllerIntegrationTest.java](#usercontrollerintegrationtestjava)

    * [Test configuration](#test-configuration-7)
    * [performRequest()](#performrequest-1)
    * [message()](#message-1)
    * [Current user](#current-user)
    * [User listing](#user-listing)
    * [Deleted users](#deleted-users)
    * [User restoration](#user-restoration)
    * [Permanent deletion](#permanent-deletion)
    * [Manager role management](#manager-role-management)
    * [Manager role revocation](#manager-role-revocation)
    * [Admin role management](#admin-role-management)
    * [Admin role revocation](#admin-role-revocation)
    * [Admin downgrade](#admin-downgrade)
    * [User deletion](#user-deletion)
    * [Overall](#overall)

  * [UserDtoTest.java](#userdtotestjava)

    * [Default values](#default-values)
    * [Builder](#builder)
    * [toBuilder()](#tobuilder)
    * [Permission management](#permission-management)

  * [UserMapperTest.java](#usermappertestjava)

    * [Test configuration](#test-configuration-8)
    * [testToUserDto()](#testtouserdto)
    * [testSignUpToUser()](#testsignuptouser)
    * [testAuthoritiesToPermissions()](#testauthoritiestopermissions)
    * [testAuthoritiesToPermissionsWithNull()](#testauthoritiestopermissionswithnull)

  * [UserServiceTest.java](#userservicetestjava)

    * [Test configuration](#test-configuration-9)
    * [setUp()](#setup-1)
    * [Login tests](#login-tests-1)
    * [Registration tests](#registration-tests-1)
    * [Role management tests](#role-management-tests)
    * [User deletion tests](#user-deletion-tests)
    * [Mockito verification](#mockito-verification)

  * [UserTest.java](#usertestjava)

    * [testUserDetailsImplementation()](#testuserdetailsimplementation)
    * [testAuthoritiesWithRoles()](#testauthoritieswithroles)
    * [testBuilderWithAllFields()](#testbuilderwithallfields)
    * [testRoleManagement()](#testrolemanagement)

---

## *TemplateApplicationTests.java*

`TemplateApplicationTests` is a test class used to verify that the Spring Boot application can start correctly in the `spring-auth` service.

It uses :

* `@SpringBootTest`,
* `@ActiveProfiles("test")`,
* `@Test`.

### `@SpringBootTest`

Loads the complete Spring Boot application context for the test.

This checks that the application's configuration, beans, and dependencies can be initialized correctly.

### `@ActiveProfiles("test")`

Activates the `test` Spring profile while running the test.

This allows the application to use configuration specifically intended for testing.

### `contextLoads()`

```java
@Test
void contextLoads() {
}
```

This test does not contain any code because its purpose is simply to verify that the application context loads successfully.

If the Spring Boot context cannot start because of a configuration or dependency problem, the test fails automatically.

---

## *TestConfigurationDebug.java*

`TestConfigurationDebug` is a test class used to inspect the Spring Boot test configuration of the `spring-auth` service.

It loads the application using the `test` profile and prints several configuration values to help identify configuration problems.

It uses :

* `Environment`,
* `@SpringBootTest`,
* `@ActiveProfiles("test")`,
* `@TestPropertySource`,
* `@Autowired`.

### `@TestPropertySource`

```java
@TestPropertySource(locations = "classpath:application-test.properties")
```

Specifies the `application-test.properties` file as an additional source of configuration for the test.

### `debugConfiguration()`

This test retrieves and prints configuration values from Spring's `Environment`.

It displays :

* active profiles,
* database URL,
* database username,
* database password,
* Hibernate DDL configuration.

The values are retrieved using:

```java
env.getProperty(...)
```

The method is mainly used to debug and verify that the expected test configuration is being loaded.

If an error occurs, it prints the error message and stack trace before rethrowing the exception, causing the test to fail.


---

## *Auth*

  ### AuthCodeTest.java

`AuthCodeTest` is a test class used to verify the `AuthCodeRepository` functionality in the `spring-auth` service.

It uses a Spring Boot test context and accesses the authentication-code and user repositories directly.

It tests :

* creating and retrieving authentication codes,
* retrieving codes by user login,
* handling users without authentication codes,
* deleting expired authentication codes.

It uses :

* `AuthCodeRepository`,
* `UserRepository`,
* `@SpringBootTest`,
* `@Transactional`.

#### `createAuthCode()`

Tests that an authentication code can be created, saved to the database, and retrieved using its generated ID.

It verifies :

* user login,
* code,
* expiration date,
* redirect URL.

The expiration date is truncated to microseconds when comparing the stored and expected values.

#### `getCodeByUserLogin()`

Creates two authentication codes for the same user and retrieves them using `findByUserLogin()`.

It verifies that:

* the returned list is not empty,
* exactly two authentication codes are returned.

#### `getAuthCodeFromUserWithNoCode()`

Retrieves a test user and searches for authentication codes associated with their login.

It verifies that an empty list is returned when the user has no authentication codes.

#### `deleteExpiredCodes()`

Tests the removal of expired authentication codes.

It creates an authentication code with an expiration date in the past, then calls `deleteExpiredCodes()`.

It verifies that:

* the code existed before deletion,
* the expired code is no longer returned afterward.

`@Transactional` ensures that the test runs inside a database transaction.

#### Test data

The class uses predefined test values for:

* authentication code,
* user login,
* expiration date,
* redirect URL.

These values are used to create predictable data for the repository tests.


  ---

  ### AuthControllerIntegrationTest.java

`AuthControllerIntegrationTest` is an integration test class used to test the authentication and OAuth2 endpoints of the `spring-auth` service.

It loads the real Spring Boot application context and uses `MockMvc` to send HTTP requests to the controllers.

It tests :

* login,
* registration,
* token refresh,
* password updates,
* logout,
* Azure OAuth2 authentication,
* authentication-code exchange,
* validation errors,
* authentication and authorization errors,
* invalid input and malformed requests.

It also generates Spring REST Docs snippets for the tested requests and responses.

#### Test configuration

The class uses :

* `@SpringBootTest`,
* `@ActiveProfiles("test")`,
* `@AutoConfigureMockMvc`,
* `@AutoConfigureRestDocs`.

The `test` profile is used for the tests, while `MockMvc` allows HTTP requests to be tested without starting a real web server.

#### `performRequest()`

Two overloaded `performRequest()` methods are used to simplify HTTP endpoint tests.

They :

* select the HTTP method,
* add the request body,
* add a JWT Authorization header when provided,
* add a cookie when required,
* set the content type,
* set the request locale,
* execute the request,
* verify the expected HTTP status,
* generate a REST Docs snippet.

The second version additionally supports a `Cookie`, which is used for refresh-token requests.

#### Test setup

`setUp()` sets the current locale to French before each test.

`tearDown()` resets the locale after each test.

The `message()` method retrieves localized messages from the application's `MessageSource`.

#### Login tests

The login tests verify :

* successful authentication with valid credentials,
* missing login,
* missing password,
* invalid email format,
* empty request body,
* malformed JSON,
* invalid login input,
* invalid password,
* non-existent users,
* unsupported content types.

Successful login must return an access token and a refresh-token cookie.

Authentication failures return HTTP `401 UNAUTHORIZED`, while validation errors return HTTP `400 BAD_REQUEST`.

#### Registration tests

The registration tests verify :

* successful user registration,
* missing first name,
* missing last name,
* missing login,
* missing password,
* invalid email format,
* empty request body,
* malformed JSON,
* invalid name input,
* invalid login input,
* unsupported content types,
* duplicate login,
* insufficient permissions.

The successful registration test also verifies that the user is stored in the database and that the password is stored as a hash rather than plain text.

A successful registration returns HTTP `201 CREATED`.

#### Refresh-token tests

The refresh-token tests verify :

* refreshing tokens with a valid refresh token,
* missing authentication,
* invalid tokens,
* missing Authorization headers,
* requests without authentication.

A valid refresh token results in a new access token.

#### Password update tests

The password-update tests verify :

* successfully changing a password with valid authentication,
* missing request data,
* attempting to update the password without authentication.

The successful test checks that the API returns the localized password-update confirmation.

#### Logout tests

The logout tests verify :

* successful logout,
* logout without authentication,
* malformed access tokens,
* expired access tokens.

A successful logout returns a confirmation message and invalidates the user's stored refresh tokens.

#### OAuth2 tests

The OAuth2 tests verify the Azure authentication flow.

`login_oauth2_success()` simulates an OAuth2 login using Spring Security's `oauth2Login()` and verifies that the application redirects to `/auth/redirect-after-login` with an authentication code.

`get_token_with_authCode()` verifies that a valid authentication code can be exchanged for a token.

`getTokenWithAuthCode_withWronLogin_ShouldReturn404()` verifies that requesting a token for a user that does not exist returns HTTP `404 NOT_FOUND`.

`getTokenWithAuthCode_withWrongCode_ShouldReturn404()` verifies that an invalid authentication code returns HTTP `404 NOT_FOUND`.

#### REST documentation

Each request tested through `performRequest()` generates a REST Docs snippet in:

```text
target/generated-snippets/auth/
```

Requests and responses are formatted using `prettyPrint()` to make the generated documentation easier to read.

#### Main dependencies

The test uses several application components through dependency injection:

* `UserAuthenticationProvider` for JWT creation,
* `UserService` for user operations,
* `AuthService` for authentication codes,
* `UserRepository` for database verification,
* `RoleRepository` for test roles,
* `PasswordEncoder` for password verification,
* `UserMapper` for converting users to DTOs,
* `MessageSource` for localized messages.

Overall, `AuthControllerIntegrationTest` verifies the complete authentication flow using the actual Spring application context, database repositories, security components, and HTTP endpoints.


  ---

  ### AuthServiceTest.java


`AuthServiceTest` is an integration test class used to verify the authentication-code functionality of `AuthService` in the `spring-auth` service.

It uses the Spring Boot test context and accesses the real authentication service, repository, password encoder, and authentication provider.

It tests :

* generating and storing authentication codes,
* retrieving and deleting authentication codes,
* generating a JWT after a valid authentication code is consumed.

#### Test configuration

The class uses :

* `@SpringBootTest`,
* `@ActiveProfiles("test")`,
* `@BeforeEach`,
* `@Test`.

`@SpringBootTest` loads the Spring application context so that the real services and repositories can be tested.

`@ActiveProfiles("test")` activates the `test` profile.

#### `setUp()`

Before each test, `ReflectionTestUtils` changes the authentication-code lifetime to **10 minutes**.

This gives the tests a predictable lifetime for generated authentication codes.

#### `generateAndStoreAuthCode_Success()`

Tests that an authentication code can be successfully generated and stored.

It :

1. generates an authentication code for a test user,
2. stores it using `AuthService`,
3. retrieves the stored codes from `AuthCodeRepository`,
4. verifies that a code was stored,
5. verifies that the generated code is not empty.

#### `retrieveAndDelete_Success()`

Tests that a valid authentication code can be retrieved and consumed.

It :

1. generates and stores an authentication code,
2. retrieves it using `retrieveAndDeleteAuthCode()`,
3. verifies that a JWT is returned.

The method also verifies the complete authentication-code flow from generation to JWT creation.

#### Main dependencies

The test uses :

* `AuthService` for authentication-code operations,
* `AuthCodeRepository` to verify stored codes,
* `UserService` for user-related authentication operations,
* `PasswordEncoder` for password operations,
* `UserAuthenticationProvider` for JWT creation and validation.


  ---
### CredentialsDtoTest.java

`CredentialsDtoTest` is a test class used to verify the validation rules and basic functionality of `CredentialsDto` in the `spring-auth` service.

It uses Jakarta Bean Validation to check that login credentials follow the required constraints.

It tests :

* valid credentials,
* invalid email addresses,
* blank logins,
* null passwords,
* passwords that are too short,
* passwords that are too long,
* minimum and maximum password lengths,
* record accessor methods.

#### Test configuration

The class uses :

* `@SpringBootTest`,
* `@BeforeEach`,
* `@AfterEach`,
* `@Test`,
* `Validator`.

The `test` Spring Boot context is loaded to provide the application's `Validator`.

The locale is set to French before each test and reset afterward.

### `hasViolation()`

This helper method checks whether the validation results contain a specific constraint violation.

It checks :

* the field name,
* the validation annotation that caused the violation.

This is used to verify that the expected validation rule was triggered.

#### Validation tests

`credentialsDto_withValidData_shouldPassValidation()` verifies that a valid email and password produce no validation errors.

`credentialsDto_withInvalidEmail_shouldFailValidation()` verifies that an incorrectly formatted email produces an `@Email` violation on `login`.

`credentialsDto_withBlankLogin_shouldFailValidation()` verifies that an empty login produces a `@NotBlank` violation.

`credentialsDto_withNullPassword_shouldFailValidation()` verifies that a missing password produces a `@NotNull` violation.

`credentialsDto_withPasswordTooShort_shouldFailValidation()` verifies that a password shorter than **8 characters** produces a `@Size` violation.

`credentialsDto_withPasswordTooLong_shouldFailValidation()` verifies that a password longer than **72 characters** produces a `@Size` violation.

#### Password length boundaries

`credentialsDto_withMinimumPasswordLength_shouldPassValidation()` verifies that a password containing exactly **8 characters** is accepted.

`credentialsDto_withMaximumPasswordLength_shouldPassValidation()` verifies that a password containing exactly **72 characters** is accepted.

These tests verify both boundaries of the password length constraint.

#### `credentialsDto_recordMethods_shouldWorkCorrectly()`

Verifies that the `CredentialsDto` record accessors return the values provided to its constructor.

It checks :

* `login()` returns the original login,
* `password()` returns the original password character array.

Overall, `CredentialsDtoTest` verifies that invalid authentication data is rejected while valid credentials and boundary values are accepted.


  ---

### SignUpDtoTest.java

`SignUpDtoTest` is a test class used to verify the validation rules and basic functionality of `SignUpDto` in the `spring-auth` service.

It uses Jakarta Bean Validation to check that registration data follows the required constraints.

It tests :

* valid registration data,
* names with hyphens, apostrophes, spaces, and Unicode characters,
* blank names and login,
* invalid characters in names,
* invalid email addresses,
* null passwords,
* password length limits,
* multiple validation errors,
* record accessor methods.

#### Test configuration

The class uses :

* `@SpringBootTest`,
* `@BeforeEach`,
* `@AfterEach`,
* `@Test`,
* `Validator`.

The Spring Boot context provides the application's `Validator`.

The locale is set to French before each test and reset afterward.

### `hasViolation()`

This helper method checks whether a validation result contains a violation for a specific field and validation annotation.

It is used to verify that the expected constraint caused the validation error.

#### Name validation

The tests verify that valid names can contain :

* hyphens,
* apostrophes,
* spaces,
* Unicode characters such as accented letters.

They also verify that :

* blank names are rejected,
* names containing invalid characters such as numbers or `@` are rejected,
* names containing only spaces are rejected.

#### Login validation

The login is validated as an email address.

The tests verify that :

* a valid email passes validation,
* an invalid email produces an `@Email` violation,
* a blank login produces an `@NotBlank` violation.

#### Password validation

The password must be provided and must contain between **8 and 72 characters**.

The tests verify :

* a null password produces a `@NotNull` violation,
* passwords shorter than 8 characters are rejected,
* passwords longer than 72 characters are rejected,
* exactly 8 characters are accepted,
* exactly 72 characters are accepted.

#### `signUpDto_withMultipleViolations_shouldReturnAllViolations()`

Tests that multiple invalid fields are detected at the same time.

It provides an invalid first name, last name, login, and password and verifies that at least three validation violations are returned.

#### `signUpDto_recordMethods_shouldWorkCorrectly()`

Verifies that the `SignUpDto` record accessors return the values provided to its constructor.

It checks :

* `firstName()`,
* `lastName()`,
* `login()`,
* `password()`.

Overall, `SignUpDtoTest` verifies that registration data is correctly validated before it can be used to create a user account.


  ---

  ## *Security*

  ### CustomAccessDeniedHandlerTest.java

`CustomAccessDeniedHandlerTest` is a test class used to verify the behavior of `CustomAccessDeniedHandler` in the `spring-auth` service.

It tests how the handler responds when an authenticated user is denied access to a resource.

It verifies :

* HTTP `403 FORBIDDEN` responses,
* JSON response formatting,
* the default localized error message,
* the `Content-Type` header,
* handling of null exceptions and messages.

#### Test configuration

The class uses :

* `@SpringBootTest`,
* `@BeforeEach`,
* `@AfterEach`,
* `@Test`,
* `MockHttpServletRequest`,
* `MockHttpServletResponse`,
* `ObjectMapper`,
* `MessageSource`.

The handler is created before each test using the application's `MessageSource`.

The locale context is reset after each test.

### `message()`

Retrieves a localized message from the application's `MessageSource`.

It is used to compare the handler's response with the expected `error.security.access.denied` message.

#### Access denied tests

`handle_withAccessDeniedException_shouldReturn403WithMessage()` verifies that an `AccessDeniedException` produces :

* HTTP `403`,
* `application/json` content type,
* the localized access-denied message.

`handle_withNullException_shouldReturn403WithDefaultMessage()` verifies that the handler still returns the default access-denied message when the exception is `null`.

`handle_withExceptionWithNullMessage_shouldReturn403WithDefaultMessage()` verifies that an exception with no message also uses the default localized message.

#### Response format tests

`handle_shouldReturnValidJsonStructure()` verifies that the response body contains valid JSON with a `message` field containing the expected error message.

`handle_shouldSetCorrectContentType()` verifies that the response uses the `application/json` content type.

`handle_shouldSetCorrectStatusCode()` verifies that the response status is `403 FORBIDDEN`.

Overall, `CustomAccessDeniedHandlerTest` verifies that access-denied errors are consistently returned as localized JSON responses with the correct HTTP status.


  ---

  ### PermissionEnumTest.java

`PermissionEnumTest` is a test class used to verify the permissions defined by `PermissionEnum` in the `spring-auth` service.

It tests :

* the four user permissions,
* their string values,
* enum lookup,
* enum string representation,
* permission naming format,
* standard enum comparison behavior.

#### Permission values

The tests verify that the four permissions have the correct values :

* `USER_READ` → `user:read`
* `USER_WRITE` → `user:write`
* `USER_UPDATE` → `user:update`
* `USER_DELETE` → `user:delete`

It also verifies that all four permissions exist and that `getPermission()` never returns `null`.

#### `valueOf()`

`permissionEnum_valueOf_shouldReturnCorrectEnum()` verifies that the standard `valueOf()` method correctly retrieves each enum constant from its name.

`permissionEnum_valueOf_withInvalidValue_shouldThrowException()` verifies that an invalid enum name throws an `IllegalArgumentException`.

#### `toString()`

`permissionEnum_toString_shouldReturnEnumName()` verifies that `toString()` returns the enum constant name, such as `USER_READ`, rather than its permission value.

#### Permission naming convention

`permissionEnum_permissions_shouldFollowNamingConvention()` checks that every permission follows the `resource:action` format and starts with `user:`.

For example:

```text
user:read
user:update
```

#### Enum comparison

`permissionEnum_shouldBeComparable()` verifies that enum constants can be compared using their declaration order through `compareTo()`.

#### `containsPermission()`

This helper method checks whether a specific permission is present in the array returned by `PermissionEnum.values()`.

Overall, `PermissionEnumTest` verifies that the application's user permissions are correctly defined and follow the expected format.


  ---

  ### RoleEnumTest.java

`PermissionEnumTest` is a test class used to verify the permissions defined by `PermissionEnum` in the `spring-auth` service.

It tests :

* the four user permissions,
* their string values,
* enum lookup,
* enum string representation,
* permission naming format,
* enum comparison behavior.

#### Permission values

The tests verify that the four permissions have the correct values :

* `USER_READ` → `user:read`
* `USER_WRITE` → `user:write`
* `USER_UPDATE` → `user:update`
* `USER_DELETE` → `user:delete`

It also verifies that all four permissions exist and that `getPermission()` returns a non-null value for each permission.

#### `valueOf()`

`permissionEnum_valueOf_shouldReturnCorrectEnum()` verifies that `valueOf()` correctly retrieves each enum constant from its name.

`permissionEnum_valueOf_withInvalidValue_shouldThrowException()` verifies that an invalid enum name throws an `IllegalArgumentException`.

#### `toString()`

`permissionEnum_toString_shouldReturnEnumName()` verifies that `toString()` returns the enum constant name, such as `USER_READ`, rather than the permission string.

#### Permission naming convention

`permissionEnum_permissions_shouldFollowNamingConvention()` checks that every permission follows the `resource:action` format and starts with `user:`.

For example :

```text
user:read
user:update
```

#### Enum comparison

`permissionEnum_shouldBeComparable()` verifies that enum constants can be compared using `compareTo()` based on their declaration order.

#### `containsPermission()`

This helper method checks whether a specific permission is present in the array returned by `PermissionEnum.values()`.

Overall, `PermissionEnumTest` verifies that the application's user permissions are correctly defined and follow the expected format.


  ---

  ### RoleTest.java

`RoleTest` is a test class used to verify the basic functionality of the `Role` entity in the `spring-auth` service.

It tests :

* creating a `Role`,
* setting and retrieving its fields,
* using all `RoleEnum` values,
* handling different description values.

#### Role creation

`role_shouldCreateWithDefaultConstructor()` verifies that a `Role` can be created using its default constructor.

#### Field tests

The tests verify that the main `Role` fields can be correctly set and retrieved :

* `id`,
* `name`,
* `description`,
* `createdAt`,
* `updatedAt`.

The values returned by the getters are compared with the values provided to the setters.

#### Role values

`role_shouldAllowAllRoleEnumValues()` verifies that the `Role` entity accepts all defined `RoleEnum` values :

* `USER`,
* `MANAGER`,
* `ADMIN`.

#### Multiple properties

`role_shouldHandleMultipleProperties()` verifies that several fields can be set on the same `Role` and that all values are correctly retrieved.

#### Description handling

The tests verify that the `description` field can contain :

* `null`,
* an empty string,
* a long string of 500 characters.

Overall, `RoleTest` verifies that the `Role` entity correctly stores and retrieves its properties and supports the different role values used by the authentication system.


  ---

  ### UserAuthenticationEntryPointTest.java

`UserAuthenticationEntryPointTest` is a test class used to verify the behavior of `UserAuthenticationEntryPoint` in the `spring-auth` service.

It tests :

* HTTP `401 UNAUTHORIZED` responses,
* JSON error responses,
* localized authentication error messages,
* handling of different `AuthenticationException` types,
* handling of null and empty exception messages.

#### Test configuration

The class uses :

* `@SpringBootTest`,
* `@BeforeEach`,
* `@AfterEach`,
* `@Test`,
* `MockHttpServletRequest`,
* `MockHttpServletResponse`,
* `ObjectMapper`,
* `MessageSource`.

The `MessageSource` is injected from the Spring application context.

Before each test, a new `UserAuthenticationEntryPoint`, request, response, and `ObjectMapper` are created.

The locale context is reset after each test.

#### `commence()`

The tests call the `commence()` method of `UserAuthenticationEntryPoint` and verify the generated HTTP response.

Authentication exceptions normally result in :

* HTTP `401 UNAUTHORIZED`,
* `application/json` content type,
* an `ErrorDto` containing a localized error message.

#### Authentication exception tests

`commence_withAuthenticationException_shouldReturn401WithMessage()` verifies that a `BadCredentialsException` returns the localized invalid-or-missing-token message.

`commence_withInsufficientAuthenticationException_shouldReturn401()` verifies the same behavior for an `InsufficientAuthenticationException`.

#### Null and empty messages

`commence_withNullException_shouldReturn401WithDefaultMessage()` verifies that a null exception uses the default authentication failure message.

`commence_withExceptionWithNullMessage_shouldReturn401WithDefaultMessage()` verifies that an exception with a null message uses the invalid-or-missing-token message.

`commence_shouldHandleEmptyExceptionMessage()` verifies the same behavior when the exception message is empty.

#### Response format

`commence_shouldReturnValidJsonStructure()` verifies that the response contains valid JSON with a `message` field containing the expected localized message.

`commence_shouldSetCorrectContentType()` verifies that the response uses `application/json`.

`commence_shouldSetCorrectStatusCode()` verifies that the response status is `401`.

Overall, `UserAuthenticationEntryPointTest` verifies that authentication failures are consistently returned as HTTP `401` JSON responses with the appropriate localized error message.


  ---

  ### UserAuthenticationProviderTest.java

`UserAuthenticationProviderTest` is a unit test class used to verify the JWT and authentication functionality of `UserAuthenticationProvider` in the `spring-auth` service.

It uses Mockito to mock `UserService` and tests :

* JWT token creation,
* strong JWT validation,
* handling of existing users,
* creation of new users,
* authority generation.

#### Test configuration

The class uses :

* `@ExtendWith(MockitoExtension.class)`,
* `@Mock`,
* `@BeforeEach`,
* `@Test`.

`UserService` is mocked so that the tests can focus on `UserAuthenticationProvider` without accessing the database.

Before each test, reflection is used to configure :

* the access-token secret key,
* the refresh-token secret key,
* the access-token lifetime of 10 minutes,
* the refresh-token lifetime of 30 days.

#### `testCreateToken()`

Tests that a JWT can be successfully created from a `UserDto`.

It verifies that :

* the token is not null,
* the token contains three parts, as expected for a JWT.

#### `testValidateTokenStrongly_ExistingUser()`

Tests strong validation of a JWT when the user already exists.

It :

1. creates a JWT,
2. configures the mocked `UserService` to return the user,
3. validates the token,
4. verifies that an `Authentication` object is created,
5. verifies that the authenticated principal contains the correct login.

It also verifies that `UserService.findByLogin()` was called.

#### `testValidateTokenStrongly_NewUser()`

Tests strong validation when the user does not exist locally.

The mocked `UserService` throws `UserNotFoundException`, simulating a user that is not yet registered.

The test then verifies that `getOrCreateAzureUser()` is called to create or retrieve the user and that authentication is successfully created.

#### `testBuildAuthorities()`

Tests the private `buildAuthorities()` method using reflection.

It verifies that the generated authorities include :

* the main role with the `ROLE_` prefix,
* the user's permissions.

For example :

```text
USER
↓
ROLE_USER
```

and :

```text
user:read
```

are included as Spring Security authorities.

Overall, `UserAuthenticationProviderTest` verifies the main JWT and authentication operations used by the `spring-auth` security system.


  ---

  ## *User*

  ### TestUserSeeder.java

`TestUserSeeder` is a seeder class used to create predefined users for testing in the `spring-auth` service.

It implements `CommandLineRunner`, so its `run()` method is executed when the application starts.

The seeder only runs with the `test` Spring profile.

It contains :

* `userRepository`,
* `passwordEncoder`,
* `roleRepository`,
* `run()`,
* `loadUserData()`.

#### `run()`

Starts the user seeding process by calling `loadUserData()`.

It also logs messages when the seeding starts and finishes.

#### `loadUserData()`

Creates test users only when the users table is empty.

Before creating the users, it retrieves the required roles :

* `USER`,
* `MANAGER`,
* `ADMIN`.

If one of these roles does not exist, a `RuntimeException` is thrown.

#### Test users

The seeder creates four predefined users :

* `test.user@test.com` → `USER`,
* `test.manager@test.com` → `MANAGER`,
* `test.admin@test.com` → `ADMIN`,
* `test.admin2@test.com` → `ADMIN`.

Each user is created using `User.builder()`.

Their passwords are encoded using `PasswordEncoder` before being stored.

The users are then saved together using `userRepository.saveAll()`.

#### Seeding conditions

`@Profile("test")` ensures that this seeder is only active when the `test` profile is enabled.

`@Order(2)` controls the execution order of the seeder so that it can run after seeders with a lower order, such as the role seeder.

If the users table already contains data, the seeder skips user creation and logs a message.

Overall, `TestUserSeeder` provides predefined users with different roles so that authentication and authorization can be tested with the `test` profile.


  ---

  ### UserControllerIntegrationTest.java

`UserControllerIntegrationTest` is an integration test class used to verify the user-management endpoints of the `spring-auth` service.

It uses the real Spring Boot application context and `MockMvc` to send HTTP requests to `UserController`.

It tests :

* retrieving users,
* authentication failures,
* JWT validation,
* user deletion and restoration,
* role management,
* authorization checks,
* user-not-found errors,
* REST Docs generation.

#### Test configuration

The class uses :

* `@SpringBootTest`,
* `@AutoConfigureMockMvc`,
* `@AutoConfigureRestDocs`,
* `@Transactional`,
* `@BeforeEach`,
* `@AfterEach`.

The `test` requests use the real application configuration and services.

`MockMvc` is used to perform HTTP requests without starting a separate web server.

The locale is set to French before each test and reset afterward.

### `performRequest()`

This helper method centralizes the execution of HTTP requests.

It :

* selects the HTTP method,
* sets the request body when provided,
* adds a JWT `Authorization` header when provided,
* sets the content type,
* sets the current locale,
* verifies the expected HTTP status,
* performs additional assertions when provided,
* generates a REST Docs snippet.

The generated documentation is stored under :

```text
target/generated-snippets/users/
```

### `message()`

Retrieves a localized message from the application's `MessageSource`.

It is used to compare API responses with the expected French error or success messages.

---

#### Current user

`me_withRealData_shouldReturnSuccess()` verifies that an authenticated user can retrieve their own information through `GET /users/me`.

It checks the user's :

* first name,
* last name,
* login,
* main role.

The authentication is performed using a JWT created by `UserAuthenticationProvider`.

`me_missingAuthorizationHeader_shouldReturnUnauthorized()` verifies that the endpoint returns `401` when no authentication token is provided.

`me_withMalformedToken_shouldReturnUnauthorized()` verifies that a malformed JWT is rejected.

`me_withExpiredToken_shouldReturnUnauthorized()` verifies that an expired JWT is rejected with the appropriate localized error message.

#### User listing

`all_withRealData_shouldReturnSuccess()` verifies that `GET /users/` returns the four active users created by the test seeder.

It parses the JSON response and verifies that all expected test-user logins are present.

`all_missingAuthorizationHeader_shouldReturnUnauthorized()` verifies that authentication is required.

`all_withMalformedToken_shouldReturnUnauthorized()` verifies that malformed JWTs are rejected.

`all_withExpiredToken_shouldReturnUnauthorized()` verifies that expired JWTs are rejected.

#### Deleted users

`allWithDeleted_withRealData_shouldReturnSuccess()` verifies that administrators can retrieve users including soft-deleted users through `GET /users/all-with-deleted`.

`deleted_withRealData_shouldReturnSuccess()` verifies that `GET /users/deleted` returns a list of deleted users.

#### User restoration

`restoreDeletedUser_withRealData_shouldReturnSuccess()` first soft-deletes a user and then restores the same user.

It verifies that :

* the deletion succeeds,
* restoration succeeds,
* the response contains the localized restoration message.

#### Permanent deletion

`deletePermanent_withRealData_shouldReturnSuccess()` verifies that an administrator can permanently delete a user.

It checks :

* HTTP `200`,
* the permanent-deletion message,
* the login of the deleted user.

Unlike soft deletion, the user is permanently removed from the database.

---

#### Manager role management

`promoteToManager_withRealData_shouldReturnSuccess()` verifies that an administrator can promote a user to `MANAGER`.

It checks the success message and verifies that the user's role is changed in the database.

`promoteToManager_missingAuthorizationHeader_shouldReturnUnauthorized()` verifies that authentication is required.

`promoteToManager_withMalformedToken_shouldReturnUnauthorized()` verifies that malformed JWTs are rejected.

`promoteToManager_asNonAdmin_shouldReturnForbidden()` verifies that a regular user cannot perform the promotion.

`promoteToManager_userNotFound_shouldReturnNotFound()` verifies that promoting a non-existent user returns `404`.

`promoteToManager_userAlreadyManager_shouldReturnConflict()` verifies that promoting an existing manager returns `409`.

`promoteToManager_userAlreadyAdmin_shouldReturnConflict()` verifies that attempting to promote an administrator to manager returns `409`.

#### Manager role revocation

`revokeManagerRole_withRealData_shouldReturnSuccess()` verifies that an administrator can revoke a manager role and change the user's role to `USER`.

It verifies the success message and the updated role.

The other revoke-manager tests verify :

* missing authentication returns `401`,
* malformed JWTs return `401`,
* non-admin users receive `403`,
* a non-existent user returns `404`.

---

#### Admin role management

`promoteToAdmin_withRealData_shouldReturnSuccess()` verifies that an administrator can promote a manager to `ADMIN`.

It checks the success message and verifies the updated role.

The other promotion tests verify :

* missing authentication returns `401`,
* malformed JWTs return `401`,
* non-admin users receive `403`,
* a non-existent user returns `404`,
* promoting an existing administrator returns `409`.

#### Admin role revocation

`revokeAdminRole_withRealData_shouldReturnSuccess()` verifies that an administrator can revoke another administrator's role and change it to `USER`.

The other tests verify :

* missing authentication returns `401`,
* malformed JWTs return `401`,
* non-admin users receive `403`,
* a non-existent user returns `404`.

#### Admin downgrade

`downgradeAdminRole_withRealData_shouldReturnSuccess()` verifies that an administrator can change another administrator's role to `MANAGER`.

It checks the success message and verifies the updated role.

The authentication and malformed-token cases are also tested, with missing authentication returning `401` and malformed tokens returning `401`.

---

#### User deletion

`deleteUser_withRealData_shouldReturnSuccess()` verifies that an administrator can soft-delete a user.

It checks :

* HTTP `200`,
* the deletion message,
* the deleted user's login,
* the user's `deleted` field in the database.

The user remains in the database but is marked as deleted.

`deleteUser_missingAuthorizationHeader_shouldReturnUnauthorized()` verifies that deletion requires authentication.

`deleteUser_withMalformedToken_shouldReturnUnauthorized()` verifies that a malformed JWT cannot be used to delete a user.

#### Overall

`UserControllerIntegrationTest` verifies the main user-management functionality of the `spring-auth` service through HTTP requests.

It covers both successful operations and error cases, including authentication, authorization, role changes, soft deletion, permanent deletion, restoration, and localized responses.


  ---

  ### UserDtoTest.java

`UserDtoTest` is a test class used to verify the functionality of `UserDto` in the `spring-auth` service.

It tests :

* default values,
* the builder,
* `toBuilder()`,
* field access,
* permission management.

#### Default values

`testDefaultValues()` verifies that a `UserDto` created without specifying values has :

* `mainRole` set to `USER`,
* an initialized permissions list,
* an empty permissions list.

#### Builder

`testBuilderWithAllFields()` verifies that the builder correctly sets all `UserDto` fields.

It checks :

* `id`,
* `firstName`,
* `lastName`,
* `login`,
* `token`,
* `mainRole`,
* `permissions`.

### `toBuilder()`

`testToBuilder()` verifies that an existing `UserDto` can be used to create a new instance with modified values.

It changes the first name while keeping the other tested fields unchanged.

#### Permission management

`testPermissionManagement()` verifies that permissions can be added to the `UserDto` permissions list.

It checks that the list contains the added permissions and has the expected size.

Overall, `UserDtoTest` verifies the default initialization, builder functionality, field handling, and permission management of `UserDto`.


  ---

  ### UserMapperTest.java

`UserMapperTest` is a test class used to verify the mapping functionality of `UserMapper` in the `spring-auth` service.

It tests the conversion between `User`, `UserDto`, and `SignUpDto`, as well as the conversion of Spring Security authorities into permission strings.

It tests :

* converting `User` → `UserDto`,
* converting `SignUpDto` → `User`,
* converting authorities → permissions,
* handling null authorities.

#### Test configuration

The class uses :

* `@SpringBootTest`,
* `@ActiveProfiles("test")`,
* `@Configuration`,
* `@Autowired`.

The internal `TestConfig` provides the `UserMapper` bean using the MapStruct-generated mapper instance.

The mapper is then injected into the test using `@Autowired`.

### `testToUserDto()`

Tests the conversion of a `User` entity into a `UserDto`.

It verifies that :

* the ID is mapped,
* the first name is mapped,
* the last name is mapped,
* the login is mapped,
* the main role is converted to its name,
* the permissions list is initialized.

The test uses a `MANAGER` role and verifies that the resulting DTO contains `"MANAGER"` as its main role.

### `testSignUpToUser()`

Tests the conversion of a `SignUpDto` into a `User`.

It verifies that :

* the first name is mapped,
* the last name is mapped,
* the login is mapped,
* the password is not mapped,
* the main role is not initialized by the mapper.

The password is expected to be `null` because `UserMapper` ignores the password during this mapping.

### `testAuthoritiesToPermissions()`

Tests the conversion of Spring Security `GrantedAuthority` objects into permission strings.

It verifies that three authorities :

* `READ`,
* `WRITE`,
* `DELETE`

are converted into a list containing the same permission strings.

It also verifies that the number of permissions is preserved.

### `testAuthoritiesToPermissionsWithNull()`

Tests the behavior when the authority collection is `null`.

It verifies that `authoritiesToPermissions()` returns `null` instead of throwing an exception.

Overall, `UserMapperTest` verifies that `UserMapper` correctly converts user data between the application's entities and DTOs while handling roles, permissions, and ignored fields.


  ---

  ### UserServceTest.java

`UserServiceTest` is a unit test class used to verify the business logic of `UserService` in the `spring-auth` service.

It uses Mockito to test the service without connecting to a real database or loading the Spring application context.

It tests :

* user login,
* user registration,
* role promotion,
* user deletion,
* authorization checks,
* error handling.

#### Test configuration

The class uses :

* `@ExtendWith(MockitoExtension.class)`,
* `@Mock`,
* `@InjectMocks`,
* `@BeforeEach`,
* `@Test`.

Mockito creates mock versions of the repositories, password encoder, mapper, and Spring Security context.

`@InjectMocks` creates the `UserService` and injects the mocked dependencies into it.

#### `setUp()`

Before each test, the mocked `SecurityContext` is placed into `SecurityContextHolder`.

This allows the deletion tests to simulate an authenticated user.

#### Login tests

`login_Successful_ReturnsUserDto()` verifies that a user can log in with valid credentials.

It checks that :

* the user is found by login,
* the password is verified,
* the user is converted into a `UserDto`,
* the expected DTO is returned.

`login_UserNotFound_ThrowsInvalidCredentialsException()` verifies that an unknown login produces an `InvalidCredentialsException`.

`login_InvalidPassword_ThrowsInvalidCredentialsException()` verifies that an incorrect password also produces an `InvalidCredentialsException`.

Both invalid login and invalid password use the same exception.

#### Registration tests

`register_Successful_ReturnsUserDto()` verifies that a new user can be registered.

It checks that :

* the login does not already exist,
* the password is encoded,
* the `USER` role is retrieved,
* the user is saved,
* the resulting user is converted into a `UserDto`.

`register_LoginExists_ThrowsUserAlreadyExistsException()` verifies that registration fails when the login already exists.

The exception also contains the existing login.

#### Role management tests

`promoteToManager_Successful_ReturnsUserDto()` verifies that a user can be promoted from `USER` to `MANAGER`.

It checks that the manager role is retrieved, the user is saved, and the updated DTO is returned.

`promoteToManager_UserNotFound_ThrowsRuntimeException()` verifies that promoting a user that does not exist throws `UserNotFoundException`.

`promoteToManager_AlreadyManager_ThrowsRuntimeException()` verifies that promoting a user who is already a manager throws `UserAlreadyManagerException`.

#### User deletion tests

`deleteUser_Successful_DeletesUser()` verifies that an authenticated manager can delete a regular user.

It mocks the security context to provide the authenticated manager and verifies that the target user is deleted through the repository.

`deleteUser_Unauthorized_ThrowsRuntimeException()` verifies that a regular user cannot delete a manager.

It checks that `UserHasLowerRightsException` is thrown and contains the authenticated user's login.

#### Mockito verification

The tests use Mockito's `when()` to configure mock behavior.

For example, repository lookups can be configured to return a specific user or an empty `Optional`.

`verify()` is used to check that important operations were actually performed, such as :

* finding a user,
* checking a password,
* retrieving a role,
* saving a user,
* deleting a user.

Overall, `UserServiceTest` verifies the main authentication, registration, role management, and authorization logic of `UserService` without requiring a real database.


  ---

  ### UserTest.java

`UserTest` is a test class used to verify the functionality of the `User` entity in the `spring-auth` service.

It tests :

* Spring Security `UserDetails` methods,
* role and authority management,
* the builder pattern,
* entity field access,
* main role management.

#### `testUserDetailsImplementation()`

Tests the implementation of Spring Security's `UserDetails` interface.

It verifies that :

* `getUsername()` returns the user's login,
* `getPassword()` returns the user's password,
* the account is not expired,
* the account is not locked,
* the credentials are not expired,
* the account is enabled.

#### `testAuthoritiesWithRoles()`

Tests the creation of Spring Security authorities from the user's main role.

It creates a `MANAGER` role and verifies that its granted authorities contain:

```text
ROLE_MANAGER
```

This confirms that the role is correctly converted into a Spring Security authority.

#### `testBuilderWithAllFields()`

Tests the Lombok builder used to create a `User`.

It sets all main user fields and verifies that the values can be retrieved correctly.

It checks :

* `id`,
* `firstName`,
* `lastName`,
* `login`,
* `password`,
* `createdAt`,
* `updatedAt`,
* `mainRole`.

#### `testRoleManagement()`

Tests setting and retrieving the user's main role.

It assigns a `USER` role to a `User` and verifies that the same role can be retrieved using `getMainRole()`.

Overall, `UserTest` verifies the basic functionality of the `User` entity, including its Spring Security integration, role handling, builder, and fields.

