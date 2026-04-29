FROM apache/skywalking-oap-server:10.1.0

ARG MYSQL_CONNECTOR_VERSION=8.0.27

USER root

RUN mkdir -p /skywalking/oap-libs /skywalking/libs \
    && curl -fsSL -o /tmp/mysql-connector-java.jar "https://repo1.maven.org/maven2/mysql/mysql-connector-java/${MYSQL_CONNECTOR_VERSION}/mysql-connector-java-${MYSQL_CONNECTOR_VERSION}.jar" \
    && cp /tmp/mysql-connector-java.jar /skywalking/oap-libs/mysql-connector-java-${MYSQL_CONNECTOR_VERSION}.jar \
    && cp /tmp/mysql-connector-java.jar /skywalking/libs/mysql-connector-java-${MYSQL_CONNECTOR_VERSION}.jar \
    && rm -f /tmp/mysql-connector-java.jar
