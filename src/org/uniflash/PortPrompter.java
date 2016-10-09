package org.uniflash;

import java.util.Iterator;
import java.util.List;
import org.uniflash.PortFinder;

public class PortPrompter {
   private String saved = null;

   private boolean checkPort() {
      List ids = PortFinder.getPortIdentifiers();
      if(this.saved == null) {
         return false;
      } else {
         Iterator var3 = ids.iterator();

         while(var3.hasNext()) {
            String id = (String)var3.next();
            if(id.equalsIgnoreCase(this.saved)) {
               return true;
            }
         }

         return false;
      }
   }

   public String getPort() {
      if(!this.checkPort()) {
         this.saved = null;
      }

      return this.saved;
   }

   public boolean hasSavedPort() {
      if(!this.checkPort()) {
         this.saved = null;
      }

      return this.saved != null;
   }

   public void savePort(String port) {
      this.saved = port;
   }
}
