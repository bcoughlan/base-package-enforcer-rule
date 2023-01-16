# base-package-enforcer-rule

A Maven enforcer rule to ensure verify that &lt;groupId>.&lt;artifactId> matches the base package.

The use case is to allow writing ArchUnit tests which enforce Maven multi-module project constraints.

# Usage

Add this to your Maven enforcer plugin config:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
  <dependencies>
    <dependency>
      <groupId>io.github.bcoughlan</groupId>
      <artifactId>base-package-enforcer-rule</artifactId>
      <version>1.0.0</version>
    </dependency>
  </dependencies>
  <configuration>
    <rules>
      <enforceBasePackage implementation="io.github.bcoughlan.basepackageenforcerrule.BasePackageEnforcerRule">
        <!-- 
        Optional: specify a custom package pattern here. If not specified the default is:
        project.groupId + '.' + project.artifactId.replace('-', '').toLowerCase()
        -->
        <pattern>...</pattern>
      </enforceBasePackage>
    </rules>
  </configuration>
 </plugin>
 ```
 
