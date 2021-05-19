// ========================================================================
// Copyright 1996-2005 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package com.neverpile.common.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/* ================================================================ */

/**
 * Handle a multipart MIME response.
 *
 * @author Greg Wilkins
 * @author Jim Crossley
 */
public class MultiPartOutputStream extends FilterOutputStream {
  /* ------------------------------------------------------------ */
  private static final String __CRLF = "\015\012";
  private static final String __DASHDASH = "--";

  public static String MULTIPART_MIXED = "multipart/mixed";
  public static String MULTIPART_X_MIXED_REPLACE = "multipart/x-mixed-replace";

  private final String boundary;

  private enum State {
    IDLE, IN_STREAM, CLOSED
  }

  private State state = State.IDLE;

  public MultiPartOutputStream(OutputStream os) throws IOException {
    super(os);
    boundary = "nvgcaa" + System.identityHashCode(this) + Long.toString(System.currentTimeMillis(), 36);
  }

  private byte[] makePartHeader(String contentType) throws UnsupportedEncodingException {
    return (__DASHDASH//
        + boundary //
        + __CRLF //
        + "Content-Type: " + contentType//
        + __CRLF + __CRLF).getBytes("iso8859-1");
  }

  private byte[] makePartTrailer() throws UnsupportedEncodingException {
    return __CRLF.getBytes("iso8859-1");
  }

  private byte[] makeStreamTrailer() throws UnsupportedEncodingException {
    return (__DASHDASH//
        + boundary //
        + __DASHDASH //
        + __CRLF).getBytes("iso8859-1")//
        ;
  }

  @Override
  public void write(int b) throws IOException {
    if (state != State.IN_STREAM)
      throw new IllegalStateException("Nicht in Stream-Modus");

    super.write(b);
  }

  @Override
  public void write(byte[] b) throws IOException {
    if (state != State.IN_STREAM)
      throw new IllegalStateException("Nicht in Stream-Modus");

    super.write(b);
  }

  public void next() throws IOException {
    switch (state){
      case IN_STREAM:
        out.write(makePartTrailer());
        // fall through

      case IDLE:
        out.write(makePartHeader("application/octet-stream"));
        break;

      case CLOSED:
        throw new IllegalStateException("Bereits geschlossen");
    }

    state = State.IN_STREAM;
  }

  @Override
  public void close() throws IOException {
    switch (state){
      case IN_STREAM:
        out.write(makePartTrailer());
        // fall through

      case IDLE:
        out.write(makeStreamTrailer());
        break;

      case CLOSED:
        throw new IllegalStateException("Bereits geschlossen");
    }

    state = State.CLOSED;

    super.close();
  }

  public void append(InputStream is) throws IOException {
    next();
    StreamUtil.copyStream(is, this);
  }

  public String getBoundary() {
    return boundary;
  }
}
