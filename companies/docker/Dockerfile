FROM adoptopenjdk/openjdk8:latest

ADD companies-service.jar app.jar

CMD java -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=7000,suspend=n -jar /app.jar

EXPOSE 8080 7000