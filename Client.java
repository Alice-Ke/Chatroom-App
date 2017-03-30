import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;
import java.time.LocalTime;

public class Client {
    Map< String, String [] > csInfo = new HashMap< String, String [] >();
    DatagramSocket sock = null;
    String name;
    IntBoxer mackFlag = new IntBoxer();
    IntBoxer deregFlag = new IntBoxer();

    public void work( String name, InetAddress host, int serverPort, int listenPort ) throws Exception {
        DatagramSocket sock = new DatagramSocket( listenPort );

        RThreadClient receiver = new RThreadClient(sock, csInfo, name, mackFlag, deregFlag );
        receiver.start();
        SThreadClient sender = new SThreadClient( name, host, serverPort, sock, csInfo, mackFlag, deregFlag);
        sender.start();

    }

    public static void main(String [] args) throws Exception {
        try {
            int listenPort = Integer.parseInt( args[ 3 ] );
            int serverPort = Integer.parseInt( args[ 2 ] );
            String serverIP = args[ 1 ];
            String name = args[ 0 ];
            InetAddress host = InetAddress.getByName( serverIP );

            Client client = new Client();
            client.work( name, host, serverPort, listenPort );
        } catch ( ArrayIndexOutOfBoundsException e ) {
            echol("Wrong Client-mode Input Format: please use 'Udpchat -c <nick-name> <server-ip> <sever-port> <client-port>'");
        }
    }

    public static void echol(Object msg) {
        System.out.println(msg);
    }
    public static void echo(Object msg) {
        System.out.print(msg);
    }

}