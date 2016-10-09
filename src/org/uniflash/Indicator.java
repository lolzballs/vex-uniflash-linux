package org.uniflash;

public interface Indicator {
   void begin();

   void message(String var1);

   void messageBegin(String var1);

   void messageEnd(String var1);

   void progress(int var1);

   void end();
}
