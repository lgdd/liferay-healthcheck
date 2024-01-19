FROM azul/zulu-openjdk-alpine:8

RUN apk --update --no-cache add dos2unix 

WORKDIR /project
COPY . .
RUN dos2unix gradlew
RUN ./gradlew assemble -PsonatypeUsername -PsonatypePassword

FROM liferay/portal:7.4.3.109-ga109

COPY --chown=liferay:liferay --from=0 /project/build/libs/*.jar /mnt/liferay/files/osgi/modules/
