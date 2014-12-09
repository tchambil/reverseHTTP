package rhttp; 
import java.io.*;
import java.net.*;
import java.util.*;
/*
* The HttpProcessor that handles requests to close the connection.
*
* author <a href ="mailto: esitemit. edu" >Eric Sit</a>
* version %I%, %G%
* (c) Eric Sit 1999
10*/
public class RhttpClose implements HttpProcessor {
protected File file;
SocketList socketList;
Hashtable controlHash;
Socket client;
HttpInputStream in;
String aID;
public boolean keepAlive = false;
public RhttpClose (HttpInputStream in, SocketList socketList,
Hashtable controllash, Socket client) throws IOException {
this.socketList = socketList;
this.controlHash = controlHash;
this.in = in;
this.client = client;
// we assume that path begins with 1closel
if (!in.getPath().startsWith(HTTP.CLOSE_BIN))
throw new HttpException (HTTP.STATUS_BAD_REQUEST, in.getPath ());
 
if (in.getPath().length() < HTTP.CLOSE_BIN.length())
throw new HttpException (HTTP.STATUS_BAD_REQUEST, in.getPath ());
int indexOfClose = HTTP.CLOSE_BIN.length();
 
this.aID = in.getPath().substring(indexOfClose);
System.out.println("Agent-ID in PAC: "+aID);
}
public void processRequest (HttpOutputStream out) throws IOException {
if (in.getMethod () == HTTP.METHOD_GET) 
{
deleteServer(aID); 
sendResponse(out);
}
}
void deleteServer(String aID) {
ServerSocket ss = (ServerSocket) socketList.get(aID);
 
InetAddress address = ss.getInetAddress();
int port = ss.getLocalPort();
// shutdown safely the controlThread for the server
ProxyFile controlThread = (ProxyFile) controlHash.get(aID);
controlThread.shutdown = true;
try {
Socket shutdownSocket = new Socket(address.getLocalHost(), port);
} catch (IOException e) {
System.err.println("Couldn't connect to control proxy to shut it down!");
e.printStackTrace();
}
controlHash.remove(aID);
socketList.remove(aID);
}
void sendResponse(HttpOutputStream out) throws IOException {
 
System.out.println("Attempting to close entire connection.");
out.setHeader ("Content-type", "text/html");
out.sendHeaders();
out.write("<hl>Connection has been closed! <hi><p>\n");
out.write("Agent ID is "+aID+"<p>");
out.write("Connection destroyed! <p>");
}
}
