package SpringApp.Services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.springframework.stereotype.Service;

@Service
public class ReadfileService {

    public static String readFileContentFromInputStream(InputStream inputStream) {
        StringBuilder contentBuilder = new StringBuilder();
        try {
            InputStreamReader isReader = new InputStreamReader(inputStream);

            BufferedReader reader = new BufferedReader(isReader);
            String line;
            while((line = reader.readLine())!= null){
                contentBuilder.append(line);
            }
            System.out.println(contentBuilder.toString());
        } catch (IOException ex){
            System.out.println(ex);
        }

        return contentBuilder.toString();
    }
}
