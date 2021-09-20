FROM openjdk:8
COPY  target/  target/
RUN  mv target/*.jar   target/apigateway.jar
EXPOSE 9097
ENTRYPOINT ["java", "-jar", "target/apigateway.jar"]
