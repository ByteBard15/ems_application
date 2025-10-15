package com.bytebard.git;

import com.bytebard.DockerConfig;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class GitAdminTask extends DefaultTask {
    private String containerName;

    private String username;

    private String password;

    @Input
    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    @Input
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Input
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private File outputFile = new File(getProject().getBuildDir(), "container-info.txt");

    @OutputFile
    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    private DockerConfig dockerConfig;

    public void setDockerConfig(DockerConfig dockerConfig) {
        this.dockerConfig = dockerConfig;
    }

    @Internal
    public DockerConfig getDockerConfig() {
        return dockerConfig;
    }

    @TaskAction
    public void createAdminAndRepo() {
        String command = String.format(
                "su - git -s /bin/sh -c \"/app/gogs/gogs admin create-user --name %s --password %s --email %s@emp --admin --config /data/gogs/conf/app.ini\"",
                username, password, username
        );

        String[] shellCmd = {
                "sh", "-lc", command
        };

        getLogger().lifecycle("Creating Gogs admin user inside container...");

        try {
            var dockerClient = getDockerConfig().getClient();
            ExecCreateCmdResponse execCreate = dockerClient.execCreateCmd(containerName)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withCmd(shellCmd)
                    .exec();

            dockerClient.execStartCmd(execCreate.getId())
                    .exec(new ExecStartResultCallback(System.out, System.err))
                    .awaitCompletion();

            try {
                if (!outputFile.getParentFile().exists()) outputFile.getParentFile().mkdirs();
                FileUtils.writeStringToFile(getProject().file(outputFile), String.format("Created admin [%s] for Container = [%s]", username, containerName), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create admin for repository", e);
        }
    }
}
