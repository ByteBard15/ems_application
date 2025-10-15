import com.bytebard.DockerConfig
import com.bytebard.git.GitAdminTask
import com.bytebard.git.GitConfigTask
import com.bytebard.git.GitContainerTask
import com.bytebard.git.GitRepoInitTask
import com.bytebard.utils.PropertyUtils
import java.util.*

plugins {
    base
}

val parent = rootProject.rootDir.parent ?: throw GradleException("Missing parent dir")
val propsFile = File(parent, "default.env")
var useEnv = project.findProperty("useEnv")?.toString()?.toBoolean() ?: false
if (useEnv) {
    useEnv = propsFile.exists()
}

val ciHost = project.findProperty("ciHost")?.toString() ?: "127.0.0.1"
val liveHost = project.findProperty("liveHost")?.toString() ?: "127.0.0.1"
val gitUsername = project.findProperty("username")?.toString() ?: "emp_admin"
val gitPassword = project.findProperty("password")?.toString() ?: "admin"
val gitRepo = project.findProperty("repo")?.toString() ?: "shared-config"
val gitHostPort = project.findProperty("hostPort")?.toString() ?: "3001"
val gitInnerPort = project.findProperty("innerPort")?.toString() ?: "3000"
val gitContainerName = project.findProperty("containerName")?.toString() ?: "shared-config-db"
val gitImageName = project.findProperty("imageName")?.toString() ?: "gogs/gogs"
val gitTag = project.findProperty("tag")?.toString() ?: "0.11.91"

val defaultProperties: Properties = if (useEnv) {
    Properties().apply { load(propsFile.inputStream()) }
} else {
    if (!propsFile.exists()) {
        PropertyUtils.generateRandomizedProperties(propsFile, liveHost, gitUsername, gitRepo)
    }
    Properties().apply {
        load(propsFile.inputStream())
    }
}

val rootDockerConfig = DockerConfig(
    gitImageName,
    gitTag,
    gitContainerName,
)

tasks.register<GitConfigTask>("createGitConfig") {
    dependsOn("initGitRepo")
    username = gitUsername
    password = gitPassword
    hostPort = gitHostPort
    repoName = gitRepo
    networkHost = ciHost
    properties = defaultProperties
}

tasks.register<GitConfigTask>("updateGitConfig") {
    username = gitUsername
    password = gitPassword
    hostPort = gitHostPort
    repoName = gitRepo
    networkHost = ciHost
    properties = defaultProperties
}

tasks.register<GitContainerTask>("createContainer") {
    containerName = gitContainerName
    hostPort = gitHostPort
    imageName = "${gitImageName}:${gitTag}"
    innerPort = gitInnerPort
    hostPort = gitHostPort
    dockerConfig = rootDockerConfig
}

tasks.register<GitAdminTask>("createGitAdmin") {
    dependsOn("createContainer")
    dockerConfig = rootDockerConfig
    username = gitUsername
    containerName = gitContainerName
    username = gitUsername
    password = gitPassword
}

tasks.register<GitRepoInitTask>("initGitRepo") {
    dependsOn("createGitAdmin")
    username = gitUsername
    password = gitPassword
    hostPort = gitHostPort
    repoName = gitRepo
}