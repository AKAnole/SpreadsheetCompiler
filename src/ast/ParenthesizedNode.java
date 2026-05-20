package ast;
public class ParenthesizedNode extends ASTNode {

    public final ASTNode inner;
    public ParenthesizedNode(ASTNode inner) {
        this.inner = inner;
    }

    @Override
    public String toTree(String prefix) {
        String childPrefix = deriveChildPrefix(prefix);
        return stripBranchChars(prefix) + "Parenthesized\n"
             + inner.toTree(childPrefix + "└── ");
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
