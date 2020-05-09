public class FunCompilerRunner {

    public static void main(String[] args){
        if (args == null || args.length == 0){
            System.err.println("Need .txt file as first argument");
            System.exit(0);
        }
        String file = args[0];
        if (!file.matches(".*\\.txt")){
            System.err.println("Need .txt file");
            System.exit(0);
        }
        if (args.length >= 2){
            Constants.stackSize = Integer.parseInt(args[1]);
        }
        Tokenizer c = new Tokenizer(file);
        c.tokenize();
        HexGenerator p = new HexGenerator(c.getTokenization(), c.getNextDataSegmentAddress());
        p.writeFunctions();
        p.parse();
        p.writeProgram(file.substring(0, file.length() - 4) + ".hex");
    }
}
