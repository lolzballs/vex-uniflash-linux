package com.sun.jna.platform;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class RasterRangesUtils {
   private static final int[] subColMasks = new int[]{128, 64, 32, 16, 8, 4, 2, 1};
   private static final Comparator<Object> COMPARATOR = new Comparator() {
      public int compare(Object o1, Object o2) {
         return ((Rectangle)o1).x - ((Rectangle)o2).x;
      }
   };

   public static boolean outputOccupiedRanges(Raster raster, RangesOutput out) {
      Rectangle bounds = raster.getBounds();
      SampleModel sampleModel = raster.getSampleModel();
      boolean hasAlpha = sampleModel.getNumBands() == 4;
      if(raster.getParent() == null && bounds.x == 0 && bounds.y == 0) {
         DataBuffer pixels = raster.getDataBuffer();
         if(pixels.getNumBanks() == 1) {
            if(sampleModel instanceof MultiPixelPackedSampleModel) {
               MultiPixelPackedSampleModel packedSampleModel = (MultiPixelPackedSampleModel)sampleModel;
               if(packedSampleModel.getPixelBitStride() == 1) {
                  return outputOccupiedRangesOfBinaryPixels(((DataBufferByte)pixels).getData(), bounds.width, bounds.height, out);
               }
            } else if(sampleModel instanceof SinglePixelPackedSampleModel && sampleModel.getDataType() == 3) {
               return outputOccupiedRanges(((DataBufferInt)pixels).getData(), bounds.width, bounds.height, hasAlpha?-16777216:16777215, out);
            }
         }
      }

      int[] pixels1 = raster.getPixels(0, 0, bounds.width, bounds.height, (int[])null);
      return outputOccupiedRanges(pixels1, bounds.width, bounds.height, hasAlpha?-16777216:16777215, out);
   }

   public static boolean outputOccupiedRangesOfBinaryPixels(byte[] binaryBits, int w, int h, RangesOutput out) {
      HashSet rects = new HashSet();
      Object prevLine = Collections.EMPTY_SET;
      int scanlineBytes = binaryBits.length / h;

      for(int i = 0; i < h; ++i) {
         TreeSet r = new TreeSet(COMPARATOR);
         int rowOffsetBytes = i * scanlineBytes;
         int startCol = -1;

         for(int unmerged = 0; unmerged < scanlineBytes; ++unmerged) {
            int firstByteCol = unmerged << 3;
            byte byteColBits = binaryBits[rowOffsetBytes + unmerged];
            if(byteColBits == 0) {
               if(startCol >= 0) {
                  r.add(new Rectangle(startCol, i, firstByteCol - startCol, 1));
                  startCol = -1;
               }
            } else if(byteColBits == 255) {
               if(startCol < 0) {
                  startCol = firstByteCol;
               }
            } else {
               for(int subCol = 0; subCol < 8; ++subCol) {
                  int col = firstByteCol | subCol;
                  if((byteColBits & subColMasks[subCol]) != 0) {
                     if(startCol < 0) {
                        startCol = col;
                     }
                  } else if(startCol >= 0) {
                     r.add(new Rectangle(startCol, i, col - startCol, 1));
                     startCol = -1;
                  }
               }
            }
         }

         if(startCol >= 0) {
            r.add(new Rectangle(startCol, i, w - startCol, 1));
         }

         Set var18 = mergeRects((Set)prevLine, r);
         rects.addAll(var18);
         prevLine = r;
      }

      rects.addAll((Collection)prevLine);
      Iterator var16 = rects.iterator();

      Rectangle var17;
      do {
         if(!var16.hasNext()) {
            return true;
         }

         var17 = (Rectangle)var16.next();
      } while(out.outputRange(var17.x, var17.y, var17.width, var17.height));

      return false;
   }

   public static boolean outputOccupiedRanges(int[] pixels, int w, int h, int occupationMask, RangesOutput out) {
      HashSet rects = new HashSet();
      Object prevLine = Collections.EMPTY_SET;

      for(int i = 0; i < h; ++i) {
         TreeSet r = new TreeSet(COMPARATOR);
         int idxOffset = i * w;
         int startCol = -1;

         for(int unmerged = 0; unmerged < w; ++unmerged) {
            if((pixels[idxOffset + unmerged] & occupationMask) != 0) {
               if(startCol < 0) {
                  startCol = unmerged;
               }
            } else if(startCol >= 0) {
               r.add(new Rectangle(startCol, i, unmerged - startCol, 1));
               startCol = -1;
            }
         }

         if(startCol >= 0) {
            r.add(new Rectangle(startCol, i, w - startCol, 1));
         }

         Set var14 = mergeRects((Set)prevLine, r);
         rects.addAll(var14);
         prevLine = r;
      }

      rects.addAll((Collection)prevLine);
      Iterator var12 = rects.iterator();

      Rectangle var13;
      do {
         if(!var12.hasNext()) {
            return true;
         }

         var13 = (Rectangle)var12.next();
      } while(out.outputRange(var13.x, var13.y, var13.width, var13.height));

      return false;
   }

   private static Set<Rectangle> mergeRects(Set<Rectangle> prev, Set<Rectangle> current) {
      HashSet unmerged = new HashSet(prev);
      if(!prev.isEmpty() && !current.isEmpty()) {
         Rectangle[] pr = (Rectangle[])prev.toArray(new Rectangle[prev.size()]);
         Rectangle[] cr = (Rectangle[])current.toArray(new Rectangle[current.size()]);
         int ipr = 0;
         int icr = 0;

         while(true) {
            while(ipr < pr.length && icr < cr.length) {
               while(cr[icr].x < pr[ipr].x) {
                  ++icr;
                  if(icr == cr.length) {
                     return unmerged;
                  }
               }

               if(cr[icr].x == pr[ipr].x && cr[icr].width == pr[ipr].width) {
                  unmerged.remove(pr[ipr]);
                  cr[icr].y = pr[ipr].y;
                  cr[icr].height = pr[ipr].height + 1;
                  ++icr;
               } else {
                  ++ipr;
               }
            }

            return unmerged;
         }
      } else {
         return unmerged;
      }
   }

   public interface RangesOutput {
      boolean outputRange(int var1, int var2, int var3, int var4);
   }
}
