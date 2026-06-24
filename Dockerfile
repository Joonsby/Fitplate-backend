FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test --no-daemon

ENTRYPOINT ["java","-Duser.timezone=Asia/Seoul","-jar","build/libs/fitplate-api-0.0.1-SNAPSHOT.jar"]