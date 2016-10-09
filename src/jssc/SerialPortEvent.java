package jssc;

public class SerialPortEvent {
   private String portName;
   private int eventType;
   private int eventValue;
   public static final int RXCHAR = 1;
   public static final int RXFLAG = 2;
   public static final int TXEMPTY = 4;
   public static final int CTS = 8;
   public static final int DSR = 16;
   public static final int RLSD = 32;
   public static final int BREAK = 64;
   public static final int ERR = 128;
   public static final int RING = 256;

   public SerialPortEvent(String portName, int eventType, int eventValue) {
      this.portName = portName;
      this.eventType = eventType;
      this.eventValue = eventValue;
   }

   public String getPortName() {
      return this.portName;
   }

   public int getEventType() {
      return this.eventType;
   }

   public int getEventValue() {
      return this.eventValue;
   }

   public boolean isRXCHAR() {
      return this.eventType == 1;
   }

   public boolean isRXFLAG() {
      return this.eventType == 2;
   }

   public boolean isTXEMPTY() {
      return this.eventType == 4;
   }

   public boolean isCTS() {
      return this.eventType == 8;
   }

   public boolean isDSR() {
      return this.eventType == 16;
   }

   public boolean isRLSD() {
      return this.eventType == 32;
   }

   public boolean isBREAK() {
      return this.eventType == 64;
   }

   public boolean isERR() {
      return this.eventType == 128;
   }

   public boolean isRING() {
      return this.eventType == 256;
   }
}
