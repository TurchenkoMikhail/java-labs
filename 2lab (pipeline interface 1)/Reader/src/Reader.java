import com.java_polytech.pipeline_interfaces.IConsumer;
import com.java_polytech.pipeline_interfaces.IReader;
import com.java_polytech.pipeline_interfaces.RC;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class Reader implements IReader{

    private InputStream inputStream;
    private IConsumer consumer;
    private Config config;
    private final MyAbstractGrammar grammar = new ReaderGrammar();

    private byte[] buffer = null;

    public RC setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        return RC.RC_SUCCESS;
    }

    public RC setConsumer(IConsumer iConsumer) {
        this.consumer = iConsumer;
        return RC.RC_SUCCESS;
    }

    //check semantic errors
    public RC setConfig(String cfgFileName) {

        config = new Config(grammar);
        RC rc = config.ParseConfig(cfgFileName);
        if(!rc.isSuccess())
            return rc;

        //semantic analysis
        String buffsize = config.get(ReaderGrammar.ReaderTokens.BUFFER_SIZE.toString());

        try {
            int bufferSize = Integer.parseInt(buffsize);
            //if negative number or very big
            if(bufferSize < 1 || bufferSize > 1000000)
                return RC.RC_READER_CONFIG_SEMANTIC_ERROR; //not positive number
            else
                buffer = new byte[bufferSize];
        } catch (NumberFormatException e){
            return RC.RC_READER_CONFIG_SEMANTIC_ERROR; //not a number
        }
        return RC.RC_SUCCESS;
    }

    public RC run() {
        RC rc;
        int size;
        try {
            while((size = inputStream.read(buffer, 0, buffer.length))!=(-1)){

                //at the end of file there can be fewer bytes then length of buffer
                size = Math.min(size, buffer.length);

                //copy data to consumer
                byte[] dataToConsumer = Arrays.copyOf(buffer, size);
                rc = consumer.consume(dataToConsumer);

                if(!rc.isSuccess())
                    return rc;
            }

        } catch (IOException e) {
            return RC.RC_READER_FAILED_TO_READ;
        }
        rc = consumer.consume(null);
        if(!rc.isSuccess())
            return rc;

        //manager will close input file
        return RC.RC_SUCCESS;
    }

}