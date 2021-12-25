import com.java_polytech.pipeline_interfaces.RC;
import java.util.Arrays;

public class ExecutorGrammar extends MyAbstractGrammar {

    private final static int BYTE = 8;
    private static final String ValidElementsInMask = "01";

    public static int GetSizeofByte() {return BYTE;}
    public static String GetValidElementsInMask() {return ValidElementsInMask;}

    public enum ExecutorTokens {
        MASK; //using .toString() method instead of saving String variable in enum
    }

    private final static RC RC_EXECUTOR_INCOMPLETE_CONFIG_ERROR = new RC(RC.RCWho.EXECUTOR,
            RC.RCType.CODE_CUSTOM_ERROR,
            "Not enough parameters in executors config for work.");

    ExecutorGrammar() {
        super(Arrays.stream(ExecutorTokens.values())
                        .map(Enum::toString)
                        .toArray(String[]::new),
                RC.RC_READER_CONFIG_FILE_ERROR,
                RC_EXECUTOR_INCOMPLETE_CONFIG_ERROR);
    }
}
