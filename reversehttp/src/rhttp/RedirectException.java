package rhttp;

import java.io.*;

/**
 * An exception handling whenever redirect is needed to be sent to the browser.
 * author <a href="mailto:esittmit.edu">Eric Sit</a> version %I%, %G% (c) Eric
 * Sit 1999
 */
public class RedirectException extends HttpException {
	protected String location;

	/*
	 * Creates a RedirectException, given an HTTP error code and a location.
	 */
	public RedirectException(int code, String location) {
		super(code, "The document has moved <A HREF=\"" + location
				+ "\">here</A>.");
		this.location = location;
	}

	/*
	 * Sends headers telling the browser the new location of the object.
	 */
	public void processRequest(HttpOutputStream out) throws IOException {
		out.setHeader("Location", location);
		super.processRequest(out);
	}
}
