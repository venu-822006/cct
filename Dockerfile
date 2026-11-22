# ===================================================================
# Multi-Stage Production Dockerfile for Personal Cloud Drive
# Runs both React Frontend & Spring Boot Backend in a single container
# ===================================================================

# Stage 1: Build React Frontend
FROM node:20-alpine AS frontend-builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Build Spring Boot Backend
FROM maven:3.9.6-eclipse-temurin-21-alpine AS backend-builder
WORKDIR /app
COPY backend/pom.xml ./backend/
# Download dependencies for layer caching
RUN cd backend && mvn dependency:go-offline -B || true
COPY backend/src ./backend/src/
# Copy built frontend assets into Spring Boot's static resources
COPY --from=frontend-builder /app/dist/ ./backend/src/main/resources/static/
WORKDIR /app/backend
RUN mvn clean package -DskipTests

# Stage 3: Minimal JRE Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN apk add --no-cache bash curl && \
    mkdir -p /app/data /app/storage_root

COPY --from=backend-builder /app/backend/target/personal-cloud-drive.jar app.jar

ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=gcloud
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-gcloud} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
