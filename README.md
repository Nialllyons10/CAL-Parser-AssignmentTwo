# CAL-Parser-AssignmentTwo

## Semantic Analysis and Intermediate Representation

### Aim

CA  | Result
------------- | -------------
Assignment 2 | 14/15
__Total__ | __93%__

The aim of this assignment is to add semantic analysis checks and intermediate representation generation to the lexical and syntax analyser you have implement in Assignment 1. The generated intermediate code should be a 3-address code and stored in a file with the ".ir" extension.

You will need to extend your submission for Assignment 1 to:

- Generate an Abstract Syntax Tree.
- Add a Symbol Table that can handle scope.
- Perform a set of semtantic checks. This following is a list of typical sematic checks:
- Is every identifier declared within scope before its is used?
- Is no identifier declared more than once in the same scope?
- Is the left-hand side of an assignment a variable of the correct type?
- Are the arguments of an arithmetic operator the integer variables or integer constants?
- Are the arguments of a boolean operator boolean variables or boolean constants?
- Is there a function for every invoked identifier?
- Does every function call have the correct number of arguments?
- Is every variable both written to and read from?
- Is every function called?
- Generate an Intermediate Representation using 3-address code.
- Feel free to add any additional semantic checks you can think of!

The .jar file for a 3-Address Code Interpreter is available at: here. It is decribed at https://www.computing.dcu.ie/~davids/courses/CA4003/taci.pdf.
