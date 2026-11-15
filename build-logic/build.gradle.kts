plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("language") {
            id = "dev.monorithm.language"
            implementationClass = "dev.monorithm.language.LanguagePlugin"
        }
    }
}
