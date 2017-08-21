# Calamari Java Interface

This project provides the Java interface to application servers for Calamari.

# Running

All data passed to the commands are done via environment variables. The classes like `TomcatOptions` or `WildflyOptions`
take these environment variables and convert them into objects that are consumed by the classes like `TomcatDeploy` or
`WildflyDeploy`.  

All environment variables have the prefix `OctopusEnvironment_` to prevent collisions with any standard environment
variables.

For example, running the Tomcat deployment can be done with a script like this:

```
export OctopusEnvironment_Octopus_Tentacle_CurrentDeployment_PackageFilePath="value"
export OctopusEnvironment_Tomcat_Deploy_Name="value"
export OctopusEnvironment_Tomcat_Deploy_Version="value"
export OctopusEnvironment_Tomcat_Deploy_Controller="value"
export OctopusEnvironment_Tomcat_Deploy_User="value"
export OctopusEnvironment_Tomcat_Deploy_Password="value"
export OctopusEnvironment_Tomcat_Deploy_Debug="value"
export OctopusEnvironment_Tomcat_Deploy_Deploy="value"
export OctopusEnvironment_Tomcat_Deploy_Tag="value"
export OctopusEnvironment_Octopus_Action_EnabledFeatures="value"
java -jar calamari.jar
```

# Testing

Arquillian is used to test the code against multiple versions of application servers.
Each application server is defined in a Maven profile. The profiles are listed below.

* eap6
* eap7
* wildfly10
* wildfly11
* domain-eap6
* domain-eap7
* domain-wildfly10
* domain-wildfly11
* tomcat7
* tomcat8
* tomcat85
* tomcat9

Tests against these profiles can be run with the following command

```
./mvnw clean verify -Pprofilename
```

For example

```
./mvnw clean verify -Peap6`
```

All containers can be tested with the command:

```
./mvnw verify -Peap6; ./mvnw verify -Peap7; ./mvnw verify -Pwildfly10; ./mvnw verify -Pwildfly11; ./mvnw verify -Ptomcat7; ./mvnw verify -Ptomcat8; ./mvnw verify -Ptomcat85; ./mvnw verify -Ptomcat9
./mvnw verify -Pdomain-eap6; ./mvnw verify -Pdomain-eap7; ./mvnw verify -Pdomain-wildfly10; ./mvnw verify -Pdomain-wildfly11
```

# Java Tools

The following java packaging and security tools can be run with these commands:

* keytool: ```java -cp tools.jar sun.security.tools.keytool.Main```
* jarsigner: ```java -cp tools.jar sun.security.tools.JarSigner```
* jar: ```java -cp tools.jar sun.tools.jar.Main```
* pack200: ```java -cp tools.jar com.sun.java.util.jar.pack.Driver```

# Packing

Use Pack200 to reduce the size of the final artifact:

```
pack200 --repack --effort=9 --segment-limit=-1 --modification-time=latest --strip-debug target/calamari.jar
```

# Error Codes

* WILDFLY-DEPLOY-ERROR-0001: There was an error taking a snapshot of the current configuration
* WILDFLY-DEPLOY-ERROR-0002: There was an error deploying the artifact
* WILDFLY-DEPLOY-ERROR-0003: There was an error reading the exsiting deployments
* WILDFLY-DEPLOY-ERROR-0004: There was an error adding the package to the server group
* WILDFLY-DEPLOY-ERROR-0005: There was an error deploying the package to the server group
* WILDFLY-DEPLOY-ERROR-0005: There was an error undeploying the package to the server group
* WILDFLY-DEPLOY-ERROR-0007: There was an error deploying the package to the standalone server
* WILDFLY-DEPLOY-ERROR-0008: There was an error enabling the package in the standalone server 
* WILDFLY-DEPLOY-ERROR-0009: There was an error logging into the management API 
* WILDFLY-DEPLOY-ERROR-0010: There was an error logging out of the management API
* WILDFLY-DEPLOY-ERROR-0011: There was an error terminating the CLI object
* WILDFLY-DEPLOY-ERROR-0012: There was an error changing the deployed state of the application
* WILDFLY-DEPLOY-ERROR-0013: The login was not completed in a reasonable amount of time
* WILDFLY-DEPLOY-ERROR-0014: An exception was thrown during the deployment.
* WILDFLY-DEPLOY-ERROR-0015: Failed to deploy the package to the WildFly/EAP standalone instance
* WILDFLY-DEPLOY-ERROR-0016: Failed to deploy the package to the WildFly/EAP domain
* TOMCAT-DEPLOY-ERROR-0001: There was an error deploying the package to Tomcat
* TOMCAT-DEPLOY-ERROR-0002: There was an error deploying a tagged package to Tomcat
* TOMCAT-DEPLOY-ERROR-0003: There was an error undeploying a package from Tomcat
* TOMCAT-DEPLOY-ERROR-0004: There was an error enabling or disabling a package in Tomcat
* TOMCAT-DEPLOY-ERROR-0005: An exception was thrown during the deployment.
* TOMCAT-DEPLOY-ERROR-0006: A HTTP return code indicated that the login failed due to bad credentials. Make sure the username and password are correct.
* TOMCAT-DEPLOY-ERROR-0007: A HTTP return code indicated that the login failed due to invalid group membership. Make sure the user is part of the manager-script group in the tomcat-users.xml file.")