package ast;

public class CellReferenceNode extends ASTNode {
    public final String reference;
    public final String column;
    public final int row;
    public CellReferenceNode(String lexeme) {
        this.reference = lexeme;
        int split = 0;
        while (split < lexeme.length() && Character.isLetter(lexeme.charAt(split))) {
            split++;
        }
        this.column = lexeme.substring(0, split);
        this.row    = Integer.parseInt(lexeme.substring(split));
    }
    @Override
    public String toTree(String prefix) {
        return prefix + "CellRef [" + reference + "]\n";
    }
}
