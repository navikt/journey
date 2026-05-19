# journey
[![Deploy to dev and prod](https://github.com/navikt/syk-dig-backend/actions/workflows/deploy.yml/badge.svg)](https://github.com/navikt/syk-dig-backend/actions/workflows/deploy.yml)

This app reads from the tsm.tsm-sykmelding topic and creates a journalpost in Dokarkiv.

## Technologies used
* Kotlin
* Spring boot
* Gradle
* Junit
* Typst

#### Requirements

* JDK 21
* Docker
## Getting started
### Building the application
#### Compile and package application
To build locally and run the integration tests you can simply run
``` bash
./gradlew bootJar
```
or  on windows
`gradlew.bat bootJar`

### Local development for PDF template
Download and install typst: https://typst.app/open-source/#download

To render the template locally with JSON input, run:
```bash
typst watch --pdf-standard=a-2a --font-path=fonts --input=data-path="$(pwd)/typst-pdf/test-data/sm.json" typst-pdf/sm.typ sm-local.pdf
```

### Contact

This project is maintained by [CODEOWNERS](CODEOWNERS)

Questions and/or feature requests? Please create an [issue](https://github.com/navikt/syk-dig-backend/issues)

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997)
