plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "2.1.0"
  kotlin("plugin.spring") version "1.4.21"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("org.springdoc:springdoc-openapi-ui:1.5.1")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.5.1")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.5.1")

  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("io.swagger.parser.v3:swagger-parser-v2-converter:2.0.24")
  testImplementation("io.projectreactor:reactor-test:3.4.1")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
}
