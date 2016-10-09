package org.uniflash;

import java.io.File;
import java.util.List;
import org.uniflash.Indicator;
import org.uniflash.PortFinder;
import org.uniflash.SerialException;

public interface FlashUtility {
   void end();

   String getExtension();

   List<PortFinder.Serial> locateSerial();

   void program(Indicator var1) throws SerialException;

   boolean requiresSerial();

   boolean setup(File var1, String[] var2, String var3) throws SerialException;
}
