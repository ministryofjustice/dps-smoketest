plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.10.0"
  kotlin("plugin.spring") version "1.9.21"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-cache")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:2.1.1")

  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
  implementation("org.awaitility:awaitility-kotlin:4.2.0")

  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.3")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.3")
  testImplementation("io.projectreactor:reactor-test:3.6.0")
  testImplementation("org.wiremock:wiremock-standalone:3.3.1")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("com.google.code.gson:gson:2.10.1")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.19") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("io.swagger.core.v3:swagger-core-jakarta:2.2.19")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "21"
    }
  }
}
