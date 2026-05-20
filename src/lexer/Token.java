package lexer;


public final class Token {

    public final TokenType type;
    public final String lexeme;
    public final int position;

    public Token(TokenType type, String lexeme, int position) {
        this.type     = type;
        this.lexeme   = lexeme;
        this.position = position;
    }

    public static Token eof(int position) {
        return new Token(TokenType.EOF, "", position);
    }

    @Override
    public String toString() {
        return String.format("%s(\"%s\")@%d", type, lexeme, position);
    }
}
