package com.sun.jna.platform;

import com.sun.jna.Platform;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.win32.User32;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;

public class KeyboardUtils {
   static final NativeKeyboardUtils INSTANCE;

   public static boolean isPressed(int keycode, int location) {
      return INSTANCE.isPressed(keycode, location);
   }

   public static boolean isPressed(int keycode) {
      return INSTANCE.isPressed(keycode);
   }

   static {
      if(GraphicsEnvironment.isHeadless()) {
         throw new HeadlessException("KeyboardUtils requires a keyboard");
      } else {
         if(Platform.isWindows()) {
            INSTANCE = new W32KeyboardUtils();
         } else {
            if(Platform.isMac()) {
               INSTANCE = new MacKeyboardUtils();
               throw new UnsupportedOperationException("No support (yet) for " + System.getProperty("os.name"));
            }

            INSTANCE = new X11KeyboardUtils();
         }

      }
   }

   // $FF: synthetic class
   static class SyntheticClass_1 {
   }

   private static class X11KeyboardUtils extends NativeKeyboardUtils {
      private X11KeyboardUtils() {
         super((SyntheticClass_1)null);
      }

      private int toKeySym(int code, int location) {
         return code >= 65 && code <= 90?97 + (code - 65):(code >= 48 && code <= 57?48 + (code - 48):(code == 16?((location & 3) != 0?'￡':'￡'):(code == 17?((location & 3) != 0?'￤':'￣'):(code == 18?((location & 3) != 0?'￪':'￩'):(code == 157?((location & 3) != 0?'￨':'\uffe7'):0)))));
      }

      public boolean isPressed(int keycode, int location) {
         X11 lib = X11.INSTANCE;
         X11.Display dpy = lib.XOpenDisplay((String)null);
         if(dpy == null) {
            throw new Error("Can\'t open X Display");
         } else {
            try {
               byte[] keys = new byte[32];
               lib.XQueryKeymap(dpy, keys);
               int keysym = this.toKeySym(keycode, location);

               for(int code = 5; code < 256; ++code) {
                  int idx = code / 8;
                  int shift = code % 8;
                  if((keys[idx] & 1 << shift) != 0) {
                     int sym = lib.XKeycodeToKeysym(dpy, (byte)code, 0).intValue();
                     if(sym == keysym) {
                        boolean var11 = true;
                        return var11;
                     }
                  }
               }
            } finally {
               lib.XCloseDisplay(dpy);
            }

            return false;
         }
      }

      // $FF: synthetic method
      X11KeyboardUtils(SyntheticClass_1 x0) {
         this();
      }
   }

   private static class MacKeyboardUtils extends NativeKeyboardUtils {
      private MacKeyboardUtils() {
         super((SyntheticClass_1)null);
      }

      public boolean isPressed(int keycode, int location) {
         return false;
      }

      // $FF: synthetic method
      MacKeyboardUtils(SyntheticClass_1 x0) {
         this();
      }
   }

   private static class W32KeyboardUtils extends NativeKeyboardUtils {
      private W32KeyboardUtils() {
         super((SyntheticClass_1)null);
      }

      private int toNative(int code, int loc) {
         return (code < 65 || code > 90) && (code < 48 || code > 57)?(code == 16?((loc & 3) != 0?161:((loc & 2) != 0?160:16)):(code == 17?((loc & 3) != 0?163:((loc & 2) != 0?162:17)):(code == 18?((loc & 3) != 0?165:((loc & 2) != 0?164:18)):0))):code;
      }

      public boolean isPressed(int keycode, int location) {
         User32 lib = User32.INSTANCE;
         return (lib.GetAsyncKeyState(this.toNative(keycode, location)) & '耀') != 0;
      }

      // $FF: synthetic method
      W32KeyboardUtils(SyntheticClass_1 x0) {
         this();
      }
   }

   private abstract static class NativeKeyboardUtils {
      private NativeKeyboardUtils() {
      }

      public abstract boolean isPressed(int var1, int var2);

      public boolean isPressed(int keycode) {
         return this.isPressed(keycode, 0);
      }

      // $FF: synthetic method
      NativeKeyboardUtils(SyntheticClass_1 x0) {
         this();
      }
   }
}
