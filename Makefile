JAVA_HOME = $(shell ./get_java_home.sh)
OSNAME = $(shell ./get_os.sh)
JNI_EXT = $(shell ./get_extension.sh)

all: env extern analyzer tainttool facade

tainttool:
	cd taint && mvn clean install -q

extern:
	cd externs && ./build.sh && ./deploy-joana.sh

env:
	cd environment && make 

facade: extern
	cd wala-facade && sbt publishLocal 


#ppl: ppl-1.1
#	cd ppl-1.1 && ./configure --enable-interfaces=Java --with-java=$(JAVA_HOME) --prefix=$(PWD)/.install && make && make install && mkdir -p ../scala-tools/lib/native/$(OSNAME) && cp interfaces/Java/jni/.libs/libppl_java.$(JNI_EXT) ../scala-tools/lib/native/$(OSNAME)/libppl_java.$(JNI_EXT) && cp interfaces/Java/ppl_java.jar ../scala-tools/lib/ && mvn install:install-file -Dfile=./interfaces/Java/ppl_java.jar -DgroupId=com.bugseng.ppl -DartifactId=ppl -Dpackaging=jar -Dversion=1.1

#ap: ppl-1.1/.install
#	cd apron && java_home=$(JAVA_HOME) ./configure -java-prefix $(JAVA_HOME) -no-ocaml -ppl-prefix $(PWD)/../ppl-1.1/.install/ -prefix $(PWD)/.install && make && make install

analyzer: tainttool extern env facade
	cd scala-tools && ./build.sh
