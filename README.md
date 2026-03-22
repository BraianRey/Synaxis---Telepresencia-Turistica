# Synaxis---Telepresencia-Turistica

# SIS-PTM — Real-Time Telepresence System

## Overview

**SIS-PTM (Sistema de Telepresencia)** is a distributed system designed to enable real-time remote experiences through mobile applications and a scalable backend infrastructure.

The system is composed of multiple independent yet integrated components:

* **Client Mobile Application** — for end users requesting telepresence services
* **Partner Mobile Application** — for service providers delivering telepresence experiences
* **Backend API (Spring Boot)** — `backend-management-service` (REST API, BCrypt, registration by role)
* **Database (H2)** — embedded file database for development; optional web console at `/h2-console` when the API is running

This repository contains mobile applications, the **Spring Boot** management API, and shared development configuration.

---

## Project Structure

```bash
.
├── mobile-client/              # Android app for end users (clients)
├── mobile-partner/             # Android app for service providers (partners)
├── backend-management-service/ # Spring Boot API (H2, registration, etc.)
├── postman/                    # Postman collection for the API
├── docs/                       # Project notes and change summaries
├── start-database.bat          # H2 connection hints and console URL (Windows)
├── start-backend.bat           # Runs the Spring Boot service (Windows)
├── .vscode/                    # Shared VS Code configuration
├── .editorconfig               # Code formatting rules
├── .gitignore                  # Git exclusions
└── README.md
```

---

## Mobile Applications

### 1. Client App (`mobile-client`)

Application used by customers to:

* Request telepresence services
* Browse available partners
* Manage sessions and history

### 2. Partner App (`mobile-partner`)

Application used by service providers to:

* Accept service requests
* Stream real-time telepresence sessions
* Manage availability and sessions

Both applications follow the same clean architecture:

```bash
data/       # Data sources, repositories, DTOs
domain/     # Use cases and business rules
ui/         # Screens and ViewModels
utils/      # Helpers and extensions
di/         # Dependency injection
```

---

## Backend

The **`backend-management-service`** module is a **Spring Boot** application that provides:

* RESTful endpoints (e.g. registration for clients and partners)
* Password hashing with **BCrypt** (Spring Security)
* Persistence with **H2** (embedded file database)

Further features (e.g. JWT, full auth flows) may be added as the project evolves.

---

## Database

The backend uses **H2** as the relational database. The file is stored under `backend-management-service/data/` when you run the app from `start-backend.bat` (working directory `backend-management-service`).

**Local development:**

1. Start the API with `start-backend.bat`; no separate database server is required.
2. Optional: open the H2 console at `http://localhost:8080/h2-console` and connect with JDBC URL `jdbc:h2:file:./data/synaxis`, user `sa`, empty password (same as `application.yaml`).
3. Hibernate `ddl-auto` is set to `update` in development.

Run `start-database.bat` on Windows for a short reminder of the JDBC URL and console.

---

## Development Environment

### Required Tools

* JDK **21**
* Gradle **8.x**
* Kotlin **1.9.x**
* Git **2.44+**
* VS Code **1.88+**
* Android Studio **4.0-9.1+**

### VS Code Extensions

Automatically suggested via `.vscode/extensions.json`:

* Kotlin support
* Java Extension Pack
* Gradle tools
* SonarLint
* GitLens
* EditorConfig

---

## Configuration Standards

This project enforces a unified development environment:

* `.editorconfig` ensures consistent formatting
* `.vscode/settings.json` enforces editor behavior
* Automatic formatting on save
* Line length limit: **120 characters**

---


## Git Workflow

### Rules

* ❌ No direct commits to `main`
* ✔ All changes must go through Pull Requests
* ✔ At least one review required (interventor)
* ✔ No self-approval

---

## Commit Convention

All commits must follow:

```bash
<type>(<scope>): <short description in English>
```

---

## Security Rules (Mandatory)

* ❌ Never commit credentials, tokens, or secrets
* ❌ `.env` files must not be tracked
* ✔ Use environment variables
* ✔ Validate all user input
* ✔ Use secure password hashing (BCrypt)

---

## Development Principles

* DRY (Don't Repeat Yourself)
* Single Responsibility Principle
* No magic numbers
* No dead/commented code
* Clear and descriptive naming

---

## Final Note

This repository represents the **single source of truth** for development standards.
All contributors must strictly follow these conventions to ensure consistency, scalability, and code quality across the entire system.

---
