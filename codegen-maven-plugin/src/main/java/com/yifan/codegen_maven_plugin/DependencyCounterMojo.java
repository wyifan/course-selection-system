package com.yifan.codegen_maven_plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.model.Dependency;

import java.util.List;

@Mojo(name = "dependency-counter", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class DependencyCounterMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    @Parameter(property = "scope")
    String scope;

    public void execute() throws MojoExecutionException, MojoFailureException {
        List<Dependency> dependencies = project.getDependencies();

        long numDependencies = dependencies.stream()
                .filter(dep -> {
                    getLog().info("dependency scope: " + dep.getScope());
                    return scope == null || scope.isEmpty() || scope.equalsIgnoreCase(dep.getScope());
                })
                .count();
        // getLog() provides access to the logging system
        getLog().info("parameter scope: " + scope);
        getLog().info("Number of dependencies: " + numDependencies);
    }
}
