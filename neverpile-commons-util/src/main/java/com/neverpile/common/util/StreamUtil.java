package com.neverpile.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A bunch of stream-related utility methods.
 */
public class StreamUtil {
  private StreamUtil() {
    // don't instantiate me
  }

  /**
   * Copy a stream from input to output
   *
   * @param is
   * @param os
   * @throws IOException
   */
  public static void copyStream(InputStream is, OutputStream os)
      throws IOException {
    int read = 0;
    byte[] buffer = new byte[4096];
    while ((read = is.read(buffer)) > 0)
      os.write(buffer, 0, read);
  }

  /**
   * Copy a stream from input to output
   *
   * @param is
   * @param os
   * @throws IOException
   */
  public static void copyAndClose(InputStream is, OutputStream os)
      throws IOException {
    try {
      int read = 0;
      byte[] buffer = new byte[4096];
      while ((read = is.read(buffer)) > 0)
        os.write(buffer, 0, read);
    } finally {
      try {
        os.close();
      } finally {
        is.close();
      }
    }
  }

  /**
   * Discard the contents of a stream
   *
   * @param is
   * @throws IOException
   */
  public static void discardStream(InputStream is) throws IOException {
    byte[] buffer = new byte[4096];
    while (is.read(buffer) > 0)
      // go on
      ;
  }
}
