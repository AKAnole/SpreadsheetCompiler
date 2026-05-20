package parser;

import ast.*;
import java.util.ArrayList;
import java.util.List;
import lexer.Token;
import lexer.TokenType;

public class Parser {
    private final List<Token> tokens;
    private int cursor;
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.cursor = 0;
    }

    public FormulaNode parseFormula() {
        consume(TokenType.EQUALS, "Expected '=' at the start of a formula");

        if (check(TokenType.EOF)) {
            throw new ParseException(
                "Formula body is empty — expected an expression after '='",
                current()
            );
        }

        ASTNode body = parseExpression();

        consume(TokenType.EOF, "Expected end of formula but found extra tokens");

        return new FormulaNode(body);
    }

    private ASTNode parseExpression() {
        ASTNode node = parseTerm();

        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            Token op     = advance();       
            ASTNode right = parseTerm();
            node = new BinaryExpressionNode(op.lexeme.charAt(0), node, right);
        }
        return node;
    }
    private ASTNode parseTerm() {
        ASTNode node = parseFactor();

        while (check(TokenType.STAR) || check(TokenType.SLASH)) {
            Token op      = advance();        
            ASTNode right = parseFactor();
            node = new BinaryExpressionNode(op.lexeme.charAt(0), node, right);
        }
        return node;
    }

    private ASTNode parseFactor() {

        if (check(TokenType.MINUS)) {
            Token op = advance();              
            ASTNode operand = parseFactor();     
            return new UnaryExpressionNode('-', operand);
        }

        if (check(TokenType.LPAREN)) {
            advance();                          
            ASTNode inner = parseExpression();
            consume(TokenType.RPAREN, "Expected ')' to close the parenthesised expression");
            return new ParenthesizedNode(inner);
        }

        if (check(TokenType.CELL_REFERENCE)) {
            Token cellToken = advance();         
            CellReferenceNode cellNode = new CellReferenceNode(cellToken.lexeme);

            if (check(TokenType.COLON)) {
                advance();                       
                Token endToken = consume(TokenType.CELL_REFERENCE,
                    "Expected a cell reference after ':' in range expression");
                CellReferenceNode endNode = new CellReferenceNode(endToken.lexeme);

                validateRange(cellNode, endNode, cellToken);
                return new RangeNode(cellNode, endNode);
            }
            return cellNode;
        }

        if (check(TokenType.NUMBER)) {
            Token numToken = advance();
            return new NumberNode(numToken.lexeme);
        }

        if (check(TokenType.IDENTIFIER)) {
            Token nameToken = advance();         // consume function name
            consume(TokenType.LPAREN,
                "Expected '(' after function name '" + nameToken.lexeme + "'");

            List<ASTNode> args = new ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                args = parseArgumentList();
            }
            consume(TokenType.RPAREN,
                "Expected ')' to close the argument list of '" + nameToken.lexeme + "'");

            return new FunctionCallNode(nameToken.lexeme, args);
        }

        if (check(TokenType.ILLEGAL)) {
            Token illegalTok = current();
            throw new ParseException(
                "Illegal character '" + illegalTok.lexeme + "' encountered",
                illegalTok
            );
        }

        Token unexpected = current();
        throw new ParseException(
            "Unexpected token — expected a number, cell reference, or function call",
            unexpected
        );
    }

    private List<ASTNode> parseArgumentList() {
        List<ASTNode> args = new ArrayList<>();
        args.add(parseExpression());

        while (check(TokenType.COMMA)) {
            Token comma = advance();          
            if (check(TokenType.RPAREN)) {
                throw new ParseException(
                    "Trailing comma in argument list — expected an expression after ','",
                    current()
                );
            }
            args.add(parseExpression());
        }
        return args;
    }

    private Token current() {
        return tokens.get(cursor);
    }

    private Token advance() {
        Token t = tokens.get(cursor);
        if (t.type != TokenType.EOF) {
            cursor++;
        }
        return t;
    }

    private boolean check(TokenType type) {
        return current().type == type;
    }

    private Token consume(TokenType expected, String errorMsg) {
        if (check(expected)) {
            return advance();
        }
        throw new ParseException(errorMsg, current());
    }

    private void validateRange(CellReferenceNode start,
                               CellReferenceNode end,
                               Token startTok) {
        if (start.column.equals(end.column) && start.row > end.row) {
            throw new ParseException(
                "Malformed range: start row " + start.row
                    + " is greater than end row " + end.row
                    + " in range " + start.reference + ":" + end.reference,
                startTok
            );
        }
    }

}
