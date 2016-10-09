package org.uniflash;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import org.uniflash.PortFinder;

public final class UniFlashConfig {
   private static final Properties PROPS = new Properties();

   static {
      try {
         InputStream is = UniFlashConfig.class.getResourceAsStream("uniflash.properties");
         PROPS.load(is);
         is.close();
      } catch (IOException var1) {
         ;
      }

   }

   public static String getProperty(String key, String defaultValue) {
      return PROPS.getProperty(key, defaultValue);
   }

   public static List<PortFinder.Serial> lookup(List<PortFinder.Serial> input, String acceptList) {
      ArrayList out = new ArrayList(input.size());
      StringTokenizer str = new StringTokenizer(acceptList, ",");

      while(str.hasMoreTokens()) {
         String element = str.nextToken().trim();
         out.addAll(PortFinder.findByID(element, input));
      }

      return out;
   }
}
