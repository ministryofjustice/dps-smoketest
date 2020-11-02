plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "1.0.6"
  kotlin("plugin.spring") version "1.4.10"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

  implementation("org.springdoc:springdoc-openapi-ui:1.4.8")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.4.8")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.4.8")

  testImplementation("io.jsonwebtoken:jjwt:0.9.1")

}
