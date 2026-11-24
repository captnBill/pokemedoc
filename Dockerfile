FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/pokemedoc-*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
# Free tiers usually give ~512 MB of RAM: cap the heap.
ENTRYPOINT ["java", "-Xmx300m", "-jar", "app.jar"]
