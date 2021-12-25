import com.java_polytech.pipeline_interfaces.IMediator;
import com.java_polytech.pipeline_interfaces.IProvider;
import com.java_polytech.pipeline_interfaces.IWriter;
import com.java_polytech.pipeline_interfaces.RC;
import com.java_polytech.pipeline_interfaces.TYPE;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Writer implements IWriter {

    private OutputStream outputStream;
    private final MyAbstractGrammar grammar = new WriterGrammar();

    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY, TYPE.CHAR_ARRAY, TYPE.INT_ARRAY};
    private TYPE currentType = null;
    private Config config;
    private IProvider provider;
    private IMediator mediator;
    private byte[] buffer = null;

    public RC setOutputStream(OutputStream outputStream) {
        if(outputStream==null)
            return RC.RC_WRITER_CONFIG_SEMANTIC_ERROR;
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

    private RC IntersectionTypes(TYPE[] providerTypes) {
        for (int i = 0; i < providerTypes.length; i++)
            for (int j = 0; j < supportedTypes.length; j++)
                if(supportedTypes[j] == providerTypes[i]){
                    currentType = supportedTypes[j];
                    return RC.RC_SUCCESS;
                }
        return RC.RC_WRITER_TYPES_INTERSECTION_EMPTY_ERROR;
    }

    public RC consume() {
        buffer = GetBytesFromCurType();

        //nothing to write
        if(buffer==null)
            return RC.RC_SUCCESS;

        try {
            outputStream.write(buffer);
        } catch (IOException e) {
            return RC.RC_WRITER_FAILED_TO_WRITE;
        }

        return RC.RC_SUCCESS;
    }

    public RC setProvider(IProvider iProvider) {
        if(iProvider==null){
            return RC.RC_WRITER_CONFIG_SEMANTIC_ERROR;
        }
        RC rc;
        this.provider = iProvider;

        rc = IntersectionTypes(provider.getOutputTypes());
        if(!rc.isSuccess())
            return rc;

        mediator = provider.getMediator(currentType);

        return RC.RC_SUCCESS;
    }

    private byte[] GetBytesFromCurType() {
        Object data = mediator.getData();
        if(data == null)
            return null;

        if(currentType == TYPE.BYTE_ARRAY)
            return (byte[])data;
        else if(currentType == TYPE.CHAR_ARRAY){
            char[] chars = (char[])data;
            return new String(chars).getBytes(StandardCharsets.UTF_8);
        }
        else if(currentType == TYPE.INT_ARRAY){
            int[] arrInt = (int[])data;
            byte[] out = new byte[arrInt.length * 4];
            for (int i = 0; i < arrInt.length; i++) {
                int j = i * 2;
                out[j++] = (byte) ((arrInt[i] & 0xFF000000) >> 24);
                out[j++] = (byte) ((arrInt[i] & 0x00FF0000) >> 16);
                out[j++] = (byte) ((arrInt[i] & 0x0000FF00) >> 8);
                out[j] = (byte) ((arrInt[i] & 0x000000FF));
            }
            return out;
        }
        return null;
    }
}
