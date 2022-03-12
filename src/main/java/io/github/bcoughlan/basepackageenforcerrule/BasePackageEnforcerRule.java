package io.github.bcoughlan.basepackageenforcerrule;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

public class BasePackageEnforcerRule implements EnforcerRule {

  private String pattern = null;

  public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
    Log log = helper.getLog();

    try {
      String packaging = (String) helper.evaluate("${project.packaging}");
      if (!"jar".equals(packaging) && !"war".equals(packaging)) {
        return;
      }

      MavenProject project = (MavenProject) helper.evaluate("${project}");
      String srcDir = (String) helper.evaluate("${project.build.sourceDirectory}");
      String expectedPackage = evaluatePattern(project);
      log.debug("Expected base package: " + expectedPackage);

      Path path = Paths.get(srcDir);
      for (String part : expectedPackage.split("\\.")) {
        path = path.resolve(part);

        if (!path.toFile().isDirectory()) {
          throw new EnforcerRuleException("Package does not exist in source directory: " + expectedPackage);
        }
        
        if (Files.list(path.resolve("..")).count() != 1) {
          Path extraPath = Files.list(path.resolve(".."))
            .filter(p -> !p.getFileName().toString().toLowerCase().equals(part.toLowerCase()))
            .findFirst()
            .get();
          String extraPackageOrFile = Paths.get(srcDir)
            .relativize(extraPath)
            .toString()
            .replace(File.separatorChar, '.');

          String type = extraPath.toFile().isDirectory() ? "package" : "file";

          throw new EnforcerRuleException(
            "Expected only " + expectedPackage + ".**, " + 
            "but there is an unexpected " + type + ": " + extraPackageOrFile
          );
        }
      }
    } catch (ExpressionEvaluationException e) {
      throw new EnforcerRuleException("Unable to lookup an expression " + e.getLocalizedMessage(), e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String evaluatePattern(MavenProject project) {
    //Performance optimisation: No need for Groovy interpreter if default pattern 
    if (pattern == null) {
      //"project.groupId + '.' + project.artifactId.replace('-', '').toLowerCase()"
      return project.getGroupId() + "." + project.getArtifactId().replace("-", "").toLowerCase();
    }
    Binding binding = new Binding();
    binding.setVariable("project", project);
    GroovyShell shell = new GroovyShell(binding);
    return (String) shell.evaluate(pattern);
  }

  public String getCacheId() {
    return null;
  }

  public boolean isCacheable() {
    return false;
  }

  public boolean isResultValid(EnforcerRule rule) {
    return false;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }
}