package com.sun.jna.platform.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.ByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.nio.Buffer;

public interface Kernel32 extends StdCallLibrary {
   Kernel32 INSTANCE = (Kernel32)Native.loadLibrary("kernel32", Kernel32.class, W32APIOptions.UNICODE_OPTIONS);

   Pointer LocalFree(Pointer var1);

   Pointer GlobalFree(Pointer var1);

   WinDef.HMODULE GetModuleHandle(String var1);

   void GetSystemTime(WinBase.SYSTEMTIME var1);

   int GetTickCount();

   int GetCurrentThreadId();

   WinNT.HANDLE GetCurrentThread();

   int GetCurrentProcessId();

   WinNT.HANDLE GetCurrentProcess();

   int GetProcessId(WinNT.HANDLE var1);

   int GetProcessVersion(int var1);

   boolean GetExitCodeProcess(WinNT.HANDLE var1, IntByReference var2);

   boolean TerminateProcess(WinNT.HANDLE var1, int var2);

   int GetLastError();

   void SetLastError(int var1);

   int GetDriveType(String var1);

   int FormatMessage(int var1, Pointer var2, int var3, int var4, PointerByReference var5, int var6, Pointer var7);

   int FormatMessage(int var1, Pointer var2, int var3, int var4, Buffer var5, int var6, Pointer var7);

   WinNT.HANDLE CreateFile(String var1, int var2, int var3, WinBase.SECURITY_ATTRIBUTES var4, int var5, int var6, WinNT.HANDLE var7);

   boolean CreateDirectory(String var1, WinBase.SECURITY_ATTRIBUTES var2);

   boolean ReadFile(WinNT.HANDLE var1, Buffer var2, int var3, IntByReference var4, WinBase.OVERLAPPED var5);

   WinNT.HANDLE CreateIoCompletionPort(WinNT.HANDLE var1, WinNT.HANDLE var2, Pointer var3, int var4);

   boolean GetQueuedCompletionStatus(WinNT.HANDLE var1, IntByReference var2, ByReference var3, PointerByReference var4, int var5);

   boolean PostQueuedCompletionStatus(WinNT.HANDLE var1, int var2, Pointer var3, WinBase.OVERLAPPED var4);

   int WaitForSingleObject(WinNT.HANDLE var1, int var2);

   int WaitForMultipleObjects(int var1, WinNT.HANDLE[] var2, boolean var3, int var4);

   boolean DuplicateHandle(WinNT.HANDLE var1, WinNT.HANDLE var2, WinNT.HANDLE var3, WinNT.HANDLEByReference var4, int var5, boolean var6, int var7);

   boolean CloseHandle(WinNT.HANDLE var1);

   boolean ReadDirectoryChangesW(WinNT.HANDLE var1, WinNT.FILE_NOTIFY_INFORMATION var2, int var3, boolean var4, int var5, IntByReference var6, WinBase.OVERLAPPED var7, OVERLAPPED_COMPLETION_ROUTINE var8);

   int GetShortPathName(String var1, char[] var2, int var3);

   Pointer LocalAlloc(int var1, int var2);

   boolean WriteFile(WinNT.HANDLE var1, byte[] var2, int var3, IntByReference var4, WinBase.OVERLAPPED var5);

   WinNT.HANDLE CreateEvent(WinBase.SECURITY_ATTRIBUTES var1, boolean var2, boolean var3, String var4);

   boolean SetEvent(WinNT.HANDLE var1);

   boolean PulseEvent(WinNT.HANDLE var1);

   WinNT.HANDLE CreateFileMapping(WinNT.HANDLE var1, WinBase.SECURITY_ATTRIBUTES var2, int var3, int var4, int var5, String var6);

   Pointer MapViewOfFile(WinNT.HANDLE var1, int var2, int var3, int var4, int var5);

   boolean UnmapViewOfFile(Pointer var1);

   boolean GetComputerName(char[] var1, IntByReference var2);

   WinNT.HANDLE OpenThread(int var1, boolean var2, int var3);

   WinNT.HANDLE OpenProcess(int var1, boolean var2, int var3);

   WinDef.DWORD GetTempPath(WinDef.DWORD var1, char[] var2);

   WinDef.DWORD GetVersion();

   boolean GetVersionEx(WinNT.OSVERSIONINFO var1);

   boolean GetVersionEx(WinNT.OSVERSIONINFOEX var1);

   void GetSystemInfo(WinBase.SYSTEM_INFO var1);

   void GetNativeSystemInfo(WinBase.SYSTEM_INFO var1);

   boolean IsWow64Process(WinNT.HANDLE var1, IntByReference var2);

   boolean GlobalMemoryStatusEx(WinBase.MEMORYSTATUSEX var1);

   WinDef.DWORD GetLogicalDriveStrings(WinDef.DWORD var1, char[] var2);

   boolean GetDiskFreeSpaceEx(String var1, WinNT.LARGE_INTEGER.ByReference var2, WinNT.LARGE_INTEGER.ByReference var3, WinNT.LARGE_INTEGER.ByReference var4);

   boolean DeleteFile(String var1);

   boolean CreatePipe(WinNT.HANDLEByReference var1, WinNT.HANDLEByReference var2, WinBase.SECURITY_ATTRIBUTES var3, int var4);

   boolean SetHandleInformation(WinNT.HANDLE var1, int var2, int var3);

   int GetFileAttributes(String var1);

   public interface OVERLAPPED_COMPLETION_ROUTINE extends StdCallCallback {
      void callback(int var1, int var2, WinBase.OVERLAPPED var3);
   }
}
