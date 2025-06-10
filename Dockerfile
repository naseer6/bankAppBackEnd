# Use official OpenJDK 21 image
FROM openjdk:21-jdk AS build
COPY . .
RUN ./mvnw clean install -U
EXPOSE 8080
ENTRYPOINT ["./mvnw", "spring-boot:run"]