package org.uniflash;

import org.arduinoflash.ArduinoFlash;
import org.arduinoflash.AvrFlash;
import org.mapleflash.MapleDVFlash;
import org.mapleflash.MapleFlash;
import org.vexflash.VexFlash;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class Main {
   private String forcePort;

   public static void main(String[] args) {
      (new Main()).start(args);
   }

   public File checkFile(String in) {
      try {
         File e = (new File(in)).getCanonicalFile();
         if(e.canRead()) {
            return e;
         }

         System.err.println("Error: Cannot open input file \"" + in + "\".");
      } catch (IOException var3) {
         System.err.println("Error: Cannot open input file \"" + in + "\".");
      }

      return null;
   }

   private void runFlash(FlashUtility u, File file, String[] args) throws SerialException {
      String p = null;
      CommandLineIndicator output = new CommandLineIndicator();
      if(this.forcePort == null) {
         List accept = u.locateSerial();
         PortFinder.Serial e;
         PortFinder.Serial port;
         Iterator var9;
         if(accept.size() > 0) {
            e = (PortFinder.Serial)accept.get(accept.size() - 1);
            if(accept.size() <= 1) {
               System.out.println("Using serial port " + e);
            } else {
               System.out.println("Warning: multiple serial ports appear possible:");
               var9 = accept.iterator();

               while(var9.hasNext()) {
                  port = (PortFinder.Serial)var9.next();
                  System.out.println(" * " + port);
               }

               System.out.println("Using " + e + "!");
            }

            p = e.getComIdentifier();
         } else {
            if(u.requiresSerial()) {
               System.out.println("Error: No matching ports found for this board (" + u.getClass().getSimpleName() + "); currently connected:");

               try {
                  Iterator port1 = PortFinder.getPortList().iterator();

                  while(port1.hasNext()) {
                     e = (PortFinder.Serial)port1.next();
                     System.out.println(" * " + e);
                  }
               } catch (RuntimeException var13) {
                  var9 = PortFinder.defaultPortList().iterator();

                  while(var9.hasNext()) {
                     port = (PortFinder.Serial)var9.next();
                     System.out.println(" * " + port.getComPort());
                  }
               }

               return;
            }

            System.out.println("Warning: no serial port found, attempting upload anyways!");
         }
      } else {
         p = this.forcePort;
      }

      if(u.setup(file, args, p)) {
         try {
            u.program(output);
         } finally {
            u.end();
         }
      } else {
         this.usage();
      }

   }

   public void start(String[] args) {
      System.out.println("UniFlash v0.7 by Stephen Carlson");
      if(args.length < 2) {
         this.usage();
      } else {
         String which = args[0];
         File file = this.checkFile(args[1]);
         String[] newArgs = new String[args.length - 2];
         if(newArgs.length > 0) {
            System.arraycopy(args, 2, newArgs, 0, newArgs.length);
         }

         this.forcePort = null;
         String[] var8 = newArgs;
         int var7 = newArgs.length;

         for(int var6 = 0; var6 < var7; ++var6) {
            String e = var8[var6];
            if(e.startsWith("-P")) {
               if(e.length() > 2) {
                  this.forcePort = e.substring(2);
               } else {
                  this.forcePort = null;
               }
            }
         }

         if(file != null) {
            try {
               if(which.equals("arduino")) {
                  this.runFlash(new ArduinoFlash(), file, newArgs);
               } else if(which.equals("avr")) {
                  this.runFlash(new AvrFlash(), file, newArgs);
               } else if(which.equals("maple")) {
                  this.runFlash(new MapleFlash(), file, newArgs);
               } else if(which.equals("mapledv")) {
                  this.runFlash(new MapleDVFlash(), file, newArgs);
               } else if(which.equals("vex")) {
                  this.runFlash(new VexFlash(), file, newArgs);
               } else {
                  this.usage();
               }
            } catch (SerialException var9) {
               System.err.println("Error: " + var9.getMessage());
            }
         }
      }

   }

   public void usage() {
      System.out.println("Usage: java -jar UniFlash.jar board filename [-P\"port\"] [args]");
      System.out.println();
      System.out.println("    board:     Board type to flash");
      System.out.println("               Supported boards: arduino avr maple mapledv vex");
      System.out.println("    filename:  Input file to flash");
      System.out.println("               arduino and avr support HEX, others use BIN");
      System.out.println("    -Pport:    Force select a port, disregarding auto-detect");
      System.out.println();
      System.out.println("Options specific to target arduino:");
      System.out.println("    -C\"path\"   Provide path to avrdude configuration");
      System.out.println("    -pmcu      Microcontroller target to flash [default m328p]");
      System.out.println();
      System.out.println("Options specific to target avr:");
      System.out.println("    -C\"path\"   Provide path to avrdude configuration");
      System.out.println("    -pmcu      Microcontroller target to flash [default m328p]");
      System.out.println("    -cmodel    Programmer to use [default avrispv2]");
      System.out.println();
      System.out.println("Options specific to target maple:");
      System.out.println("    -flash     Write program to FLASH [default]");
      System.out.println("    -ram       Write program to RAM");
      System.out.println("               Program must be linked using different script!");
      System.out.println();
      System.out.println("Options specific to target mapledv:");
      System.out.println();
      System.out.println("Options specific to target vex:");
      System.out.println("    -clean     Overwrite all memory [default]");
      System.out.println("    -hybrid    Overwrite firmware section only");
      System.out.println("               Program must be linked using different script!");
      System.out.println("    -user      Overwrite user section only");
      System.out.println();
   }
}
