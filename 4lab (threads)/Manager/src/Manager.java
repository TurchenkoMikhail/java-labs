import com.java_polytech.pipeline_interfaces.*;
import java.io.*;

import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

public class Manager implements IConfigurable {

    private static Logger logger;
    private final static String loggerFile = "log.txt";

    private OutputStream outputStream;
    private InputStream inputStream;
    private String readerConfigFile;
    private String[] executorConfigFiles;
    private String writerConfigFile;

    private String readerName;
    private String[] executorNames;
    private String writerName;

    private IReader reader;
    private IExecutor[] executors;
    private IWriter writer;

    private Config config;

    private MyAbstractGrammar grammar = new ManagerGrammar();

    RC RC_CANT_FIND_READER_CLASS = new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Can't find reader class");
    RC RC_CANT_FIND_WRITER_CLASS = new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Can't find writer class");
    RC RC_CANT_FIND_EXECUTOR_CLASS = new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Can't find executor class");
    RC RC_CANT_CLOSE_STREAMS = new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Failed to close streams");
    RC RC_MANAGER_THREAD_ERROR = new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Error while building threads");

    static RC RC_CANT_BUILD_LOGGER = new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Error while building logger");

    public RC setConfig(String s) {
        config = new Config(grammar);
        RC rc = config.ParseConfig(s);
        if(!rc.isSuccess())
            return rc;

        //set config to fields of manager
        for(ManagerGrammar.ManagerTokens token: ManagerGrammar.ManagerTokens.values()){

            String stringToken = token.toString();

            switch(token){
                case INPUT_FILE:
                    try {
                        inputStream = new FileInputStream(config.get(stringToken));
                    } catch (FileNotFoundException e) {
                        return RC.RC_MANAGER_INVALID_INPUT_FILE;
                    }
                    break;
                case OUTPUT_FILE:
                    try {
                        outputStream = new FileOutputStream(config.get(stringToken));
                    } catch (FileNotFoundException e) {
                        return RC.RC_MANAGER_INVALID_OUTPUT_FILE;
                    }
                    break;
                case READER_NAME:
                    readerName = config.get(stringToken);
                    break;
                case WRITER_NAME:
                    writerName = config.get(stringToken);
                    break;
                case EXECUTOR_NAME:
                    executorNames = config.get(stringToken).split(grammar.GetSeparatorToken());
                    break;
                case READER_CONFIG:
                    readerConfigFile = config.get(stringToken);
                    break;
                case WRITER_CONFIG:
                    writerConfigFile = config.get(stringToken);
                    break;
                case EXECUTOR_CONFIG:
                    executorConfigFiles = config.get(stringToken).split(grammar.GetSeparatorToken());
                    break;
            }//switch

        }//for

        //if amount of executors != amount of configs for them
        if(executorNames.length!=executorConfigFiles.length)
            return RC.RC_EXECUTOR_CONFIG_GRAMMAR_ERROR;

        return RC.RC_SUCCESS;
    }

    //get elements of pipeline
    private RC GetReader(String readerClassName){
        try {
            Class<?> tmp = Class.forName(readerClassName);

            if (IReader.class.isAssignableFrom(tmp))
                reader = (IReader) tmp.getDeclaredConstructor().newInstance();
            else
                return RC_CANT_FIND_READER_CLASS;
        }
        catch(Exception e) {
            return RC_CANT_FIND_READER_CLASS;
        }

        return RC.RC_SUCCESS;
    }
    private RC GetWriter(String writerClassName){
        try {
            Class<?> tmp = Class.forName(writerClassName);
            if (IWriter.class.isAssignableFrom(tmp))
                writer = (IWriter) tmp.getDeclaredConstructor().newInstance();
            else
                return RC_CANT_FIND_WRITER_CLASS; }
        catch (Exception e) {
            return RC_CANT_FIND_WRITER_CLASS;
        }
        return RC.RC_SUCCESS;
    }

    private RC GetExecutors(String[] executorClassNames){
        executors = new IExecutor[executorClassNames.length];
        for(int i=0; i<executorClassNames.length; ++i) {
            try {
                Class<?> tmp = Class.forName(executorClassNames[i]);
                if (IExecutor.class.isAssignableFrom(tmp))
                    executors[i] = (IExecutor) tmp.getDeclaredConstructor().newInstance();
                else
                    return RC_CANT_FIND_EXECUTOR_CLASS;
            } catch (Exception e) {
                return RC_CANT_FIND_EXECUTOR_CLASS;
            }
        }
        return RC.RC_SUCCESS;
    }

    //set all information about elements of pipeline
    private RC setReader(){

        RC err = reader.setConfig(readerConfigFile);
        if (!err.isSuccess())
            return err;

        err = reader.setInputStream(inputStream);
        if (!err.isSuccess())
            return err;

        err = reader.setConsumer(executors[0]);
        if (!err.isSuccess())
            return err;


        return RC.RC_SUCCESS;
    }

    private RC setWriter(){

        RC err = writer.setConfig(writerConfigFile);
        if (!err.isSuccess())
            return err;
        err = writer.setOutputStream(outputStream);
        if (!err.isSuccess())
            return err;
        return RC.RC_SUCCESS;
    }

    //setConsumer method includes setProvider in itself
    private RC setExecutors(){
        RC err;
        for(int i=0; i<executors.length; ++i) {
            err = executors[i].setConfig(executorConfigFiles[i]);
            if (!err.isSuccess())
                return err;
        }

        //if more than 1 executor in pipeline
        if(executors.length>1) {
            err = executors[0].setConsumer(executors[1]);
            if (!err.isSuccess())
                return err;

            for (int i = 1; i < executors.length - 1; ++i) {
                err = executors[i].setConsumer(executors[i + 1]);
                if (!err.isSuccess())
                    return err;
            }
        }

        err = executors[executors.length-1].setConsumer(writer);
        if (!err.isSuccess())
            return err;
        return RC.RC_SUCCESS;
    }

    //start pipeline
    public RC RunPipeline(String filename) {
        RC err = SetLogger();
        if(!err.isSuccess())
            return err;

        err = setConfig(filename);
        if (!err.isSuccess())
            return err;

        err = GetWriter(writerName);
        if (!err.isSuccess())
            return err;
        err = GetReader(readerName);
        if (!err.isSuccess())
            return err;
        err = GetExecutors(executorNames);
        if (!err.isSuccess())
            return err;

        err = setReader();
        if (!err.isSuccess())
            return err;
        err = setExecutors();
        if (!err.isSuccess())
            return err;
        err = setWriter();
        if (!err.isSuccess())
            return err;

        //start threads
        Thread threadReader = new Thread(reader);
        threadReader.start();

        Thread[] threadExecutors = new Thread[executors.length];
        for (int i = 0; i < executors.length; i++) {
            threadExecutors[i] = new Thread(executors[i]);
            threadExecutors[i].start();
        }

        Thread threadWriter = new Thread(writer);
        threadWriter.start();

        try {
            threadReader.join();
            for (int i = 0; i < executors.length; i++) {
                threadExecutors[i].join();
            }
            threadWriter.join();
        } catch (InterruptedException ex) {
            return RC_MANAGER_THREAD_ERROR;
        }

        //close streams
        try {
            inputStream.close();
            outputStream.close();
        }catch(IOException e){
            return RC_CANT_CLOSE_STREAMS;
        }

        //if successfully completed
        return RC.RC_SUCCESS;
    }

    private static RC SetLogger(){
        logger = Logger.getLogger("MyLogger");
        FileHandler fh;
        try {
            fh = new FileHandler(loggerFile);
            SimpleFormatter sf = new SimpleFormatter();
            fh.setFormatter(sf);
            logger.addHandler(fh);
            logger.setUseParentHandlers(false);
        } catch(SecurityException ex){
                return RC_CANT_BUILD_LOGGER;
        } catch (IOException ex) {
            return RC_CANT_BUILD_LOGGER;
        }
        return RC.RC_SUCCESS;
    }

    private boolean CheckThreadErrors(){
        if(!reader.GetError().isSuccess())
            return true;
        else if(!writer.GetError().isSuccess())
            return true;

        for (IExecutor executor : executors) {
            if (!executor.GetError().isSuccess())
                return true;
        }

        return false;
    }

    public void run(String filename){
        RC rc = RunPipeline(filename);

        //if manager couldn't build logger, write to console about it
        if(rc.equals(RC_CANT_BUILD_LOGGER)){
            System.out.println("ERROR: " + rc.who.get() + " : " + rc.info);
        }

        //else write to log file
        else if(!rc.isSuccess() || CheckThreadErrors()) {
            System.out.println("ERROR. More information in log file...");

            logger.log(Level.INFO, "ERROR: " + rc.who.get() + " : " + rc.info);

            if(!reader.GetError().isSuccess()) {
                rc = reader.GetError();
                logger.log(Level.INFO, "ERROR: " + rc.who.get() + " : " + rc.info);
            }
            if(!writer.GetError().isSuccess()) {
                rc = writer.GetError();
                logger.log(Level.INFO, "ERROR: " + rc.who.get() + " : " + rc.info);
            }
            for (IExecutor executor : executors) {
                if (!executor.GetError().isSuccess()) {
                    rc = executor.GetError();
                    logger.log(Level.INFO, "ERROR: " + rc.who.get() + " : " + rc.info);
                }
            }
        }

        else{
            logger.log(Level.INFO,"All is corrected!");
            System.out.println("All is corrected!");
        }
    }

}

class Main{

    public static void main(String[] args){
        if(args.length!=1){
            System.out.println("ERROR: Main function: Wrong amount of arguments in command line!\n");
        }else{
            Manager manager = new Manager();
            manager.run(args[0]);
        }
    }
}
