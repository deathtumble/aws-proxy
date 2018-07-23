FROM openjdk:8-jdk-alpine
VOLUME /tmp

COPY artifacts/startup-script /var/startup-script
COPY artifacts/aws_proxy_consul.json /var/aws_proxy_consul.json
COPY artifacts/aws_proxy_goss.yml /var/aws_proxy_goss.yml

ENV AWS_ACCESS_KEY_ID ''
ENV AWS_SECRET_ACCESS_KEY ''
ENV aws.region eu-west-1

EXPOSE 8080

ADD target/aws-proxy-0.1.0-SNAPSHOT.jar app.jar

VOLUME ["/opt/consul/conf/"]
VOLUME ["/etc/goss/"]

RUN cp /var/startup-script /usr/local/bin/startup-script
RUN chmod 774 /usr/local/bin/startup-script

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]