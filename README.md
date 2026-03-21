# Synaxis---Telepresencia-Turistica

# SIS-PTM — Real-Time Telepresence System

## Overview

**SIS-PTM (Sistema de Telepresencia)** is a distributed system designed to enable real-time remote experiences through mobile applications and a scalable backend infrastructure.

The system is composed of multiple independent yet integrated components:

* **Client Mobile Application** — for end users requesting telepresence services
* **Partner Mobile Application** — for service providers delivering telepresence experiences
* **Backend API (Spring Boot)** — business logic, authentication, and orchestration *(to be implemented)*
* **Database (PostgreSQL)** — persistent storage and schema versioning *(to be implemented)*

This repository currently contains the **foundation layer**, including both mobile applications and shared development configuration.

---

## Project Structure

```bash
.
├── mobile-client/      # Android app for end users (clients)
├── mobile-partner/     # Android app for service providers (partners)
├── backend/            # Spring Boot backend (planned)
├── database/           # PostgreSQL + Flyway migrations (planned)
├── .vscode/            # Shared VS Code configuration
├── .editorconfig       # Code formatting rules
├── .gitignore          # Git exclusions
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

The backend will be implemented using **Spring Boot** and will provide:

* RESTful API endpoints
* Authentication and authorization (JWT)
* Business logic and orchestration
* Integration with PostgreSQL

---

## Database

The system will use **PostgreSQL** as the primary relational database.

Key principles:

* Schema versioning via **Flyway**
* No direct schema changes in shared environments
* All changes must be versioned (`V1__init.sql`, etc.)

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
