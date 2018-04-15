Final return is
	Machine() (imprecision factor = 2) returned with:
		PowerState with (2 disjunct(s)):
			   AbstractState with:
				labels =
					ret	-> Linear
					v1ⁱⁿ|a	-> Linear
					v2ⁱⁿ|b	-> Linear
				linear = LinearContext (3 dim(s)) with (2 conjunct(s)):
					   -v1ⁱⁿ|a + v2ⁱⁿ|b	≥ 1
					 ∧ ret	= 1
			 ∨ AbstractState with:
				labels =
					ret	-> Linear
					v1ⁱⁿ|a	-> Linear
					v2ⁱⁿ|b	-> Linear
				linear = LinearContext (3 dim(s)) with (3 conjunct(s)):
					   -ret	≥ -1
					 ∧ ret	≥ 0
					 ∧ v1ⁱⁿ|a + -v2ⁱⁿ|b + -ret	≥ 0
		Frame:	block: BB[SSA:-1..-2]4 - Features.disjunct(II)I
			pc: -1:exit
