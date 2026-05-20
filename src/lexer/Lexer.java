package lexer;

import java.util.ArrayList;
import java.util.List;

public class Lexer {

    private final String source;
    private int pos;
    private final List<Token> tokens;

    public Lexer(String source) {
        this.source = source;
        this.pos    = 0;
        this.tokens = new ArrayList<>();
    }

    public List<Token> tokenize() {
        while (!isAtEnd()) {
            skipWhitespace();
            if (isAtEnd()) break;
            scanToken();
        }
        tokens.add(Token.eof(pos));
        return List.copyOf(tokens);
    }

    private void scanToken() {
        int start = pos;
        char c = advance();

        switch (c) {
            case '=' -> emit(TokenType.EQUALS,  "=",  start);
            case '+' -> emit(TokenType.PLUS,    "+",  start);
            case '-' -> emit(TokenType.MINUS,   "-",  start);
            case '*' -> emit(TokenType.STAR,    "*",  start);
            case '/' -> emit(TokenType.SLASH,   "/",  start);
            case '(' -> emit(TokenType.LPAREN,  "(",  start);
            case ')' -> emit(TokenType.RPAREN,  ")",  start);
            case ',' -> emit(TokenType.COMMA,   ",",  start);
            case ':' -> emit(TokenType.COLON,   ":",  start);
            default  -> {
                if (Character.isDigit(c)) {
                    scanNumber(start);
                } else if (Character.isLetter(c)) {
                    scanIdentifierOrCellRef(start);
                } else {
                    emit(TokenType.ILLEGAL, String.valueOf(c), start);
                }
            }
        }
    }

    private void scanNumber(int start) {
        while (!isAtEnd() && Character.isDigit(peek())) {
            advance();
        }
        String lexeme = source.substring(start, pos);
        emit(TokenType.NUMBER, lexeme, start);
    }

    private void scanIdentifierOrCellRef(int start) {
        while (!isAtEnd() && Character.isLetter(peek())) {
            advance();
        }

        if (!isAtEnd() && Character.isDigit(peek())) {
            while (!isAtEnd() && Character.isDigit(peek())) {
                advance();
            }
            String lexeme = source.substring(start, pos);
            emit(TokenType.CELL_REFERENCE, lexeme, start);
        } else {
            String lexeme = source.substring(start, pos);
            emit(TokenType.IDENTIFIER, lexeme, start);
        }
    }

    private char advance() {
        return source.charAt(pos++);
    }

    private char peek() {
        return source.charAt(pos);
    }

    private void skipWhitespace() {
        while (!isAtEnd() && Character.isWhitespace(peek())) {
            advance();
        }
    }

    private boolean isAtEnd() {
        return pos >= source.length();
    }

    private void emit(TokenType type, String lexeme, int start) {
        tokens.add(new Token(type, lexeme, start));
    }
}
