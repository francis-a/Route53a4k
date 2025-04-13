plugins {
    kotlin("jvm") version "2.1.10"
    application
}

group = "net.eyecu.dyn.route53a4k"
version = "0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    implementation("org.jobrunr:jobrunr:7.4.1")
    implementation("software.amazon.awssdk:route53:2.30.26")
    implementation("software.amazon.awssdk:sts:2.30.26")
    implementation("org.slf4j:slf4j-simple:2.0.16")

    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:5.12.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk-jvm:1.13.17") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.jupiter", module = "junit-jupiter")
    }

}

application {
    mainClass.set("net.eyecu.dyn.Route53a4k")
}

kotlin {
    jvmToolchain(17)
}

tasks {

    test {
        useJUnitPlatform()
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    jar {
        archiveClassifier = "app"
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes(mapOf("Main-Class" to application.mainClass))
        }
    }
}
