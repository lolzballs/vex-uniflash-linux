package org.uniflash;

public class SerialException extends Exception {
   private static final long serialVersionUID = -9221237469195283666L;

   public SerialException(String message) {
      super(message);
   }

   public SerialException(String message, Throwable cause) {
      super(message, cause);
   }
}
