import com.google.gson.Gson;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class ShutdownThread extends Thread {
    private Parameters parameters;

    ShutdownThread(Parameters parameters){
        this.parameters = parameters;
    }

    public void run(){
        System.out.println("In shutdown hook");
        Gson gson = new Gson();
        String json = gson.toJson(parameters);
        try{
            PrintWriter out = new PrintWriter("weights8.txt");
            out.println(json);
            out.close();
        } catch(FileNotFoundException e){
            e.printStackTrace();
        }
    }
}
