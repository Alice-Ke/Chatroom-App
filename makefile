JC=javac

soucefiles=Client.java RThreadClient.java SThreadClient.java Server.java IntBoxer.java

classfiles=$(soucefiles:.java=.class)

all:$(classfiles)

%.class:%.java
	$(JC) $<

clean:
	$(RM) *.class