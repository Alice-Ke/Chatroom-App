import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    Map< String, String [] > csInfo = new HashMap< String, String [] >();
    DatagramSocket sock = null;

    public static void echol(String msg) {
        System.out.println(msg);
    }
    public static void echo(Object msg) {
        System.out.print(msg);
    }
    public void broadcast( Map< String, String [] > csInfo ) throws Exception {
        byte [] bcsInfo = makePacket( csInfo, "table" );
        for ( Map.Entry < String, String [] > entry : csInfo.entrySet() ) {
            String [] arr = entry.getValue();
            Object [] rArr = stringToRaw( arr );

            if ( rArr[ 2 ] == "Yes") {
                DatagramPacket dpcsInfo = new DatagramPacket(bcsInfo, bcsInfo.length, (InetAddress)rArr[ 0 ], (Integer)rArr[ 1 ] );
                sock.send( dpcsInfo );
            }
        }
    }

    public String readOffline(String destName) {
        try {
            File file = new File(destName + ".txt");
            if (file.exists()) {

                BufferedReader br = new BufferedReader(new FileReader(file.getAbsoluteFile()));
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();

                while (line != null) {
                    sb.append(">>>");
                    sb.append(line);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
                file.delete();
                return sb.toString();
            }
        }


        catch (Exception e) {

            e.printStackTrace();

        }
        return null;
    }

    // content, dest name
    public void writeOffline(String destName, String content ) {
        BufferedWriter bw = null;
        FileWriter fw = null;

        try {
            File file = new File(destName + ".txt");
            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }
            // true = append file
            fw = new FileWriter(file.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);

            bw.write(content);
            bw.newLine();

        } catch (IOException e) {

            e.printStackTrace();

        } finally {
            try {
                if (bw != null)
                    bw.close();

                if (fw != null)
                    fw.close();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
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
    // TODO: throw Exception
    public byte [] makePacket( Object some, String cmd ) throws Exception {
        Map< String, Object > packetDic = new HashMap< String, Object >();
        packetDic.put( cmd, some );
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(packetDic);
        return byteOut.toByteArray();
    }

    public Map< String, Object > decode( byte [] data ) throws Exception {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
        ObjectInputStream in = new ObjectInputStream(byteIn);
        Object inObj = in.readObject(); // dictionary
        Map< String, Object > dic = ( Map< String, Object > ) inObj;
        return dic;
    }

    public void work( int port ) {
        try {
            sock = new DatagramSocket( port );
            byte[] buffer = new byte[65536];
            DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);

            while (true) {
                sock.receive(incoming);
                byte[] data = incoming.getData();
                // get port and address info
                int cPort = incoming.getPort();
                InetAddress cIP = incoming.getAddress();
                Map< String, Object > packetDic = decode( data );

                String name;
                if ( ( name  = ( String )packetDic.get("dereg") ) != null ) {
                    // update the table
                    String [] entry = csInfo.get( name );
                    entry[ 2 ] = "No";
                    csInfo.put( name, entry );
                    String sregAck = "[You are Offline. Bye]";
                    byte [] back = makePacket( sregAck, "deregack" );
                    DatagramPacket dpack = new DatagramPacket(back, back.length, cIP, cPort );
                    sock.send( dpack );

                    broadcast( csInfo);
                }

                else if ( packetDic.get("reg" ) != null  ) {
                    String s = (String)packetDic.get("reg" );

                    String scIP = cIP.getHostAddress();

                    String scPort = Integer.toString( cPort );
                    String [] cInfoArr = { scIP, scPort, "Yes" };
                    // name: { clientIP, clientPort, status }
                    csInfo.put( s, cInfoArr );
                    // send registration ack to the client
                    String sregAck = "[Welcome, You are registered.]";
                    byte [] back = makePacket( sregAck, "ack" );
                    DatagramPacket dpack = new DatagramPacket(back, back.length, cIP, cPort );
                    sock.send( dpack );

                    String offMessage;
                    if ( ( offMessage = readOffline(s) ) != null ) {
                        String offAck = "[You have messages]";
                        byte [] boffAck = makePacket( offAck, "sendoff" );
                        DatagramPacket dpoffAck = new DatagramPacket(boffAck, boffAck.length, cIP, cPort );
                        sock.send( dpoffAck );

                        byte [] boffMessage = makePacket( offMessage, "sendoff" );
                        DatagramPacket dpoffMessage = new DatagramPacket(boffMessage, boffMessage.length, cIP, cPort );
                        sock.send( dpoffMessage );

                    }
                    // broadcast table
                    broadcast( csInfo);
                }

                else if ( packetDic.get("send" ) != null ) {
                    String s = (String)packetDic.get("send" );
                    String [] sArr = s.split(" ", 2 );
                    String destName = sArr[ 0 ];
                    if ( ( csInfo.get( destName ) )[ 2 ].equals("No")) {
                        writeOffline( destName, sArr[1]);
                        // send saved ack to client
                        String savedAck = "[Messages received by the server and saved]";
                        byte [] bsavedAck = makePacket( savedAck, "savedAck");
                        DatagramPacket dpsavedAck = new DatagramPacket(bsavedAck, bsavedAck.length, cIP, cPort );
                        sock.send( dpsavedAck);
                        //rebroadcast the table
                        broadcast( csInfo );
                    }
                    // if client is online
                    else {
                        String sregAck = "[Client " + destName + " exists!!]";
                        byte [] back = makePacket( sregAck, "errorAck" );
                        DatagramPacket dpack = new DatagramPacket(back, back.length, cIP, cPort );
                        sock.send( dpack );
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws Exception {
        try {
            Server server = new Server();
            server.work( Integer.parseInt( args[ 0 ] ));
        } catch ( ArrayIndexOutOfBoundsException e ) {
            echol("Wrong Server-mode Input Format: please use 'Udpchat -s <port>'");
        }
    }
}
