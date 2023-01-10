// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Mult.asm

// Multiplies R0 and R1 and stores the result in R2.
// (R0, R1, R2 refer to RAM[0], RAM[1], and RAM[2], respectively)
//
// This program only needs to handle arguments that satisfy
// R0 >= 0, R1 >= 0, and R0*R1 < 32768.

// Assume: R0 and R1 already contain the numbers we want to multiply
// Recall: M = RAM[A]


// Initialize RAM[2], the location of the product, to 0
  @R2
  M = 0

// Initialize RAM[3], the location of the counter i, to 0
  @R3
  M = 0

// If either a or b == 0, immediately jump to end
// any number * 0 = 0, and 0 is already stored in RAM[2]
  @R0
  D = M
  @END
  D; JEQ
  @R1
  D = M
  @END
  D; JEQ

// Want to loop a times (assume that a > 0)
// sum = b + b + .. + b (a times)
// for (i = 0; i < a; i++) {
//    sum += b
// }

// Because you can't transfer from RAM[] to RAM[], have to store one of them in register
// Move RAM[2], currSum, to D
(LOOP)
  @R2
  D = M
// Add b to D, which currently has multiple of a
// Copy this to RAM[2], to be returned or used again in another iteration
  @R1
  D = D + M
  @R2
  M = D
// Increment the counter
  @R3
  M = M + 1
// Store the curr value of i in D
// Compare a with D. If D < a, jmp to loop start
  D = M
  @R0
  D = D - M
  @LOOP
  D; JLT

// Infinite loop at end of program
(END)
  @END
  0; JMP