import com.java_polytech.pipeline_interfaces.*;
import javafx.util.Pair;

import java.lang.Integer;
import java.util.ArrayList;

public class Executor implements IExecutor {

    private boolean isAlive = true;

    private IConsumer consumer;
    private Config config;
    private MyAbstractGrammar grammar = new ExecutorGrammar();

    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY};
    private TYPE currentType;

    private IProvider provider;
    private IMediator mediator;
    private byte[] mask;
    private ArrayList<Pair<Integer, byte[]>> buffer = new ArrayList<>();

    private Object lock = new Object();
    private RC error = RC.RC_SUCCESS;
    public RC GetError() {return error;}

    class ByteMediator implements IMediator {
        @Override
        public Pair<Integer, Object> getData() {
            synchronized (lock) {
                if (buffer == null || buffer.size()==0) {
                    return null;
                }
                Pair<Integer, byte[]> pair = buffer.get(buffer.size()-1);
                buffer.remove(buffer.size()-1);
                //System.out.println("Executor: give bytes to consumer");
                return new Pair<>(pair.getKey(), pair.getValue());
            }
        }
    }

    public RC setConsumer(IConsumer iConsumer) {
        if(iConsumer==null)
            return RC.RC_MANAGER_CONFIG_SEMANTIC_ERROR;
        RC rc;
        this.consumer = iConsumer;
        rc = consumer.setProvider(this);
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
        if(type == TYPE.BYTE_ARRAY)
            return new ByteMediator();

        return null;
    }

    @Override
    public boolean isAlive() {
        return isAlive;
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

    public void run() {
        Pair<Integer, Object> data;
        do {
            data = mediator.getData();

            if (data != null) {
                byte[] bytes = (byte[]) (data.getValue());
                //System.out.println("Executor: got bytes from provider");

                for (int i = 0; i < bytes.length; ++i) {
                    bytes[i] = (byte) (bytes[i] ^ mask[i % mask.length]);
                }
                synchronized (lock) {
                    buffer.add(new Pair<>(data.getKey(), bytes));
                    //System.out.println("Executor: executed bytes and put into buffer");
                }

            }

        }while(provider.isAlive() || data!=null);
        isAlive = false;

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

    private RC IntersectionTypes(TYPE[] providerTypes) {
        for (int i = 0; i < providerTypes.length; i++)
            for (int j = 0; j < supportedTypes.length; j++)
                if(supportedTypes[j] == providerTypes[i]){
                    currentType = supportedTypes[j];
                    return RC.RC_SUCCESS;
                }
        return RC.RC_WRITER_TYPES_INTERSECTION_EMPTY_ERROR;
    }

    @Override
    public RC setProvider(IProvider iProvider) {
        if(iProvider==null)
            return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
        provider = iProvider;
        RC rc = IntersectionTypes(provider.getOutputTypes());
        if(!rc.isSuccess())
            return rc;

        mediator = provider.getMediator(currentType);
        return RC.RC_SUCCESS;
    }

}
