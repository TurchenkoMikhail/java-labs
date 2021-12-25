import com.java_polytech.pipeline_interfaces.*;

public class Executor implements IExecutor {

    private IConsumer consumer;
    private Config config;
    private MyAbstractGrammar grammar = new ExecutorGrammar();

    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY};
    private TYPE currentType;

    private IProvider provider;
    private IMediator mediator;
    private byte[] mask;
    private byte[] buffer;

    class ByteMediator implements IMediator {
        @Override
        public Object getData() {
            if (buffer == null) {
                return null;
            }
            byte[] data = new byte[buffer.length];
            System.arraycopy(buffer, 0, data, 0, buffer.length);
            return data;
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

    public RC consume() {
        RC rc;
        byte[] bytes = (byte[])mediator.getData();
        if(bytes!=null) {
            //collect data to private field and execute algorithm
            buffer = new byte[bytes.length];
            for (int i = 0; i < bytes.length; ++i) {
                buffer[i] = (byte) (bytes[i] ^ mask[i % mask.length]);
            }
        }else
            buffer = null;
        rc = consumer.consume();
        if(!rc.isSuccess())
            return rc;
        return RC.RC_SUCCESS;
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
