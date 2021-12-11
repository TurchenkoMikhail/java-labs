import com.java_polytech.pipeline_interfaces.RC;
import java.util.Arrays;

public class WriterGrammar extends MyAbstractGrammar{

    //no grammar tokens
    public enum ExecutorTokens {}

    WriterGrammar() {
        super(Arrays.stream(ExecutorTokens.values())
                        .map(Enum::toString)
                        .toArray(String[]::new),
                RC.RC_READER_CONFIG_GRAMMAR_ERROR,
                RC.RC_READER_CONFIG_FILE_ERROR,
                null);
    }
}
