import com.java_polytech.pipeline_interfaces.RC;
import java.util.Arrays;

public class ManagerGrammar extends MyAbstractGrammar{
    public enum ManagerTokens {
        READER_NAME,
        EXECUTOR_NAME,
        WRITER_NAME,
        INPUT_FILE,
        OUTPUT_FILE,
        READER_CONFIG,
        WRITER_CONFIG,
        EXECUTOR_CONFIG
    }

    private final static RC RC_MANAGER_INCOMPLETE_CONFIG_ERROR = new RC(
            RC.RCWho.MANAGER,
            RC.RCType.CODE_CUSTOM_ERROR,
            "Not enough parameters in manager's config for work.");

    ManagerGrammar() {
        super(Arrays.stream(ManagerTokens.values())
                        .map(Enum::toString)
                        .toArray(String[]::new),
                RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR,
                RC.RC_MANAGER_CONFIG_FILE_ERROR,
                RC_MANAGER_INCOMPLETE_CONFIG_ERROR);
    }
}
