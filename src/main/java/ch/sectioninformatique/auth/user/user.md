# USER PACKAGE
USER Documentation of Spring-Auth

## Table of Contents
- [User.java](#userdtojava)
    - [Spring Security](#spring-security)
    - [mainRole](#mainrole)
- [UserController.java](#usercontrollerjava)
    - [Endpoints](#endpoints)
    - [Security](#security)
    - [Deletion](#deletion)
    - [Internationalization](#internationalization)
- [UserDto.java](#userdtojava)
    - [UserDto.java](#userdtojava)
- [UserExceptions.java](#userexceptionsjava)
    - [LoginBasedException](#userdtojava)
    - [User exceptions](#user-exceptions)
- [UserMapper.java](#usermapperjava)
    - [toUserDto()](#touserdto)
    - [signUpToUser()](#signuptouser)
    - [authoritiesToPermissions()](#authoritiestopermissions)
    - [clearPasswordAfterMapping()](#clearpasswordaftermapping)
- [UserRepository.java](#userrepositoryjava)
    - [User lookup](#user-lookup)
    - [deletePermanentByLogin()](#deletepermanentbylogin)
    - [existsByLogin()](#existsbylogin)
- [UserSeeder.java](#userseederjava)
    - [loadUserData()](#loaduserdata)
- [UserService.java](#userservicejava)
    - [Authentication](#authentication)
    - [Registration](#registration)
    - [Password management](#password-management)
    - [Refresh tokens](#refresh-tokens)
    - [User retrieval](#user-retrieval)
    - [User restoration](#user-restoration)
    - [Role management](#role-management)
    - [Authorization](#authorization)
    - [User deletion](#user-deletion)
    - [Azure users](#azure-users)
    - [User updates](#user-updates)
    - [Security and transactions](#security-and-transactions)

---

## *User.java*

`User` is a JPA Entity class that represents a user in the database.

It also implements Spring Security's `UserDetails` interface, allowing the user to be authenticated and authorized by Spring Security.

It contains :

* `id`,
* `firstName`,
* `lastName`,
* `login`,
* `password`,
* `createdAt`,
* `updatedAt`,
* `deleted`,
* `mainRole`

The `login` is unique and is used as the user's authentication identifier.

The `password` stores the user's **hashed password** and is limited to 72 characters in the database.

`createdAt` and `updatedAt` are automatically managed by Hibernate to track when the account was created and last modified.

The class uses **soft deletion**. When a user is deleted, `@SQLDelete` changes the `deleted` field to `true` instead of physically removing the user from the database.

The `deletedFilter` can then filter users based on their deletion status. This means the user's database record remains available, but the account is treated as deleted.

### Spring Security

`getAuthorities()` converts the user's main role into Spring Security authorities using `Role.getGrantedAuthorities()`.

`getUsername()` returns the user's `login`, which is used as the username by Spring Security.

`getPassword()` returns the user's stored hashed password for authentication.

The `UserDetails` methods check the `deleted` status of the account. If `deleted` is `true`, the account is considered expired, locked, disabled, and its credentials are considered expired.

### `mainRole`

`mainRole` represents the user's main role and is stored using a `@ManyToOne` relationship with the `Role` entity.

The role is loaded eagerly so that it is available when the user's authorities are retrieved.

`@Data`, `@Builder`, and `@NoArgsConstructor` from Lombok generate common methods, a builder, and a no-argument constructor.


---

## *UserController.java*

`UserController` is a REST Controller responsible for handling HTTP requests related to users in the `spring-auth` authentication service.

It uses `UserService` to manage users and `MessageSource` to return localized success and error messages.

All endpoints use the `/users` path.

It contains :

* `authenticatedUser()`,
* `allUsers()`,
* `allWithDeletedUsers()`,
* `deletedUsers()`,
* `restoreDeletedUser()`,
* `promoteToManager()`,
* `revokeManagerRole()`,
* `promoteToAdmin()`,
* `revokeAdminRole()`,
* `downgradeAdminRole()`,
* `getUserByLogin()`,
* `delete()`,
* `updateUser()`

### Endpoints

* `GET /users/me` → retrieves the currently authenticated user's information.
* `GET /users/` → retrieves all active users.
* `GET /users/all-with-deleted` → retrieves all users, including soft-deleted users.
* `GET /users/deleted` → retrieves only soft-deleted users.
* `PUT /users/{login}/restore` → restores a soft-deleted user.
* `PUT /users/{login}/promote-manager` → promotes a user to manager.
* `PUT /users/{login}/revoke-manager` → removes the manager role from a user.
* `PUT /users/{login}/promote-admin` → promotes a user to administrator.
* `PUT /users/{login}/revoke-admin` → removes the administrator role from a user.
* `PUT /users/{login}/downgrade-admin` → changes an administrator back to manager.
* `GET /users/{login}` → retrieves a user using their login.
* `DELETE /users/{login}` → soft-deletes a user by default.
* `DELETE /users/{login}/{hardDelete}` → soft-deletes or permanently deletes a user depending on the `hardDelete` value.
* `PUT /users/{login}` → updates a user's information.

### Security

The controller uses `@PreAuthorize` to restrict access to its endpoints.

The permissions used are :

* `user:read` → reading users,
* `user:update` → updating users and roles,
* `user:delete` → deleting users.

The administrator role is required for operations that modify administrator privileges.

### Deletion

The `delete()` method performs a soft deletion by default.

If `hardDelete` is `true`, the user is permanently removed from the database.

The response contains a localized message and the login of the deleted user.

### Internationalization

`MessageSource` and `LocaleContextHolder` are used to return messages in the language of the current request.

This includes messages for role changes, user restoration, deletion, and user-not-found errors.


---

## *UserDto.java*

`UserDto` is a Data Transfer Object used to transfer user information between the `spring-auth` service and its clients.

It contains user information without including sensitive data such as the user's password.

It contains :

* `id`,
* `firstName`,
* `lastName`,
* `login`,
* `token`,
* `deleted`,
* `mainRole`,
* `permissions`

The `token` contains the JWT authentication token associated with the user.

The `deleted` field indicates whether the user has been soft-deleted and defaults to `false`.

The `mainRole` contains the user's main role and defaults to `USER`.

The `permissions` list contains the permissions granted to the user and defaults to an empty list.

`@Data` from Lombok generates getters, setters, `toString()`, `equals()`, and `hashCode()`.

`@Builder(toBuilder = true)` provides a builder for creating `UserDto` objects and allows existing objects to be copied and modified using `toBuilder()`.


---

## *UserExceptions.java*

`UserExceptions` is a container class for user-related exceptions in the `spring-auth` service.

It groups exceptions used when creating, finding, or changing users.

It contains :

* `UserAlreadyExistsException`,
* `UserNotFoundException`,
* `UserAlreadyAdminException`,
* `UserAlreadyManagerException`,
* `UserAlreadyRegularException`

### `LoginBasedException`

`LoginBasedException` is a private base exception used by exceptions that need to store a user's login.

It extends `AppException` and implements `MessageKeyProvider`.

The login is also provided as a message argument so it can be included in the localized error message.

### User exceptions

`UserAlreadyExistsException` is thrown when a user with the specified login already exists. It returns `409 CONFLICT`.

`UserNotFoundException` is thrown when a user cannot be found using their login or ID. It returns `404 NOT_FOUND`.

`UserAlreadyAdminException` is thrown when trying to promote a user who is already an administrator. It returns `409 CONFLICT`.

`UserAlreadyManagerException` is thrown when trying to promote a user who is already a manager. It returns `409 CONFLICT`.

`UserAlreadyRegularException` is thrown when trying to demote a user who is already a regular user. It returns `409 CONFLICT`.

Each exception provides a message key through `MessageKeyProvider`, allowing `GlobalExceptionHandler` to retrieve the corresponding translated error message.


---

## *UserMapper.java*

`UserMapper` is a MapStruct mapper used to convert between user entities and DTOs.

It contains :

* `toUserDto()`,
* `signUpToUser()`,
* `authoritiesToPermissions()`,
* `clearPasswordAfterMapping()`

### `toUserDto()`

Converts a `User` entity into a `UserDto`.

It maps the user's basic information and converts :

* the main role into a role name,
* Spring Security authorities into a list of permissions.

The `token` field is ignored because it is handled separately.

### `signUpToUser()`

Converts a `SignUpDto` into a `User` entity.

The password, role, ID, deletion status, and timestamps are ignored because they are handled separately by the application.

### `authoritiesToPermissions()`

Converts a collection of Spring Security `GrantedAuthority` objects into a list of permission strings.

### `clearPasswordAfterMapping()`

Runs after `signUpToUser()` and clears the password stored in the source `SignUpDto`.

The password is stored as a mutable `char[]`, allowing its contents to be replaced with `'\0'` after the mapping is completed.

`@Mapper(componentModel = "spring")` tells MapStruct to generate the mapper implementation as a Spring bean.

MapStruct automatically generates the implementation of the mapping methods during compilation.


---

## *UserRepository.java*

`UserRepository` is a repository interface used to access and manage `User` entities.

It extends `JpaRepository<User, Long>`, which provides standard CRUD operations.

It contains :

* `findByLogin()`,
* `findByLoginAndDeletedFalse()`,
* `findAllWithDeleted()`,
* `findAllDeleted()`,
* `findByIdDeleted()`,
* `findByLoginDeleted()`,
* `deletePermanentByLogin()`,
* `existsByLogin()`

### User lookup

`findByLogin()` finds a user by their login, including soft-deleted users.

`findByLoginAndDeletedFalse()` finds a user by their login only if the account is not soft-deleted.

`findAllWithDeleted()` retrieves all users, including soft-deleted users.

`findAllDeleted()` retrieves only users where `deleted = true`.

`findByIdDeleted()` finds a soft-deleted user using their ID.

`findByLoginDeleted()` finds a soft-deleted user using their login.

### `deletePermanentByLogin()`

Permanently removes a user from the database using their login.

Unlike the normal deletion operation, this uses a native SQL `DELETE` query and bypasses the entity's soft-delete mechanism.

`@Modifying` indicates that the query modifies the database, while `@Transactional` ensures that the operation is executed within a transaction.

### `existsByLogin()`

Checks whether a user with a specific login already exists.

This can be used to prevent duplicate accounts during registration.

Spring Data JPA automatically provides the implementation of this repository.


---

## *UserSeeder.java*

`UserSeeder` is a seeder class used to initialize the `spring-auth` database with predefined users for development.

It implements `CommandLineRunner`, so its `run()` method is executed automatically when the application starts.

It contains :

* `run()`,
* `loadUserData()`,
* `userRepository`,
* `passwordEncoder`,
* `roleRepository`

`@Profile("dev")` means the seeder only runs when the `dev` Spring profile is active.

`@Order(2)` controls the execution order of the seeder so that the required roles are created before the users.

### `loadUserData()`

Checks whether the `users` table is empty before creating the default users.

It first retrieves the `USER`, `MANAGER`, and `ADMIN` roles from `RoleRepository`.

It then creates **8 predefined users** with different roles, including :

* a predefined deleted user,
* regular users with the `USER` role,
* a user with the `MANAGER` role,
* a user with the `ADMIN` role.

Passwords are encoded using `PasswordEncoder` before being stored in the database.

The users are created using `User.builder()` and saved together using `userRepository.saveAll()`.

If the `users` table already contains users, the seeder skips the creation process.

If a required role does not exist, a `RuntimeException` is thrown.

The class also uses a `Logger` to report when user seeding starts, completes, or is skipped.


---

## *UserService.java*

`UserService` is a service class responsible for managing users and authentication-related operations in the `spring-auth` service.

It handles :

* user authentication and registration,
* password management,
* refresh tokens,
* user search and retrieval,
* role management,
* user deletion and restoration,
* Azure user integration,
* user information updates.

It uses :

* `UserRepository`,
* `RoleRepository`,
* `RefreshTokenRepository`,
* `PasswordEncoder`,
* `UserMapper`,
* `EntityManager`.

### Authentication

`login()` verifies a user's login and password.

The password is checked using `PasswordEncoder`. If the login or password is invalid, an `InvalidCredentialsException` is thrown without revealing which part of the credentials was incorrect.

### Registration

`register()` creates a new user.

It checks that the login is not already used, encodes the password, assigns the requested role, and saves the user in the database.

The method uses `SERIALIZABLE` transaction isolation to prevent conflicts when registering users simultaneously.

### Password management

`updatePassword()` verifies the user's current password before replacing it with a newly encoded password.

The old and new passwords are received through `PasswordUpdateDto`.

### Refresh tokens

The service manages refresh tokens used to obtain new access tokens.

* `storeRefreshToken()` hashes and stores a refresh token and removes the previous token for the user.
* `assertValidRefreshToken()` checks that the stored token hash matches and that the token has not expired or been revoked.
* `revokeRefreshToken()` marks the user's refresh token as revoked.
* `deleteRefreshTokens()` removes all refresh tokens belonging to a user.

Refresh tokens are stored as **SHA-256 hashes** rather than as their original values.

### User retrieval

The service provides several methods for retrieving users:

* `findByLogin()` → retrieves an active user using their login.
* `findById()` → retrieves a user using their ID.
* `allUsers()` → retrieves all active users.
* `allWithDeletedUsers()` → retrieves all users, including soft-deleted users.
* `deletedUsers()` → retrieves only soft-deleted users.

`UserMapper` converts the retrieved `User` entities into `UserDto` objects.

### User restoration

`restoreDeletedUser()` restores a soft-deleted user by setting `deleted` to `false`.

### Role management

The service provides methods for changing a user's main role:

* `promoteToManager()` → changes a user to `MANAGER`.
* `revokeManagerRole()` → changes a manager back to `USER`.
* `promoteToAdmin()` → changes a user to `ADMIN`.
* `revokeAdminRole()` → changes an administrator back to `USER`.
* `downgradeAdminRole()` → changes an administrator to `MANAGER`.

The methods check the user's current role and throw specific exceptions if the requested role change is invalid.

### Authorization

`canPerformAction()` checks whether one user can perform an action on another user based on their roles.

The hierarchy is :

* `ADMIN` → can act on all roles.
* `MANAGER` → can act on `USER` and `MANAGER`, but not `ADMIN`.
* `USER` → cannot perform role-based management actions.

This check is used when deleting users.

### User deletion

`deleteUser()` can either soft-delete or permanently delete a user.

The authenticated user's role is compared with the target user's role before the deletion is performed.

* `hardDelete = false` → uses the normal repository deletion, which triggers soft deletion.
* `hardDelete = true` → permanently removes the user from the database.

`deletePermanentUser()` always permanently deletes the user after performing the same authorization check.

### Azure users

`getOrCreateAzureUser()` integrates users authenticated through Azure AD with the local database.

If the user already exists, their existing information is returned.

If they do not exist, a new local account is created with their Azure information and the default `USER` role.

A generated password is stored for the local account, although Azure authentication is used for the user's login.

### User updates

`updateUser()` updates a user's :

* first name,
* last name,
* login,
* main role.

It does not handle password changes, which are managed separately by `updatePassword()`.

### Security and transactions

Several methods use `@Transactional` with different isolation levels to ensure that database operations are executed safely.

`SERIALIZABLE` is used for operations such as registration, role changes, deletion, and restoration where concurrent modifications could cause conflicts.

`SecurityContextHolder` is used to retrieve the currently authenticated user when checking permissions for user management operations.
