package org.stmflash;

public class STMDevice {
   public static final STMDevice[] STM_DEVICES = new STMDevice[]{new STMDevice(1042, "Low-density", 536871424, 536881152, 134250496, 4, 1024), new STMDevice(1040, "Medium-density", 536871424, 536891392, 134348800, 4, 1024), new STMDevice(1044, "High-density", 536871424, 536936448, 134742016, 2, 2048), new STMDevice(1048, "Connectivity line", 536875008, 536936448, 134479872, 2, 2048), new STMDevice(1056, "Medium-density VL", 536871424, 536879104, 134348800, 4, 1024), new STMDevice(1072, "XL-density", 536872960, 536969216, 135266304, 2, 2048)};
   private final int flashEnd;
   private final int flashStart;
   private final short id;
   private final String name;
   private final int pageCount;
   private final int pageSize;
   private final int ramEnd;
   private final int ramStart;

   public STMDevice(int id, String name, int ramStart, int ramEnd, int flashEnd, int pageCount, int pageSize) {
      this.flashEnd = flashEnd;
      this.flashStart = 134217728;
      this.id = (short)id;
      this.name = name;
      this.pageCount = pageCount;
      this.pageSize = pageSize;
      this.ramEnd = ramEnd;
      this.ramStart = ramStart;
   }

   public int getFlashEnd() {
      return this.flashEnd;
   }

   public int getFlashSize() {
      return this.getFlashEnd() - this.getFlashStart();
   }

   public int getFlashStart() {
      return this.flashStart;
   }

   public short getID() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public int getPageCount() {
      return this.pageCount;
   }

   public int getPageSize() {
      return this.pageSize;
   }

   public int getRamEnd() {
      return this.ramEnd;
   }

   public int getRAMSize() {
      return this.getRamEnd() - this.getRamStart();
   }

   public int getRamStart() {
      return this.ramStart;
   }
}
