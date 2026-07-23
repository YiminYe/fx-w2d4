# ── Activity 1 end state ──────────────────────────────────────────────────────────
# Multi-stage: build with Maven+JDK, ship JRE-only.
#
# Stage 1 needs Maven, a full JDK, your source and the whole .m2 cache. None of that
# is needed to RUN the app, and none of it reaches the final image.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# pom.xml first, on its own layer. Dependencies change rarely; source changes constantly.
# Docker only re-runs a layer when the files it copies have changed — so editing a
# controller does NOT re-download the internet.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# Stage 2: a fresh, small base. Only the jar crosses over.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Documentation, not a firewall rule: EXPOSE records the port, -p publishes it.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
