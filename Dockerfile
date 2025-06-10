FROM ubuntu:latest AS build

RUN apt-get update && apt-get install -y openjdk-21-jdk maven

COPY . .

RUN mvn clean install -U

EXPOSE 8080

ENTRYPOINT ["java","-jar","target/your-app.jar"]
