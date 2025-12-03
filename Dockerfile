# ---------- Etapa 1: Build ----------
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copiamos el pom y descargamos dependencias (cacheable)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiamos el resto del proyecto y construimos
COPY src ./src
RUN mvn clean package -DskipTests

# ---------- Etapa 2: Runtime ----------
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

# Copiamos el JAR desde la imagen anterior
COPY --from=builder /app/target/*.jar app.jar

# Variable de entorno para la base de datos (puedes sobreescribir desde docker-compose)
ENV SPRING_PROFILES_ACTIVE=prod

# Exponemos el puerto del backend (por ejemplo, 8080)
EXPOSE 8080

# Comando para ejecutar la app
ENTRYPOINT ["java", "-jar", "app.jar"]
