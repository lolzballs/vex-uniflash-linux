package jssc;

import java.lang.reflect.Method;
import jssc.SerialNativeInterface;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

public class SerialPort {
   private SerialNativeInterface serialInterface;
   private SerialPortEventListener eventListener;
   private long portHandle;
   private String portName;
   private boolean portOpened = false;
   private boolean maskAssigned = false;
   private boolean eventListenerAdded = false;
   private Method methodErrorOccurred = null;
   public static final int BAUDRATE_110 = 110;
   public static final int BAUDRATE_300 = 300;
   public static final int BAUDRATE_600 = 600;
   public static final int BAUDRATE_1200 = 1200;
   public static final int BAUDRATE_4800 = 4800;
   public static final int BAUDRATE_9600 = 9600;
   public static final int BAUDRATE_14400 = 14400;
   public static final int BAUDRATE_19200 = 19200;
   public static final int BAUDRATE_38400 = 38400;
   public static final int BAUDRATE_57600 = 57600;
   public static final int BAUDRATE_115200 = 115200;
   public static final int BAUDRATE_128000 = 128000;
   public static final int BAUDRATE_256000 = 256000;
   public static final int DATABITS_5 = 5;
   public static final int DATABITS_6 = 6;
   public static final int DATABITS_7 = 7;
   public static final int DATABITS_8 = 8;
   public static final int STOPBITS_1 = 1;
   public static final int STOPBITS_2 = 2;
   public static final int STOPBITS_1_5 = 3;
   public static final int PARITY_NONE = 0;
   public static final int PARITY_ODD = 1;
   public static final int PARITY_EVEN = 2;
   public static final int PARITY_MARK = 3;
   public static final int PARITY_SPACE = 4;
   public static final int PURGE_RXABORT = 2;
   public static final int PURGE_RXCLEAR = 8;
   public static final int PURGE_TXABORT = 1;
   public static final int PURGE_TXCLEAR = 4;
   public static final int MASK_RXCHAR = 1;
   public static final int MASK_RXFLAG = 2;
   public static final int MASK_TXEMPTY = 4;
   public static final int MASK_CTS = 8;
   public static final int MASK_DSR = 16;
   public static final int MASK_RLSD = 32;
   public static final int MASK_BREAK = 64;
   public static final int MASK_ERR = 128;
   public static final int MASK_RING = 256;
   public static final int FLOWCONTROL_NONE = 0;
   public static final int FLOWCONTROL_RTSCTS_IN = 1;
   public static final int FLOWCONTROL_RTSCTS_OUT = 2;
   public static final int FLOWCONTROL_XONXOFF_IN = 4;
   public static final int FLOWCONTROL_XONXOFF_OUT = 8;
   public static final int ERROR_FRAME = 8;
   public static final int ERROR_OVERRUN = 2;
   public static final int ERROR_PARITY = 4;
   private static final int PARAMS_FLAG_IGNPAR = 1;
   private static final int PARAMS_FLAG_PARMRK = 2;
   private int linuxMask;
   private EventThread eventThread;

   public SerialPort(String portName) {
      this.portName = portName;
      this.serialInterface = new SerialNativeInterface();
   }

   public String getPortName() {
      return this.portName;
   }

   public boolean isOpened() {
      return this.portOpened;
   }

   public boolean openPort() throws SerialPortException {
      if(this.portOpened) {
         throw new SerialPortException(this.portName, "openPort()", "Port already opened");
      } else if(this.portName == null) {
         throw new SerialPortException(this.portName, "openPort()", "Null not permitted");
      } else {
         boolean useTIOCEXCL = System.getProperty("JSSC_NO_TIOCEXCL") == null && System.getProperty("JSSC_NO_TIOCEXCL".toLowerCase()) == null;
         this.portHandle = this.serialInterface.openPort(this.portName, useTIOCEXCL);
         if(this.portHandle == -1L) {
            throw new SerialPortException(this.portName, "openPort()", "Port busy");
         } else if(this.portHandle == -2L) {
            throw new SerialPortException(this.portName, "openPort()", "Port not found");
         } else if(this.portHandle == -3L) {
            throw new SerialPortException(this.portName, "openPort()", "Permission denied");
         } else if(this.portHandle == -4L) {
            throw new SerialPortException(this.portName, "openPort()", "Incorrect serial port");
         } else {
            this.portOpened = true;
            return true;
         }
      }
   }

   public boolean setParams(int baudRate, int dataBits, int stopBits, int parity) throws SerialPortException {
      return this.setParams(baudRate, dataBits, stopBits, parity, true, true);
   }

   public boolean setParams(int baudRate, int dataBits, int stopBits, int parity, boolean setRTS, boolean setDTR) throws SerialPortException {
      this.checkPortOpened("setParams()");
      if(stopBits == 1) {
         stopBits = 0;
      } else if(stopBits == 3) {
         stopBits = 1;
      }

      int flags = 0;
      if(System.getProperty("JSSC_IGNPAR") != null || System.getProperty("JSSC_IGNPAR".toLowerCase()) != null) {
         flags |= 1;
      }

      if(System.getProperty("JSSC_PARMRK") != null || System.getProperty("JSSC_PARMRK".toLowerCase()) != null) {
         flags |= 2;
      }

      return this.serialInterface.setParams(this.portHandle, baudRate, dataBits, stopBits, parity, setRTS, setDTR, flags);
   }

   public boolean purgePort(int flags) throws SerialPortException {
      this.checkPortOpened("purgePort()");
      return this.serialInterface.purgePort(this.portHandle, flags);
   }

   public boolean setEventsMask(int mask) throws SerialPortException {
      this.checkPortOpened("setEventsMask()");
      if(SerialNativeInterface.getOsType() != 0 && SerialNativeInterface.getOsType() != 2 && SerialNativeInterface.getOsType() != 3) {
         boolean returnValue = this.serialInterface.setEventsMask(this.portHandle, mask);
         if(!returnValue) {
            throw new SerialPortException(this.portName, "setEventsMask()", "Can\'t set mask");
         } else {
            if(mask > 0) {
               this.maskAssigned = true;
            } else {
               this.maskAssigned = false;
            }

            return returnValue;
         }
      } else {
         this.linuxMask = mask;
         if(mask > 0) {
            this.maskAssigned = true;
         } else {
            this.maskAssigned = false;
         }

         return true;
      }
   }

   public int getEventsMask() throws SerialPortException {
      this.checkPortOpened("getEventsMask()");
      return SerialNativeInterface.getOsType() != 0 && SerialNativeInterface.getOsType() != 2 && SerialNativeInterface.getOsType() != 3?this.serialInterface.getEventsMask(this.portHandle):this.linuxMask;
   }

   private int getLinuxMask() {
      return this.linuxMask;
   }

   public boolean setRTS(boolean enabled) throws SerialPortException {
      this.checkPortOpened("setRTS()");
      return this.serialInterface.setRTS(this.portHandle, enabled);
   }

   public boolean setDTR(boolean enabled) throws SerialPortException {
      this.checkPortOpened("setDTR()");
      return this.serialInterface.setDTR(this.portHandle, enabled);
   }

   public boolean writeBytes(byte[] buffer) throws SerialPortException {
      this.checkPortOpened("writeBytes()");
      return this.serialInterface.writeBytes(this.portHandle, buffer);
   }

   public boolean writeByte(byte singleByte) throws SerialPortException {
      this.checkPortOpened("writeByte()");
      return this.writeBytes(new byte[]{singleByte});
   }

   public boolean writeString(String string) throws SerialPortException {
      this.checkPortOpened("writeString()");
      return this.writeBytes(string.getBytes());
   }

   public boolean writeInt(int singleInt) throws SerialPortException {
      this.checkPortOpened("writeInt()");
      return this.writeBytes(new byte[]{(byte)singleInt});
   }

   public boolean writeIntArray(int[] buffer) throws SerialPortException {
      this.checkPortOpened("writeIntArray()");
      byte[] byteArray = new byte[buffer.length];

      for(int i = 0; i < buffer.length; ++i) {
         byteArray[i] = (byte)buffer[i];
      }

      return this.writeBytes(byteArray);
   }

   public byte[] readBytes(int byteCount) throws SerialPortException {
      this.checkPortOpened("readBytes()");
      return this.serialInterface.readBytes(this.portHandle, byteCount);
   }

   public String readString(int byteCount) throws SerialPortException {
      this.checkPortOpened("readString()");
      return new String(this.readBytes(byteCount));
   }

   public String readHexString(int byteCount) throws SerialPortException {
      this.checkPortOpened("readHexString()");
      return this.readHexString(byteCount, " ");
   }

   public String readHexString(int byteCount, String separator) throws SerialPortException {
      this.checkPortOpened("readHexString()");
      String[] strBuffer = this.readHexStringArray(byteCount);
      String returnString = "";
      boolean insertSeparator = false;
      String[] arr$ = strBuffer;
      int len$ = strBuffer.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String value = arr$[i$];
         if(insertSeparator) {
            returnString = returnString + separator;
         }

         returnString = returnString + value;
         insertSeparator = true;
      }

      return returnString;
   }

   public String[] readHexStringArray(int byteCount) throws SerialPortException {
      this.checkPortOpened("readHexStringArray()");
      int[] intBuffer = this.readIntArray(byteCount);
      String[] strBuffer = new String[intBuffer.length];

      for(int i = 0; i < intBuffer.length; ++i) {
         String value = Integer.toHexString(intBuffer[i]).toUpperCase();
         if(value.length() == 1) {
            value = "0" + value;
         }

         strBuffer[i] = value;
      }

      return strBuffer;
   }

   public int[] readIntArray(int byteCount) throws SerialPortException {
      this.checkPortOpened("readIntArray()");
      byte[] buffer = this.readBytes(byteCount);
      int[] intBuffer = new int[buffer.length];

      for(int i = 0; i < buffer.length; ++i) {
         if(buffer[i] < 0) {
            intBuffer[i] = 256 + buffer[i];
         } else {
            intBuffer[i] = buffer[i];
         }
      }

      return intBuffer;
   }

   private void waitBytesWithTimeout(String methodName, int byteCount, int timeout) throws SerialPortException, SerialPortTimeoutException {
      this.checkPortOpened("waitBytesWithTimeout()");
      boolean timeIsOut = true;
      long startTime = System.currentTimeMillis();

      while(System.currentTimeMillis() - startTime < (long)timeout) {
         if(this.getInputBufferBytesCount() >= byteCount) {
            timeIsOut = false;
            break;
         }

         try {
            Thread.sleep(0L, 100);
         } catch (InterruptedException var8) {
            ;
         }
      }

      if(timeIsOut) {
         throw new SerialPortTimeoutException(this.portName, methodName, timeout);
      }
   }

   public byte[] readBytes(int byteCount, int timeout) throws SerialPortException, SerialPortTimeoutException {
      this.checkPortOpened("readBytes()");
      this.waitBytesWithTimeout("readBytes()", byteCount, timeout);
      return this.readBytes(byteCount);
   }

   public String readString(int byteCount, int timeout) throws SerialPortException, SerialPortTimeoutException {
      this.checkPortOpened("readString()");
      this.waitBytesWithTimeout("readString()", byteCount, timeout);
      return this.readString(byteCount);
   }

   public String readHexString(int byteCount, int timeout) throws SerialPortException, SerialPortTimeoutException {
      this.checkPortOpened("readHexString()");
      this.waitBytesWithTimeout("readHexString()", byteCount, timeout);
      return this.readHexString(byteCount);
   }

   public String readHexString(int byteCount, String separator, int timeout) throws SerialPortException, SerialPortTimeoutException {
      this.checkPortOpened("readHexString()");
      this.waitBytesWithTimeout("readHexString()", byteCount, timeout);
      return this.readHexString(byteCount, separator);
   }

   public String[] readHexStringArray(int byteCount, int timeout) throws SerialPortException, SerialPortTimeoutException {
      this.checkPortOpened("readHexStringArray()");
      this.waitBytesWithTimeout("readHexStringArray()", byteCount, timeout);
      return this.readHexStringArray(byteCount);
   }

   public int[] readIntArray(int byteCount, int timeout) throws SerialPortException, SerialPortTimeoutException {
      this.checkPortOpened("readIntArray()");
      this.waitBytesWithTimeout("readIntArray()", byteCount, timeout);
      return this.readIntArray(byteCount);
   }

   public byte[] readBytes() throws SerialPortException {
      this.checkPortOpened("readBytes()");
      int byteCount = this.getInputBufferBytesCount();
      return byteCount <= 0?null:this.readBytes(byteCount);
   }

   public String readString() throws SerialPortException {
      this.checkPortOpened("readString()");
      int byteCount = this.getInputBufferBytesCount();
      return byteCount <= 0?null:this.readString(byteCount);
   }

   public String readHexString() throws SerialPortException {
      this.checkPortOpened("readHexString()");
      int byteCount = this.getInputBufferBytesCount();
      return byteCount <= 0?null:this.readHexString(byteCount);
   }

   public String readHexString(String separator) throws SerialPortException {
      this.checkPortOpened("readHexString()");
      int byteCount = this.getInputBufferBytesCount();
      return byteCount <= 0?null:this.readHexString(byteCount, separator);
   }

   public String[] readHexStringArray() throws SerialPortException {
      this.checkPortOpened("readHexStringArray()");
      int byteCount = this.getInputBufferBytesCount();
      return byteCount <= 0?null:this.readHexStringArray(byteCount);
   }

   public int[] readIntArray() throws SerialPortException {
      this.checkPortOpened("readIntArray()");
      int byteCount = this.getInputBufferBytesCount();
      return byteCount <= 0?null:this.readIntArray(byteCount);
   }

   public int getInputBufferBytesCount() throws SerialPortException {
      this.checkPortOpened("getInputBufferBytesCount()");
      return this.serialInterface.getBuffersBytesCount(this.portHandle)[0];
   }

   public int getOutputBufferBytesCount() throws SerialPortException {
      this.checkPortOpened("getOutputBufferBytesCount()");
      return this.serialInterface.getBuffersBytesCount(this.portHandle)[1];
   }

   public boolean setFlowControlMode(int mask) throws SerialPortException {
      this.checkPortOpened("setFlowControlMode()");
      return this.serialInterface.setFlowControlMode(this.portHandle, mask);
   }

   public int getFlowControlMode() throws SerialPortException {
      this.checkPortOpened("getFlowControlMode()");
      return this.serialInterface.getFlowControlMode(this.portHandle);
   }

   public boolean sendBreak(int duration) throws SerialPortException {
      this.checkPortOpened("sendBreak()");
      return this.serialInterface.sendBreak(this.portHandle, duration);
   }

   private int[][] waitEvents() {
      return this.serialInterface.waitEvents(this.portHandle);
   }

   private void checkPortOpened(String methodName) throws SerialPortException {
      if(!this.portOpened) {
         throw new SerialPortException(this.portName, methodName, "Port not opened");
      }
   }

   public int[] getLinesStatus() throws SerialPortException {
      this.checkPortOpened("getLinesStatus()");
      return this.serialInterface.getLinesStatus(this.portHandle);
   }

   public boolean isCTS() throws SerialPortException {
      this.checkPortOpened("isCTS()");
      return this.serialInterface.getLinesStatus(this.portHandle)[0] == 1;
   }

   public boolean isDSR() throws SerialPortException {
      this.checkPortOpened("isDSR()");
      return this.serialInterface.getLinesStatus(this.portHandle)[1] == 1;
   }

   public boolean isRING() throws SerialPortException {
      this.checkPortOpened("isRING()");
      return this.serialInterface.getLinesStatus(this.portHandle)[2] == 1;
   }

   public boolean isRLSD() throws SerialPortException {
      this.checkPortOpened("isRLSD()");
      return this.serialInterface.getLinesStatus(this.portHandle)[3] == 1;
   }

   public void addEventListener(SerialPortEventListener listener) throws SerialPortException {
      this.addEventListener(listener, 1, false);
   }

   public void addEventListener(SerialPortEventListener listener, int mask) throws SerialPortException {
      this.addEventListener(listener, mask, true);
   }

   private void addEventListener(SerialPortEventListener listener, int mask, boolean overwriteMask) throws SerialPortException {
      this.checkPortOpened("addEventListener()");
      if(this.eventListenerAdded) {
         throw new SerialPortException(this.portName, "addEventListener()", "Event listener already added");
      } else {
         if(this.maskAssigned && overwriteMask || !this.maskAssigned) {
            this.setEventsMask(mask);
         }

         this.eventListener = listener;
         this.eventThread = this.getNewEventThread();
         this.eventThread.setName("EventThread " + this.portName);

         try {
            Method ex = this.eventListener.getClass().getMethod("errorOccurred", new Class[]{SerialPortException.class});
            ex.setAccessible(true);
            this.methodErrorOccurred = ex;
         } catch (SecurityException var5) {
            ;
         } catch (NoSuchMethodException var6) {
            ;
         }

         this.eventThread.start();
         this.eventListenerAdded = true;
      }
   }

   private EventThread getNewEventThread() {
      return (EventThread)(SerialNativeInterface.getOsType() != 0 && SerialNativeInterface.getOsType() != 2 && SerialNativeInterface.getOsType() != 3?new EventThread():new LinuxEventThread());
   }

   public boolean removeEventListener() throws SerialPortException {
      this.checkPortOpened("removeEventListener()");
      if(!this.eventListenerAdded) {
         throw new SerialPortException(this.portName, "removeEventListener()", "Can\'t remove event listener, because listener not added");
      } else {
         this.eventThread.terminateThread();
         this.setEventsMask(0);
         if(Thread.currentThread().getId() != this.eventThread.getId() && this.eventThread.isAlive()) {
            try {
               this.eventThread.join(5000L);
            } catch (InterruptedException var2) {
               throw new SerialPortException(this.portName, "removeEventListener()", "Event listener thread interrupted");
            }
         }

         this.methodErrorOccurred = null;
         this.eventListenerAdded = false;
         return true;
      }
   }

   public boolean closePort() throws SerialPortException {
      this.checkPortOpened("closePort()");
      if(this.eventListenerAdded) {
         this.removeEventListener();
      }

      boolean returnValue = this.serialInterface.closePort(this.portHandle);
      if(returnValue) {
         this.maskAssigned = false;
         this.portOpened = false;
      }

      return returnValue;
   }

   // $FF: synthetic class
   static class SyntheticClass_1 {
   }

   private class LinuxEventThread extends EventThread {
      private final int INTERRUPT_BREAK = 512;
      private final int INTERRUPT_TX = 1024;
      private final int INTERRUPT_FRAME = 2048;
      private final int INTERRUPT_OVERRUN = 4096;
      private final int INTERRUPT_PARITY = 8192;
      private int interruptBreak;
      private int interruptTX;
      private int interruptFrame;
      private int interruptOverrun;
      private int interruptParity;
      private int preCTS;
      private int preDSR;
      private int preRLSD;
      private int preRING;

      public LinuxEventThread() {
         super((SyntheticClass_1)null);
         int[][] eventArray = SerialPort.this.waitEvents();

         for(int i = 0; i < eventArray.length; ++i) {
            int eventType = eventArray[i][0];
            int eventValue = eventArray[i][1];
            switch(eventType) {
            case 8:
               this.preCTS = eventValue;
               break;
            case 16:
               this.preDSR = eventValue;
               break;
            case 32:
               this.preRLSD = eventValue;
               break;
            case 256:
               this.preRING = eventValue;
               break;
            case 512:
               this.interruptBreak = eventValue;
               break;
            case 1024:
               this.interruptTX = eventValue;
               break;
            case 2048:
               this.interruptFrame = eventValue;
               break;
            case 4096:
               this.interruptOverrun = eventValue;
               break;
            case 8192:
               this.interruptParity = eventValue;
            }
         }

      }

      public void run() {
         while(!super.threadTerminated) {
            int[][] eventArray = SerialPort.this.waitEvents();
            int mask = SerialPort.this.getLinuxMask();
            boolean interruptTxChanged = false;
            int errorMask = 0;

            for(int ex = 0; ex < eventArray.length; ++ex) {
               boolean sendEvent = false;
               int eventType = eventArray[ex][0];
               int eventValue = eventArray[ex][1];
               if(eventType > 0 && !super.threadTerminated) {
                  switch(eventType) {
                  case 1:
                     if((mask & 1) == 1 && eventValue > 0) {
                        sendEvent = true;
                     }
                     break;
                  case 4:
                     if((mask & 4) == 4 && eventValue == 0 && interruptTxChanged) {
                        sendEvent = true;
                     }
                     break;
                  case 8:
                     if(eventValue != this.preCTS) {
                        this.preCTS = eventValue;
                        if((mask & 8) == 8) {
                           sendEvent = true;
                        }
                     }
                     break;
                  case 16:
                     if(eventValue != this.preDSR) {
                        this.preDSR = eventValue;
                        if((mask & 16) == 16) {
                           sendEvent = true;
                        }
                     }
                     break;
                  case 32:
                     if(eventValue != this.preRLSD) {
                        this.preRLSD = eventValue;
                        if((mask & 32) == 32) {
                           sendEvent = true;
                        }
                     }
                     break;
                  case 256:
                     if(eventValue != this.preRING) {
                        this.preRING = eventValue;
                        if((mask & 256) == 256) {
                           sendEvent = true;
                        }
                     }
                     break;
                  case 512:
                     if(eventValue != this.interruptBreak) {
                        this.interruptBreak = eventValue;
                        if((mask & 64) == 64) {
                           eventType = 64;
                           eventValue = 0;
                           sendEvent = true;
                        }
                     }
                     break;
                  case 1024:
                     if(eventValue != this.interruptTX) {
                        this.interruptTX = eventValue;
                        interruptTxChanged = true;
                     }
                     break;
                  case 2048:
                     if(eventValue != this.interruptFrame) {
                        this.interruptFrame = eventValue;
                        errorMask |= 8;
                     }
                     break;
                  case 4096:
                     if(eventValue != this.interruptOverrun) {
                        this.interruptOverrun = eventValue;
                        errorMask |= 2;
                     }
                     break;
                  case 8192:
                     if(eventValue != this.interruptParity) {
                        this.interruptParity = eventValue;
                        errorMask |= 4;
                     }

                     if((mask & 128) == 128 && errorMask != 0) {
                        eventType = 128;
                        eventValue = errorMask;
                        sendEvent = true;
                     }
                  }

                  if(sendEvent) {
                     SerialPort.this.eventListener.serialEvent(new SerialPortEvent(SerialPort.this.portName, eventType, eventValue));
                  }
               }
            }

            try {
               Thread.sleep(0L, 100);
            } catch (Exception var9) {
               ;
            }
         }

      }
   }

   private class EventThread extends Thread {
      private boolean threadTerminated;

      private EventThread() {
         this.threadTerminated = false;
      }

      public void run() {
         label22:
         while(true) {
            if(!this.threadTerminated) {
               int[][] eventArray = SerialPort.this.waitEvents();
               int i = 0;

               while(true) {
                  if(i >= eventArray.length) {
                     continue label22;
                  }

                  if(eventArray[i][0] > 0 && !this.threadTerminated) {
                     SerialPort.this.eventListener.serialEvent(new SerialPortEvent(SerialPort.this.portName, eventArray[i][0], eventArray[i][1]));
                  }

                  ++i;
               }
            }

            return;
         }
      }

      private void terminateThread() {
         this.threadTerminated = true;
      }

      // $FF: synthetic method
      EventThread(SyntheticClass_1 x1) {
         this();
      }
   }
}
