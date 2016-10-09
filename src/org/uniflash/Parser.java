package org.uniflash;

import java.io.IOException;

public interface Parser {
   void close();

   int length();

   int read(byte[] var1, int var2) throws IOException;
}
