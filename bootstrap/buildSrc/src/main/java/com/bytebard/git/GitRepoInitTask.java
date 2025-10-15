package com.bytebard.git;

import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class GitRepoInitTask extends DefaultTask {
    private String hostPort;

    private String repoName;

    private String username;

    private String password;

    @Input
    public String getHostPort() {
        return hostPort;
    }

    public void setHostPort(String hostPort) {
        this.hostPort = hostPort;
    }

    @Input
    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
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

    @TaskAction
    public void createRepo() {
        OkHttpClient client = new OkHttpClient();

        String url = String.format("http://127.0.0.1:%s/api/v1/admin/users/%s/repos", hostPort, username);
        String json = String.format("{\"name\":\"%s\",\"private\":false}", repoName);

        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", Credentials.basic(username, password))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String respBody = response.body().string();
            getLogger().lifecycle("Create repo HTTP " + response.code() + "; body: " + respBody);

            var status = response.code();
            if (status == 422 && respBody.contains("repository already exists")) {
                getLogger().warn("Repository already exists for user {}: {}", username, repoName);
            } else if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to create repo: " + status + " - " + respBody);
            }
            try {
                if (!outputFile.getParentFile().exists()) outputFile.getParentFile().mkdirs();
                FileUtils.writeStringToFile(getProject().file(outputFile), String.format("Created repository %s with response = %s", repoName, respBody), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
