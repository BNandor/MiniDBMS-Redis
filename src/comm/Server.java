package comm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.NotYetConnectedException;

public class Server extends Thread {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private boolean run;
    private PrintWriter messageSender;
    private BufferedReader messageReader;
    private Worker worker;

    public Server(int port, Worker worker) {

        run = true;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.worker = worker;
        worker.start();
    }

    public void run() {
        try {

            System.out.println("Server:waiting for client");
            clientSocket = serverSocket.accept();
            worker.setSocket(clientSocket);
            messageReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            System.out.println("Server:waiting for job");
            String job;
            while ((job = messageReader.readLine()) != null) {
                System.out.println("Server:got job from client,pushing to worker:"+job);
                worker.addJob(job);
            }


        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void stopServer() {
        run = false;
//        try {
//            serverSocket.close();
//        }catch (IOException ex){
//        }
    }

    public String readLine() {
        if (clientSocket == null) {
            throw new NotYetConnectedException();
        }
        String line = null;
        try {
            line = messageReader.readLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return line;
    }
}
