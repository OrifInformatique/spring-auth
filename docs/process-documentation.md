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