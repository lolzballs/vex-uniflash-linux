package com.sun.jna;

import com.sun.jna.AltCallingConvention;
import com.sun.jna.Callback;
import com.sun.jna.CallbackParameterContext;
import com.sun.jna.CallbackProxy;
import com.sun.jna.CallbackResultContext;
import com.sun.jna.FromNativeConverter;
import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeMapped;
import com.sun.jna.NativeMappedConverter;
import com.sun.jna.NativeString;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.Structure;
import com.sun.jna.ToNativeConverter;
import com.sun.jna.TypeMapper;
import com.sun.jna.WString;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

class CallbackReference extends WeakReference {
   static final Map callbackMap = new WeakHashMap();
   static final Map directCallbackMap = new WeakHashMap();
   static final Map allocations = new WeakHashMap();
   private static final Method PROXY_CALLBACK_METHOD;
   Pointer cbstruct;
   CallbackProxy proxy;
   Method method;

   public static Callback getCallback(Class type, Pointer p) {
      return getCallback(type, p, false);
   }

   private static Callback getCallback(Class type, Pointer p, boolean direct) {
      if(p == null) {
         return null;
      } else if(!type.isInterface()) {
         throw new IllegalArgumentException("Callback type must be an interface");
      } else {
         Map map = direct?directCallbackMap:callbackMap;
         synchronized(map) {
            Iterator ctype = map.keySet().iterator();

            while(ctype.hasNext()) {
               Callback foptions = (Callback)ctype.next();
               if(type.isAssignableFrom(foptions.getClass())) {
                  CallbackReference options = (CallbackReference)map.get(foptions);
                  Pointer h = options != null?options.getTrampoline():getNativeFunctionPointer(foptions);
                  if(p.equals(h)) {
                     return foptions;
                  }
               }
            }

            int ctype1 = AltCallingConvention.class.isAssignableFrom(type)?1:0;
            HashMap foptions1 = new HashMap();
            Map options1 = Native.getLibraryOptions(type);
            if(options1 != null) {
               foptions1.putAll(options1);
            }

            foptions1.put("invoking-method", getCallbackMethod(type));
            NativeFunctionHandler h1 = new NativeFunctionHandler(p, ctype1, foptions1);
            Callback cb = (Callback)Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, h1);
            map.put(cb, (Object)null);
            return cb;
         }
      }
   }

   private CallbackReference(Callback callback, int callingConvention, boolean direct) {
      super(callback);
      TypeMapper mapper = Native.getTypeMapper(callback.getClass());
      String arch = System.getProperty("os.arch").toLowerCase();
      boolean ppc = "ppc".equals(arch) || "powerpc".equals(arch);
      if(direct) {
         Method peer = getCallbackMethod(callback);
         Class[] msg = peer.getParameterTypes();

         for(int i = 0; i < msg.length; ++i) {
            if(ppc && (msg[i] == Float.TYPE || msg[i] == Double.TYPE)) {
               direct = false;
               break;
            }

            if(mapper != null && mapper.getFromNativeConverter(msg[i]) != null) {
               direct = false;
               break;
            }
         }

         if(mapper != null && mapper.getToNativeConverter(peer.getReturnType()) != null) {
            direct = false;
         }
      }

      Class[] nativeParamTypes;
      Class returnType;
      long var12;
      if(direct) {
         this.method = getCallbackMethod(callback);
         nativeParamTypes = this.method.getParameterTypes();
         returnType = this.method.getReturnType();
         var12 = Native.createNativeCallback(callback, this.method, nativeParamTypes, returnType, callingConvention, true);
         this.cbstruct = var12 != 0L?new Pointer(var12):null;
      } else {
         if(callback instanceof CallbackProxy) {
            this.proxy = (CallbackProxy)callback;
         } else {
            this.proxy = new DefaultCallbackProxy(getCallbackMethod(callback), mapper);
         }

         nativeParamTypes = this.proxy.getParameterTypes();
         returnType = this.proxy.getReturnType();
         int var13;
         if(mapper != null) {
            for(var13 = 0; var13 < nativeParamTypes.length; ++var13) {
               FromNativeConverter var15 = mapper.getFromNativeConverter(nativeParamTypes[var13]);
               if(var15 != null) {
                  nativeParamTypes[var13] = var15.nativeType();
               }
            }

            ToNativeConverter var14 = mapper.getToNativeConverter(returnType);
            if(var14 != null) {
               returnType = var14.nativeType();
            }
         }

         for(var13 = 0; var13 < nativeParamTypes.length; ++var13) {
            nativeParamTypes[var13] = this.getNativeType(nativeParamTypes[var13]);
            if(!isAllowableNativeType(nativeParamTypes[var13])) {
               String var16 = "Callback argument " + nativeParamTypes[var13] + " requires custom type conversion";
               throw new IllegalArgumentException(var16);
            }
         }

         returnType = this.getNativeType(returnType);
         if(!isAllowableNativeType(returnType)) {
            String var17 = "Callback return type " + returnType + " requires custom type conversion";
            throw new IllegalArgumentException(var17);
         }

         var12 = Native.createNativeCallback(this.proxy, PROXY_CALLBACK_METHOD, nativeParamTypes, returnType, callingConvention, false);
         this.cbstruct = var12 != 0L?new Pointer(var12):null;
      }

   }

   private Class getNativeType(Class cls) {
      if(Structure.class.isAssignableFrom(cls)) {
         Structure.newInstance(cls);
         if(!Structure.ByValue.class.isAssignableFrom(cls)) {
            return Pointer.class;
         }
      } else {
         if(NativeMapped.class.isAssignableFrom(cls)) {
            return NativeMappedConverter.getInstance(cls).nativeType();
         }

         if(cls == String.class || cls == WString.class || cls == String[].class || cls == WString[].class || Callback.class.isAssignableFrom(cls)) {
            return Pointer.class;
         }
      }

      return cls;
   }

   private static Method checkMethod(Method m) {
      if(m.getParameterTypes().length > 256) {
         String msg = "Method signature exceeds the maximum parameter count: " + m;
         throw new UnsupportedOperationException(msg);
      } else {
         return m;
      }
   }

   static Class findCallbackClass(Class type) {
      if(!Callback.class.isAssignableFrom(type)) {
         throw new IllegalArgumentException(type.getName() + " is not derived from com.sun.jna.Callback");
      } else if(type.isInterface()) {
         return type;
      } else {
         Class[] ifaces = type.getInterfaces();
         int i = 0;

         while(true) {
            if(i < ifaces.length) {
               if(!Callback.class.isAssignableFrom(ifaces[i])) {
                  ++i;
                  continue;
               }

               try {
                  getCallbackMethod(ifaces[i]);
                  return ifaces[i];
               } catch (IllegalArgumentException var4) {
                  ;
               }
            }

            if(Callback.class.isAssignableFrom(type.getSuperclass())) {
               return findCallbackClass(type.getSuperclass());
            }

            return type;
         }
      }
   }

   private static Method getCallbackMethod(Callback callback) {
      return getCallbackMethod(findCallbackClass(callback.getClass()));
   }

   private static Method getCallbackMethod(Class cls) {
      Method[] pubMethods = cls.getDeclaredMethods();
      Method[] classMethods = cls.getMethods();
      HashSet pmethods = new HashSet(Arrays.asList(pubMethods));
      pmethods.retainAll(Arrays.asList(classMethods));
      Iterator methods = pmethods.iterator();

      while(methods.hasNext()) {
         Method msg = (Method)methods.next();
         if(Callback.FORBIDDEN_NAMES.contains(msg.getName())) {
            methods.remove();
         }
      }

      Method[] var7 = (Method[])((Method[])pmethods.toArray(new Method[pmethods.size()]));
      if(var7.length == 1) {
         return checkMethod(var7[0]);
      } else {
         for(int var8 = 0; var8 < var7.length; ++var8) {
            Method m = var7[var8];
            if("callback".equals(m.getName())) {
               return checkMethod(m);
            }
         }

         String var9 = "Callback must implement a single public method, or one public method named \'callback\'";
         throw new IllegalArgumentException(var9);
      }
   }

   public Pointer getTrampoline() {
      return this.cbstruct.getPointer(0L);
   }

   protected void finalize() {
      this.dispose();
   }

   protected synchronized void dispose() {
      if(this.cbstruct != null) {
         Native.freeNativeCallback(this.cbstruct.peer);
         this.cbstruct.peer = 0L;
         this.cbstruct = null;
      }

   }

   private Callback getCallback() {
      return (Callback)this.get();
   }

   private static Pointer getNativeFunctionPointer(Callback cb) {
      if(Proxy.isProxyClass(cb.getClass())) {
         InvocationHandler handler = Proxy.getInvocationHandler(cb);
         if(handler instanceof NativeFunctionHandler) {
            return ((NativeFunctionHandler)handler).getPointer();
         }
      }

      return null;
   }

   public static Pointer getFunctionPointer(Callback cb) {
      return getFunctionPointer(cb, false);
   }

   private static Pointer getFunctionPointer(Callback cb, boolean direct) {
      Pointer fp = null;
      if(cb == null) {
         return null;
      } else if((fp = getNativeFunctionPointer(cb)) != null) {
         return fp;
      } else {
         int callingConvention = cb instanceof AltCallingConvention?1:0;
         Map map = direct?directCallbackMap:callbackMap;
         synchronized(map) {
            CallbackReference cbref = (CallbackReference)map.get(cb);
            if(cbref == null) {
               cbref = new CallbackReference(cb, callingConvention, direct);
               map.put(cb, cbref);
            }

            return cbref.getTrampoline();
         }
      }
   }

   private static boolean isAllowableNativeType(Class cls) {
      return cls == Void.TYPE || cls == Void.class || cls == Boolean.TYPE || cls == Boolean.class || cls == Byte.TYPE || cls == Byte.class || cls == Short.TYPE || cls == Short.class || cls == Character.TYPE || cls == Character.class || cls == Integer.TYPE || cls == Integer.class || cls == Long.TYPE || cls == Long.class || cls == Float.TYPE || cls == Float.class || cls == Double.TYPE || cls == Double.class || Structure.ByValue.class.isAssignableFrom(cls) && Structure.class.isAssignableFrom(cls) || Pointer.class.isAssignableFrom(cls);
   }

   private static Pointer getNativeString(Object value, boolean wide) {
      if(value != null) {
         NativeString ns = new NativeString(value.toString(), wide);
         allocations.put(value, ns);
         return ns.getPointer();
      } else {
         return null;
      }
   }

   static {
      try {
         PROXY_CALLBACK_METHOD = CallbackProxy.class.getMethod("callback", new Class[]{Object[].class});
      } catch (Exception var1) {
         throw new Error("Error looking up CallbackProxy.callback() method");
      }
   }

   private static class NativeFunctionHandler implements InvocationHandler {
      private Function function;
      private Map options;

      public NativeFunctionHandler(Pointer address, int callingConvention, Map options) {
         this.function = new Function(address, callingConvention);
         this.options = options;
      }

      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         if(Library.Handler.OBJECT_TOSTRING.equals(method)) {
            String o1 = "Proxy interface to " + this.function;
            Method m = (Method)this.options.get("invoking-method");
            Class cls = CallbackReference.findCallbackClass(m.getDeclaringClass());
            o1 = o1 + " (" + cls.getName() + ")";
            return o1;
         } else if(Library.Handler.OBJECT_HASHCODE.equals(method)) {
            return new Integer(this.hashCode());
         } else if(Library.Handler.OBJECT_EQUALS.equals(method)) {
            Object o = args[0];
            return o != null && Proxy.isProxyClass(o.getClass())?Function.valueOf(Proxy.getInvocationHandler(o) == this):Boolean.FALSE;
         } else {
            if(Function.isVarArgs(method)) {
               args = Function.concatenateVarArgs(args);
            }

            return this.function.invoke(method.getReturnType(), args, this.options);
         }
      }

      public Pointer getPointer() {
         return this.function;
      }
   }

   private class DefaultCallbackProxy implements CallbackProxy {
      private Method callbackMethod;
      private ToNativeConverter toNative;
      private FromNativeConverter[] fromNative;

      public DefaultCallbackProxy(Method callbackMethod, TypeMapper mapper) {
         this.callbackMethod = callbackMethod;
         Class[] argTypes = callbackMethod.getParameterTypes();
         Class returnType = callbackMethod.getReturnType();
         this.fromNative = new FromNativeConverter[argTypes.length];
         if(NativeMapped.class.isAssignableFrom(returnType)) {
            this.toNative = NativeMappedConverter.getInstance(returnType);
         } else if(mapper != null) {
            this.toNative = mapper.getToNativeConverter(returnType);
         }

         for(int e = 0; e < this.fromNative.length; ++e) {
            if(NativeMapped.class.isAssignableFrom(argTypes[e])) {
               this.fromNative[e] = new NativeMappedConverter(argTypes[e]);
            } else if(mapper != null) {
               this.fromNative[e] = mapper.getFromNativeConverter(argTypes[e]);
            }
         }

         if(!callbackMethod.isAccessible()) {
            try {
               callbackMethod.setAccessible(true);
            } catch (SecurityException var7) {
               throw new IllegalArgumentException("Callback method is inaccessible, make sure the interface is public: " + callbackMethod);
            }
         }

      }

      private Object invokeCallback(Object[] args) {
         Class[] paramTypes = this.callbackMethod.getParameterTypes();
         Object[] callbackArgs = new Object[args.length];

         for(int result = 0; result < args.length; ++result) {
            Class cb = paramTypes[result];
            Object i = args[result];
            if(this.fromNative[result] != null) {
               CallbackParameterContext context = new CallbackParameterContext(cb, this.callbackMethod, args, result);
               callbackArgs[result] = this.fromNative[result].fromNative(i, context);
            } else {
               callbackArgs[result] = this.convertArgument(i, cb);
            }
         }

         Object var11 = null;
         Callback var12 = CallbackReference.this.getCallback();
         if(var12 != null) {
            try {
               var11 = this.convertResult(this.callbackMethod.invoke(var12, callbackArgs));
            } catch (IllegalArgumentException var8) {
               Native.getCallbackExceptionHandler().uncaughtException(var12, var8);
            } catch (IllegalAccessException var9) {
               Native.getCallbackExceptionHandler().uncaughtException(var12, var9);
            } catch (InvocationTargetException var10) {
               Native.getCallbackExceptionHandler().uncaughtException(var12, var10.getTargetException());
            }
         }

         for(int var13 = 0; var13 < callbackArgs.length; ++var13) {
            if(callbackArgs[var13] instanceof Structure && !(callbackArgs[var13] instanceof Structure.ByValue)) {
               ((Structure)callbackArgs[var13]).autoWrite();
            }
         }

         return var11;
      }

      public Object callback(Object[] args) {
         try {
            return this.invokeCallback(args);
         } catch (Throwable var3) {
            Native.getCallbackExceptionHandler().uncaughtException(CallbackReference.this.getCallback(), var3);
            return null;
         }
      }

      private Object convertArgument(Object value, Class dstType) {
         if(value instanceof Pointer) {
            if(dstType == String.class) {
               value = ((Pointer)value).getString(0L);
            } else if(dstType == WString.class) {
               value = new WString(((Pointer)value).getString(0L, true));
            } else if(dstType != String[].class && dstType != WString[].class) {
               if(Callback.class.isAssignableFrom(dstType)) {
                  value = CallbackReference.getCallback(dstType, (Pointer)value);
               } else if(Structure.class.isAssignableFrom(dstType)) {
                  Structure s = Structure.newInstance(dstType);
                  if(Structure.ByValue.class.isAssignableFrom(dstType)) {
                     byte[] buf = new byte[s.size()];
                     ((Pointer)value).read(0L, (byte[])buf, 0, buf.length);
                     s.getPointer().write(0L, (byte[])buf, 0, buf.length);
                  } else {
                     s.useMemory((Pointer)value);
                  }

                  s.read();
                  value = s;
               }
            } else {
               value = ((Pointer)value).getStringArray(0L, dstType == WString[].class);
            }
         } else if((Boolean.TYPE == dstType || Boolean.class == dstType) && value instanceof Number) {
            value = Function.valueOf(((Number)value).intValue() != 0);
         }

         return value;
      }

      private Object convertResult(Object value) {
         if(this.toNative != null) {
            value = this.toNative.toNative(value, new CallbackResultContext(this.callbackMethod));
         }

         if(value == null) {
            return null;
         } else {
            Class cls = value.getClass();
            if(Structure.class.isAssignableFrom(cls)) {
               return Structure.ByValue.class.isAssignableFrom(cls)?value:((Structure)value).getPointer();
            } else if(cls != Boolean.TYPE && cls != Boolean.class) {
               if(cls != String.class && cls != WString.class) {
                  if(cls != String[].class && cls != WString.class) {
                     return Callback.class.isAssignableFrom(cls)?CallbackReference.getFunctionPointer((Callback)value):value;
                  } else {
                     StringArray sa = cls == String[].class?new StringArray((String[])((String[])value)):new StringArray((WString[])((WString[])value));
                     CallbackReference.allocations.put(value, sa);
                     return sa;
                  }
               } else {
                  return CallbackReference.getNativeString(value, cls == WString.class);
               }
            } else {
               return Boolean.TRUE.equals(value)?Function.INTEGER_TRUE:Function.INTEGER_FALSE;
            }
         }
      }

      public Class[] getParameterTypes() {
         return this.callbackMethod.getParameterTypes();
      }

      public Class getReturnType() {
         return this.callbackMethod.getReturnType();
      }
   }
}
