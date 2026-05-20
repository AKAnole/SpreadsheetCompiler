package ast;
import java.util.List;
public class FunctionCallNode extends ASTNode {
    public final String name;
    public final List<ASTNode> arguments;
    public FunctionCallNode(String name, List<ASTNode> arguments) {
        this.name      = name.toUpperCase();
        this.arguments = List.copyOf(arguments);
    }

    @Override
    public String toTree(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(stripBranchChars(prefix))
          .append("FunctionCall [").append(name).append("]\n");

        String childPrefix = deriveChildPrefix(prefix);
        for (int i = 0; i < arguments.size(); i++) {
            boolean last   = (i == arguments.size() - 1);
            String  branch = last ? "└── " : "├── ";
            sb.append(arguments.get(i).toTree(childPrefix + branch));
        }
        return sb.toString();
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
