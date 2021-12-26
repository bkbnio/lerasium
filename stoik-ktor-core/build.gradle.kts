plugins {
  id("io.bkbn.sourdough.library")
}

dependencies {
  val ktorVersion = "1.6.5"
  val logbackVersion = "1.2.7"
  api("io.ktor:ktor-server-core:$ktorVersion")
  api("io.ktor:ktor-server-netty:$ktorVersion")
  api("ch.qos.logback:logback-classic:$logbackVersion")
  api("io.ktor:ktor-serialization:$ktorVersion")
}
