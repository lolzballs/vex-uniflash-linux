package org.mapleflash;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DFUDevice {
   public static final Pattern DFU_REGEX = Pattern.compile("^.+0x([0-9a-f]{4}):0x([0-9a-f]{4}).+alt=([0-9]+).+name=\"(.+?)\".*$", 2);
   private final String name;
   private final int pid;
   private final int vid;
   private final int alt;

   public static DFUDevice fromString(String line) {
      Matcher m = DFU_REGEX.matcher(line);
      return m.matches()?new DFUDevice(m.group(4).trim(), Integer.parseInt(m.group(1).toUpperCase(), 16), Integer.parseInt(m.group(2).toUpperCase(), 16), Integer.parseInt(m.group(3))):null;
   }

   public DFUDevice(String name, int vid, int pid, int alt) {
      this.alt = alt;
      this.name = name;
      this.pid = pid;
      this.vid = vid;
   }

   public int getAlt() {
      return this.alt;
   }

   public String getName() {
      return this.name;
   }

   public int getVID() {
      return this.vid;
   }

   public int getPID() {
      return this.pid;
   }

   public String getUSBID() {
      return String.format("%04X:%04X", new Object[]{Integer.valueOf(this.vid), Integer.valueOf(this.pid)});
   }

   public String toString() {
      return "DFU device " + this.name + " at " + this.getUSBID() + " (alt " + this.alt + ")";
   }
}
