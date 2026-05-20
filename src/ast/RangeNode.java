package ast;

public class RangeNode extends ASTNode {
    public final CellReferenceNode start;
    public final CellReferenceNode end;

    public RangeNode(CellReferenceNode start, CellReferenceNode end) {
        this.start = start;
        this.end   = end;
    }

    @Override
    public String toTree(String prefix) {
        String childPrefix = deriveChildPrefix(prefix);
        return stripBranchChars(prefix)
             + "Range [" + start.reference + ":" + end.reference + "]\n"
             + start.toTree(childPrefix + "├── ")
             + end.toTree(childPrefix   + "└── ");
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
