FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN ./gradlew build -x test

ENTRYPOINT ["java","-jar","build/libs/fitplate-api-0.0.1-SNAPSHOT.jar"]