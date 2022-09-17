# Lerasium

Welcome to Lerasium, a highly opinionated service generator for Kotlin

## How to install

Lerasium publishes all releases to Maven Central. As such, using the release versions of `Lerasium` is as simple as
declaring the dependencies block of your `build.gradle.kts`

```kotlin
repositories {
  mavenCentral()
}

dependencies {
  ksp("io.bkbn:lerasium-api-processor:latest.release")
  implementation("io.bkbn:lerasium-api:latest.release")
}
```

In addition to publishing releases to Maven Central, a snapshot version gets published to GitHub Packages on every merge
to `main`. These can be consumed by adding the repository to your gradle build file. Instructions can be
found [here](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package)

### Add the generated code as a source set

TODO

## Lerasium In a Nutshell

TODO

## The Playground

In addition to the documentation available here, Lerasium has a working example available in the
playground module.  

Go ahead and fork the repo and run it directly to get a sense of what Lerasium can do!
