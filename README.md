# Building
Prerequsities:
* Maven (package maven on Ubuntu and Arch Linux)

Then the following command in the project root directory builds
a runnable jar file in ./target/

`mvn compile assembly:single`

# Running
Prerequisities:
* Running tourenplaner-server with URAR support (use the urar branch)

`java -jar target/chrenderclient-0.1-SNAPSHOT-jar-with-dependencies.jar`
