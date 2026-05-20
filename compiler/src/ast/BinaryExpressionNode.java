package ast;

public class BinaryExpressionNode extends ASTNode {

    public final char operator;
    public final ASTNode left;
    public final ASTNode right;
    public BinaryExpressionNode(char operator, ASTNode left, ASTNode right) {
        this.operator = operator;
        this.left     = left;
        this.right    = right;
    }

    @Override
    public String toTree(String prefix) {
        String childPrefix = deriveChildPrefix(prefix);

        return prefix.replace("└── ", "").replace("├── ", "")
             + (prefix.isEmpty() ? "" : "")
             + formatSelf(prefix)
             + left.toTree(childPrefix  + "├── ")
             + right.toTree(childPrefix + "└── ");
    }

    private String formatSelf(String prefix) {
        String stripped = prefix.replaceAll("[└├│ ─]*$", "");
        return stripped + "BinaryExpr [" + operator + "]\n";
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
