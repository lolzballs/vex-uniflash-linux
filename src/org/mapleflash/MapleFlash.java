package org.mapleflash;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import org.mapleflash.DFUDevice;
import org.uniflash.FlashUtility;
import org.uniflash.Indicator;
import org.uniflash.PortFinder;
import org.uniflash.SerialException;
import org.uniflash.SerialPortIO;
import org.uniflash.Utils;

public class MapleFlash implements FlashUtility {
   public static final int PID = 3;
   public static final int VID = 7855;
   private File file;
   private String method;
   private String pathToDFU;
   private SerialPortIO port;

   private static void autoReset(SerialPortIO port) {
      try {
         port.setDTR(true);
         Utils.delay(50L);
         port.setDTR(false);
         port.setRTS(true);
         port.setDTR(true);
         Utils.delay(50L);
         port.setDTR(false);
         Utils.delay(50L);
         port.write("1EAF");
         port.flush();
         Utils.delay(50L);
      } catch (IOException var5) {
         ;
      } finally {
         port.close();
      }

   }

   private void dfuUpload(DFUDevice dev, Indicator output) throws SerialException {
      try {
         output.message("Calling dfu-util");
         int e = Utils.execPrint(true, new String[]{this.pathToDFU, "-a", Integer.toString(dev.getAlt()), "-R", "-d", dev.getUSBID(), "-D", "\"" + this.file.getPath() + "\""});
         output.message("DFU process exited with code " + e);
      } catch (IOException var4) {
         throw new SerialException("Check path to \"" + this.pathToDFU + "\".", var4);
      }
   }

   public void end() {
   }

   private String execDFU(String args) {
      try {
         Process p = Runtime.getRuntime().exec(this.pathToDFU + " " + args);
         if(p != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder out = new StringBuilder(1024);

            String str;
            while((str = br.readLine()) != null) {
               String nstr = str.trim().toLowerCase();
               if(nstr.length() > 0 && !nstr.startsWith("dfu-util") && !nstr.startsWith("operation not") && !nstr.startsWith("no dfu") && !nstr.contains("free software")) {
                  out.append(str);
                  out.append('\n');
               }
            }

            br.close();
            return out.toString();
         }
      } catch (Exception var7) {
         ;
      }

      return null;
   }

   private DFUDevice[] getDeviceList() {
      String text = this.execDFU("-l");
      if(text != null) {
         String[] lines = text.split("\n");
         LinkedList out = new LinkedList();
         String[] var7 = lines;
         int var6 = lines.length;

         for(int var5 = 0; var5 < var6; ++var5) {
            String ret = var7[var5];
            if(ret.trim().length() > 0) {
               DFUDevice dev = DFUDevice.fromString(ret);
               if(dev != null) {
                  out.add(dev);
               }
            }
         }

         DFUDevice[] var9 = new DFUDevice[out.size()];
         return (DFUDevice[])out.toArray(var9);
      } else {
         return new DFUDevice[0];
      }
   }

   private boolean getDFUPath() {
      String os = System.getProperty("os.name", "nix").toLowerCase().trim();
      if(os.contains("win")) {
         this.pathToDFU = "dfu-util.exe";
      } else if(os.contains("mac")) {
         this.pathToDFU = "dfu-util-mac";
      } else {
         this.pathToDFU = "dfu-util";
      }

      return this.execDFU("-l") != null;
   }

   public String getExtension() {
      return "bin";
   }

   public List<PortFinder.Serial> locateSerial() {
      try {
         return PortFinder.findByID(Integer.toString(7855, 16), PortFinder.getPortList());
      } catch (RuntimeException var2) {
         return PortFinder.defaultPortList();
      }
   }

   private DFUDevice lookForCorrect(DFUDevice[] list) {
      DFUDevice wanted = null;
      DFUDevice[] var6 = list;
      int var5 = list.length;

      for(int var4 = 0; var4 < var5; ++var4) {
         DFUDevice test = var6[var4];
         if(test.getPID() == 3 && test.getVID() == 7855) {
            if(wanted == null) {
               wanted = test;
            }

            if(test.getName().contains(this.method)) {
               wanted = test;
            }
         }
      }

      return wanted;
   }

   public void program(Indicator output) throws SerialException {
      if(!this.getDFUPath()) {
         throw new SerialException("No DFU program found. Is " + this.pathToDFU + " installed?");
      } else {
         DFUDevice initial = this.lookForCorrect(this.getDeviceList());
         if(initial != null) {
            this.dfuUpload(initial, output);
         } else {
            if(this.port == null) {
               throw new SerialException("No Maple bootloader or serial ports found.");
            }

            boolean done = false;
            output.messageBegin("Auto-reset Maple on " + this.port.getName());
            autoReset(this.port);
            output.messageEnd("done.");

            for(int i = 0; i < 30; ++i) {
               DFUDevice dev = this.lookForCorrect(this.getDeviceList());
               if(dev != null) {
                  this.dfuUpload(dev, output);
                  done = true;
                  break;
               }

               Utils.delay(100L);
            }

            if(!done) {
               throw new SerialException("No response from Maple after auto-reset.");
            }
         }

      }
   }

   public boolean requiresSerial() {
      return false;
   }

   public boolean setup(File file, String[] args, String port) throws SerialException {
      boolean flash = true;
      String[] var8 = args;
      int var7 = args.length;

      for(int var6 = 0; var6 < var7; ++var6) {
         String arg = var8[var6];
         if(arg.equals("-flash")) {
            flash = true;
         } else if(arg.equals("-ram")) {
            flash = false;
         } else if(!arg.startsWith("-P")) {
            return false;
         }
      }

      if(flash) {
         this.method = "FLASH";
      } else {
         this.method = "RAM";
      }

      this.file = file;
      if(port == null) {
         this.port = null;
      } else {
         this.port = Utils.openSerialPort(port);
      }

      return true;
   }
}
