import java.io.*;
import java.net.*;
import java.util.*;
import java.time.LocalTime;
import java.lang.*;

public class SThreadClient extends Thread {

    DatagramSocket sock = null;
    String name;
    int serverPort;
    InetAddress serverIP;
    Map< String, String [] > csInfoS;
    BufferedReader cin = new BufferedReader(new InputStreamReader(System.in));
    Map< String, Object > packetDic = new HashMap< String, Object >();
    IntBoxer mackFlag = new IntBoxer();
    IntBoxer deregFlag = new IntBoxer();

    public SThreadClient( String name, InetAddress host, int serverPort, DatagramSocket sock, Map< String, String [] > csInfo, IntBoxer mackFlag, IntBoxer deregFlag ) {
        this.sock = sock;
        this.name = name;
        serverIP = host;
        this.serverPort = serverPort;
        csInfoS = csInfo;
        this.mackFlag = mackFlag;
        this.deregFlag = deregFlag;

    }

    public void sendOffline( String destName, String content ) throws Exception {
        LocalTime l = LocalTime.now();
        String offDisplay = destName + " " + "client " + name + ": " + l + " " + content;
        byte [] offMessage = makePacket( offDisplay, "send" );
        DatagramPacket dpoffMessage = new DatagramPacket(offMessage, offMessage.length, serverIP, serverPort );
        sock.send(dpoffMessage);
    }


    public Object [] stringToRaw( String [] arr ) throws Exception {
        String sIP = arr[ 0 ];
        String sPort = arr[ 1 ];

        Object [] rarr = new Object[ 3 ];
        rarr [ 0 ] = InetAddress.getByName( sIP );
        rarr [ 1 ] = Integer.parseInt( sPort );
        rarr [ 2 ] = arr[ 2 ];

        return rarr;
    }

    public byte [] makePacket( Object some, String cmd ) throws Exception {
        packetDic = new HashMap< String, Object >();
        packetDic.put( cmd, some );
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(packetDic);
        return byteOut.toByteArray();
    }

    public void run() {

        try {
            // byte [] bn = name.getBytes();
            byte [] bn = makePacket( name, "reg" );
            DatagramPacket reg = new DatagramPacket( bn, bn.length, serverIP, serverPort );
            sock.send( reg );
            echo(">>>");

            mainLoop:
            while (true) {
                String s = (String)cin.readLine();
                String [] comArr = s.split( " ", 2 );
                echo(">>>");

                // send message to client
                if ( comArr[ 0 ].equals("send")) {
                    String [] sArr = s.split(" ", 3);
                    String content = sArr[ 2 ];
                    String destName = sArr[ 1 ];
                    // lookup for dest IP and port
                    String [] sdestArr = csInfoS.get( destName );
                    Object [] rdestArr = stringToRaw( sdestArr );
                    String status = ( String ) rdestArr[ 2 ];
                    if ( status.equals( "Yes") ) {
                        String mesDisplay = "Client " + name + ": " + content;
                        byte [] bmessage = makePacket( mesDisplay, "send" );
                        // restore flag
                        mackFlag.value = 1;
                        DatagramPacket dpmessage = new DatagramPacket(bmessage, bmessage.length, (InetAddress)rdestArr[ 0 ], (Integer)rdestArr[ 1 ] );
                        sock.send(dpmessage);
                        Thread.sleep( 500 );

                        if ( mackFlag.value == 1) {
                            // send to server
                            sendOffline( destName, content );
                            echol("[No ACK from " + destName + ", message sent to server.");
                            echo(">>>");
                        }
                    }
                    // if status offline
                    else {
                        sendOffline( destName, content);
                    }


                }

                if ( comArr[ 0 ].equals("reg")) {
                    byte [] rebn = makePacket( name, "reg" );
                    DatagramPacket rereg = new DatagramPacket( rebn, rebn.length, serverIP, serverPort );
                    sock.send( rereg );
                }

                //send dereg message to the server
                if ( comArr[ 0 ].equals("dereg")) {
                    byte [] bmessage = makePacket( name, "dereg" );
                    DatagramPacket dpmessage = new DatagramPacket(bmessage, bmessage.length, serverIP, serverPort );
                    sock.send(dpmessage);
                        deregFlag.value = 1;
                        Thread.sleep( 500 );
                        if ( deregFlag.value != 0){
                    for ( int i = 0; i < 5; i++ ) {
                        echol("[Sorry, please retry]");
                        echo(">>>");
                        String temp = (String)cin.readLine();
                        echo(">>>");
                        sock.send(dpmessage);
                        deregFlag.value = 1;
                        Thread.sleep( 500 );
                        if ( deregFlag.value == 0) {
                            break;
                        }
                    }
                    // if server not ack
                    if ( deregFlag.value != 0 ) {
                        echol("[Server not responding]");
                        echo(">>>[Exiting]");
                        break mainLoop;

                        //TODO: exit
                    }
                }
            }

            }
        }

        catch ( Exception e) {
            e.printStackTrace();
        }
    }

    public  void echol(Object msg) {
        System.out.println(msg);
    }
    public  void echo(Object msg) {
        System.out.print(msg);
    }

}