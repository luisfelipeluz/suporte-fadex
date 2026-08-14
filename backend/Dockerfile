# =============================================================================
# Backend — Spring Boot 3.5 / Java 21
# =============================================================================
# Build em multi-stage: a imagem final carrega apenas o JRE e o jar, sem Maven
# nem código-fonte.

# --- Estágio 1: compilação ---------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# As dependências mudam bem menos que o código: copiar o pom primeiro faz o
# Docker reaproveitar a camada de download entre builds.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src

# Os testes já rodam no fluxo de desenvolvimento e exigiriam mais tempo aqui;
# a imagem apenas empacota o que foi validado.
RUN mvn -B -ntp clean package -DskipTests

# --- Estágio 2: execução -----------------------------------------------------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Não roda como root.
RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=build /build/target/*.jar app.jar

RUN chown spring:spring app.jar
USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
