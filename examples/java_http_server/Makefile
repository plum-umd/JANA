P := org/jibble/simplewebserver

SimpleWebServer.jar: Manifest.txt $(P)/RequestThread.class $(P)/SimpleWebServer.class
	jar cfm SimpleWebServer.jar $^

$(P)/SimpleWebServer.class: $(P)/RequestThread.class

%.class: %.java
	javac $<

start: SimpleWebServer.jar
	java -jar SimpleWebServer.jar

clean:
	rm -Rf SimpleWebServer.jar $(P)/*.class
