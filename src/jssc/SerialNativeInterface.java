package jssc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SerialNativeInterface {
   private static final String libVersion = "2.6";
   private static final String libMinorSuffix = "0";
   public static final int OS_LINUX = 0;
   public static final int OS_WINDOWS = 1;
   public static final int OS_SOLARIS = 2;
   public static final int OS_MAC_OS_X = 3;
   private static int osType = -1;
   public static final long ERR_PORT_BUSY = -1L;
   public static final long ERR_PORT_NOT_FOUND = -2L;
   public static final long ERR_PERMISSION_DENIED = -3L;
   public static final long ERR_INCORRECT_SERIAL_PORT = -4L;
   public static final String PROPERTY_JSSC_NO_TIOCEXCL = "JSSC_NO_TIOCEXCL";
   public static final String PROPERTY_JSSC_IGNPAR = "JSSC_IGNPAR";
   public static final String PROPERTY_JSSC_PARMRK = "JSSC_PARMRK";

   private static boolean isLibFolderExist(String libFolderPath) {
      boolean returnValue = false;
      File folder = new File(libFolderPath);
      if(folder.exists() && folder.isDirectory()) {
         returnValue = true;
      }

      return returnValue;
   }

   private static boolean isLibFileExist(String libFilePath) {
      boolean returnValue = false;
      File folder = new File(libFilePath);
      if(folder.exists() && folder.isFile()) {
         returnValue = true;
      }

      return returnValue;
   }

   private static boolean extractLib(String libFilePath, String osName, String libName) {
      boolean returnValue = false;
      File libFile = new File(libFilePath);
      InputStream input = null;
      FileOutputStream output = null;
      input = SerialNativeInterface.class.getResourceAsStream("/libs/" + osName + "/" + libName);
      if(input != null) {
         byte[] buffer = new byte[4096];

         try {
            output = new FileOutputStream(libFilePath);

            int read;
            while((read = input.read(buffer)) != -1) {
               output.write(buffer, 0, read);
            }

            output.close();
            input.close();
            returnValue = true;
         } catch (Exception var13) {
            try {
               output.close();
               if(libFile.exists()) {
                  libFile.delete();
               }
            } catch (Exception var12) {
               ;
            }

            try {
               input.close();
            } catch (Exception var11) {
               ;
            }
         }
      }

      return returnValue;
   }

   public static int getOsType() {
      return osType;
   }

   public static String getLibraryVersion() {
      return "2.6.0";
   }

   public static String getLibraryBaseVersion() {
      return "2.6";
   }

   public static String getLibraryMinorSuffix() {
      return "0";
   }

   public native long openPort(String var1, boolean var2);

   public native boolean setParams(long var1, int var3, int var4, int var5, int var6, boolean var7, boolean var8, int var9);

   public native boolean purgePort(long var1, int var3);

   public native boolean closePort(long var1);

   public native boolean setEventsMask(long var1, int var3);

   public native int getEventsMask(long var1);

   public native int[][] waitEvents(long var1);

   public native boolean setRTS(long var1, boolean var3);

   public native boolean setDTR(long var1, boolean var3);

   public native byte[] readBytes(long var1, int var3);

   public native boolean writeBytes(long var1, byte[] var3);

   public native int[] getBuffersBytesCount(long var1);

   public native boolean setFlowControlMode(long var1, int var3);

   public native int getFlowControlMode(long var1);

   public native String[] getSerialPortNames();

   public native int[] getLinesStatus(long var1);

   public native boolean sendBreak(long var1, int var3);

   static {
      String osName = System.getProperty("os.name");
      String architecture = System.getProperty("os.arch");
      String userHome = System.getProperty("user.home");
      String fileSeparator = System.getProperty("file.separator");
      String tmpFolder = System.getProperty("java.io.tmpdir");
      String libRootFolder = (new File(userHome)).canWrite()?userHome:tmpFolder;
      String javaLibPath = System.getProperty("java.library.path");
      if(osName.equals("Linux")) {
         osName = "linux";
         osType = 0;
      } else if(osName.startsWith("Win")) {
         osName = "windows";
         osType = 1;
      } else if(osName.equals("SunOS")) {
         osName = "solaris";
         osType = 2;
      } else if(osName.equals("Mac OS X") || osName.equals("Darwin")) {
         osName = "mac_os_x";
         osType = 3;
      }

      if(!architecture.equals("i386") && !architecture.equals("i686")) {
         if(!architecture.equals("amd64") && !architecture.equals("universal")) {
            if(architecture.equals("arm")) {
               String loadLib = "sf";
               if(!javaLibPath.toLowerCase().contains("gnueabihf") && !javaLibPath.toLowerCase().contains("armhf")) {
                  try {
                     Process ex = Runtime.getRuntime().exec("readelf -A /proc/self/exe");
                     BufferedReader reader = new BufferedReader(new InputStreamReader(ex.getInputStream()));
                     String buffer = "";

                     while((buffer = reader.readLine()) != null && !buffer.isEmpty()) {
                        if(buffer.toLowerCase().contains("Tag_ABI_VFP_args".toLowerCase())) {
                           loadLib = "hf";
                           break;
                        }
                     }

                     reader.close();
                  } catch (Exception var13) {
                     ;
                  }
               } else {
                  loadLib = "hf";
               }

               architecture = "arm" + loadLib;
            }
         } else {
            architecture = "x86_64";
         }
      } else {
         architecture = "x86";
      }

      String libFolderPath = libRootFolder + fileSeparator + ".jssc" + fileSeparator + osName;
      String libName = "jSSC-2.6_" + architecture;
      libName = System.mapLibraryName(libName);
      if(libName.endsWith(".dylib")) {
         libName = libName.replace(".dylib", ".jnilib");
      }

      boolean loadLib1 = false;
      if(isLibFolderExist(libFolderPath)) {
         if(isLibFileExist(libFolderPath + fileSeparator + libName)) {
            loadLib1 = true;
         } else if(extractLib(libFolderPath + fileSeparator + libName, osName, libName)) {
            loadLib1 = true;
         }
      } else if((new File(libFolderPath)).mkdirs() && extractLib(libFolderPath + fileSeparator + libName, osName, libName)) {
         loadLib1 = true;
      }

      if(loadLib1) {
         System.load(libFolderPath + fileSeparator + libName);
      }

   }
}
