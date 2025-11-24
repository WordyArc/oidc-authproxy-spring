plugins {
	kotlin("jvm") version "2.3.0-RC"
	kotlin("plugin.spring") version "2.2.21"
	id("io.spring.dependency-management") version "1.1.7"
    `java-library`
    `maven-publish`
}

group = "dev.owlmajin"
version = "0.0.1"
description = "OIDC AuthProxy Spring Boot starter"


val springBootVersion by extra("4.0.0")

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    implementation("org.springframework:spring-webflux")

    compileOnly("org.springframework.boot:spring-boot-starter-webflux")
    compileOnly("org.springframework.boot:spring-boot-starter-web")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")


    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = rootProject.name
            version = project.version.toString()
        }
    }
}