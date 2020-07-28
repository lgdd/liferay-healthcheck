FROM azul/zulu-openjdk-alpine:8

WORKDIR /project
COPY . .
RUN ./gradlew clean build

FROM liferay/portal:7.2.1-ga2

COPY --chown=liferay:liferay --from=0 /project/build/libs/*.jar /mnt/liferay/files/osgi/modules/
