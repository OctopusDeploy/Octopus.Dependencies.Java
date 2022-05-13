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
* wildfly13
* domain-eap6
* domain-eap7
* domain-wildfly13
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
./mvnw clean verify -Peap6
```

All containers can be tested with the command:

```
./mvnw verify -Peap6; ./mvnw verify -Peap7; ./mvnw verify -Pwildfly13; ./mvnw verify -Ptomcat7; ./mvnw verify -Ptomcat8; ./mvnw verify -Ptomcat85; ./mvnw verify -Ptomcat9
./mvnw verify -Pdomain-eap6; ./mvnw verify -Pdomain-eap7; ./mvnw verify -Pdomain-wildfly13
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