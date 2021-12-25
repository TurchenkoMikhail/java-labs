import com.java_polytech.pipeline_interfaces.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

public class Reader implements IReader{

    private InputStream inputStream;
    private IConsumer consumer;
    private Config config;
    private final MyAbstractGrammar grammar = new ReaderGrammar();

    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY, TYPE.CHAR_ARRAY, TYPE.INT_ARRAY};

    private int buffer_size;
    private int size = 0;
    private byte[] buffer = null;

    //inner classes
    class ByteMediator implements IMediator {

        public Object getData() {
            if (buffer == null) {
                return null;
            }
            byte[] data = new byte[size];
            System.arraycopy(buffer, 0, data, 0, size);
            return data;
        }
    }

    class CharMediator implements IMediator {
        @Override
        public Object getData() {
            if (buffer == null) {
                return null;
            }
            return new String(buffer, StandardCharsets.UTF_8).toCharArray();
        }
    }

    class IntMediator implements IMediator {
        @Override
        public Object getData() {
            if (buffer == null) {
                return null;
            }
            IntBuffer intBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
            int[] arr = new int[intBuffer.remaining()];
            intBuffer.get(arr);
            return arr;
        }
    }

    public RC setInputStream(InputStream inputStream) {
        if (inputStream == null)
            return RC.RC_READER_FAILED_TO_READ;
        this.inputStream = inputStream;
        return RC.RC_SUCCESS;
    }

    public RC setConsumer(IConsumer iConsumer) {
        if (iConsumer == null)
            return RC.RC_READER_CONFIG_SEMANTIC_ERROR;
        RC rc;
        this.consumer = iConsumer;
        rc = this.consumer.setProvider(this);
        if(!rc.isSuccess())
            return rc;

        return RC.RC_SUCCESS;
    }

    @Override
    public TYPE[] getOutputTypes() {
        return supportedTypes;
    }

    @Override
    public IMediator getMediator(TYPE type) {
        switch (type){
            case BYTE_ARRAY:
                return new ByteMediator();
            case CHAR_ARRAY:
                return new CharMediator();
            case INT_ARRAY:
                return new IntMediator();
            default:
                return null;
        }
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
                this.buffer_size = bufferSize;
        } catch (NumberFormatException e){
            return RC.RC_READER_CONFIG_SEMANTIC_ERROR; //not a number
        }
        return RC.RC_SUCCESS;
    }

    public RC run() {
        RC rc;
        byte[] tmpBytes = new byte[buffer_size];
        try {
            while((size = inputStream.read(tmpBytes, 0, buffer_size))!=(-1)){

                //at the end of file there can be fewer bytes then length of buffer
                size = Math.min(size, tmpBytes.length);

                buffer = new byte[size];
                System.arraycopy(tmpBytes, 0, buffer, 0, size);

                //copy data to consumer
                rc = consumer.consume();
                if(!rc.isSuccess())
                    return rc;
            }

            if(size<=0){
                buffer = null;
            }

            //give null to next consumer
           rc = consumer.consume();
            if(!rc.isSuccess())
                return rc;

        } catch (IOException e) {
            return RC.RC_READER_FAILED_TO_READ;
        }

        //manager will close input file
        return RC.RC_SUCCESS;
    }

}