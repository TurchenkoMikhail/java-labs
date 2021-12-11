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

                String[] token = line.split(MyAbstractGrammar.GetDelimiter());

                //must be 2 strings: element of grammar and its value
                if (token.length != 2)
                    return grammar.getGrammarErrorCode();

                //if grammar element is not valid
                if (!grammar.isValidToken(token[0]))
                    return grammar.getGrammarErrorCode();

                config.put(token[0],token[1]);
            } //while()

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
