FROM maven:3.9.11-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY . .

RUN mvn --batch-mode -q -gs .mvn/global-settings.xml -s .mvn/settings.xml -pl gateway-app -am package -Dmaven.test.skip=true
RUN mkdir -p /app && cp gateway-app/target/gateway-app.jar /app/app.jar
RUN cd /app && java -Djarmode=tools -jar app.jar extract && rm app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app/app

COPY --from=builder /app/app /app/app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
