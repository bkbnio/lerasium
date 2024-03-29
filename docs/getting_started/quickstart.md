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
  ksp("io.bkbn:lerasium-core-processor:latest.release")
  implementation("io.bkbn:lerasium-core:latest.release")

  // Relational Database Annotations + Processor
  ksp("io.bkbn:lerasium-rdbms-processor:latest.release")
  implementation("io.bkbn:lerasium-rdbms:latest.release")

  // REST API Annotations + Processor
  ksp("io.bkbn:lerasium-api-processor:latest.release")
  implementation("io.bkbn:lerasium-api:latest.release")
}
```

# Creating your first domain

Inside your source package, create a new file called `User.kt` and add the following code:

```kotlin
import io.bkbn.lerasium.api.Api
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Sensitive
import io.bkbn.lerasium.core.auth.Actor
import io.bkbn.lerasium.core.auth.Password
import io.bkbn.lerasium.core.auth.Username
import io.bkbn.lerasium.core.domain.LerasiumDomain
import io.bkbn.lerasium.persistence.Index
import io.bkbn.lerasium.rdbms.Table

@Api
@Actor
@Domain("User")
@Table(name = "users")
interface User : LerasiumDomain {
  @Username
  @Index(true)
  val email: String

  @Password
  @Sensitive
  val password: String
}
```

TODO Continue
