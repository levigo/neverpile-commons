// MultipartInputStream.java
// $Id: MultipartInputStream.java,v 1.8 2000/08/16 21:38:01 ylafon Exp $
// (c) COPYRIGHT MIT and INRIA, 1996.
// Please first read the full copyright statement in file COPYRIGHT.html

package com.neverpile.common.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A class to handle multipart MIME input streams. See RC 1521. This class
 * handles multipart input streams, as defined by the RFC 1521. It prvides a
 * sequential interface to all MIME parts, and for each part it delivers a
 * suitable InputStream for getting its body.
 */

public class MultipartInputStream extends InputStream {
	InputStream in = null;
	byte[] boundary = null;
	byte[] buffer = null;

	private enum State {
		IDLE, IN_PART, END_OF_PART, END_OF_STREAM, CLOSED
	}

	private State state = State.IDLE;

	private final Map<String, String> currentHeaders = new HashMap<String, String>();

	// Skip to next input boundary, set stream at begining of content:
	// Returns true if boundary was found, false otherwise.
	protected boolean skipToBoundary() throws IOException {
		int ch;
		while ((ch = in.read()) != -1) {
			if (ch == '-') {
				if ((ch = in.read()) != '-')
					continue;

				for (int i = 0; i < boundary.length; i++)
					if (((byte) in.read()) != boundary[i])
						continue;


				// check line terminator
				switch (ch = in.read()){
					case '\r' :
						if (in.read() != '\n')
							throw new IOException("Invalid part boundary: no \\n");
						break;

					case '-' :
						// last part
						if (in.read() != '-')
							throw new IOException(
									"Invalid part boundary: missing second end-of-stream dash");
						state = State.END_OF_STREAM;
						return false;

					default :
						throw new IOException(
								"Invalid part boundary: unexpected character following boundary separator: "
										+ ch);
				}

				// read header lines
				currentHeaders.clear();

				String previousFieldName = null;
				String headerLine;
				while ((headerLine = readLine()).length() > 0) {
					if (Character.isWhitespace(headerLine.charAt(0))) {
						if (null == previousFieldName)
							throw new IOException(
									"Invalid part boundary: field value continuation but no current field");
						currentHeaders.put(previousFieldName,
								currentHeaders.get(previousFieldName) + "\r\n"
										+ trimLeadingWhitespace(headerLine));
					}

					int idx = headerLine.indexOf(':');
					if (idx < 0)
						throw new IOException(
								"Invalid part boundary: missing colon in header line");
					String fieldName = headerLine.substring(0, idx);
					String fieldValue = trimLeadingWhitespace(headerLine
							.substring(idx + 1));

					currentHeaders.put(fieldName.trim(), fieldValue);

					previousFieldName = fieldName;
				}

				state = State.IN_PART;
				return true;
			}
		}

		state = State.END_OF_STREAM;
		return false;
	}

	private String trimLeadingWhitespace(String s) {
		int i = 0;
		while (Character.isWhitespace(s.charAt(i)))
			i++;

		if (i > 0)
			return s.substring(i);
		else
			return s;
	}

	private String readLine() throws IOException {
		StringBuilder sb = new StringBuilder(80);
		int ch;
		while ((ch = in.read()) >= 0)
			if (ch == '\r') {
				if (in.read() != '\n')
					throw new IOException(
							"Invalid part boundary: missing \\n in header line");
				break;
			} else
				sb.append((char) ch);

		return sb.toString();
	}

	/**
	 * Read one byte of data from the current part.
	 * 
	 * @return A byte of data, or <strong>-1</strong> if end of file.
	 * @exception IOException If some IO error occured.
	 */

	public int read() throws IOException {
		int ch;
		if (state != State.IN_PART)
			return -1;

		switch (ch = in.read()){
			case '\r' :
				// check for a boundary
				in.mark(boundary.length + 3);
				if (in.read() == '\n' && in.read() == '-' && in.read() == '-') {
					boolean isBoundary = true;
					for (int i = 0; i < boundary.length; i++)
						if (((byte) in.read()) != boundary[i]) {
							isBoundary = false;
							break;
						}

					if (isBoundary) {
						if (in.read() == '-') { // last part?
							if (in.read() != '-')
								throw new IOException(
										"Invalid part boundary: missing second end-of-stream dash");
							state = State.END_OF_STREAM;
						} else
							state = State.END_OF_PART;

						in.reset();
						return -1;
					}
				} else {
					in.reset();
					return ch;
				}

				// not reached
			case -1 :
				state = State.END_OF_STREAM;
				return -1;

			default :
				return ch;
		}
	}

	/**
	 * Read n bytes of data from the current part.
	 * 
	 * @return the number of bytes data, read or <strong>-1</strong> if end of
	 *         file.
	 * @exception IOException If some IO error occured.
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		int got = 0;
		int ch;

		while (got < len) {
			if ((ch = read()) == -1)
				return (got == 0) ? -1 : got;
			b[off + (got++)] = (byte) (ch & 0xFF);
		}
		return got;
	}

	public long skip(long n) throws IOException {
		while ((--n >= 0) && (read() != -1));
		return n;
	}

	public int available() throws IOException {
		return in.available();
	}

	/**
	 * Switch to the next available part of data. One can interrupt the current
	 * part, and use this method to switch to next part before current part was
	 * totally read.
	 * 
	 * @return A boolean <strong>true</strong> if there next partis ready, or
	 *         <strong>false</strong> if this was the last part.
	 */

	public boolean nextInputStream() throws IOException {
		switch (state){
			case IDLE :
			case END_OF_PART :
			case IN_PART :
				return skipToBoundary();

			default :
			case END_OF_STREAM :
				return false;

			case CLOSED :
				throw new IllegalStateException("Multipart stream is closed");
		}
	}

	/**
	 * Construct a new multipart input stream.
	 * 
	 * @param in The initial (multipart) input stream.
	 * @param boundary The input stream MIME boundary.
	 */

	public MultipartInputStream(InputStream in, byte[] boundary) {
		this.in = (in.markSupported() ? in : new BufferedInputStream(in,
				boundary.length + 4));
		this.boundary = boundary;
		this.buffer = new byte[boundary.length];
	}

	@Override
	public void close() throws IOException {
		in.close();
		state = State.CLOSED;
	}
}
