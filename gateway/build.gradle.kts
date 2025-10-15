plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency)
}

dependencyManagement {
    imports {
        val cloudBOM = libs.spring.cloud.dependencies.get().toString()
        val bootBOM = libs.spring.boot.dependencies.get().toString()
        mavenBom(cloudBOM)
        mavenBom(bootBOM)
    }
}

dependencies {
    implementation(project(":core:messaging"))
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.3.0")
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.cloud.starter.config)
    implementation(libs.spring.cloud.gateway.server)
    implementation(libs.spring.cloud.eureka.client)
    implementation(libs.spring.cloud.loadbalancer)
    implementation(libs.spring.boot.starter.webflux)
}
