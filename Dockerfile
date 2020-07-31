FROM azul/zulu-openjdk-alpine:8

WORKDIR /project
COPY . .
RUN mkdir ~/.gradle && \
    echo "sonatypeUsername=" >> ~/.gradle/gradle.properties && \
    echo "sonatypePassword=" >> ~/.gradle/gradle.properties && \
    ./gradlew assemble

FROM liferay/portal:7.2.1-ga2

COPY --chown=liferay:liferay --from=0 /project/build/libs/*.jar /mnt/liferay/files/osgi/modules/
