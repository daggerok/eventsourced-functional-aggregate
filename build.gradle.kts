import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
  java
  id("io.franzbecker.gradle-lombok") version "2.1"
  id("org.springframework.boot") version "2.2.0.BUILD-SNAPSHOT"
  id("io.spring.dependency-management") version "1.0.7.RELEASE"
}

tasks.withType(Wrapper::class.java) {
  val gradleWrapperVersion: String by project
  gradleVersion = gradleWrapperVersion
  distributionType = Wrapper.DistributionType.BIN
}

java {
  val javaVersion = JavaVersion.VERSION_12
  sourceCompatibility = javaVersion
  targetCompatibility = javaVersion
}

repositories {
  mavenCentral()
  maven { url = uri("https://repo.spring.io/milestone/") }
  maven { url = uri("https://repo.spring.io/snapshot/") }
}

lombok {
  val lombokVersion: String by project
  version = lombokVersion
}

val vavrVersion: String by project
val junitJupiterVersion: String by project

dependencies {
  implementation("org.springframework.boot:spring-boot-starter")
  testImplementation("org.springframework.boot:spring-boot-starter-test")

  implementation("io.vavr:vavr:$vavrVersion")
  annotationProcessor("org.projectlombok:lombok")

  testImplementation(platform("org.junit:junit-bom:$junitJupiterVersion"))
  testRuntime("org.junit.platform:junit-platform-launcher")
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
  testImplementation("junit:junit")
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    showExceptions = true
    showStandardStreams = true
    events(PASSED, SKIPPED, FAILED)
  }
}

tasks.create<Zip>("sources") {
  dependsOn("clean")
  shouldRunAfter("clean", "assemble")
  description = "Archives sources in a zip file"
  group = "Archive"
  from("src") {
    into("src")
  }
  from(".gitignore")
  from(".java-version")
  from(".travis.yml")
  from("build.gradle.kts")
  from("pom.xml")
  from("README.md")
  from("settings.gradle.kts")
  archiveFileName.set("${project.buildDir}/sources-${project.version}.zip")
}

tasks {
  named("clean") {
    doLast {
      delete(
          project.buildDir,
          "${project.projectDir}/out"
      )
    }
  }
}

defaultTasks("clean", "sources", "build")
