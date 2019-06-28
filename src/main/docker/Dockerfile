FROM digitalgenius/alpine-jdk
ADD eth-explorer-*.jar app.jar
VOLUME [ "/logs" ]
EXPOSE 8000
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
#WORKDIR eth-explorer-boot
#ENTRYPOINT ["./bin/eth-explorer"]
