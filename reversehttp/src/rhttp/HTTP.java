package rhttp;

import java.io.*;
import java.net.*;
import java.util.*;

/*
 * HTTP contains useful constants and routines to implement the HTTP protocol.
 *
 * author <a href= "mailto: esitemit. edu" >Eric Sit</a>
 * version %I%, %G%
 * (c) Eric Sit 1999*/

public class HTTP {
	public static final String SERVER_INFO = "HTTPD/1.0";
	public static final String CONTROL_BIN = "/control/";
	public static final String DATA_BIN = "/data/";
	public static final String PAC_BIN = "/pac/";
	public static final String CLOSE_BIN = "/close/";
	public static final String SERVERS_BIN = "/servers/";
	public static final String LOG_BIN = "log/";
	public static final String TMP_BIN = "tmp/";
	public static final File SERVER_LOCATION = new File(
			System.getProperty("user.dir"));
	public static final File HTML_ROOT = new File(SERVER_LOCATION, "html");
	public static final int PORT = 80;
	public static final String DEFAULT_INDEX = "index.html";
	public static final String METHOD_GET = "GET";
	public static final String METHOD_POST = "POST";
	public static final String METHOD_HEAD = "HEAD";
	// standard HTTP error codes
	public static final int STATUS_OKAY = 200;
	public static final int STATUS_NO_CONTENT = 204;
	public static final int STATUS_MOVED_PERMANENTLY = 301;
	public static final int STATUS_MOVED_TEMPORARILY = 302;
	public static final int STATUS_BAD_REQUEST = 400;
	public static final int STATUS_FORBIDDEN = 403;
	public static final int STATUS_NOT_FOUND = 404;
	public static final int STATUS_NOT_ALLOWED = 405;
	public static final int STATUS_INTERNAL_ERROR = 500;
	public static final int STATUS_NOT_IMPLEMENTED = 501;

	/*
	 * Given a HTTP code, returns an appropriate string message
	 */
	public static String getCodeMessage(int code) {
		switch (code) {
		case STATUS_OKAY:
			return "OK";
		case STATUS_NO_CONTENT:
			return "No Content";
		case STATUS_MOVED_PERMANENTLY:
			return "Moved Permanently";
		case STATUS_MOVED_TEMPORARILY:
			return "Moved Temporarily";
		case STATUS_BAD_REQUEST:
			return "Bad Request";
		case STATUS_FORBIDDEN:
			return "Forbidden";
		case STATUS_NOT_FOUND:
			return "Not Found";
		case STATUS_NOT_ALLOWED:
			return "Method Not Allowed";
		case STATUS_INTERNAL_ERROR:
			return "Internal Server Error";
		case STATUS_NOT_IMPLEMENTED:
			return "Not Implemented";
		default:
			return "Unknown Code C" + code + ")";
		}
	}

	/*
	 * Vector containing server information
	 */

	protected static final Vector environment = new Vector();
	static {
		environment.addElement("SERVERSOFTWARE=" + SERVER_INFO);
		environment.addElement("GATEWAYINTERFACE=" + "CGI/1.0");
		environment.addElement("SERVERPORT=" + PORT);
		environment.addElement("DOCUMENT-ROOT=" + HTML_ROOT.getPath());
		try {
			environment.addElement

			("SERVER_NAME=" + InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException ex) {
			environment.addElement("SERVER_NAME=kocalhost");
		}
	}

	/*
	 * Strips all relative components from a path, including //, /.!, and I..!,
	 * returning the resulting properly canonicalized path.
	 */

	public static String canonicalizePath(String path) {
		char[] chars = path.toCharArray();
		int length = chars.length;
		int idx, odx = 0;
		while ((idx = indexOf(chars, length, '/', odx)) < length - 1) {
			int ndx = indexOf(chars, length, '/', idx + 1), kill = -1;
			if (ndx == idx + 1) {
				kill = 1;
			} else if ((ndx >= idx + 2) && (chars[idx + 1] == '.')) {
				if (ndx == idx + 2) {
					kill = 2;
				} else if ((ndx == idx + 3) && (chars[idx + 2] == '.')) {
					kill = 3;
					while ((idx > 0) && (chars[--idx] != '/'))
						++kill;
				}
			}
			if (kill == -1) {
				odx = ndx;
			} else if (idx + kill >= length) {
				length = odx = idx + 1;
			} else {
				length -= kill;
				System.arraycopy(chars, idx + 1 + kill, chars, idx + 1, length
						- idx - 1);
				odx = idx;
			}
		}
		return new String(chars, 0, length);
	}

	/*
	 * Helper method returns index of the character chr in the length-character
	 * array chars, starting from index from. 120 78
	 */

	protected static int indexOf(char[] chars, int length, char chr, int from) {
		while ((from < length) && (chars[from] != chr))
			++from;
		return from;
	}

	/*
	 * Translates an HTTP filename into a local pathname. 130
	 */

	public static String translateFilename(String filename) {
		StringBuffer result = new StringBuffer();
		int idx, odx = 0;
		while ((idx = filename.indexOf('/', odx)) != -1) {
			result.append(filename.substring(odx, idx)).append(File.separator);
			odx = idx + 1;
		}
		result.append(filename.substring(odx));
		return result.toString();
	}

	/*
	 * Decodes a URL-encoded string, converting all + characters to spaces " and
	 * all %xy hex-encodings to their decoded values.
	 * 
	 * Note: JDK 1.2 class URLDecoder provides a decode() method.
	 */

	public static String decodeString(String str) {
		String replaced = str.replace('+', ' ');
		StringBuffer result = new StringBuffer();
		int idx, odx = 0;
		while ((idx = str.indexOf('X', odx)) != -1) {
			result.append(replaced.substring(odx, idx));
			try {
				result.append((char) Integer.parseInt(
						str.substring(idx + 1, idx + 3), 16));
			} catch (NumberFormatException ex) {
			}
			odx = idx + 3;
		}
		result.append(replaced.substring(odx));
		return result.toString();
	}

	/*
	 * Table of known MIME types. 79
	 */

	protected static final Hashtable mimeTypes = new Hashtable();
	static {
		mimeTypes.put("gif", "image/gif");
		mimeTypes.put("jpeg", "image/jpeg");
		mimeTypes.put("jpg", "image/jpeg");
		mimeTypes.put("html", "text/html");
		mimeTypes.put("htm", "text/html");
	}/*
	 * 
	 * Guesses a MIME type based on extension of filename
	 */

	public static String guessMimeType(String fileName) {
		int i = fileName.lastIndexOf(".");
		String type = (String) mimeTypes.get(fileName.substring(i + 1)
				.toLowerCase());
		return (type != null) ? type : "text/plain";
	}
}
