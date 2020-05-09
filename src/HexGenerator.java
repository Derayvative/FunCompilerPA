import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class HexGenerator {

    //Tokens from the Tokenizer
    private ArrayList<Token> tokenization;
    //Current index we are parsing in the tokenization ArrayList
    private int currentTokenInd;
    private int currentTag;
    //String output (hexcode) to be run on our processor
    private StringBuilder program;

    private HashMap<Integer, Integer> labelToLine;
    //Size of stack desired
    private int stackSize;
    //Address of first stack  pointer
    private int stackPtr;
    //Number of stacks you need
    int numStacks;
    //Start of main (e.g. past the stack set up , data segment, and code for functions)
    private int startOfMain;
    //Maps the number associated with a function to the index it starts
    private HashMap<Integer, Integer> funNumToTokenIndex;

    public HexGenerator(ArrayList<Token> tokenization, int endOfDataSegment) {
        this.tokenization = tokenization;
        currentTokenInd = 0;
        program = new StringBuilder();
        currentTag = 0;
        stackPtr = endOfDataSegment;
        labelToLine = new HashMap<>();
        funNumToTokenIndex = new HashMap<>();
        startOfMain = -1;
        stackSize = Constants.stackSize;
        numStacks = Constants.numStacks;
    }

    //Gets next nonempty kind in the tokenization arraylist
    public Token.Kind peek() {
        while (tokenization.get(currentTokenInd).kind == Token.Kind.EMPTY) {
            currentTokenInd += 1;
        }
        return tokenization.get(currentTokenInd).kind;
    }

    //Pops the current token in tokenization
    void consume() {
        peek();
        currentTokenInd++;
        while (tokenization.get(currentTokenInd).kind == Token.Kind.EMPTY) {
            currentTokenInd += 1;
        }
    }

    String getID() {
        return tokenization.get(currentTokenInd).varName;
    }

    int getValue() {
        return tokenization.get(currentTokenInd).value;
    }

    private int e1(int doit) {
        //Parenthesis evaluation
        if (peek() == Token.Kind.LEFT) {
            consume();
            int v = expression(doit);
            if (peek() != Token.Kind.RIGHT) {

            }
            consume();
            return v;
        }
        //Numeric constant evaluation
        else if (peek() == Token.Kind.INT) {
            int v = getValue();
            if (v < 0 || v >= 65536) {

            }
            if (doit == 1) {
                int upper = v / 256;
                int lower = v % 256;
                //Moves numeric value into register 10
                program.append("8" + Integer.toHexString(lower / 16) + Integer.toHexString(lower % 16) + "a\n");
                if (!((upper == 0 && lower < 128) || (upper == 255 && lower >= 128))) {
                    program.append("9" + Integer.toHexString(upper / 16) + Integer.toHexString(upper % 16) + "a\n");
                }
            }
            consume();
            return v;
        }
        //Function definition
        else if (peek() == Token.Kind.FUN) {
            //The function might be defined below the call (since we can jump back), so we put an * and ? to indicate
            //that later on we need to replace these with the address of the function variable
            if (doit == 1) {
                program.append("*" + getValue() + "\n");
                program.append("?" + getValue() + "\n");
            }
            consume();
            while (peek() != Token.Kind.LBRACE) {
                consume();
            }
            statement(0);
            return 1;
        }
        //This is for variable that is an argument of the function (dollar sign in ID indicates function argument)
        //We only support up to 3 args, but this is not the main focus of our project
        else if (peek() == Token.Kind.ID && getID().charAt(0) == '$') {
            String ID = getID();
            int varNum = Integer.parseInt(ID.substring(1));
            consume();
            //$1 -> Register 7, $2 -> Register 8, $3 -> Register 9
            int register = varNum + 6;
            if (peek() == Token.Kind.LEFT) {
                consume();
                int varCount = 0;
                while (peek() != Token.Kind.RIGHT) {
                    expression(doit);
                    if (doit == 1) {
                        program.append("0A0" + (varCount + 7) + "\n");
                    }
                    if (peek() == Token.Kind.COMMA) {
                        consume();
                    }
                    varCount++;
                }
                consume();
                if (doit == 1) {
                    program.append("0" + Integer.toHexString(register) + "0d\n");
                    callFunction();
                    //Moves return register contents into working register contents
                    program.append("060A\n");
                }
            } else if (doit == 1) {
                program.append("0" + Integer.toHexString(register) + "0a\n");
            }
            return 1;
        }
        //Variable, for both the value of the variable (e.g. x = y) or value of return (e.g. x = y())
        else if (peek() == Token.Kind.ID) {
            String ID = getID();
            int varNum = Integer.parseInt(ID.substring(3));
            consume();
            //Load the variable load address into Reg 12 (C)
            int address = varNum * 2;
            int upper = address / 256;
            int lower = address % 256;
            //Checks for function call. If so it calls function and then gets the return value
            if (peek() == Token.Kind.LEFT) {
                consume();
                int varCount = 0;
                while (peek() != Token.Kind.RIGHT) {
                    expression(doit);
                    if (doit == 1) {
                        program.append("0A0" + (varCount + 7) + "\n");
                    }
                    if (peek() == Token.Kind.COMMA) {
                        consume();
                    }
                    varCount++;
                }
                consume();
                if (doit == 1) {
                    //Moves address of function to regC
                    program.append("8" + Integer.toHexString(lower / 16) + Integer.toHexString(lower % 16) + "c\n");
                    if (!((upper == 0 && lower < 128) || (upper == 255 && lower >= 128))) {
                        program.append("9" + Integer.toHexString(upper / 16) + Integer.toHexString(upper % 16) + "c\n");
                    }
                    //Gets value of function from address
                    program.append("fc0d\n");
                    //Calls function
                    callFunction();
                    //Moves return register contents into working register contents
                    program.append("060A\n");
                }
            }
            //Value of numeric constant
            else if (doit == 1) {
                //Moves value of numeric constant into register C
                program.append("8" + Integer.toHexString(lower / 16) + Integer.toHexString(lower % 16) + "c\n");
                if (!((upper == 0 && lower < 128) || (upper == 255 && lower >= 128))) {
                    program.append("9" + Integer.toHexString(upper / 16) + Integer.toHexString(upper % 16) + "c\n");
                }
                program.append("fc0a\n");
            }
            return 1;
        } else {
            return 0;
        }
    }

    private int e2(int doit) {
        int val = e1(doit);
        //Multiplication is not supported currently (tokenizer will never produce MUL token)
        while (peek() == Token.Kind.MUL) {
            if (doit == 1) {
                pushAToStack();
            }
            consume();
            e2(doit);
            if (doit == 1) {
                popStackToB();
                program.append("4ABA\n");
            }

        }
        return val;
    }

    private int e3(int doit) {
        int val = e2(doit);
        //Plus and Minus have same priority
        while (peek() == Token.Kind.PLUS || peek() == Token.Kind.MINUS) {
            Token.Kind operation = peek();
            //Stores first subtraction operand on stack
            if (doit == 1) {
                pushAToStack();
            }
            consume();
            e2(doit);
            if (doit == 1) {
                //Gets back first subtraction operand from stack
                popStackToB();
                //Add contents of register B and A and puts it into A
                if (operation == Token.Kind.PLUS) {
                    program.append("1BAA\n");
                }
                //Reg[A] = Reg[B] - Reg[A]
                else {
                    program.append("0BAA\n");
                }
            }
        }
        return val;
    }

    //== -> returns 1 if equal, 0 if not equal
    private int e4(int doit) {
        int val = e3(doit);
        while (peek() == Token.Kind.EQEQ) {
            if (doit == 1) {
                pushAToStack();
            }
            consume();
            e3(doit);
            if (doit == 1) {
                popStackToB();
                //New instruction for our processor that returns 1 A and B equal, 0 otherwise
                program.append("5ABA\n");
            }
        }
        return val;
    }

    private int expression(int doit) {
        return e4(doit);
    }

    private int statement(int doit) {
        switch (peek()) {
            //Insert hexcode as is
            case ASIS:
                String hex = tokenization.get(currentTokenInd).varName;
                if (doit == 1) {
                    program.append(hex + "\n");
                }
                consume();
                return 1;
            //Variable
            case ID:
                String ID = getID();
                //This is the case where the variable is a local function argument, in which case we get it from the register
                if (ID.charAt(0) == '$') {
                    int varNum = Integer.parseInt(ID.substring(1));
                    consume();
                    int register = varNum + 6;
                    //If we are trying to call a function that was passed as a local variable
                    if (peek() == Token.Kind.LEFT) {
                        consume();
                        //Load the variable load address into Reg 12 (C)
                        int varCount = 0;
                        //Stores arguments for function call
                        while (peek() != Token.Kind.RIGHT) {
                            expression(doit);
                            if (doit == 1) {
                                program.append("0A0" + (varCount + 7) + "\n");
                            }
                            if (peek() == Token.Kind.COMMA) {
                                consume();
                            }
                            varCount++;
                        }
                        consume();
                        //Calls function.
                        if (doit == 1) {
                            program.append("0" + Integer.toHexString(register) + "0d\n");
                            callFunction();
                        }
                    } else {
                        if (peek() != Token.Kind.EQ) {

                        }
                        consume();
                        int v = expression(doit);
                        if (doit == 1) {
                            //Load the variable load address into Reg 12 (C)
                            program.append("0A0" + register + "\n");
                        }
                    }
                } else {
                    int varNum = Integer.parseInt(ID.substring(3));
                    consume();
                    if (peek() == Token.Kind.LEFT) {
                        consume();
                        //Load the variable load address into Reg 12 (C)
                        int address = varNum * 2;
                        int upper = address / 256;
                        int lower = address % 256;
                        int varCount = 0;
                        //Stores function arguments before call
                        while (peek() != Token.Kind.RIGHT) {
                            expression(doit);
                            if (doit == 1) {
                                program.append("0A0" + (varCount + 7) + "\n");
                            }
                            if (peek() == Token.Kind.COMMA) {
                                consume();
                            }
                            varCount++;
                        }
                        //Calls function
                        if (doit == 1) {
                            //Gets address where address of first instruction of function is stored
                            program.append("8" + Integer.toHexString(lower / 16) + Integer.toHexString(lower % 16) + "c\n");
                            if (!((upper == 0 && lower < 128) || (upper == 255 && lower >= 128))) {
                                program.append("9" + Integer.toHexString(upper / 16) + Integer.toHexString(upper % 16) + "c\n");
                            }
                            //Load contents of address c into register d
                            program.append("fc0d\n");
                            callFunction();
                        }

                        //Consumes the ID, L parenthesis, R parenthesis
                        consume();
                    } else {
                        if (peek() != Token.Kind.EQ) {

                        }
                        consume();
                        int v = expression(doit);
                        if (doit == 1) {
                            //Load the variable load address into Reg 12 (C)
                            int address = varNum * 2;
                            int upper = address / 256;
                            int lower = address % 256;
                            program.append("8" + Integer.toHexString(lower / 16) + Integer.toHexString(lower % 16) + "c\n");
                            if (!((upper == 0 && lower < 128) || (upper == 255 && lower >= 128))) {
                                program.append("9" + Integer.toHexString(upper / 16) + Integer.toHexString(upper % 16) + "c\n");
                            }
                            program.append("fc1a\n");
                        }
                    }
                }
                if (peek() == Token.Kind.SEMI) {
                    consume();
                }
                return 1;
            case LBRACE:
                consume();
                seq(doit);
                if (peek() != Token.Kind.RBRACE) {

                }
                consume();
                return 1;
            case IF:
                consume();
                int condition = expression(doit);
                //currIf numerically identifies every if statement and else statement
                int currIf = currentTag;
                currentTag += 2;
                //$ and $$ are effectively labels used to indicate where to jump to. Later on, these tags are replaced
                //with the numeric address we need to jump to
                //This part is for jumping over the if body, if the condition is false
                if (doit == 1) {
                    //Gets address of end of if body
                    program.append("$" + currIf + "\n");
                    program.append("$$" + currIf + "\n");
                    program.append("EA0D\n");
                }
                statement(doit);
                //Jumps to end of else at end of if body
                if (peek() == Token.Kind.ELSE && doit == 1) {
                    program.append("$" + (currIf + 1) + "\n");
                    program.append("$$" + (currIf + 1) + "\n");
                    program.append("E00D\n");
                }
                //% indicates the end of the if, else, while and is later replaced with @ annotation in the hex file
                if (doit == 1) {
                    program.append("%" + currIf + "\n");
                }
                if (peek() == Token.Kind.ELSE) {
                    consume();
                    statement(doit);
                    if (doit == 1) {
                        program.append("%" + (currIf + 1) + "\n");
                    }

                }
                return 1;
            case WHILE:
                consume();
                int currWhi = currentTag;
                currentTag += 2;
                if (doit == 1) {
                    program.append("%" + (currWhi) + "\n");
                }
                int conditionWhile = expression(doit);
                //If condition evaluates to 0, this jumps to the end of the while loop
                if (doit == 1) {
                    program.append("$" + (currWhi + 1) + "\n");
                    program.append("$$" + (currWhi + 1) + "\n");
                    program.append("EA0D\n");
                }
                statement(doit);
                //Jumps back to condition evaluation
                if (doit == 1) {
                    program.append("$" + (currWhi) + "\n");
                    program.append("$$" + (currWhi) + "\n");
                    program.append("E00D\n");
                    program.append("%" + (currWhi + 1) + "\n");
                }
                return 1;
            case PRINT:
                consume();
                expression(doit);
                if (doit == 1) {
                    program.append("0A00\n");
                }
                return 1;
            case RET:
                consume();
                //return value should be in Register A (10)
                expression(doit);
                if (doit == 1) {
                    //Moves return value into return register (6)
                    program.append("0A06\n");
                    //Pops return address off of stack and goes back to there
                    returnFromFunction();
                }
            default:
                return 0;
        }
    }

    private void seq(int doit) {
        while (statement(doit) != 0) {

        }
    }

    public void parse() {
        seq(1);
        replaceLabels();
    }

    //5, in our convention, is the stack pointer.
    //This stores contents of A at sp address and increments the sp
    private void pushAToStack() {
        program.append("F51A\n");
        program.append("2025\n");
    }

    //This decrements the pointer, and puts the contents of new top of stack
    //into Register B
    private void popStackToB() {
        program.append("3025\n");
        program.append("F50B\n");
    }

    //The return address is at top of stack. This gets return address and jumps to it.
    private void returnFromFunction() {
        popStackToB();
        program.append("E00B\n");
    }

    private void callFunction() {
        //Pound and ? sign indicates that at this step we need to move the current line number into A.
        //This is to save the return address on our stack
        saveCallerSavedRegs();
        program.append("#\n");
        program.append("?\n");
        pushAToStack();
        //Jump to start of function
        program.append("E00D\n");
        //This part runs after we return from the function called and restores the caller saved register
        loadCallerSavedRegs();
    }

    //For whilles and ifs and elses mostly. This in essence replaces labels with line numbers so the code can run on the processor
    private void mapLabelToLine() {
        Scanner sc = new Scanner(program.toString());
        int lineNum = 0;
        while (sc.hasNextLine()) {
            String next = sc.nextLine();
            if (next.charAt(0) == '%') {
                labelToLine.put(Integer.parseInt(next.substring(1)), lineNum);
            } else {
                if (next.charAt(0) == '#' || next.charAt(0) == '*') {
                    lineNum += 4;
                } else if (next.charAt(0) == '@' || next.charAt(0) == '?' || next.charAt(0) == '&') {
                    lineNum = lineNum + 0;
                } else {
                    lineNum += 2;
                }
            }

            if (next.charAt(0) == '&') {
                int funKey = Integer.parseInt(next.substring(1));
                int correspondingIndex = funNumToTokenIndex.get(funKey);
                tokenization.get(correspondingIndex).value = lineNum + stackSize * numStacks + stackPtr;
            }
        }
    }

    //Replaces labels with annotations or hex code for our processor.
    private void replaceLabels() {
        mapLabelToLine();
        Scanner sc = new Scanner(program.toString());
        program = new StringBuilder();
        int lineNum = 0;
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.charAt(0) == '$') {
                if (line.length() > 1 && line.charAt(1) != '$') {
                    int jumpTarget = labelToLine.get(Integer.parseInt(line.substring(1))) + stackSize * numStacks + stackPtr;
                    int lower = jumpTarget % 256;
                    int lowerTop = lower / 16;
                    int lowerBot = lower % 16;
                    String lowerTopHex = Integer.toHexString(lowerTop);
                    String lowerBotHex = Integer.toHexString(lowerBot);
                    program.append("8" + lowerTopHex + lowerBotHex + "D" + "\n");
                } else {
                    int jumpTarget = labelToLine.get(Integer.parseInt(line.substring(2))) + stackPtr + stackSize * numStacks;
                    int upper = jumpTarget / 256;
                    int upperTop = upper / 16;
                    int upperBot = upper % 16;
                    String upperTopHex = Integer.toHexString(upperTop);
                    String upperBotHex = Integer.toHexString(upperBot);
                    program.append("9" + upperTopHex + upperBotHex + "D" + "\n");
                }
                lineNum += 2;
            } else if (line.charAt(0) == '%') {
                program.append("@" + Integer.toHexString((lineNum + stackSize * numStacks + stackPtr) / 2) + "\n");
            } else if (line.equals("@")) {
                program.append("@" + Integer.toHexString((lineNum + stackSize * numStacks + stackPtr) / 2) + "\n");
                startOfMain = lineNum + stackSize * numStacks + stackPtr;
            } else if (line.charAt(0) == '*') {
                int v = tokenization.get(funNumToTokenIndex.get(Integer.parseInt(line.substring(1)))).value;
                int upper = v / 256;
                int lower = v % 256;
                program.append("8" + Integer.toHexString(lower / 16) + Integer.toHexString(lower % 16) + "a\n");
                program.append("9" + Integer.toHexString(upper / 16) + Integer.toHexString(upper % 16) + "a\n");
                lineNum += 4;
            } else if (line.charAt(0) == '#') {
                //Return address. The instruction 4 after this one is the first instruction after the function call
                int v = lineNum + stackPtr + stackSize * numStacks + 10;
                int upper = v / 256;
                int lower = v % 256;
                program.append("8" + Integer.toHexString(lower / 16) + Integer.toHexString(lower % 16) + "a\n");
                program.append("9" + Integer.toHexString(upper / 16) + Integer.toHexString(upper % 16) + "a\n");
                lineNum += 4;
            } else if (line.charAt(0) == '&') {
                int funKey = Integer.parseInt(line.substring(1));
                int correspondingIndex = funNumToTokenIndex.get(funKey);
                tokenization.get(correspondingIndex).value = lineNum + stackSize * numStacks + stackPtr;
                program.append("@" + Integer.toHexString((lineNum + stackSize * numStacks + stackPtr) / 2) + "\n");
            } else if (line.charAt(0) == '?') {

            } else {
                lineNum += 2;
                program.append(line + "\n");
            }
        }
    }

    public void writeProgram(String outputFileName) {
        try {
            FileWriter outputWriter = new FileWriter(outputFileName);
            //Writes instructions at top setting stack pointer and telling it to skip over data segment
            outputWriter.write("@0\n");
            //Set stack pointer
            int upper = stackPtr / 256;
            int lower = stackPtr % 256;
            outputWriter.write("8" + Integer.toHexString(lower / 16) + Integer.toHexString(lower % 16) + "5\n");
            outputWriter.write("9" + Integer.toHexString(upper / 16) + Integer.toHexString(upper % 16) + "5\n");
            //Load first instruction address and jump to it
            int firstIns = startOfMain;
            int upperFI = firstIns / 256;
            int lowerFI = firstIns % 256;
            outputWriter.write("8" + Integer.toHexString(lowerFI / 16) + Integer.toHexString(lowerFI % 16) + "D\n");
            outputWriter.write("9" + Integer.toHexString(upperFI / 16) + Integer.toHexString(upperFI % 16) + "D\n");
            outputWriter.write("E00D\n");
            outputWriter.write("FFFF\n");
            //Where stack begins
            outputWriter.write("@6\n");
            outputWriter.write("@" + Integer.toHexString(stackPtr / 2) + "\n");
            //Where program begins
            //outputWriter.write("@" + Integer.toHexString((stackPtr + stackSize)/2) + "\n");
            outputWriter.write(program.toString());
            outputWriter.write("FFFF\n");
            outputWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //Writes the contents of all functions defined in the program (this is done before the main in the hex file)
    public void writeFunctions() {
        int funNum = 1;
        for (int i = 0; i < tokenization.size(); i++) {
            if (tokenization.get(i).kind == Token.Kind.FUN) {
                funNumToTokenIndex.put(funNum, i);
                tokenization.get(i).value = funNum;
                funNum++;
            }
        }
        for (int i = 0; i < tokenization.size(); i++) {
            if (tokenization.get(i).kind == Token.Kind.FUN) {
                program.append("&" + tokenization.get(i).value + "\n");
                currentTokenInd = i + 1;
                //THis is done to skip over the function arguments. E.g. start after the next left brace
                while (tokenization.get(currentTokenInd).kind != Token.Kind.LBRACE) {
                    currentTokenInd = currentTokenInd + 1;
                }
                //Adds contents of statement into hex
                statement(1);
                //Adds return code
                returnFromFunction();
            }
        }
        program.append("@\n");
        currentTokenInd = 0;
    }

    ///Saves reg 7,8,9 on stack
    private void saveCallerSavedRegs() {
        program.append("F517\n");
        program.append("2025\n");
        program.append("F518\n");
        program.append("2025\n");
        program.append("F519\n");
        program.append("2025\n");
    }

    //Restores reg 7,8,9 from stack
    private void loadCallerSavedRegs() {
        program.append("3025\n");
        program.append("F509\n");
        program.append("3025\n");
        program.append("F508\n");
        program.append("3025\n");
        program.append("F507\n");
    }

}
