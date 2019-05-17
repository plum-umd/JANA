# dump LLVM IR
robovm -cp . -dump-intermediates red_jit

####
#### Output directory
####
# temporary output files: (.o and .bc etc.)
# ~/.robovm/cache/<os>/<target>/<build>/<red_jit_abs_path>
# native executable:
# ./red_jit/
