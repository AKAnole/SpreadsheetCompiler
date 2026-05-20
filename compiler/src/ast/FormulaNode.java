package ast;

public class FormulaNode extends ASTNode {
    public final ASTNode expression;
    public FormulaNode(ASTNode expression) {
        this.expression = expression;
    }
    @Override
    public String toTree(String prefix) {
        return "Formula\n" + expression.toTree(prefix + "└── ");
    }

}
