package com.sun.jna.platform.win32;

import com.sun.jna.IntegerType;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;

public interface BaseTSD extends StdCallLibrary {
   public static class SIZE_T extends ULONG_PTR {
      public SIZE_T() {
         this(0L);
      }

      public SIZE_T(long value) {
         super(value);
      }
   }

   public static class DWORD_PTR extends IntegerType {
      public DWORD_PTR() {
         this(0L);
      }

      public DWORD_PTR(long value) {
         super(Pointer.SIZE, value);
      }
   }

   public static class ULONG_PTR extends IntegerType {
      public ULONG_PTR() {
         this(0L);
      }

      public ULONG_PTR(long value) {
         super(Pointer.SIZE, value);
      }
   }

   public static class SSIZE_T extends LONG_PTR {
      public SSIZE_T() {
         this(0L);
      }

      public SSIZE_T(long value) {
         super(value);
      }
   }

   public static class LONG_PTR extends IntegerType {
      public LONG_PTR() {
         this(0L);
      }

      public LONG_PTR(long value) {
         super(Pointer.SIZE, value);
      }
   }
}
