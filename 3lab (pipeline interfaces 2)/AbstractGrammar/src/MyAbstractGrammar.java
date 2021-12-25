import com.java_polytech.pipeline_interfaces.RC;

public class MyAbstractGrammar {

    private final static String DELIMITER = "=";
    public static String GetDelimiter() {return DELIMITER;}

    private final static String COMMENT_SYMBOLS = "//";
    public static String GetCommentSymbols() {return COMMENT_SYMBOLS;}

    private final static String SEPARATOR_TOKEN = ",";
    public static String GetSeparatorToken(){return SEPARATOR_TOKEN;}

    private final String[] tokens;

    private final RC noFileError;
    private final RC invalidConfigError; // turn in into RC custom error

    protected MyAbstractGrammar(String[] tokens,
                              RC noFileError, RC incompleteConfigError) {
        this.tokens = tokens;
        this.noFileError = noFileError;
        this.invalidConfigError = incompleteConfigError;
    }

    public boolean isValidToken(String str) {

        for (String token : tokens) {
            if (token.equals(str)) {
                return true;
            }
        }
        return false;
    }

    public final int getNumTokens() {
        if(this.tokens==null)
            return 0;
        else
            return this.tokens.length;
    }

    public RC getNoFileErrorCode() { return noFileError;}

    public RC getIncompleteConfigErrorCode(){return invalidConfigError;}

}
