@256
D=A
@SP
M=D
// null
@null$ret.0
D=A
@SP
M=M+1
A=M-1
M=D
@LCL
D=M
@SP
M=M+1
A=M-1
M=D
@ARG
D=M
@SP
M=M+1
A=M-1
M=D
@THIS
D=M
@SP
M=M+1
A=M-1
M=D
@THAT
D=M
@SP
M=M+1
A=M-1
M=D
@5
D=A
@null
D=D+A
@SP
D=M-D
@ARG
M=D
@SP
D=M
@LCL
M=D
@Sys.init
0;JMP
(null$ret.0)
@Sys.init
0;JMP
// function Main.fibonacci 0
(Main.fibonacci)
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
// push constant 2
@2
D=A
@SP
M=M+1
A=M-1
M=D
// lt                     // checks if n<2
@SP
AM=M-1
D=M
A=A-1
D=M-D
M=-1
@EQ_jump0
D;JLT
@SP
A=M-1
M=0
(EQ_jump0)
// if-goto IF_TRUE
@SP
AM=M-1
D=M
@Main.fibonacci$IF_TRUE
D;JNE
// goto IF_FALSE
@Main.fibonacci$IF_FALSE
0;JMP
// label IF_TRUE          // if n<2, return n
(Main.fibonacci$IF_TRUE)
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
// label IF_FALSE         // if n>=2, returns fib(n-2)+fib(n-1)
(Main.fibonacci$IF_FALSE)
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
// push constant 2
@2
D=A
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
// call Main.fibonacci 1  // computes fib(n-2)
@Main.fibonacci$ret.1
D=A
@SP
M=M+1
A=M-1
M=D
@LCL
D=M
@SP
M=M+1
A=M-1
M=D
@ARG
D=M
@SP
M=M+1
A=M-1
M=D
@THIS
D=M
@SP
M=M+1
A=M-1
M=D
@THAT
D=M
@SP
M=M+1
A=M-1
M=D
@5
D=A
@1
D=D+A
@SP
D=M-D
@ARG
M=D
@SP
D=M
@LCL
M=D
@Main.fibonacci
0;JMP
(Main.fibonacci$ret.1)
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
// push constant 1
@1
D=A
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
// call Main.fibonacci 1  // computes fib(n-1)
@Main.fibonacci$ret.2
D=A
@SP
M=M+1
A=M-1
M=D
@LCL
D=M
@SP
M=M+1
A=M-1
M=D
@ARG
D=M
@SP
M=M+1
A=M-1
M=D
@THIS
D=M
@SP
M=M+1
A=M-1
M=D
@THAT
D=M
@SP
M=M+1
A=M-1
M=D
@5
D=A
@1
D=D+A
@SP
D=M-D
@ARG
M=D
@SP
D=M
@LCL
M=D
@Main.fibonacci
0;JMP
(Main.fibonacci$ret.2)
// add                    // returns fib(n-1) + fib(n-2)
@SP
AM=M-1
D=M
@SP
A=M-1
M=M+D
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
// function Sys.init 0
(Sys.init)
// push constant 4
@4
D=A
@SP
M=M+1
A=M-1
M=D
// call Main.fibonacci 1   // computes the 4'th fibonacci element
@Sys.init$ret.3
D=A
@SP
M=M+1
A=M-1
M=D
@LCL
D=M
@SP
M=M+1
A=M-1
M=D
@ARG
D=M
@SP
M=M+1
A=M-1
M=D
@THIS
D=M
@SP
M=M+1
A=M-1
M=D
@THAT
D=M
@SP
M=M+1
A=M-1
M=D
@5
D=A
@1
D=D+A
@SP
D=M-D
@ARG
M=D
@SP
D=M
@LCL
M=D
@Main.fibonacci
0;JMP
(Sys.init$ret.3)
// label WHILE
(Sys.init$WHILE)
// goto WHILE              // loops infinitely
@Sys.init$WHILE
0;JMP
// infinite loop
(END)
@END
0;JMP