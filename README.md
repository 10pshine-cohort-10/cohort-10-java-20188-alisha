# Contact Management System (Java Spring Boot + React.js)

A complete, production-structured Fullstack **Contact Management System** organized into separate **`frontend/`** and **`backend/`** directories. The backend follows a feature-based package layout (`auth`, `user`, `contact`) with shared `common`, `config` and `security` packages.

---

## Project Structure

```text
├── backend/                    # Java + Spring Boot 3.x + JPA/Hibernate + SQL Server
│   ├── database/
│   │   └── sqlserver_schema.sql
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/tenpearls/contactmanagement/
│   │   │   │   ├── ContactManagementApplication.java
│   │   │   │   ├── config/         # SecurityConfig
│   │   │   │   ├── common/
│   │   │   │   │   ├── exception/  # GlobalExceptionHandler (@RestControllerAdvice) & custom exceptions
│   │   │   │   │   ├── response/   # ApiErrorResponse
│   │   │   │   │   ├── audit/
│   │   │   │   │   └── util/
│   │   │   │   ├── security/       # JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetailsService
│   │   │   │   ├── auth/
│   │   │   │   │   ├── controller/ # AuthController
│   │   │   │   │   ├── service/    # AuthService, AuthServiceImpl
│   │   │   │   │   └── dto/        # LoginRequest, RegisterRequest, AuthResponse
│   │   │   │   ├── user/
│   │   │   │   │   ├── controller/ # UserController
│   │   │   │   │   ├── service/    # UserService, UserServiceImpl
│   │   │   │   │   ├── repository/ # UserRepository (Spring Data JPA)
│   │   │   │   │   ├── entity/     # User
│   │   │   │   │   └── dto/        # UserResponse, ChangePasswordRequest, UpdateProfileRequest
│   │   │   │   └── contact/
│   │   │   │       ├── controller/ # ContactController
│   │   │   │       ├── service/    # ContactService, ContactServiceImpl
│   │   │   │       ├── repository/ # ContactRepository (Spring Data JPA)
│   │   │   │       ├── entity/     # Contact, ContactEmail, ContactPhone
│   │   │   │       └── dto/        # ContactRequest
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── logback-spring.xml
│   │   └── test/
│   │       ├── java/com/tenpearls/contactmanagement/
│   │       │   ├── auth/           # AuthControllerTest, AuthServiceImplTest
│   │       │   ├── user/           # UserServiceImplTest
│   │       │   └── contact/        # ContactControllerTest, ContactServiceImplTest, ContactRepositoryTest
│   │       └── resources/
│   │           └── application.yml
│   └── pom.xml                 # Maven dependencies, JaCoCo, SonarQube plugin
│
├── frontend/                   # React.js + TypeScript + Tailwind CSS SPA
│   ├── public/                 # Static assets & rounded white-background favicon
│   ├── src/
│   │   ├── components/         # AuthModal, ContactList, ContactFormModal, DeleteConfirmModal, ContactDetailModal, UserProfileView, ChangePasswordModal, Navbar, Toast
│   │   ├── data/               # Country list & sample contacts
│   │   ├── services/           # Spring Boot REST API client (api.ts), storage, Slf4j-style logger
│   │   └── types/              # TypeScript interfaces
│   ├── .env.example            # Frontend environment variables (copy to .env)
│   ├── index.html
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.ts          # Includes /api proxy -> http://localhost:8080
│
├── .coderabbit.yaml            # Automated code review configuration
├── .gitignore                  # Single root-level ignore file for backend and frontend
├── README.md
└── sonar-project.properties    # SonarQube quality rules for Java & React/TypeScript
```

---

## Technology Stack

- **Backend**: Java (JDK 17 or newer), Spring Boot 3.x, Spring Web, Spring Data JPA, Hibernate 6, Spring Security + JWT
- **Database**: Microsoft SQL Server (`sqlserver_schema.sql`)
- **Logging**: SLF4J + Logback (`logback-spring.xml` with console + rolling file appenders)
- **Testing**: JUnit 5, Mockito, Spring MockMvc, `@DataJpaTest`
- **Code Quality**: SonarQube (`sonar-project.properties` + `sonar-maven-plugin` + JaCoCo)
- **Frontend**: React 19, TypeScript, Tailwind CSS, Vite, Lucide Icons
- **Version Control**: Git (feature branching, pull requests, and `main`/`develop` merge workflow)

---

## Running Locally

Run every command block below from the repository root.

### 1. Database Setup (Microsoft SQL Server)
Execute `backend/database/sqlserver_schema.sql` in SQL Server Management Studio (SSMS) or `sqlcmd`. Use a dedicated database for this project.

Then set the required environment variables (see below) before starting the backend.

#### Environment Variables (Backend)

The backend does **not** contain any hardcoded passwords or secrets. `DB_PASSWORD` and `JWT_SECRET` are **required**, and the application will not start without them.

| Variable | Required | Description |
|----------|----------|-------------|
| `DB_PASSWORD` | Yes | SQL Server password |
| `JWT_SECRET` | Yes | JWT signing key (minimum 32 characters) |
| `DB_HOST` | No | Database host (default: `localhost`) |
| `DB_PORT` | No | Database port (default: `1433`) |
| `DB_NAME` | No | Database name (default: `ContactManagementDb`) |
| `DB_USER` | No | Database user (default: `sa`) |
| `DB_TRUST_SERVER_CERT` | No | Set to `true` only for a local SQL Server with a self-signed certificate (default: `false`) |
| `SERVER_PORT` | No | Backend port (default: `8080`) |
| `CORS_ALLOWED_ORIGINS` | No | Allowed frontend origins (default: `http://localhost:3000,http://localhost:5173`) |

Example (Windows PowerShell):
```powershell
$env:DB_PASSWORD="your_database_password"
$env:JWT_SECRET="your_random_secret_of_at_least_32_characters"
$env:DB_TRUST_SERVER_CERT="true"   # local self-signed SQL Server only
```

Example (Linux / macOS):
```bash
export DB_PASSWORD="your_database_password"
export JWT_SECRET="your_random_secret_of_at_least_32_characters"
export DB_TRUST_SERVER_CERT="true"   # local self-signed SQL Server only
```

### 2. Run Java Spring Boot Backend & Tests
```bash
cd backend
mvn clean test          # Executes JUnit 5 & Mockito tests + generates JaCoCo coverage report
mvn spring-boot:run     # Starts REST API on http://localhost:8080
cd ..
```

### 3. Run SonarQube Analysis
With SonarScanner CLI installed:
```bash
sonar-scanner -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<TOKEN>
```

### 4. Run React.js Frontend
```bash
cd frontend
npm install
npm run dev             # Starts Vite dev server on http://localhost:3000 (proxies /api -> localhost:8080)
```

The frontend supports two modes:

- **Local mode (default):** contacts and accounts are kept in the browser's local storage. No backend is needed.
- **Backend mode:** copy `frontend/.env.example` to `frontend/.env`, set `VITE_USE_BACKEND_API=true`, and start the Spring Boot API first. Contacts and accounts are then stored in SQL Server. `VITE_API_BASE_URL` is optional; in development the Vite proxy forwards `/api` to `http://localhost:8080`.

---

## Recommended Git Branching Strategy
- `main`: Production-ready releases
- `develop`: Integration branch for completed features
- `feature/<ticket-name>`: Isolated feature branches (e.g., `feature/contact-pagination`, `feature/jwt-auth`)
- `bugfix/<issue>`: Targeted fixes merged back via Pull Request after SonarQube & JUnit checks pass