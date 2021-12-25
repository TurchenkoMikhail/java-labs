import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

import com.java_polytech.pipeline_interfaces.*;

// syntax analyzer
public class Config {

    private final HashMap<String, String> config = new HashMap<>();
    private final MyAbstractGrammar grammar;

    public Config(MyAbstractGrammar grammar){
       this.grammar = grammar;
    }

    //parse
    public RC ParseConfig(String filename){
        try (Scanner scanner = new Scanner(new File(filename))){
            String line;

            while (scanner.hasNext()){
                line = scanner.nextLine();

                //delete spaces and tabs
                line = line.replace(" ", "");
                line = line.replace("\t", "");

                //delete comments
                int offset = line.indexOf(grammar.GetCommentSymbols());
                if(offset!= (-1))
                    line = line.substring(0, offset);

                String[] token = line.split(MyAbstractGrammar.GetDelimiter());

                if(token.length==2 && grammar.isValidToken(token[0]))
                    config.put(token[0],token[1]);

            } //while()

            //if some config parameters are empty
            if (config.size() != grammar.getNumTokens()){
                return grammar.getIncompleteConfigErrorCode();
            }

            return RC.RC_SUCCESS;

        } catch(FileNotFoundException e){
            return grammar.getNoFileErrorCode();
        }

    } //RC ParseConfig function

    public String get(String key) { return config.get(key);}
}
