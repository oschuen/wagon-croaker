# wagon-croaker

This is a maven wagon implementation using the [JFrog CLI tool](https://www.jfrog.com/confluence/display/CLI/JFrog+CLI) to upload and download artifacts stored in an [Artifactory](https://jfrog.com/artifactory) repository.

## Extension

To use the wagon it needs to be provided as build-extension:

pom.xml of your project
```xml
	<pluginRepositories>
		<pluginRepository>
			<id>wagon-croaker-mvn-repo</id>
			<url>https://raw.github.com/oschuen/wagon-croaker/mvn-repo/</url>
			<snapshots>
				<enabled>true</enabled>
				<updatePolicy>always</updatePolicy>
			</snapshots>
			<releases>
				<enabled>true</enabled>
				<updatePolicy>always</updatePolicy>
			</releases>
		</pluginRepository>
	</pluginRepositories>
	<build>
		<extensions>
			<extension>
				<groupId>com.github.oschuen</groupId>
				<artifactId>wagon-croaker</artifactId>
				<version>3.3.3</version>
			</extension>
		</extensions>
	</build>
```

## Deployment

There are different ways to create the url of the repository in the deployment 

```xml
	<distributionManagement>
		<repository>
			<id>artifactory-local</id>
			<name>artifactory local</name>
			<url>croaker:repo://libs-release-local</url>
		</repository>
	</distributionManagement>	
```

In the example above, you have to make the CLI tool aware of the settings by configuring them beforehand:
```
jfrog rt c --url=http://localhost:8081/artifactory --user=admin --password=password
```

Otherwise you can specify the complete url
```xml
<url>croaker:http://localhost:8081/artifactorylibs-release-local</url>
```
In this case the credentials have to be provided in the settings.xml
```xml
	<servers>
    <server>
			<id>artifactory-local</id>
			<username>admin</username>
			<password>password</password>
		</server>
	</servers>
```
