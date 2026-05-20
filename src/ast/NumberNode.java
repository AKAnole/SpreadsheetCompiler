package ast;

public class NumberNode extends ASTNode {
    public final long value;
    public NumberNode(String lexeme) {
        this.value = Long.parseLong(lexeme);
    }

    @Override
    public String toTree(String prefix) {
        return prefix + "Number [" + value + "]\n";
    }
}
