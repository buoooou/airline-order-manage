# --- 阶段 1: 构建 Angular 前端 ---
FROM node:20-alpine AS frontend-builder
WORKDIR /app

RUN npm install -g pnpm

# 复制依赖描述文件
COPY frontend/package.json frontend/pnpm-lock.yaml ./
# 安装依赖
RUN pnpm install
# 复制所有源代码
COPY frontend/ ./
# 构建应用
RUN pnpm run build

# --- 阶段 2: 构建 Spring Boot 后端 ---
FROM maven:3.8.5-openjdk-8 AS backend-builder
WORKDIR /app
# 缓存 Maven 依赖
COPY backend/pom.xml .
RUN mvn dependency:go-offline
# 复制后端源代码
COPY backend/src ./src

# (关键修正) 从 frontend-builder 阶段复制正确的构建产物路径
# 假设诊断出的路径是 /app/dist/frontend/browser
COPY --from=frontend-builder /app/dist/*/browser/* ./src/main/resources/static/
# 打包后端应用，此时前端文件已在 static 目录中
RUN mvn package -DskipTests

# --- 阶段 3: 创建最终的运行镜像 ---
FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
# 使用通配符复制 JAR 包
COPY --from=backend-builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]