plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.1.0"
  kotlin("plugin.spring") version "2.0.21"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.1.0")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.2.0")

  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
  implementation("org.awaitility:awaitility-kotlin:4.2.2")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.1.0")
  testImplementation("io.projectreactor:reactor-test:3.7.0")
  testImplementation("org.wiremock:wiremock-standalone:3.9.2")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("com.google.code.gson:gson:2.11.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.24") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("io.swagger.core.v3:swagger-core-jakarta:2.2.26")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}
