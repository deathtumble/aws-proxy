FROM openjdk:8-jdk-alpine
VOLUME /tmp

COPY artifacts/dumb-init /usr/local/bin/dumb-init
COPY artifacts/startup-script /var/startup-script
COPY artifacts/aws-proxy-consul.json /var/aws-proxy-consul.json
COPY artifacts/aws-proxy-goss.yaml /var/aws-proxy-goss.yaml

RUN chmod 777 /usr/local/bin/* 

VOLUME ["/etc/consul/"]
VOLUME ["/etc/goss/"]

RUN cp /var/startup-script /usr/local/bin/startup-script
RUN chmod 774 /usr/local/bin/startup-script

ENV AWS_ACCESS_KEY_ID ''
ENV AWS_SECRET_ACCESS_KEY ''
ENV aws.region eu-west-1

EXPOSE 8081

ADD target/aws-proxy-0.1.0-SNAPSHOT.jar app.jar

ENTRYPOINT ["/usr/local/bin/dumb-init", "--"]
CMD ["/usr/local/bin/startup-script"]