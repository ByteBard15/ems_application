plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency)
}

dependencyManagement {
    imports {
        val bootBOM = libs.spring.boot.dependencies.get().toString()
        mavenBom(bootBOM)
    }
}


dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.amqp)
    implementation(project(":core:utils"))
}