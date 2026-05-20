package tests;

import ast.FormulaNode;
import java.util.List;
import lexer.Lexer;
import lexer.Token;
import parser.ParseException;
import parser.Parser;

public class ValidTests {

    private static final String GREEN  = "\u001B[32m";
    private static final String RED    = "\u001B[31m";
    private static final String RESET  = "\u001B[0m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";
    private static final String BOLD   = "\u001B[1m";

    public static int run() {
        System.out.println(BOLD + "\n══════════════════════════════════════════════");
        System.out.println("       VALID FORMULA TEST SUITE");
        System.out.println("══════════════════════════════════════════════" + RESET);

        String[] cases = {
            "=1",
            "=42",
            "=A1",
            "=A1+B2",
            "=A1-B2",
            "=A1*B2",
            "=A1/B2",

            "=A1+B2*C3",           
            "=A1*B2+C3",
            "=1+2+3+4",            
            "=10-3-2",

            "=(A1+B1)*2",
            "=(A1+B2)*(C3-D4)",
            "=((A1))",

            "=-1",
            "=-A1",
            "=-(A1+B1)",

            "=SUM(A1,B2)",
            "=MAX(1,2,3)",
            "=MIN(A1,B2,C3)",
            "=SUM(5)",

            "=SUM(A1+B1,C2)",
            "=MAX(A1*2,B2-1)",

            "=MAX(1,2,SUM(A1,B1))",
            "=SUM(MAX(A1,B1),MIN(C1,D1))",
            "=SUM(A1,MAX(B1,MIN(C1,D1)))",

            "=SUM(A1:A5)",
            "=MAX(B1:B10)",
            "=SUM(A1:Z99)",

            "=SUM(A1,B2,5)",
            "=A1+SUM(B1,C1)*2",
            "=SUM(A1:A5)+MAX(B1:B5)",
            "=IF(A1,B2,C3)",
            "=(SUM(A1:A10)+MAX(B1:B10))/2",

            "=AA10+BB20",
            "=ZZ99",
        };

        int passed = 0;
        int total  = cases.length;

        for (int i = 0; i < cases.length; i++) {
            System.out.printf(CYAN + "\n[TEST %02d/%02d]%s %s\n" + RESET,
                              i + 1, total, RESET, cases[i]);
            boolean ok = runTest(cases[i]);
            if (ok) passed++;
        }

        System.out.println(BOLD + "\n──────────────────────────────────────────────");
        System.out.printf("  Valid Tests: %s%d/%d passed%s\n",
                          passed == total ? GREEN : RED, passed, total, RESET);
        System.out.println("──────────────────────────────────────────────" + RESET);

        return passed;
    }

    private static boolean runTest(String source) {
        try {
            // ---- PHASE 1 — SCANNING ----
            Lexer lexer   = new Lexer(source);
            List<Token> tokens = lexer.tokenize();

            for (Token t : tokens) {
                System.out.printf("    %-18s  lexeme=%-8s  pos=%d%n",
                                  t.type, "\"" + t.lexeme + "\"", t.position);
            }

            Parser parser = new Parser(tokens);
            FormulaNode ast = parser.parseFormula();

            System.out.println(YELLOW + "  AST Tree:" + RESET);
            System.out.println(indent(ast.toTree(""), 4));

            System.out.println(GREEN + "  ✔  PASS" + RESET);
            return true;

        } catch (ParseException pe) {
            System.out.println(RED + "  ✘  FAIL — " + pe.getMessage() + RESET);
            return false;
        } catch (Exception ex) {
            System.out.println(RED + "  ✘  FAIL (unexpected) — " + ex.getMessage() + RESET);
            return false;
        }
    }

    private static String indent(String text, int spaces) {
        String pad = " ".repeat(spaces);
        return pad + text.replace("\n", "\n" + pad);
    }
}
