import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;
import java.time.LocalTime;

public class RThreadClient extends Thread {

  DatagramSocket sock = null;
  Map< String, String [] > csInfoR;
  String name;
  Map< String, Object > packetDic = new HashMap< String, Object >();
  IntBoxer mackFlag = new IntBoxer();
  IntBoxer deregFlag = new IntBoxer();

  public RThreadClient( DatagramSocket sock, Map< String, String [] > csInfo, String name, IntBoxer mackFlag, IntBoxer deregFlag ) {
    this.sock = sock;
    csInfoR = csInfo;
    this.name = name;
    this.mackFlag = mackFlag;
    this.deregFlag = deregFlag;
  }

  public byte [] makePacket( Object some, String cmd ) throws Exception {
    packetDic = new HashMap< String, Object >();
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


  public void run() {

    String s;
    try {
      while (true) {

        byte[] buffer = new byte[65536];
        DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
        sock.receive(reply);
        byte[] data = reply.getData();
        Map< String, Object > packetDic = decode( data ); //get dictionary

        // check if table so easy !
        if ( packetDic.get("send") != null ) {
          s = ( String )packetDic.get("send");
          echol(s);
          echo(">>>");
          // send packet to ack the sender
          InetAddress cIP = reply.getAddress();
          int cPort = reply.getPort();
          String smesAck = "[Message received by " + name + "]";
          byte [] back = makePacket( smesAck, "sendAck" );
          DatagramPacket dpack = new DatagramPacket(back, back.length, cIP, cPort );
          sock.send( dpack );
        }

        if ( packetDic.get("sendAck") != null ) {
          s = ( String )packetDic.get("sendAck");
          // reset the flag
          mackFlag.value = 0;
          echol(s);
          echo(">>>");
        }
        if ( packetDic.get("sendoff") != null ) {
          s = ( String )packetDic.get("sendoff");
          echol(s);
          echo(">>>");
        }
        if ( packetDic.get("ack") != null ) {
          s = ( String )packetDic.get("ack");
          echol(s);
          echo(">>>");
        }
        if ( packetDic.get("savedAck") != null ) {
          s = ( String )packetDic.get("savedAck");
          echol(s);
          echo(">>>");
        }
        if ( packetDic.get("errorAck") != null ) {
          s = ( String )packetDic.get("errorAck");
          echol(s);
          echo(">>>");
        }
        if ( packetDic.get("deregack") != null ) {
          s = ( String )packetDic.get("deregack");
          //reset the flag
          deregFlag.value = 0;
          echol(s);
          echo(">>>");

        }

        if ( packetDic.get("table") != null ) {
          Map<String, String [] > newTable = (Map<String, String [] >) packetDic.get("table");
          csInfoR.putAll( newTable );
          echol( "[Client table updated.]");
          echo(">>>");
        }
      }
    } catch ( Exception e) {
      e.printStackTrace();
    }
  }

  public static void echol(Object msg) {
    System.out.println(msg);
  }
  public  void echo(Object msg) {
    System.out.print(msg);
  }

}