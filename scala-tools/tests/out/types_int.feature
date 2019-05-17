Final return is
	Machine() (imprecision factor = 1) returned with:
		PowerState with (1 disjunct(s)):
			   AbstractState with:
				labels =
					ret	-> Linear
					v1ⁱⁿ|a_byte	-> Linear
					v2ⁱⁿ|a_short	-> Linear
					v3ⁱⁿ|a_int	-> Linear
					v4ⁱⁿ|a_long	-> Linear
				linear = LinearContext (5 dim(s)) with (1 conjunct(s)):
					   v1ⁱⁿ|a_byte + v2ⁱⁿ|a_short + v3ⁱⁿ|a_int + v4ⁱⁿ|a_long + -ret	= 0
		Frame:	block: BB[SSA:-1..-2]2 - Features.types_int(BSIJ)J
			pc: -1:exit
