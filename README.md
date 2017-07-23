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
java -cp calamari.jar com.octopus.calamari.tomcat.TomcatDeploy
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
./mvnw clean verify -Peap6
```

All containers can be tested with the command:

```
./mvnw verify -Peap6; ./mvnw verify -Peap7; ./mvnw verify -Pwildfly10; ./mvnw verify -Pwildfly11; ./mvnw verify -Ptomcat7; ./mvnw verify -Ptomcat8; ./mvnw verify -Ptomcat85; ./mvnw verify -Ptomcat9
./mvnw verify -Pdomain-eap6; ./mvnw verify -Pdomain-eap7; ./mvnw verify -Pdomain-wildfly10; ./mvnw verify -Pdomain-wildfly11
```