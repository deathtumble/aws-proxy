FROM openjdk:8-jdk-alpine
VOLUME /tmp

COPY artifacts/concourse_web_consul.json /var/concourse_web_consul.json
COPY artifacts/concourse_goss.yml /var/concourse_goss.yml

ENV AWS_ACCESS_KEY_ID ''
ENV AWS_SECRET_ACCESS_KEY ''
ENV aws.region eu-west-1

EXPOSE 8080

ADD target/aws-proxy-0.1.0-SNAPSHOT.jar app.jar

VOLUME ["/opt/consul/conf/"]
VOLUME ["/etc/goss/"]

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]