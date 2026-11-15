import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.mavenPublish)
    id("dev.monorithm.design")
}

group = "dev.monorithm"
version = "0.0.1"

kotlin {
    explicitApi()

    // The compilations of each platform stop on each warning. The shared metadata compilations do not: there, the
    // KLIB loader warns about the AndroidX libraries that JetBrains and Google each publish for Compose Multiplatform.
    targets.matching { it.platformType != KotlinPlatformType.common }.configureEach {
        compilations.configureEach {
            compileTaskProvider.configure { compilerOptions.allWarningsAsErrors.set(true) }
        }
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
            implementation(libs.compose.foundation)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmTest.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}

// The design: the submodule design/ at the commit of one tag of monorithm/design.
design {
    source = rootProject.layout.projectDirectory.dir("design")
    tokens = layout.projectDirectory.file("src/commonMain/kotlin/dev/monorithm/ui/Tokens.kt")
    packageName = "dev.monorithm.ui"
}

// The test of the tokens reads the pinned IR in the submodule design/.
val designDir: String = rootProject.layout.projectDirectory.dir("design").asFile.path
tasks.named<Test>("jvmTest") {
    systemProperty("monorithm.design", designDir)
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
