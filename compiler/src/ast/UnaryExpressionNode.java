package ast;

public class UnaryExpressionNode extends ASTNode {

    public final char operator;
    public final ASTNode operand;

    public UnaryExpressionNode(char operator, ASTNode operand) {
        this.operator = operator;
        this.operand  = operand;
    }

    @Override
    public String toTree(String prefix) {
        String childPrefix = deriveChildPrefix(prefix);
        return stripBranchChars(prefix) + "UnaryExpr [" + operator + "]\n"
             + operand.toTree(childPrefix + "└── ");
    }

    private String stripBranchChars(String prefix) {
        return prefix.replaceAll("[└├│ ─]*$", "");
    }

    private String deriveChildPrefix(String prefix) {
        if (prefix.endsWith("└── ")) {
            return prefix.substring(0, prefix.length() - 4) + "    ";
        } else if (prefix.endsWith("├── ")) {
            return prefix.substring(0, prefix.length() - 4) + "│   ";
        }
        return prefix;
    }
}
