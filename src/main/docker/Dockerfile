FROM anapsix/alpine-java
VOLUME /tmp
ADD schat-0.0.1.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
