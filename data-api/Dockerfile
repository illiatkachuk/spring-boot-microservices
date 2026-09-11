# ---------- Stage 1: build the jar with Maven and a full JDK ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy only pom.xml first and download dependencies. Docker caches this layer,
# so dependencies are downloaded again only when pom.xml changes, not on every code edit.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Stage 2: run on a JRE-only image ----------
# Nothing from stage 1 ends up here except the jar: no Maven, no JDK, no source code.
# Ubuntu-based rather than -alpine: the alpine JRE 17 image is published for amd64 only,
# while this one also runs natively on arm64 (Apple Silicon).
FROM eclipse-temurin:17-jre
WORKDIR /app

# Don't run the application as root inside the container.
RUN groupadd --system app && useradd --system --gid app app
USER app

COPY --from=build /workspace/target/data-api.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
