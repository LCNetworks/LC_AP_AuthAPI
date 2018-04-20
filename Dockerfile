FROM java:8

ARG app=sso-oauth2
ENV logging.file=logs/${app}.log

COPY ${app}.jar /app/app.jar

WORKDIR /app

CMD ["java", "-jar", "app.jar"]