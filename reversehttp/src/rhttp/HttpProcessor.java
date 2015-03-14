package rhttp;

import java.io.*;

/**
 * The common interface for all HttpProcessors. HttpProcessors handle HTTP
 * requests.
 * 
 * author <a href="mailto:esitemit.edu">Eric Sit</a> version %I%, %G% (c) Eric
 * Sit 1999 10
 */
public interface HttpProcessor {
	/** Processes the request, writing any appropriate data to out. */

	public void processRequest(HttpOutputStream out) throws IOException;
}
