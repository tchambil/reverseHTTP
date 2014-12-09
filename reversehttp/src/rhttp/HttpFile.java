package rhttp;
import java.io.*;
/**
* The HttpProcessor that handles simple requests for files.
*
* author <a href="mailto:esitemit.edu">Eric Sit</a>
* version %I%, %G%
* (c) Eric Sit 1999
10
87*/
public class HttpFile implements HttpProcessor {
protected File file;
/**
* Constructor for HttpFile.
*
* Client's file request for a file is passed into the contructor which throws any
* excpetions that result from the client's request.
* The constructor fully parses the client's request. If the client attempts to post 20
* to a file, we throw an appropriate HttpException.
*
* We translate the request into a file within the local HTML document directory, appending
* index.html if the request ends in /.*/

public HttpFile (HttpInputStream in) throws IOException {
if (in.getMethod() == HTTP.METHOD_POST)
throw new HttpException (HTTP.STATUS_NOT_ALLOWED,
"<TT>" + in.getMethod () + " " + in.getPath () + "</TT>");
file = new File (HTTP.HTML_ROOT, HTTP.translateFilename (in.getPath ())); 
if (in.getPath ().endsWith ("/"))
file = new File (file, HTTP.DEFAULT_INDEX);
if (!file.exists ())
throw new HttpException (HTTP.STATUS_NOT_FOUND,
"File <TT>" + in.getPath () + "</TT> not found.");
if (file.isDirectory ())
throw new RedirectException (HTTP.STATUS_MOVED_PERMANENTLY,
in.getPath () + "/");
if (!file.isFile () || !file.canRead ())
throw new HttpException (HTTP.STATUS_FORBIDDEN, in.getPath ());  
}
/*
* When it is time to send a response, this method is called.
* We set Content-type and Content-length headers, then call sendHeaders() to send
* the headers; and finally, transmit the body of the file if necessary.
* */

public void processRequest (HttpOutputStream out) throws IOException {
out.setHeader ("Content-type", HTTP.guessMimeType(file.getName ()));  
out.setHeader ("Content-length", String.valueOf(file.length ()));
if (out.sendHeaders ()) {
FileInputStream in = new FileInputStream (file);
out.write(in);
 
in.close ();
}
}

}