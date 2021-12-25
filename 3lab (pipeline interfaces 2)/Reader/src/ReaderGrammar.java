import com.java_polytech.pipeline_interfaces.RC;

import java.util.Arrays;

public class ReaderGrammar extends MyAbstractGrammar {

    public enum ReaderTokens {
        BUFFER_SIZE //using .toString() method instead of saving String variable in enum
    }

    private final static RC RC_READER_INCOMPLETE_CONFIG_ERROR = new RC(RC.RCWho.READER,
            RC.RCType.CODE_CUSTOM_ERROR,
            "Not enough parameters in readers config for work.");

    ReaderGrammar() {
        super(Arrays.stream(ReaderTokens.values())
                        .map(Enum::toString)
                        .toArray(String[]::new),
                RC.RC_READER_CONFIG_FILE_ERROR,
                RC_READER_INCOMPLETE_CONFIG_ERROR);
    }
}
