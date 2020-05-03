import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Compiler {

    private String programFile;

    private int nextDataSegmentAddress = 10;

    private ArrayList<Token> tokenization;

    public Compiler(String programFile) {
        this.programFile = programFile;
        tokenization = new ArrayList<>();
    }

    public void tokenize(){
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(programFile));
            int nxt = 0;
            String currentWord = "";
            int numAdjEquals = 0;
            int valueBeingBuilt = 0;
            boolean isValBeingBuilt = false;
            //Maps the name of a variable to its memory address on the data segment.
            HashMap<String, Integer> dataSegmentMap = new HashMap<>();
            while ((nxt = reader.read()) != -1){
                char nxtChar = (char)nxt;
                if (currentWord.length() >= 1 && !(((nxtChar >= 'a' && nxtChar <= 'z') || (nxtChar >= '0' && nxtChar <= '9')))) {
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
                    else if (currentWord.equals("return")){
                        Token t = new Token(Token.Kind.RET, 0, "RET");
                        tokenization.add(t);
                    }
                    else if (currentWord.equals("fun")){
                        //System.out.println("fun");
                        Token t = new Token(Token.Kind.FUN, 0, "FUN");
                        tokenization.add(t);
                    }
                    else{
                        //System.out.println(currentWord);
                        String varName = "";
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
                else if (numAdjEquals == 1){
                    Token t = new Token(Token.Kind.EQ, 0, "EQ");
                    tokenization.add(t);
                    //System.out.println("=");
                    numAdjEquals = 0;
                }
                else if (numAdjEquals == 2){
                    Token t = new Token(Token.Kind.EQEQ, 0, "EQEQ");
                    tokenization.add(t);
                    //System.out.println("==");
                    numAdjEquals = 0;
                }

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
                else if (nxtChar == ','){
                    Token t = new Token(Token.Kind.COMMA, 0, "COMMA");
                    tokenization.add(t);
                }
                else if (nxtChar == '*'){
                    Token t = new Token(Token.Kind.MUL, 0, "MUL");
                    tokenization.add(t);
                    //System.out.println("*");
                }
                else if (nxtChar == '+'){
                    Token t = new Token(Token.Kind.PLUS, 0, "PLUS");
                    tokenization.add(t);
                    //System.out.println("+");
                }
                else if (nxtChar == '-'){
                    Token t = new Token(Token.Kind.MINUS, 0, "MINUS");
                    tokenization.add(t);
                }
                else if (nxtChar == ';'){
                    //System.out.println(";");
                }
                if ((nxtChar >= 'a' && nxtChar <= 'z') || (nxtChar >= '0' && nxtChar <= '9' && currentWord.length() >= 1)){
                    currentWord = currentWord + nxtChar;
                }
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
        tokenizeFunctionArguments();
    }

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
                        System.out.println(tokenization.get(argIn).varName);
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
                            System.out.println(tokenization.get(funBodyIn).varName);
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
                System.out.println(tokenization.get(funBodyIn).kind);
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

}
