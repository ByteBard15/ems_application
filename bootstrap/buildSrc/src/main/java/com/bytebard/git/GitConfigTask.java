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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@CacheableTask
public abstract class GitConfigTask extends DefaultTask {
    private String username;

    private String password;

    private String hostPort;

    private String repoName;

    private String networkHost;

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

    public void setNetworkHost(String networkHost) {
        this.networkHost = networkHost;
    }

    @Input
    public String getNetworkHost() {
        return networkHost;
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
                if (!deleted)
                    getLogger().warn("Could not fully delete existing local dir: " + localDir.getAbsolutePath());
            }
            if (!localDir.mkdirs()) {
                getLogger().lifecycle("Local directory created/exists: " + localDir.getAbsolutePath());
            }

            UsernamePasswordCredentialsProvider cp = new UsernamePasswordCredentialsProvider(username, password);
            getLogger().lifecycle("Cloning empty repo from http://" + networkHost + ":" + hostPort + "/" + username + "/" + repoName + ".git using JGit ...");

            try (Git git = Git.cloneRepository()
                    .setURI("http://" + networkHost + ":" + hostPort + "/" + username + "/" + repoName + ".git")
                    .setDirectory(localDir)
                    .setCredentialsProvider(cp)
                    .call()) {
                writeYamlConfigs(localDir, props);

                git.add().addFilepattern(".").call();
                PersonIdent author = new PersonIdent("automation", "automation@local");
                git.commit().setMessage("Add config YAMLs").setAuthor(author).call();
                git.push().setCredentialsProvider(cp).call();
            }

            getLogger().lifecycle("Local repo initialized and pushed.");

            try {
                if (!outputFile.getParentFile().exists()) outputFile.getParentFile().mkdirs();
                FileUtils.writeStringToFile(getProject().file(outputFile), String.format("Created repo [%s] and pushed YAML configs for user [%s]",
                        repoName, username), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed during repo init / config push: "+ e.getMessage(), e);
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

        writeApplicationYaml(dir, yaml, m);
        writeAuthServiceYaml(dir, yaml, m);
        writeGatewayServiceYaml(dir, yaml, m);
        writeEmployeeServiceYaml(dir, yaml, m);
        writeDiscoveryServiceYaml(dir, yaml, m);
    }

    private void writeApplicationYaml(File dir, Yaml yaml, Map<String, String> m) throws Exception {
        Map<String, Object> application = new LinkedHashMap<>();

        Map<String, Object> server = Map.of("shutdown", "graceful");
        application.put("server", server);

        Map<String, Object> rabbitmq = Map.of(
                "host", m.get("RABBITMQ_HOST"),
                "port", m.get("RABBITMQ_PORT"),
                "username", m.get("RABBITMQ_USERNAME"),
                "password", m.get("RABBITMQ_PASSWORD"),
                "virtual-host", m.get("RABBITMQ_VIRTUAL_HOST"),
                "queues", Map.of(
                        "user-events", m.get("RABBITMQ_USER_EVENTS_QUEUE"),
                        "dl-events", m.get("RABBITMQ_DL_EVENTS_QUEUE")
                ),
                "exchanges", Map.of(
                        "main", m.get("RABBITMQ_MAIN_EXCHANGE"),
                        "dlx", m.get("RABBITMQ_DL_EXCHANGE")
                ),
                "listeners", Map.of("enabled", "true")
        );

        Map<String, Object> spring = new LinkedHashMap<>();
        spring.put("application", Map.of("name", "global-config"));
        spring.put("profiles", Map.of("active", "dev"));
        spring.put("rabbitmq", rabbitmq);

        application.put("spring", spring);
        application.put("logging", Map.of(
                "level", Map.of(
                        "root", m.getOrDefault("LOG_LEVEL", "INFO"),
                        "org.springframework.cloud.gateway", "TRACE",
                        "org.springframework.cloud.loadbalancer", "TRACE"
                )
        ));
        application.put("management",
                Map.of("endpoints", Map.of("web", Map.of("exposure", Map.of("include", "health,info"))))
        );

        writeYamlFile(dir, "application.yml", yaml, application);
    }

    private void writeAuthServiceYaml(File dir, Yaml yaml, Map<String, String> m) throws Exception {
        Map<String, Object> auth = new LinkedHashMap<>();

        auth.put("server", Map.of("port", Integer.parseInt(m.getOrDefault("AUTH_SERVICE_PORT", "8081"))));

        Map<String, Object> datasource = Map.of(
                "driver-class-name", "org.postgresql.Driver",
                "username", m.get("DB_USERNAME"),
                "password", m.get("DB_PASSWORD"),
                "url", String.format("jdbc:postgresql://%s:%s/%s", m.get("DB_HOST"), m.get("DB_PORT"), m.get("DB_NAME")),
                "hikari", Map.of("maximum-pool-size", Integer.parseInt(m.getOrDefault("DB_POOL_SIZE", "10")))
        );

        Map<String, Object> jpa = Map.of(
                "hibernate", Map.of(
                        "ddl-auto", "none",
                        "show-sql", true,
                        "database-platform", "org.hibernate.dialect.PostgreSQLDialect"
                )
        );

        Map<String, Object> spring = Map.of(
                "application", Map.of("name", "auth-service"),
                "datasource", datasource,
                "jpa", jpa,
                "jwt", Map.of(
                        "secret", m.get("JWT_SECRET"),
                        "expiry-in-hours", m.get("JWT_EXPIRATION_IN_HOURS"),
                        "issuer", m.get("JWT_ISSUER")
                )
        );

        auth.put("spring", spring);
        auth.put("eureka", Map.of(
                "client",
                Map.of(
                        "service-url",
                        Map.of("defaultZone",
                                m.getOrDefault("DISCOVERY_SERVICE_URL", "")))
                )
        );

        writeYamlFile(dir, "auth-service.yml", yaml, auth);
    }

    private void writeGatewayServiceYaml(File dir, Yaml yaml, Map<String, String> m) throws Exception {
        Map<String, Object> root = new LinkedHashMap<>();

        root.put("server", Map.of("port", Integer.parseInt(m.getOrDefault("GATEWAY_SERVICE_PORT", "8080"))));

        root.put("eureka", Map.of(
                "client", Map.of(
                        "service-url", Map.of("defaultZone", m.getOrDefault("DISCOVERY_SERVICE_URL", "http://172.17.0.1:8761/eureka"))
                )
        ));

        root.put("springdoc", Map.of(
                "api-docs", Map.of(
                        "enabled", true,
                        "path", "/v3/api-docs"
                ),
                "swagger-ui", Map.of(
                        "enabled", true,
                        "path", "/swagger-ui.html",
                        "url", "/v3/api-docs"
                )
        ));

        root.put("management", Map.of(
                "endpoints", Map.of(
                        "web", Map.of(
                                "exposure", Map.of(
                                        "include", "gateway,health,info"
                                )
                        )
                )
        ));

        Map<String, Object> spring = new LinkedHashMap<>();
        spring.put("application", Map.of("name", "gateway-service"));
        spring.put("main", Map.of("web-application-type", "reactive"));
        spring.put("webflux", Map.of(
                "static-path-pattern", "/static/**"
        ));
        spring.put("web", Map.of(
                "resources", Map.of(
                        "static-locations", List.of(
                                "classpath:/static/",
                                "classpath:/docs/",
                                "classpath:/public/"
                        )
                )
        ));

        List<Map<String, Object>> routes = List.of(
                Map.of(
                        "id", "users",
                        "uri", "lb://employee-service",
                        "predicates", List.of("Path=/api/v1/users/**")
                ),
                Map.of(
                        "id", "departments",
                        "uri", "lb://employee-service",
                        "predicates", List.of("Path=/api/v1/departments/**")
                ),
                Map.of(
                        "id", "auth",
                        "uri", "lb://auth-service",
                        "predicates", List.of("Path=/api/v1/auth/**")
                ),
                Map.of(
                        "id", "swagger-ui",
                        "uri", "forward:/",
                        "predicates", List.of("Path=/swagger-ui.html,/swagger-ui/**,/webjars/**"),
                        "filters", List.of("RewritePath=/swagger-ui/(?<segment>.*),/swagger-ui/${segment}")
                ),
                Map.of(
                        "id", "api-docs",
                        "uri", "forward:/",
                        "predicates", List.of("Path=/v3/api-docs,/v3/api-docs/**")
                )
        );

        Map<String, Object> serverUnderGateway = Map.of("webflux", Map.of("routes", routes));
        Map<String, Object> gateway = new LinkedHashMap<>();
        gateway.put("server", serverUnderGateway);
        gateway.put("discovery", Map.of(
                "locator", Map.of(
                        "enabled", true,
                        "lower-case-service-id", true
                )
        ));

        spring.put("cloud", Map.of("gateway", gateway));
        root.put("spring", spring);

        writeYamlFile(dir, "gateway-service.yml", yaml, root);
    }

    private void writeEmployeeServiceYaml(File dir, Yaml yaml, Map<String, String> m) throws Exception {
        Map<String, Object> emp = new LinkedHashMap<>();

        emp.put("server", Map.of("port", Integer.parseInt(m.getOrDefault("EMPLOYEE_SERVICE_PORT", "8082"))));

        Map<String, Object> datasource = Map.of(
                "driver-class-name", "org.postgresql.Driver",
                "username", m.get("DB_USERNAME"),
                "password", m.get("DB_PASSWORD"),
                "url", String.format("jdbc:postgresql://%s:%s/%s", m.get("DB_HOST"), m.get("DB_PORT"), m.get("DB_NAME"))
        );

        Map<String, Object> jpa = Map.of(
                "hibernate", Map.of(
                        "ddl-auto", "none",
                        "show-sql", true,
                        "database-platform", "org.hibernate.dialect.PostgreSQLDialect"
                )
        );

        Map<String, Object> spring = Map.of(
                "application", Map.of("name", "employee-service"),
                "rabbitmq", Map.of("listeners", Map.of("enabled", "false")),
                "datasource", datasource,
                "jpa", jpa,
                "jwt", Map.of(
                        "secret", m.get("JWT_SECRET"),
                        "expiry-in-hours", m.get("JWT_EXPIRATION_IN_HOURS"),
                        "issuer", m.get("JWT_ISSUER")
                ),
                "auth", Map.of("default-password", m.get("DEFAULT_PASSWORD"))
        );

        emp.put("spring", spring);
        emp.put("eureka", Map.of("client", Map.of("service-url", Map.of("defaultZone", m.get("DISCOVERY_SERVICE_URL")))));

        writeYamlFile(dir, "employee-service.yml", yaml, emp);
    }

    private void writeDiscoveryServiceYaml(File dir, Yaml yaml, Map<String, String> m) throws Exception {
        Map<String, Object> disc = new LinkedHashMap<>();

        disc.put("server", Map.of("port", m.get("DISCOVERY_SERVICE_PORT")));
        disc.put("spring", Map.of("application", Map.of("name", "discovery-service")));
        disc.put("eureka", Map.of("client", Map.of("register-with-eureka", false, "fetch-registry", false)));

        writeYamlFile(dir, "discovery-service.yml", yaml, disc);
    }

    private void writeYamlFile(File dir, String filename, Yaml yaml, Map<String, Object> data) throws Exception {
        File outFile = new File(dir, filename);
        try (OutputStream out = new FileOutputStream(outFile)) {
            yaml.dump(data, new OutputStreamWriter(out, StandardCharsets.UTF_8));
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

