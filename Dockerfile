# MOVRIT 백엔드 이미지입니다(Java 17, Spring Boot).
# 1단계에서 jar를 만들고, 2단계에는 실행에 필요한 JRE와 jar만 담습니다. 비밀값은 이미지에 넣지 않고 실행할 때 환경 변수로 받습니다.
# 빌드: docker build -t movrit-backend .
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
# 의존성 정의를 먼저 복사해, 소스만 바뀌었을 때는 내려받은 라이브러리 층을 다시 씁니다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test && cp build/libs/movrit-*-SNAPSHOT.jar app.jar

FROM eclipse-temurin:17-jre
# 관리자 권한이 아닌 전용 사용자로 실행합니다. 업로드 파일은 /data(볼륨)에 둡니다.
RUN useradd --system --uid 1001 movrit && mkdir -p /data && chown movrit /data
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER movrit
ENV UPLOAD_DIR=/data/uploads \
    PRIVATE_UPLOAD_DIR=/data/uploads-private \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Seoul"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
