package org.uniflash;

import org.uniflash.Indicator;

public class CommandLineIndicator implements Indicator {
   private int lastPos = 0;

   public void begin() {
      System.out.println("Programming... ");
      System.out.println("0    10   20   30   40   50   60   70   80   90   100");
      System.out.println("==================================================");
   }

   public void message(String message) {
      System.out.println(message);
   }

   public void messageBegin(String message) {
      System.out.print(message + "... ");
   }

   public void messageEnd(String message) {
      System.out.println(message);
   }

   public void progress(int progress) {
      progress /= 2;
      if(progress > this.lastPos) {
         for(int i = this.lastPos; i < progress; ++i) {
            System.out.print('*');
         }

         this.lastPos = progress;
      }

   }

   public void end() {
      System.out.println();
      System.out.println("==================================================");
   }
}
