import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Parser {

    private ArrayList<Token> tokenization;
    private int currentTokenInd;
    private int currentTag;
    private StringBuilder program;
    private HashMap<Integer, Integer> labelToLine;
    private int stackSize = 100;
    private int stackPtr;
    private int startOfMain;
    private HashMap<Integer, Integer> funNumToTokenIndex;

    public Parser(ArrayList<Token> tokenization, int endOfDataSegment) {
        this.tokenization = tokenization;
        currentTokenInd = 0;
        program = new StringBuilder();
        currentTag = 0;
        stackPtr = endOfDataSegment;
        labelToLine = new HashMap<>();
        funNumToTokenIndex = new HashMap<>();
        startOfMain = -1;
    }

    public Token.Kind peek() {
        while (tokenization.get(currentTokenInd).kind == Token.Kind.EMPTY) {
            currentTokenInd += 1;
        }
        return tokenization.get(currentTokenInd).kind;
    }

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
        if (peek() == Token.Kind.LEFT) {
            consume();
            int v = expression(doit);
            if (peek() != Token.Kind.RIGHT) {

            }
            consume();
            return v;
        } else if (peek() == Token.Kind.INT) {
            int v = getValue();
            if (v < 0 || v >= 65536) {
                System.out.println("Invalid Numerical Constant");
            }
            if (doit == 1) {
                int upper = v / 256;
                int lower = v % 256;
                program.append("8" + Integer.toHexString(lower / 16) + Integer.toHexString(lower % 16) + "a\n");
                if (!((upper == 0 && lower < 128) || (upper == 255 && lower >= 128))) {
                    program.append("9" + Integer.toHexString(upper / 16) + Integer.toHexString(upper % 16) + "a\n");
                }
            }
            consume();
            return v;
        } else if (peek() == Token.Kind.FUN) {
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
        //This is for variable that is an argument of the function
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
        } else if (peek() == Token.Kind.ID) {
            String ID = getID();
            int varNum = Integer.parseInt(ID.substring(3));
            consume();
            //Load the variable load address into Reg 12 (C)
            int address = varNum * 2;
            int upper = address / 256;
            int lower = address % 256;
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
                    program.append("8" + Integer.toHexString(lower / 16) + Integer.toHexString(lower % 16) + "c\n");
                    if (!((upper == 0 && lower < 128) || (upper == 255 && lower >= 128))) {
                        program.append("9" + Integer.toHexString(upper / 16) + Integer.toHexString(upper % 16) + "c\n");
                    }
                    program.append("fc0d\n");
                    callFunction();
                    //Moves return register contents into working register contents
                    program.append("060A\n");
                }
            } else if (doit == 1) {
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
        while (peek() == Token.Kind.PLUS || peek() == Token.Kind.MINUS) {
            Token.Kind operation = peek();
            if (doit == 1) {
                pushAToStack();
            }
            consume();
            e2(doit);
            if (doit == 1) {
                popStackToB();
                if (operation == Token.Kind.PLUS) {
                    program.append("1BAA\n");
                } else {
                    program.append("0BAA\n");
                }
            }
        }
        return val;
    }

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
                program.append("5ABA\n");
            }
        }
        return val;
    }

    private int expression(int doit) {
        return e4(doit);
    }

    private int statement(int doit) {
        System.out.println("STate" + peek());
        switch (peek()) {
            case ID:
                String ID = getID();
                if (ID.charAt(0) == '$') {
                    int varNum = Integer.parseInt(ID.substring(1));
                    consume();
                    int register = varNum + 6;
                    if (peek() == Token.Kind.LEFT) {
                        consume();
                        //Load the variable load address into Reg 12 (C)
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
                        if (doit == 1) {
                            program.append("8" + Integer.toHexString(lower / 16) + Integer.toHexString(lower % 16) + "c\n");
                            if (!((upper == 0 && lower < 128) || (upper == 255 && lower >= 128))) {
                                program.append("9" + Integer.toHexString(upper / 16) + Integer.toHexString(upper % 16) + "c\n");
                            }
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
                int currIf = currentTag;
                currentTag += 2;
                if (doit == 1) {
                    program.append("$" + currIf + "\n");
                    program.append("$$" + currIf + "\n");
                    program.append("EA0D\n");
                }
                statement(doit);
                if (peek() == Token.Kind.ELSE && doit == 1) {
                    program.append("$" + (currIf + 1) + "\n");
                    program.append("$$" + (currIf + 1) + "\n");
                    program.append("E00D\n");
                }
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
                if (doit == 1) {
                    program.append("$" + (currWhi + 1) + "\n");
                    program.append("$$" + (currWhi + 1) + "\n");
                    program.append("EA0D\n");
                }
                statement(doit);
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
        for (int i = 0; i < tokenization.size(); i++) {
            System.out.println(tokenization.get(i).enumString);
        }
        System.out.println(program);
        seq(1);
        System.out.println(program);
        replaceLabels();
    }

    private void pushAToStack() {
        program.append("F51A\n");
        program.append("2015\n");
    }

    private void popStackToB() {
        program.append("3015\n");
        program.append("F50B\n");
    }

    private void returnFromFunction() {
        popStackToB();
        program.append("E00B\n");
    }

    private void callFunction() {
        //Pound sign indicates that at this step we need to move the current line number into A.
        saveCallerSavedRegs();
        program.append("#\n");
        program.append("?\n");
        pushAToStack();
        program.append("E00D\n");
        loadCallerSavedRegs();
    }

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
                tokenization.get(correspondingIndex).value = lineNum + stackSize + stackPtr;
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
                    int jumpTarget = labelToLine.get(Integer.parseInt(line.substring(1))) + stackSize + stackPtr;
                    int lower = jumpTarget % 256;
                    int lowerTop = lower / 16;
                    int lowerBot = lower % 16;
                    String lowerTopHex = Integer.toHexString(lowerTop);
                    String lowerBotHex = Integer.toHexString(lowerBot);
                    program.append("8" + lowerTopHex + lowerBotHex + "D" + "\n");
                } else {
                    int jumpTarget = labelToLine.get(Integer.parseInt(line.substring(2))) + stackPtr + stackSize;
                    int upper = jumpTarget / 256;
                    int upperTop = upper / 16;
                    int upperBot = upper % 16;
                    String upperTopHex = Integer.toHexString(upperTop);
                    String upperBotHex = Integer.toHexString(upperBot);
                    program.append("9" + upperTopHex + upperBotHex + "D" + "\n");
                }
                lineNum += 2;
            } else if (line.charAt(0) == '%') {
                program.append("@" + Integer.toHexString((lineNum + stackSize + stackPtr) / 2) + "\n");
            } else if (line.equals("@")) {
                program.append("@" + Integer.toHexString((lineNum + stackSize + stackPtr) / 2) + "\n");
                startOfMain = lineNum + stackSize + stackPtr;
            } else if (line.charAt(0) == '*') {
                int v = tokenization.get(funNumToTokenIndex.get(Integer.parseInt(line.substring(1)))).value;
                int upper = v / 256;
                int lower = v % 256;
                program.append("8" + Integer.toHexString(lower / 16) + Integer.toHexString(lower % 16) + "a\n");
                program.append("9" + Integer.toHexString(upper / 16) + Integer.toHexString(upper % 16) + "a\n");
                lineNum += 4;
            } else if (line.charAt(0) == '#') {
                //Return address. The instruction 4 after this one is the first instruction after the function call
                int v = lineNum + stackPtr + stackSize + 10;
                int upper = v / 256;
                int lower = v % 256;
                program.append("8" + Integer.toHexString(lower / 16) + Integer.toHexString(lower % 16) + "a\n");
                program.append("9" + Integer.toHexString(upper / 16) + Integer.toHexString(upper % 16) + "a\n");
                lineNum += 4;
            } else if (line.charAt(0) == '&') {
                int funKey = Integer.parseInt(line.substring(1));
                int correspondingIndex = funNumToTokenIndex.get(funKey);
                tokenization.get(correspondingIndex).value = lineNum + stackSize + stackPtr;
                program.append("@" + Integer.toHexString((lineNum + stackSize + stackPtr) / 2) + "\n");
            } else if (line.charAt(0) == '?') {

            } else {
                lineNum += 2;
                program.append(line + "\n");
            }
        }
        System.out.println(program + "\n");
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
            //Where stack begins
            outputWriter.write("@5\n");
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
                while (tokenization.get(currentTokenInd).kind != Token.Kind.LBRACE) {
                    currentTokenInd = currentTokenInd + 1;
                }
                statement(1);
                returnFromFunction();
            }
        }
        program.append("@\n");
        System.out.println(program.toString());
        currentTokenInd = 0;
    }

    private void saveCallerSavedRegs() {
        program.append("F517\n");
        program.append("2015\n");
        program.append("F518\n");
        program.append("2015\n");
        program.append("F519\n");
        program.append("2015\n");
    }

    private void loadCallerSavedRegs() {
        program.append("3015\n");
        program.append("F509\n");
        program.append("3015\n");
        program.append("F508\n");
        program.append("3015\n");
        program.append("F507\n");
    }

}
