package com.sun.jna.platform.win32;

import com.sun.jna.WString;
import com.sun.jna.platform.FileUtils;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.ShellAPI;
import java.io.File;
import java.io.IOException;

public class W32FileUtils extends FileUtils {
   public boolean hasTrash() {
      return true;
   }

   public void moveToTrash(File[] files) throws IOException {
      Shell32 shell = Shell32.INSTANCE;
      ShellAPI.SHFILEOPSTRUCT fileop = new ShellAPI.SHFILEOPSTRUCT();
      fileop.wFunc = 3;
      String[] paths = new String[files.length];

      int ret;
      for(ret = 0; ret < paths.length; ++ret) {
         paths[ret] = files[ret].getAbsolutePath();
      }

      fileop.pFrom = new WString(fileop.encodePaths(paths));
      fileop.fFlags = 84;
      ret = shell.SHFileOperation(fileop);
      if(ret != 0) {
         throw new IOException("Move to trash failed: " + Kernel32Util.formatMessageFromLastErrorCode(ret));
      } else if(fileop.fAnyOperationsAborted) {
         throw new IOException("Move to trash aborted");
      }
   }
}
