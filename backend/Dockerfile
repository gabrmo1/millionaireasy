# Estágio 1: Build da aplicação com o Gradle
FROM gradle:8.5.0-jdk21-jammy AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle build --no-daemon

# Estágio 2: Criação da imagem final com a JRE
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copia o JAR construído do estágio anterior
COPY --from=build /home/gradle/src/build/libs/*.jar application.jar
EXPOSE 8080
# Define o comando para rodar a aplicação
ENTRYPOINT ["java", "-jar", "application.jar"]