import com.java_polytech.pipeline_interfaces.IWriter;
import com.java_polytech.pipeline_interfaces.RC;

import java.io.IOException;
import java.io.OutputStream;
public class Writer implements IWriter {

    private OutputStream outputStream;
    private final MyAbstractGrammar grammar = new WriterGrammar();
    private Config config;

    public RC setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
        return RC.RC_SUCCESS;
    }


    public RC setConfig(String s) {
        config = new Config(grammar);
        RC rc = config.ParseConfig(s);
        if(!rc.isSuccess())
            return rc;
        return RC.RC_SUCCESS;
    }


    public RC consume(byte[] data) {
        //no consumer, end program. Manager will close stream
        if(data==null){
            return RC.RC_SUCCESS;
        }

        try {
            outputStream.write(data);
        } catch (IOException e) {
            return RC.RC_WRITER_FAILED_TO_WRITE;
        }

        return RC.RC_SUCCESS;
    }


}
