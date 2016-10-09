/*package org.uniflash;

import java.io.IOException;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class SerialPortIO implements SerialPortEventListener {
   private final SerialPort port;
   private final Object rxLock = new Object();
   private long timeout;
   private final Object txLock;

   public SerialPortIO(String name) throws IOException {
      this.port = new SerialPort(name);
      this.timeout = 0L;
      this.txLock = new Object();

      try {
         this.port.openPort();
         this.port.setDTR(false);
         this.port.setRTS(false);
         this.port.setFlowControlMode(0);
         this.port.setEventsMask(7);
         this.port.addEventListener(this);
      } catch (SerialPortException var3) {
         if(var3.getExceptionType().equals("Port already opened")) {
            throw new IOException("Port is in use");
         } else {
            throw new IOException("Failed to open port");
         }
      }
   }

   public void close() {
      try {
         this.flush();
         this.port.closePort();
      } catch (SerialPortException var2) {
         ;
      }

   }

   public void flush() {
      while(true) {
         try {
            if(this.port.getOutputBufferBytesCount() > 0) {
               Object var1 = this.txLock;
               synchronized(this.txLock) {
                  this.txLock.wait(30L);
                  continue;
               }
            }
         } catch (Exception var3) {
            ;
         }

         return;
      }
   }

   public String getName() {
      return this.port.getPortName();
   }

   public long getTimeout() {
      return this.timeout;
   }

   public void purge() {
      try {
         this.port.purgePort(8);
      } catch (SerialPortException var2) {
         ;
      }

   }

   public byte read() throws IOException {
      return this.read(1)[0];
   }

   public byte[] read(int length) throws IOException {
      try {
         Object e = this.rxLock;
         synchronized(this.rxLock) {
            int count = this.port.getInputBufferBytesCount();
            byte[] val;
            if(count >= length) {
               val = this.port.readBytes(length);
            } else {
               long future = System.currentTimeMillis() + this.timeout;
               long now = System.currentTimeMillis();

               do {
                  try {
                     this.rxLock.wait(future - now);
                  } catch (InterruptedException var10) {
                     ;
                  }

                  count = this.port.getInputBufferBytesCount();
                  now = System.currentTimeMillis();
               } while(count < length && now < future);

               if(count < length) {
                  throw new IOException("Timeout when reading " + length + " bytes");
               }

               val = this.port.readBytes(length);
            }

            return val;
         }
      } catch (SerialPortException var12) {
         throw new IOException("Error when reading " + length + " bytes");
      }
   }

   public void serialEvent(SerialPortEvent e) {
      Object var2;
      if(!e.isRXCHAR() && !e.isRXFLAG()) {
         if(e.isTXEMPTY()) {
            var2 = this.txLock;
            synchronized(this.txLock) {
               this.txLock.notifyAll();
            }
         }
      } else {
         var2 = this.rxLock;
         synchronized(this.rxLock) {
            this.rxLock.notifyAll();
         }
      }

   }

   public void setDTR(boolean enabled) throws IOException {
      try {
         this.port.setDTR(enabled);
      } catch (SerialPortException var3) {
         throw new IOException("Failed to set DTR = " + enabled);
      }
   }

   public void setRTS(boolean enabled) throws IOException {
      try {
         this.port.setRTS(enabled);
      } catch (SerialPortException var3) {
         throw new IOException("Failed to set RTS = " + enabled);
      }
   }

   public void setParams(int baud, int parity) throws IOException {
      try {
         this.port.setParams(baud, 8, 1, parity, false, false);
      } catch (SerialPortException var4) {
         throw new IOException("Failed to set port parameters");
      }
   }

   public void setTimeout(long timeout) {
      this.timeout = timeout;
   }

   public void write(int data) throws IOException {
      try {
         this.port.writeInt(data);
      } catch (SerialPortException var3) {
         throw new IOException("Error when writing byte");
      }
   }

   public void write(byte[] data) throws IOException {
      try {
         this.port.writeBytes(data);
      } catch (SerialPortException var3) {
         throw new IOException("Error when writing " + data.length + " bytes");
      }
   }

   public void write(String str) throws IOException {
      try {
         this.port.writeString(str);
      } catch (SerialPortException var3) {
         throw new IOException("Error when writing " + str.length() + " characters");
      }
   }
}
*/