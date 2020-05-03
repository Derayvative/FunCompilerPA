public class FunCompilerRunner {

    public static void main(String[] args){
        Compiler c = new Compiler("input.txt");
        c.tokenize();
        for (int i = 0; i < c.getTokenization().size(); i++){
            System.out.println(c.getTokenization().get(i).enumString);
        }
        Parser p = new Parser(c.getTokenization(), c.getNextDataSegmentAddress());
        p.writeFunctions();
        p.parse();
        p.writeProgram("output.hex");
    }
}
