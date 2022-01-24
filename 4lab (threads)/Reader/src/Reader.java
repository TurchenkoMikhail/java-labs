import com.java_polytech.pipeline_interfaces.*;
import javafx.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;

public class Reader implements IReader{

    private boolean isAlive = true;

    private InputStream inputStream;
    private IConsumer consumer;
    private Config config;
    private final MyAbstractGrammar grammar = new ReaderGrammar();

    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY, TYPE.CHAR_ARRAY, TYPE.INT_ARRAY};

    private int buffer_size;
    private int size = 0;

    private Object lock = new Object();
    //Integer means number of block
    private ArrayList<Pair<Integer, byte[]>> buffer = new ArrayList<>();

    //now run() method returns void, so remember error code
    private RC error = RC.RC_SUCCESS;
    public RC GetError(){return error;}

    //inner classes
    class ByteMediator implements IMediator {
        public Pair<Integer, Object> getData() {

            synchronized (lock) {
                if (buffer == null || buffer.size()==0) {
                    return null;
                }
                Pair<Integer, byte[]> pair = buffer.get(buffer.size()-1);
                buffer.remove(buffer.size()-1);
                //System.out.println("Reader: gave new data to consumer");
                return new Pair<>(pair.getKey(), pair.getValue());
            }
        }
    }

    class CharMediator implements IMediator {
        @Override
        public Pair<Integer, Object> getData() {

            synchronized (lock) {
                if (buffer == null || buffer.size()==0) {
                    return null;
                }
                Pair<Integer, byte[]> pair = buffer.get(buffer.size()-1);
                buffer.remove(buffer.size()-1);
                return new Pair<>(pair.getKey(), new String(pair.getValue(), StandardCharsets.UTF_8).toCharArray());
            }
        }
    }

    class IntMediator implements IMediator {
        @Override
        public Pair<Integer, Object> getData() {

            synchronized (lock) {
                if (buffer == null || buffer.size()==0) {
                    return null;
                }
                Pair<Integer, byte[]> pair = buffer.get(buffer.size()-1);
                buffer.remove(buffer.size()-1);
                IntBuffer intBuffer = ByteBuffer.wrap(pair.getValue()).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
                int[] arr = new int[intBuffer.remaining()];
                intBuffer.get(arr);
                return new Pair<>(pair.getKey(), arr);
            }
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

    public boolean isAlive() {
        return isAlive;
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

    public void run() {

        int blocksNum = 0;

        byte[] tmpBytes = new byte[buffer_size];
        try {

            //read to tmpBytes
            while((size = inputStream.read(tmpBytes, 0, buffer_size))!=(-1)){
                //at the end of file there can be fewer bytes then length of buffer
                size = Math.min(size, tmpBytes.length);
                //System.out.println("Reader: read bytes");

                synchronized (lock) {
                    byte[] bytes = new byte[size];
                    System.arraycopy(tmpBytes, 0, bytes, 0, size);
                    buffer.add(new Pair<>(blocksNum, bytes));
                    //System.out.println("Reader: added bytes to buffer");
                    ++blocksNum;
                }

            }

            if(size<=0){
                isAlive = false;
            }

        } catch (IOException e) {
            error = RC.RC_READER_FAILED_TO_READ;
            isAlive = false;
        }

    }

}