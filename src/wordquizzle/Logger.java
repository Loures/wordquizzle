package wordquizzle;

public class Logger {
    public static final String ANSI_RESET  = "\u001B[0m";
    public static final String ANSI_RED    = "\u001B[31m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_GREEN  = "\u001B[32m";

    public static void logErr(Object obj) {
        StringBuilder builder = new StringBuilder(ANSI_RED).append("[ERROR] ").append(ANSI_RESET).append(obj.toString());
        System.err.println(builder);
    }

    public static void logWarn(Object obj) {
        StringBuilder builder = new StringBuilder(ANSI_YELLOW).append("[WARN] ").append(ANSI_RESET).append(obj.toString());
        System.err.println(builder);
    }

    public static void logInfo(Object obj) {
        StringBuilder builder = new StringBuilder(ANSI_GREEN).append("[INFO] ").append(ANSI_RESET).append(obj.toString());
        System.out.println(builder);
    }
}