import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.mavenPublish)
    id("dev.monorithm.language")
}

group = "dev.monorithm"
version = "0.0.1"

kotlin {
    explicitApi()
    compilerOptions {
        allWarningsAsErrors = true
    }

    jvm()
    android {
        namespace = "dev.monorithm.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.ui)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmTest.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

// The language: the submodule language/ at the commit of one tag of monorithm/design.
language {
    source = rootProject.layout.projectDirectory.dir("language")
    tokens = layout.projectDirectory.file("src/commonMain/kotlin/dev/monorithm/ui/tokens/Tokens.kt")
    packageName = "dev.monorithm.ui.tokens"
}

// The test of the tokens reads the pinned IR in the submodule language/.
val languageDir: String = rootProject.layout.projectDirectory.dir("language").asFile.path
tasks.named<Test>("jvmTest") {
    systemProperty("monorithm.language", languageDir)
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "ui", version.toString())

    pom {
        name = "Monorithm UI for Compose Multiplatform"
        description = "The Compose Multiplatform library of the Monorithm Experience Language."
        inceptionYear = "2026"
        url = "https://github.com/monorithm/compose"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "harnicode"
                name = "Harnicode"
                url = "https://github.com/monorithm"
            }
        }
        scm {
            url = "https://github.com/monorithm/compose"
            connection = "scm:git:https://github.com/monorithm/compose.git"
            developerConnection = "scm:git:ssh://git@github.com/monorithm/compose.git"
        }
    }
}
