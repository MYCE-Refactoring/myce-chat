FROM eclipse-temurin:21-jre

## 실행 경로
WORKDIR /app/myce-backend/chat

## 실행 파일
COPY ./api/build/libs/*.jar myce-chat.jar

## 실행 포트
EXPOSE 8031

ENTRYPOINT ["java", "-jar", "myce-chat.jar", "--spring.profiles.active=product"]