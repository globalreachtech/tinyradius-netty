import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jreleaser.model.Active

plugins {
    `java-library`
    `maven-publish`
    jacoco
    alias(libs.plugins.freefair.lombok)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.jreleaser)
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.compileJava {
    options.release = 17
}

group = "com.globalreachtech"
version = "3.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.log4j.api)
    implementation(libs.netty.codec.base)

    testImplementation(libs.log4j.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.awaitility)
    testImplementation(libs.assertj.core)
    testImplementation(libs.jradius.core) { isTransitive = false } // reference hashing utilities
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

            from(components["java"])

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
