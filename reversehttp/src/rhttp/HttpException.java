package rhttp; 
import java.io.*;
/*
* An exception handling whenever something unusual needs to be sent to browser.
*
* author <a href="maito:esitemit.edu">Eric Sit</a>
* version %I%, %G%
* (c) Eric Sit 1999
*/
public class HttpException extends IOException implements HttpProcessor {
protected int code;
/*
* Creates an HttpException, given an HTTP error code and a location.
* */

public HttpException (int code, String detail) {
super (detail);
this.code = code;
} 
/** Sends back an HTML page giving error.
 * 
 */
public void processRequest (HttpOutputStream out) throws IOException {
out.setCode (code);
out.setHeader ("Content-Type", "text/html");
if (out.sendHeaders ()) {

String msg = HTTP.getCodeMessage (code);
out.write ("<HTML><HEAD><TITLE>" + code + " " +
msg + "</TITLE></HEAD>\n" + "<BODY><H1>" + msg + "</H1>\n" +
getMessage () + "<P>\n</BODY></HTML>\n");

}
}
}