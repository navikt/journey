FROM gcr.io/distroless/java25-debian13@sha256:fae156597c6b6c0f563088a07eed364bdc2d9fbf935f463c3a48461bafbefa5b

WORKDIR /app

COPY typst-pdf /app/typst-pdf
COPY build/libs/journey-all.jar app.jar

ENV XDG_CACHE_HOME="/tmp"
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"

EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]
