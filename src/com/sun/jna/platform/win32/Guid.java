package com.sun.jna.platform.win32;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;

public interface Guid extends StdCallLibrary {
   public static class GUID extends Structure {
      public int Data1;
      public short Data2;
      public short Data3;
      public byte[] Data4 = new byte[8];

      public GUID() {
      }

      public GUID(Pointer memory) {
         this.useMemory(memory);
         this.read();
      }

      public static class ByReference extends GUID implements Structure.ByReference {
         public ByReference() {
         }

         public ByReference(Pointer memory) {
            super(memory);
         }
      }
   }
}
