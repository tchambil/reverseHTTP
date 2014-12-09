 
package rhttp;
import java.io.*;
import java.net.*;
import java.util.*;
/*
* The HttpProcessor that handles requests for a list of available servers.
*
* author <a href="mailto:esitemit.edu">Eric Sit</a>
* version %I%, %G% 10
* (c) Eric Sit 1999*/

public class RhttpServers implements HttpProcessor {
protected File file;
SocketList socketList;
HttpInputStream in;
public boolean keepAlive = false;
 
public RhttpServers(HttpInputStream in, SocketList socketList) throws IOException {
	this.socketList = socketList;
	this.in = in;
}

public void processRequest (HttpOutputStream out) throws IOException {
	
if (in.getMethod()== HTTP.METHOD_GET) {
	listServers(out);
	}  
}
void listServers(HttpOutputStream out) throws IOException {
// figure out the localhost
String localHost;
try {
localHost = InetAddress.getLocalHost ().getHostName();
} catch (UnknownHostException ex) {  
localHost = "localhost";
}
// display server list
System.out.println("Showing server list.");
out.setHeader ("Content-type", "text/html");
out.sendHeaders();
out.write("<hi>Available Servers/hl><p>\n");  
if (socketList.isEmpty())
out.write("No servers have been added.");
else {
Enumeration slEnum = socketList.keys();
while (slEnum.hasMoreElements()) {
String aID = (String) slEnum.nextElement();
out.write("<hr>");
out.write("For RHA " + aID +", we have allocated a tunneling proxy at ");
// figure out which local port has been allocated to mirror it. 60
 
ServerSocket localSS = (ServerSocket) socketList.get(aID);
out.write("<a href=http://"+ localHost +":" + localSS.getLocalPort() +"/>");
out.write("http://"+ localHost + ":" + localSS.getLocalPort() +"/</a><br>");
out.write("You need change your proxy configuration to use this proxy.<p>");
// write direction to get to a PAC file
out.write("Use this PAC file: <a href=http://"+ localHost +"/pac/"+ aID +".pac"+">");
out.write("http://"+ localHost +"/pac/"+ aID +" .pac"+"</a><p>");
out.write("Then browse: <a href=http://" + aID +">http://" +aID+ "</a><p>\n"); 
out.write("Kill connection: <a href=http://"+ localHost +"/close/" +aID+ ">");
out.write("http://"+ localHost +"/close/" +aID+ "</a>.<p>");
}
}
}
}
