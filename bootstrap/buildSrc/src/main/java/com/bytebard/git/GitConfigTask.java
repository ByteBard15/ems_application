package com.bytebard.git;

import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.*;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@CacheableTask
public abstract class GitConfigTask extends DefaultTask {
    private String username;

    private String password;

    private String hostPort;

    private String repoName;

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

    private Properties properties;

    public void setProperties(Properties props) {
        this.properties = props == null ? new Properties() : props;
    }

    @Internal
    public Properties getProperties() {
        return properties;
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
    public void createRepoAndPushConfig() {
        getLogger().lifecycle("Starting GitInitAndConfigTask: create repo, generate YAML configs, commit and push.");

        Properties props = getProperties();

        try {
            File localDir = new File(getProject().getBuildDir(), repoName);
            if (localDir.exists()) {
                boolean deleted = deleteRecursively(localDir);
                if (!deleted) getLogger().warn("Could not fully delete existing local dir: " + localDir.getAbsolutePath());
            }
            if (!localDir.mkdirs()) {
                getLogger().lifecycle("Local directory created/exists: " + localDir.getAbsolutePath());
            }

            UsernamePasswordCredentialsProvider cp = new UsernamePasswordCredentialsProvider(username, password);
            getLogger().lifecycle("Cloning empty repo from http://localhost:" + hostPort + "/" + username + "/" + repoName + ".git using JGit ...");

            Git git = Git.cloneRepository()
                    .setURI("http://localhost:" + hostPort + "/" + username + "/" + repoName + ".git")
                    .setDirectory(localDir)
                    .setCredentialsProvider(cp)
                    .call();

            writeYamlConfigs(localDir, props);

            git.add().addFilepattern(".").call();
            PersonIdent author = new PersonIdent("automation", "automation@local");
            git.commit().setMessage("Add config YAMLs").setAuthor(author).call();
            git.push().setCredentialsProvider(cp).call();

            getLogger().lifecycle("Local repo initialized and pushed.");

            try {
                if (!outputFile.getParentFile().exists()) outputFile.getParentFile().mkdirs();
                FileUtils.writeStringToFile(getProject().file(outputFile), String.format("Created repo [%s] and pushed YAML configs for user [%s]",
                        repoName, username), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed during repo init / config push", e);
        }
    }

    private void writeYamlConfigs(File dir, Properties props) throws Exception {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);

        Map<String, String> m = new HashMap<>();
        for (String name : props.stringPropertyNames()) {
            m.put(name, props.getProperty(name));
        }

        // application.yml
        Map<String, Object> application = new LinkedHashMap<>();
        Map<String, Object> server = Map.of("shutdown", "graceful");
        application.put("server", server);
        Map<String, Object> spring = new LinkedHashMap<>();
        spring.put("application", Map.of("name", "global-config"));
        spring.put("profiles", Map.of("active", "dev"));
        application.put("spring", spring);
        Map<String,Object> logging = Map.of("level", Map.of("root", m.getOrDefault("LOG_LEVEL","INFO")));
        application.put("logging", logging);
        Map<String,Object> management = Map.of("endpoints", Map.of("web", Map.of("exposure", Map.of("include", "health,info"))));
        application.put("management", management);

        File appFile = new File(dir, "application.yml");
        try (OutputStream out = new FileOutputStream(appFile)) {
            yaml.dump(application, new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8));
        }

        // auth-service.yml
        Map<String,Object> auth = new LinkedHashMap<>();
        auth.put("server", Map.of("port", Integer.parseInt(m.getOrDefault("AUTH_SERVICE_PORT", "8081"))));
        Map<String,Object> authSpring = new LinkedHashMap<>();
        authSpring.put("application", Map.of("name", "auth-service"));

        Map<String,Object> datasource = new LinkedHashMap<>();
        datasource.put("driver-class-name", "org.postgresql.Driver");
        datasource.put("username", m.get("DB_USERNAME"));
        datasource.put("password", m.get("DB_PASSWORD"));
        var url = String.format("jdbc:postgresql://%s:%s/%s", m.get("DB_HOST"), m.get("DB_PORT"), m.get("DB_NAME"));
        datasource.put("url", url);
        Map<String,Object> hikari = new LinkedHashMap<>();
        hikari.put("maximum-pool-size", Integer.parseInt(m.getOrDefault("DB_POOL_SIZE", "10")));
        datasource.put("hikari", hikari);

        Map<String,Object> jpa = new LinkedHashMap<>();
        jpa.put("hibernate", Map.of("ddl-auto", "none", "show-sql", true, "database-platform", "org.hibernate.dialect.PostgreSQLDialect"));

        authSpring.put("datasource", datasource);
        authSpring.put("jpa", jpa);
        authSpring.put("jwt", Map.of(
                "secret",
                m.get("JWT_SECRET"),
                "expiry-in-hours",
                m.get("JWT_EXPIRATION_IN_HOURS"),
                "issuer",
                m.get("JWT_ISSUER"))
        );
        auth.put("spring", authSpring);


        auth.put("eureka", Map.of("client", Map.of("service-url", Map.of("defaultZone", m.getOrDefault("DISCOVERY_SERVICE_URL","http://localhost:8761") + "/eureka/"))));

        try (OutputStream out = new FileOutputStream(new File(dir, "auth-service.yml"))) {
            yaml.dump(auth, new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8));
        }

        // gateway-service.yml
        Map<String,Object> gateway = new LinkedHashMap<>();
        gateway.put("server", Map.of("port", Integer.parseInt(m.getOrDefault("GATEWAY_SERVICE_PORT", "8080"))));
        Map<String,Object> gatewaySpring = new LinkedHashMap<>();
        gatewaySpring.put("application", Map.of("name", "gateway-service"));
        gatewaySpring.put("cloud", Map.of("gateway", Map.of("routes", List.of(
                Map.of("id","employee","uri","lb://employee-service","predicates", List.of("Path=/employee/**")),
                Map.of("id","auth","uri","lb://auth-service","predicates", List.of("Path=/auth/**"))
        ))));
        gateway.put("spring", gatewaySpring);
        gateway.put("eureka", Map.of("client", Map.of("service-url", Map.of("defaultZone", "http://localhost:8761/eureka/"))));
        try (OutputStream out = new FileOutputStream(new File(dir, "gateway-service.yml"))) {
            yaml.dump(gateway, new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8));
        }

        // employee-service.yml
        Map<String,Object> emp = new LinkedHashMap<>();
        emp.put("server", Map.of("port", 8082));
        Map<String,Object> empSpring = new LinkedHashMap<>();
        empSpring.put("application", Map.of("name", "employee-service"));

        empSpring.put("datasource", datasource);
        empSpring.put("jpa", jpa);
        empSpring.put("jwt", Map.of(
                "secret",
                m.get("JWT_SECRET"),
                "expiry-in-hours",
                m.get("JWT_EXPIRATION_IN_HOURS"),
                "issuer",
                m.get("JWT_ISSUER"))
        );
        empSpring.put("auth", Map.of("default-password", m.get("DEFAULT_PASSWORD")));
        emp.put("spring", empSpring);
        emp.put("eureka", Map.of("client", Map.of("service-url", Map.of("defaultZone","http://localhost:8761/eureka/"))));
        try (OutputStream out = new FileOutputStream(new File(dir, "employee-service.yml"))) {
            yaml.dump(emp, new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8));
        }

        // discovery-service.yml
        Map<String,Object> disc = new LinkedHashMap<>();
        disc.put("server", Map.of("port", m.get("DISCOVERY_SERVICE_PORT")));
        disc.put("spring",
                Map.of(
                        "application",
                        Map.of("name", "discovery-service")
                )
        );
        disc.put("eureka", Map.of("client", Map.of("register-with-eureka", false, "fetch-registry", false)));
        try (OutputStream out = new FileOutputStream(new File(dir, "discovery-service.yml"))) {
            yaml.dump(disc, new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8));
        }
    }

    private boolean deleteRecursively(File f) {
        if (!f.exists()) return true;
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File c : files) {
                    if (!deleteRecursively(c)) return false;
                }
            }
        }
        return f.delete();
    }
}

