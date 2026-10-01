# Step 1: Build the Maven application
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run Tomcat / WAR file using Maven
CMD ["mvn", "org.apache.tomcat.maven:tomcat7-maven-plugin:2.2:run"]