package com.sun.jna;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;

public class NativeLong extends IntegerType {
   public static final int SIZE;

   public NativeLong() {
      this(0L);
   }

   public NativeLong(long value) {
      super(SIZE, value);
   }

   static {
      SIZE = Native.LONG_SIZE;
   }
}
