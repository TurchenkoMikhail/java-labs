import com.java_polytech.pipeline_interfaces.IConsumer;
import com.java_polytech.pipeline_interfaces.IExecutor;
import com.java_polytech.pipeline_interfaces.RC;

import java.util.Arrays;

public class Executor implements IExecutor {

    private IConsumer consumer;
    private Config config;
    private MyAbstractGrammar grammar = new ExecutorGrammar();

    private byte[] mask;
    private byte[] encodedBuff;

    public Executor() {}

    public RC setConsumer(IConsumer iConsumer) {
        this.consumer = iConsumer;
        return RC.RC_SUCCESS;
    }

    //semantic analysis
    public RC setConfig(String cfgFileName) {
        config = new Config(grammar);
        RC err = config.ParseConfig(cfgFileName);
        if (!err.isSuccess())
            return err;

        //check if mask is valid
        String mask = config.get(ExecutorGrammar.ExecutorTokens.MASK.toString());
        if(!CheckMask(mask)) {
            return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
        }
        this.mask = CreateMask(mask);

        return RC.RC_SUCCESS;
    }

    public RC consume(byte[] bytes) {
        if(bytes == null){
            return consumer.consume(null); //give null forward to pipeline
        }

        //collect data to private field and execute algorithm
        encodedBuff = new byte[bytes.length];
        for (int i = 0; i < bytes.length; ++i){
            encodedBuff[i] = (byte) (bytes[i] ^ mask[i % mask.length]);
        }

        byte[] buffToConsumer = Arrays.copyOf(encodedBuff, encodedBuff.length);
        return consumer.consume(buffToConsumer);
    }

    private boolean CheckMask(String mask){
        if(mask==null || mask.length() % ExecutorGrammar.GetSizeofByte() != 0) {
            return false;
        }


        for(int i=0; i<mask.length(); ++i){
            //check if all elements in mask are valid
            if(ExecutorGrammar.GetValidElementsInMask().indexOf(mask.charAt(i))==(-1))
                return false;
        }

        return true;
    }

    private byte[] CreateMask(String mask){

        final char ZERO_CODE = '0';

        int bufferSize = mask.length()/ExecutorGrammar.GetSizeofByte();
        byte[] newMask = new byte[bufferSize];
        int temp;

        for(int size=0; size<bufferSize; ++size){

            newMask[size] = 0;
            for(int i=0; i<ExecutorGrammar.GetSizeofByte(); ++i){
                //set binary 0 or 1 and shift it
                temp = (mask.charAt(size*ExecutorGrammar.GetSizeofByte()+(ExecutorGrammar.GetSizeofByte()-(i+1))) - ZERO_CODE);
                temp = (byte)(temp << i);
                newMask[size] = (byte)(newMask[size] | temp);
            }
        }

        return newMask;
    }

}
