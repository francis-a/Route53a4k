import org.panteleyev.jpackage.ImageType.APP_IMAGE

plugins {
    kotlin("jvm") version "2.1.21"
    application
    id("org.panteleyev.jpackageplugin") version "1.6.1"
}


group = "net.eyecu.dyn.route53a4k"
version = "0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    implementation("org.jobrunr:jobrunr:7.5.1")
    implementation("software.amazon.awssdk:route53:2.31.50")
    implementation("software.amazon.awssdk:sts:2.31.54")
    implementation("org.slf4j:slf4j-simple:2.0.17")

    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk-jvm:1.14.2") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.jupiter", module = "junit-jupiter")
    }

}

application {
    applicationName = "Route53a4k"
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
        archiveClassifier = ""
        version = ""
        archiveBaseName = ""
        archiveFileName = "${application.applicationName}.jar"
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes(mapOf("Main-Class" to application.mainClass))
        }
    }

    jpackage {
        appName = application.applicationName

        version = ""
        mainClass = application.mainClass.get()
        mainJar = jar.get().archiveFileName.get()

        destination = layout.buildDirectory.dir("jpackage").get().asFile.absolutePath
        type = APP_IMAGE
        removeDestination = true
    }

}