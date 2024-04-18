FROM cr.siemens.com/container-hardening-service/releases/openjre17:latest

ARG APP_USER
ARG APP_DIR
WORKDIR $APP_DIR
ADD docker/harden.sh / 
RUN wget https://truststore.pki.rds.amazonaws.com/eu-central-1/eu-central-1-bundle.pem --directory-prefix=$APP_DIR 
RUN keytool -import -alias docdb -cacerts -file $APP_DIR/eu-central-1-bundle.pem -noprompt -storepass changeit
COPY ./deploy/springboot.jks /app/
RUN chmod 700 /harden.sh \
	&& sh -c "/harden.sh" \
	&& rm -rf /harden.sh

VOLUME /tmp
EXPOSE 8080
#EXPOSE 8444
ADD target/*.jar app.jar
ENV JAVA_OPTS="-Xms256M -Xmx1024M"
USER $APP_USER
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar app.jar" ]
