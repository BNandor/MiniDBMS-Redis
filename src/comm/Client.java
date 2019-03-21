package comm;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import struct.Databases;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;

public class Client {

    public static final String getDatabasesQuery = "GET DATABASES";
    private static final int timeout = 2000;
    private int port;
    private String ip;
    private Socket mSocket;
    private PrintWriter messageSender;
    private BufferedReader messageReader;
    private static Client instance;

    public static Client getClient() {
        if (instance == null) {
            synchronized (Client.class) {
                if (instance == null) {
                    instance = new Client("localhost", 1695);
                    instance.connect();
                }
            }
        }

        return instance;
    }

    public Client(String ip, int port) {
        this.port = port;
        this.ip = ip;
    }

    private void waitTimeout() {
        try {
            while (!messageReader.ready()) {
                Thread.sleep(timeout / 10);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        try {
            System.out.println("Client:Connecting to server");
            mSocket = new Socket(ip, port);
            messageSender = new PrintWriter(mSocket.getOutputStream());
            messageReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            System.out.println("Client:Succesfully connected to server");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void write(String text) {
        if (mSocket == null) {
            throw new NotYetConnectedException();
        }

        System.out.println("Client writing" + text);
        messageSender.write(text);
        System.out.println("Client flushing" + text);
        messageSender.flush();
    }

    public ArrayList<String> readAll() {
        if (mSocket == null) {
            throw new NotYetConnectedException();
        }

        ArrayList<String> list = new ArrayList<>();
        String line;

        try {
            if (!messageReader.ready()) {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }

            while (messageReader.ready()) {
                list.add(messageReader.readLine());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public String readLine() {
        if (mSocket == null) {
            throw new NotYetConnectedException();
        }
        String line = null;
        try {
            System.out.println("Client reading line");
            line = messageReader.readLine();
            System.out.println("Client read line" + line);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return line;
    }

    public synchronized Databases getDatabases() {

        write(getDatabasesQuery + "\n");

        try {

            String xml = "", line;
            waitTimeout();
            while (messageReader.ready() && (line = messageReader.readLine()) != null) {
                xml = xml + line;
            }
            XmlMapper xmlReader = new XmlMapper();
            return xmlReader.readValue(xml, Databases.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Client: Error, could not read Databases from Server(timeout)");
        return null;
    }

}
