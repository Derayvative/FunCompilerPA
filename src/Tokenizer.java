import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

//Converts File containing fun language code into arraylist of tokens.
public class Tokenizer {

    //File name
    private String programFile;

    //This address is the last address of the data segment and is used to pick memory addresses for variables used in fun code
    private int nextDataSegmentAddress = 12;

    //ArrayList of Tokens that is what is fed into the Hex generator
    private ArrayList<Token> tokenization;

    public Tokenizer(String programFile) {
        this.programFile = programFile;
        tokenization = new ArrayList<>();
    }

    //Tokenizaation is very similar to P2
    public void tokenize(){
        StringReader reader = null;
        try {
            //This returns a StringBuilder with the contents of the file
            reader = buildFileContents(programFile);
            //Next character in file
            int nxt = 0;
            String currentWord = "";
            int numAdjEquals = 0;
            int valueBeingBuilt = 0;
            boolean isValBeingBuilt = false;
            //Maps the name of a variable to its memory address on the data segment.
            HashMap<String, Integer> dataSegmentMap = new HashMap<>();
            while ((nxt = reader.read()) != -1){
                char nxtChar = (char)nxt;
                if (currentWord.length() >= 1 && !(((nxtChar >= 'a' && nxtChar <= 'z') || (nxtChar >= 'A' && nxtChar <= 'Z') || (nxtChar >= '0' && nxtChar <= '9')))) {
                    //If statement in fun
                    if (currentWord.equals("if")){
                        //System.out.println("if");
                        Token t = new Token(Token.Kind.IF, 0, "IF");
                        tokenization.add(t);
                    }
                    //While in fun
                    else if (currentWord.equals("while")){
                        //System.out.println("while");
                        Token t = new Token(Token.Kind.WHILE, 0, "WHILE");
                        tokenization.add(t);
                    }
                    //ELse in fun
                    else if (currentWord.equals("else")){
                        //System.out.println("else");
                        Token t = new Token(Token.Kind.ELSE, 0, "ELSE");
                        tokenization.add(t);
                    }
                    //Print in fun
                    else if (currentWord.equals("print")){
                        //System.out.println("print");
                        Token t = new Token(Token.Kind.PRINT, 0, "PRINT");
                        tokenization.add(t);
                    }
                    //Return in fun
                    else if (currentWord.equals("return")){
                        Token t = new Token(Token.Kind.RET, 0, "RET");
                        tokenization.add(t);
                    }
                    //Start of function definition in fun
                    else if (currentWord.equals("fun")){
                        //System.out.println("fun");
                        Token t = new Token(Token.Kind.FUN, 0, "FUN");
                        tokenization.add(t);
                    }
                    //In a fun program, you can put $ + hex to do in-line hex code. E.g. if your program has $0a00
                    //the processor will move register A contents to register 0 when it gets thee
                    else if (currentWord.charAt(0) == '$'){
                        Token t = new Token(Token.Kind.ASIS, 0, "ASIS", currentWord.substring(1));
                        tokenization.add(t);
                    }
                    //Variable name
                    else{
                        //System.out.println(currentWord);
                        String varName = "";
                        //If this is a new variable, put a spot for in the data segment
                        if (!dataSegmentMap.containsKey(currentWord)) {
                            dataSegmentMap.put(currentWord, nextDataSegmentAddress);
                            nextDataSegmentAddress = nextDataSegmentAddress + 2;
                        }
                        varName = "var" + (dataSegmentMap.get(currentWord) / 2);
                        Token t = new Token(Token.Kind.ID, 0, "ID", varName);
                        tokenization.add(t);
                    }
                    currentWord = "";
                }
                //Numeric constant
                else if ((isValBeingBuilt && !((nxtChar >= '0' && nxtChar <= '9') || (nxtChar == '_')) && currentWord.length() == 0)){
                    Token t = new Token(Token.Kind.INT, valueBeingBuilt, "INT");
                    tokenization.add(t);
                    isValBeingBuilt = false;
                    //System.out.println(valueBeingBuilt);
                    valueBeingBuilt = 0;
                }
                if (nxtChar == '=') {
                    numAdjEquals++;
                }
                //'='
                else if (numAdjEquals == 1){
                    Token t = new Token(Token.Kind.EQ, 0, "EQ");
                    tokenization.add(t);
                    //System.out.println("=");
                    numAdjEquals = 0;
                }
                //==
                else if (numAdjEquals == 2){
                    Token t = new Token(Token.Kind.EQEQ, 0, "EQEQ");
                    tokenization.add(t);
                    //System.out.println("==");
                    numAdjEquals = 0;
                }
                //Braces
                if (nxtChar == '{'){
                    Token t = new Token(Token.Kind.LBRACE, 0, "LBRACE");
                    tokenization.add(t);
                    //System.out.println("{");
                }
                else if (nxtChar == '}'){
                    Token t = new Token(Token.Kind.RBRACE, 0, "RBRACE");
                    tokenization.add(t);
                    //System.out.println("}");
                }
                //Parenthesis
                else if (nxtChar == '('){
                    Token t = new Token(Token.Kind.LEFT, 0, "LEFT");
                    tokenization.add(t);
                    //System.out.println("(");
                }
                else if (nxtChar == ')'){
                    Token t = new Token(Token.Kind.RIGHT, 0, "RIGHT");
                    tokenization.add(t);
                    //System.out.println(")");
                }
                //Comma (for function arguments like add(x,y)
                else if (nxtChar == ','){
                    Token t = new Token(Token.Kind.COMMA, 0, "COMMA");
                    tokenization.add(t);
                }
                //Addition
                else if (nxtChar == '+'){
                    Token t = new Token(Token.Kind.PLUS, 0, "PLUS");
                    tokenization.add(t);
                    //System.out.println("+");
                }
                //Subtraction (multiplication not supported)
                else if (nxtChar == '-'){
                    Token t = new Token(Token.Kind.MINUS, 0, "MINUS");
                    tokenization.add(t);
                }
                //Semicolon
                else if (nxtChar == ';'){
                    //System.out.println(";");
                }
                //Builds word
                if ((nxtChar >= 'a' && nxtChar <= 'z')  || (nxtChar >= 'A' && nxtChar <= 'Z') || (nxtChar >= '0' && nxtChar <= '9' && currentWord.length() >= 1) || (nxtChar == '$' && currentWord.length() == 0)){
                    currentWord = currentWord + nxtChar;
                }
                //BUilds numeric value
                if (((nxtChar >= '0' && nxtChar <= '9') || nxtChar == '_') && currentWord.length() == 0){
                    if (nxtChar != '_'){
                        if (!isValBeingBuilt){
                            isValBeingBuilt = true;
                            valueBeingBuilt = 0;
                        }
                        int digit = nxtChar - '0';
                        valueBeingBuilt *= 10;
                        valueBeingBuilt += digit;
                    }
                }
            }
            //At the end, we could still be building a word or numeric value, so we do a final check
            if (currentWord.length() >= 1 ) {

                if (currentWord.equals("if")){
                    //System.out.println("if");
                    Token t = new Token(Token.Kind.IF, 0, "IF");
                    tokenization.add(t);
                }
                else if (currentWord.equals("while")){
                    //System.out.println("while");
                    Token t = new Token(Token.Kind.WHILE, 0, "WHILE");
                    tokenization.add(t);
                }
                else if (currentWord.equals("else")){
                    //System.out.println("else");
                    Token t = new Token(Token.Kind.ELSE, 0, "ELSE");
                    tokenization.add(t);
                }
                else if (currentWord.equals("print")){
                    //System.out.println("print");
                    Token t = new Token(Token.Kind.PRINT, 0, "PRINT");
                    tokenization.add(t);
                }
                else if (currentWord.equals("fun")){
                    //System.out.println("fun");
                    Token t = new Token(Token.Kind.FUN, 0, "FUN");
                    tokenization.add(t);
                }
                else{
                    //System.out.println(currentWord);
                    Token t = new Token(Token.Kind.ID, 0, "ID");
                    tokenization.add(t);
                    if (!dataSegmentMap.containsKey(currentWord)){
                        dataSegmentMap.put(currentWord, nextDataSegmentAddress);
                        nextDataSegmentAddress = nextDataSegmentAddress + 2;
                    }
                }
                currentWord = "";
            }
            else if ((isValBeingBuilt && currentWord.length() == 0)){
                Token t = new Token(Token.Kind.INT, valueBeingBuilt, "INT");
                tokenization.add(t);
                isValBeingBuilt = false;
                //System.out.println(valueBeingBuilt);
                valueBeingBuilt = 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        tokenization.add(new Token(Token.Kind.END, 0, ""));
        substituteStackPointerSet();
        tokenizeFunctionArguments();
    }

    //In our code for switching cores, the size of the stack could vary, so we don't know the address of the stack immediately
    //Therefore, in our code for switching core, we put $xxx1 to indicate replace this line with moving the lower order bitsaddress of stack
    //of core 1 to reg 4. We put $yyy1 to indicate move the high order bits of the stack address to reg 5.
    private void substituteStackPointerSet() {
        for (int i = 0; i < tokenization.size(); i++){

            if (tokenization.get(i).kind == Token.Kind.ASIS && tokenization.get(i).varName.strip().length() >= 4 && tokenization.get(i).varName.strip().substring(0,3).equals("xxx")) {
                int stackNum = Integer.parseInt(tokenization.get(i).varName.substring(3));
                int stackAddressLower = (stackNum * Constants.stackSize + nextDataSegmentAddress) % 256;
                int upper = stackAddressLower / 16;
                int lower = stackAddressLower % 16;
                tokenization.get(i).varName = "8" + Integer.toHexString(upper) + Integer.toHexString(lower) + "5";
                tokenization.get(i).varName = "8" + Integer.toHexString(upper) + Integer.toHexString(lower) + "5";
            }
            else if (tokenization.get(i).kind == Token.Kind.ASIS && tokenization.get(i).varName.strip().length() >= 4 && tokenization.get(i).varName.strip().substring(0,3).equals("yyy")) {
                int stackNum = Integer.parseInt(tokenization.get(i).varName.substring(3));
                int stackAddressUpper = (stackNum * Constants.stackSize + nextDataSegmentAddress) / 256;
                int upper = stackAddressUpper / 16;
                int lower = stackAddressUpper % 16;
                tokenization.get(i).varName = "9" + Integer.toHexString(upper) + Integer.toHexString(lower) + "5";
                tokenization.get(i).varName = "9" + Integer.toHexString(upper) + Integer.toHexString(lower) + "5";
            }
        }
    }

    //Converts file address into string reader containng its contents
    private StringReader buildFileContents(String programFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(programFile));
        StringBuilder newProgram = new StringBuilder();
        String nxt;
        newProgram.append(getMultiCoreFunctions());
        while ((nxt = reader.readLine()) != null){
            newProgram.append(nxt + "\n");
        }
        return new StringReader(newProgram.toString());
    }

    //This is for functions with arguments. We run this on them, so that the function arguments are passed by value (local).
    //E.g. even if you have a global variable named x and you have a function with argument x, calling the function will not
    //replace the value of the global variable x
    public void tokenizeFunctionArguments(){
        for (int i = 0; i < tokenization.size(); i++){
            if (tokenization.get(i).kind == Token.Kind.FUN && tokenization.get(i+1).kind == Token.Kind.LEFT){
                int argIn = i + 2;
                HashMap<String, String> argNameMap = new HashMap<>();
                int argNum = 1;
                while (tokenization.get(argIn).kind == Token.Kind.ID || tokenization.get(argIn).kind == Token.Kind.COMMA || tokenization.get(argIn).kind == Token.Kind.RIGHT){
                    if (tokenization.get(argIn).kind == Token.Kind.ID){
                        argNameMap.put(tokenization.get(argIn).varName,"$" + argNum);
                        tokenization.get(argIn).varName = "$" + argNum;
                        argNum++;
                    }
                    argIn++;
                }
                //By here argIn is the index of the right parenthesis if there are arguments
                int funBodyIn = argIn;
                if (tokenization.get(funBodyIn).kind != Token.Kind.LBRACE){

                }
                else{
                    int bracketBalance = 1;
                    funBodyIn = funBodyIn + 1;
                    while (bracketBalance > 0){
                        if (tokenization.get(funBodyIn).kind == Token.Kind.ID && argNameMap.containsKey(tokenization.get(funBodyIn).varName)){
                            tokenization.get(funBodyIn).varName = argNameMap.get(tokenization.get(funBodyIn).varName);
                        }
                        else if (tokenization.get(funBodyIn).kind == Token.Kind.LBRACE){
                            bracketBalance++;
                        }
                        else if (tokenization.get(funBodyIn).kind == Token.Kind.RBRACE){
                            bracketBalance--;
                        }
                        funBodyIn++;
                    }
                }
            }
        }
    }

    public int getNextDataSegmentAddress() {
        return nextDataSegmentAddress;
    }

    public ArrayList<Token> getTokenization() {
        return tokenization;
    }

    public void setTokenization(ArrayList<Token> tokenization) {
        this.tokenization = tokenization;
    }

    //Prepends the contents of standardlib.txt, which is for supporting multicore operations
    //Adds the functions wake(coreNum, pc) and pause(core) and resume(core)
    private String getMultiCoreFunctions() throws IOException {
        //If dollar sign is at the end of a line, it means interpret this line as hex. Don't try to convert it.
        //Syntax: wake(a,b) - wake core a (mod num cores) at pc b
        BufferedReader reader = new BufferedReader(new FileReader("standardlib.txt"));
        StringBuilder newProgram = new StringBuilder();
        String nxt;
        String lib = "";
        while ((nxt = reader.readLine()) != null) {
            if (nxt.strip().length() > 0 && nxt.strip().charAt(0) == '~') {
                int stackNum = Integer.parseInt(nxt.strip().substring(1));
                int startOfStack = nextDataSegmentAddress + stackNum * Constants.stackSize;
                int upper = startOfStack / 256;
                int lower = startOfStack % 256;
                if (upper == 0 && lower < 128 || upper == 255 && lower >= 128) {
                    nxt = "\t$8" + Integer.toHexString(lower / 16) + Integer.toHexString(lower % 16) + "5";
                } else {
                    nxt = "\t$8" + Integer.toHexString(lower / 16) + Integer.toHexString(lower % 16) + "5\n";
                    nxt = nxt + "\t$9" + Integer.toHexString(upper / 16) + Integer.toHexString(upper % 16) + "5";
                }
            }
            lib = lib + (nxt + "\n");
        }
        return lib;
    }
}
