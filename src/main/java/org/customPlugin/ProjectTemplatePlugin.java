package org.customPlugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

public class ProjectTemplatePlugin implements Plugin<Project> {

    private static final Logger LOGGER = Logging.getLogger(ProjectTemplatePlugin.class);

    @Override
    public void apply(Project project) {
        project.getTasks().register("generateSpringBootProject", task -> {
            task.doLast(t -> {
                LOGGER.lifecycle("Starting Spring Boot project generation...");

                String basePackage = Optional.ofNullable((String) project.findProperty("basePackage"))
                        .orElseThrow(() -> {
                            LOGGER.error("Base package property is missing.");
                            return new IllegalArgumentException("basePackage property is required.");
                        });

                try {
                    generateProjectStructure(project.getProjectDir().toPath(), basePackage);
                    updateBuildGradle(project.getProjectDir().toPath());
                    LOGGER.lifecycle("Project generated successfully!");
                } catch (Exception e) {
                    LOGGER.error("Error generating project: {}", e.getMessage(), e);
                    throw new RuntimeException("Error generating project", e);
                }
            });
        });
    }

    private void generateProjectStructure(Path projectDir, String basePackage) throws IOException {
        LOGGER.lifecycle("Generating project structure at: {}", projectDir);
        String basePath = basePackage.replace(".", "/");
        Path mainJava = projectDir.resolve("src/main/java").resolve(basePath);

        if (!Files.exists(mainJava)) {
            LOGGER.error("Base package directory not found: {}", mainJava);
            throw new IllegalArgumentException("Base package directory not found: " + mainJava);
        }

        LOGGER.info("Creating common folders...");
        createDirectories(mainJava,
                "common/constants",
                "common/exceptionHandlers",
                "common/model/dtos",
                "common/model/enums",
                "common/utility",
                "common/GlobalExceptionHandler.java"
        );

        LOGGER.info("Creating feature folders...");
        createDirectories(mainJava,
                "feature1/boundaries/controller",
                "feature1/boundaries/validator",
                "feature1/boundaries/model/dtos",
                "feature1/boundaries/model/enums",
                "feature1/service/impl",
                "feature1/repository",
                "feature1/IntegrationServices"
        );
    }

    private void updateBuildGradle(Path projectDir) throws IOException {
        Path buildGradleFile = projectDir.resolve("build.gradle");

        if (Files.exists(buildGradleFile)) {
            LOGGER.lifecycle("Updating build.gradle file...");
            String buildGradleContent = """
                    plugins {
                        id 'java'
                        id 'org.springframework.boot' version '3.3.3'
                        id 'io.spring.dependency-management' version '1.1.6'
                        id 'jacoco'
                        id 'application'
                    }

                    application {
                        mainClassName = 'com.vibranium.app.VibraniumApplication'
                    }

                    group = 'com.vibranium'
                    version = '0.0.1-SNAPSHOT'

                    java {
                        toolchain {
                            languageVersion = JavaLanguageVersion.of(17)
                        }
                    }

                    repositories {
                        mavenCentral()
                    }

                    dependencies {
                        implementation 'org.springframework.boot:spring-boot-starter-web'
                        testImplementation 'org.springframework.boot:spring-boot-starter-test'
                        testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
                        implementation 'org.springframework.boot:spring-boot-starter-webflux'
                        implementation 'org.springframework.boot:spring-boot-starter-validation'

                        compileOnly 'org.projectlombok:lombok:1.18.28' // Check for latest version
                        annotationProcessor 'org.projectlombok:lombok:1.18.28'
                        implementation 'org.springframework.boot:spring-boot-starter-actuator'

                        implementation 'org.slf4j:slf4j-api:2.0.9' // SLF4J API
                        implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0'
                    }

                    tasks.named('test') {
                        useJUnitPlatform()
                    }

                    tasks.withType(Test).configureEach {
                        failFast=true
                        testLogging {
                            exceptionFormat = 'full'
                            events 'started', 'skipped', 'passed', 'failed'
                            showStandardStreams = true
                        }
                    }

                    jacoco {
                        toolVersion = "0.8.12"
                        reportsDirectory = layout.buildDirectory.dir('customJacocoReportDir')
                    }

                    jacocoTestReport {
                        reports {
                            xml.required = false
                            csv.required = false
                            html.outputLocation = layout.buildDirectory.dir('jacocoHtml')
                        }
                    }

                    jacocoTestCoverageVerification {
                        violationRules {
                            rule {
                                limit {
                                    minimum = 0.5
                                }
                            }

                            rule {
                                enabled = false
                                element = 'CLASS'
                                includes = ['org.gradle.*']

                                limit {
                                    counter = 'LINE'
                                    value = 'TOTALCOUNT'
                                    maximum = 0.3
                                }
                            }
                        }
                    }
            """;

            Files.writeString(buildGradleFile, buildGradleContent);
            LOGGER.lifecycle("build.gradle file updated successfully!");
        } else {
            LOGGER.error("build.gradle file not found at: {}", buildGradleFile);
        }
    }

    private void createDirectories(Path parent, String... paths) throws IOException {
        for (String path : paths) {
            Path dir = parent.resolve(path);
            if (!Files.exists(dir)) {
                if (path.endsWith(".java")) {
                    LOGGER.debug("Creating file: {}", dir);
                    Files.writeString(dir, "// Placeholder for " + path, StandardOpenOption.CREATE);
                } else {
                    LOGGER.debug("Creating directory: {}", dir);
                    Files.createDirectories(dir);
                }
            } else {
                LOGGER.info("Already exists: {}", dir);
            }
        }
    }

}
