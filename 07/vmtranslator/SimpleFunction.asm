// function SimpleFunction.test 2
(SimpleFunction.SimpleFunction.test)
@0
D=A
@SP
M=M+1
A=M-1
M=D
@0
D=A
@SP
M=M+1
A=M-1
M=D
// push local 0
@LCL
D=M
@0
A=D+A
D=M
@SP
M=M+1
A=M-1
M=D
// push local 1
@LCL
D=M
@1
A=D+A
D=M
@SP
M=M+1
A=M-1
M=D
// add
@SP
AM=M-1
D=M
@SP
A=M-1
M=M+D
// not
@SP
A=M-1
M=!M
// push argument 0
@ARG
D=M
@0
A=D+A
D=M
@SP
M=M+1
A=M-1
M=D
// add
@SP
AM=M-1
D=M
@SP
A=M-1
M=M+D
// push argument 1
@ARG
D=M
@1
A=D+A
D=M
@SP
M=M+1
A=M-1
M=D
// sub
@SP
AM=M-1
D=M
@SP
A=M-1
M=M-D
// return
@LCL
D=M
@R13
M=D
// pseudo-assembly: R14 = *(R13 - 5)
@R13
D=M
@5
A=D-A
D=M
@R14
M=D
@SP
A=M-1
D=M
@ARG
A=M
M=D
@ARG
D=M+1
@SP
M=D
// pseudo-assembly: THAT = *(R13 - 1)
@R13
D=M
@1
A=D-A
D=M
@THAT
M=D
// pseudo-assembly: THIS = *(R13 - 2)
@R13
D=M
@2
A=D-A
D=M
@THIS
M=D
// pseudo-assembly: ARG = *(R13 - 3)
@R13
D=M
@3
A=D-A
D=M
@ARG
M=D
// pseudo-assembly: LCL = *(R13 - 4)
@R13
D=M
@4
A=D-A
D=M
@LCL
M=D
@R13
A=M
0;JMP
// infinite loop
(END)
@END
0;JMP