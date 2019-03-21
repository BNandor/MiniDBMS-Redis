package structbuilder;

import comm.Worker;

import java.io.IOException;
import java.rmi.ServerException;

public class DatabaseBuilder {

    public static synchronized void createDatabase(String databaseName){
        try {
            Runtime.getRuntime().exec("mkdir "+ Worker.path_to_work+"/"+databaseName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void deleteDatabase(String databaseName){
        try {
            if(Worker.path_to_work.equals("")||databaseName.equals("")||Worker.path_to_work.equals(" ")||databaseName.equals(" ")){
                throw new ServerException("Cannot delete database, wrong database name");
            }

            Runtime.getRuntime().exec("rm -rf "+ Worker.path_to_work+"/"+databaseName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
