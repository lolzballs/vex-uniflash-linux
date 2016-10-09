package org.vexflash;

import java.io.IOException;
import org.uniflash.SerialException;
import org.uniflash.SerialPortIO;
import org.uniflash.Utils;
import org.vexflash.STMDevice;

public class STMState {
   private static final int RETRIES = 5;
   private int bootloaderVersion;
   private int cmdER;
   private int cmdGID;
   private int cmdGO;
   private int cmdGVR;
   private int cmdRD;
   private int cmdRP;
   private int cmdUR;
   private int cmdUW;
   private int cmdWM;
   private int cmdWP;
   private STMDevice device = null;
   private byte option1;
   private byte option2;
   private SerialPortIO port;
   private int version;

   public STMState(SerialPortIO port) {
      this.port = port;
   }

   public void command(int command) throws SerialException {
      try {
         this.port.write(command & 255);
         this.port.write(~command & 255);
         this.verify();
      } catch (IOException var3) {
         throw new SerialException("Error sending command " + command, var3);
      }
   }

   public void commandER() throws SerialException {
      this.command(this.cmdER);
      this.command(255);
   }

   public void commandER(byte[] pages) throws SerialException {
      int len = pages.length;
      this.command(this.cmdER);

      try {
         this.port.write(len - 1);
         this.port.write(pages);
         this.port.write(Utils.checksum(len - 1, pages, 0, len));
      } catch (IOException var4) {
         throw new SerialException("Failed to erase memory", var4);
      }

      this.verify();
   }

   public void commandGET() throws SerialException {
      this.command(0);

      byte[] data;
      try {
         int e = Utils.readOne(this.port) + 1;
         data = Utils.readExactly(this.port, e);
      } catch (IOException var3) {
         throw new SerialException("Failed to read bootloader command set", var3);
      }

      this.bootloaderVersion = data[0] & 255;
      this.cmdGVR = data[2] & 255;
      this.cmdGID = data[3] & 255;
      this.cmdRD = data[4] & 255;
      this.cmdGO = data[5] & 255;
      this.cmdWM = data[6] & 255;
      this.cmdER = data[7] & 255;
      this.cmdWP = data[8] & 255;
      this.cmdUW = data[9] & 255;
      this.cmdRP = data[10] & 255;
      this.cmdUR = data[11] & 255;
      this.verify();
   }

   public void commandGID() throws SerialException {
      STMDevice newDevice = null;
      this.command(this.cmdGID);

      try {
         int e = Utils.readOne(this.port) + 1;
         byte[] verData = Utils.readExactly(this.port, e);
         if(e != 2) {
            throw new SerialException("Unsupported device - " + e + " PID bytes");
         }

         short pid = (short)((verData[0] & 255) << 8 | verData[1] & 255);
         STMDevice[] var8 = STMDevice.STM_DEVICES;
         int var7 = STMDevice.STM_DEVICES.length;

         for(int var6 = 0; var6 < var7; ++var6) {
            STMDevice dev = var8[var6];
            if(dev.getID() == pid) {
               newDevice = dev;
               break;
            }
         }

         if(newDevice == null) {
            throw new SerialException(String.format("Unsupported device - PID %4X", new Object[]{Integer.valueOf(pid & '\uffff')}));
         }

         this.device = newDevice;
      } catch (IOException var9) {
         throw new SerialException("Failed to read device information", var9);
      }

      this.verify();
   }

   public void commandGO(int address) throws SerialException {
      this.command(this.cmdGO);

      try {
         this.port.write(Utils.memAddress(address));
      } catch (IOException var3) {
         throw new SerialException("Error when starting execution", var3);
      }
   }

   public void commandGVR() throws SerialException {
      this.command(this.cmdGVR);

      try {
         byte[] e = Utils.readExactly(this.port, 3);
         this.version = e[0] & 255;
         this.option1 = e[1];
         this.option2 = e[2];
      } catch (IOException var2) {
         throw new SerialException("Error when retrieving version and options", var2);
      }

      this.verify();
   }

   public byte[] commandRD(int start, int length) throws SerialException {
      this.command(this.cmdRD);

      try {
         int e = length - 1;
         this.port.write(Utils.memAddress(start));
         this.verify();
         this.port.write(e & 255);
         this.port.write(~e & 255);
         this.verify();
         return Utils.readExactly(this.port, length);
      } catch (IOException var4) {
         throw new SerialException("Error when reading memory", var4);
      }
   }

   public void commandRP() throws SerialException {
      this.command(this.cmdRP);
      this.verify();
   }

   public void commandUR() throws SerialException {
      this.command(this.cmdUR);
      this.verify();
   }

   public void commandUW() throws SerialException {
      this.command(this.cmdUW);
      this.verify();
   }

   public void commandWM(int start, byte[] data) throws SerialException {
      int len = data.length;
      if(len >= 4 && len <= 256 && len % 4 == 0) {
         this.command(this.cmdWM);

         try {
            this.port.write(Utils.memAddress(start));
            this.verify();
            this.port.write(len - 1);
            this.port.write(data);
            this.port.write(Utils.checksum(len - 1, data, 0, len));
            this.verify();
         } catch (IOException var5) {
            throw new SerialException("Error when programming memory", var5);
         }
      } else {
         throw new IllegalArgumentException("Must be qword-aligned data, 4-256 bytes");
      }
   }

   public void commandWP(byte[] sectors) throws SerialException {
      this.command(this.cmdWP);

      try {
         int e = sectors.length;
         this.port.write(e - 1);
         this.port.write(sectors);
         this.port.write(Utils.checksum(e - 1, sectors, 0, e));
      } catch (IOException var3) {
         throw new SerialException("Error when write-protecting chip", var3);
      }

      this.verify();
   }

   public int getBootloaderVersion() {
      return this.bootloaderVersion;
   }

   public STMDevice getDevice() {
      return this.device;
   }

   public int getFlashSize() {
      if(this.device == null) {
         throw new IllegalStateException("Must use commandGID() before using this method");
      } else {
         return this.device.getFlashSize();
      }
   }

   public int getRAMSize() {
      if(this.device == null) {
         throw new IllegalStateException("Must use commandGID() before using this method");
      } else {
         return this.device.getRAMSize();
      }
   }

   public int getUserCodeAddress() {
      if(this.device == null) {
         throw new IllegalStateException("Must use commandGID() before using this method");
      } else {
         return this.getDevice().getFlashStart();
      }
   }

   public byte getOption1() {
      return this.option1;
   }

   public byte getOption2() {
      return this.option2;
   }

   public int getVersion() {
      return this.version;
   }

   public boolean negotiate() throws SerialException {
      boolean verified = false;

      try {
         this.port.write(127);
         this.port.flush();
         this.verify();
         verified = true;
      } catch (SerialException var3) {
         ;
      } catch (IOException var4) {
         throw new SerialException("I/O error during negotiation", var4);
      }

      return verified;
   }

   public void setDevice(STMDevice device) {
      this.device = device;
   }

   public void verify() throws SerialException {
      int result = -1;

      for(int i = 0; i < 5 && result == -1; ++i) {
         try {
            result = Utils.readOne(this.port);
         } catch (IOException var4) {
            result = -1;
         }
      }

      if(result == -1) {
         throw new SerialException("Connection lost to controller (EOF)");
      } else if(result != 121) {
         throw new SerialException(String.format("Controller non-acknowledge: %2X", new Object[]{Integer.valueOf(result & 255)}));
      }
   }
}
