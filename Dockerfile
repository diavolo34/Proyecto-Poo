# Servidor Java de ConstruTrack para publicarlo en internet (Render u otro hosting con Docker).
# Etapa 1: compila el código (sin librerías externas).
FROM eclipse-temurin:21-jdk AS compilacion
WORKDIR /app
COPY ConstruTrack/src ./src
RUN javac --release 17 -encoding UTF-8 -d out $(find src -name "*.java") \
 && cd src && find . -type f ! -name "*.java" -exec cp --parents {} ../out/ \;

# Etapa 2: imagen liviana solo con lo necesario para ejecutar.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compilacion /app/out ./out
# Render define la variable PORT; los datos se guardan en el disco temporal del contenedor.
ENV DATOS=/tmp/datos_construtrack.txt
EXPOSE 8080
CMD ["java", "-Djava.awt.headless=true", "-cp", "out", "construtrack.App", "--sin-ventana"]
