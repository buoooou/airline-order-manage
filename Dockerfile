FROM node:18-alpine as frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build -- --configuration production

# --- 阶段 2: 构建 Spring Boot 后端 ---
FROM maven:3.8.5-openjdk-17 as backend-builder
WORKDIR /app/backend
COPY backend/pom.xml ./
RUN mvn dependency:go-offline
COPY backend/ ./
COPY --from=frontend-builder /app/frontend/dist/frontend /app/backend/src/main/resources/static
RUN mvn clean package -DskipTests

# --- 阶段 3: 创建最终的运行镜像 ---
FROM amazoncorretto:17-alpine-jdk
WORKDIR /app
COPY --from=backend-builder /app/backend/target/airline-order-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]