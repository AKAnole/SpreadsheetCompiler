package ast;

public abstract class ASTNode {
    public abstract String toTree(String prefix);
    @Override
    public String toString() {
        return toTree("");
    }
}
