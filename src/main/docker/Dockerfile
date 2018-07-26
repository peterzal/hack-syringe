FROM openjdk:8-jdk-alpine
ARG BUILD_VERSION

ADD syringe-${BUILD_VERSION}.jar syringe.jar

ENTRYPOINT exec java -jar -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap /syringe.jar

EXPOSE 8090