# Setting up your project

First of all, you will need to create a gradle-based Kotlin project with the followings requirements

1. Java 17 or higher
2. Kotlin 1.8 or higher
3. Gradle 7 or higher

We recommend using our application starter, [kt-sourdough](https://github.com/bkbnio/kt-sourdough), to get you started.
However, you are free to use any other starter you prefer.

It is highly recommended to start with a clean project, but this also is not a requirement.

# Adding Lerasium to your project

Once you have a project setup, you will need to add the following to your `build.gradle.kts` file of the module you wish
to use as your lerasium application:

In your plugins block, add the [KSP]() plugin

```kotlin
plugins {
  // ...
  id("com.google.devtools.ksp") version "{{ Match version with Kotlin Version }}"
}
```

Then add the lerasium dependencies to your dependencies block

```kotlin 
dependencies {
  // ...
  // Core Annotations + Processor
  ksp("io.bkbn:lerasium-core-processor:0.5.0")
  implementation("io.bkbn:lerasium-core:0.5.0")

  // Relational Database Annotations + Processor
  ksp("io.bkbn:lerasium-rdbms-processor:0.5.0")
  implementation("io.bkbn:lerasium-rdbms:0.5.0")

  // REST API Annotations + Processor
  ksp("io.bkbn:lerasium-api-processor:0.5.0")
  implementation("io.bkbn:lerasium-api:0.5.0")
}
```

# Creating your first domain

Inside your source package, create a new file called `User.kt` and add the following code:

```kotlin
@Domain
interface User {
  val 
