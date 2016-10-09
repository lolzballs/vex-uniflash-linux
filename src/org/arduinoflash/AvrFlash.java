package org.arduinoflash;

import java.util.List;
import org.arduinoflash.AbstractAvrFlash;
import org.uniflash.PortFinder;
import org.uniflash.UniFlashConfig;

public class AvrFlash extends AbstractAvrFlash {
   protected String getProgrammer() {
      return this.programmer;
   }

   public List<PortFinder.Serial> locateSerial() {
      try {
         return UniFlashConfig.lookup(PortFinder.getPortList(), UniFlashConfig.getProperty("AvrFlash.accept", "067B,1FFB,1781,1B4F"));
      } catch (RuntimeException var2) {
         return PortFinder.defaultPortList();
      }
   }
}
