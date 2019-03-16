package persistence;

import com.sun.corba.se.spi.orbutil.threadpool.Work;
import comm.Worker;
import redis.clients.jedis.Jedis;

import javax.naming.OperationNotSupportedException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RedisConnector {//Lifecycle: start thread, create connection, {insert, get, select}, kill
    public static final int num_of_tables=30;
    public static final String path_to_redis_executable="redis-5.0.3/src/redis-server";
    private Jedis j;
    private Process redisProcess;
    public RedisConnector() {

    }

    public void start() {
        try {
            System.out.println(path_to_redis_executable+" --dir "+ Worker.path_to_work+"/"+XML.getDatabasesInstance().getDatabaseList().get(Worker.currentlyWorking).getDatabaseName()+
                    " --dbfilename "+XML.getDatabasesInstance().getDatabaseList().get(Worker.currentlyWorking).getDatabaseName()+".dmp");

            redisProcess=Runtime.getRuntime().exec(path_to_redis_executable+" --dir "+ Worker.path_to_work+"/"+XML.getDatabasesInstance().getDatabaseList().get(Worker.currentlyWorking).getDatabaseName()+
                    " --dbfilename "+XML.getDatabasesInstance().getDatabaseList().get(Worker.currentlyWorking).getDatabaseName()+".dmp");
            System.out.println("Working Directory = " +
                    System.getProperty("user.dir"));
            BufferedReader in = new BufferedReader(new InputStreamReader(redisProcess.getInputStream()));
            String line;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            while(in.ready() && (line=in.readLine())!=null){
                System.out.println(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void killServer()  {
        if(!redisProcess.isAlive()){
            System.out.println("RedisConnector: cannot kill server which was not started already");
            return;
        }
        if(j == null){
            System.out.println("RedisConnector: cannot save db because not connected");
        }
        j.save();
        j.shutdown();
        j.close();
        j=null;
        redisProcess=null;
        System.out.println("RDB stopped");
    }

    public void dropselected(){
        j.flushDB();
    }
    public boolean running(){
        if(redisProcess==null)return false;
        return redisProcess.isAlive();
    }

    public  void connect(){
        j = new Jedis("localhost",6379);
    }
    public void set(String key,String value){
        j.set(key,value);
    }
    public String get(String key){
        return j.get(key);
    }
    public void select(int slot){
        j.select(slot);
    }
    public void save(){
        j.save();
    }
    public void delkey(String key){
        j.del(key);
    }




    public boolean keyExists(String key){
        return j.exists(key);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        killServer();
    }
}
