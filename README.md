# Project Manager API

A robust backend REST API built with **Spring Boot** and **Java** for managing projects, tasks, and teams. This system features secure authentication using **JWT via HttpOnly cookies**, role-based access control (Admin/User), and fully containerized deployment using **Docker**.

## 🚀 Key Features

* [cite_start]**Secure Authentication:** User registration, login, and logout using JWT stored in HttpOnly cookies (prevents XSS attacks)[cite: 1, 3].
* [cite_start]**Project Management:** Create projects, search by query, and manage details[cite: 1, 4].
* [cite_start]**Team Collaboration:** Invite users to projects via username or email and manage memberships[cite: 2, 6].
* [cite_start]**Task Tracking:** Create, update, delete, and assign tasks with priority levels and due dates[cite: 1, 11].
* [cite_start]**Comments System:** Discuss tasks directly with comments[cite: 2].
* **Role-Based Access:** Distinction between Project Admins and standard Users.

## 🛠️ Tech Stack

* **Language:** Java
* **Framework:** Spring Boot
* **Database:** PostgreSQL
* **Containerization:** Docker & Docker Compose
* [cite_start]**Documentation:** OpenAPI / Swagger [cite: 1]

## 📋 Prerequisites

Ensure you have the following installed on your machine:
* [Docker](https://www.docker.com/products/docker-desktop)
* [Git](https://git-scm.com/)

## ⚡ Installation & Running

This project uses **Docker Compose** to orchestrate both the Spring Boot application and the PostgreSQL database.

1.  **Clone the repository:**
    ```bash
    git clone <your-repo-url>
    cd <your-repo-name>
    ```
2. **Environment variables set up:**

 *Create and update ``.env`` file based on the included ``.env.template`` file*
```properties
DB_URL= "YOUR DATABASE URL"
DB_USERNAME= "YOUR DATABASE USERNAME"
JWT_SECRET= "YOUR SECRET KEY"

# Docker compose related vars
DB_PASSWORD= "YOUR DATABASE PASSWORD"
DB_NAME= "DB NAME"

```
3. **Build and Run:**
    Run the following command to build the images and start the containers:
    ```bash
    docker-compose up --build -d
    ```

4. **Access the API:**
    The server typically runs on: `http://localhost:8080`. You can access the OpenApi docs on `http://localhost:8080/swagger-ui/index.html`

## 🔌 Main API Endpoints

### [cite_start]Authentication [cite: 3]
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Register a new user account |
| `POST` | `/api/auth/login` | Login and receive HttpOnly JWT cookies |
| `POST` | `/api/auth/logout` | Logout (clears cookies & invalidates token) |
| `POST` | `/api/auth/refresh` | Refresh access token |

### [cite_start]Projects & Invitations [cite: 1, 2]
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/project` | Get all projects for the current user |
| `POST` | `/api/project` | Create a new project |
| `POST` | `/api/project/{id}/invite` | Invite a user (by username/email) |
| `POST` | `/api/project/{id}/join` | Accept or decline an invitation |

### [cite_start]Tasks [cite: 1, 4]
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/project/{pid}/tasks` | Get all tasks in a project |
| `POST` | `/api/project/{pid}/tasks` | Create a new task |
| `PATCH` | `/api/project/{pid}/tasks/{tid}`| Update task status, priority, or assignee |
| `GET` | `/api/tasks/assigned` | Get tasks assigned to the current user |

### [cite_start]Comments [cite: 2]
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/project/{pid}/tasks/{tid}/comments` | Add a comment to a task |
| `GET` | `/api/project/{pid}/tasks/{tid}/comments` | View task comments |

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).

