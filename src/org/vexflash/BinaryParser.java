package org.vexflash;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.uniflash.Parser;

public class BinaryParser implements Parser {
   private final InputStream is;
   private final int len;

   public BinaryParser(File file) throws IOException {
      this.is = new BufferedInputStream(new FileInputStream(file), 1024);
      this.len = (int)file.length();
   }

   public void close() {
      try {
         this.is.close();
      } catch (IOException var2) {
         ;
      }

   }

   public int length() {
      return this.len;
   }

   public int read(byte[] output, int length) throws IOException {
      int offset;
      int read;
      for(offset = 0; offset < length && (read = this.is.read(output, offset, length - offset)) > 0; offset += read) {
         ;
      }

      return offset;
   }
}
