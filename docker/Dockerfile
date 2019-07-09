FROM azul/zulu-openjdk-alpine:11.0.3-jre

EXPOSE 3000

COPY build/libs/*.jar /app/service.jar
COPY docker/start.sh /app/start.sh

ENTRYPOINT ["/app/start.sh"]
