package com.sun.jna.win32;

import com.sun.jna.FunctionMapper;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeMapped;
import com.sun.jna.NativeMappedConverter;
import com.sun.jna.Pointer;
import java.lang.reflect.Method;

public class StdCallFunctionMapper implements FunctionMapper {
   protected int getArgumentNativeStackSize(Class cls) {
      if(NativeMapped.class.isAssignableFrom(cls)) {
         cls = NativeMappedConverter.getInstance(cls).nativeType();
      }

      if(cls.isArray()) {
         return Pointer.SIZE;
      } else {
         try {
            return Native.getNativeSize(cls);
         } catch (IllegalArgumentException var3) {
            throw new IllegalArgumentException("Unknown native stack allocation size for " + cls);
         }
      }
   }

   public String getFunctionName(NativeLibrary library, Method method) {
      String name = method.getName();
      int pop = 0;
      Class[] argTypes = method.getParameterTypes();

      for(int decorated = 0; decorated < argTypes.length; ++decorated) {
         pop += this.getArgumentNativeStackSize(argTypes[decorated]);
      }

      String var12 = name + "@" + pop;
      byte conv = 1;

      try {
         name = library.getFunction(var12, conv).getName();
      } catch (UnsatisfiedLinkError var11) {
         try {
            name = library.getFunction("_" + var12, conv).getName();
         } catch (UnsatisfiedLinkError var10) {
            ;
         }
      }

      return name;
   }
}
