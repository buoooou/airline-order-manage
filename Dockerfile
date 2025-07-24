# --- 阶段 1: 构建 Angular 前端 ---
FROM node:20-alpine as frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build -- --configuration production

# --- 阶段 2: 构建 Spring Boot 后端 ---
FROM maven:3.8.5-openjdk-8 as backend-builder
WORKDIR /app/backend
COPY backend/pom.xml ./
RUN mvn dependency:go-offline
COPY backend/ ./

# (重要修改) 不再拷贝到src目录，而是拷贝到target目录下的一个临时位置
COPY --from=frontend-builder /app/frontend/dist/frontend /app/backend/target/frontend

# 现在，让Maven来完成所有工作，包括通过新加的插件来复制前端文件
RUN mvn clean package -DskipTests

# --- 阶段 3: 创建最终的运行镜像 ---
FROM amazoncorretto:8-alpine-jdk
WORKDIR /app
COPY --from=backend-builder /app/backend/target/airline-order-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]