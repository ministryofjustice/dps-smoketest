plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.0.1"
  kotlin("plugin.spring") version "2.0.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.0.2-beta-3")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:4.1.0")

  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
  implementation("org.awaitility:awaitility-kotlin:4.2.1")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.0.2-beta-3")
  testImplementation("io.projectreactor:reactor-test:3.6.6")
  testImplementation("org.wiremock:wiremock-standalone:3.7.0")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("com.google.code.gson:gson:2.11.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.22") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("io.swagger.core.v3:swagger-core-jakarta:2.2.22")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}
