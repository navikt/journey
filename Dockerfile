FROM gcr.io/distroless/java25-debian13@sha256:d000221060080b5691b1521d3113f701bd3e0041c6b792a00cf9006d8d3b4ec0

WORKDIR /app

COPY typst-pdf /app/typst-pdf
COPY build/libs/journey-all.jar app.jar

ENV XDG_CACHE_HOME="/tmp"
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"

EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]
