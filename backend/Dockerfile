# ---- Step 1: build the WAR with Maven + Java 21 ----
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Step 2: run it on Tomcat 9 (supports Java 21 and javax.servlet) ----
FROM tomcat:9.0-jdk21-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/api.war /usr/local/tomcat/webapps/ROOT.war

# Keep memory low enough for Render's free plan (512 MB)
ENV JAVA_OPTS="-Xmx350m -Xss512k"

COPY start.sh /usr/local/tomcat/start.sh
RUN sed -i 's/\r$//' /usr/local/tomcat/start.sh && chmod +x /usr/local/tomcat/start.sh
CMD ["/usr/local/tomcat/start.sh"]