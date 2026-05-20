package tests;

public class TestRunner {

    public static void main(String[] args) {
        int validPassed   = ValidTests.run();
        int invalidPassed = InvalidTests.run();

        System.out.println("\n\u001B[1m╔══════════════════════════════════════════════╗");
        System.out.println("║          OVERALL TEST RESULTS               ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.printf ("║  Valid   formulas passed : %-3d                ║%n", validPassed);
        System.out.printf ("║  Invalid formulas caught : %-3d                ║%n", invalidPassed);
        System.out.println("╚══════════════════════════════════════════════╝\u001B[0m");
    }
}
