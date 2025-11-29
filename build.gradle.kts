import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jreleaser.model.Active

plugins {
    `java-library`
    id("io.freefair.lombok") version "9.1.0"
    jacoco
    id("org.sonarqube") version "7.1.0.6387"
    `maven-publish`
    id("org.jreleaser") version "1.21.0"
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.compileJava {
    options.release = 17
}

group = "com.globalreachtech"
version = "2.3.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.log4j.api)
    implementation(libs.netty.codec.base)
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")

    testImplementation(libs.log4j.core)
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.20.0")
    testImplementation("org.awaitility:awaitility:4.3.0")
    testImplementation("org.assertj:assertj-core:3.27.6")
    testImplementation("net.jradius:jradius-core:1.1.5") {
        isTransitive = false // for reference hashing utilities
    }
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", "globalreachtech_tinyradius-netty")
        property("sonar.organization", "globalreachtech")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.exclusions", "'**/AttributeType*','**/PacketType*'")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

jreleaser {
    signing {
        active = Active.ALWAYS
        armored = true
    }
    deploy {
        maven {
            mavenCentral {
                register("sonatype") {
                    active = Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = groupId
            artifactId = project.name
            pom {
                name = project.name
                description = "TinyRadius-Netty is a small Java Radius library"
                url = "https://github.com/globalreachtech/tinyradius-netty"
                licenses {
                    license {
                        name = "GNU Lesser General Public License, version 2.1"
                        url = "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html"
                    }
                }
                developers {
                    developer {
                        id = "horaceli"
                        url = "https://github.com/horaceli"
                    }
                    developer {
                        id = "globalreachtech"
                        organizationUrl = "https://www.globalreachtech.com/"
                    }
                }
                scm {
                    url = "https://github.com/globalreachtech/tinyradius-netty"
                }
            }
        }
    }
    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}
