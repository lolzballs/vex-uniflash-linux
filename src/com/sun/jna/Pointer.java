package com.sun.jna;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class Pointer {
   public static final int SIZE;
   public static final Pointer NULL;
   protected long peer;

   public static final Pointer createConstant(long peer) {
      return new Opaque(peer);
   }

   public static final Pointer createConstant(int peer) {
      return new Opaque((long)peer & -1L);
   }

   Pointer() {
   }

   public Pointer(long peer) {
      this.peer = peer;
   }

   public Pointer share(long offset) {
      return this.share(offset, 0L);
   }

   public Pointer share(long offset, long sz) {
      return offset == 0L?this:new Pointer(this.peer + offset);
   }

   public void clear(long size) {
      this.setMemory(0L, size, (byte)0);
   }

   public boolean equals(Object o) {
      return o == this?true:(o == null?false:o instanceof Pointer && ((Pointer)o).peer == this.peer);
   }

   public int hashCode() {
      return (int)((this.peer >>> 32) + (this.peer & -1L));
   }

   public long indexOf(long offset, byte value) {
      return Native.indexOf(this.peer + offset, value);
   }

   public void read(long offset, byte[] buf, int index, int length) {
      Native.read(this.peer + offset, buf, index, length);
   }

   public void read(long offset, short[] buf, int index, int length) {
      Native.read(this.peer + offset, buf, index, length);
   }

   public void read(long offset, char[] buf, int index, int length) {
      Native.read(this.peer + offset, buf, index, length);
   }

   public void read(long offset, int[] buf, int index, int length) {
      Native.read(this.peer + offset, buf, index, length);
   }

   public void read(long offset, long[] buf, int index, int length) {
      Native.read(this.peer + offset, buf, index, length);
   }

   public void read(long offset, float[] buf, int index, int length) {
      Native.read(this.peer + offset, buf, index, length);
   }

   public void read(long offset, double[] buf, int index, int length) {
      Native.read(this.peer + offset, buf, index, length);
   }

   public void read(long offset, Pointer[] buf, int index, int length) {
      for(int i = 0; i < length; ++i) {
         Pointer p = this.getPointer(offset + (long)(i * SIZE));
         Pointer oldp = buf[i + index];
         if(oldp == null || p == null || p.peer != oldp.peer) {
            buf[i + index] = p;
         }
      }

   }

   public void write(long offset, byte[] buf, int index, int length) {
      Native.write(this.peer + offset, buf, index, length);
   }

   public void write(long offset, short[] buf, int index, int length) {
      Native.write(this.peer + offset, buf, index, length);
   }

   public void write(long offset, char[] buf, int index, int length) {
      Native.write(this.peer + offset, buf, index, length);
   }

   public void write(long offset, int[] buf, int index, int length) {
      Native.write(this.peer + offset, buf, index, length);
   }

   public void write(long offset, long[] buf, int index, int length) {
      Native.write(this.peer + offset, buf, index, length);
   }

   public void write(long offset, float[] buf, int index, int length) {
      Native.write(this.peer + offset, buf, index, length);
   }

   public void write(long offset, double[] buf, int index, int length) {
      Native.write(this.peer + offset, buf, index, length);
   }

   public void write(long bOff, Pointer[] buf, int index, int length) {
      for(int i = 0; i < length; ++i) {
         this.setPointer(bOff + (long)(i * SIZE), buf[index + i]);
      }

   }

   Object getValue(long offset, Class type, Object currentValue) {
      Object result = null;
      if(Structure.class.isAssignableFrom(type)) {
         Structure nm2 = (Structure)currentValue;
         if(Structure.ByReference.class.isAssignableFrom(type)) {
            nm2 = Structure.updateStructureByReference(type, nm2, this.getPointer(offset));
         } else {
            nm2.useMemory(this, (int)offset);
            nm2.read();
         }

         result = nm2;
      } else if(type != Boolean.TYPE && type != Boolean.class) {
         if(type != Byte.TYPE && type != Byte.class) {
            if(type != Short.TYPE && type != Short.class) {
               if(type != Character.TYPE && type != Character.class) {
                  if(type != Integer.TYPE && type != Integer.class) {
                     if(type != Long.TYPE && type != Long.class) {
                        if(type != Float.TYPE && type != Float.class) {
                           if(type != Double.TYPE && type != Double.class) {
                              Pointer nm1;
                              Pointer tc2;
                              if(Pointer.class.isAssignableFrom(type)) {
                                 nm1 = this.getPointer(offset);
                                 if(nm1 != null) {
                                    tc2 = currentValue instanceof Pointer?(Pointer)currentValue:null;
                                    if(tc2 != null && nm1.peer == tc2.peer) {
                                       result = tc2;
                                    } else {
                                       result = nm1;
                                    }
                                 }
                              } else if(type == String.class) {
                                 nm1 = this.getPointer(offset);
                                 result = nm1 != null?nm1.getString(0L):null;
                              } else if(type == WString.class) {
                                 nm1 = this.getPointer(offset);
                                 result = nm1 != null?new WString(nm1.getString(0L, true)):null;
                              } else if(Callback.class.isAssignableFrom(type)) {
                                 nm1 = this.getPointer(offset);
                                 if(nm1 == null) {
                                    result = null;
                                 } else {
                                    Callback tc3 = (Callback)currentValue;
                                    Pointer value1 = CallbackReference.getFunctionPointer(tc3);
                                    if(!nm1.equals(value1)) {
                                       tc3 = CallbackReference.getCallback(type, nm1);
                                    }

                                    result = tc3;
                                 }
                              } else if(Buffer.class.isAssignableFrom(type)) {
                                 nm1 = this.getPointer(offset);
                                 if(nm1 == null) {
                                    result = null;
                                 } else {
                                    tc2 = currentValue == null?null:Native.getDirectBufferPointer((Buffer)currentValue);
                                    if(tc2 == null || !tc2.equals(nm1)) {
                                       throw new IllegalStateException("Can\'t autogenerate a direct buffer on memory read");
                                    }

                                    result = currentValue;
                                 }
                              } else if(NativeMapped.class.isAssignableFrom(type)) {
                                 NativeMapped nm = (NativeMapped)currentValue;
                                 if(nm != null) {
                                    Object tc = this.getValue(offset, nm.nativeType(), (Object)null);
                                    result = nm.fromNative(tc, new FromNativeContext(type));
                                 } else {
                                    NativeMappedConverter tc1 = NativeMappedConverter.getInstance(type);
                                    Object value = this.getValue(offset, tc1.nativeType(), (Object)null);
                                    result = tc1.fromNative(value, new FromNativeContext(type));
                                 }
                              } else {
                                 if(!type.isArray()) {
                                    throw new IllegalArgumentException("Reading \"" + type + "\" from memory is not supported");
                                 }

                                 result = currentValue;
                                 if(currentValue == null) {
                                    throw new IllegalStateException("Need an initialized array");
                                 }

                                 this.getArrayValue(offset, currentValue, type.getComponentType());
                              }
                           } else {
                              result = new Double(this.getDouble(offset));
                           }
                        } else {
                           result = new Float(this.getFloat(offset));
                        }
                     } else {
                        result = new Long(this.getLong(offset));
                     }
                  } else {
                     result = new Integer(this.getInt(offset));
                  }
               } else {
                  result = new Character(this.getChar(offset));
               }
            } else {
               result = new Short(this.getShort(offset));
            }
         } else {
            result = new Byte(this.getByte(offset));
         }
      } else {
         result = Function.valueOf(this.getInt(offset) != 0);
      }

      return result;
   }

   private void getArrayValue(long offset, Object o, Class cls) {
      boolean length = false;
      int var12 = Array.getLength(o);
      if(cls == Byte.TYPE) {
         this.read(offset, (byte[])((byte[])((byte[])o)), 0, var12);
      } else if(cls == Short.TYPE) {
         this.read(offset, (short[])((short[])((short[])o)), 0, var12);
      } else if(cls == Character.TYPE) {
         this.read(offset, (char[])((char[])((char[])o)), 0, var12);
      } else if(cls == Integer.TYPE) {
         this.read(offset, (int[])((int[])((int[])o)), 0, var12);
      } else if(cls == Long.TYPE) {
         this.read(offset, (long[])((long[])((long[])o)), 0, var12);
      } else if(cls == Float.TYPE) {
         this.read(offset, (float[])((float[])((float[])o)), 0, var12);
      } else if(cls == Double.TYPE) {
         this.read(offset, (double[])((double[])((double[])o)), 0, var12);
      } else if(Pointer.class.isAssignableFrom(cls)) {
         this.read(offset, (Pointer[])((Pointer[])((Pointer[])o)), 0, var12);
      } else {
         int size;
         if(Structure.class.isAssignableFrom(cls)) {
            Structure[] array = (Structure[])((Structure[])o);
            if(Structure.ByReference.class.isAssignableFrom(cls)) {
               Pointer[] tc = this.getPointerArray(offset, array.length);

               for(size = 0; size < array.length; ++size) {
                  array[size] = Structure.updateStructureByReference(cls, array[size], tc[size]);
               }
            } else {
               for(int var14 = 0; var14 < array.length; ++var14) {
                  if(array[var14] == null) {
                     array[var14] = Structure.newInstance(cls);
                  }

                  array[var14].useMemory(this, (int)(offset + (long)(var14 * array[var14].size())));
                  array[var14].read();
               }
            }
         } else {
            if(!NativeMapped.class.isAssignableFrom(cls)) {
               throw new IllegalArgumentException("Reading array of " + cls + " from memory not supported");
            }

            NativeMapped[] var13 = (NativeMapped[])((NativeMapped[])o);
            NativeMappedConverter var15 = NativeMappedConverter.getInstance(cls);
            size = Native.getNativeSize(o.getClass(), o) / var13.length;

            for(int i = 0; i < var13.length; ++i) {
               Object value = this.getValue(offset + (long)(size * i), var15.nativeType(), var13[i]);
               var13[i] = (NativeMapped)var15.fromNative(value, new FromNativeContext(cls));
            }
         }
      }

   }

   public byte getByte(long offset) {
      return Native.getByte(this.peer + offset);
   }

   public char getChar(long offset) {
      return Native.getChar(this.peer + offset);
   }

   public short getShort(long offset) {
      return Native.getShort(this.peer + offset);
   }

   public int getInt(long offset) {
      return Native.getInt(this.peer + offset);
   }

   public long getLong(long offset) {
      return Native.getLong(this.peer + offset);
   }

   public NativeLong getNativeLong(long offset) {
      return new NativeLong(NativeLong.SIZE == 8?this.getLong(offset):(long)this.getInt(offset));
   }

   public float getFloat(long offset) {
      return Native.getFloat(this.peer + offset);
   }

   public double getDouble(long offset) {
      return Native.getDouble(this.peer + offset);
   }

   public Pointer getPointer(long offset) {
      return Native.getPointer(this.peer + offset);
   }

   public ByteBuffer getByteBuffer(long offset, long length) {
      return Native.getDirectByteBuffer(this.peer + offset, length).order(ByteOrder.nativeOrder());
   }

   public String getString(long offset, boolean wide) {
      return Native.getString(this.peer + offset, wide);
   }

   public String getString(long offset) {
      String encoding = System.getProperty("jna.encoding");
      if(encoding != null) {
         long len = this.indexOf(offset, (byte)0);
         if(len != -1L) {
            if(len > 2147483647L) {
               throw new OutOfMemoryError("String exceeds maximum length: " + len);
            }

            byte[] data = this.getByteArray(offset, (int)len);

            try {
               return new String(data, encoding);
            } catch (UnsupportedEncodingException var8) {
               ;
            }
         }
      }

      return this.getString(offset, false);
   }

   public byte[] getByteArray(long offset, int arraySize) {
      byte[] buf = new byte[arraySize];
      this.read(offset, (byte[])buf, 0, arraySize);
      return buf;
   }

   public char[] getCharArray(long offset, int arraySize) {
      char[] buf = new char[arraySize];
      this.read(offset, (char[])buf, 0, arraySize);
      return buf;
   }

   public short[] getShortArray(long offset, int arraySize) {
      short[] buf = new short[arraySize];
      this.read(offset, (short[])buf, 0, arraySize);
      return buf;
   }

   public int[] getIntArray(long offset, int arraySize) {
      int[] buf = new int[arraySize];
      this.read(offset, (int[])buf, 0, arraySize);
      return buf;
   }

   public long[] getLongArray(long offset, int arraySize) {
      long[] buf = new long[arraySize];
      this.read(offset, (long[])buf, 0, arraySize);
      return buf;
   }

   public float[] getFloatArray(long offset, int arraySize) {
      float[] buf = new float[arraySize];
      this.read(offset, (float[])buf, 0, arraySize);
      return buf;
   }

   public double[] getDoubleArray(long offset, int arraySize) {
      double[] buf = new double[arraySize];
      this.read(offset, (double[])buf, 0, arraySize);
      return buf;
   }

   public Pointer[] getPointerArray(long base) {
      ArrayList array = new ArrayList();
      int offset = 0;

      for(Pointer p = this.getPointer(base); p != null; p = this.getPointer(base + (long)offset)) {
         array.add(p);
         offset += SIZE;
      }

      return (Pointer[])((Pointer[])array.toArray(new Pointer[array.size()]));
   }

   public Pointer[] getPointerArray(long offset, int arraySize) {
      Pointer[] buf = new Pointer[arraySize];
      this.read(offset, (Pointer[])buf, 0, arraySize);
      return buf;
   }

   public String[] getStringArray(long base) {
      return this.getStringArray(base, -1, false);
   }

   public String[] getStringArray(long base, int length) {
      return this.getStringArray(base, length, false);
   }

   public String[] getStringArray(long base, boolean wide) {
      return this.getStringArray(base, -1, wide);
   }

   public String[] getStringArray(long base, int length, boolean wide) {
      ArrayList strings = new ArrayList();
      int offset = 0;
      Pointer p;
      if(length != -1) {
         p = this.getPointer(base + (long)offset);
         int var10 = 0;

         while(var10++ < length) {
            String s1 = p == null?null:p.getString(0L, wide);
            strings.add(s1);
            if(var10 < length) {
               offset += SIZE;
               p = this.getPointer(base + (long)offset);
            }
         }
      } else {
         while((p = this.getPointer(base + (long)offset)) != null) {
            String s = p == null?null:p.getString(0L, wide);
            strings.add(s);
            offset += SIZE;
         }
      }

      return (String[])((String[])strings.toArray(new String[strings.size()]));
   }

   void setValue(long offset, Object value, Class type) {
      if(type != Boolean.TYPE && type != Boolean.class) {
         if(type != Byte.TYPE && type != Byte.class) {
            if(type != Short.TYPE && type != Short.class) {
               if(type != Character.TYPE && type != Character.class) {
                  if(type != Integer.TYPE && type != Integer.class) {
                     if(type != Long.TYPE && type != Long.class) {
                        if(type != Float.TYPE && type != Float.class) {
                           if(type != Double.TYPE && type != Double.class) {
                              if(type == Pointer.class) {
                                 this.setPointer(offset, (Pointer)value);
                              } else if(type == String.class) {
                                 this.setPointer(offset, (Pointer)value);
                              } else if(type == WString.class) {
                                 this.setPointer(offset, (Pointer)value);
                              } else if(Structure.class.isAssignableFrom(type)) {
                                 Structure tc = (Structure)value;
                                 if(Structure.ByReference.class.isAssignableFrom(type)) {
                                    this.setPointer(offset, tc == null?null:tc.getPointer());
                                    if(tc != null) {
                                       tc.autoWrite();
                                    }
                                 } else {
                                    tc.useMemory(this, (int)offset);
                                    tc.write();
                                 }
                              } else if(Callback.class.isAssignableFrom(type)) {
                                 this.setPointer(offset, CallbackReference.getFunctionPointer((Callback)value));
                              } else if(Buffer.class.isAssignableFrom(type)) {
                                 Pointer tc1 = value == null?null:Native.getDirectBufferPointer((Buffer)value);
                                 this.setPointer(offset, tc1);
                              } else if(NativeMapped.class.isAssignableFrom(type)) {
                                 NativeMappedConverter tc2 = NativeMappedConverter.getInstance(type);
                                 Class nativeType = tc2.nativeType();
                                 this.setValue(offset, tc2.toNative(value, new ToNativeContext()), nativeType);
                              } else {
                                 if(!type.isArray()) {
                                    throw new IllegalArgumentException("Writing " + type + " to memory is not supported");
                                 }

                                 this.setArrayValue(offset, value, type.getComponentType());
                              }
                           } else {
                              this.setDouble(offset, value == null?0.0D:((Double)value).doubleValue());
                           }
                        } else {
                           this.setFloat(offset, value == null?0.0F:((Float)value).floatValue());
                        }
                     } else {
                        this.setLong(offset, value == null?0L:((Long)value).longValue());
                     }
                  } else {
                     this.setInt(offset, value == null?0:((Integer)value).intValue());
                  }
               } else {
                  this.setChar(offset, value == null?'\u0000':((Character)value).charValue());
               }
            } else {
               this.setShort(offset, value == null?0:((Short)value).shortValue());
            }
         } else {
            this.setByte(offset, value == null?0:((Byte)value).byteValue());
         }
      } else {
         this.setInt(offset, Boolean.TRUE.equals(value)?-1:0);
      }

   }

   private void setArrayValue(long offset, Object value, Class cls) {
      if(cls == Byte.TYPE) {
         byte[] buf = (byte[])((byte[])value);
         this.write(offset, (byte[])buf, 0, buf.length);
      } else if(cls == Short.TYPE) {
         short[] var11 = (short[])((short[])value);
         this.write(offset, (short[])var11, 0, var11.length);
      } else if(cls == Character.TYPE) {
         char[] var12 = (char[])((char[])value);
         this.write(offset, (char[])var12, 0, var12.length);
      } else if(cls == Integer.TYPE) {
         int[] var13 = (int[])((int[])value);
         this.write(offset, (int[])var13, 0, var13.length);
      } else if(cls == Long.TYPE) {
         long[] var14 = (long[])((long[])value);
         this.write(offset, (long[])var14, 0, var14.length);
      } else if(cls == Float.TYPE) {
         float[] var16 = (float[])((float[])value);
         this.write(offset, (float[])var16, 0, var16.length);
      } else if(cls == Double.TYPE) {
         double[] var17 = (double[])((double[])value);
         this.write(offset, (double[])var17, 0, var17.length);
      } else if(Pointer.class.isAssignableFrom(cls)) {
         Pointer[] var19 = (Pointer[])((Pointer[])value);
         this.write(offset, (Pointer[])var19, 0, var19.length);
      } else if(Structure.class.isAssignableFrom(cls)) {
         Structure[] var20 = (Structure[])((Structure[])value);
         if(Structure.ByReference.class.isAssignableFrom(cls)) {
            Pointer[] tc = new Pointer[var20.length];

            for(int nativeType = 0; nativeType < var20.length; ++nativeType) {
               if(var20[nativeType] == null) {
                  tc[nativeType] = null;
               } else {
                  tc[nativeType] = var20[nativeType].getPointer();
                  var20[nativeType].write();
               }
            }

            this.write(offset, (Pointer[])tc, 0, tc.length);
         } else {
            for(int var15 = 0; var15 < var20.length; ++var15) {
               if(var20[var15] == null) {
                  var20[var15] = Structure.newInstance(cls);
               }

               var20[var15].useMemory(this, (int)(offset + (long)(var15 * var20[var15].size())));
               var20[var15].write();
            }
         }
      } else {
         if(!NativeMapped.class.isAssignableFrom(cls)) {
            throw new IllegalArgumentException("Writing array of " + cls + " to memory not supported");
         }

         NativeMapped[] var22 = (NativeMapped[])((NativeMapped[])value);
         NativeMappedConverter var18 = NativeMappedConverter.getInstance(cls);
         Class var21 = var18.nativeType();
         int size = Native.getNativeSize(value.getClass(), value) / var22.length;

         for(int i = 0; i < var22.length; ++i) {
            Object element = var18.toNative(var22[i], new ToNativeContext());
            this.setValue(offset + (long)(i * size), element, var21);
         }
      }

   }

   public void setMemory(long offset, long length, byte value) {
      Native.setMemory(this.peer + offset, length, value);
   }

   public void setByte(long offset, byte value) {
      Native.setByte(this.peer + offset, value);
   }

   public void setShort(long offset, short value) {
      Native.setShort(this.peer + offset, value);
   }

   public void setChar(long offset, char value) {
      Native.setChar(this.peer + offset, value);
   }

   public void setInt(long offset, int value) {
      Native.setInt(this.peer + offset, value);
   }

   public void setLong(long offset, long value) {
      Native.setLong(this.peer + offset, value);
   }

   public void setNativeLong(long offset, NativeLong value) {
      if(NativeLong.SIZE == 8) {
         this.setLong(offset, value.longValue());
      } else {
         this.setInt(offset, value.intValue());
      }

   }

   public void setFloat(long offset, float value) {
      Native.setFloat(this.peer + offset, value);
   }

   public void setDouble(long offset, double value) {
      Native.setDouble(this.peer + offset, value);
   }

   public void setPointer(long offset, Pointer value) {
      Native.setPointer(this.peer + offset, value != null?value.peer:0L);
   }

   public void setString(long offset, String value, boolean wide) {
      Native.setString(this.peer + offset, value, wide);
   }

   public void setString(long offset, String value) {
      byte[] data = Native.getBytes(value);
      this.write(offset, (byte[])data, 0, data.length);
      this.setByte(offset + (long)data.length, (byte)0);
   }

   public String toString() {
      return "native@0x" + Long.toHexString(this.peer);
   }

   public static long nativeValue(Pointer p) {
      return p.peer;
   }

   public static void nativeValue(Pointer p, long value) {
      p.peer = value;
   }

   static {
      if((SIZE = Native.POINTER_SIZE) == 0) {
         throw new Error("Native library not initialized");
      } else {
         NULL = null;
      }
   }

   // $FF: synthetic class
   static class SyntheticClass_1 {
   }

   private static class Opaque extends Pointer {
      private final String MSG;

      private Opaque(long peer) {
         super(peer);
         this.MSG = "This pointer is opaque: " + this;
      }

      public long indexOf(long offset, byte value) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void read(long bOff, byte[] buf, int index, int length) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void read(long bOff, char[] buf, int index, int length) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void read(long bOff, short[] buf, int index, int length) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void read(long bOff, int[] buf, int index, int length) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void read(long bOff, long[] buf, int index, int length) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void read(long bOff, float[] buf, int index, int length) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void read(long bOff, double[] buf, int index, int length) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void write(long bOff, byte[] buf, int index, int length) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void write(long bOff, char[] buf, int index, int length) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void write(long bOff, short[] buf, int index, int length) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void write(long bOff, int[] buf, int index, int length) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void write(long bOff, long[] buf, int index, int length) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void write(long bOff, float[] buf, int index, int length) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void write(long bOff, double[] buf, int index, int length) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public byte getByte(long bOff) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public char getChar(long bOff) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public short getShort(long bOff) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public int getInt(long bOff) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public long getLong(long bOff) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public float getFloat(long bOff) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public double getDouble(long bOff) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public Pointer getPointer(long bOff) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public String getString(long bOff, boolean wide) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void setByte(long bOff, byte value) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void setChar(long bOff, char value) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void setShort(long bOff, short value) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void setInt(long bOff, int value) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void setLong(long bOff, long value) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void setFloat(long bOff, float value) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void setDouble(long bOff, double value) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void setPointer(long offset, Pointer value) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public void setString(long offset, String value, boolean wide) {
         throw new UnsupportedOperationException(this.MSG);
      }

      public String toString() {
         return "opaque@0x" + Long.toHexString(this.peer);
      }

      // $FF: synthetic method
      Opaque(long x0, SyntheticClass_1 x1) {
         this(x0);
      }
   }
}
