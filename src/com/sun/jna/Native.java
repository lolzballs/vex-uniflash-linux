package com.sun.jna;

import com.sun.jna.Callback;
import com.sun.jna.CallbackReference;
import com.sun.jna.FromNativeContext;
import com.sun.jna.FromNativeConverter;
import com.sun.jna.Function;
import com.sun.jna.FunctionMapper;
import com.sun.jna.IntegerType;
import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeMapped;
import com.sun.jna.NativeMappedConverter;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import com.sun.jna.ToNativeContext;
import com.sun.jna.ToNativeConverter;
import com.sun.jna.TypeMapper;
import com.sun.jna.WString;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Window;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;

public final class Native {
   private static final String VERSION = "3.3.0";
   private static String nativeLibraryPath = null;
   private static boolean unpacked;
   private static Map typeMappers = new WeakHashMap();
   private static Map alignments = new WeakHashMap();
   private static Map options = new WeakHashMap();
   private static Map libraries = new WeakHashMap();
   private static final Callback.UncaughtExceptionHandler DEFAULT_HANDLER = new Callback.UncaughtExceptionHandler() {
      public void uncaughtException(Callback c, Throwable e) {
         System.err.println("JNA: Callback " + c + " threw the following exception:");
         e.printStackTrace();
      }
   };
   private static Callback.UncaughtExceptionHandler callbackExceptionHandler;
   public static final int POINTER_SIZE;
   public static final int LONG_SIZE;
   public static final int WCHAR_SIZE;
   public static final int SIZE_T_SIZE;
   private static final int TYPE_VOIDP = 0;
   private static final int TYPE_LONG = 1;
   private static final int TYPE_WCHAR_T = 2;
   private static final int TYPE_SIZE_T = 3;
   private static final Object finalizer;
   private static final ThreadLocal lastError;
   private static Map registeredClasses;
   private static Map registeredLibraries;
   private static Object unloader;
   private static final int CVT_UNSUPPORTED = -1;
   private static final int CVT_DEFAULT = 0;
   private static final int CVT_POINTER = 1;
   private static final int CVT_STRING = 2;
   private static final int CVT_STRUCTURE = 3;
   private static final int CVT_STRUCTURE_BYVAL = 4;
   private static final int CVT_BUFFER = 5;
   private static final int CVT_ARRAY_BYTE = 6;
   private static final int CVT_ARRAY_SHORT = 7;
   private static final int CVT_ARRAY_CHAR = 8;
   private static final int CVT_ARRAY_INT = 9;
   private static final int CVT_ARRAY_LONG = 10;
   private static final int CVT_ARRAY_FLOAT = 11;
   private static final int CVT_ARRAY_DOUBLE = 12;
   private static final int CVT_ARRAY_BOOLEAN = 13;
   private static final int CVT_BOOLEAN = 14;
   private static final int CVT_CALLBACK = 15;
   private static final int CVT_FLOAT = 16;
   private static final int CVT_NATIVE_MAPPED = 17;
   private static final int CVT_WSTRING = 18;
   private static final int CVT_INTEGER_TYPE = 19;
   private static final int CVT_POINTER_TYPE = 20;
   private static final int CVT_TYPE_MAPPER = 21;

   private static void dispose() {
      NativeLibrary.disposeAll();
      deleteNativeLibrary();
   }

   private static boolean deleteNativeLibrary() {
      String path = nativeLibraryPath;
      if(path != null && unpacked) {
         File flib = new File(path);
         if(flib.delete()) {
            nativeLibraryPath = null;
            unpacked = false;
            return true;
         } else {
            try {
               ClassLoader e = Native.class.getClassLoader();
               Field f = ClassLoader.class.getDeclaredField("nativeLibraries");
               f.setAccessible(true);
               List libs = (List)f.get(e);
               Iterator i = libs.iterator();

               Object lib;
               String name;
               do {
                  if(!i.hasNext()) {
                     return false;
                  }

                  lib = i.next();
                  f = lib.getClass().getDeclaredField("name");
                  f.setAccessible(true);
                  name = (String)f.get(lib);
               } while(!name.equals(path) && name.indexOf(path) == -1 && !name.equals(flib.getCanonicalPath()));

               Method m = lib.getClass().getDeclaredMethod("finalize", new Class[0]);
               m.setAccessible(true);
               m.invoke(lib, new Object[0]);
               nativeLibraryPath = null;
               if(unpacked && flib.exists()) {
                  if(flib.delete()) {
                     unpacked = false;
                     return true;
                  } else {
                     return false;
                  }
               } else {
                  return true;
               }
            } catch (Exception var9) {
               return false;
            }
         }
      } else {
         return true;
      }
   }

   private static native void initIDs();

   public static synchronized native void setProtected(boolean var0);

   public static synchronized native boolean isProtected();

   /** @deprecated */
   public static synchronized native void setPreserveLastError(boolean var0);

   /** @deprecated */
   public static synchronized native boolean getPreserveLastError();

   public static long getWindowID(Window w) throws HeadlessException {
      return getComponentID(w);
   }

   public static long getComponentID(Component c) throws HeadlessException {
      if(GraphicsEnvironment.isHeadless()) {
         throw new HeadlessException("No native windows when headless");
      } else if(c.isLightweight()) {
         throw new IllegalArgumentException("Component must be heavyweight");
      } else if(!c.isDisplayable()) {
         throw new IllegalStateException("Component must be displayable");
      } else if(Platform.isX11() && System.getProperty("java.version").startsWith("1.4") && !c.isVisible()) {
         throw new IllegalStateException("Component must be visible");
      } else {
         return getWindowHandle0(c);
      }
   }

   public static Pointer getWindowPointer(Window w) throws HeadlessException {
      return getComponentPointer(w);
   }

   public static Pointer getComponentPointer(Component c) throws HeadlessException {
      return new Pointer(getComponentID(c));
   }

   private static native long getWindowHandle0(Component var0);

   public static Pointer getDirectBufferPointer(Buffer b) {
      long peer = _getDirectBufferPointer(b);
      return peer == 0L?null:new Pointer(peer);
   }

   private static native long _getDirectBufferPointer(Buffer var0);

   public static String toString(byte[] buf) {
      return toString(buf, System.getProperty("jna.encoding"));
   }

   public static String toString(byte[] buf, String encoding) {
      String s = null;
      if(encoding != null) {
         try {
            s = new String(buf, encoding);
         } catch (UnsupportedEncodingException var4) {
            ;
         }
      }

      if(s == null) {
         s = new String(buf);
      }

      int term = s.indexOf(0);
      if(term != -1) {
         s = s.substring(0, term);
      }

      return s;
   }

   public static String toString(char[] buf) {
      String s = new String(buf);
      int term = s.indexOf(0);
      if(term != -1) {
         s = s.substring(0, term);
      }

      return s;
   }

   public static Object loadLibrary(Class interfaceClass) {
      return loadLibrary((String)null, (Class)interfaceClass);
   }

   public static Object loadLibrary(Class interfaceClass, Map options) {
      return loadLibrary((String)null, interfaceClass, options);
   }

   public static Object loadLibrary(String name, Class interfaceClass) {
      return loadLibrary(name, interfaceClass, Collections.EMPTY_MAP);
   }

   public static Object loadLibrary(String name, Class interfaceClass, Map options) {
      Library.Handler handler = new Library.Handler(name, interfaceClass, options);
      ClassLoader loader = interfaceClass.getClassLoader();
      Library proxy = (Library)Proxy.newProxyInstance(loader, new Class[]{interfaceClass}, handler);
      cacheOptions(interfaceClass, options, proxy);
      return proxy;
   }

   private static void loadLibraryInstance(Class cls) {
      if(cls != null && !libraries.containsKey(cls)) {
         try {
            Field[] e = cls.getFields();

            for(int i = 0; i < e.length; ++i) {
               Field field = e[i];
               if(field.getType() == cls && Modifier.isStatic(field.getModifiers())) {
                  libraries.put(cls, new WeakReference(field.get((Object)null)));
                  break;
               }
            }
         } catch (Exception var4) {
            throw new IllegalArgumentException("Could not access instance of " + cls + " (" + var4 + ")");
         }
      }

   }

   static Class findEnclosingLibraryClass(Class cls) {
      if(cls == null) {
         return null;
      } else {
         Map declaring = libraries;
         synchronized(libraries) {
            if(options.containsKey(cls)) {
               return cls;
            }
         }

         if(Library.class.isAssignableFrom(cls)) {
            return cls;
         } else {
            if(Callback.class.isAssignableFrom(cls)) {
               cls = CallbackReference.findCallbackClass(cls);
            }

            Class declaring1 = cls.getDeclaringClass();
            Class fromDeclaring = findEnclosingLibraryClass(declaring1);
            return fromDeclaring != null?fromDeclaring:findEnclosingLibraryClass(cls.getSuperclass());
         }
      }
   }

   public static Map getLibraryOptions(Class type) {
      Map var1 = libraries;
      synchronized(libraries) {
         Class interfaceClass = findEnclosingLibraryClass(type);
         if(interfaceClass != null) {
            loadLibraryInstance(interfaceClass);
         } else {
            interfaceClass = type;
         }

         if(!options.containsKey(interfaceClass)) {
            try {
               Field e = interfaceClass.getField("OPTIONS");
               e.setAccessible(true);
               options.put(interfaceClass, e.get((Object)null));
            } catch (NoSuchFieldException var5) {
               ;
            } catch (Exception var6) {
               throw new IllegalArgumentException("OPTIONS must be a public field of type java.util.Map (" + var6 + "): " + interfaceClass);
            }
         }

         return (Map)options.get(interfaceClass);
      }
   }

   public static TypeMapper getTypeMapper(Class cls) {
      Map var1 = libraries;
      synchronized(libraries) {
         Class interfaceClass = findEnclosingLibraryClass(cls);
         if(interfaceClass != null) {
            loadLibraryInstance(interfaceClass);
         } else {
            interfaceClass = cls;
         }

         if(!typeMappers.containsKey(interfaceClass)) {
            try {
               Field e = interfaceClass.getField("TYPE_MAPPER");
               e.setAccessible(true);
               typeMappers.put(interfaceClass, e.get((Object)null));
            } catch (NoSuchFieldException var6) {
               Map options = getLibraryOptions(cls);
               if(options != null && options.containsKey("type-mapper")) {
                  typeMappers.put(interfaceClass, options.get("type-mapper"));
               }
            } catch (Exception var7) {
               throw new IllegalArgumentException("TYPE_MAPPER must be a public field of type " + TypeMapper.class.getName() + " (" + var7 + "): " + interfaceClass);
            }
         }

         return (TypeMapper)typeMappers.get(interfaceClass);
      }
   }

   public static int getStructureAlignment(Class cls) {
      Map var1 = libraries;
      synchronized(libraries) {
         Class interfaceClass = findEnclosingLibraryClass(cls);
         if(interfaceClass != null) {
            loadLibraryInstance(interfaceClass);
         } else {
            interfaceClass = cls;
         }

         if(!alignments.containsKey(interfaceClass)) {
            try {
               Field value = interfaceClass.getField("STRUCTURE_ALIGNMENT");
               value.setAccessible(true);
               alignments.put(interfaceClass, value.get((Object)null));
            } catch (NoSuchFieldException var6) {
               Map options = getLibraryOptions(interfaceClass);
               if(options != null && options.containsKey("structure-alignment")) {
                  alignments.put(interfaceClass, options.get("structure-alignment"));
               }
            } catch (Exception var7) {
               throw new IllegalArgumentException("STRUCTURE_ALIGNMENT must be a public field of type int (" + var7 + "): " + interfaceClass);
            }
         }

         Integer value1 = (Integer)alignments.get(interfaceClass);
         return value1 != null?value1.intValue():0;
      }
   }

   static byte[] getBytes(String s) {
      try {
         return getBytes(s, System.getProperty("jna.encoding"));
      } catch (UnsupportedEncodingException var2) {
         return s.getBytes();
      }
   }

   static byte[] getBytes(String s, String encoding) throws UnsupportedEncodingException {
      return encoding != null?s.getBytes(encoding):s.getBytes();
   }

   public static byte[] toByteArray(String s) {
      byte[] bytes = getBytes(s);
      byte[] buf = new byte[bytes.length + 1];
      System.arraycopy(bytes, 0, buf, 0, bytes.length);
      return buf;
   }

   public static byte[] toByteArray(String s, String encoding) throws UnsupportedEncodingException {
      byte[] bytes = getBytes(s, encoding);
      byte[] buf = new byte[bytes.length + 1];
      System.arraycopy(bytes, 0, buf, 0, bytes.length);
      return buf;
   }

   public static char[] toCharArray(String s) {
      char[] chars = s.toCharArray();
      char[] buf = new char[chars.length + 1];
      System.arraycopy(chars, 0, buf, 0, chars.length);
      return buf;
   }

   static String getNativeLibraryResourcePath(int osType, String arch, String name) {
      arch = arch.toLowerCase();
      String osPrefix;
      switch(osType) {
      case 0:
         osPrefix = "darwin";
         break;
      case 1:
         if("x86".equals(arch)) {
            arch = "i386";
         } else if("x86_64".equals(arch)) {
            arch = "amd64";
         }

         osPrefix = "linux-" + arch;
         break;
      case 2:
         if("i386".equals(arch)) {
            arch = "x86";
         }

         osPrefix = "win32-" + arch;
         break;
      case 3:
         osPrefix = "sunos-" + arch;
         break;
      default:
         osPrefix = name.toLowerCase();
         if("x86".equals(arch)) {
            arch = "i386";
         }

         if("x86_64".equals(arch)) {
            arch = "amd64";
         }

         if("powerpc".equals(arch)) {
            arch = "ppc";
         }

         int space = osPrefix.indexOf(" ");
         if(space != -1) {
            osPrefix = osPrefix.substring(0, space);
         }

         osPrefix = osPrefix + "-" + arch;
      }

      return "/com/sun/jna/" + osPrefix;
   }

   private static void loadNativeLibrary() {
      String libName = "jnidispatch";
      String bootPath = System.getProperty("jna.boot.library.path");
      if(bootPath != null) {
         String[] e = bootPath.split(File.pathSeparator);
         int i = 0;

         while(i < e.length) {
            String path = (new File(new File(e[i]), System.mapLibraryName(libName))).getAbsolutePath();

            try {
               System.load(path);
               nativeLibraryPath = path;
               return;
            } catch (UnsatisfiedLinkError var10) {
               if(Platform.isMac()) {
                  String orig;
                  String ext;
                  if(path.endsWith("dylib")) {
                     orig = "dylib";
                     ext = "jnilib";
                  } else {
                     orig = "jnilib";
                     ext = "dylib";
                  }

                  try {
                     path = path.substring(0, path.lastIndexOf(orig)) + ext;
                     System.load(path);
                     nativeLibraryPath = path;
                     return;
                  } catch (UnsatisfiedLinkError var9) {
                     ;
                  }
               }

               ++i;
            }
         }
      }

      try {
         System.loadLibrary(libName);
         nativeLibraryPath = libName;
      } catch (UnsatisfiedLinkError var8) {
         loadNativeLibraryFromJar();
      }

   }

   private static void loadNativeLibraryFromJar() {
      String libname = System.mapLibraryName("jnidispatch");
      String arch = System.getProperty("os.arch");
      String name = System.getProperty("os.name");
      String resourceName = getNativeLibraryResourcePath(Platform.getOSType(), arch, name) + "/" + libname;
      URL url = Native.class.getResource(resourceName);
      if(url == null && Platform.isMac() && resourceName.endsWith(".dylib")) {
         resourceName = resourceName.substring(0, resourceName.lastIndexOf(".dylib")) + ".jnilib";
         url = Native.class.getResource(resourceName);
      }

      if(url == null) {
         throw new UnsatisfiedLinkError("jnidispatch (" + resourceName + ") not found in resource path");
      } else {
         File lib = null;
         if(url.getProtocol().toLowerCase().equals("file")) {
            try {
               lib = new File(new URI(url.toString()));
            } catch (URISyntaxException var22) {
               lib = new File(url.getPath());
            }

            if(!lib.exists()) {
               throw new Error("File URL " + url + " could not be properly decoded");
            }
         } else {
            InputStream is = Native.class.getResourceAsStream(resourceName);
            if(is == null) {
               throw new Error("Can\'t obtain jnidispatch InputStream");
            }

            FileOutputStream fos = null;

            try {
               lib = File.createTempFile("jna", Platform.isWindows()?".dll":null);
               lib.deleteOnExit();
               ClassLoader e = Native.class.getClassLoader();
               if(Platform.deleteNativeLibraryAfterVMExit() && (e == null || e.equals(ClassLoader.getSystemClassLoader()))) {
                  Runtime.getRuntime().addShutdownHook(new DeleteNativeLibrary(lib));
               }

               fos = new FileOutputStream(lib);
               byte[] buf = new byte[1024];

               int count;
               while((count = is.read(buf, 0, buf.length)) > 0) {
                  fos.write(buf, 0, count);
               }
            } catch (IOException var23) {
               throw new Error("Failed to create temporary file for jnidispatch library: " + var23);
            } finally {
               try {
                  is.close();
               } catch (IOException var21) {
                  ;
               }

               if(fos != null) {
                  try {
                     fos.close();
                  } catch (IOException var20) {
                     ;
                  }
               }

            }

            unpacked = true;
         }

         System.load(lib.getAbsolutePath());
         nativeLibraryPath = lib.getAbsolutePath();
      }
   }

   private static native int sizeof(int var0);

   private static native String getNativeVersion();

   private static native String getAPIChecksum();

   public static int getLastError() {
      return ((Integer)lastError.get()).intValue();
   }

   public static native void setLastError(int var0);

   static void updateLastError(int e) {
      lastError.set(new Integer(e));
   }

   public static Library synchronizedLibrary(final Library library) {
      Class cls = library.getClass();
      if(!Proxy.isProxyClass(cls)) {
         throw new IllegalArgumentException("Library must be a proxy class");
      } else {
         InvocationHandler ih = Proxy.getInvocationHandler(library);
         if(!(ih instanceof Library.Handler)) {
            throw new IllegalArgumentException("Unrecognized proxy handler: " + ih);
         } else {
            final Library.Handler handler = (Library.Handler)ih;
            InvocationHandler newHandler = new InvocationHandler() {
               public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                  synchronized(handler.getNativeLibrary()) {
                     return handler.invoke(library, method, args);
                  }
               }
            };
            return (Library)Proxy.newProxyInstance(cls.getClassLoader(), cls.getInterfaces(), newHandler);
         }
      }
   }

   public static String getWebStartLibraryPath(String libName) {
      if(System.getProperty("javawebstart.version") == null) {
         return null;
      } else {
         try {
            ClassLoader e = Native.class.getClassLoader();
            Method m = (Method)AccessController.doPrivileged(new PrivilegedAction() {
               public Object run() {
                  try {
                     Method e = ClassLoader.class.getDeclaredMethod("findLibrary", new Class[]{String.class});
                     e.setAccessible(true);
                     return e;
                  } catch (Exception var2) {
                     return null;
                  }
               }
            });
            String libpath = (String)m.invoke(e, new Object[]{libName});
            return libpath != null?(new File(libpath)).getParent():null;
         } catch (Exception var4) {
            return null;
         }
      }
   }

   public static int getNativeSize(Class type, Object value) {
      if(type.isArray()) {
         int e = Array.getLength(value);
         if(e > 0) {
            Object o = Array.get(value, 0);
            return e * getNativeSize(type.getComponentType(), o);
         } else {
            throw new IllegalArgumentException("Arrays of length zero not allowed: " + type);
         }
      } else if(Structure.class.isAssignableFrom(type) && !Structure.ByReference.class.isAssignableFrom(type)) {
         if(value == null) {
            value = Structure.newInstance(type);
         }

         return ((Structure)value).size();
      } else {
         try {
            return getNativeSize(type);
         } catch (IllegalArgumentException var4) {
            throw new IllegalArgumentException("The type \"" + type.getName() + "\" is not supported: " + var4.getMessage());
         }
      }
   }

   public static int getNativeSize(Class cls) {
      if(NativeMapped.class.isAssignableFrom(cls)) {
         cls = NativeMappedConverter.getInstance(cls).nativeType();
      }

      if(cls != Boolean.TYPE && cls != Boolean.class) {
         if(cls != Byte.TYPE && cls != Byte.class) {
            if(cls != Short.TYPE && cls != Short.class) {
               if(cls != Character.TYPE && cls != Character.class) {
                  if(cls != Integer.TYPE && cls != Integer.class) {
                     if(cls != Long.TYPE && cls != Long.class) {
                        if(cls != Float.TYPE && cls != Float.class) {
                           if(cls != Double.TYPE && cls != Double.class) {
                              if(Structure.class.isAssignableFrom(cls)) {
                                 return Structure.ByValue.class.isAssignableFrom(cls)?Structure.newInstance(cls).size():POINTER_SIZE;
                              } else if(!Pointer.class.isAssignableFrom(cls) && !Buffer.class.isAssignableFrom(cls) && !Callback.class.isAssignableFrom(cls) && String.class != cls && WString.class != cls) {
                                 throw new IllegalArgumentException("Native size for type \"" + cls.getName() + "\" is unknown");
                              } else {
                                 return POINTER_SIZE;
                              }
                           } else {
                              return 8;
                           }
                        } else {
                           return 4;
                        }
                     } else {
                        return 8;
                     }
                  } else {
                     return 4;
                  }
               } else {
                  return WCHAR_SIZE;
               }
            } else {
               return 2;
            }
         } else {
            return 1;
         }
      } else {
         return 4;
      }
   }

   public static boolean isSupportedNativeType(Class cls) {
      if(Structure.class.isAssignableFrom(cls)) {
         return true;
      } else {
         try {
            return getNativeSize(cls) != 0;
         } catch (IllegalArgumentException var2) {
            return false;
         }
      }
   }

   public static void setCallbackExceptionHandler(Callback.UncaughtExceptionHandler eh) {
      callbackExceptionHandler = eh == null?DEFAULT_HANDLER:eh;
   }

   public static Callback.UncaughtExceptionHandler getCallbackExceptionHandler() {
      return callbackExceptionHandler;
   }

   public static void register(String libName) {
      register(getNativeClass(getCallingClass()), NativeLibrary.getInstance(libName));
   }

   public static void register(NativeLibrary lib) {
      register(getNativeClass(getCallingClass()), lib);
   }

   static Class getNativeClass(Class cls) {
      Method[] methods = cls.getDeclaredMethods();

      int idx;
      for(idx = 0; idx < methods.length; ++idx) {
         if((methods[idx].getModifiers() & 256) != 0) {
            return cls;
         }
      }

      idx = cls.getName().lastIndexOf("$");
      if(idx != -1) {
         String name = cls.getName().substring(0, idx);

         try {
            return getNativeClass(Class.forName(name, true, cls.getClassLoader()));
         } catch (ClassNotFoundException var5) {
            ;
         }
      }

      throw new IllegalArgumentException("Can\'t determine class with native methods from the current context (" + cls + ")");
   }

   static Class getCallingClass() {
      Class[] context = (new SecurityManager() {
         public Class[] getClassContext() {
            return super.getClassContext();
         }
      }).getClassContext();
      if(context.length < 4) {
         throw new IllegalStateException("This method must be called from the static initializer of a class");
      } else {
         return context[3];
      }
   }

   public static void unregister() {
      unregister(getNativeClass(getCallingClass()));
   }

   public static void unregister(Class cls) {
      Map var1 = registeredClasses;
      synchronized(registeredClasses) {
         if(registeredClasses.containsKey(cls)) {
            unregister(cls, (long[])((long[])registeredClasses.get(cls)));
            registeredClasses.remove(cls);
            registeredLibraries.remove(cls);
         }

      }
   }

   private static native void unregister(Class var0, long[] var1);

   private static String getSignature(Class cls) {
      if(cls.isArray()) {
         return "[" + getSignature(cls.getComponentType());
      } else {
         if(cls.isPrimitive()) {
            if(cls == Void.TYPE) {
               return "V";
            }

            if(cls == Boolean.TYPE) {
               return "Z";
            }

            if(cls == Byte.TYPE) {
               return "B";
            }

            if(cls == Short.TYPE) {
               return "S";
            }

            if(cls == Character.TYPE) {
               return "C";
            }

            if(cls == Integer.TYPE) {
               return "I";
            }

            if(cls == Long.TYPE) {
               return "J";
            }

            if(cls == Float.TYPE) {
               return "F";
            }

            if(cls == Double.TYPE) {
               return "D";
            }
         }

         return "L" + replace(".", "/", cls.getName()) + ";";
      }
   }

   static String replace(String s1, String s2, String str) {
      StringBuffer buf = new StringBuffer();

      while(true) {
         int idx = str.indexOf(s1);
         if(idx == -1) {
            buf.append(str);
            return buf.toString();
         }

         buf.append(str.substring(0, idx));
         buf.append(s2);
         str = str.substring(idx + s1.length());
      }
   }

   private static int getConversion(Class type, TypeMapper mapper) {
      if(type == Boolean.class) {
         type = Boolean.TYPE;
      } else if(type == Byte.class) {
         type = Byte.TYPE;
      } else if(type == Short.class) {
         type = Short.TYPE;
      } else if(type == Character.class) {
         type = Character.TYPE;
      } else if(type == Integer.class) {
         type = Integer.TYPE;
      } else if(type == Long.class) {
         type = Long.TYPE;
      } else if(type == Float.class) {
         type = Float.TYPE;
      } else if(type == Double.class) {
         type = Double.TYPE;
      } else if(type == Void.class) {
         type = Void.TYPE;
      }

      if(mapper == null || mapper.getFromNativeConverter(type) == null && mapper.getToNativeConverter(type) == null) {
         if(Pointer.class.isAssignableFrom(type)) {
            return 1;
         } else if(String.class == type) {
            return 2;
         } else if(WString.class.isAssignableFrom(type)) {
            return 18;
         } else if(Buffer.class.isAssignableFrom(type)) {
            return 5;
         } else if(Structure.class.isAssignableFrom(type)) {
            return Structure.ByValue.class.isAssignableFrom(type)?4:3;
         } else {
            if(type.isArray()) {
               switch(type.getName().charAt(1)) {
               case 'B':
                  return 6;
               case 'C':
                  return 8;
               case 'D':
                  return 12;
               case 'E':
               case 'G':
               case 'H':
               case 'K':
               case 'L':
               case 'M':
               case 'N':
               case 'O':
               case 'P':
               case 'Q':
               case 'R':
               case 'T':
               case 'U':
               case 'V':
               case 'W':
               case 'X':
               case 'Y':
               default:
                  break;
               case 'F':
                  return 11;
               case 'I':
                  return 9;
               case 'J':
                  return 10;
               case 'S':
                  return 7;
               case 'Z':
                  return 13;
               }
            }

            return type.isPrimitive()?(type == Boolean.TYPE?14:0):(Callback.class.isAssignableFrom(type)?15:(IntegerType.class.isAssignableFrom(type)?19:(PointerType.class.isAssignableFrom(type)?20:(NativeMapped.class.isAssignableFrom(type)?17:-1))));
         }
      } else {
         return 21;
      }
   }

   public static void register(Class cls, NativeLibrary lib) {
      Method[] methods = cls.getDeclaredMethods();
      ArrayList mlist = new ArrayList();
      TypeMapper mapper = (TypeMapper)lib.getOptions().get("type-mapper");

      for(int handles = 0; handles < methods.length; ++handles) {
         if((methods[handles].getModifiers() & 256) != 0) {
            mlist.add(methods[handles]);
         }
      }

      long[] var30 = new long[mlist.size()];

      for(int i = 0; i < var30.length; ++i) {
         Method method = (Method)mlist.get(i);
         String sig = "(";
         Class rclass = method.getReturnType();
         Class[] ptypes = method.getParameterTypes();
         long[] atypes = new long[ptypes.length];
         long[] closure_atypes = new long[ptypes.length];
         int[] cvt = new int[ptypes.length];
         ToNativeConverter[] toNative = new ToNativeConverter[ptypes.length];
         FromNativeConverter fromNative = null;
         int rcvt = getConversion(rclass, mapper);
         boolean throwLastError = false;
         long rtype;
         long closure_rtype;
         switch(rcvt) {
         case -1:
            throw new IllegalArgumentException(rclass + " is not a supported return type (in method " + method.getName() + " in " + cls + ")");
         case 0:
         case 1:
         case 2:
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 13:
         case 14:
         case 15:
         case 16:
         case 18:
         default:
            closure_rtype = rtype = Structure.FFIType.get(rclass).peer;
            break;
         case 3:
            closure_rtype = rtype = Structure.FFIType.get(Pointer.class).peer;
            break;
         case 4:
            closure_rtype = Structure.FFIType.get(Pointer.class).peer;
            rtype = Structure.FFIType.get(rclass).peer;
            break;
         case 17:
         case 19:
         case 20:
            closure_rtype = Structure.FFIType.get(Pointer.class).peer;
            rtype = Structure.FFIType.get(NativeMappedConverter.getInstance(rclass).nativeType()).peer;
            break;
         case 21:
            fromNative = mapper.getFromNativeConverter(rclass);
            closure_rtype = Structure.FFIType.get(rclass).peer;
            rtype = Structure.FFIType.get(fromNative.nativeType()).peer;
         }

         for(int etypes = 0; etypes < ptypes.length; ++etypes) {
            Class name = ptypes[etypes];
            sig = sig + getSignature(name);
            cvt[etypes] = getConversion(name, mapper);
            if(cvt[etypes] == -1) {
               throw new IllegalArgumentException(name + " is not a supported argument type (in method " + method.getName() + " in " + cls + ")");
            }

            if(cvt[etypes] != 17 && cvt[etypes] != 19) {
               if(cvt[etypes] == 21) {
                  toNative[etypes] = mapper.getToNativeConverter(name);
               }
            } else {
               name = NativeMappedConverter.getInstance(name).nativeType();
            }

            switch(cvt[etypes]) {
            case 0:
               closure_atypes[etypes] = atypes[etypes] = Structure.FFIType.get(name).peer;
               break;
            case 4:
            case 17:
            case 19:
            case 20:
               atypes[etypes] = Structure.FFIType.get(name).peer;
               closure_atypes[etypes] = Structure.FFIType.get(Pointer.class).peer;
               break;
            case 21:
               if(name.isPrimitive()) {
                  closure_atypes[etypes] = Structure.FFIType.get(name).peer;
               } else {
                  closure_atypes[etypes] = Structure.FFIType.get(Pointer.class).peer;
               }

               atypes[etypes] = Structure.FFIType.get(toNative[etypes].nativeType()).peer;
               break;
            default:
               closure_atypes[etypes] = atypes[etypes] = Structure.FFIType.get(Pointer.class).peer;
            }
         }

         sig = sig + ")";
         sig = sig + getSignature(rclass);
         Class[] var32 = method.getExceptionTypes();

         for(int var33 = 0; var33 < var32.length; ++var33) {
            if(LastErrorException.class.isAssignableFrom(var32[var33])) {
               throwLastError = true;
               break;
            }
         }

         String var34 = method.getName();
         FunctionMapper fmapper = (FunctionMapper)lib.getOptions().get("function-mapper");
         if(fmapper != null) {
            var34 = fmapper.getFunctionName(lib, method);
         }

         Function f = lib.getFunction(var34, method);

         try {
            var30[i] = registerMethod(cls, method.getName(), sig, cvt, closure_atypes, atypes, rcvt, closure_rtype, rtype, rclass, f.peer, f.getCallingConvention(), throwLastError, toNative, fromNative);
         } catch (NoSuchMethodError var29) {
            throw new UnsatisfiedLinkError("No method " + method.getName() + " with signature " + sig + " in " + cls);
         }
      }

      Map var31 = registeredClasses;
      synchronized(registeredClasses) {
         registeredClasses.put(cls, var30);
         registeredLibraries.put(cls, lib);
      }

      cacheOptions(cls, lib.getOptions(), (Object)null);
   }

   private static void cacheOptions(Class cls, Map libOptions, Object proxy) {
      Map var3 = libraries;
      synchronized(libraries) {
         if(!libOptions.isEmpty()) {
            options.put(cls, libOptions);
         }

         if(libOptions.containsKey("type-mapper")) {
            typeMappers.put(cls, libOptions.get("type-mapper"));
         }

         if(libOptions.containsKey("structure-alignment")) {
            alignments.put(cls, libOptions.get("structure-alignment"));
         }

         if(proxy != null) {
            libraries.put(cls, new WeakReference(proxy));
         }

         if(!cls.isInterface() && Library.class.isAssignableFrom(cls)) {
            Class[] ifaces = cls.getInterfaces();

            for(int i = 0; i < ifaces.length; ++i) {
               if(Library.class.isAssignableFrom(ifaces[i])) {
                  cacheOptions(ifaces[i], libOptions, proxy);
                  break;
               }
            }
         }

      }
   }

   private static native long registerMethod(Class var0, String var1, String var2, int[] var3, long[] var4, long[] var5, int var6, long var7, long var9, Class var11, long var12, int var14, boolean var15, ToNativeConverter[] var16, FromNativeConverter var17);

   private static NativeMapped fromNative(Class cls, Object value) {
      return (NativeMapped)NativeMappedConverter.getInstance(cls).fromNative(value, new FromNativeContext(cls));
   }

   private static Class nativeType(Class cls) {
      return NativeMappedConverter.getInstance(cls).nativeType();
   }

   private static Object toNative(ToNativeConverter cvt, Object o) {
      return cvt.toNative(o, new ToNativeContext());
   }

   private static Object fromNative(FromNativeConverter cvt, Object o, Class cls) {
      return cvt.fromNative(o, new FromNativeContext(cls));
   }

   public static native long ffi_prep_cif(int var0, int var1, long var2, long var4);

   public static native void ffi_call(long var0, long var2, long var4, long var6);

   public static native long ffi_prep_closure(long var0, ffi_callback var2);

   public static native void ffi_free_closure(long var0);

   static native int initialize_ffi_type(long var0);

   public static void main(String[] args) {
      String DEFAULT_TITLE = "Java Native Access (JNA)";
      String DEFAULT_VERSION = "3.3.0";
      String DEFAULT_BUILD = "3.3.0 (package information missing)";
      Package pkg = Native.class.getPackage();
      String title = pkg != null?pkg.getSpecificationTitle():"Java Native Access (JNA)";
      if(title == null) {
         title = "Java Native Access (JNA)";
      }

      String version = pkg != null?pkg.getSpecificationVersion():"3.3.0";
      if(version == null) {
         version = "3.3.0";
      }

      title = title + " API Version " + version;
      System.out.println(title);
      version = pkg != null?pkg.getImplementationVersion():"3.3.0 (package information missing)";
      if(version == null) {
         version = "3.3.0 (package information missing)";
      }

      System.out.println("Version: " + version);
      System.out.println(" Native: " + getNativeVersion() + " (" + getAPIChecksum() + ")");
      System.exit(0);
   }

   static synchronized native void freeNativeCallback(long var0);

   static synchronized native long createNativeCallback(Callback var0, Method var1, Class[] var2, Class var3, int var4, boolean var5);

   static native int invokeInt(long var0, int var2, Object[] var3);

   static native long invokeLong(long var0, int var2, Object[] var3);

   static native void invokeVoid(long var0, int var2, Object[] var3);

   static native float invokeFloat(long var0, int var2, Object[] var3);

   static native double invokeDouble(long var0, int var2, Object[] var3);

   static native long invokePointer(long var0, int var2, Object[] var3);

   private static native void invokeStructure(long var0, int var2, Object[] var3, long var4, long var6);

   static Structure invokeStructure(long fp, int callFlags, Object[] args, Structure s) {
      invokeStructure(fp, callFlags, args, s.getPointer().peer, s.getTypeInfo().peer);
      return s;
   }

   static native Object invokeObject(long var0, int var2, Object[] var3);

   static native long open(String var0);

   static native void close(long var0);

   static native long findSymbol(long var0, String var2);

   static native long indexOf(long var0, byte var2);

   static native void read(long var0, byte[] var2, int var3, int var4);

   static native void read(long var0, short[] var2, int var3, int var4);

   static native void read(long var0, char[] var2, int var3, int var4);

   static native void read(long var0, int[] var2, int var3, int var4);

   static native void read(long var0, long[] var2, int var3, int var4);

   static native void read(long var0, float[] var2, int var3, int var4);

   static native void read(long var0, double[] var2, int var3, int var4);

   static native void write(long var0, byte[] var2, int var3, int var4);

   static native void write(long var0, short[] var2, int var3, int var4);

   static native void write(long var0, char[] var2, int var3, int var4);

   static native void write(long var0, int[] var2, int var3, int var4);

   static native void write(long var0, long[] var2, int var3, int var4);

   static native void write(long var0, float[] var2, int var3, int var4);

   static native void write(long var0, double[] var2, int var3, int var4);

   static native byte getByte(long var0);

   static native char getChar(long var0);

   static native short getShort(long var0);

   static native int getInt(long var0);

   static native long getLong(long var0);

   static native float getFloat(long var0);

   static native double getDouble(long var0);

   static Pointer getPointer(long addr) {
      long peer = _getPointer(addr);
      return peer == 0L?null:new Pointer(peer);
   }

   private static native long _getPointer(long var0);

   static native String getString(long var0, boolean var2);

   static native void setMemory(long var0, long var2, byte var4);

   static native void setByte(long var0, byte var2);

   static native void setShort(long var0, short var2);

   static native void setChar(long var0, char var2);

   static native void setInt(long var0, int var2);

   static native void setLong(long var0, long var2);

   static native void setFloat(long var0, float var2);

   static native void setDouble(long var0, double var2);

   static native void setPointer(long var0, long var2);

   static native void setString(long var0, String var2, boolean var3);

   public static native long malloc(long var0);

   public static native void free(long var0);

   public static native ByteBuffer getDirectByteBuffer(long var0, long var2);

   static {
      callbackExceptionHandler = DEFAULT_HANDLER;
      loadNativeLibrary();
      POINTER_SIZE = sizeof(0);
      LONG_SIZE = sizeof(1);
      WCHAR_SIZE = sizeof(2);
      SIZE_T_SIZE = sizeof(3);
      initIDs();
      if(Boolean.getBoolean("jna.protected")) {
         setProtected(true);
      }

      finalizer = new Object() {
         protected void finalize() {
            Native.dispose();
         }
      };
      lastError = new ThreadLocal() {
         protected synchronized Object initialValue() {
            return new Integer(0);
         }
      };
      registeredClasses = new HashMap();
      registeredLibraries = new HashMap();
      unloader = new Object() {
         protected void finalize() {
            synchronized(Native.registeredClasses) {
               Iterator i = Native.registeredClasses.entrySet().iterator();

               while(i.hasNext()) {
                  Entry e = (Entry)i.next();
                  Native.unregister((Class)e.getKey(), (long[])((long[])e.getValue()));
                  i.remove();
               }

            }
         }
      };
   }

   public interface ffi_callback {
      void invoke(long var1, long var3, long var5);
   }

   public static class DeleteNativeLibrary extends Thread {
      private final File file;

      public DeleteNativeLibrary(File file) {
         this.file = file;
      }

      public void run() {
         if(!Native.deleteNativeLibrary()) {
            try {
               Runtime.getRuntime().exec(new String[]{System.getProperty("java.home") + "/bin/java", "-cp", System.getProperty("java.class.path"), this.getClass().getName(), this.file.getAbsolutePath()});
            } catch (IOException var2) {
               var2.printStackTrace();
            }
         }

      }

      public static void main(String[] args) {
         if(args.length == 1) {
            File file = new File(args[0]);
            if(file.exists()) {
               long start = System.currentTimeMillis();

               while(!file.delete() && file.exists()) {
                  try {
                     Thread.sleep(10L);
                  } catch (InterruptedException var5) {
                     ;
                  }

                  if(System.currentTimeMillis() - start > 5000L) {
                     System.err.println("Could not remove temp file: " + file.getAbsolutePath());
                     break;
                  }
               }
            }
         }

         System.exit(0);
      }
   }
}
