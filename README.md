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

* Docker
* JDK 21
* Typst

Run `mise i` to install required dependencies.

## Getting started
### Building the application
#### Compile and package application
To build locally and run the integration tests you can simply run
```bash
./gradlew bootJar
```
or  on windows
`gradlew.bat bootJar`

### Working with PDF generation

You can work with PDF generation locally by using typst built in watcher:

```bash
typst watch --pdf-standard=a-2a --root=/ --font-path typst-pdf/fonts --input=data-path=test-data/sykmelding.json typst-pdf/sykmelding.typ
```

Then you can open ./typst-pdf/sykmelding.pdf. Use a PDF-viewer that supports auto-reloading for the best experience.

If you do changes to the payload generation, you can re-run `updateTypstTestData` in `no/nav/journey/pdf/TypstClientTest.kt`, this will update `test-data/sykmelding.json`. You must re-start the watcher after changing the test-data.

### Contact

This project is maintained by [CODEOWNERS](CODEOWNERS)

Questions and/or feature requests? Please create an [issue](https://github.com/navikt/syk-dig-backend/issues)

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997)