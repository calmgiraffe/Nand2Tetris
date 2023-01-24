// function SimpleFunction.test 2
(SimpleFunction.test)
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
@frame
M=D
// pseudo-assembly: retAddress = *(frame - 5)
@frame
D=M
@5
A=D-A
D=M
@retAddress
M=D
@SP
AM=M-1
D=M
@ARG
A=M
M=D
@ARG
D=M+1
@SP
M=D
// pseudo-assembly: THAT = *(frame - 1)
@frame
D=M
@1
A=D-A
D=M
@THAT
M=D
// pseudo-assembly: THIS = *(frame - 2)
@frame
D=M
@2
A=D-A
D=M
@THIS
M=D
// pseudo-assembly: ARG = *(frame - 3)
@frame
D=M
@3
A=D-A
D=M
@ARG
M=D
// pseudo-assembly: LCL = *(frame - 4)
@frame
D=M
@4
A=D-A
D=M
@LCL
M=D
@retAddress
A=M
0;JMP
// infinite loop
(END)
@END
0;JMP