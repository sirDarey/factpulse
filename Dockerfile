# ---------- Build Stage ----------
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app

# Install Maven manually (because slim doesn't come with it)
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

# ---------- Runtime Stage ----------
FROM eclipse-temurin:17-jdk-jammy AS runtime

WORKDIR /app

# Copy built JAR from build stage
COPY --from=build /app/target/auth-*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]