package org.uniflash;

public class ProgressMonitorIndicator implements Indicator {
   private int lastProgress = 0;
   private Object mon;

   public ProgressMonitorIndicator(Object mon) {
      this.mon = mon;
   }

   public void begin() {
      this.lastProgress = 0;
   }

   public void message(String message) {
      //this.mon.subTask(message);
   }

   public void messageBegin(String message) {
      //this.mon.subTask(message);
   }

   public void messageEnd(String message) {
   }

   public void progress(int progress) {
      //this.mon.worked(progress - this.lastProgress);
      this.lastProgress = progress;
   }

   public void end() {
   }
}
