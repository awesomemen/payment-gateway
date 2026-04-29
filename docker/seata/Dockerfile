FROM apache/seata-server:2.2.0

ARG MYSQL_CONNECTOR_VERSION=8.0.27

USER root

RUN mkdir -p /lib/jdbc \
    && mkdir -p /seata-server/libs \
    && curl -fsSL -o /tmp/mysql-connector-java.jar "https://repo1.maven.org/maven2/mysql/mysql-connector-java/${MYSQL_CONNECTOR_VERSION}/mysql-connector-java-${MYSQL_CONNECTOR_VERSION}.jar" \
    && cp /tmp/mysql-connector-java.jar /lib/jdbc/mysql-connector-j-${MYSQL_CONNECTOR_VERSION}.jar \
    && cp /tmp/mysql-connector-java.jar /seata-server/libs/mysql-connector-j-${MYSQL_CONNECTOR_VERSION}.jar \
    && rm -f /tmp/mysql-connector-java.jar
