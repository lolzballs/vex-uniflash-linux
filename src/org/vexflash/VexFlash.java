package org.vexflash;

import org.uniflash.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class VexFlash implements FlashUtility {
   public static final int BAUD = 115200;
   private static final int ERASE_PAGES = 150;
   public static final int MODE_DEFAULT = 0;
   public static final int MODE_HYBRID = 1;
   public static final int MODE_FIRMWARE = 2;
   private static final short[] BOOTLOAD = new short[]{201, 54, 184, 71, 37};
   private static final short[] STOP_USER_CODE = new short[]{15, 15, 33, 222, 8, 0, 0, 0, 8, 241, 4};
   private static final short[] SYSINFO = new short[]{201, 54, 184, 71, 33};
   private Parser fileData;
   private int mode;
   private SerialPortIO port;
   private org.stmflash.STMState state;

   private static void askSysInfo(SerialPortIO port) throws SerialException {
      try {
         parityNone(port);
         short[] var4 = SYSINFO;
         int var3 = SYSINFO.length;

         for(int var2 = 0; var2 < var3; ++var2) {
            short e = var4[var2];
            port.write(e & 255);
         }

         port.flush();
      } catch (IOException var5) {
         var5.printStackTrace();
         throw new SerialException("Failed to get system info", var5);
      }
   }

   private static void killUserCode(SerialPortIO port) throws SerialException {
      try {
         paritySTM(port);
         short[] var6 = STOP_USER_CODE;
         int var5 = STOP_USER_CODE.length;

         for(int var4 = 0; var4 < var5; ++var4) {
            short e = var6[var4];
            long now = System.currentTimeMillis();
            port.write(e & 255);
            if(System.currentTimeMillis() - now > 450L) {
               throw new SerialException("Timeout when stopping user code");
            }
         }

         port.flush();
      } catch (IOException var7) {
         throw new SerialException("Failed to stop user code", var7);
      }
   }

   private static void parityNone(SerialPortIO port) throws SerialException {
      try {
         port.flush();
         port.setParams(115200, 0);
         port.purge();
      } catch (IOException var2) {
         throw new SerialException("Failed to set parity=NONE", var2);
      }
   }

   private static void paritySTM(SerialPortIO port) throws SerialException {
      try {
         port.flush();
         port.setParams(115200, 2);
         port.purge();
      } catch (IOException var2) {
         throw new SerialException("Failed to set parity=EVEN", var2);
      }
   }

   private static void resetVexNET(SerialPortIO port) {
      try {
         parityNone(port);
         port.write(20);
         port.flush();
      } catch (Exception var2) {
         ;
      }

   }

   private static void vexInit(SerialPortIO port) throws SerialException {
      try {
         parityNone(port);

         for(int e = 0; e < 5; ++e) {
            short[] var5 = BOOTLOAD;
            int var4 = BOOTLOAD.length;

            for(int var3 = 0; var3 < var4; ++var3) {
               short value = var5[var3];
               port.write(value & 255);
            }

            port.flush();
            Utils.delay(50L);
         }

      } catch (Exception var6) {
         throw new SerialException("Failed to initialize controller", var6);
      }
   }

   private void connect(Indicator output) throws SerialException {
      output.messageBegin("Stopping user code");
      killUserCode(this.port);
      output.messageEnd("done.");
      Utils.delay(50L);
      output.messageBegin("Interrogating VEX system");
      this.getSystemInformation();
      output.messageEnd("done.");
      output.messageBegin("Initializing controller");
      vexInit(this.port);
      output.messageEnd("done.");
      Utils.delay(400L);
      this.stmInit();
   }

   public void end() {
      if(this.fileData != null) {
         this.fileData.close();
      }

      if(this.port != null) {
         this.port.close();
      }

   }

   private void eraseAll(Indicator output) throws SerialException {
      output.messageBegin("Erasing memory");
      this.state.commandER();
      output.messageEnd("done.");
      Utils.delay(100L);
   }

   private void eraseBegin(Indicator output) throws SerialException {
      byte[] pages = new byte[150];

      for(int i = 0; i < 150; ++i) {
         pages[i] = (byte)i;
      }

      output.messageBegin(String.format("Erasing pages 0 to %d", new Object[]{Integer.valueOf(150)}));
      this.state.commandER(pages);
      output.messageEnd("done.");
      Utils.delay(100L);
   }

   public String getExtension() {
      return "bin";
   }

   private void getSystemInformation() throws SerialException {
      try {
         askSysInfo(this.port);
         Utils.readExactly(this.port, 14);
      } catch (IOException var2) {
         throw new SerialException("No response when requesting system info", var2);
      }
   }

   public List<PortFinder.Serial> locateSerial() {
      try {
         return UniFlashConfig.lookup(PortFinder.getPortList(), UniFlashConfig.getProperty("VexFlash.accept", "067B,04D8"));
      } catch (RuntimeException var2) {
         return PortFinder.defaultPortList();
      }
   }

   public void program(Indicator output) throws SerialException {
      this.connect(output);
      if(this.mode != 0 && this.mode != 2) {
         if(this.mode == 1) {
            this.eraseBegin(output);
            this.reflash(output);
            this.restartCode(output);
         }
      } else {
         this.eraseAll(output);
         this.reflash(output);
         if(this.mode != 2) {
            this.restartCode(output);
         }
      }

      resetVexNET(this.port);
   }

   private void reflash(Indicator output) throws SerialException {
      int offset = 0;
      int addr = this.state.getUserCodeAddress();
      int size = this.fileData.length();
      int flashSize = this.state.getFlashSize();
      byte[] buffer = new byte[256];
      if(size >= flashSize) {
         throw new SerialException(String.format("Out of memory!\nCode size: %d KiB\nAvailable: %d KiB", new Object[]{Integer.valueOf(size / 1024), Integer.valueOf(flashSize / 1024)}));
      } else {
         output.begin();

         try {
            int len;
            while(offset < size && (len = this.fileData.read(buffer, buffer.length)) > 0) {
               for(int e = len; e < buffer.length; ++e) {
                  buffer[e] = -1;
               }

               try {
                  this.state.commandWM(addr + offset, buffer);
               } catch (SerialException var9) {
                  Utils.delay(8000L);
                  Utils.eat(this.port);
                  this.connect(output);
                  this.state.commandWM(addr + offset, buffer);
               }

               offset += len;
               output.progress(100 * offset / size);
            }

            output.end();
         } catch (IOException var10) {
            output.end();
            throw new SerialException("Error when flashing code", var10);
         }
      }
   }

   public boolean requiresSerial() {
      return true;
   }

   private void restartCode(Indicator output) throws SerialException {
      output.message("Starting user code");
      this.state.commandGO(this.state.getUserCodeAddress());
      Utils.delay(100L);
   }

   public boolean setup(File file, String[] args, String port) throws SerialException {
      String[] var7 = args;
      int var6 = args.length;

      for(int var5 = 0; var5 < var6; ++var5) {
         String e = var7[var5];
         if(e.equalsIgnoreCase("-clean")) {
            this.mode = 0;
         } else if(e.equalsIgnoreCase("-hybrid")) {
            this.mode = 1;
         } else if(e.equalsIgnoreCase("-firmware")) {
            this.mode = 2;
         } else if(!e.startsWith("-P")) {
            return false;
         }
      }

      try {
         this.fileData = new org.stmflash.BinaryParser(file);
      } catch (IOException var8) {
         throw new SerialException("When opening file " + file.getName());
      }

      this.port = Utils.openSerialPort(port);
      this.port.setTimeout(500L);
      return true;
   }

   private void stmInit() throws SerialException {
      try {
         paritySTM(this.port);
         Utils.eat(this.port);
         this.state = new org.stmflash.STMState(this.port);
         this.state.negotiate();
         this.state.commandGET();
         this.state.commandGID();
      } catch (Exception var2) {
         var2.printStackTrace();
         throw new SerialException("Failed to initialize controller", var2);
      }

      if(this.state.getDevice() == null) {
         throw new SerialException("Unable to determine Cortex type");
      }
   }
}
