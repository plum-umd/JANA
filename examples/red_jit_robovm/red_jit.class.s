	.section	__TEXT,__text,regular,pure_instructions
	.macosx_version_min 10, 7
	.globl	"_[j]red_jit[ldcext]"
"_[j]red_jit[ldcext]":                  ## @"[j]red_jit[ldcext]"
## BB#0:                                ## %label0
	pushq	%rbp
	movq	%rsp, %rbp
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rax
	movq	(%rax), %rax
	testq	%rax, %rax
	jne	LBB0_2
## BB#1:                                ## %notLoaded.i
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rsi
	callq	__bcLdcClass
LBB0_2:                                 ## %ldcClassWrapper.exit
	popq	%rbp
	retq

	.globl	"_[j]red_jit[allocator][clinit]"
"_[j]red_jit[allocator][clinit]":       ## @"[j]red_jit[allocator][clinit]"
## BB#0:                                ## %label0
	pushq	%rbp
	movq	%rsp, %rbp
	pushq	%rbx
	pushq	%rax
	movq	%rdi, %rbx
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rax
	testb	$2, 9(%rax)
	jne	LBB1_2
## BB#1:                                ## %label2
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rsi
	movq	%rbx, %rdi
	callq	__bcInitializeClass
LBB1_2:                                 ## %label1
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rsi
	movq	%rbx, %rdi
	callq	__bcAllocate
	addq	$8, %rsp
	popq	%rbx
	popq	%rbp
	retq

	.globl	"_[j]red_jit.iters_to_jit(I)[get][clinit]"
"_[j]red_jit.iters_to_jit(I)[get][clinit]": ## @"[j]red_jit.iters_to_jit(I)[get][clinit]"
## BB#0:                                ## %label0
	pushq	%rbp
	movq	%rsp, %rbp
	pushq	%rbx
	pushq	%rax
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rbx
	testb	$2, 9(%rbx)
	jne	LBB2_2
## BB#1:                                ## %label2
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rsi
	callq	__bcInitializeClass
LBB2_2:                                 ## %label1
	movq	(%rbx), %rax
	movl	160(%rax), %eax
	addq	$8, %rsp
	popq	%rbx
	popq	%rbp
	retq

	.globl	"_[j]red_jit.iters_to_jit(I)[set][clinit]"
"_[j]red_jit.iters_to_jit(I)[set][clinit]": ## @"[j]red_jit.iters_to_jit(I)[set][clinit]"
## BB#0:                                ## %label0
	pushq	%rbp
	movq	%rsp, %rbp
	pushq	%r14
	pushq	%rbx
	movl	%esi, %ebx
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %r14
	testb	$2, 9(%r14)
	jne	LBB3_2
## BB#1:                                ## %label2
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rsi
	callq	__bcInitializeClass
LBB3_2:                                 ## %label1
	movq	(%r14), %rax
	movl	%ebx, 160(%rax)
	popq	%rbx
	popq	%r14
	popq	%rbp
	retq

	.globl	"_[j]red_jit.scale(I)[get][clinit]"
"_[j]red_jit.scale(I)[get][clinit]":    ## @"[j]red_jit.scale(I)[get][clinit]"
## BB#0:                                ## %label0
	pushq	%rbp
	movq	%rsp, %rbp
	pushq	%rbx
	pushq	%rax
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rbx
	testb	$2, 9(%rbx)
	jne	LBB4_2
## BB#1:                                ## %label2
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rsi
	callq	__bcInitializeClass
LBB4_2:                                 ## %label1
	movq	(%rbx), %rax
	movl	164(%rax), %eax
	addq	$8, %rsp
	popq	%rbx
	popq	%rbp
	retq

	.globl	"_[j]red_jit.scale(I)[set][clinit]"
"_[j]red_jit.scale(I)[set][clinit]":    ## @"[j]red_jit.scale(I)[set][clinit]"
## BB#0:                                ## %label0
	pushq	%rbp
	movq	%rsp, %rbp
	pushq	%r14
	pushq	%rbx
	movl	%esi, %ebx
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %r14
	testb	$2, 9(%r14)
	jne	LBB5_2
## BB#1:                                ## %label2
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rsi
	callq	__bcInitializeClass
LBB5_2:                                 ## %label1
	movq	(%r14), %rax
	movl	%ebx, 164(%rax)
	popq	%rbx
	popq	%r14
	popq	%rbp
	retq

	.section	__TEXT,__textcoal_nt,coalesced,pure_instructions
	.globl	"_[J]red_jit.<init>()V"
	.weak_definition	"_[J]red_jit.<init>()V"
"_[J]red_jit.<init>()V":                ## @"[J]red_jit.<init>()V"
## BB#0:                                ## %label0
	pushq	%rbp
	movq	%rsp, %rbp
	## InlineAsm Start
	movq	-65536(%rsp), %rax
	## InlineAsm End
	callq	"_[J]java.lang.Object.<init>()V"
	popq	%rbp
	retq

"L_[J]red_jit.<init>()V_end":

	.globl	"_[J]red_jit.main([Ljava/lang/String;)V"
	.weak_definition	"_[J]red_jit.main([Ljava/lang/String;)V"
"_[J]red_jit.main([Ljava/lang/String;)V": ## @"[J]red_jit.main([Ljava/lang/String;)V"
## BB#0:                                ## %label0
	pushq	%rbp
	movq	%rsp, %rbp
	pushq	%r15
	pushq	%r14
	pushq	%r13
	pushq	%r12
	pushq	%rbx
	pushq	%rax
	movq	%rdi, %r14
	## InlineAsm Start
	movq	-65536(%rsp), %rax
	## InlineAsm End
	movl	$1, -44(%rbp)
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %r13
	movq	(%r13), %rax
	cmpl	$0, 160(%rax)
	js	LBB7_4
## BB#1:
	xorl	%ebx, %ebx
LBB7_2:                                 ## %label2
                                        ## =>This Inner Loop Header: Depth=1
	movq	%r14, %rdi
	callq	"_[j]java.lang.System.out(Ljava/io/PrintStream;)[get][clinit]"
	movq	%rax, %r15
	movq	%r14, %rdi
	callq	"_[j]java.lang.StringBuilder[allocator][clinit]"
	movq	%rax, %r12
	movq	%r14, %rdi
	movq	%r12, %rsi
	callq	"_[J]java.lang.StringBuilder.<init>()V"
	movq	%r14, %rdi
	callq	"_[j]str_branch1_3A_00[ldcstring]"
	movq	%r14, %rdi
	movq	%r12, %rsi
	movq	%rax, %rdx
	callq	"_[J]java.lang.StringBuilder.append(Ljava/lang/String;)Ljava/lang/StringBuilder;"
	movb	(%rax), %cl
	movq	%r14, %rdi
	movq	%rax, %rsi
	movl	%ebx, %edx
	callq	"_[J]java.lang.StringBuilder.append(I)Ljava/lang/StringBuilder;"
	movb	(%rax), %cl
	movq	%r14, %rdi
	movq	%rax, %rsi
	callq	"_[J]java.lang.StringBuilder.toString()Ljava/lang/String;"
	movb	(%r15), %cl
	movq	%r14, %rdi
	movq	%r15, %rsi
	movq	%rax, %rdx
	callq	"_[j]java.io.PrintStream.println(Ljava/lang/String;)V[lookup]"
	movq	%r14, %rdi
	movl	%ebx, %esi
	callq	"_[J]red_jit.branch1(I)I"
	incl	%ebx
	movq	(%r13), %rax
	cmpl	160(%rax), %ebx
	jle	LBB7_2
## BB#3:                                ## %label3
	cmpl	$0, -44(%rbp)
	je	LBB7_5
LBB7_4:                                 ## %label4
	movl	$42, %esi
	movq	%r14, %rdi
	callq	"_[J]red_jit.branch1(I)I"
	jmp	LBB7_6
LBB7_5:                                 ## %label5
	movl	$42, %esi
	movq	%r14, %rdi
	callq	"_[J]red_jit.branch2(I)I"
LBB7_6:                                 ## %label6
	addq	$8, %rsp
	popq	%rbx
	popq	%r12
	popq	%r13
	popq	%r14
	popq	%r15
	popq	%rbp
	retq

"L_[J]red_jit.main([Ljava/lang/String;)V_end":

	.globl	"_[j]str_branch1_3A_00[ldcstring]"
	.weak_definition	"_[j]str_branch1_3A_00[ldcstring]"
	.align	4, 0x90
"_[j]str_branch1_3A_00[ldcstring]":     ## @"[j]str_branch1_3A_00[ldcstring]"
## BB#0:                                ## %label0
	pushq	%rbp
	movq	%rsp, %rbp
	movq	_str_branch1_3A_00_ptr@GOTPCREL(%rip), %rsi
	movq	_str_branch1_3A_00@GOTPCREL(%rip), %rdx
	movl	$8, %ecx
	callq	__bcLdcString
	popq	%rbp
	retq

	.section	__TEXT,__text,regular,pure_instructions
	.globl	"_[j]red_jit.main([Ljava/lang/String;)V[clinit]"
"_[j]red_jit.main([Ljava/lang/String;)V[clinit]": ## @"[j]red_jit.main([Ljava/lang/String;)V[clinit]"
## BB#0:                                ## %label0
	pushq	%rbp
	movq	%rsp, %rbp
	pushq	%r14
	pushq	%rbx
	movq	%rsi, %r14
	movq	%rdi, %rbx
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rax
	testb	$2, 9(%rax)
	jne	LBB9_2
## BB#1:                                ## %label2
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rsi
	movq	%rbx, %rdi
	callq	__bcInitializeClass
LBB9_2:                                 ## %label1
	movq	%rbx, %rdi
	movq	%r14, %rsi
	callq	"_[J]red_jit.main([Ljava/lang/String;)V"
	popq	%rbx
	popq	%r14
	popq	%rbp
	retq

	.section	__TEXT,__textcoal_nt,coalesced,pure_instructions
	.globl	"_[J]red_jit.branch1(I)I"
	.weak_definition	"_[J]red_jit.branch1(I)I"
"_[J]red_jit.branch1(I)I":              ## @"[J]red_jit.branch1(I)I"
## BB#0:                                ## %label0
	pushq	%rbp
	movq	%rsp, %rbp
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rax
	movq	(%rax), %rax
	movl	164(%rax), %eax
	testl	%eax, %eax
	js	LBB10_3
## BB#1:
	xorl	%ecx, %ecx
LBB10_2:                                ## %label2
                                        ## =>This Inner Loop Header: Depth=1
	addl	%ecx, %esi
	incl	%ecx
	cmpl	%eax, %ecx
	jle	LBB10_2
LBB10_3:                                ## %label3
	movl	%esi, %eax
	popq	%rbp
	retq

"L_[J]red_jit.branch1(I)I_end":

	.section	__TEXT,__text,regular,pure_instructions
	.globl	"_[j]red_jit.branch1(I)I[clinit]"
"_[j]red_jit.branch1(I)I[clinit]":      ## @"[j]red_jit.branch1(I)I[clinit]"
## BB#0:                                ## %label0
	pushq	%rbp
	movq	%rsp, %rbp
	pushq	%r14
	pushq	%rbx
	movl	%esi, %r14d
	movq	%rdi, %rbx
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rax
	testb	$2, 9(%rax)
	jne	LBB11_2
## BB#1:                                ## %label2
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rsi
	movq	%rbx, %rdi
	callq	__bcInitializeClass
LBB11_2:                                ## %label1
	movq	%rbx, %rdi
	movl	%r14d, %esi
	callq	"_[J]red_jit.branch1(I)I"
	popq	%rbx
	popq	%r14
	popq	%rbp
	retq

	.section	__TEXT,__textcoal_nt,coalesced,pure_instructions
	.globl	"_[J]red_jit.branch2(I)I"
	.weak_definition	"_[J]red_jit.branch2(I)I"
"_[J]red_jit.branch2(I)I":              ## @"[J]red_jit.branch2(I)I"
## BB#0:                                ## %label0
	pushq	%rbp
	movq	%rsp, %rbp
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rax
	movq	(%rax), %rax
	movl	164(%rax), %eax
	testl	%eax, %eax
	js	LBB12_3
## BB#1:
	xorl	%ecx, %ecx
LBB12_2:                                ## %label2
                                        ## =>This Inner Loop Header: Depth=1
	addl	%ecx, %esi
	incl	%ecx
	cmpl	%eax, %ecx
	jle	LBB12_2
LBB12_3:                                ## %label3
	movl	%esi, %eax
	popq	%rbp
	retq

"L_[J]red_jit.branch2(I)I_end":

	.section	__TEXT,__text,regular,pure_instructions
	.globl	"_[j]red_jit.branch2(I)I[clinit]"
"_[j]red_jit.branch2(I)I[clinit]":      ## @"[j]red_jit.branch2(I)I[clinit]"
## BB#0:                                ## %label0
	pushq	%rbp
	movq	%rsp, %rbp
	pushq	%r14
	pushq	%rbx
	movl	%esi, %r14d
	movq	%rdi, %rbx
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rax
	testb	$2, 9(%rax)
	jne	LBB13_2
## BB#1:                                ## %label2
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rsi
	movq	%rbx, %rdi
	callq	__bcInitializeClass
LBB13_2:                                ## %label1
	movq	%rbx, %rdi
	movl	%r14d, %esi
	callq	"_[J]red_jit.branch2(I)I"
	popq	%rbx
	popq	%r14
	popq	%rbp
	retq

	.section	__TEXT,__textcoal_nt,coalesced,pure_instructions
	.globl	"_[J]red_jit.<clinit>()V"
	.weak_definition	"_[J]red_jit.<clinit>()V"
"_[J]red_jit.<clinit>()V":              ## @"[J]red_jit.<clinit>()V"
## BB#0:                                ## %label0
	pushq	%rbp
	movq	%rsp, %rbp
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rax
	movq	(%rax), %rcx
	movl	$50, 160(%rcx)
	movq	(%rax), %rax
	movl	$10000, 164(%rax)       ## imm = 0x2710
	popq	%rbp
	retq

"L_[J]red_jit.<clinit>()V_end":

	.section	__TEXT,__text,regular,pure_instructions
	.globl	"_[j]red_jit[info]"
"_[j]red_jit[info]":                    ## @"[j]red_jit[info]"
## BB#0:                                ## %label0
	pushq	%rbp
	movq	%rsp, %rbp
	movq	"_[j]red_jit[infostruct]"@GOTPCREL(%rip), %rax
	popq	%rbp
	retq

	.section	__TEXT,__const_coal,coalesced
	.globl	_str_red_5Fjit_2Ejava_00 ## @str_red_5Fjit_2Ejava_00
	.weak_definition	_str_red_5Fjit_2Ejava_00
_str_red_5Fjit_2Ejava_00:
	.asciz	"red_jit.java"

	.section	__DATA,__const
	.align	3                       ## @"[j]red_jit[cattributes]"
"l_[j]red_jit[cattributes]":
	.long	1                       ## 0x1
	.byte	1                       ## 0x1
	.quad	_str_red_5Fjit_2Ejava_00

	.section	__DATA,__datacoal_nt,coalesced
	.globl	_str_branch1_3A_00_ptr  ## @str_branch1_3A_00_ptr
	.weak_definition	_str_branch1_3A_00_ptr
	.align	3
_str_branch1_3A_00_ptr:
	.quad	0

	.section	__TEXT,__const_coal,coalesced
	.globl	_str_branch1_3A_00      ## @str_branch1_3A_00
	.weak_definition	_str_branch1_3A_00
_str_branch1_3A_00:
	.asciz	"branch1:"

	.globl	_str_red_5Fjit_00       ## @str_red_5Fjit_00
	.weak_definition	_str_red_5Fjit_00
_str_red_5Fjit_00:
	.asciz	"red_jit"

	.section	__DATA,__const
	.align	4                       ## @"[j]red_jit[vtable]"
"l_[j]red_jit[vtable]":
	.short	11                      ## 0xb
	.space	6
	.quad	"_[J]java.lang.Object.clone()Ljava/lang/Object;"
	.quad	"_[J]java.lang.Object.equals(Ljava/lang/Object;)Z"
	.quad	"_[J]java.lang.Object.finalize()V"
	.quad	"_[J]java.lang.Object.getClass()Ljava/lang/Class;"
	.quad	"_[J]java.lang.Object.hashCode()I"
	.quad	"_[J]java.lang.Object.notify()V"
	.quad	"_[J]java.lang.Object.notifyAll()V"
	.quad	"_[J]java.lang.Object.toString()Ljava/lang/String;"
	.quad	"_[J]java.lang.Object.wait()V"
	.quad	"_[J]java.lang.Object.wait(J)V"
	.quad	"_[J]java.lang.Object.wait(JI)V"

	.section	__TEXT,__const_coal,coalesced
	.globl	_str_java_2Flang_2FObject_00 ## @str_java_2Flang_2FObject_00
	.weak_definition	_str_java_2Flang_2FObject_00
	.align	4
_str_java_2Flang_2FObject_00:
	.asciz	"java/lang/Object"

	.globl	_str_iters_5Fto_5Fjit_00 ## @str_iters_5Fto_5Fjit_00
	.weak_definition	_str_iters_5Fto_5Fjit_00
_str_iters_5Fto_5Fjit_00:
	.asciz	"iters_to_jit"

	.globl	_str_scale_00           ## @str_scale_00
	.weak_definition	_str_scale_00
_str_scale_00:
	.asciz	"scale"

	.globl	_str__3Cinit_3E_00      ## @str__3Cinit_3E_00
	.weak_definition	_str__3Cinit_3E_00
_str__3Cinit_3E_00:
	.asciz	"<init>"

	.section	__DATA,__datacoal_nt,coalesced
	.globl	"_[j]red_jit.<init>()V[linetable]" ## @"[j]red_jit.<init>()V[linetable]"
	.weak_definition	"_[j]red_jit.<init>()V[linetable]"
	.align	2
"_[j]red_jit.<init>()V[linetable]":
	.long	4294967295              ## 0xffffffff

	.section	__TEXT,__const_coal,coalesced
	.globl	_str_main_00            ## @str_main_00
	.weak_definition	_str_main_00
_str_main_00:
	.asciz	"main"

	.globl	_str__28_5BLjava_2Flang_2FString_3B_29V_00 ## @str__28_5BLjava_2Flang_2FString_3B_29V_00
	.weak_definition	_str__28_5BLjava_2Flang_2FString_3B_29V_00
	.align	4
_str__28_5BLjava_2Flang_2FString_3B_29V_00:
	.asciz	"([Ljava/lang/String;)V"

	.section	__DATA,__datacoal_nt,coalesced
	.globl	"_[j]red_jit.main([Ljava/lang/String;)V[linetable]" ## @"[j]red_jit.main([Ljava/lang/String;)V[linetable]"
	.weak_definition	"_[j]red_jit.main([Ljava/lang/String;)V[linetable]"
	.align	2
"_[j]red_jit.main([Ljava/lang/String;)V[linetable]":
	.long	4294967295              ## 0xffffffff

	.section	__TEXT,__const_coal,coalesced
	.globl	_str_branch1_00         ## @str_branch1_00
	.weak_definition	_str_branch1_00
_str_branch1_00:
	.asciz	"branch1"

	.globl	_str__28I_29I_00        ## @str__28I_29I_00
	.weak_definition	_str__28I_29I_00
_str__28I_29I_00:
	.asciz	"(I)I"

	.section	__DATA,__datacoal_nt,coalesced
	.globl	"_[j]red_jit.branch1(I)I[linetable]" ## @"[j]red_jit.branch1(I)I[linetable]"
	.weak_definition	"_[j]red_jit.branch1(I)I[linetable]"
	.align	2
"_[j]red_jit.branch1(I)I[linetable]":
	.long	4294967295              ## 0xffffffff

	.section	__TEXT,__const_coal,coalesced
	.globl	_str_branch2_00         ## @str_branch2_00
	.weak_definition	_str_branch2_00
_str_branch2_00:
	.asciz	"branch2"

	.section	__DATA,__datacoal_nt,coalesced
	.globl	"_[j]red_jit.branch2(I)I[linetable]" ## @"[j]red_jit.branch2(I)I[linetable]"
	.weak_definition	"_[j]red_jit.branch2(I)I[linetable]"
	.align	2
"_[j]red_jit.branch2(I)I[linetable]":
	.long	4294967295              ## 0xffffffff

	.section	__TEXT,__const_coal,coalesced
	.globl	_str__3Cclinit_3E_00    ## @str__3Cclinit_3E_00
	.weak_definition	_str__3Cclinit_3E_00
_str__3Cclinit_3E_00:
	.asciz	"<clinit>"

	.section	__DATA,__datacoal_nt,coalesced
	.globl	"_[j]red_jit.<clinit>()V[linetable]" ## @"[j]red_jit.<clinit>()V[linetable]"
	.weak_definition	"_[j]red_jit.<clinit>()V[linetable]"
	.align	2
"_[j]red_jit.<clinit>()V[linetable]":
	.long	4294967295              ## 0xffffffff

	.globl	"_[j]red_jit[infostruct]" ## @"[j]red_jit[infostruct]"
	.weak_definition	"_[j]red_jit[infostruct]"
	.align	4
"_[j]red_jit[infostruct]":
	.quad	0
	.long	129                     ## 0x81
	.space	4
	.quad	_str_red_5Fjit_00
	.quad	"_[J]red_jit.<clinit>()V"
	.quad	"_[j]red_jit[typeinfo]"
	.quad	"l_[j]red_jit[vtable]"
	.quad	0
	.long	(0+168)&-1
	.long	(0+16)&-1
	.long	(0+16)&-1
	.short	0                       ## 0x0
	.short	0                       ## 0x0
	.short	0                       ## 0x0
	.short	2                       ## 0x2
	.short	5                       ## 0x5
	.quad	_str_java_2Flang_2FObject_00
	.quad	"l_[j]red_jit[cattributes]"
	.short	20485                   ## 0x5005
	.quad	_str_iters_5Fto_5Fjit_00
	.long	(0+160)&-1
	.short	20485                   ## 0x5005
	.quad	_str_scale_00
	.long	(0+164)&-1
	.short	16385                   ## 0x4001
	.short	65535                   ## 0xffff
	.quad	_str__3Cinit_3E_00
	.byte	9                       ## 0x9
	.quad	"_[J]red_jit.<init>()V"
	.long	"L_[J]red_jit.<init>()V_end" - "_[J]red_jit.<init>()V"
	.quad	"_[j]red_jit.<init>()V[linetable]"
	.short	5                       ## 0x5
	.short	65535                   ## 0xffff
	.quad	_str_main_00
	.quad	_str__28_5BLjava_2Flang_2FString_3B_29V_00
	.quad	"_[J]red_jit.main([Ljava/lang/String;)V"
	.long	"L_[J]red_jit.main([Ljava/lang/String;)V_end" - "_[J]red_jit.main([Ljava/lang/String;)V"
	.quad	"_[j]red_jit.main([Ljava/lang/String;)V[linetable]"
	.short	5                       ## 0x5
	.short	65535                   ## 0xffff
	.quad	_str_branch1_00
	.quad	_str__28I_29I_00
	.quad	"_[J]red_jit.branch1(I)I"
	.long	"L_[J]red_jit.branch1(I)I_end" - "_[J]red_jit.branch1(I)I"
	.quad	"_[j]red_jit.branch1(I)I[linetable]"
	.short	5                       ## 0x5
	.short	65535                   ## 0xffff
	.quad	_str_branch2_00
	.quad	_str__28I_29I_00
	.quad	"_[J]red_jit.branch2(I)I"
	.long	"L_[J]red_jit.branch2(I)I_end" - "_[J]red_jit.branch2(I)I"
	.quad	"_[j]red_jit.branch2(I)I[linetable]"
	.short	16388                   ## 0x4004
	.short	65535                   ## 0xffff
	.quad	_str__3Cclinit_3E_00
	.byte	9                       ## 0x9
	.quad	"_[J]red_jit.<clinit>()V"
	.long	"L_[J]red_jit.<clinit>()V_end" - "_[J]red_jit.<clinit>()V"
	.quad	"_[j]red_jit.<clinit>()V[linetable]"
	.space	4


.subsections_via_symbols
