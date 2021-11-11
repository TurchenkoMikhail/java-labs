import java.io.*;

final class Config {
    final private String inputFile;
    final private String outputFile;
    final private int bufferSize;
    final private String mask;

    private enum tokensName {
        INPUT_FILE(0), OUTPUT_FILE(1), BUFFER_SIZE(2), MASK(3);
        final private int token;
        static final private String[] str = {"INPUT_FILE", "OUTPUT_FILE", "BUFFER_SIZE", "MASK"};
        tokensName(int i) { token = i;}
        public int GetValue() {return token;}
    }

    static final private String delimiter = "=", slash = "/";
    static final private int numOfTokens = 4;

    public Config(String _inputFile, String _outputFile, int _bufferSize, String _mask){
    inputFile = _inputFile;
    outputFile = _outputFile;
    bufferSize = _bufferSize;
    mask = _mask;
}

    public String getInputFile() {return inputFile;}
    public String getOutputFile() {return outputFile;}
    public int getBufferSize() {return bufferSize;}
    public String getMask() {return mask;}

    public static Config SyntaxAnalysisConfig(String configName){
        try {
            FileReader reader = new FileReader(configName);
            BufferedReader buffReader = new BufferedReader(reader);
            String str, inputFile=null, outputFile=null, mask = null;
            int bufferSize=0, index;

            //syntax analysis
            while((str = buffReader.readLine())!=null){
                str = str.replace("\t", ""); //delete tabs
                str = str.replace(" ", ""); //delete spaces
                for (int i = 0; i < numOfTokens; ++i) {

                    if (str.startsWith(tokensName.str[i])){
                        //find delimiter
                        if (str.contains(delimiter)) {

                            index = str.indexOf(delimiter) + 1;
                            int start = index; //starting index

                            //going through text until end of string
                            while (index < str.length() && str.charAt(index) != slash.charAt(0))
                                ++index;
                            int end = index; //ending index
                            char[] dst = new char[end - start];

                            str.getChars(start, end, dst, 0);

                            if (tokensName.str[i].equals(tokensName.str[tokensName.INPUT_FILE.GetValue()]))
                                inputFile = new String(dst);
                            else if (tokensName.str[i].equals(tokensName.str[tokensName.OUTPUT_FILE.GetValue()]))
                                outputFile = new String(dst);
                            else if (tokensName.str[i].equals(tokensName.str[tokensName.BUFFER_SIZE.GetValue()])) {
                                String num = new String(dst);

                                try {
                                    bufferSize = Integer.parseInt(num);
                                } catch (NumberFormatException e) {
                                    System.out.println("Syntax error in size of buffer!\n");
                                    return null;
                                }
                            } else if (tokensName.str[i].equals(tokensName.str[tokensName.MASK.GetValue()]))
                                mask = new String(dst);
                        }

                    }
                }
            }

            if(inputFile!=null && outputFile!=null && mask!=null)
              return new Config(inputFile, outputFile, bufferSize, mask);
            else{

                if(inputFile==null || outputFile==null)
                    System.out.println("Syntax error in parsing files!\n");
                else
                    System.out.println("Mask does not exist!\n");
                return null;
            }

        } catch (FileNotFoundException e) {
            System.out.println("Incorrect path to config file!\n");
        }catch (IOException e) {
            System.out.println("Unexpected error during reading the file...\n");
        }catch(StringIndexOutOfBoundsException e){
            System.out.println("Syntax error during reading the file!\n");
        }

        return null;
    }
}

class Encoder{

    static final int ZERO = 0, MILLION = 1000000, BYTE = 8;
    static final char cZERO = '0', cONE = '1';
    static final int ZERO_CODE = '0'; //code of '0' symbol

    FileInputStream input;
    FileOutputStream output;
    public int bufferSize;
    byte[] mask;
    byte[] encodedBuffer;

    private static boolean CheckMask(String mask){
        if(mask.length() % BYTE != 0)
            return false;
        for(int i=0; i<mask.length(); ++i){
            if (mask.charAt(i)!=cZERO && mask.charAt(i)!=cONE)
                return false;
        }
        return true;
    }

    private static byte[] CreateMask(String mask){
        int bufferSize = mask.length()/BYTE;
        byte[] newMask = new byte[bufferSize];
        int temp;

        for(int size=0; size<bufferSize; ++size){

            newMask[size] = 0;
            for(int i=0; i<BYTE; ++i){
                //set binary 0 or 1 and shift it
                temp = (mask.charAt(size*BYTE+(BYTE-(i+1))) - ZERO_CODE);
                temp = (byte)(temp << i);
                newMask[size] = (byte)(newMask[size] | temp);
            }
        }

        return newMask;
    }

    public static Encoder SemanticAnalysisConfig(Config config){
        try {
            FileInputStream _input = new FileInputStream(config.getInputFile());
            FileOutputStream _output = new FileOutputStream(config.getOutputFile());
            int bufferSize = config.getBufferSize();
            String mask = config.getMask();
            boolean checkMask = CheckMask(mask);

            if(checkMask && bufferSize > ZERO && bufferSize < MILLION){
                byte[] _mask = CreateMask(mask);
                return new Encoder(_input, _output, bufferSize, _mask);
            }

            //errors
            else{
                if(bufferSize <= ZERO || bufferSize >= MILLION)
                    System.out.println("BufferSize is invalid!\n");
                else //if(!checkMask)
                    System.out.println("Mask is invalid!\n");
            }

        } catch (FileNotFoundException e) {
            System.out.println("Cannot open such file!");
        }

        return null;
    }

    public Encoder(FileInputStream _input, FileOutputStream _output, int _bufferSize, byte[] _mask) {
        input = _input;
        output = _output;
        bufferSize = _bufferSize;
        mask = _mask;

        //creating mask for encoding and decoding
        encodedBuffer = new byte[bufferSize];

    } //Encoder (Config config)

    public void Encode() {

        byte[] str = new byte[bufferSize];
        int size;

        try {
            while((size = input.read(str, 0, bufferSize))!=(-1)){
                size = Math.min(size, bufferSize);
                for (int i = 0; i < size; i++) {
                    encodedBuffer[i] = (byte) (str[i] ^ mask[i % mask.length]);
                }
                output.write(encodedBuffer, 0, size);
            }

        } catch (IOException e) {
            System.out.println("Unexpected error during reading the file...\n");
        }

    } //void Encode()
}

public class First {

    static Config config;
    static Encoder encoder;

    public static void main(String[] args){

        if(args.length == 1){
            config = config.SyntaxAnalysisConfig(args[0]);

            if(config != null){
                encoder = encoder.SemanticAnalysisConfig(config);

                if(encoder != null) {
                    encoder.Encode();
                    System.out.println("\nSuccess!\n");
                }
            }
        }
        else
            System.out.println("Incorrect number of parameters in command line!\n");

    }

}