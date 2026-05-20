package tests;

import java.util.List;
import lexer.Lexer;
import lexer.Token;
import parser.ParseException;
import parser.Parser;

public class InvalidTests {

    private static final String GREEN  = "\u001B[32m";
    private static final String RED    = "\u001B[31m";
    private static final String RESET  = "\u001B[0m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";
    private static final String BOLD   = "\u001B[1m";

    public static int run() {
        System.out.println(BOLD + "\n══════════════════════════════════════════════");
        System.out.println("       INVALID FORMULA TEST SUITE");
        System.out.println("══════════════════════════════════════════════" + RESET);

        String[][] cases = {

            { "=",               "Formula body is empty" },

            { "=A1+",            "expected a number, cell reference, or function call" },
            { "=A1*",            "expected a number, cell reference, or function call" },
            { "=+*5",            "expected a number, cell reference, or function call" },
            { "=1+2+",           "expected a number, cell reference, or function call" },

            { "=((A1)",          "Expected ')' to close" },
            { "=(A1+B2",         "Expected ')' to close" },
            { "=A1+B2)",         "Expected end of formula but found extra tokens" },

            { "=SUM(A1,)",       "Trailing comma" },
            { "=SUM(,A1)",       "expected a number, cell reference, or function call" },
            { "=SUM A1,B2)",     "Expected '(' after function name" },
            { "=SUM(A1 B2)",     "Expected ')' to close" },

            { "=MAX(,)",         "expected a number, cell reference, or function call" },

            { "A1+B2",           "Expected '=' at the start of a formula" },
            { "SUM(A1)",         "Expected '=' at the start of a formula" },
            { "42",              "Expected '=' at the start of a formula" },

            { "=A1@B2",          "Illegal character" },
            { "=A1#5",           "Illegal character" },

            { "=SUM(A5:A1)",     "Malformed range" },

            { "=1 2",            "Expected end of formula" },
            { "=A1 A2",          "Expected end of formula" },

            { "=SUM(MAX(A1,B1)", "Expected ')' to close" },
        };

        int passed = 0;
        int total  = cases.length;

        for (int i = 0; i < cases.length; i++) {
            String input    = cases[i][0];
            String expected = cases[i][1];
            System.out.printf(CYAN + "\n[TEST %02d/%02d]%s Input: %-28s  expecting error containing: \"%s\"\n" + RESET,
                              i + 1, total, RESET, "\"" + input + "\"", expected);
            boolean ok = runTest(input, expected);
            if (ok) passed++;
        }

        System.out.println(BOLD + "\n──────────────────────────────────────────────");
        System.out.printf("  Invalid Tests: %s%d/%d correctly rejected%s\n",
                          passed == total ? GREEN : RED, passed, total, RESET);
        System.out.println("──────────────────────────────────────────────" + RESET);

        return passed;
    }

    private static boolean runTest(String source, String expectedFragment) {
        try {
            Lexer       lexer  = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            Parser      parser = new Parser(tokens);
            parser.parseFormula();
            System.out.println(RED + "  ✘  FAIL — parser accepted an invalid formula!" + RESET);
            return false;

        } catch (ParseException pe) {
            String msg = pe.getMessage();
            System.out.println(YELLOW + "  Error: " + msg + RESET);

            if (msg.contains(expectedFragment)) {
                System.out.println(GREEN + "  ✔  PASS (error correctly detected)" + RESET);
                return true;
            } else {
                System.out.println(RED + "  ✘  FAIL — error message did not contain: \""
                                   + expectedFragment + "\"" + RESET);
                return false;
            }
        } catch (Exception ex) {
            System.out.println(RED + "  ✘  FAIL (unexpected exception) — " + ex + RESET);
            return false;
        }
    }
}
