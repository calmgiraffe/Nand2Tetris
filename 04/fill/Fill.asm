// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel;
// the screen should remain fully black as long as the key is pressed. 
// When no key is pressed, the program clears the screen, i.e. writes
// "white" in every pixel;
// the screen should remain fully clear as long as no key is pressed.

// Recall:
// (KEYBOARD) = 0x6000
// (SCREEN) = 0x4000

// Keyboard:
// When a key is pressed, its 16b character code appears at RAM[KBD]
// character codes range between 32 and 152, inclusive. If no key is pressed, the code 0 appears
// For now, can use a bitwise mask to detect when RAM[KBD] != 0
// Need some type of loop to listen to RAM[KBD]. When R[] != 0, break out of loop

// Screen:
// numRows = 256, wordsPerRow = 32
// Want to loop 256 * 32 = 8192 times
// To fill a word black, point A to the word. Then, set M to -1
// Have one loop for the entire screen filling process. At the end of this loop, listen for keyboard input.
// If no keyboard input, loop again, but setting M to 0 each time

// while true:
//    if key pressed, set flag to -1
//    else, set flag to 0
//    fill screen with flag


  @24576 // Set maxword
  D = A
  @maxword
  M = D
  @16384 // Set currword
  D = A
  @currword
  M = D

(START)
  // Read current value from keyboard port, store in D
  @KBD
  D = M

  // if D != 0 (key press) skip to SETBLACK block
  // else, continue and set the flag to white
  @SETBLACK 
  D; JNE
  @flag
  M = 0
  @FILLSCREEN // Jump to FILLSCREEN as to not set flag to black
  0; JMP

(SETBLACK)
  @flag
  M = -1

(FILLSCREEN)
  // Set word (row of 16 pixels) to flag color
  @flag
  D = M
  @currword
  A = M // Set RAM to point at currword
  M = D // Then set RAM to flag

  // Increment currword, then compare counter and maxRows
  @currword
  M = M + 1
  D = M
  @maxword
  D = D - M
  @FILLSCREEN // If D is negative, iterate to next pixel
  D; JLT

  @16384 // Reset currword once all pixels drawn
  D = A
  @currword
  M = D

  @START
  0; JMP