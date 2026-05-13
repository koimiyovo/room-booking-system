# Stage 1: Build
FROM maven:3.9-eclipse-temurin-19 AS builder
WORKDIR /app

# Copy all pom.xml files first to cache dependency resolution
COPY pom.xml .
COPY domain/pom.xml domain/
COPY infrastructure-api/pom.xml infrastructure-api/
COPY infrastructure-persistence/pom.xml infrastructure-persistence/
COPY infrastructure-provider/pom.xml infrastructure-provider/
COPY bootstrap/pom.xml bootstrap/
RUN mvn dependency:go-offline -q

# Copy source and build
COPY domain/src domain/src
COPY infrastructure-api/src infrastructure-api/src
COPY infrastructure-persistence/src infrastructure-persistence/src
COPY infrastructure-provider/src infrastructure-provider/src
COPY bootstrap/src bootstrap/src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:19-jre AS runtime
WORKDIR /app
COPY --from=builder /app/bootstrap/target/bootstrap-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
