.PHONY: all shared computeBound ainterp check check-bounds check-ainterp \
	gen-bounds gen-ainterp clean clean-hard clobber clobber-externals

# regression test settings
TEST_FILES := $(wildcard tests/*.java ../examples/staged/*/*.java)
TEST_CLASSES := $(TEST_FILES:.java=.class)
CPUCOUNT := $(shell getconf _NPROCESSORS_ONLN)
CMD=python ../run-static.py ainterp
BOUNDS_ARGS=-a proceduresummary
INTERBOUNDS_ARGS=-a boundanalysis
AINTERP_ARGS=-a ainterp -t "\-E./ainterp/e1_externals.txt"
OUT_DIR=tests/out

# ainterp tool targets
all:
	make AInterp.jar

help:
	@echo "building ainterp:"
	@echo "\tmake      \tbuilds AInterp.jar (default)"
	@echo "\tmake check\truns regression tests"

AInterp.jar: shared computeBound ainterp
	cd ainterp; sbt assembly

shared: shared/*.scala
	cd shared; sbt clean; sbt publishLocal

computeBound: computeBound/*.scala
	cd computeBound; sbt publishLocal

ainterp: ainterp/*.scala
	mkdir -p ainterp/lib
	cp -R lib/* ainterp/lib/
	cd ainterp; sbt publishLocal

classes: $(TEST_CLASSES)

# regression test targets
%.class: %.java
	javac -g $<

check:
	make -j $(CPUCOUNT) check-feature
	make -j $(CPUCOUNT) check-ainterp
	make -j $(CPUCOUNT) check-bounds
	# make -j $(CPUCOUNT) check-interbounds
	make -j $(CPUCOUNT) check-annot

# bounds regression tests
check-bounds: $(TEST_CLASSES) check-bounds-test1 check-bounds-twovars1 \
	check-bounds-twovars2 check-bounds-twoloops1 check-bounds-nestedloops1 \
	check-bounds-nestedloops2 check-bounds-nestedloopsbackedges check-bounds-e4 \
  # check-bounds-delimsearch check-bounds-delimsearcharray
	# check-bounds-cpwcheck check-bounds-cdelimsearch

check-bounds-test1: $(TEST_CLASSES)
	$(CMD) $(BOUNDS_ARGS) -c $(OUT_DIR)/test1.bounds tests/PPLTest.class "PPLTest.test1(II)I"

check-bounds-twovars1: $(TEST_CLASSES)
	$(CMD) $(BOUNDS_ARGS) -c $(OUT_DIR)/twovars1.bounds tests/PPLTest.class "PPLTest.twovars1(II)I"

check-bounds-twovars2: $(TEST_CLASSES)
	$(CMD) $(BOUNDS_ARGS) -c $(OUT_DIR)/twovars2.bounds tests/PPLTest.class "PPLTest.twovars2(II)I"

check-bounds-twoloops1: $(TEST_CLASSES)
	$(CMD) $(BOUNDS_ARGS) -c $(OUT_DIR)/twoloops1.bounds tests/PPLTest.class "PPLTest.twoloops1(II)I"

check-bounds-nestedloops1: $(TEST_CLASSES)
	$(CMD) $(BOUNDS_ARGS) -c $(OUT_DIR)/nestedloops1.bounds tests/NestedLoops.class "NestedLoops.nestedLoops1(II)I"

check-bounds-nestedloops2: $(TEST_CLASSES)
	$(CMD) $(BOUNDS_ARGS) -c $(OUT_DIR)/nestedloops2.bounds tests/NestedLoops.class "NestedLoops.nestedLoops2(II)I"

check-bounds-nestedloopsbackedges: $(TEST_CLASSES)
	$(CMD) $(BOUNDS_ARGS) -c $(OUT_DIR)/nestedloopsbackedges.bounds tests/NestedLoops.class "NestedLoops.nestedLoopsBackEdges(II)I"

check-bounds-e4: $(TEST_CLASSES)
	$(CMD) $(BOUNDS_ARGS) -c $(OUT_DIR)/e4.bounds -t "\-E./ainterp/e1_externals.txt" ../examples/staged/7/E4.class  "E4.checkit(II)I"

check-bounds-delimsearch: $(TEST_CLASSES)
	$(CMD) $(BOUNDS_ARGS) -c $(OUT_DIR)/delimsearch.bounds -t "\-E./ainterp/e1_externals.txt" ../examples/staged/4/DelimSearch.class "DelimSearch.search(IIII)I"

check-bounds-delimsearcharray: $(TEST_CLASSES)
	$(CMD) $(BOUNDS_ARGS) -c $(OUT_DIR)/delimsearcharray.bounds -t "\-E./ainterp/e1_externals.txt" ../examples/staged/4/DelimSearch.class "DelimSearch.search([B[B)I"

check-bounds-cdelimsearch:
	java -jar AInterp.jar -a bounds --context 2-cfa -E./ainterp/e1_externals.txt -D../information/sample_4/challenge_program/src -Scom.cyberpointllc.stac.delimsearch.DelimSearch.* "com.cyberpointllc.stac.delimsearch.DelimSearch.search(Ljava/lang/String;Ljava/lang/String;)I" | diff $(OUT_DIR)/cdelimsearch.bounds -

check-bounds-cpwcheck:
	java -jar AInterp.jar -a bounds --context 2-cfa -E./ainterp/e1_externals.txt -D../information/sample_3/challenge_program/src -Scom.cyberpointllc.stac.naivepwcheck.NaivePWCheck.* "com.cyberpointllc.stac.naivepwcheck.NaivePWCheck.verifyPassword(Ljava/lang/String;)Z" | diff $(OUT_DIR)/cpwcheck.bounds -

# regression test generation helpers
gen-bounds:
	@echo '$(CMD) $(BOUNDS_ARGS) tests/PPLTest.class "PPLTest.test1(II)I" > $(OUT_DIR)/test1.bounds'
	@echo '$(CMD) $(BOUNDS_ARGS) tests/PPLTest.class "PPLTest.twovars1(II)I" > $(OUT_DIR)/twovars1.bounds'
	@echo '$(CMD) $(BOUNDS_ARGS) tests/PPLTest.class "PPLTest.twovars2(II)I" > $(OUT_DIR)/twovars2.bounds'
	@echo '$(CMD) $(BOUNDS_ARGS) tests/PPLTest.class "PPLTest.twoloops1(II)I" > $(OUT_DIR)/twoloops1.bounds'
	@echo '$(CMD) $(BOUNDS_ARGS) tests/NestedLoops.class "NestedLoops.nestedLoops1(II)I" > $(OUT_DIR)/nestedloops1.bounds'
	@echo '$(CMD) $(BOUNDS_ARGS) tests/NestedLoops.class "NestedLoops.nestedLoops2(II)I" > $(OUT_DIR)/nestedloops2.bounds'
	@echo '$(CMD) $(BOUNDS_ARGS) tests/NestedLoops.class "NestedLoops.nestedLoopsBackEdges(II)I" > $(OUT_DIR)/nestedloopsbackedges.bounds'
	@echo '$(CMD) $(BOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt" ../examples/staged/7/E4.class  "E4.checkit(II)I" > $(OUT_DIR)/e4.bounds'
	@echo '$(CMD) $(BOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt" ../examples/staged/4/DelimSearch.class "DelimSearch.search(IIII)I" > $(OUT_DIR)/delimsearch.bounds'
	@echo '$(CMD) $(BOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt" ../examples/staged/4/DelimSearch.class "DelimSearch.search([B[B)I" > $(OUT_DIR)/delimsearcharray.bounds'
	@echo 'java -jar AInterp.jar -a bounds --context 2-cfa -E./ainterp/e1_externals.txt -D../information/sample_4/challenge_program/src -Scom.cyberpointllc.stac.delimsearch.DelimSearch.* "com.cyberpointllc.stac.delimsearch.DelimSearch.search(Ljava/lang/String;Ljava/lang/String;)I" > $(OUT_DIR)/cdelimsearch.bounds'
	@echo 'java -jar AInterp.jar -a bounds --context 2-cfa -E./ainterp/e1_externals.txt -D../information/sample_3/challenge_program/src -Scom.cyberpointllc.stac.naivepwcheck.NaivePWCheck.* "com.cyberpointllc.stac.naivepwcheck.NaivePWCheck.verifyPassword(Ljava/lang/String;)Z" > $(OUT_DIR)/cpwcheck.bounds'


# interbounds regression tests
check-interbounds: $(TEST_CLASSES) check-interbounds-intertest1 \
	check-interbounds-sumfactorial check-interbounds-delimsearch \
	check-interbounds-pwcheck check-interbounds-cdelimsearch \
	check-interbounds-cpwcheck

check-interbounds-intertest1: tests/InterBounds.class
	$(CMD) $(INTERBOUNDS_ARGS) -c $(OUT_DIR)/intertest1.interbounds -t "\-E./ainterp/e1_externals.txt" $< "InterBounds.test1(II)I"

check-interbounds-sumfactorial: tests/InterBounds.class
	$(CMD) $(INTERBOUNDS_ARGS) -c $(OUT_DIR)/sumfactorial.interbounds -t "\-E./ainterp/e1_externals.txt" $< "InterBounds.sumFactorial(I)I"

check-interbounds-delimsearch: ../examples/staged/4/DelimSearch.class
	$(CMD) $(INTERBOUNDS_ARGS) -c $(OUT_DIR)/interdelimsearch.interbounds -t "\-E./ainterp/e1_externals.txt" $< "DelimSearch.test()I"

check-interbounds-pwcheck: ../examples/staged/3/PWCheck.class
	$(CMD) $(INTERBOUNDS_ARGS) -c $(OUT_DIR)/interpwcheck.interbounds -t "\-E./ainterp/e1_externals.txt" $< "PWCheck.test()Z"

check-interbounds-cpwcheck:
	java -jar AInterp.jar -a interbounds --context 1-cfa -E./ainterp/e1_externals.txt -D../information/sample_3/challenge_program/src -Scom.cyberpointllc.stac.* "com.cyberpointllc.stac.host.Main.main([Ljava/lang/String;)V" | diff $(OUT_DIR)/cpwcheck.interbounds -

check-interbounds-cdelimsearch:
	java -jar AInterp.jar -a interbounds --context 2-cfa -E./ainterp/e1_externals.txt -D../information/sample_4/challenge_program/src -Scom.cyberpointllc.stac.* "com.cyberpointllc.stac.host.Main.main([Ljava/lang/String;)V" | diff $(OUT_DIR)/cdelimsearch.interbounds -

gen-interbounds:
	@echo '$(CMD) $(INTERBOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt" tests/InterBounds.class "InterBounds.test1(II)I" > $(OUT_DIR)/intertest1.interbounds'
	@echo '$(CMD) $(INTERBOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt" tests/InterBounds.class "InterBounds.sumFactorial(I)I" > $(OUT_DIR)/sumfactorial.interbounds'
	@echo '$(CMD) $(INTERBOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt" ../examples/staged/4/DelimSearch.class "DelimSearch.test()I" > $(OUT_DIR)/interdelimsearch.interbounds'
	@echo '$(CMD) $(INTERBOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt" ../examples/staged/3/PWCheck.class "PWCheck.test()Z" > $(OUT_DIR)/interpwcheck.interbounds'
	@echo 'java -jar AInterp.jar -a interbounds --context 1-cfa -E./ainterp/e1_externals.txt -D../information/sample_3/challenge_program/src -Scom.cyberpointllc.stac.* "com.cyberpointllc.stac.host.Main.main([Ljava/lang/String;)V" > $(OUT_DIR)/cpwcheck.interbounds'
	@echo 'java -jar AInterp.jar -a interbounds --context 2-cfa -E./ainterp/e1_externals.txt -D../information/sample_4/challenge_program/src -Scom.cyberpointllc.stac.* "com.cyberpointllc.stac.host.Main.main([Ljava/lang/String;)V" > $(OUT_DIR)/cdelimsearch.interbounds'

# ainterp regression tests
check-ainterp: $(TEST_CLASSES) \
	check-ainterp-test1 check-ainterp-test2 check-ainterp-test3 check-ainterp-test4 check-ainterp-test5 \
	check-ainterp-twovars1 check-ainterp-twovars2 \
	check-ainterp-twoloops1
check-ainterp-test1: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/test1.ainterp tests/PPLTest.class "PPLTest.test1(II)I"
check-ainterp-test2: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/test2.ainterp tests/PPLTest.class "PPLTest.test2(III)I"
check-ainterp-test3: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/test3.ainterp tests/PPLTest.class "PPLTest.test3(II)I"
check-ainterp-test4: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/test4.ainterp tests/PPLTest.class "PPLTest.test4(II)I"
check-ainterp-test5: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/test5.ainterp tests/PPLTest.class "PPLTest.test5(I)I"
check-ainterp-twovars1: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/twovars1.ainterp tests/PPLTest.class "PPLTest.twovars1(II)I"
check-ainterp-twovars2: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/twovars2.ainterp tests/PPLTest.class "PPLTest.twovars2(II)I"
check-ainterp-twoloops1: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/twoloops1.ainterp tests/PPLTest.class "PPLTest.twoloops1(II)I"

gen-ainterp:
	@echo '$(CMD) $(AINTERP_ARGS) tests/PPLTest.class "PPLTest.test1(II)I" > $(OUT_DIR)/test1.ainterp'
	@echo '$(CMD) $(AINTERP_ARGS) tests/PPLTest.class "PPLTest.test2(III)I" > $(OUT_DIR)/test2.ainterp'
	@echo '$(CMD) $(AINTERP_ARGS) tests/PPLTest.class "PPLTest.test3(II)I" > $(OUT_DIR)/test3.ainterp'
	@echo '$(CMD) $(AINTERP_ARGS) tests/PPLTest.class "PPLTest.test4(II)I" > $(OUT_DIR)/test4.ainterp'
	@echo '$(CMD) $(AINTERP_ARGS) tests/PPLTest.class "PPLTest.test5(I)I" > $(OUT_DIR)/test5.ainterp'
	@echo '$(CMD) $(AINTERP_ARGS) tests/PPLTest.class "PPLTest.twovars1(II)I" > $(OUT_DIR)/twovars1.ainterp'
	@echo '$(CMD) $(AINTERP_ARGS) tests/PPLTest.class "PPLTest.twovars2(II)I" > $(OUT_DIR)/twovars2.ainterp'
	@echo '$(CMD) $(AINTERP_ARGS) tests/PPLTest.class "PPLTest.twoloops1(II)I" > $(OUT_DIR)/twoloops1.ainterp'

# ainterp with annotations regression test
check-annot: check-annot-test1 check-annot-test2 check-annot-testAnnotSyntax
check-annot-test1: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/test1.annot -t "-A tests/PPLTest.yml" tests/PPLTest.class "PPLTest.test1(II)I"
check-annot-test2: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/test2.annot -t "-A tests/PPLTest.yml" tests/PPLTest.class "PPLTest.test2(III)I"
check-annot-testAnnotSyntax: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/testAnnotSyntax.annot -t "-A tests/PPLTest.yml" tests/PPLTest.class "PPLTest.testAnnotSyntax(III)I"

gen-annot:
	@echo '$(CMD) $(AINTERP_ARGS) -t "-A tests/PPLTest.yml" tests/PPLTest.class "PPLTest.test1(II)I" > $(OUT_DIR)/test1.annot'
	@echo '$(CMD) $(AINTERP_ARGS) -t "-A tests/PPLTest.yml" tests/PPLTest.class "PPLTest.test2(III)I" > $(OUT_DIR)/test2.annot'
	@echo '$(CMD) $(AINTERP_ARGS) -t "-A tests/PPLTest.yml" tests/PPLTest.class "PPLTest.testAnnotSyntax(III)I" > $(OUT_DIR)/testAnnotSyntax.annot'

check-feature: \
	check-feature-disjunct \
	check-feature-types-int \
	check-feature-div-const  check-feature-div-const-neg  check-feature-div \
	check-feature-mult-const check-feature-mult-const-neg check-feature-mult \
	check-feature-mod-const  check-feature-mod-const-neg  check-feature-mod

check-feature-disjunct: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/disjunct.feature tests/Features.class "Features.disjunct(II)I"
check-feature-types-int: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/types_int.feature tests/Features.class "Features.types_int(BSIJ)J"
check-feature-div-const: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/div_const.feature tests/Features.class "Features.div_const(I)I"
check-feature-div-const-neg: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/div_const_neg.feature tests/Features.class "Features.div_const_neg(I)I"
check-feature-div: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/div.feature tests/Features.class "Features.div(II)I"
check-feature-mult-const: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/mult_const.feature tests/Features.class "Features.mult_const(I)I"
check-feature-mult-const-neg: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/mult_const_neg.feature tests/Features.class "Features.mult_const_neg(I)I"
check-feature-mult: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/mult.feature tests/Features.class "Features.mult(II)I"
check-feature-mod-const: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/mod_const.feature tests/Features.class "Features.mod_const(I)I"
check-feature-mod-const-neg: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/mod_const_neg.feature tests/Features.class "Features.mod_const_neg(I)I"
check-feature-mod: $(TEST_CLASSES)
	$(CMD) $(AINTERP_ARGS) -c $(OUT_DIR)/mod.feature tests/Features.class "Features.mod(II)I"

gen-feature:
	@echo '$(CMD) $(AINTERP_ARGS) tests/Features.class "Features.disjunct(II)I" > $(OUT_DIR)/disjunct.feature'
	@echo '$(CMD) $(AINTERP_ARGS) tests/Features.class "Features.types_int(BSIJ)J" > $(OUT_DIR)/types_int.feature'
	@echo '$(CMD) $(AINTERP_ARGS) tests/Features.class "Features.div_const(I)I" > $(OUT_DIR)/div_const.feature'
	@echo '$(CMD) $(AINTERP_ARGS) tests/Features.class "Features.div_const_neg(I)I" > $(OUT_DIR)/div_const_neg.feature'
	@echo '$(CMD) $(AINTERP_ARGS) tests/Features.class "Features.div(II)I" > $(OUT_DIR)/div.feature'
	@echo '$(CMD) $(AINTERP_ARGS) tests/Features.class "Features.mult_const(I)I" > $(OUT_DIR)/mult_const.feature'
	@echo '$(CMD) $(AINTERP_ARGS) tests/Features.class "Features.mult_const_neg(I)I" > $(OUT_DIR)/mult_const_neg.feature'
	@echo '$(CMD) $(AINTERP_ARGS) tests/Features.class "Features.mult(II)I" > $(OUT_DIR)/mult.feature'
	@echo '$(CMD) $(AINTERP_ARGS) tests/Features.class "Features.mod_const(I)I" > $(OUT_DIR)/mod_const.feature'
	@echo '$(CMD) $(AINTERP_ARGS) tests/Features.class "Features.mod_const_neg(I)I" > $(OUT_DIR)/mod_const_neg.feature'
	@echo '$(CMD) $(AINTERP_ARGS) tests/Features.class "Features.mod(II)I" > $(OUT_DIR)/mod.feature'

# staged examples runners
e1: ../examples/staged/1/E1.class
	$(CMD) $(BOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt --debug" $< "E1.foo(IIIII)I"

e2: ../examples/staged/2/E2.class
	$(CMD) $(BOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt --debug" $< "E2.checkPwI(IIIII)I"

pwcheck: ../examples/staged/3/PWCheck.class
	$(CMD) $(BOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt" $< "PWCheck.verifyS([B)Z"

delimsearch: ../examples/staged/4/DelimSearch.class
	$(CMD) $(BOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt --debug" $< "DelimSearch.search(IIII)I"

delimsearcharray: ../examples/staged/4/DelimSearch.class
	$(CMD) $(BOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt --debug" $< "DelimSearch.search([B[B)I"

delimsearch2: ../examples/staged/5/DelimSearch2.class
	$(CMD) $(BOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt" $< "DelimSearch2.search(BBBBBBBB)I"

e4: ../examples/staged/7/E4.class
	$(CMD) $(BOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt --debug" $< "E4.checkit(II)I"

intertest1: tests/InterBounds.class
	$(CMD) $(INTERBOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt --debug" $< "InterBounds.test1(II)I"

sumfactorial: tests/InterBounds.class
	$(CMD) $(INTERBOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt --debug" $< "InterBounds.sumFactorial(I)I"

interdelimsearch: ../examples/staged/4/DelimSearch.class
	$(CMD) $(INTERBOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt --debug" $< "DelimSearch.test()I"

interpwcheck: ../examples/staged/3/PWCheck.class
	$(CMD) $(INTERBOUNDS_ARGS) -t "\-E./ainterp/e1_externals.txt" $< "PWCheck.test()Z"

# cleanup targets
clean:
	cd shared; sbt clean
	cd computeBound; sbt clean
	cd ainterp; sbt clean
	rm -f AInterp.jar

clobber:
	make clean
	cd ainterp; rm -rf project/project/; rm -rf project/target/; rm -r target/*
	cd shared/; rm -rf project/*; rm -rf target/*
	cd computeBound/; rm -rf project/*; rm -rf target/*
	rm -rf ~/.ivy2/local/edu.umd.soucis
	rm -rf ~/.ivy2/cache/edu.umd.soucis

clobber-externals:
	rm -rf ~/.m2/repository/edu/joana
	rm -rf ~/.m2/repository/com/ibm
	rm -rf ~/.ivy2/local/edu.illinois.wala
	rm -rf ~/.ivy2/cache/edu.illinois.wala/
	rm -rf ~/.ivy2/cache/edu.joana
	rm -rf ~/.ivy2/cache/com.ibm.wala
