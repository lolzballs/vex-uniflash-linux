package org.arduinoflash;

import java.io.File;
import java.io.IOException;
import org.uniflash.FlashUtility;
import org.uniflash.Indicator;
import org.uniflash.SerialException;
import org.uniflash.Utils;

public abstract class AbstractAvrFlash implements FlashUtility {
   private String conf;
   private File file;
   private String part;
   private String port;
   protected String programmer;

   private static String stripQuotes(String in) {
      int ln = in.length() - 1;
      return in.length() <= 1 || (in.charAt(0) != 34 || in.charAt(ln) != 34) && (in.charAt(0) != 39 || in.charAt(ln) != 39)?in:in.substring(1, ln);
   }

   private void avrdudeUpload(Indicator output) throws SerialException {
      try {
         output.message("Calling avrdude");
         String e = "";
         if(this.conf != null) {
            e = "-C\"" + this.conf + "\"";
         }

         int ret = Utils.execPrint(true, new String[]{"avrdude", e, "-p" + this.part, "-c" + this.getProgrammer(), "-P" + this.port, "-U", "flash:w:\"" + this.file.getPath() + "\":i"});
         output.message("avrdude exited with code " + ret);
      } catch (IOException var4) {
         throw new SerialException("avrdude not found; check system configuration.", var4);
      }
   }

   public void end() {
   }

   public String getExtension() {
      return "hex";
   }

   protected abstract String getProgrammer();

   public void program(Indicator output) throws SerialException {
      this.avrdudeUpload(output);
   }

   public boolean requiresSerial() {
      return true;
   }

   public boolean setup(File file, String[] args, String port) throws SerialException {
      this.part = "m328p";
      this.programmer = "avrispv2";
      this.conf = null;
      String[] var7 = args;
      int var6 = args.length;

      for(int var5 = 0; var5 < var6; ++var5) {
         String arg = var7[var5];
         if(arg.length() > 2) {
            if(arg.startsWith("-C")) {
               this.conf = stripQuotes(arg.substring(2));
            } else if(arg.startsWith("-p")) {
               this.part = stripQuotes(arg.substring(2));
            } else if(arg.startsWith("-c")) {
               this.programmer = stripQuotes(arg.substring(2));
            } else if(!arg.startsWith("-P")) {
               return false;
            }
         } else if(!arg.equals("-P")) {
            return false;
         }
      }

      this.file = file;
      this.port = port;
      return true;
   }
}
