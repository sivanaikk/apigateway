FROM tomcat:8.0
USER root
WORKDIR /usr/local/tomcat/webapps/
RUN \
apt-get update && \
apt-get install unzip wget -y && \
rm -rf /var/lib/apt/lists/*
RUN wget -O /usr/local/tomcat/webapps/github-actions-artifact.zip  \
&& unzip '/usr/local/tomcat/webapps/github-actions-artifact.zip' -d /usr/local/tomcat/webapps/ && rm /usr/local/tomcat/webapps/github-actions-artifact.zip || true;
ENV JAVA_HOME=/usr/bin/java
WORKDIR /usr/local/tomcat/webapps
USER  root
EXPOSE 8080
CMD ['catalina.sh','run']
