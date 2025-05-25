# 🐞 Bugwise

This is the backend system for **Bugwise**, an intelligent bug tracking and project management platform. Built with Spring Boot, the system supports collaborative development workflows for Developers, Testers, Admins, and Project Managers.

---

## 🧩 Architecture Overview

![Bugwise Architecture](Bugwise%20Application%20Architechture.png)

### Key Components: 
- **Backend**: Spring Boot microservices
- **Database**: PostgreSQL
- **Cache & Search**: Redis and Elasticsearch
- **Messaging**: WebSockets + RabbitMQ
- **AI Analysis**: Spring AI + OpenAI
- **Security**: JWT-based authentication and authorization

---

## 🛠 Technologies Used

- Java 21
- Spring Boot
- Spring Security (JWT Authentication)
- PostgreSQL
- Redis
- Elasticsearch
- RabbitMQ
- WebSocket
- OpenAI API (Spring AI)
- Docker, Docker Compose

--- 

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven
- Docker & Docker Compose

### Run Locally

```bash
# Clone the repository
git clone https://github.com/your-org/bugwise-backend.git
cd bugwise-backend

# Copy environment config
cp .env.example .env

# Build and run with Docker
docker-compose up --build

# Build the project
./mvnw clean install

# Run the backend
./mvnw spring-boot:run
