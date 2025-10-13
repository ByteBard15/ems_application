package com.bytebard.git;

import com.bytebard.DockerConfig;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public abstract class GitContainerTask extends DefaultTask {

    private String containerName;

    private String imageName;

    private String innerPort;

    private String hostPort;

    private File outputFile = new File(getProject().getBuildDir(), "container-info.txt");

    @OutputFile
    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    private DockerConfig dockerConfig;

    @Input
    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    @Input
    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    @Input
    public String getInnerPort() {
        return innerPort;
    }

    public void setInnerPort(String innerPort) {
        this.innerPort = innerPort;
    }

    @Input
    public String getHostPort() {
        return hostPort;
    }

    public void setHostPort(String hostPort) {
        this.hostPort = hostPort;
    }

    public void setDockerConfig(DockerConfig dockerConfig) {
        this.dockerConfig = dockerConfig;
    }

    @Internal
    public DockerConfig getDockerConfig() {
        return dockerConfig;
    }

    @TaskAction
    public void ensureContainer() {
        getLogger().lifecycle("Ensuring Gogs container '{}'", containerName);

        DockerClient dockerClient = getDockerConfig().getClient();

        var containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        var existing = containers.stream()
                .filter(c -> c.getNames() != null && java.util.Arrays.asList(c.getNames()).contains("/" + containerName))
                .findFirst();

        if (existing.isPresent()) {
            getLogger().lifecycle("Container already exists: {}", existing.get().getId());
        } else {
            getLogger().lifecycle("Creating container from image {}", imageName);
            dockerClient.pullImageCmd(imageName).start();
            var create = dockerClient.createContainerCmd(imageName)
                    .withName(containerName)
                    .exec();
            dockerClient.startContainerCmd(create.getId()).exec();
            getLogger().lifecycle("Container created and started: {}", create.getId());

            writeIniFile();
            dockerClient.restartContainerCmd(containerName).exec();
        }

        try {
            if (!outputFile.getParentFile().exists()) outputFile.getParentFile().mkdirs();
            FileUtils.writeStringToFile(getProject().file(outputFile), String.format("Created container %s", containerName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeIniFile() {
        String secret = UUID.randomUUID().toString().replace("-", "").substring(0, 32);

        String appIni = String.join("\n",
                "[database]",
                "DB_TYPE = sqlite3",
                "PATH = /data/gogs/data/gogs.db",
                "",
                "[repository]",
                "ROOT = /data/gogs/repositories",
                "",
                "[server]",
                "ROOT_URL = http://localhost:" + innerPort + "/",
                "HTTP_PORT = " + innerPort,
                "SSH_PORT = 22",
                "PROTOCOL = http",
                "DOMAIN = localhost",
                "",
                "[security]",
                "INSTALL_LOCK = true",
                "SECRET_KEY = " + secret,
                "",
                "[service]",
                "REGISTER_EMAIL_CONFIRM = false",
                "",
                "[log]",
                "MODE = console",
                "",
                "[mailer]",
                "ENABLED = false"
        );

        String[] heredoc = {
                "sh", "-lc",
                "mkdir -p /data/gogs/conf && cat > /data/gogs/conf/app.ini <<'EOF'\n" + appIni + "\nEOF"
        };

        getLogger().lifecycle("Writing app.ini inside container (as root) ...");
        var dockerClient = getDockerConfig().getClient();

        ExecCreateCmdResponse execCreate = dockerClient.execCreateCmd(containerName)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd(heredoc)
                .exec();

        try {
            dockerClient.execStartCmd(execCreate.getId())
                    .exec(new ExecStartResultCallback(System.out, System.err))
                    .awaitCompletion();

            ExecCreateCmdResponse chownExec = dockerClient.execCreateCmd(containerName)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withCmd("chown", "-R", "git:git", "/data/gogs")
                    .exec();

            dockerClient.execStartCmd(chownExec.getId())
                    .exec(new ExecStartResultCallback(System.out, System.err))
                    .awaitCompletion();
        } catch (Exception e) {
            throw new RuntimeException("Failed to write container info", e);
        }

        getLogger().info("Restarting container so Gogs reads the new app.ini ...");
        dockerClient.restartContainerCmd(containerName).exec();
    }
}