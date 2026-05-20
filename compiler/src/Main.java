import ast.FormulaNode;
import java.util.List;
import java.util.Scanner;
import lexer.Lexer;
import lexer.Token;
import parser.ParseException;
import parser.Parser;
import tests.TestRunner;

public class Main {

    private static final String BANNER =
        "\u001B[1m\u001B[36m" +
        "╔═══════════════════════════════════════════════════════╗\n" +
        "║   Spreadsheet Formula Language — Compiler Front-End  ║\n" +
        "║   Scanner · Parser · AST · Error Reporting           ║\n" +
        "╚═══════════════════════════════════════════════════════╝" +
        "\u001B[0m";

    public static void main(String[] args) {
        System.out.println(BANNER);

        if (args.length == 0) {
            TestRunner.main(new String[]{});
            System.out.println();
            repl();
        } else {
            switch (args[0]) {
                case "--demo" -> demo();
                case "--repl" -> repl();
                case "--test" -> TestRunner.main(new String[]{});
                default       -> compileSingle(args[0]);
            }
        }
    }

    private static void demo() {
        System.out.println("\n\u001B[1m\u001B[35m════════════════════  DEMO  ════════════════════\u001B[0m");

        header("Demo 1 — Simple Valid Formula");
        compile("=A1 + B2");

        header("Demo 2 — Complex Valid Formula");
        compile("=(SUM(A1:A10) + MAX(B1:B10)) / 2");

        header("Demo 3 — Invalid Formula (error reporting)");
        compile("=SUM(A1,)");
    }

    public static void compile(String source) {
        System.out.println("\u001B[33mInput  :\u001B[0m " + source);

        System.out.println("\u001B[33mTokens :\u001B[0m");
        Lexer       lexer  = new Lexer(source);
        List<Token> tokens = lexer.tokenize();
        for (Token t : tokens) {
            System.out.printf("  %-18s  \"%s\"  @pos %d%n",
                              t.type, t.lexeme, t.position);
        }

        try {
            Parser      parser = new Parser(tokens);
            FormulaNode ast    = parser.parseFormula();
            System.out.println("\u001B[33mAST    :\u001B[0m");
            System.out.println(ast.toTree(""));
            System.out.println("\u001B[32m✔ Compiled successfully.\u001B[0m");
        } catch (ParseException pe) {
            System.out.println("\u001B[31m✘ " + pe.getMessage() + "\u001B[0m");
        }
        System.out.println();
    }

    private static void repl() {
        System.out.println("\u001B[1m\nInteractive REPL — type a formula and press Enter.");
        System.out.println("Commands: :quit / :q  →  exit    :json  →  toggle JSON output");
        System.out.println("Example:  =SUM(A1:A5)+MAX(B1:B5)\u001B[0m\n");

        Scanner input  = new Scanner(System.in);
        while (true) {
            System.out.print("\u001B[36m> \u001B[0m");
            if (!input.hasNextLine()) break;
            String line = input.nextLine().trim();

            if (line.isEmpty())       continue;
            if (line.equals(":quit") || line.equals(":q")) {
                System.out.println("Bye!");
                break;
            }
            replCompile(line); 
        }
        input.close();
    }

    private static void replCompile(String source) {
        Lexer       lexer  = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        System.out.println("\u001B[33mTokens:\u001B[0m " + tokens);

        try {
            Parser      parser = new Parser(tokens);
            FormulaNode ast    = parser.parseFormula();

            System.out.println("\u001B[33mAST:\u001B[0m\n" + ast.toTree(""));
            System.out.println("\u001B[32m✔\u001B[0m");

        } catch (ParseException pe) {
            System.out.println("\u001B[31m✘ " + pe.getMessage() + "\u001B[0m");
        }
    }


    private static void compileSingle(String formula) {
        compile(formula);
    }

    private static void header(String title) {
        System.out.println("\n\u001B[1m── " + title + " ──\u001B[0m");
    }
}
