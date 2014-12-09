package rhttp;
import java.io.*;
import java.net.*;
import java.util.*;
/*
* The HttpProcessor that handles requests for PAC files. PAC files are "Proxy automatic
* configuration files."
*
* This processor depends on a file "proxy.cfg" which contains site specific proxy 10
" information.
*
* author <a href="mailto: esitemit. edu" >Eric Sit</a>
* version %I%, %G%
* (c) Eric Sit 1999
* */

public class RhttpPac implements HttpProcessor {
protected File file;
HttpInputStream in; 
SocketList socketList;
String host;
int localPort;
String localHost;
public boolean keepAlive = false;
public RhttpPac (HttpInputStream in, SocketList socketList) throws IOException {
this.in = in; 
this.socketList = socketList;
System.out.println(in.getPath());
// we assume that path begins with /pac/ and ends with .pac
if (!in.getPath().startsWith(HTTP.PAC_BIN))
throw new HttpException (HTTP.STATUS_BAD_REQUEST, in.getPath());
if (!in.getPath().endsWith("'.pac"))
throw new HttpException (HTTP.STATUS_BAD_REQUEST, in.getPath()); 
if (in.getPath().length() < HTTP.PAC_BIN.length())
throw new HttpException (HTTP.STATUS_BAD_REQUEST, in.getPath ());
int indexOfPac = HTTP.PAC_BIN.length();
int indexOfExt = in.getPath().lastIndexOf(".pac");
String aID = in.getPath().substring(indexOfPac, indexOfExt);
host = aID;
 
ServerSocket ss = (ServerSocket) socketList.get(aID);
localPort = ss.getLocalPort();
// figure out the localhost
try {
localHost = InetAddress.getLocalHost ().getHostName();
} catch (UnknownHostException ex) {
localHost = "localhost";
}  
}

public void processRequest (HttpOutputStream out) throws IOException {
File file = new File("proxy.cfg");
FileInputStream from_file;

try {
from_file = new FileInputStream(file);
} catch (FileNotFoundException e) { 
System.err.println("Proxy config file unavailable!");
throw new IOException(e.toString());
}
System.out.println("Generating PAC.");
out.setHeader ("Content-type", "application/x-ns-proxy-autoconfig");  
out.sendHeaders();
out.write("function FindProxyForURL(url, host)\n");
out.write("{\n");
//These connect directly if the machine they are trying to
//connect to starts with host - ie http://host
out.write("if (shExpMatch( host, \""+host+"*\"))\n\t return \"PROXY "+
localHost +":"+localPort+";\"\n");
// copy rest of proxy config from file 90
byte[] buffer = new byte[4096]; int bytes_read;
try {
while ((bytes_read = from_file.read(buffer)) != -1)
{
out.write(buffer, 0, bytes_read);
out.flush();
 
}
}
catch (IOException e) {
System.err.println("Error writing proxy info to socket!");
try {
from_file.close(); 
} catch (IOException ef) {
System.err.println("Couldn't close file!");
ef.printStackTrace();
}
e.printStackTrace();
throw new IOException(e.toString() + "Error writing proxy info to socket");
} finally {
try {
from_file.close();
} catch (IOException e) {  
System.err.println("Couldn't close file!");
e.printStackTrace();
}
}
out.write("}\n");
out.flush();
}

}
