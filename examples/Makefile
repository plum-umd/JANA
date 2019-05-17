CC := cc
JAVAC := javac
CLANG := clang
JAVAP := javap

%.o: %.c
	$(CC) -c $<

%.class: %.java
	$(JAVAC) -g $<

# generate readable llvm-ir from a c file
%.ll: %.c
	$(CLANG) -S -emit-llvm $<

# generate readable java bytecode from a java bytecode file
%.jvm: %.class
	$(JAVAP) -c $* > $*.jvm

clean:
	rm -Rf *.o *.class *.ll *.jvm
