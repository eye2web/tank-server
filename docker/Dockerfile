FROM azul/zulu-openjdk-alpine:11.0.3-jre
MAINTAINER PPF

EXPOSE 8080

COPY build/libs/*.jar /app/service.jar
COPY docker/start.sh /app/start.sh
COPY docker/retrieve_keystore.sh /app/retrieve_keystore.sh

ENTRYPOINT ["/app/start.sh"]
