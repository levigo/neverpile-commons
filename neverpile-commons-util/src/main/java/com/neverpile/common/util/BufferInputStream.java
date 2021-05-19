package com.neverpile.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * A BufferInputStream is similar to a {@link java.io.ByteArrayInputStream}but
 * differs from the latter in a few important points:
 * <ul>
 * <li>The byte array it reads from may be filled while the stream is being
 * read. More specifically, additional input may be added.
 * <li>The stream is not considered to bet at END_OF_STREAM until it is explicitly told
 * so.
 * <ul>
 * The BufferInputStream is useful only in conjunction with at least two
 * threads: one that is reading data and another one which adds more data or
 * tells the stream that END_OF_STREAM has been reached.
 */
public class BufferInputStream extends InputStream implements Serializable {
  private static final long serialVersionUID = 3978146543239182128L;

  /**
   * A list of buffers which are "queued" for reading
   */
  private final List<byte[]> buffers = new LinkedList<byte[]>();

  private byte[] currentBuffer = null;

  /**
   * A read pointer into the current buffer
   */
  private int readCursor = -1;

  /**
   * A flag indicating whether more data is to be expected or not.
   */
  private boolean noMoreData = false;

  /**
   * Exceptions may be generated by the supplier and piped to the reader by
   * setting this field.
   */
  private IOException exception;

  /**
   * Construct a new BufferInputStream.
   */
  public BufferInputStream() {
    super();
  }

  /**
   * Add a new chunk of data to be read.
   *
   * @param data
   */
  public synchronized void addData(byte data[]) {
    buffers.add(data);
    notifyAll();
  }

  /**
   * Signal the end of available data.
   */
  public synchronized void signalNoMoreData() {
    noMoreData = true;
    notifyAll();
  }

  /*
   * (non-Javadoc)
   *
   * @see java.io.InputStream#read()
   */
  public int read() throws IOException {
    while (null == currentBuffer || readCursor >= currentBuffer.length)
      if (!fetchNextBuffer()) {
        // END_OF_STREAM;
        return -1;
      }

    return currentBuffer[readCursor++] & 0xff;
  }

  /**
   * Make the next buffer the current one. Wait until a buffer becomes available
   * or there is no more data.
   *
   * @return true if there was more data, false otherwise
   * @throws IOException if an exception was detected after the production of
   *                     input data has commenced; see
   *                     {@link #setIOException(IOException)}
   */
  private synchronized boolean fetchNextBuffer() throws IOException {
    // wait for more data to arrive
    while (!noMoreData && buffers.size() == 0 && null == exception)
      try {
        wait();
      } catch (InterruptedException e) {
        // ignore
      }

    if (null != exception)
      throw exception;

    // if there is no more data, tell caller so
    if (buffers.size() == 0)
      return false;

    // pop a buffer from the list, reset read cursor
    currentBuffer = buffers.remove(0);
    readCursor = 0;

    return true;
  }

  /**
   * Set the IOException to be propagated to readers of this stream. This
   * mechanism may be used to propagate exceptions which are detected only after
   * the production of input data has commenced.
   *
   * @param exception
   */
  public synchronized void setIOException(IOException exception) {
    this.exception = exception;
    this.noMoreData = true;
    notifyAll();
  }

  /*
   * (non-Javadoc)
   *
   * @see java.io.InputStream#close()
   */
  public void close() throws IOException {
    super.close();
    noMoreData = true;
    buffers.clear();
    currentBuffer = null;
  }

  /**
   * Fill buffer from the supplied stream. This happens synchronously. If any
   * exception occurs during the operation, the exception is propagated to the
   * stream, but not thrown.
   *
   * @param src
   */
  public void fillFromStream(final InputStream src) {
    fillFromStream(src, true);
  }

  /**
   * Fill buffer from the supplied stream. This happens synchronously. If any
   * exception occurs during the operation, the exception is propagated to the
   * stream, but not thrown.
   *
   * @param src
   */
  public void fillFromStream(final InputStream src, boolean closeSrc) {
    try {
      // copy data from ISRA to the buffer stream.
      int read;
      byte[] buffer = new byte[65536];
      do {
        read = src.read(buffer);
        if (read > 0) {
          byte[] b = new byte[read];
          System.arraycopy(buffer, 0, b, 0, read);
          addData(b);
        }
      } while (read > 0);
    } catch (IOException e) {
      setIOException(e);
    } catch (Exception e) {
      setIOException(new IOException(e.toString()));
    } finally {
      if (closeSrc)
        try {
          src.close();
        } catch (IOException e) {
          // ignore
        }
    }
    signalNoMoreData();
  }
}
