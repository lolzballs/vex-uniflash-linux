package jssc;

public class SerialPortTimeoutException extends Exception {
   private String portName;
   private String methodName;
   private int timeoutValue;

   public SerialPortTimeoutException(String portName, String methodName, int timeoutValue) {
      super("Port name - " + portName + "; Method name - " + methodName + "; Serial port operation timeout (" + timeoutValue + " ms).");
      this.portName = portName;
      this.methodName = methodName;
      this.timeoutValue = timeoutValue;
   }

   public String getPortName() {
      return this.portName;
   }

   public String getMethodName() {
      return this.methodName;
   }

   public int getTimeoutValue() {
      return this.timeoutValue;
   }
}
