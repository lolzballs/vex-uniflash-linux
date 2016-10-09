package com.sun.jna.platform.win32;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.StdCallLibrary;

public interface LMAccess extends StdCallLibrary {
   int FILTER_TEMP_DUPLICATE_ACCOUNT = 1;
   int FILTER_NORMAL_ACCOUNT = 2;
   int FILTER_INTERDOMAIN_TRUST_ACCOUNT = 8;
   int FILTER_WORKSTATION_TRUST_ACCOUNT = 16;
   int FILTER_SERVER_TRUST_ACCOUNT = 32;
   int USER_PRIV_MASK = 3;
   int USER_PRIV_GUEST = 0;
   int USER_PRIV_USER = 1;
   int USER_PRIV_ADMIN = 2;

   public static class GROUP_INFO_3 extends Structure {
      public WString grpi3_name;
      public WString grpi3_comment;
      public WinNT.PSID grpi3_group_sid;
      public int grpi3_attributes;

      public GROUP_INFO_3() {
      }

      public GROUP_INFO_3(Pointer memory) {
         this.useMemory(memory);
         this.read();
      }
   }

   public static class GROUP_INFO_2 extends Structure {
      public WString grpi2_name;
      public WString grpi2_comment;
      public int grpi2_group_id;
      public int grpi2_attributes;

      public GROUP_INFO_2() {
      }

      public GROUP_INFO_2(Pointer memory) {
         this.useMemory(memory);
         this.read();
      }
   }

   public static class GROUP_INFO_1 extends Structure {
      public WString grpi1_name;
      public WString grpi1_comment;

      public GROUP_INFO_1() {
      }

      public GROUP_INFO_1(Pointer memory) {
         this.useMemory(memory);
         this.read();
      }
   }

   public static class GROUP_INFO_0 extends Structure {
      public WString grpi0_name;

      public GROUP_INFO_0() {
      }

      public GROUP_INFO_0(Pointer memory) {
         this.useMemory(memory);
         this.read();
      }
   }

   public static class LOCALGROUP_USERS_INFO_0 extends Structure {
      public WString lgrui0_name;

      public LOCALGROUP_USERS_INFO_0() {
      }

      public LOCALGROUP_USERS_INFO_0(Pointer memory) {
         this.useMemory(memory);
         this.read();
      }
   }

   public static class GROUP_USERS_INFO_0 extends Structure {
      public WString grui0_name;

      public GROUP_USERS_INFO_0() {
      }

      public GROUP_USERS_INFO_0(Pointer memory) {
         this.useMemory(memory);
         this.read();
      }
   }

   public static class USER_INFO_1 extends Structure {
      public WString usri1_name;
      public WString usri1_password;
      public int usri1_password_age;
      public int usri1_priv;
      public WString usri1_home_dir;
      public WString usri1_comment;
      public int usri1_flags;
      public WString usri1_script_path;

      public USER_INFO_1() {
      }

      public USER_INFO_1(Pointer memory) {
         this.useMemory(memory);
         this.read();
      }
   }

   public static class USER_INFO_0 extends Structure {
      public WString usri0_name;

      public USER_INFO_0() {
      }

      public USER_INFO_0(Pointer memory) {
         this.useMemory(memory);
         this.read();
      }
   }

   public static class LOCALGROUP_INFO_1 extends Structure {
      public WString lgrui1_name;
      public WString lgrui1_comment;

      public LOCALGROUP_INFO_1() {
      }

      public LOCALGROUP_INFO_1(Pointer memory) {
         this.useMemory(memory);
         this.read();
      }
   }

   public static class LOCALGROUP_INFO_0 extends Structure {
      public WString lgrui0_name;

      public LOCALGROUP_INFO_0() {
      }

      public LOCALGROUP_INFO_0(Pointer memory) {
         this.useMemory(memory);
         this.read();
      }
   }
}
