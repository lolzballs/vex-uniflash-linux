package com.sun.jna.ptr;

public class ShortByReference extends ByReference {
   public ShortByReference() {
      this((byte)0);
   }

   public ShortByReference(short value) {
      super(2);
      this.setValue(value);
   }

   public void setValue(short value) {
      this.getPointer().setShort(0L, value);
   }

   public short getValue() {
      return this.getPointer().getShort(0L);
   }
}
