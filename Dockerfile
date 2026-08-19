FROM gcr.io/distroless/java25-debian13@sha256:ddebd80c83f2bd441f33ed907e3ba700ed3c16c57696b1c2c41f0bf0badc40f3

WORKDIR /app

COPY typst-pdf /app/typst-pdf
COPY build/libs/journey-all.jar app.jar

ENV XDG_CACHE_HOME="/tmp"
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"

EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]
