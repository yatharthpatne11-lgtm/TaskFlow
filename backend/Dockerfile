# Step 1: Build the Maven application with Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run Tomcat / WAR file using Maven
CMD ["mvn", "org.apache.tomcat.maven:tomcat7-maven-plugin:2.2:run"]