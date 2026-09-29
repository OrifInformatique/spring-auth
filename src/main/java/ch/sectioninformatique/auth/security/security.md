# SECURITY PACKAGE
SECURITY Documentation of Spring-Auth


## *Table of Contents*

* [CorsConfigurationValidator.java](#corsconfigurationvalidatorjava)
* [CustomAccessDeniedHandler.java](#customaccessdeniedhandlerjava)
  * [handle()](#handle)
  * [Internationalization](#internationalization)
  * [Error response](#error-response)

* [JwtAuthFilter.java](#jwtauthfilterjava)

  * [doFilterInternal()](#dofilterinternal)
  * [JWT errors](#jwt-errors)
  * [Unexpected errors](#unexpected-errors)
  * [Internationalization](#internationalization-1)
  * [Security context](#security-context)

* [PermissionEnum.java](#permissionenumjava)

* [Role.java](#rolejava)

  * [Users relationship](#users-relationship)

* [RoleEnum.java](#roleenumjava)

  * [Roles and permissions](#roles-and-permissions)
  * [getGrantedAuthorities()](#getgrantedauthorities)

* [RoleRepository.java](#rolerepositoryjava)

  * [findByName()](#findbyname)

* [RoleSeeder.java](#roleseederjava)

  * [loadRoles()](#loadroles)

* [SecurityConfig.java](#securityconfigjava)

  * [securityFilterChain()](#securityfilterchain)
  * [CORS](#cors)
  * [OAuth2](#oauth2)
  * [Endpoint authorization](#endpoint-authorization)
  * [oauth2UserService()](#oauth2userservice)
  * [isDevelopmentOrTest()](#isdevelopmentortest)

* [SecurityExceptions.java](#securityexceptionsjava)

  * [RoleNotFoundException](#rolenotfoundexception)
  * [TokenNotFromTrustedTenantException](#tokennotfromtrustedtenantexception)
  * [MissingJwtClaimException](#missingjwtclaimexception)
  * [UnauthorizedActionException](#unauthorizedactionexception)
  * [UserHasLowerRightsException](#userhaslowerrightsexception)
  * [HashAlgorithmUnavailableException](#hashalgorithmunavailableexception)
  * [CorsConfigurationException](#corsconfigurationexception)

* [UserAuthenticationEntryPoint.java](#userauthenticationentrypointjava)

  * [commence()](#commence)
  * [Internationalization](#internationalization-2)
  * [Error response](#error-response)

* [UserAuthenticationProvider.java](#userauthenticationproviderjava)

  * [Token configuration](#token-configuration)
  * [init()](#init)
  * [createToken()](#createtoken)
  * [createRefreshToken()](#createrefreshtoken)
  * [createExpiredRefreshToken()](#createexpiredrefreshtoken)
  * [buildAuthorities()](#buildauthorities)
  * [validateToken()](#validatetoken)
  * [validateTokenStrongly()](#validatetokenstrongly)
  * [validateRefreshToken()](#validaterefreshtoken)
  * [Authentication](#authentication)

---

## *CorsConfigurationValidator.java*

`CorsConfigurationValidator` is a configuration class used to validate the application's CORS settings when the `spring-auth` service starts.

It checks that configured origins, HTTP methods, and headers follow the application's security rules.

It contains :

* `validateCorsConfiguration()`,
* `validateOrigins()`,
* `validateMethods()`,
* `validateHeaders()`,
* `isDevelopmentOrTest()`.

### CORS validation

`validateCorsConfiguration()` is automatically executed after the configuration bean is created using `@PostConstruct`.

It validates :

* allowed origins,
* allowed HTTP methods,
* allowed headers.

If the configuration is invalid, a `CorsConfigurationException` is thrown and the application startup fails.

### `validateOrigins()`

Validates the configured CORS origins.

It checks that :

* at least one origin is configured,
* origins are valid URLs,
* only `http` and `https` protocols are allowed,
* duplicate origins are rejected,
* `localhost` and `127.0.0.1` are not allowed in production,
* the wildcard `*` is only allowed in development or test environments.

### `validateMethods()`

Validates the HTTP methods allowed by CORS.

The allowed methods are :

* `GET`,
* `POST`,
* `PUT`,
* `DELETE`,
* `PATCH`,
* `OPTIONS`,
* `HEAD`.

It also rejects duplicate methods and methods outside this whitelist.

### `validateHeaders()`

Validates the HTTP headers allowed by CORS.

Only headers from the application's whitelist can be configured, including :

* `Authorization`,
* `Content-Type`,
* `Accept`,
* `Accept-Language`,
* `Cache-Control`,
* `X-Requested-With`,
* `X-CSRF-Token`,
* `X-API-Key`.

Wildcard headers (`*`) and duplicate headers are rejected.

---

## *CustomAccessDeniedHandler.java*

`CustomAccessDeniedHandler` is a Spring Security component used to handle requests where an authenticated user does not have the required permissions.

It implements Spring Security's `AccessDeniedHandler` and returns an HTTP `403 FORBIDDEN` response in JSON format.

It contains :

* `handle()`,
* `messageSource`,
* `OBJECT_MAPPER`.

### `handle()`

Handles `AccessDeniedException` when an authenticated user tries to access a protected resource without sufficient permissions.

It :

* sets the HTTP status to `403 FORBIDDEN`,
* sets the response content type to JSON,
* retrieves a translated error message using `MessageSource`,
* creates an `ErrorDto`,
* converts the error response to JSON using `ObjectMapper`.

The default message uses the translation key `error.security.access.denied`.

If the exception contains a message that corresponds to a valid translation key, that message is used instead.

If the key does not exist, the default access-denied message is used.

### Internationalization

`MessageSource` and `LocaleContextHolder` are used to return the error message in the language of the current request.

This allows access-denied responses to be localized.

### Error response

The error message is placed inside an `ErrorDto` before being written to the HTTP response.

This keeps the JSON error format consistent with the rest of the application's error handling system.

---

## *JwtAuthFilter.java*

`JwtAuthFilter` is a Spring Security filter responsible for validating JWT authentication tokens in incoming HTTP requests.

It extends `OncePerRequestFilter`, ensuring that the filter is executed only once per request.

It contains :

* `doFilterInternal()`,
* `userAuthenticationProvider`,
* `mapper`,
* `messageSource`.

### `doFilterInternal()`

Processes each incoming request and checks its `Authorization` header.

If the header does not exist or does not start with `Bearer`, the request continues without authentication.

If a Bearer token is present, the filter extracts the JWT and passes it to `UserAuthenticationProvider` for validation.

If validation succeeds, the resulting `Authentication` object is stored in Spring Security's `SecurityContext`.

### JWT errors

If JWT validation fails, the filter :

* clears the security context,
* returns HTTP `401 UNAUTHORIZED`,
* creates a localized error message,
* returns the error as JSON.

Different JWT errors have different messages, including :

* expired token,
* invalid claims,
* invalid signature,
* other invalid token errors.

The response contains :

* `message`,
* `error`.

The `error` value is set to `INVALID_TOKEN`.

### Unexpected errors

Other `RuntimeException` errors are logged and re-thrown so that they can be handled by the application's normal exception handling system.

### Internationalization

`MessageSource` and `LocaleContextHolder` are used to retrieve JWT error messages according to the current request's language.

### Security context

When a JWT is successfully validated, the authenticated user is stored in Spring Security's `SecurityContextHolder`.

This allows the rest of the application to identify the authenticated user and apply authorization rules such as `@PreAuthorize`.

---

## *PermissionEnum.java*

`PermissionEnum` is an enumeration that defines the permissions available in the `spring-auth` service.

Each permission represents a specific action that can be performed on users. These permissions are used by Spring Security for authorization checks.

It contains :

* `USER_READ`,
* `USER_WRITE`,
* `USER_UPDATE`,
* `USER_DELETE`

Each permission has a corresponding string used by Spring Security:

* `USER_READ` → `user:read`,
* `USER_WRITE` → `user:write`,
* `USER_UPDATE` → `user:update`,
* `USER_DELETE` → `user:delete`

The `getPermission()` method returns the string associated with a permission.

These permissions can be combined with roles to implement more specific access control.


---

## *Role.java*

`Role` is a JPA Entity class that represents a role in the `spring-auth` service.

It maps to the `roles` table in the database and stores the role's name, description, and timestamps.

It contains :

* `id`,
* `name`,
* `description`,
* `createdAt`,
* `updatedAt`,
* `users`

The `name` uses `RoleEnum` and is stored as a string in the database. Each role has a unique name.

`createdAt` and `updatedAt` are automatically managed by Hibernate to track when the role was created and last modified.

### Users relationship

The `users` field contains the users who have this role as their `mainRole`.

It uses a `@OneToMany` relationship with `User`. The relationship is mapped through the `mainRole` field in the `User` entity.

The relationship uses eager fetching so the users associated with the role are loaded automatically.

`@JsonIgnore` prevents the users from being included when the role is converted to JSON, avoiding recursive references between `Role` and `User`.

`@Getter` and `@Setter` from Lombok automatically generate the getters and setters for the class.


---

## *RoleEnum.java*

`RoleEnum` is an enumeration that defines the roles available in the `spring-auth` service.

Each role has a predefined set of permissions that determines which actions a user can perform.

It contains :

* `USER`,
* `MANAGER`,
* `ADMIN`

### Roles and permissions

* `USER` → `USER_READ`
* `MANAGER` → `USER_READ`, `USER_WRITE`, `USER_UPDATE`
* `ADMIN` → `USER_READ`, `USER_WRITE`, `USER_UPDATE`, `USER_DELETE`

The permissions are stored as a `Set<PermissionEnum>` for each role.

### `getGrantedAuthorities()`

Converts the role's permissions into Spring Security `SimpleGrantedAuthority` objects.

It also adds a role authority using the `ROLE_` prefix.

For example, the `ADMIN` role produces authorities such as:

* `user:read`,
* `user:write`,
* `user:update`,
* `user:delete`,
* `ROLE_ADMIN`

These authorities are then used by Spring Security for authorization checks.

`getPermissions()` returns the permissions associated with the role.


---

## *RoleRepository.java*

`RoleRepository` is a repository interface used to access and manage `Role` entities in the `spring-auth` database.

It extends `CrudRepository<Role, Long>`, which provides standard CRUD operations such as creating, finding, updating, and deleting roles.

It contains :

* `findByName()`

### `findByName()`

Finds a role using its `RoleEnum` name.

It returns an `Optional<Role>`, meaning the role may or may not exist in the database.

Spring Data JPA automatically generates the implementation of this method based on its name.

`@Repository` marks the interface as a Spring Data repository.

---

## *RoleSeeder.java*

`RoleSeeder` is a seeder class used to initialize the `spring-auth` database with the default roles.

It implements `ApplicationListener<ContextRefreshedEvent>`, so the role initialization runs when the Spring application context is refreshed.

It contains :

* `onApplicationEvent()`,
* `loadRoles()`,
* `roleRepository`

### `loadRoles()`

Checks whether the default roles already exist in the database.

It creates the following roles if they do not exist :

* `USER` → Default user role,
* `MANAGER` → Manager role,
* `ADMIN` → Administrator role.

Each role is assigned a description before being saved using `RoleRepository`.

If a role already exists, it is not created again.

This ensures that the required roles are available in the database when the application starts.


---

## *SecurityConfig.java*

`SecurityConfig` is the main Spring Security configuration class for the `spring-auth` service.

It configures authentication, authorization, JWT authentication, OAuth2 login, CORS, sessions, and security error handling.

It contains :

* `securityFilterChain()`,
* `oauth2UserService()`,
* `isDevelopmentOrTest()`

### `securityFilterChain()`

Configures the application's Spring Security filter chain.

It :

* adds `JwtAuthFilter` before `BasicAuthenticationFilter`,
* disables CSRF protection,
* uses `IF_REQUIRED` session management,
* configures CORS,
* configures OAuth2 login,
* defines which endpoints are public or protected.

Authentication failures are handled by `UserAuthenticationEntryPoint`, while access-denied errors are handled by `CustomAccessDeniedHandler`.

### CORS

CORS settings are loaded from the application properties:

* `cors.allowed-origins`,
* `cors.allowed-methods`,
* `cors.allowed-headers`

Credentials are allowed for cross-origin requests.

### OAuth2

The application supports OAuth2 login.

The OAuth2 configuration :

* loads user information using `oauth2UserService()`,
* redirects successful authentication to `/oauth2/success`,
* redirects failed authentication to `/oauth2/error`.

Detailed OAuth2 information and user attributes are only logged in development or test environments.

### Endpoint authorization

The following endpoints are publicly accessible:

* `POST /auth/login`,
* `POST /auth/register`,
* `POST /auth/refresh`,
* `OPTIONS /**`,
* `/oauth2/login`,
* `/oauth2/login/**`,
* `/oauth2/authorization/**`,
* `POST /oauth2/token`,
* `/oauth2/error`,
* `/login/oauth2/code/**`

`/oauth2/success` requires authentication.

All other requests require an authenticated user.

### `oauth2UserService()`

Creates the `OAuth2UserService` used to retrieve user information from the OAuth2 provider.

It uses `DefaultOAuth2UserService` to load the user's attributes.

User attributes are only logged in development or test environments to avoid exposing sensitive information in production logs.

### `isDevelopmentOrTest()`

Checks the active Spring profiles.

It returns `true` when the `dev` or `test` profile is active.

If no profile is configured, it also treats the application as a development environment.

This is mainly used to control the amount of information written to the logs.

---

## *SecurityExceptions.java*

`SecurityExceptions` is a container class for security and authorization-related exceptions in the `spring-auth` service.

It groups security-specific exceptions as static inner classes.

It contains :

* `RoleNotFoundException`,
* `TokenNotFromTrustedTenantException`,
* `MissingJwtClaimException`,
* `UnauthorizedActionException`,
* `UserHasLowerRightsException`,
* `HashAlgorithmUnavailableException`,
* `CorsConfigurationException`

### `RoleNotFoundException`

Thrown when a role cannot be found.

It returns HTTP `404 NOT_FOUND` and includes the role name as a message argument.

### `TokenNotFromTrustedTenantException`

Thrown when an Azure token does not come from a trusted tenant.

It returns HTTP `403 FORBIDDEN`.

### `MissingJwtClaimException`

Thrown when a required claim is missing from a JWT token.

It returns HTTP `403 FORBIDDEN` and includes the missing claim name as a message argument.

### `UnauthorizedActionException`

Thrown when a user attempts an action they are not authorized to perform.

It returns HTTP `403 FORBIDDEN`.

### `UserHasLowerRightsException`

Thrown when a user attempts an action against another user with insufficient rights.

It returns HTTP `403 FORBIDDEN` and includes the target user's login as a message argument.

### `HashAlgorithmUnavailableException`

Thrown when the required hash algorithm is unavailable.

It returns HTTP `500 INTERNAL_SERVER_ERROR`.

### `CorsConfigurationException`

Thrown when the application's CORS configuration is invalid.

It returns HTTP `500 INTERNAL_SERVER_ERROR` and can receive a message key and optional arguments for the localized error message.

All exceptions extend `AppException` and implement `MessageKeyProvider`, allowing the global exception handling system to return the appropriate HTTP status and translated error message.

---

## *UserAuthenticationEntryPoint.java*

`UserAuthenticationEntryPoint` is a Spring Security component used to handle requests from unauthenticated users in the `spring-auth` service.

It implements `AuthenticationEntryPoint` and returns an HTTP `401 UNAUTHORIZED` response in JSON format.

It contains :

* `commence()`,
* `messageSource`,
* `OBJECT_MAPPER`

### `commence()`

Handles authentication failures when an unauthenticated user tries to access a protected resource.

It :

* sets the HTTP status to `401 UNAUTHORIZED`,
* sets the response content type to JSON,
* retrieves a translated error message using `MessageSource`,
* creates an `ErrorDto`,
* converts the error response to JSON using `ObjectMapper`.

If the authentication exception contains a message matching a translation key, that translated message is used.

If no matching message exists, the default `error.security.authentication.token.invalid.or.missing` message is used.

### Internationalization

`MessageSource` and `LocaleContextHolder` are used to return the error message according to the current request's language.

### Error response

The error message is placed inside an `ErrorDto` before being written to the HTTP response.

This provides a consistent JSON response when authentication fails.

---

## *UserAuthenticationProvider.java*

`UserAuthenticationProvider` is a Spring Security component responsible for creating and validating JWT tokens in the `spring-auth` service.

It handles :

* access token creation,
* refresh token creation and validation,
* JWT validation,
* conversion of roles and permissions into Spring Security authorities,
* authentication of users,
* Azure user integration.

It uses :

* `UserService`,
* `Algorithm`,
* `JWTVerifier`,
* `DecodedJWT`

### Token configuration

The class uses environment variables for the JWT configuration:

* `SECURITY_JWT_TOKEN_SECRET_ACCESS_KEY` → secret used for access tokens,
* `SECURITY_JWT_TOKEN_SECRET_REFRESH_KEY` → secret used for refresh tokens,
* `SECURITY_JWT_TOKEN_ACCESS_TOKEN_LIFETIME` → access token lifetime,
* `SECURITY_JWT_TOKEN_REFRESH_TOKEN_LIFETIME` → refresh token lifetime.

The Azure issuer URI is also loaded from the application configuration.

### `init()`

Runs after dependency injection using `@PostConstruct`.

It Base64-encodes the configured access and refresh secrets before they are used to create JWT signing algorithms.

### `createToken()`

Creates an access JWT for a user.

The token contains :

* the user's login as the subject,
* token type `access`,
* issue time,
* expiration time,
* first name,
* last name,
* main role,
* permissions.

The token is signed using the access-token secret.

### `createRefreshToken()`

Creates a refresh JWT for a user.

It contains the user's login, token type `refresh`, issue time, and expiration time.

It is signed using a separate refresh-token secret.

### `createExpiredRefreshToken()`

Creates an already expired refresh token.

It is used during logout so that the client can immediately invalidate its refresh token.

### `buildAuthorities()`

Converts user roles and permissions into Spring Security authorities.

For each role, it creates a `ROLE_` authority and adds the authorities defined by `RoleEnum`.

For example, the `ADMIN` role produces `ROLE_ADMIN` and its associated permissions.

### `validateToken()`

Validates an access JWT using the access-token secret.

It verifies the token and extracts the user's information from its claims.

The extracted information is converted into a `UserDto`, and the user's role is converted into Spring Security authorities.

It then returns a `UsernamePasswordAuthenticationToken` containing the authenticated user.

### `validateTokenStrongly()`

Performs JWT validation and also checks the user against the database.

If the user exists, their current information and permissions are loaded from the database and used to create the authentication.

If the user does not exist, the method checks the Azure token issuer and required `firstName` and `lastName` claims.

A new Azure user is then created with the `USER` role.

This method therefore provides additional database and Azure validation compared to `validateToken()`.

### `validateRefreshToken()`

Validates a refresh JWT using the refresh-token secret.

It also checks that the token contains the `refresh` token type.

If the token is invalid or expired, JWT verification throws an exception.

### Authentication

The class creates `UsernamePasswordAuthenticationToken` objects containing the authenticated user and their Spring Security authorities.

These authentication objects are used by Spring Security to identify the user and apply authorization rules.

