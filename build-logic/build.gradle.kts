plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("design") {
            id = "dev.monorithm.design"
            implementationClass = "dev.monorithm.design.DesignPlugin"
        }
    }
}
