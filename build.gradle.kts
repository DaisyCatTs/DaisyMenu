import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val ktlint by configurations.creating

plugins {
    kotlin("jvm") version "2.2.21"
    `maven-publish`
    `java-library`
}

group = "com.github.fu3i0n"
version = "1.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

val versions =
    mapOf(
        "paperApi" to "1.21.10-R0.1-SNAPSHOT",
        "kotlin" to "2.2.21",
        "ktlint" to "1.8.0",
        "coroutines" to "1.10.2",
    )

dependencies {
    compileOnly("io.papermc.paper:paper-api:${versions["paperApi"]}")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:${versions["kotlin"]}")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions["coroutines"]}")

    ktlint("com.pinterest.ktlint:ktlint-cli:${versions["ktlint"]}") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

val targetJavaVersion = 21

kotlin {
    jvmToolchain(targetJavaVersion)
}

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all",
        )
        jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
    }
}

val ktlintCheck by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("**/src/**/*.kt", "**.kts", "!**/build/**")
}

tasks {
    check {
        dependsOn("ktlintFormat")
    }

    register<JavaExec>("ktlintFormat") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Check Kotlin code style and format"
        classpath = ktlint
        mainClass.set("com.pinterest.ktlint.Main")
        jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
        args("-F", "**/src/**/*.kt", "**.kts", "!**/build/**")
    }

    val jarDir = layout.projectDirectory.dir("Jar")
    val projectVersion = version.toString()

    register<Copy>("copyToJar") {
        from(jar)
        into(jarDir)
        rename { "DaisyMenu-$projectVersion.jar" }
    }

    build {
        finalizedBy("copyToJar")
    }

    jar {
        manifest {
            attributes(
                "Implementation-Title" to "DaisyMenu",
                "Implementation-Version" to version,
                "Implementation-Vendor" to "fu3i0n",
            )
        }
    }
}

// JitPack compatible publishing
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "com.github.fu3i0n"
            artifactId = "DaisyMenu"
            version = project.version.toString()

            pom {
                name.set("DaisyMenu")
                description.set("The #1 Kotlin GUI Library for Paper Minecraft Servers")
                url.set("https://github.com/fu3i0n/DaisyMenu")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("fu3i0n")
                        name.set("fu3i0n")
                        url.set("https://github.com/fu3i0n")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/fu3i0n/DaisyMenu.git")
                    developerConnection.set("scm:git:ssh://github.com/fu3i0n/DaisyMenu.git")
                    url.set("https://github.com/fu3i0n/DaisyMenu")
                }
            }
        }
    }
}
