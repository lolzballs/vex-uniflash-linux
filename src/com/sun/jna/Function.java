package com.sun.jna;

import com.sun.jna.Callback;
import com.sun.jna.CallbackReference;
import com.sun.jna.FromNativeContext;
import com.sun.jna.FromNativeConverter;
import com.sun.jna.FunctionParameterContext;
import com.sun.jna.FunctionResultContext;
import com.sun.jna.Memory;
import com.sun.jna.MethodParameterContext;
import com.sun.jna.MethodResultContext;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeMapped;
import com.sun.jna.NativeMappedConverter;
import com.sun.jna.NativeString;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.Structure;
import com.sun.jna.ToNativeContext;
import com.sun.jna.ToNativeConverter;
import com.sun.jna.TypeMapper;
import com.sun.jna.WString;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

public class Function extends Pointer {
   public static final int MAX_NARGS = 256;
   public static final int C_CONVENTION = 0;
   public static final int ALT_CONVENTION = 1;
   private static final int MASK_CC = 3;
   public static final int THROW_LAST_ERROR = 4;
   static final Integer INTEGER_TRUE = new Integer(-1);
   static final Integer INTEGER_FALSE = new Integer(0);
   private NativeLibrary library;
   private final String functionName;
   int callFlags;
   final Map options;
   static final String OPTION_INVOKING_METHOD = "invoking-method";

   public static Function getFunction(String libraryName, String functionName) {
      return NativeLibrary.getInstance(libraryName).getFunction(functionName);
   }

   public static Function getFunction(String libraryName, String functionName, int callFlags) {
      return NativeLibrary.getInstance(libraryName).getFunction(functionName, callFlags);
   }

   public static Function getFunction(Pointer p) {
      return getFunction(p, 0);
   }

   public static Function getFunction(Pointer p, int callFlags) {
      return new Function(p, callFlags);
   }

   Function(NativeLibrary library, String functionName, int callFlags) {
      this.checkCallingConvention(callFlags & 3);
      if(functionName == null) {
         throw new NullPointerException("Function name must not be null");
      } else {
         this.library = library;
         this.functionName = functionName;
         this.callFlags = callFlags;
         this.options = library.options;

         try {
            this.peer = library.getSymbolAddress(functionName);
         } catch (UnsatisfiedLinkError var5) {
            throw new UnsatisfiedLinkError("Error looking up function \'" + functionName + "\': " + var5.getMessage());
         }
      }
   }

   Function(Pointer functionAddress, int callFlags) {
      this.checkCallingConvention(callFlags & 3);
      if(functionAddress != null && functionAddress.peer != 0L) {
         this.functionName = functionAddress.toString();
         this.callFlags = callFlags;
         this.peer = functionAddress.peer;
         this.options = Collections.EMPTY_MAP;
      } else {
         throw new NullPointerException("Function address may not be null");
      }
   }

   private void checkCallingConvention(int convention) throws IllegalArgumentException {
      switch(convention) {
      case 0:
      case 1:
         return;
      default:
         throw new IllegalArgumentException("Unrecognized calling convention: " + convention);
      }
   }

   public String getName() {
      return this.functionName;
   }

   public int getCallingConvention() {
      return this.callFlags & 3;
   }

   public Object invoke(Class returnType, Object[] inArgs) {
      return this.invoke(returnType, inArgs, this.options);
   }

   public Object invoke(Class returnType, Object[] inArgs, Map options) {
      Object[] args = new Object[0];
      if(inArgs != null) {
         if(inArgs.length > 256) {
            throw new UnsupportedOperationException("Maximum argument count is 256");
         }

         args = new Object[inArgs.length];
         System.arraycopy(inArgs, 0, args, 0, args.length);
      }

      TypeMapper mapper = (TypeMapper)options.get("type-mapper");
      Method invokingMethod = (Method)options.get("invoking-method");
      boolean allowObjects = Boolean.TRUE.equals(options.get("allow-objects"));

      for(int nativeType = 0; nativeType < args.length; ++nativeType) {
         args[nativeType] = this.convertArgument(args, nativeType, invokingMethod, mapper, allowObjects);
      }

      Class var18 = returnType;
      Object resultConverter = null;
      if(NativeMapped.class.isAssignableFrom(returnType)) {
         NativeMappedConverter result = NativeMappedConverter.getInstance(returnType);
         resultConverter = result;
         var18 = result.nativeType();
      } else if(mapper != null) {
         resultConverter = mapper.getFromNativeConverter(returnType);
         if(resultConverter != null) {
            var18 = ((FromNativeConverter)resultConverter).nativeType();
         }
      }

      Object var19 = this.invoke(args, var18, allowObjects);
      if(resultConverter != null) {
         Object i;
         if(invokingMethod != null) {
            i = new MethodResultContext(returnType, this, inArgs, invokingMethod);
         } else {
            i = new FunctionResultContext(returnType, this, inArgs);
         }

         var19 = ((FromNativeConverter)resultConverter).fromNative(var19, (FromNativeContext)i);
      }

      if(inArgs != null) {
         for(int var20 = 0; var20 < inArgs.length; ++var20) {
            Object inArg = inArgs[var20];
            if(inArg != null) {
               if(inArg instanceof Structure) {
                  if(!(inArg instanceof Structure.ByValue)) {
                     ((Structure)inArg).autoRead();
                  }
               } else if(args[var20] instanceof PostCallRead) {
                  ((PostCallRead)args[var20]).read();
                  if(args[var20] instanceof PointerArray) {
                     PointerArray array = (PointerArray)args[var20];
                     if(Structure.ByReference[].class.isAssignableFrom(inArg.getClass())) {
                        Class type = inArg.getClass().getComponentType();
                        Structure[] ss = (Structure[])((Structure[])inArg);

                        for(int si = 0; si < ss.length; ++si) {
                           Pointer p = array.getPointer((long)(Pointer.SIZE * si));
                           ss[si] = Structure.updateStructureByReference(type, ss[si], p);
                        }
                     }
                  }
               } else if(Structure[].class.isAssignableFrom(inArg.getClass())) {
                  Structure.autoRead((Structure[])((Structure[])inArg));
               }
            }
         }
      }

      return var19;
   }

   Object invoke(Object[] args, Class returnType, boolean allowObjects) {
      Object result = null;
      if(returnType != null && returnType != Void.TYPE && returnType != Void.class) {
         if(returnType != Boolean.TYPE && returnType != Boolean.class) {
            if(returnType != Byte.TYPE && returnType != Byte.class) {
               if(returnType != Short.TYPE && returnType != Short.class) {
                  if(returnType != Character.TYPE && returnType != Character.class) {
                     if(returnType != Integer.TYPE && returnType != Integer.class) {
                        if(returnType != Long.TYPE && returnType != Long.class) {
                           if(returnType != Float.TYPE && returnType != Float.class) {
                              if(returnType != Double.TYPE && returnType != Double.class) {
                                 if(returnType == String.class) {
                                    result = this.invokeString(this.callFlags, args, false);
                                 } else if(returnType == WString.class) {
                                    String p = this.invokeString(this.callFlags, args, true);
                                    if(p != null) {
                                       result = new WString(p);
                                    }
                                 } else {
                                    if(Pointer.class.isAssignableFrom(returnType)) {
                                       return this.invokePointer(this.callFlags, args);
                                    }

                                    if(Structure.class.isAssignableFrom(returnType)) {
                                       Structure var9;
                                       if(Structure.ByValue.class.isAssignableFrom(returnType)) {
                                          var9 = Native.invokeStructure(this.peer, this.callFlags, args, Structure.newInstance(returnType));
                                          var9.autoRead();
                                          result = var9;
                                       } else {
                                          result = this.invokePointer(this.callFlags, args);
                                          if(result != null) {
                                             var9 = Structure.newInstance(returnType);
                                             var9.useMemory((Pointer)result);
                                             var9.autoRead();
                                             result = var9;
                                          }
                                       }
                                    } else if(Callback.class.isAssignableFrom(returnType)) {
                                       result = this.invokePointer(this.callFlags, args);
                                       if(result != null) {
                                          result = CallbackReference.getCallback(returnType, (Pointer)result);
                                       }
                                    } else {
                                       Pointer var10;
                                       if(returnType == String[].class) {
                                          var10 = this.invokePointer(this.callFlags, args);
                                          if(var10 != null) {
                                             result = var10.getStringArray(0L);
                                          }
                                       } else if(returnType == WString[].class) {
                                          var10 = this.invokePointer(this.callFlags, args);
                                          if(var10 != null) {
                                             String[] arr = var10.getStringArray(0L, true);
                                             WString[] warr = new WString[arr.length];

                                             for(int i = 0; i < arr.length; ++i) {
                                                warr[i] = new WString(arr[i]);
                                             }

                                             result = warr;
                                          }
                                       } else if(returnType == Pointer[].class) {
                                          var10 = this.invokePointer(this.callFlags, args);
                                          if(var10 != null) {
                                             result = var10.getPointerArray(0L);
                                          }
                                       } else {
                                          if(!allowObjects) {
                                             throw new IllegalArgumentException("Unsupported return type " + returnType + " in function " + this.getName());
                                          }

                                          result = Native.invokeObject(this.peer, this.callFlags, args);
                                          if(result != null && !returnType.isAssignableFrom(result.getClass())) {
                                             throw new ClassCastException("Return type " + returnType + " does not match result " + result.getClass());
                                          }
                                       }
                                    }
                                 }
                              } else {
                                 result = new Double(Native.invokeDouble(this.peer, this.callFlags, args));
                              }
                           } else {
                              result = new Float(Native.invokeFloat(this.peer, this.callFlags, args));
                           }
                        } else {
                           result = new Long(Native.invokeLong(this.peer, this.callFlags, args));
                        }
                     } else {
                        result = new Integer(Native.invokeInt(this.peer, this.callFlags, args));
                     }
                  } else {
                     result = new Character((char)Native.invokeInt(this.peer, this.callFlags, args));
                  }
               } else {
                  result = new Short((short)Native.invokeInt(this.peer, this.callFlags, args));
               }
            } else {
               result = new Byte((byte)Native.invokeInt(this.peer, this.callFlags, args));
            }
         } else {
            result = valueOf(Native.invokeInt(this.peer, this.callFlags, args) != 0);
         }
      } else {
         Native.invokeVoid(this.peer, this.callFlags, args);
         result = null;
      }

      return result;
   }

   private Pointer invokePointer(int callFlags, Object[] args) {
      long ptr = Native.invokePointer(this.peer, callFlags, args);
      return ptr == 0L?null:new Pointer(ptr);
   }

   private Object convertArgument(Object[] args, int index, Method invokingMethod, TypeMapper mapper, boolean allowObjects) {
      Object arg = args[index];
      Class argClass;
      if(arg != null) {
         argClass = arg.getClass();
         Object ss = null;
         if(NativeMapped.class.isAssignableFrom(argClass)) {
            ss = NativeMappedConverter.getInstance(argClass);
         } else if(mapper != null) {
            ss = mapper.getToNativeConverter(argClass);
         }

         if(ss != null) {
            Object type;
            if(invokingMethod != null) {
               type = new MethodParameterContext(this, args, index, invokingMethod);
            } else {
               type = new FunctionParameterContext(this, args, index);
            }

            arg = ((ToNativeConverter)ss).toNative(arg, (ToNativeContext)type);
         }
      }

      if(arg != null && !this.isPrimitiveArray(arg.getClass())) {
         argClass = arg.getClass();
         Class var15;
         if(arg instanceof Structure) {
            Structure var14 = (Structure)arg;
            var14.autoWrite();
            if(var14 instanceof Structure.ByValue) {
               var15 = var14.getClass();
               if(invokingMethod != null) {
                  Class[] var16 = invokingMethod.getParameterTypes();
                  if(isVarArgs(invokingMethod)) {
                     if(index < var16.length - 1) {
                        var15 = var16[index];
                     } else {
                        Class var17 = var16[var16.length - 1].getComponentType();
                        if(var17 != Object.class) {
                           var15 = var17;
                        }
                     }
                  } else {
                     var15 = var16[index];
                  }
               }

               if(Structure.ByValue.class.isAssignableFrom(var15)) {
                  return var14;
               }
            }

            return var14.getPointer();
         } else if(arg instanceof Callback) {
            return CallbackReference.getFunctionPointer((Callback)arg);
         } else if(arg instanceof String) {
            return (new NativeString((String)arg, false)).getPointer();
         } else if(arg instanceof WString) {
            return (new NativeString(arg.toString(), true)).getPointer();
         } else if(arg instanceof Boolean) {
            return Boolean.TRUE.equals(arg)?INTEGER_TRUE:INTEGER_FALSE;
         } else if(String[].class == argClass) {
            return new StringArray((String[])((String[])arg));
         } else if(WString[].class == argClass) {
            return new StringArray((WString[])((WString[])arg));
         } else if(Pointer[].class == argClass) {
            return new PointerArray((Pointer[])((Pointer[])arg));
         } else if(NativeMapped[].class.isAssignableFrom(argClass)) {
            return new NativeMappedArray((NativeMapped[])((NativeMapped[])arg));
         } else if(Structure[].class.isAssignableFrom(argClass)) {
            Structure[] var13 = (Structure[])((Structure[])arg);
            var15 = argClass.getComponentType();
            boolean byRef = Structure.ByReference.class.isAssignableFrom(var15);
            if(byRef) {
               Pointer[] pointers = new Pointer[var13.length + 1];

               for(int i = 0; i < var13.length; ++i) {
                  pointers[i] = var13[i] != null?var13[i].getPointer():null;
               }

               return new PointerArray(pointers);
            } else if(var13.length == 0) {
               throw new IllegalArgumentException("Structure array must have non-zero length");
            } else if(var13[0] == null) {
               Structure.newInstance(var15).toArray(var13);
               return var13[0].getPointer();
            } else {
               Structure.autoWrite(var13);
               return var13[0].getPointer();
            }
         } else if(argClass.isArray()) {
            throw new IllegalArgumentException("Unsupported array argument type: " + argClass.getComponentType());
         } else if(allowObjects) {
            return arg;
         } else if(!Native.isSupportedNativeType(arg.getClass())) {
            throw new IllegalArgumentException("Unsupported argument type " + arg.getClass().getName() + " at parameter " + index + " of function " + this.getName());
         } else {
            return arg;
         }
      } else {
         return arg;
      }
   }

   private boolean isPrimitiveArray(Class argClass) {
      return argClass.isArray() && argClass.getComponentType().isPrimitive();
   }

   public void invoke(Object[] args) {
      this.invoke(Void.class, args);
   }

   private String invokeString(int callFlags, Object[] args, boolean wide) {
      Pointer ptr = this.invokePointer(callFlags, args);
      String s = null;
      if(ptr != null) {
         if(wide) {
            s = ptr.getString(0L, wide);
         } else {
            s = ptr.getString(0L);
         }
      }

      return s;
   }

   public String toString() {
      return this.library != null?"native function " + this.functionName + "(" + this.library.getName() + ")@0x" + Long.toHexString(this.peer):"native function@0x" + Long.toHexString(this.peer);
   }

   public Object invokeObject(Object[] args) {
      return this.invoke(Object.class, args);
   }

   public Pointer invokePointer(Object[] args) {
      return (Pointer)this.invoke(Pointer.class, args);
   }

   public String invokeString(Object[] args, boolean wide) {
      Object o = this.invoke(wide?WString.class:String.class, args);
      return o != null?o.toString():null;
   }

   public int invokeInt(Object[] args) {
      return ((Integer)this.invoke(Integer.class, args)).intValue();
   }

   public long invokeLong(Object[] args) {
      return ((Long)this.invoke(Long.class, args)).longValue();
   }

   public float invokeFloat(Object[] args) {
      return ((Float)this.invoke(Float.class, args)).floatValue();
   }

   public double invokeDouble(Object[] args) {
      return ((Double)this.invoke(Double.class, args)).doubleValue();
   }

   public void invokeVoid(Object[] args) {
      this.invoke(Void.class, args);
   }

   public boolean equals(Object o) {
      if(o == this) {
         return true;
      } else if(o == null) {
         return false;
      } else if(o.getClass() != this.getClass()) {
         return false;
      } else {
         Function other = (Function)o;
         return other.callFlags == this.callFlags && other.options.equals(this.options) && other.peer == this.peer;
      }
   }

   public int hashCode() {
      return this.callFlags + this.options.hashCode() + super.hashCode();
   }

   static Object[] concatenateVarArgs(Object[] inArgs) {
      if(inArgs != null && inArgs.length > 0) {
         Object lastArg = inArgs[inArgs.length - 1];
         Class argType = lastArg != null?lastArg.getClass():null;
         if(argType != null && argType.isArray()) {
            Object[] varArgs = (Object[])((Object[])lastArg);
            Object[] fullArgs = new Object[inArgs.length + varArgs.length];
            System.arraycopy(inArgs, 0, fullArgs, 0, inArgs.length - 1);
            System.arraycopy(varArgs, 0, fullArgs, inArgs.length - 1, varArgs.length);
            fullArgs[fullArgs.length - 1] = null;
            inArgs = fullArgs;
         }
      }

      return inArgs;
   }

   static boolean isVarArgs(Method m) {
      try {
         Method e = m.getClass().getMethod("isVarArgs", new Class[0]);
         return Boolean.TRUE.equals(e.invoke(m, new Object[0]));
      } catch (SecurityException var2) {
         ;
      } catch (NoSuchMethodException var3) {
         ;
      } catch (IllegalArgumentException var4) {
         ;
      } catch (IllegalAccessException var5) {
         ;
      } catch (InvocationTargetException var6) {
         ;
      }

      return false;
   }

   static Boolean valueOf(boolean b) {
      return b?Boolean.TRUE:Boolean.FALSE;
   }

   private static class PointerArray extends Memory implements PostCallRead {
      private final Pointer[] original;

      public PointerArray(Pointer[] arg) {
         super((long)(Pointer.SIZE * (arg.length + 1)));
         this.original = arg;

         for(int i = 0; i < arg.length; ++i) {
            this.setPointer((long)(i * Pointer.SIZE), arg[i]);
         }

         this.setPointer((long)(Pointer.SIZE * arg.length), (Pointer)null);
      }

      public void read() {
         this.read(0L, this.original, 0, this.original.length);
      }
   }

   private static class NativeMappedArray extends Memory implements PostCallRead {
      private final NativeMapped[] original;

      public NativeMappedArray(NativeMapped[] arg) {
         super((long)Native.getNativeSize(arg.getClass(), arg));
         this.original = arg;
         Class nativeType = arg.getClass().getComponentType();
         this.setValue(0L, this.original, this.original.getClass());
      }

      public void read() {
         this.getValue(0L, this.original.getClass(), this.original);
      }
   }

   public interface PostCallRead {
      void read();
   }
}
