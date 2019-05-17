#!/bin/bash
mvn install:install-file -Dfile=./joana2/dist/joana.api.jar -DgroupId=edu.joana -DartifactId=api -Dpackaging=jar -Dversion=1.0
mvn install:install-file -Dfile=./joana2/dist/joana.ifc.sdg.core.jar -DgroupId=edu.joana -DartifactId=sdg -Dpackaging=jar -Dversion=1.0
mvn install:install-file -Dfile=./joana2/dist/joana.ui.ifc.sdg.graphviewer.jar -DgroupId=edu.joana -DartifactId=graphviewer -Dpackaging=jar -Dversion=1.0
mvn install:install-file -Dfile=./joana2/dist/joana.ui.ifc.wala.console.jar -DgroupId=edu.joana -DartifactId=console -Dpackaging=jar -Dversion=1.0
mvn install:install-file -Dfile=./joana2/dist/joana.wala.core.jar -DgroupId=edu.joana -DartifactId=wala-core -Dpackaging=jar -Dversion=1.0
mvn install:install-file -Dfile=./joana2/dist/joana.wala.dictionary.jar -DgroupId=edu.joana -DartifactId=wala-dictionary -Dpackaging=jar -Dversion=1.0
