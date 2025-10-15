plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.docker-java:docker-java-core:3.3.5")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.3.5")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r")
    implementation("com.squareup.okhttp3:okhttp:5.2.1")
    implementation("com.squareup.okhttp3:okhttp:5.2.1")
    implementation("org.yaml:snakeyaml:2.1")
}

