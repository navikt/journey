FROM gcr.io/distroless/java25-debian13@sha256:7bd38f96c69b64b0889cee9e249288189aaa28d4998385e21d29fa8d34ef38c4

WORKDIR /app

COPY typst-pdf /app/typst-pdf
COPY build/libs/journey-all.jar app.jar

ENV XDG_CACHE_HOME="/tmp"
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"

EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]
