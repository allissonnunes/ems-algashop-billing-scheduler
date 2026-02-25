FROM eclipse-temurin:25-jre-jammy

ARG JAR_NAME=app.jar

WORKDIR /app

COPY build/libs/$JAR_NAME ./application.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar application.jar"]