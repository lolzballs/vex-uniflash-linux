package com.sun.jna;

import java.nio.CharBuffer;

class NativeString implements CharSequence, Comparable {
   private Pointer pointer;
   private boolean wide;

   public NativeString(String string) {
      this(string, false);
   }

   public NativeString(String string, boolean wide) {
      if(string == null) {
         throw new NullPointerException("String must not be null");
      } else {
         this.wide = wide;
         if(wide) {
            int data = (string.length() + 1) * Native.WCHAR_SIZE;
            this.pointer = new Memory((long)data);
            this.pointer.setString(0L, string, true);
         } else {
            byte[] data1 = Native.getBytes(string);
            this.pointer = new Memory((long)(data1.length + 1));
            this.pointer.write(0L, (byte[])data1, 0, data1.length);
            this.pointer.setByte((long)data1.length, (byte)0);
         }

      }
   }

   public int hashCode() {
      return this.toString().hashCode();
   }

   public boolean equals(Object other) {
      return other instanceof CharSequence?this.compareTo(other) == 0:false;
   }

   public String toString() {
      return this.pointer.getString(0L, this.wide);
   }

   public Pointer getPointer() {
      return this.pointer;
   }

   public char charAt(int index) {
      return this.toString().charAt(index);
   }

   public int length() {
      return this.toString().length();
   }

   public CharSequence subSequence(int start, int end) {
      return CharBuffer.wrap(this.toString()).subSequence(start, end);
   }

   public int compareTo(Object other) {
      return other == null?1:this.toString().compareTo(other.toString());
   }
}
