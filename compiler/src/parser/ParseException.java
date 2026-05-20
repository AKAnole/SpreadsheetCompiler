package parser;

import lexer.Token;

public class ParseException extends RuntimeException {

    public final Token offendingToken;
    public ParseException(String message, Token offendingToken) {
        super(buildMessage(message, offendingToken));
        this.offendingToken = offendingToken;
    }
    private static String buildMessage(String message, Token t) {
        return String.format(
            "[Syntax Error] at position %d: %s (got %s \"%s\")",
            t.position, message, t.type, t.lexeme
        );
    }
}
