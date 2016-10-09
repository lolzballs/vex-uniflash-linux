package com.sun.jna.platform;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.RasterRangesUtils;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.peer.ComponentPeer;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;

public class WindowUtils {
   private static final String TRANSPARENT_OLD_BG = "transparent-old-bg";
   private static final String TRANSPARENT_OLD_OPAQUE = "transparent-old-opaque";
   private static final String TRANSPARENT_ALPHA = "transparent-alpha";
   public static final Shape MASK_NONE = null;

   private static NativeWindowUtils getInstance() {
      return Holder.INSTANCE;
   }

   public static void setWindowMask(Window w, Shape mask) {
      getInstance().setWindowMask(w, (Shape)mask);
   }

   public static void setComponentMask(Component c, Shape mask) {
      getInstance().setWindowMask(c, mask);
   }

   public static void setWindowMask(Window w, Icon mask) {
      getInstance().setWindowMask(w, (Icon)mask);
   }

   public static boolean isWindowAlphaSupported() {
      return getInstance().isWindowAlphaSupported();
   }

   public static GraphicsConfiguration getAlphaCompatibleGraphicsConfiguration() {
      return getInstance().getAlphaCompatibleGraphicsConfiguration();
   }

   public static void setWindowAlpha(Window w, float alpha) {
      getInstance().setWindowAlpha(w, Math.max(0.0F, Math.min(alpha, 1.0F)));
   }

   public static void setWindowTransparent(Window w, boolean transparent) {
      getInstance().setWindowTransparent(w, transparent);
   }

   // $FF: synthetic class
   static class SyntheticClass_1 {
   }

   private static class X11WindowUtils extends NativeWindowUtils {
      private boolean didCheck;
      private long[] alphaVisualIDs;
      private static final long OPAQUE = 4294967295L;
      private static final String OPACITY = "_NET_WM_WINDOW_OPACITY";

      private X11WindowUtils() {
         this.alphaVisualIDs = new long[0];
      }

      private static X11.Pixmap createBitmap(X11.Display dpy, X11.Window win, Raster raster) {
         X11 x11 = X11.INSTANCE;
         Rectangle bounds = raster.getBounds();
         int width = bounds.x + bounds.width;
         int height = bounds.y + bounds.height;
         X11.Pixmap pm = x11.XCreatePixmap(dpy, win, width, height, 1);
         X11.GC gc = x11.XCreateGC(dpy, pm, new NativeLong(0L), (X11.XGCValues)null);
         if(gc == null) {
            return null;
         } else {
            x11.XSetForeground(dpy, gc, new NativeLong(0L));
            x11.XFillRectangle(dpy, pm, gc, 0, 0, width, height);
            final ArrayList rlist = new ArrayList();

            try {
               RasterRangesUtils.outputOccupiedRanges(raster, new RasterRangesUtils.RangesOutput() {
                  public boolean outputRange(int x, int y, int w, int h) {
                     rlist.add(new Rectangle(x, y, w, h));
                     return true;
                  }
               });
               X11.XRectangle[] rects = (X11.XRectangle[])((X11.XRectangle[])(new X11.XRectangle()).toArray(rlist.size()));

               for(int UNMASKED = 0; UNMASKED < rects.length; ++UNMASKED) {
                  Rectangle r = (Rectangle)rlist.get(UNMASKED);
                  rects[UNMASKED].x = (short)r.x;
                  rects[UNMASKED].y = (short)r.y;
                  rects[UNMASKED].width = (short)r.width;
                  rects[UNMASKED].height = (short)r.height;
                  Pointer p = rects[UNMASKED].getPointer();
                  p.setShort(0L, (short)r.x);
                  p.setShort(2L, (short)r.y);
                  p.setShort(4L, (short)r.width);
                  p.setShort(6L, (short)r.height);
                  rects[UNMASKED].setAutoSynch(false);
               }

               boolean var17 = true;
               x11.XSetForeground(dpy, gc, new NativeLong(1L));
               x11.XFillRectangles(dpy, pm, gc, rects, rects.length);
               return pm;
            } finally {
               x11.XFreeGC(dpy, gc);
            }
         }
      }

      public boolean isWindowAlphaSupported() {
         return this.getAlphaVisualIDs().length > 0;
      }

      private static long getVisualID(GraphicsConfiguration config) {
         try {
            Object e = config.getClass().getMethod("getVisual", (Class[])null).invoke(config, (Object[])null);
            return ((Number)e).longValue();
         } catch (Exception var2) {
            var2.printStackTrace();
            return -1L;
         }
      }

      public GraphicsConfiguration getAlphaCompatibleGraphicsConfiguration() {
         if(this.isWindowAlphaSupported()) {
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] devices = env.getScreenDevices();

            for(int i = 0; i < devices.length; ++i) {
               GraphicsConfiguration[] configs = devices[i].getConfigurations();

               for(int j = 0; j < configs.length; ++j) {
                  long visualID = getVisualID(configs[j]);
                  long[] ids = this.getAlphaVisualIDs();

                  for(int k = 0; k < ids.length; ++k) {
                     if(visualID == ids[k]) {
                        return configs[j];
                     }
                  }
               }
            }
         }

         return super.getAlphaCompatibleGraphicsConfiguration();
      }

      private synchronized long[] getAlphaVisualIDs() {
         if(this.didCheck) {
            return this.alphaVisualIDs;
         } else {
            this.didCheck = true;
            X11 x11 = X11.INSTANCE;
            X11.Display dpy = x11.XOpenDisplay((String)null);
            if(dpy == null) {
               return this.alphaVisualIDs;
            } else {
               X11.XVisualInfo info = null;

               try {
                  int screen = x11.XDefaultScreen(dpy);
                  X11.XVisualInfo template = new X11.XVisualInfo();
                  template.screen = screen;
                  template.depth = 32;
                  template.c_class = 4;
                  NativeLong mask = new NativeLong(14L);
                  IntByReference pcount = new IntByReference();
                  info = x11.XGetVisualInfo(dpy, mask, template, pcount);
                  if(info != null) {
                     ArrayList list = new ArrayList();
                     X11.XVisualInfo[] infos = (X11.XVisualInfo[])((X11.XVisualInfo[])info.toArray(pcount.getValue()));

                     int i;
                     for(i = 0; i < infos.length; ++i) {
                        X11.Xrender.XRenderPictFormat format = X11.Xrender.INSTANCE.XRenderFindVisualFormat(dpy, infos[i].visual);
                        if(format.type == 1 && format.direct.alphaMask != 0) {
                           list.add(infos[i].visualid);
                        }
                     }

                     this.alphaVisualIDs = new long[list.size()];

                     for(i = 0; i < this.alphaVisualIDs.length; ++i) {
                        this.alphaVisualIDs[i] = ((Number)list.get(i)).longValue();
                     }

                     long[] var15 = this.alphaVisualIDs;
                     return var15;
                  }
               } finally {
                  if(info != null) {
                     x11.XFree(info.getPointer());
                  }

                  x11.XCloseDisplay(dpy);
               }

               return this.alphaVisualIDs;
            }
         }
      }

      private static X11.Window getContentWindow(Window w, X11.Display dpy, X11.Window win, Point offset) {
         if(w instanceof Frame && !((Frame)w).isUndecorated() || w instanceof Dialog && !((Dialog)w).isUndecorated()) {
            X11 x11 = X11.INSTANCE;
            X11.WindowByReference rootp = new X11.WindowByReference();
            X11.WindowByReference parentp = new X11.WindowByReference();
            PointerByReference childrenp = new PointerByReference();
            IntByReference countp = new IntByReference();
            x11.XQueryTree(dpy, win, rootp, parentp, childrenp, countp);
            Pointer p = childrenp.getValue();
            int[] ids = p.getIntArray(0L, countp.getValue());
            int len$ = ids.length;
            byte i$ = 0;
            if(i$ < len$) {
               int id = ids[i$];
               X11.Window child = new X11.Window((long)id);
               X11.XWindowAttributes xwa = new X11.XWindowAttributes();
               x11.XGetWindowAttributes(dpy, child, xwa);
               offset.x = -xwa.x;
               offset.y = -xwa.y;
               win = child;
            }

            if(p != null) {
               x11.XFree(p);
            }
         }

         return win;
      }

      private static X11.Window getDrawable(Component w) {
         int id = (int)Native.getComponentID(w);
         return id == 0?null:new X11.Window((long)id);
      }

      public void setWindowAlpha(final Window w, final float alpha) {
         if(!this.isWindowAlphaSupported()) {
            throw new UnsupportedOperationException("This X11 display does not provide a 32-bit visual");
         } else {
            Runnable action = new Runnable() {
               public void run() {
                  X11 x11 = X11.INSTANCE;
                  X11.Display dpy = x11.XOpenDisplay((String)null);
                  if(dpy != null) {
                     try {
                        X11.Window win = X11WindowUtils.getDrawable(w);
                        if(alpha == 1.0F) {
                           x11.XDeleteProperty(dpy, win, x11.XInternAtom(dpy, "_NET_WM_WINDOW_OPACITY", false));
                        } else {
                           int opacity = (int)((long)(alpha * 4.2949673E9F) & -1L);
                           IntByReference patom = new IntByReference(opacity);
                           x11.XChangeProperty(dpy, win, x11.XInternAtom(dpy, "_NET_WM_WINDOW_OPACITY", false), X11.XA_CARDINAL, 32, 0, patom.getPointer(), 1);
                        }
                     } finally {
                        x11.XCloseDisplay(dpy);
                     }

                  }
               }
            };
            this.whenDisplayable(w, action);
         }
      }

      public void setWindowTransparent(final Window w, final boolean transparent) {
         if(!(w instanceof RootPaneContainer)) {
            throw new IllegalArgumentException("Window must be a RootPaneContainer");
         } else if(!this.isWindowAlphaSupported()) {
            throw new UnsupportedOperationException("This X11 display does not provide a 32-bit visual");
         } else if(!w.getGraphicsConfiguration().equals(this.getAlphaCompatibleGraphicsConfiguration())) {
            throw new IllegalArgumentException("Window GraphicsConfiguration \'" + w.getGraphicsConfiguration() + "\' does not support transparency");
         } else {
            boolean isTransparent = w.getBackground() != null && w.getBackground().getAlpha() == 0;
            if(transparent != isTransparent) {
               this.whenDisplayable(w, new Runnable() {
                  public void run() {
                     JRootPane root = ((RootPaneContainer)w).getRootPane();
                     JLayeredPane lp = root.getLayeredPane();
                     Container content = root.getContentPane();
                     if(content instanceof X11TransparentContentPane) {
                        ((X11TransparentContentPane)content).setTransparent(transparent);
                     } else if(transparent) {
                        X11TransparentContentPane x11content = X11WindowUtils.this.new X11TransparentContentPane(content);
                        root.setContentPane(x11content);
                        lp.add(new RepaintTrigger(x11content), JLayeredPane.DRAG_LAYER);
                     }

                     X11WindowUtils.this.setLayersTransparent(w, transparent);
                     X11WindowUtils.this.setForceHeavyweightPopups(w, transparent);
                     X11WindowUtils.this.setDoubleBuffered(w, !transparent);
                  }
               });
            }
         }
      }

      private void setWindowShape(final Window w, final PixmapSource src) {
         Runnable action = new Runnable() {
            public void run() {
               X11 x11 = X11.INSTANCE;
               X11.Display dpy = x11.XOpenDisplay((String)null);
               if(dpy != null) {
                  X11.Pixmap pm = null;

                  try {
                     X11.Window win = X11WindowUtils.getDrawable(w);
                     pm = src.getPixmap(dpy, win);
                     X11.Xext ext = X11.Xext.INSTANCE;
                     ext.XShapeCombineMask(dpy, win, 0, 0, 0, pm == null?X11.Pixmap.None:pm, 0);
                  } finally {
                     if(pm != null) {
                        x11.XFreePixmap(dpy, pm);
                     }

                     x11.XCloseDisplay(dpy);
                  }

                  X11WindowUtils.this.setForceHeavyweightPopups(X11WindowUtils.this.getWindow(w), pm != null);
               }
            }
         };
         this.whenDisplayable(w, action);
      }

      protected void setMask(Component w, final Raster raster) {
         this.setWindowShape(this.getWindow(w), new PixmapSource() {
            public X11.Pixmap getPixmap(X11.Display dpy, X11.Window win) {
               return raster != null? X11WindowUtils.createBitmap(dpy, win, raster):null;
            }
         });
      }

      // $FF: synthetic method
      X11WindowUtils(SyntheticClass_1 x0) {
         this();
      }

      private interface PixmapSource {
         X11.Pixmap getPixmap(X11.Display var1, X11.Window var2);
      }

      private class X11TransparentContentPane extends TransparentContentPane {
         private static final long serialVersionUID = 1L;
         private Memory buffer;
         private int[] pixels;
         private final int[] pixel = new int[4];

         public X11TransparentContentPane(Container oldContent) {
            super(oldContent);
         }

         protected void paintDirect(BufferedImage buf, Rectangle bounds) {
            Window window = SwingUtilities.getWindowAncestor(this);
            X11 x11 = X11.INSTANCE;
            X11.Display dpy = x11.XOpenDisplay((String)null);
            X11.Window win = X11WindowUtils.getDrawable(window);
            Point offset = new Point();
            win = X11WindowUtils.getContentWindow(window, dpy, win, offset);
            X11.GC gc = x11.XCreateGC(dpy, win, new NativeLong(0L), (X11.XGCValues)null);
            Raster raster = buf.getData();
            int w = bounds.width;
            int h = bounds.height;
            if(this.buffer == null || this.buffer.getSize() != (long)(w * h * 4)) {
               this.buffer = new Memory((long)(w * h * 4));
               this.pixels = new int[w * h];
            }

            for(int xwa = 0; xwa < h; ++xwa) {
               for(int image = 0; image < w; ++image) {
                  raster.getPixel(image, xwa, this.pixel);
                  int alpha = this.pixel[3] & 255;
                  int red = this.pixel[2] & 255;
                  int green = this.pixel[1] & 255;
                  int blue = this.pixel[0] & 255;
                  this.pixels[xwa * w + image] = alpha << 24 | blue << 16 | green << 8 | red;
               }
            }

            X11.XWindowAttributes var18 = new X11.XWindowAttributes();
            x11.XGetWindowAttributes(dpy, win, var18);
            X11.XImage var19 = x11.XCreateImage(dpy, var18.visual, 32, 2, 0, this.buffer, w, h, 32, w * 4);
            this.buffer.write(0L, (int[])this.pixels, 0, this.pixels.length);
            offset.x += bounds.x;
            offset.y += bounds.y;
            x11.XPutImage(dpy, win, gc, var19, 0, 0, offset.x, offset.y, w, h);
            x11.XFree(var19.getPointer());
            x11.XFreeGC(dpy, gc);
            x11.XCloseDisplay(dpy);
         }
      }
   }

   private static class MacWindowUtils extends NativeWindowUtils {
      private static final String WDRAG = "apple.awt.draggableWindowBackground";

      private MacWindowUtils() {
      }

      public boolean isWindowAlphaSupported() {
         return true;
      }

      private OSXMaskingContentPane installMaskingPane(Window w) {
         OSXMaskingContentPane content;
         if(w instanceof RootPaneContainer) {
            RootPaneContainer oldContent = (RootPaneContainer)w;
            Container oldContent1 = oldContent.getContentPane();
            if(oldContent1 instanceof OSXMaskingContentPane) {
               content = (OSXMaskingContentPane)oldContent1;
            } else {
               content = new OSXMaskingContentPane(oldContent1);
               oldContent.setContentPane(content);
            }
         } else {
            Component oldContent2 = w.getComponentCount() > 0?w.getComponent(0):null;
            if(oldContent2 instanceof OSXMaskingContentPane) {
               content = (OSXMaskingContentPane)oldContent2;
            } else {
               content = new OSXMaskingContentPane(oldContent2);
               w.add(content);
            }
         }

         return content;
      }

      public void setWindowTransparent(Window w, boolean transparent) {
         boolean isTransparent = w.getBackground() != null && w.getBackground().getAlpha() == 0;
         if(transparent != isTransparent) {
            this.setBackgroundTransparent(w, transparent, "setWindowTransparent");
         }

      }

      private void fixWindowDragging(Window w, String context) {
         if(w instanceof RootPaneContainer) {
            JRootPane p = ((RootPaneContainer)w).getRootPane();
            Boolean oldDraggable = (Boolean)p.getClientProperty("apple.awt.draggableWindowBackground");
            if(oldDraggable == null) {
               p.putClientProperty("apple.awt.draggableWindowBackground", Boolean.FALSE);
               if(w.isDisplayable()) {
                  System.err.println(context + "(): To avoid content dragging, " + context + "() must be called before the window is realized, or " + "apple.awt.draggableWindowBackground" + " must be set to Boolean.FALSE before the window is realized.  If you really want content dragging, set " + "apple.awt.draggableWindowBackground" + " on the window\'s root pane to Boolean.TRUE before calling " + context + "() to hide this message.");
               }
            }
         }

      }

      public void setWindowAlpha(final Window w, final float alpha) {
         if(w instanceof RootPaneContainer) {
            JRootPane p = ((RootPaneContainer)w).getRootPane();
            p.putClientProperty("Window.alpha", new Float(alpha));
            this.fixWindowDragging(w, "setWindowAlpha");
         }

         this.whenDisplayable(w, new Runnable() {
            public void run() {
               ComponentPeer peer = w.getPeer();

               try {
                  peer.getClass().getMethod("setAlpha", new Class[]{Float.TYPE}).invoke(peer, new Object[]{new Float(alpha)});
               } catch (Exception var3) {
                  ;
               }

            }
         });
      }

      protected void setWindowMask(Component w, Raster raster) {
         if(raster != null) {
            this.setWindowMask(w, this.toShape(raster));
         } else {
            this.setWindowMask(w, (Shape)(new Rectangle(0, 0, w.getWidth(), w.getHeight())));
         }

      }

      public void setWindowMask(Component c, Shape shape) {
         if(c instanceof Window) {
            Window w = (Window)c;
            OSXMaskingContentPane content = this.installMaskingPane(w);
            content.setMask(shape);
            this.setBackgroundTransparent(w, shape != WindowUtils.MASK_NONE, "setWindowMask");
         }

      }

      private void setBackgroundTransparent(Window w, boolean transparent, String context) {
         JRootPane rp = w instanceof RootPaneContainer?((RootPaneContainer)w).getRootPane():null;
         if(transparent) {
            if(rp != null) {
               rp.putClientProperty("transparent-old-bg", w.getBackground());
            }

            w.setBackground(new Color(0, 0, 0, 0));
         } else if(rp != null) {
            Color bg = (Color)rp.getClientProperty("transparent-old-bg");
            if(bg != null) {
               bg = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), bg.getAlpha());
            }

            w.setBackground(bg);
            rp.putClientProperty("transparent-old-bg", (Object)null);
         } else {
            w.setBackground((Color)null);
         }

         this.fixWindowDragging(w, context);
      }

      // $FF: synthetic method
      MacWindowUtils(SyntheticClass_1 x0) {
         this();
      }

      private static class OSXMaskingContentPane extends JPanel {
         private static final long serialVersionUID = 1L;
         private Shape shape;

         public OSXMaskingContentPane(Component oldContent) {
            super(new BorderLayout());
            if(oldContent != null) {
               this.add(oldContent, "Center");
            }

         }

         public void setMask(Shape shape) {
            this.shape = shape;
            this.repaint();
         }

         public void paint(Graphics graphics) {
            Graphics2D g = (Graphics2D)graphics.create();
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            g.dispose();
            if(this.shape != null) {
               g = (Graphics2D)graphics.create();
               g.setClip(this.shape);
               super.paint(g);
               g.dispose();
            } else {
               super.paint(graphics);
            }

         }
      }
   }

   private static class W32WindowUtils extends NativeWindowUtils {
      private W32WindowUtils() {
      }

      private WinDef.HWND getHWnd(Component w) {
         WinDef.HWND hwnd = new WinDef.HWND();
         hwnd.setPointer(Native.getComponentPointer(w));
         return hwnd;
      }

      public boolean isWindowAlphaSupported() {
         return Boolean.getBoolean("sun.java2d.noddraw");
      }

      private boolean usingUpdateLayeredWindow(Window w) {
         if(w instanceof RootPaneContainer) {
            JRootPane root = ((RootPaneContainer)w).getRootPane();
            return root.getClientProperty("transparent-old-bg") != null;
         } else {
            return false;
         }
      }

      private void storeAlpha(Window w, byte alpha) {
         if(w instanceof RootPaneContainer) {
            JRootPane root = ((RootPaneContainer)w).getRootPane();
            Byte b = alpha == -1?null:new Byte(alpha);
            root.putClientProperty("transparent-alpha", b);
         }

      }

      private byte getAlpha(Window w) {
         if(w instanceof RootPaneContainer) {
            JRootPane root = ((RootPaneContainer)w).getRootPane();
            Byte b = (Byte)root.getClientProperty("transparent-alpha");
            if(b != null) {
               return b.byteValue();
            }
         }

         return -1;
      }

      public void setWindowAlpha(final Window w, final float alpha) {
         if(!this.isWindowAlphaSupported()) {
            throw new UnsupportedOperationException("Set sun.java2d.noddraw=true to enable transparent windows");
         } else {
            this.whenDisplayable(w, new Runnable() {
               public void run() {
                  WinDef.HWND hWnd = W32WindowUtils.this.getHWnd(w);
                  User32 user = User32.INSTANCE;
                  int flags = user.GetWindowLong(hWnd, -20);
                  byte level = (byte)((int)(255.0F * alpha) & 255);
                  if(W32WindowUtils.this.usingUpdateLayeredWindow(w)) {
                     WinUser.BLENDFUNCTION blend = new WinUser.BLENDFUNCTION();
                     blend.SourceConstantAlpha = level;
                     blend.AlphaFormat = 1;
                     user.UpdateLayeredWindow(hWnd, (WinDef.HDC)null, (WinUser.POINT)null, (WinUser.SIZE)null, (WinDef.HDC)null, (WinUser.POINT)null, 0, blend, 2);
                  } else if(alpha == 1.0F) {
                     flags &= -524289;
                     user.SetWindowLong(hWnd, -20, flags);
                  } else {
                     flags |= 524288;
                     user.SetWindowLong(hWnd, -20, flags);
                     user.SetLayeredWindowAttributes(hWnd, 0, level, 2);
                  }

                  W32WindowUtils.this.setForceHeavyweightPopups(w, alpha != 1.0F);
                  W32WindowUtils.this.storeAlpha(w, level);
               }
            });
         }
      }

      public void setWindowTransparent(final Window w, final boolean transparent) {
         if(!(w instanceof RootPaneContainer)) {
            throw new IllegalArgumentException("Window must be a RootPaneContainer");
         } else if(!this.isWindowAlphaSupported()) {
            throw new UnsupportedOperationException("Set sun.java2d.noddraw=true to enable transparent windows");
         } else {
            boolean isTransparent = w.getBackground() != null && w.getBackground().getAlpha() == 0;
            if(transparent != isTransparent) {
               this.whenDisplayable(w, new Runnable() {
                  public void run() {
                     User32 user = User32.INSTANCE;
                     WinDef.HWND hWnd = W32WindowUtils.this.getHWnd(w);
                     int flags = user.GetWindowLong(hWnd, -20);
                     JRootPane root = ((RootPaneContainer)w).getRootPane();
                     JLayeredPane lp = root.getLayeredPane();
                     Container content = root.getContentPane();
                     if(content instanceof W32TransparentContentPane) {
                        ((W32TransparentContentPane)content).setTransparent(transparent);
                     } else if(transparent) {
                        W32TransparentContentPane w32content = W32WindowUtils.this.new W32TransparentContentPane(content);
                        root.setContentPane(w32content);
                        lp.add(new RepaintTrigger(w32content), JLayeredPane.DRAG_LAYER);
                     }

                     if(transparent && !W32WindowUtils.this.usingUpdateLayeredWindow(w)) {
                        flags |= 524288;
                        user.SetWindowLong(hWnd, -20, flags);
                     } else if(!transparent && W32WindowUtils.this.usingUpdateLayeredWindow(w)) {
                        flags &= -524289;
                        user.SetWindowLong(hWnd, -20, flags);
                     }

                     W32WindowUtils.this.setLayersTransparent(w, transparent);
                     W32WindowUtils.this.setForceHeavyweightPopups(w, transparent);
                     W32WindowUtils.this.setDoubleBuffered(w, !transparent);
                  }
               });
            }
         }
      }

      public void setWindowMask(Component w, Shape mask) {
         if(mask instanceof Area && ((Area)mask).isPolygonal()) {
            this.setMask(w, (Area)mask);
         } else {
            super.setWindowMask(w, mask);
         }

      }

      private void setWindowRegion(final Component w, final WinDef.HRGN hrgn) {
         this.whenDisplayable(w, new Runnable() {
            public void run() {
               GDI32 gdi = GDI32.INSTANCE;
               User32 user = User32.INSTANCE;
               WinDef.HWND hWnd = W32WindowUtils.this.getHWnd(w);

               try {
                  user.SetWindowRgn(hWnd, hrgn, true);
                  W32WindowUtils.this.setForceHeavyweightPopups(W32WindowUtils.this.getWindow(w), hrgn != null);
               } finally {
                  gdi.DeleteObject(hrgn);
               }

            }
         });
      }

      private void setMask(Component w, Area area) {
         GDI32 gdi = GDI32.INSTANCE;
         PathIterator pi = area.getPathIterator((AffineTransform)null);
         int mode = pi.getWindingRule() == 1?2:1;
         float[] coords = new float[6];
         ArrayList points = new ArrayList();
         int size = 0;

         ArrayList sizes;
         for(sizes = new ArrayList(); !pi.isDone(); pi.next()) {
            int lppt = pi.currentSegment(coords);
            if(lppt == 0) {
               size = 1;
               points.add(new WinUser.POINT((int)coords[0], (int)coords[1]));
            } else if(lppt == 1) {
               ++size;
               points.add(new WinUser.POINT((int)coords[0], (int)coords[1]));
            } else {
               if(lppt != 4) {
                  throw new RuntimeException("Area is not polygonal: " + area);
               }

               sizes.add(new Integer(size));
            }
         }

         WinUser.POINT[] var14 = (WinUser.POINT[])((WinUser.POINT[])(new WinUser.POINT()).toArray(points.size()));
         WinUser.POINT[] pts = (WinUser.POINT[])((WinUser.POINT[])points.toArray(new WinUser.POINT[points.size()]));

         for(int counts = 0; counts < var14.length; ++counts) {
            var14[counts].x = pts[counts].x;
            var14[counts].y = pts[counts].y;
         }

         int[] var15 = new int[sizes.size()];

         for(int hrgn = 0; hrgn < var15.length; ++hrgn) {
            var15[hrgn] = ((Integer)sizes.get(hrgn)).intValue();
         }

         WinDef.HRGN var16 = gdi.CreatePolyPolygonRgn(var14, var15, var15.length, mode);
         this.setWindowRegion(w, var16);
      }

      protected void setMask(Component w, Raster raster) {
         GDI32 gdi = GDI32.INSTANCE;
         final WinDef.HRGN region = raster != null?gdi.CreateRectRgn(0, 0, 0, 0):null;
         if(region != null) {
            final WinDef.HRGN tempRgn = gdi.CreateRectRgn(0, 0, 0, 0);

            try {
               RasterRangesUtils.outputOccupiedRanges(raster, new RasterRangesUtils.RangesOutput() {
                  public boolean outputRange(int x, int y, int w, int h) {
                     GDI32 gdi = GDI32.INSTANCE;
                     gdi.SetRectRgn(tempRgn, x, y, x + w, y + h);
                     return gdi.CombineRgn(region, region, tempRgn, 2) != 0;
                  }
               });
            } finally {
               gdi.DeleteObject(tempRgn);
            }
         }

         this.setWindowRegion(w, region);
      }

      // $FF: synthetic method
      W32WindowUtils(SyntheticClass_1 x0) {
         this();
      }

      private class W32TransparentContentPane extends TransparentContentPane {
         private static final long serialVersionUID = 1L;
         private WinDef.HDC memDC;
         private WinDef.HBITMAP hBitmap;
         private Pointer pbits;
         private Dimension bitmapSize;

         public W32TransparentContentPane(Container content) {
            super(content);
         }

         private void disposeBackingStore() {
            GDI32 gdi = GDI32.INSTANCE;
            if(this.hBitmap != null) {
               gdi.DeleteObject(this.hBitmap);
               this.hBitmap = null;
            }

            if(this.memDC != null) {
               gdi.DeleteDC(this.memDC);
               this.memDC = null;
            }

         }

         public void removeNotify() {
            super.removeNotify();
            this.disposeBackingStore();
         }

         public void setTransparent(boolean transparent) {
            super.setTransparent(transparent);
            if(!transparent) {
               this.disposeBackingStore();
            }

         }

         protected void paintDirect(BufferedImage buf, Rectangle bounds) {
            Window win = SwingUtilities.getWindowAncestor(this);
            GDI32 gdi = GDI32.INSTANCE;
            User32 user = User32.INSTANCE;
            int x = bounds.x;
            int y = bounds.y;
            Point origin = SwingUtilities.convertPoint(this, x, y, win);
            int w = bounds.width;
            int h = bounds.height;
            int ww = win.getWidth();
            int wh = win.getHeight();
            WinDef.HDC screenDC = user.GetDC((WinDef.HWND)null);
            WinNT.HANDLE oldBitmap = null;

            try {
               if(this.memDC == null) {
                  this.memDC = gdi.CreateCompatibleDC(screenDC);
               }

               if(this.hBitmap == null || !win.getSize().equals(this.bitmapSize)) {
                  if(this.hBitmap != null) {
                     gdi.DeleteObject(this.hBitmap);
                     this.hBitmap = null;
                  }

                  WinGDI.BITMAPINFO raster = new WinGDI.BITMAPINFO();
                  raster.bmiHeader.biWidth = ww;
                  raster.bmiHeader.biHeight = wh;
                  raster.bmiHeader.biPlanes = 1;
                  raster.bmiHeader.biBitCount = 32;
                  raster.bmiHeader.biCompression = 0;
                  raster.bmiHeader.biSizeImage = ww * wh * 4;
                  PointerByReference pixel = new PointerByReference();
                  this.hBitmap = gdi.CreateDIBSection(this.memDC, raster, 0, pixel, (Pointer)null, 0);
                  this.pbits = pixel.getValue();
                  this.bitmapSize = new Dimension(ww, wh);
               }

               oldBitmap = gdi.SelectObject(this.memDC, this.hBitmap);
               Raster var32 = buf.getData();
               int[] var33 = new int[4];
               int[] bits = new int[w];

               for(int winSize = 0; winSize < h; ++winSize) {
                  int winLoc;
                  for(winLoc = 0; winLoc < w; ++winLoc) {
                     var32.getPixel(winLoc, winSize, var33);
                     int srcLoc = (var33[3] & 255) << 24;
                     int blend = var33[2] & 255;
                     int hWnd = (var33[1] & 255) << 8;
                     int bref = (var33[0] & 255) << 16;
                     bits[winLoc] = srcLoc | blend | hWnd | bref;
                  }

                  winLoc = wh - (origin.y + winSize) - 1;
                  this.pbits.write((long)((winLoc * ww + origin.x) * 4), (int[])bits, 0, bits.length);
               }

               WinUser.SIZE var34 = new WinUser.SIZE();
               var34.cx = win.getWidth();
               var34.cy = win.getHeight();
               WinUser.POINT var35 = new WinUser.POINT();
               var35.x = win.getX();
               var35.y = win.getY();
               WinUser.POINT var36 = new WinUser.POINT();
               WinUser.BLENDFUNCTION var37 = new WinUser.BLENDFUNCTION();
               WinDef.HWND var38 = W32WindowUtils.this.getHWnd(win);
               ByteByReference var39 = new ByteByReference();
               IntByReference iref = new IntByReference();
               byte level = W32WindowUtils.this.getAlpha(win);

               try {
                  if(user.GetLayeredWindowAttributes(var38, (IntByReference)null, var39, iref) && (iref.getValue() & 2) != 0) {
                     level = var39.getValue();
                  }
               } catch (UnsatisfiedLinkError var30) {
                  ;
               }

               var37.SourceConstantAlpha = level;
               var37.AlphaFormat = 1;
               user.UpdateLayeredWindow(var38, screenDC, var35, var34, this.memDC, var36, 0, var37, 2);
            } finally {
               user.ReleaseDC((WinDef.HWND)null, screenDC);
               if(this.memDC != null && oldBitmap != null) {
                  gdi.SelectObject(this.memDC, oldBitmap);
               }

            }
         }
      }
   }

   private static class Holder {
      public static boolean requiresVisible;
      public static final NativeWindowUtils INSTANCE;

      static {
         if(Platform.isWindows()) {
            INSTANCE = new W32WindowUtils();
         } else if(Platform.isMac()) {
            INSTANCE = new MacWindowUtils();
         } else {
            if(!Platform.isX11()) {
               String os = System.getProperty("os.name");
               throw new UnsupportedOperationException("No support for " + os);
            }

            INSTANCE = new X11WindowUtils();
            requiresVisible = System.getProperty("java.version").matches("^1\\.4\\..*");
         }

      }
   }

   public abstract static class NativeWindowUtils {
      protected Window getWindow(Component c) {
         return c instanceof Window?(Window)c:SwingUtilities.getWindowAncestor(c);
      }

      protected void whenDisplayable(Component w, final Runnable action) {
         if(!w.isDisplayable() || Holder.requiresVisible && !w.isVisible()) {
            if(Holder.requiresVisible) {
               this.getWindow(w).addWindowListener(new WindowAdapter() {
                  public void windowOpened(WindowEvent e) {
                     e.getWindow().removeWindowListener(this);
                     action.run();
                  }

                  public void windowClosed(WindowEvent e) {
                     e.getWindow().removeWindowListener(this);
                  }
               });
            } else {
               w.addHierarchyListener(new HierarchyListener() {
                  public void hierarchyChanged(HierarchyEvent e) {
                     if((e.getChangeFlags() & 2L) != 0L && e.getComponent().isDisplayable()) {
                        e.getComponent().removeHierarchyListener(this);
                        action.run();
                     }

                  }
               });
            }
         } else {
            action.run();
         }

      }

      protected Raster toRaster(Shape mask) {
         WritableRaster raster = null;
         if(mask != WindowUtils.MASK_NONE) {
            Rectangle bounds = mask.getBounds();
            if(bounds.width > 0 && bounds.height > 0) {
               BufferedImage clip = new BufferedImage(bounds.x + bounds.width, bounds.y + bounds.height, 12);
               Graphics2D g = clip.createGraphics();
               g.setColor(Color.black);
               g.fillRect(0, 0, bounds.x + bounds.width, bounds.y + bounds.height);
               g.setColor(Color.white);
               g.fill(mask);
               raster = clip.getRaster();
            }
         }

         return raster;
      }

      protected Raster toRaster(Component c, Icon mask) {
         WritableRaster raster = null;
         if(mask != null) {
            Rectangle bounds = new Rectangle(0, 0, mask.getIconWidth(), mask.getIconHeight());
            BufferedImage clip = new BufferedImage(bounds.width, bounds.height, 2);
            Graphics2D g = clip.createGraphics();
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, bounds.width, bounds.height);
            g.setComposite(AlphaComposite.SrcOver);
            mask.paintIcon(c, g, 0, 0);
            raster = clip.getAlphaRaster();
         }

         return raster;
      }

      protected Shape toShape(Raster raster) {
         final Area area = new Area(new Rectangle(0, 0, 0, 0));
         RasterRangesUtils.outputOccupiedRanges(raster, new RasterRangesUtils.RangesOutput() {
            public boolean outputRange(int x, int y, int w, int h) {
               area.add(new Area(new Rectangle(x, y, w, h)));
               return true;
            }
         });
         return area;
      }

      public void setWindowAlpha(Window w, float alpha) {
      }

      public boolean isWindowAlphaSupported() {
         return false;
      }

      public GraphicsConfiguration getAlphaCompatibleGraphicsConfiguration() {
         GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
         GraphicsDevice dev = env.getDefaultScreenDevice();
         return dev.getDefaultConfiguration();
      }

      public void setWindowTransparent(Window w, boolean transparent) {
      }

      protected void setDoubleBuffered(Component root, boolean buffered) {
         if(root instanceof JComponent) {
            ((JComponent)root).setDoubleBuffered(buffered);
         }

         if(root instanceof JRootPane && buffered) {
            ((JRootPane)root).setDoubleBuffered(true);
         } else if(root instanceof Container) {
            Component[] kids = ((Container)root).getComponents();

            for(int i = 0; i < kids.length; ++i) {
               this.setDoubleBuffered(kids[i], buffered);
            }
         }

      }

      protected void setLayersTransparent(Window w, boolean transparent) {
         Color bg = transparent?new Color(0, 0, 0, 0):null;
         if(w instanceof RootPaneContainer) {
            RootPaneContainer rpc = (RootPaneContainer)w;
            JRootPane root = rpc.getRootPane();
            JLayeredPane lp = root.getLayeredPane();
            Container c = root.getContentPane();
            JComponent content = c instanceof JComponent?(JComponent)c:null;
            if(transparent) {
               lp.putClientProperty("transparent-old-opaque", Boolean.valueOf(lp.isOpaque()));
               lp.setOpaque(false);
               root.putClientProperty("transparent-old-opaque", Boolean.valueOf(root.isOpaque()));
               root.setOpaque(false);
               if(content != null) {
                  content.putClientProperty("transparent-old-opaque", Boolean.valueOf(content.isOpaque()));
                  content.setOpaque(false);
               }

               root.putClientProperty("transparent-old-bg", root.getParent().getBackground());
            } else {
               lp.setOpaque(Boolean.TRUE.equals(lp.getClientProperty("transparent-old-opaque")));
               lp.putClientProperty("transparent-old-opaque", (Object)null);
               root.setOpaque(Boolean.TRUE.equals(root.getClientProperty("transparent-old-opaque")));
               root.putClientProperty("transparent-old-opaque", (Object)null);
               if(content != null) {
                  content.setOpaque(Boolean.TRUE.equals(content.getClientProperty("transparent-old-opaque")));
                  content.putClientProperty("transparent-old-opaque", (Object)null);
               }

               bg = (Color)root.getClientProperty("transparent-old-bg");
               root.putClientProperty("transparent-old-bg", (Object)null);
            }
         }

         w.setBackground(bg);
      }

      protected void setMask(Component c, Raster raster) {
         throw new UnsupportedOperationException("Window masking is not available");
      }

      protected void setWindowMask(Component w, Raster raster) {
         if(w.isLightweight()) {
            throw new IllegalArgumentException("Component must be heavyweight: " + w);
         } else {
            this.setMask(w, raster);
         }
      }

      public void setWindowMask(Component w, Shape mask) {
         this.setWindowMask(w, this.toRaster(mask));
      }

      public void setWindowMask(Component w, Icon mask) {
         this.setWindowMask(w, this.toRaster(w, mask));
      }

      protected void setForceHeavyweightPopups(Window w, boolean force) {
         if(!(w instanceof HeavyweightForcer)) {
            Window[] owned = w.getOwnedWindows();

            for(int b = 0; b < owned.length; ++b) {
               if(owned[b] instanceof HeavyweightForcer) {
                  if(force) {
                     return;
                  }

                  owned[b].dispose();
               }
            }

            Boolean var5 = Boolean.valueOf(System.getProperty("jna.force_hw_popups", "true"));
            if(force && var5.booleanValue()) {
               new HeavyweightForcer(w);
            }
         }

      }

      protected abstract class TransparentContentPane extends JPanel implements AWTEventListener {
         private static final long serialVersionUID = 1L;
         private boolean transparent;

         public TransparentContentPane(Container oldContent) {
            super(new BorderLayout());
            this.add(oldContent, "Center");
            this.setTransparent(true);
            if(oldContent instanceof JPanel) {
               ((JComponent)oldContent).setOpaque(false);
            }

         }

         public void addNotify() {
            super.addNotify();
            Toolkit.getDefaultToolkit().addAWTEventListener(this, 2L);
         }

         public void removeNotify() {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
            super.removeNotify();
         }

         public void setTransparent(boolean transparent) {
            this.transparent = transparent;
            this.setOpaque(!transparent);
            this.setDoubleBuffered(!transparent);
            this.repaint();
         }

         public void eventDispatched(AWTEvent e) {
            if(e.getID() == 300 && SwingUtilities.isDescendingFrom(((ContainerEvent)e).getChild(), this)) {
               Component child = ((ContainerEvent)e).getChild();
               NativeWindowUtils.this.setDoubleBuffered(child, false);
            }

         }

         public void paint(Graphics gr) {
            if(this.transparent) {
               Rectangle r = gr.getClipBounds();
               int w = r.width;
               int h = r.height;
               if(this.getWidth() > 0 && this.getHeight() > 0) {
                  BufferedImage buf = new BufferedImage(w, h, 3);
                  Graphics2D g = buf.createGraphics();
                  g.setComposite(AlphaComposite.Clear);
                  g.fillRect(0, 0, w, h);
                  g.dispose();
                  g = buf.createGraphics();
                  g.translate(-r.x, -r.y);
                  super.paint(g);
                  g.dispose();
                  this.paintDirect(buf, r);
               }
            } else {
               super.paint(gr);
            }

         }

         protected abstract void paintDirect(BufferedImage var1, Rectangle var2);
      }
   }

   protected static class RepaintTrigger extends JComponent {
      private static final long serialVersionUID = 1L;
      private final Listener listener = this.createListener();
      private final JComponent content;
      private Rectangle dirty;

      public RepaintTrigger(JComponent content) {
         this.content = content;
      }

      public void addNotify() {
         super.addNotify();
         Window w = SwingUtilities.getWindowAncestor(this);
         this.setSize(this.getParent().getSize());
         w.addComponentListener(this.listener);
         w.addWindowListener(this.listener);
         Toolkit.getDefaultToolkit().addAWTEventListener(this.listener, 48L);
      }

      public void removeNotify() {
         Toolkit.getDefaultToolkit().removeAWTEventListener(this.listener);
         Window w = SwingUtilities.getWindowAncestor(this);
         w.removeComponentListener(this.listener);
         w.removeWindowListener(this.listener);
         super.removeNotify();
      }

      protected void paintComponent(Graphics g) {
         Rectangle bounds = g.getClipBounds();
         if(this.dirty != null && this.dirty.contains(bounds)) {
            this.dirty = null;
         } else {
            if(this.dirty == null) {
               this.dirty = bounds;
            } else {
               this.dirty = this.dirty.union(bounds);
            }

            this.content.repaint(this.dirty);
         }

      }

      protected Listener createListener() {
         return new Listener();
      }

      protected class Listener extends WindowAdapter implements ComponentListener, HierarchyListener, AWTEventListener {
         public void windowOpened(WindowEvent e) {
            RepaintTrigger.this.repaint();
         }

         public void componentHidden(ComponentEvent e) {
         }

         public void componentMoved(ComponentEvent e) {
         }

         public void componentResized(ComponentEvent e) {
            RepaintTrigger.this.setSize(RepaintTrigger.this.getParent().getSize());
            RepaintTrigger.this.repaint();
         }

         public void componentShown(ComponentEvent e) {
            RepaintTrigger.this.repaint();
         }

         public void hierarchyChanged(HierarchyEvent e) {
            RepaintTrigger.this.repaint();
         }

         public void eventDispatched(AWTEvent e) {
            if(e instanceof MouseEvent) {
               Component src = ((MouseEvent)e).getComponent();
               if(src != null && SwingUtilities.isDescendingFrom(src, RepaintTrigger.this.content)) {
                  MouseEvent me = SwingUtilities.convertMouseEvent(src, (MouseEvent)e, RepaintTrigger.this.content);
                  Component c = SwingUtilities.getDeepestComponentAt(RepaintTrigger.this.content, me.getX(), me.getY());
                  if(c != null) {
                     RepaintTrigger.this.setCursor(c.getCursor());
                  }
               }
            }

         }
      }
   }

   private static class HeavyweightForcer extends Window {
      private static final long serialVersionUID = 1L;
      private final boolean packed;

      public HeavyweightForcer(Window parent) {
         super(parent);
         this.pack();
         this.packed = true;
      }

      public boolean isVisible() {
         return this.packed;
      }

      public Rectangle getBounds() {
         return this.getOwner().getBounds();
      }
   }
}
