plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "1.1.0"
  kotlin("plugin.spring") version "1.4.10"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  // TODO Pinned due to SSE Emitter bug in spring-web:5.2.10.RELEASE (a dependency of org.springframework.boot:2.3.5.RELEASE) - remove when >= org.springframework.boot:2.3.6.RELEASE
  implementation("org.springframework:spring-web:5.2.11.RELEASE")

  implementation("org.springdoc:springdoc-openapi-ui:1.4.8")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.4.8")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.4.8")

  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.0.23")
  testImplementation("io.projectreactor:reactor-test:3.4.0")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
}
