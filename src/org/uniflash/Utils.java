package org.uniflash;

import java.io.IOException;
import java.io.InputStream;
import org.uniflash.SerialException;
import org.uniflash.SerialPortIO;

public final class Utils {
   public static int checksum(int current, byte[] data, int offset, int length) {
      for(int i = offset; i < offset + length; ++i) {
         current ^= data[i] & 255;
      }

      return current & 255;
   }

   public static void delay(long time) {
      try {
         Thread.sleep(time);
      } catch (InterruptedException var3) {
         ;
      }

   }

   public static void eat(SerialPortIO port) {
      port.purge();
   }

   public static int execPrint(boolean out, String... command) throws IOException {
      ProcessBuilder builder = new ProcessBuilder(command);
      builder.redirectErrorStream(true);
      Process p = builder.start();
      InputStream is = p.getInputStream();

      int c;
      while((c = is.read()) >= 0) {
         if(out) {
            System.out.write(c);
            System.out.flush();
         }
      }

      is.close();
      return p.exitValue();
   }

   public static byte[] memAddress(int address) {
      byte[] data = new byte[]{(byte)(address >> 24 & 255), (byte)(address >> 16 & 255), (byte)(address >> 8 & 255), (byte)(address & 255), 0};
      data[4] = (byte)checksum(0, data, 0, 4);
      return data;
   }

   public static SerialPortIO openSerialPort(String id) throws SerialException {
      try {
         return new SerialPortIO(id);
      } catch (IOException var2) {
         throw new SerialException(var2.getMessage(), var2);
      }
   }

   public static byte[] readExactly(SerialPortIO port, int length) throws IOException {
      return port.read(length);
   }

   public static int readOne(SerialPortIO port) throws IOException {
      return port.read();
   }
}
