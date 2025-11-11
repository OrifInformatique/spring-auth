# Application Documentation

## Table of Contents
- [Documentation Tools](#documentation-tools)
- [Overview](#overview)
- [1. Spring-Auth](#1-spring-auth)
  - [1.1 General Information](#11-general-information)
  - [1.2 Root Files](#12-root-files)
  - [1.3 Root Folders](#13-root-folders)
  - [1.4 Source Structure (`src`)](#14-source-structure-src)
    - [1.4.1 `main`](#141-main)
    - [1.4.2 `test`](#142-test)
    - [1.5 Main Java Modules (`main/java`)](#15-main-java-modules-mainjava)
    - [1.6 Security Module (`main/java/security`)](#16-security-module-mainjavasecurity)
    - [1.7 Auth Module (`main/java/auth`)](#17-auth-module-mainjavaauth)
    - [1.8 Users Module (`main/java/users`)](#18-users-module-mainjavausers)
    - [1.9 Configuration Module (`main/java/config`)](#19-configuration-module-mainjavaconfig)
    - [1.10 Error and Exception Managment (`main/java/app`)](#110-error-and-exception-managment-mainjavaapp)
    - [1.11 Main Test Modules (`main/test`)](#111-main-test-modules-maintest)
    - [1.12 Security Tests (`main/test/security`)](#112-security-tests-maintestsecurity)
    - [1.13 Authentication Tests (`main/test/auth`)](#113-authentication-tests-maintestauth)
    - [1.14 User Tests (`main/test/user`)](#114-user-tests-maintestuser)
---

## Documentation Tools
Recomended Mermaid Preview Tool [Markdown Preview Mermaid Support](https://marketplace.visualstudio.com/items?itemName=bierner.markdown-mermaid).

---
## Overview

This document describes the **structure, components, and processes** of the spring-auth application, including configuration files, folder organization, and module responsibilities.  

This application powers a **authentication system**, providing:
- Secure authentication and authorization (delegated to spring-auth)
- User and role management  

![app interactions](frontend_backend_auth_architecture.png)  
*Illustrates interactions between the frontend and backend modules of the `template_frontback` app, as well as the `spring-auth` app.*

---
## 1. Spring-Auth

### 1.1 General Information
The `spring-auth` module is a standalone Spring Boot application that provides authentication and authorization services. It manages user credentials, roles, and permissions, and integrates with Microsoft Entra Azure AD for OAuth2 authentication.

**Tools & Dependencies:**
- Java / OpenJDK 21  
- Spring Boot 3.3.5  
- Maven 3.9  
- MariaDB 11.4  
- Docker Desktop  

> **Note:** Detailed setup and run instructions are provided in the project’s main [`README.md`](../README.md).

---

### 1.2 Root Files

| File | Description |
|------|-------------|
| `pom.xml` | Defines project dependencies, plugins, and build configurations. |
| `init.sql` | SQL script to create and initialize the database schema. |
| `Dockerfile` | Defines Docker image build stages and application setup. |
| `compose.yml` | Configures Docker environment and additional services. |
| `application.properties` | Global configuration properties for Spring Boot. |
| `.env` | Environment variables for local development and deployment. |
| `README.md` | Project overview, setup instructions, and documentation links. |

--

### 1.3 Root Folders

| Folder | Description |
|--------|-------------|
| `src` | Contains the application’s source code and resources. |
| `target` | Compiled classes and build artifacts. |
| `docs` | Documentation. |

---

### 1.4 Source Structure (`src`)

#### 1.4.1 `main`
Contains the core functionality of the application.  

- **`java`** – Source code (controllers, services, entities, configurations, etc.)  
- **`resources`** – Configuration files, static resources, and templates  

#### 1.4.2 `test`
Contains test classes for unit and integration tests.  

- **`java`** – Test classes corresponding to the application’s source code  
- **`resources`** – Test-specific configuration or data  

> *Testing frameworks, execution instructions, and coverage details will be added once the Java modules are finalized.*

---

### 1.5 Main Java Modules (`main/java`)

| Module | Responsibility |
|--------|----------------|
| `app` | Global error and exception handling used throughout the application. |
| `auth` | Handles authorization processes such as login and registration. |
| `security` | Security-related classes: JWT filters, password encoding, and authentication management. |
| `users` | Manages user profiles, roles, and permissions. |
| `TemplateApplication.java` | Main Spring Boot entry point containing the `main()` method. Run the project from this class. |

---
### 1.8 Tests Controlleurs (`main/java/test`)

| File | Description |
|------|-------------|
| `TestControlleur.java` | Controlleur to test fonctionalities. |
---

### 1.6Security Module (`main/java/security`)

```mermaid
sequenceDiagram
    participant Client
    participant JwtAuthFilter
    participant UserAuthenticationProvider
    participant UserService
    participant UserController
    participant UserAuthenticationEntryPoint

    Client->>JwtAuthFilter: HTTP request with JWT token
    note right of JwtAuthFilter: Filter intercepts request
    JwtAuthFilter->>UserAuthenticationProvider: validateToken(token)
    note right of UserAuthenticationProvider: Check token & optionally fetch user
    UserAuthenticationProvider->>UserService: Lookup user in DB
    alt User exists
        UserService->>UserAuthenticationProvider: Return user info
    else User not found
        UserService->>UserAuthenticationProvider: Create new Azure user
    end
    UserAuthenticationProvider->>JwtAuthFilter: Return Authentication object
    alt Authentication fails
        JwtAuthFilter->>UserAuthenticationEntryPoint: 401 Unauthorized
        UserAuthenticationEntryPoint->>Client: Return 401 response
    else Authentication succeeds
        JwtAuthFilter->>UserController: Forward request
        note right of UserController: Controller handles action
        UserController->>UserService: Perform business logic
        UserService->>UserController: Return resul
        UserController->>Client: Return HTTP response
    end
```
*Sequence Diagram showing JWT authentication and request handling flow.*

| File | Description |
|------|-------------|
| `JwtAuthFilter.java` | Authentication filter that processes tokens for incoming requests. |
| `PermissionEnum.java` | Enumeration defining available permissions. |
| `Role.java` | Role entity class representing a user role. |
| `RoleEnum.java` | Enumeration defining roles and their permissions. |
| `RoleRepository.java` | Interface for database operations related to roles. |
| `RoleSeeder.java` | Seeds the database with predefined roles. |
| `SecurityConfig.java` | Security configuration defining the filter chain and access rules. |
| `UserAuthenticationEntryPoint.java` | Handles unauthenticated access by returning a 401 response. |
| `UserAuthenticationProvider.java` | Authentication provider for validating user credentials. |
| `WebClientConfig.java` | Web client configuration for communication with other apps. |
| `WebConfig.java` | Web configuration for general web-related settings. |

---

---

### 1.7Auth Module (`main/java/auth`)
```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant UserService
    participant UserRepository

    Client->>AuthController: /auth/login with credentials
    AuthController->>UserService: userService.login(credentialsDto)
    UserService->>UserRepository: userRepository.findByLogin(credentialsDto.login())
    UserRepository->>UserService: User found
    UserService->>AuthController: Return UserDto
    AuthController->>Client: Response with UserDto
```
*Sequence Diagram showing an example of the authentication flow.*

| File | Description |
|------|-------------|
| `AuthController.java` | Controller handling user authentication and registration. |
| `CredentialsDto.java` | Data Transfer Object (DTO) for login credentials. |
| `NewPasswordDto.java` | DTO for handling new password requests. |
| `OAuth2Controller.java` | Controller handling OAuth2 authentication flows. |
| `PasswordConfig.java` | Configuration class for password policies and encryption. |
| `SignUpDto.java` | DTO for registration functionalities. |


---

### 1.8 Users Module (`main/java/users`)

```mermaid
classDiagram
    %% =====================
    %% Entities
    %% =====================
    class User {
        +long id
        +String firstName
        +String lastName
        +String login
        +Date createdAt
        +Date updatedAt
        +Role mainRole
        +Collection<GrantedAuthority> getAuthorities()
        +String getUsername() 
        +boolean isAccountNonExpired()
        +boolean isAccountNonLocked()
        +boolean isCredentialsNonExpired()
        +boolean isEnabled()
        +Role getMainRole()
        +void setMainRole(Role role)
    }

    class Role {
        +long id
        +RoleEnum name
        +String description
        +Date createdAt
        +Date updatedAt
        +Set<User> users
    }

    class RoleEnum {
        <<enum>>
        +USER
        +MANAGER
        +ADMIN
        --
        -Set<PermissionEnum> permissions
        +Set<PermissionEnum> getPermissions()
        +Set<SimpleGrantedAuthority> getGrantedAuthorities()
    }

    class PermissionEnum {
        <<enum>>
        +USER_READ("user:read")
        +USER_WRITE("user:write")
        +USER_UPDATE("user:update")
        +USER_DELETE("user:delete")
        --
        -String permission
        +String getPermission()
    }

    %% =====================
    %% DTOs
    %% =====================
    class UserDto {
        <<DTO>>
        +Long id
        +String firstName
        +String lastName
        +String login
        +String token
        +String refreshToken
        +String mainRole = "USER"
        +List<String> permissions = new ArrayList<>()
    }

    class SignupDto {
        <<DTO>>
        +String firstName
        +String lastName
        +String login
    }

    %% =====================
    %% Mapper
    %% =====================
    class UserMapper {
        <<interface / singleton>>
        +UserDto toUserDto(User user)
        +User signUpToUser(SignupDto signupDto)
        +List<String> authoritiesToPermissions(Collection<GrantedAuthority> authorities)
    }

    %% =====================
    %% Relationships
    %% =====================
    User --> "0..1" Role : mainRole
    Role --> "0..*" User : users
    Role --> RoleEnum : uses
    RoleEnum --> "0..*" PermissionEnum : defines
    UserMapper ..> User : uses
    UserMapper ..> UserDto : creates
    UserMapper ..> SignupDto : uses
    User ..|> UserDetails
```
*Class Diagram showing the `User`, `Role`, `UserDto`, and `SignUpDto` structure.*
```mermaid
sequenceDiagram
    participant Client
    participant SecurityLayer
    participant UserController
    participant UserService
    participant UserRepository
    participant UserMapper

    %% /users/me
    Client->>SecurityLayer: /users/me
    SecurityLayer->>UserController: Authorized UserDto extracted from token
    UserController-->>Client: ResponseEntity<UserDto> (current authenticated user)

    %% /users/all
    Client->>SecurityLayer: /users/all
    SecurityLayer->>UserController: Authorized request (requires user:read)
    UserController->>UserService: allUsers()
    UserService->>UserRepository: findAll()
    UserRepository-->>UserService: List<User>
    UserService-->>UserController: List<User>
    UserController-->>Client: ResponseEntity<List<User>>

    %% /users/{userId}/promote-manager
    Client->>SecurityLayer: /users/{userId}/promote-manager
    SecurityLayer->>UserController: Authorized request (requires user:update)
    UserController->>UserService: promoteToManager(userId)
    UserService->>UserRepository: findById(userId)
    UserRepository-->>UserService: Found User
    UserService->>UserRepository: save(user with updated manager role)
    UserService->>UserMapper: toUserDto(user)
    UserMapper-->>UserService: UserDto
    UserService-->>UserController: UserDto
    UserController-->>Client: ResponseEntity("User promoted to manager successfully")
```
*Sequence Diagram showing an example of the user management flow.*

| File | Description |
|------|-------------|
| `User.java` | Entity class representing a user in the system. |
| `UserController.java` | Handles HTTP requests related to users. |
| `UserDto.java` | DTO for communication between backend and frontend. |
| `UserMapper.java` | Handles conversion between `User` entities and `UserDto` objects. |
| `UserRepository.java` | Interface for database operations related to users. |
| `UserSeeder.java` | Seeds the database with test users for development. |
| `UserService.java` | Business logic for user functionalities (creation, update, role assignment, etc.). |

---

### 1.9 Configuration Module (`main/java/config`)
| File | Description |
|------|-------------|
| `MapperConfig.java` | Fallback configuration to expose MapStruct mappers as Spring beans. |

---

### 1.10 Error and Exception Managment (`main/java/app`)

| File | Description |
|------|-------------|
| `errors/ErrorDto.java` | Record serving as Data Transfer object for Errors. |
| `exceptions/AppException.java` | Custome exception class for application specifique errors. |
| `exceptions/RestExceptionHandler.java` | Global exception handler for REST API endpoints. |

---
### 1.11 Main Test Modules (`main/test`)
| File | Description |
|------|-------------|
| `security` | Tests related to the security and authentification fonctionalities of the app. |
| `test` | Tests related to the test endpoints of the app. |
| `user` | Tests related to the user managment fonctionalities of the app. |
| `TemplateApplicationTests.java` | Load the context of the app. |
| `TestConfigurationDebug.java` | Test the existence of the environnment variables. |

---
### 1.12 Security Tests (`main/test/security`)
| File | Description |
|------|-------------|
| `UserAuthentificationProvider.java` | Tests the methodes in `UserAuthentificationProvider`. |

---
### 1.13 Authentication Tests (`main/test/auth`)
| File | Description |
|------|-------------|
| `AuthControllerIntegrationTest.java` | Tests the methodes in `AuthController` and save the result in data files. |
| `AuthControllerDocTest.java` | Generate the documentation of the methodes in `AuthController` from the data files. |

---
### 1.14 User Tests (`main/test/user`)
| File | Description |
|------|-------------|
| `TestUserSeeder.java` | Specialised seeder that create `Users` only in test mode. |
| `UserControllerIntegrationTest.java` | Tests the methodes in `UserController` and save the result in data files. |
| `UserControllerDocTest.java` | Generate the documentation of the methodes in `UserController` from the data files. |
| `UserDtoTest.java` | Test the `UserDto` object. |
| `UserMapperTest.java` | Test the `UserMapper` interface. |
| `UserServiceTest.java` | Test the `UserService` class. |
| `UserTest.java` | Test the `User` object. |
---