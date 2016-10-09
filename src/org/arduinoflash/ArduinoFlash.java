package org.arduinoflash;

import java.util.List;
import org.arduinoflash.AbstractAvrFlash;
import org.uniflash.PortFinder;
import org.uniflash.UniFlashConfig;

public class ArduinoFlash extends AbstractAvrFlash {
   protected String getProgrammer() {
      return "arduino";
   }

   public List<PortFinder.Serial> locateSerial() {
      try {
         return UniFlashConfig.lookup(PortFinder.getPortList(), UniFlashConfig.getProperty("ArduinoFlash.accept", "2341,0403"));
      } catch (RuntimeException var2) {
         return PortFinder.defaultPortList();
      }
   }
}
