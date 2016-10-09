package org.mapleflash;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.stmflash.BinaryParser;
import org.stmflash.STMState;
import org.uniflash.FlashUtility;
import org.uniflash.Indicator;
import org.uniflash.Parser;
import org.uniflash.PortFinder;
import org.uniflash.SerialException;
import org.uniflash.SerialPortIO;
import org.uniflash.Utils;

public class MapleDVFlash implements FlashUtility {
   public static final int BAUD = 115200;
   public static final int BL_SIZE = 8192;
   public static final int PID = 3;
   public static final int VID = 7855;
   private Parser fileData;
   private SerialPortIO port;
   private STMState state;

   private static void paritySTM(SerialPortIO port) throws SerialException {
      try {
         port.flush();
         port.setParams(115200, 2);
         port.setDTR(false);
         port.setRTS(false);
         port.purge();
      } catch (IOException var2) {
         throw new SerialException("Failed to set parity=EVEN", var2);
      }
   }

   private void connect(Indicator output) throws SerialException {
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

   private void eraseBegin(Indicator output, int count) throws SerialException {
      if(count >= 256) {
         this.eraseAll(output);
      } else {
         byte[] pages = new byte[count];

         for(int i = 0; i < count; ++i) {
            pages[i] = (byte)i;
         }

         output.messageBegin(String.format("Erasing pages 0 to %d", new Object[]{Integer.valueOf(count)}));
         this.state.commandER(pages);
         output.messageEnd("done.");
         Utils.delay(100L);
      }

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

   public void program(Indicator output) throws SerialException {
      this.connect(output);
      this.port.setTimeout(2000L);
      this.eraseBegin(output, Math.min(256, 1 + (this.fileData.length() + 8192) / this.state.getDevice().getPageSize()));
      this.port.setTimeout(500L);
      this.reflash(output);
      this.restartCode(output);
   }

   private void reflash(Indicator output) throws SerialException {
      int offset = 0;
      int addr = this.state.getUserCodeAddress() + 8192;
      int size = this.fileData.length();
      int flashSize = this.state.getFlashSize() - 8192;
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

               this.state.commandWM(addr + offset, buffer);
               offset += len;
               output.progress(100 * offset / size);
            }

            output.end();
         } catch (IOException var9) {
            output.end();
            throw new SerialException("Error when flashing code", var9);
         }
      }
   }

   public boolean requiresSerial() {
      return true;
   }

   private void restartCode(Indicator output) throws SerialException {
      output.message("Starting user code");
      this.state.commandGO(this.state.getUserCodeAddress() + 8192);
   }

   public boolean setup(File file, String[] args, String port) throws SerialException {
      String[] var7 = args;
      int var6 = args.length;

      for(int var5 = 0; var5 < var6; ++var5) {
         String e = var7[var5];
         if(!e.startsWith("-P")) {
            return false;
         }
      }

      try {
         this.fileData = new BinaryParser(file);
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
         this.state = new STMState(this.port);
         this.state.negotiate();
         this.state.commandGET();
         this.state.commandGID();
      } catch (Exception var2) {
         throw new SerialException("Failed to initialize chip", var2);
      }

      if(this.state.getDevice() == null) {
         throw new SerialException("Unable to determine chip type");
      }
   }
}
