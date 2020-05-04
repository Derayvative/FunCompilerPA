public class Token {

    public enum Kind{
        ASIS,    //As Is Token - the varName will be set to a hex instruction (e.g. E015) and the token will be included as is
        COMMA,   //Comma
        ELSE,    // else
        EMPTY,
        END,     // <end of string>
        EQ,      // =
        EQEQ,    // ==
        FUN,     // Function Definition Start
        ID,      // <identifier>
        IF,      // if
        INT,     // <integer value >
        LBRACE,  // {
        LEFT,    // (
        MINUS,   // -
        MUL,     // *
        NONE,    // <no valid token>
        PLUS,    // +
        PRINT,   // print
        RBRACE,  // }
        RET,     // return
        RIGHT,   // )
        SEMI,    // ;
        WHILE    // while
    }

    public Kind kind;
    public int value;
    public String varName;
    public int start;
    public String enumString;
    int tailRecurse;

    public Token(Kind kind, int value, String enumString) {
        this.kind = kind;
        this.value = value;
        this.enumString = enumString;
    }

    public Token(Kind kind, int value, String enumString, String varName) {
        this.kind = kind;
        this.value = value;
        this.enumString = enumString;
        this.varName = varName;
    }
}
