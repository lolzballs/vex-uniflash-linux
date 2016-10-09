package org.uniflash;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jssc.SerialPortList;

public class PortFinder {
   private static final String WIN_FTDI_ENUM = "SYSTEM\\CurrentControlSet\\Enum\\FTDIBUS";
   private static final String WIN_USB_ENUM = "SYSTEM\\CurrentControlSet\\Enum\\USB";
   private static final Pattern WIN_USB_ID = Pattern.compile("VID_([0-9a-f]{4}).+PID_([0-9a-f]{4})", 2);

   private static void cleanupExtras(List<Serial> have, List<String> want) {
      Iterator var3 = want.iterator();

      while(var3.hasNext()) {
         String unused = (String)var3.next();
         boolean found = false;
         Iterator var6 = have.iterator();

         while(var6.hasNext()) {
            Serial used = (Serial)var6.next();
            if(unused.equalsIgnoreCase(used.getComPort())) {
               found = true;
               break;
            }
         }

         if(!found) {
            have.add(new HardwareSerial(unused));
         }
      }

   }

   public static List<Serial> defaultPortList() {
      List ports = getPortIdentifiers();
      ArrayList out = new ArrayList(ports.size());
      Iterator var3 = ports.iterator();

      while(var3.hasNext()) {
         String port = (String)var3.next();
         out.add(new HardwareSerial(port));
      }

      return out;
   }

   private static List<Serial> enumLinuxCom(List<String> coms) {
      ArrayList out = new ArrayList(16);
      File[] usbDevices = (new File("/sys/bus/usb/devices")).listFiles();
      File[] var6 = usbDevices;
      int var5 = usbDevices.length;

      for(int var4 = 0; var4 < var5; ++var4) {
         File usbDevice = var6[var4];
         String uvid = readOneLine(new File(usbDevice, "idVendor"));
         String upid = readOneLine(new File(usbDevice, "idProduct"));
         String name = readOneLine(new File(usbDevice, "product"));
         if(uvid != null && upid != null) {
            if(name == null) {
               name = "USB Serial Port";
            }

            int vid;
            int pid;
            try {
               vid = Integer.parseInt(uvid, 16);
               pid = Integer.parseInt(upid, 16);
            } catch (NumberFormatException var21) {
               pid = 0;
               vid = 0;
            }

            String usbId = usbDevice.getName();
            File[] var16;
            int var15 = (var16 = usbDevice.listFiles()).length;

            label55:
            for(int var14 = 0; var14 < var15; ++var14) {
               File devDir = var16[var14];
               if(devDir.isDirectory() && devDir.getName().contains(usbId)) {
                  Iterator var18 = coms.iterator();

                  String port;
                  String pname;
                  File pfile;
                  do {
                     if(!var18.hasNext()) {
                        continue label55;
                     }

                     port = (String)var18.next();
                     pname = (new File(port)).getName();
                     pfile = new File(devDir, "tty");
                  } while(!(new File(devDir, pname)).exists() && !(new File(pfile, pname)).exists());

                  out.add(new USBSerial(name, vid, pid, port));
               }
            }
         }
      }

      cleanupExtras(out, coms);
      return out;
   }

   private static void enumWindowsCom_(String key, List<Serial> out, List<String> coms, String overrideName) {
      String[] keys;
      try {
         keys = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, key);
      } catch (Win32Exception var23) {
         keys = new String[0];
      }

      String[] var8 = keys;
      int var7 = keys.length;

      for(int var6 = 0; var6 < var7; ++var6) {
         String usb = var8[var6];

         try {
            String path = key + "\\" + usb;
            String[] subKeys = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, path);
            String[] var14 = subKeys;
            int var13 = subKeys.length;

            for(int var12 = 0; var12 < var13; ++var12) {
               String sk = var14[var12];
               String name = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, path + "\\" + sk, "FriendlyName");
               int pos = name.lastIndexOf(40);
               if(pos > 0) {
                  name = name.substring(0, pos).trim();
               }

               String com = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, path + "\\" + sk + "\\Device Parameters", "PortName");
               Iterator var19 = coms.iterator();

               while(var19.hasNext()) {
                  String pluggedIn = (String)var19.next();
                  if(pluggedIn.equalsIgnoreCase(com)) {
                     Matcher m = WIN_USB_ID.matcher(usb);
                     if(m.find()) {
                        int vid = Integer.parseInt(m.group(1), 16);
                        int pid = Integer.parseInt(m.group(2), 16);
                        if(overrideName != null) {
                           name = overrideName;
                        }

                        out.add(new USBSerial(name, vid, pid, pluggedIn));
                        break;
                     }
                  }
               }
            }
         } catch (Win32Exception var24) {
            ;
         }
      }

   }

   private static List<Serial> enumWindowsCom(List<String> coms) {
      ArrayList out = new ArrayList(16);
      enumWindowsCom_("SYSTEM\\CurrentControlSet\\Enum\\USB", out, coms, (String)null);
      enumWindowsCom_("SYSTEM\\CurrentControlSet\\Enum\\FTDIBUS", out, coms, "FTDI USB Device");
      cleanupExtras(out, coms);
      return out;
   }

   public static List<Serial> findByID(String id, List<Serial> input) {
      ArrayList out = new ArrayList(16);
      String m = id.toUpperCase();
      Iterator var5 = input.iterator();

      while(true) {
         Serial ser;
         do {
            if(!var5.hasNext()) {
               return out;
            }

            ser = (Serial)var5.next();
         } while(!ser.getID().toUpperCase().contains(m) && !ser.getName().toUpperCase().startsWith(id));

         out.add(ser);
      }
   }

   public static List<String> getPortIdentifiers() {
      String[] e = SerialPortList.getPortNames();
      ArrayList out = new ArrayList(e.length);
      Collections.addAll(out, e);
      return out;
   }

   public static List<Serial> getPortList() {
      List ports = getPortIdentifiers();
      String os = System.getProperty("os.name", "nix").toLowerCase();
      if(os.startsWith("win")) {
         return enumWindowsCom(ports);
      } else if(os.startsWith("mac")) {
         throw new UnsupportedOperationException("No extended serial on OSX yet");
      } else {
         return enumLinuxCom(ports);
      }
   }

   private static String readOneLine(File input) {
      try {
         BufferedReader e = new BufferedReader(new FileReader(input));
         String s = e.readLine();
         e.close();
         return s;
      } catch (IOException var3) {
         return null;
      }
   }

   public static class HardwareSerial implements Serial {
      private final String comPort;

      public HardwareSerial(String comPort) {
         this.comPort = comPort;
      }

      public String getComIdentifier() {
         return this.comPort;
      }

      public String getComPort() {
         return this.comPort;
      }

      public String getID() {
         return this.getComPort();
      }

      private String getType() {
         String comPortLC = this.comPort.toLowerCase();
         return comPortLC.indexOf("usb") < 0 && this.comPort.indexOf("acm") < 0?(this.comPort.indexOf("bt") >= 0?"Bluetooth Serial":"Hardware Serial"):"USB Serial";
      }

      public String getName() {
         return this.getType() + " " + this.comPort;
      }

      public String toString() {
         return this.getComPort() + " (" + this.getType() + ")";
      }
   }

   public interface Serial {
      String getComIdentifier();

      String getComPort();

      String getID();

      String getName();
   }

   public static class USBSerial implements Serial {
      private final String comPort;
      private final String name;
      private final int pid;
      private final int vid;

      public USBSerial(String name, int vid, int pid, String com) {
         this.comPort = com;
         this.name = name;
         this.pid = pid;
         this.vid = vid;
      }

      public String getComIdentifier() {
         return this.comPort;
      }

      public String getComPort() {
         return this.comPort;
      }

      public String getName() {
         return this.name;
      }

      public String getID() {
         return String.format("%04X:%04X", new Object[]{Integer.valueOf(this.vid), Integer.valueOf(this.pid)});
      }

      public String toString() {
         return this.getComPort() + " (" + this.name + " " + this.getID() + ")";
      }
   }
}
