package com.sun.jna;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.util.*;

public abstract class Structure {
   private static final boolean REVERSE_FIELDS;
   private static final boolean REQUIRES_FIELD_ORDER;
   static final boolean isPPC;
   static final boolean isSPARC;
   public static final int ALIGN_DEFAULT = 0;
   public static final int ALIGN_NONE = 1;
   public static final int ALIGN_GNUC = 2;
   public static final int ALIGN_MSVC = 3;
   private static final int MAX_GNUC_ALIGNMENT;
   protected static final int CALCULATE_SIZE = -1;
   private Pointer memory;
   private int size;
   private int alignType;
   private int structAlignment;
   private final Map structFields;
   private final Map nativeStrings;
   private TypeMapper typeMapper;
   private long typeInfo;
   private List fieldOrder;
   private boolean autoRead;
   private boolean autoWrite;
   private Structure[] array;
   private static final ThreadLocal reads;
   private static final ThreadLocal busy;

   protected Structure() {
      this((Pointer)null);
   }

   protected Structure(TypeMapper mapper) {
      this((Pointer)null, 0, mapper);
   }

   protected Structure(Pointer p) {
      this(p, 0);
   }

   protected Structure(Pointer p, int alignment) {
      this(p, alignment, (TypeMapper)null);
   }

   protected Structure(Pointer p, int alignment, TypeMapper mapper) {
      this.size = -1;
      this.structFields = new LinkedHashMap();
      this.nativeStrings = new HashMap();
      this.autoRead = true;
      this.autoWrite = true;
      this.setAlignType(alignment);
      this.setTypeMapper(mapper);
      if(p != null) {
         this.useMemory(p);
      } else {
         this.allocateMemory(-1);
      }

   }

   Map fields() {
      return this.structFields;
   }

   protected void setTypeMapper(TypeMapper mapper) {
      if(mapper == null) {
         Class declaring = this.getClass().getDeclaringClass();
         if(declaring != null) {
            mapper = Native.getTypeMapper(declaring);
         }
      }

      this.typeMapper = mapper;
      this.size = -1;
      if(this.memory instanceof AutoAllocated) {
         this.memory = null;
      }

   }

   protected void setAlignType(int alignType) {
      if(alignType == 0) {
         Class declaring = this.getClass().getDeclaringClass();
         if(declaring != null) {
            alignType = Native.getStructureAlignment(declaring);
         }

         if(alignType == 0) {
            if(Platform.isWindows()) {
               alignType = 3;
            } else {
               alignType = 2;
            }
         }
      }

      this.alignType = alignType;
      this.size = -1;
      if(this.memory instanceof AutoAllocated) {
         this.memory = null;
      }

   }

   protected Memory autoAllocate(int size) {
      return new AutoAllocated(size);
   }

   protected void useMemory(Pointer m) {
      this.useMemory(m, 0);
   }

   protected void useMemory(Pointer m, int offset) {
      try {
         this.memory = m;
         if(this.size == -1) {
            this.size = this.calculateSize(false);
         }

         if(this.size != -1) {
            this.memory = m.share((long)offset, (long)this.size);
         }

         this.array = null;
      } catch (IndexOutOfBoundsException var4) {
         throw new IllegalArgumentException("Structure exceeds provided memory bounds");
      }
   }

   protected void ensureAllocated() {
      if(this.memory == null) {
         this.allocateMemory();
      } else if(this.size == -1) {
         this.size = this.calculateSize(true);
      }

   }

   protected void allocateMemory() {
      this.allocateMemory(this.calculateSize(true));
   }

   protected void allocateMemory(int size) {
      if(size == -1) {
         size = this.calculateSize(false);
      } else if(size <= 0) {
         throw new IllegalArgumentException("Structure size must be greater than zero: " + size);
      }

      if(size != -1) {
         if(this.memory == null || this.memory instanceof AutoAllocated) {
            this.memory = this.autoAllocate(size);
         }

         this.size = size;
      }

   }

   public int size() {
      this.ensureAllocated();
      if(this.size == -1) {
         this.size = this.calculateSize(true);
      }

      return this.size;
   }

   public void clear() {
      this.memory.clear((long)this.size());
   }

   public Pointer getPointer() {
      this.ensureAllocated();
      return this.memory;
   }

   static Set busy() {
      return (Set)busy.get();
   }

   static Map reading() {
      return (Map)reads.get();
   }

   public void read() {
      this.ensureAllocated();
      if(!busy().contains(this)) {
         busy().add(this);
         if(this instanceof ByReference) {
            reading().put(this.getPointer(), this);
         }

         try {
            Iterator i = this.structFields.values().iterator();

            while(i.hasNext()) {
               this.readField((StructField)i.next());
            }
         } finally {
            busy().remove(this);
            if(reading().get(this.getPointer()) == this) {
               reading().remove(this.getPointer());
            }

         }

      }
   }

   public Object readField(String name) {
      this.ensureAllocated();
      StructField f = (StructField)this.structFields.get(name);
      if(f == null) {
         throw new IllegalArgumentException("No such field: " + name);
      } else {
         return this.readField(f);
      }
   }

   Object getField(StructField structField) {
      try {
         return structField.field.get(this);
      } catch (Exception var3) {
         throw new Error("Exception reading field \'" + structField.name + "\' in " + this.getClass() + ": " + var3);
      }
   }

   void setField(StructField structField, Object value) {
      try {
         structField.field.set(this, value);
      } catch (IllegalAccessException var4) {
         throw new Error("Unexpectedly unable to write to field \'" + structField.name + "\' within " + this.getClass() + ": " + var4);
      }
   }

   static Structure updateStructureByReference(Class type, Structure s, Pointer address) {
      if(address == null) {
         s = null;
      } else {
         if(s == null || !address.equals(s.getPointer())) {
            Structure s1 = (Structure)reading().get(address);
            if(s1 != null && type.equals(s1.getClass())) {
               s = s1;
            } else {
               s = newInstance(type);
               s.useMemory(address);
            }
         }

         s.autoRead();
      }

      return s;
   }

   Object readField(StructField structField) {
      int offset = structField.offset;
      Class fieldType = structField.type;
      FromNativeConverter readConverter = structField.readConverter;
      if(readConverter != null) {
         fieldType = readConverter.nativeType();
      }

      Object currentValue = !Structure.class.isAssignableFrom(fieldType) && !Callback.class.isAssignableFrom(fieldType) && !Buffer.class.isAssignableFrom(fieldType) && !Pointer.class.isAssignableFrom(fieldType) && !NativeMapped.class.isAssignableFrom(fieldType) && !fieldType.isArray()?null:this.getField(structField);
      Object result = this.memory.getValue((long)offset, fieldType, currentValue);
      if(readConverter != null) {
         result = readConverter.fromNative(result, structField.context);
      }

      this.setField(structField, result);
      return result;
   }

   public void write() {
      this.ensureAllocated();
      if(this instanceof ByValue) {
         this.getTypeInfo();
      }

      if(!busy().contains(this)) {
         busy().add(this);

         try {
            Iterator i = this.structFields.values().iterator();

            while(i.hasNext()) {
               StructField sf = (StructField)i.next();
               if(!sf.isVolatile) {
                  this.writeField(sf);
               }
            }
         } finally {
            busy().remove(this);
         }

      }
   }

   public void writeField(String name) {
      this.ensureAllocated();
      StructField f = (StructField)this.structFields.get(name);
      if(f == null) {
         throw new IllegalArgumentException("No such field: " + name);
      } else {
         this.writeField(f);
      }
   }

   public void writeField(String name, Object value) {
      this.ensureAllocated();
      StructField f = (StructField)this.structFields.get(name);
      if(f == null) {
         throw new IllegalArgumentException("No such field: " + name);
      } else {
         this.setField(f, value);
         this.writeField(f);
      }
   }

   void writeField(StructField structField) {
      if(!structField.isReadOnly) {
         int offset = structField.offset;
         Object value = this.getField(structField);
         Class fieldType = structField.type;
         ToNativeConverter converter = structField.writeConverter;
         if(converter != null) {
            value = converter.toNative(value, new StructureWriteContext(this, structField.field));
            fieldType = converter.nativeType();
         }

         if(String.class == fieldType || WString.class == fieldType) {
            boolean e = fieldType == WString.class;
            if(value != null) {
               NativeString msg = new NativeString(value.toString(), e);
               this.nativeStrings.put(structField.name, msg);
               value = msg.getPointer();
            } else {
               value = null;
               this.nativeStrings.remove(structField.name);
            }
         }

         try {
            this.memory.setValue((long)offset, value, fieldType);
         } catch (IllegalArgumentException var8) {
            var8.printStackTrace();
            String msg1 = "Structure field \"" + structField.name + "\" was declared as " + structField.type + (structField.type == fieldType?"":" (native type " + fieldType + ")") + ", which is not supported within a Structure";
            throw new IllegalArgumentException(msg1);
         }
      }
   }

   private boolean hasFieldOrder() {
      synchronized(this) {
         return this.fieldOrder != null;
      }
   }

   protected List getFieldOrder() {
      synchronized(this) {
         if(this.fieldOrder == null) {
            this.fieldOrder = new ArrayList();
         }

         return this.fieldOrder;
      }
   }

   protected void setFieldOrder(String[] fields) {
      this.getFieldOrder().addAll(Arrays.asList(fields));
      this.size = -1;
      if(this.memory instanceof AutoAllocated) {
         this.memory = null;
      }

   }

   protected void sortFields(List fields, List names) {
      for(int i = 0; i < names.size(); ++i) {
         String name = (String)names.get(i);

         for(int f = 0; f < fields.size(); ++f) {
            Field field = (Field)fields.get(f);
            if(name.equals(field.getName())) {
               Collections.swap(fields, i, f);
               break;
            }
         }
      }

   }

   protected List getFields(boolean force) {
      ArrayList flist = new ArrayList();

      for(Class fieldOrder = this.getClass(); !fieldOrder.equals(Structure.class); fieldOrder = fieldOrder.getSuperclass()) {
         ArrayList classFields = new ArrayList();
         Field[] fields = fieldOrder.getDeclaredFields();

         for(int i = 0; i < fields.length; ++i) {
            int modifiers = fields[i].getModifiers();
            if(!Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
               classFields.add(fields[i]);
            }
         }

         if(REVERSE_FIELDS) {
            Collections.reverse(classFields);
         }

         flist.addAll(0, classFields);
      }

      if(REQUIRES_FIELD_ORDER || this.hasFieldOrder()) {
         List var8 = this.getFieldOrder();
         if(var8.size() < flist.size()) {
            if(force) {
               throw new Error("This VM does not store fields in a predictable order; you must use Structure.setFieldOrder to explicitly indicate the field order: " + System.getProperty("java.vendor") + ", " + System.getProperty("java.version"));
            }

            return null;
         }

         this.sortFields(flist, var8);
      }

      return flist;
   }

   int calculateSize(boolean force) {
      this.structAlignment = 1;
      int calculatedSize = 0;
      List fields = this.getFields(force);
      if(fields == null) {
         return -1;
      } else {
         boolean firstField = true;

         for(Iterator size = fields.iterator(); size.hasNext(); firstField = false) {
            Field field = (Field)size.next();
            int modifiers = field.getModifiers();
            Class type = field.getType();
            StructField structField = new StructField();
            structField.isVolatile = Modifier.isVolatile(modifiers);
            structField.isReadOnly = Modifier.isFinal(modifiers);
            if(Modifier.isFinal(modifiers)) {
               field.setAccessible(true);
            }

            structField.field = field;
            structField.name = field.getName();
            structField.type = type;
            if(Callback.class.isAssignableFrom(type) && !type.isInterface()) {
               throw new IllegalArgumentException("Structure Callback field \'" + field.getName() + "\' must be an interface");
            }

            if(type.isArray() && Structure.class.equals(type.getComponentType())) {
               String fieldAlignment2 = "Nested Structure arrays must use a derived Structure type so that the size of the elements can be determined";
               throw new IllegalArgumentException(fieldAlignment2);
            }

            boolean fieldAlignment = true;
            if(Modifier.isPublic(field.getModifiers())) {
               Object value = this.getField(structField);
               if(value == null) {
                  if(Structure.class.isAssignableFrom(type) && !ByReference.class.isAssignableFrom(type)) {
                     try {
                        value = newInstance(type);
                        this.setField(structField, value);
                     } catch (IllegalArgumentException var16) {
                        String e = "Can\'t determine size of nested structure: " + var16.getMessage();
                        throw new IllegalArgumentException(e);
                     }
                  } else if(type.isArray()) {
                     if(force) {
                        throw new IllegalStateException("Array fields must be initialized");
                     }

                     return -1;
                  }
               }

               Class nativeType = type;
               if(NativeMapped.class.isAssignableFrom(type)) {
                  NativeMappedConverter e2 = NativeMappedConverter.getInstance(type);
                  if(value == null) {
                     value = e2.defaultValue();
                     this.setField(structField, value);
                  }

                  nativeType = e2.nativeType();
                  structField.writeConverter = e2;
                  structField.readConverter = e2;
                  structField.context = new StructureReadContext(this, field);
               } else if(this.typeMapper != null) {
                  ToNativeConverter e1 = this.typeMapper.getToNativeConverter(type);
                  FromNativeConverter msg = this.typeMapper.getFromNativeConverter(type);
                  if(e1 != null && msg != null) {
                     value = e1.toNative(value, new StructureWriteContext(this, structField.field));
                     nativeType = value != null?value.getClass():Pointer.class;
                     structField.writeConverter = e1;
                     structField.readConverter = msg;
                     structField.context = new StructureReadContext(this, field);
                  } else if(e1 != null || msg != null) {
                     String msg1 = "Structures require bidirectional type conversion for " + type;
                     throw new IllegalArgumentException(msg1);
                  }
               }

               int fieldAlignment1;
               try {
                  structField.size = Native.getNativeSize(nativeType, value);
                  fieldAlignment1 = this.getNativeAlignment(nativeType, value, firstField);
               } catch (IllegalArgumentException var17) {
                  if(!force && this.typeMapper == null) {
                     return -1;
                  }

                  String msg2 = "Invalid Structure field in " + this.getClass() + ", field name \'" + structField.name + "\', " + structField.type + ": " + var17.getMessage();
                  throw new IllegalArgumentException(msg2);
               }

               this.structAlignment = Math.max(this.structAlignment, fieldAlignment1);
               if(calculatedSize % fieldAlignment1 != 0) {
                  calculatedSize += fieldAlignment1 - calculatedSize % fieldAlignment1;
               }

               structField.offset = calculatedSize;
               calculatedSize += structField.size;
               this.structFields.put(structField.name, structField);
            }
         }

         if(calculatedSize > 0) {
            int size1 = this.calculateAlignedSize(calculatedSize);
            if(this instanceof ByValue) {
               this.getTypeInfo();
            }

            if(this.memory != null && !(this.memory instanceof AutoAllocated)) {
               this.memory = this.memory.share(0L, (long)size1);
            }

            return size1;
         } else {
            throw new IllegalArgumentException("Structure " + this.getClass() + " has unknown size (ensure " + "all fields are public)");
         }
      }
   }

   int calculateAlignedSize(int calculatedSize) {
      if(this.alignType != 1 && calculatedSize % this.structAlignment != 0) {
         calculatedSize += this.structAlignment - calculatedSize % this.structAlignment;
      }

      return calculatedSize;
   }

   protected int getStructAlignment() {
      if(this.size == -1) {
         this.calculateSize(true);
      }

      return this.structAlignment;
   }

   protected int getNativeAlignment(Class type, Object value, boolean isFirstElement) {
      boolean alignment = true;
      if(NativeMapped.class.isAssignableFrom(type)) {
         NativeMappedConverter size = NativeMappedConverter.getInstance(type);
         type = size.nativeType();
         value = size.toNative(value, new ToNativeContext());
      }

      int size1 = Native.getNativeSize(type, value);
      int alignment1;
      if(!type.isPrimitive() && Long.class != type && Integer.class != type && Short.class != type && Character.class != type && Byte.class != type && Boolean.class != type && Float.class != type && Double.class != type) {
         if(Pointer.class != type && !Buffer.class.isAssignableFrom(type) && !Callback.class.isAssignableFrom(type) && WString.class != type && String.class != type) {
            if(Structure.class.isAssignableFrom(type)) {
               if(ByReference.class.isAssignableFrom(type)) {
                  alignment1 = Pointer.SIZE;
               } else {
                  if(value == null) {
                     value = newInstance(type);
                  }

                  alignment1 = ((Structure)value).getStructAlignment();
               }
            } else {
               if(!type.isArray()) {
                  throw new IllegalArgumentException("Type " + type + " has unknown " + "native alignment");
               }

               alignment1 = this.getNativeAlignment(type.getComponentType(), (Object)null, isFirstElement);
            }
         } else {
            alignment1 = Pointer.SIZE;
         }
      } else {
         alignment1 = size1;
      }

      if(this.alignType == 1) {
         alignment1 = 1;
      } else if(this.alignType == 3) {
         alignment1 = Math.min(8, alignment1);
      } else if(this.alignType == 2 && (!isFirstElement || !Platform.isMac() || !isPPC)) {
         alignment1 = Math.min(MAX_GNUC_ALIGNMENT, alignment1);
      }

      return alignment1;
   }

   public String toString() {
      return this.toString(0, true);
   }

   private String format(Class type) {
      String s = type.getName();
      int dot = s.lastIndexOf(".");
      return s.substring(dot + 1);
   }

   private String toString(int indent, boolean showContents) {
      String LS = System.getProperty("line.separator");
      String name = this.format(this.getClass()) + "(" + this.getPointer() + ")";
      if(!(this.getPointer() instanceof Memory)) {
         name = name + " (" + this.size() + " bytes)";
      }

      String prefix = "";

      for(int contents = 0; contents < indent; ++contents) {
         prefix = prefix + "  ";
      }

      String var12 = LS;
      if(!showContents) {
         var12 = "...}";
      } else {
         Iterator buf = this.structFields.values().iterator();

         while(buf.hasNext()) {
            StructField BYTES_PER_ROW = (StructField)buf.next();
            Object i = this.getField(BYTES_PER_ROW);
            String type = this.format(BYTES_PER_ROW.type);
            String index = "";
            var12 = var12 + prefix;
            if(BYTES_PER_ROW.type.isArray() && i != null) {
               type = this.format(BYTES_PER_ROW.type.getComponentType());
               index = "[" + Array.getLength(i) + "]";
            }

            var12 = var12 + "  " + type + " " + BYTES_PER_ROW.name + index + "@" + Integer.toHexString(BYTES_PER_ROW.offset);
            if(i instanceof Structure) {
               i = ((Structure)i).toString(indent + 1, !(i instanceof ByReference));
            }

            var12 = var12 + "=";
            if(i instanceof Long) {
               var12 = var12 + Long.toHexString(((Long)i).longValue());
            } else if(i instanceof Integer) {
               var12 = var12 + Integer.toHexString(((Integer)i).intValue());
            } else if(i instanceof Short) {
               var12 = var12 + Integer.toHexString(((Short)i).shortValue());
            } else if(i instanceof Byte) {
               var12 = var12 + Integer.toHexString(((Byte)i).byteValue());
            } else {
               var12 = var12 + String.valueOf(i).trim();
            }

            var12 = var12 + LS;
            if(!buf.hasNext()) {
               var12 = var12 + prefix + "}";
            }
         }
      }

      if(indent == 0 && Boolean.getBoolean("jna.dump_memory")) {
         byte[] var13 = this.getPointer().getByteArray(0L, this.size());
         boolean var14 = true;
         var12 = var12 + LS + "memory dump" + LS;

         for(int var15 = 0; var15 < var13.length; ++var15) {
            if(var15 % 4 == 0) {
               var12 = var12 + "[";
            }

            if(var13[var15] >= 0 && var13[var15] < 16) {
               var12 = var12 + "0";
            }

            var12 = var12 + Integer.toHexString(var13[var15] & 255);
            if(var15 % 4 == 3 && var15 < var13.length - 1) {
               var12 = var12 + "]" + LS;
            }
         }

         var12 = var12 + "]";
      }

      return name + " {" + var12;
   }

   public Structure[] toArray(Structure[] array) {
      this.ensureAllocated();
      int i;
      if(this.memory instanceof AutoAllocated) {
         Memory size = (Memory)this.memory;
         i = array.length * this.size();
         if(size.size() < (long)i) {
            this.useMemory(this.autoAllocate(i));
         }
      }

      array[0] = this;
      int var4 = this.size();

      for(i = 1; i < array.length; ++i) {
         array[i] = newInstance(this.getClass());
         array[i].useMemory(this.memory.share((long)(i * var4), (long)var4));
         array[i].read();
      }

      if(!(this instanceof ByValue)) {
         this.array = array;
      }

      return array;
   }

   public Structure[] toArray(int size) {
      return this.toArray((Structure[])((Structure[])Array.newInstance(this.getClass(), size)));
   }

   private Class baseClass() {
      return (this instanceof ByReference || this instanceof ByValue) && Structure.class.isAssignableFrom(this.getClass().getSuperclass())?this.getClass().getSuperclass():this.getClass();
   }

   public boolean equals(Object o) {
      if(o == this) {
         return true;
      } else if(!(o instanceof Structure)) {
         return false;
      } else if(o.getClass() != this.getClass() && ((Structure)o).baseClass() != this.baseClass()) {
         return false;
      } else {
         Structure s = (Structure)o;
         if(s.size() == this.size()) {
            this.clear();
            this.write();
            byte[] buf = this.getPointer().getByteArray(0L, this.size());
            s.clear();
            s.write();
            byte[] sbuf = s.getPointer().getByteArray(0L, s.size());
            return Arrays.equals(buf, sbuf);
         } else {
            return false;
         }
      }
   }

   public int hashCode() {
      this.clear();
      this.write();
      return Arrays.hashCode(this.getPointer().getByteArray(0L, this.size()));
   }

   protected void cacheTypeInfo(Pointer p) {
      this.typeInfo = p.peer;
   }

   Pointer getTypeInfo() {
      Pointer p = getTypeInfo(this);
      this.cacheTypeInfo(p);
      return p;
   }

   public void setAutoSynch(boolean auto) {
      this.setAutoRead(auto);
      this.setAutoWrite(auto);
   }

   public void setAutoRead(boolean auto) {
      this.autoRead = auto;
   }

   public boolean getAutoRead() {
      return this.autoRead;
   }

   public void setAutoWrite(boolean auto) {
      this.autoWrite = auto;
   }

   public boolean getAutoWrite() {
      return this.autoWrite;
   }

   static Pointer getTypeInfo(Object obj) {
      return FFIType.get(obj);
   }

   public static Structure newInstance(Class type) throws IllegalArgumentException {
      String msg;
      try {
         Structure e = (Structure)type.newInstance();
         if(e instanceof ByValue) {
            e.allocateMemory();
         }

         return e;
      } catch (InstantiationException var3) {
         msg = "Can\'t instantiate " + type + " (" + var3 + ")";
         throw new IllegalArgumentException(msg);
      } catch (IllegalAccessException var4) {
         msg = "Instantiation of " + type + " not allowed, is it public? (" + var4 + ")";
         throw new IllegalArgumentException(msg);
      }
   }

   private static void structureArrayCheck(Structure[] ss) {
      Pointer base = ss[0].getPointer();
      int size = ss[0].size();

      for(int si = 1; si < ss.length; ++si) {
         if(ss[si].getPointer().peer != base.peer + (long)(size * si)) {
            String msg = "Structure array elements must use contiguous memory (bad backing address at Structure array index " + si + ")";
            throw new IllegalArgumentException(msg);
         }
      }

   }

   public static void autoRead(Structure[] ss) {
      structureArrayCheck(ss);
      if(ss[0].array == ss) {
         ss[0].autoRead();
      } else {
         for(int si = 0; si < ss.length; ++si) {
            ss[si].autoRead();
         }
      }

   }

   public void autoRead() {
      if(this.getAutoRead()) {
         this.read();
         if(this.array != null) {
            for(int i = 1; i < this.array.length; ++i) {
               this.array[i].autoRead();
            }
         }
      }

   }

   public static void autoWrite(Structure[] ss) {
      structureArrayCheck(ss);
      if(ss[0].array == ss) {
         ss[0].autoWrite();
      } else {
         for(int si = 0; si < ss.length; ++si) {
            ss[si].autoWrite();
         }
      }

   }

   public void autoWrite() {
      if(this.getAutoWrite()) {
         this.write();
         if(this.array != null) {
            for(int i = 1; i < this.array.length; ++i) {
               this.array[i].autoWrite();
            }
         }
      }

   }

   static {
      Field[] fields = MemberOrder.class.getFields();
      ArrayList names = new ArrayList();

      for(int expected = 0; expected < fields.length; ++expected) {
         names.add(fields[expected].getName());
      }

      List var5 = Arrays.asList(MemberOrder.FIELDS);
      ArrayList reversed = new ArrayList(var5);
      Collections.reverse(reversed);
      REVERSE_FIELDS = names.equals(reversed);
      REQUIRES_FIELD_ORDER = !names.equals(var5) && !REVERSE_FIELDS;
      String arch = System.getProperty("os.arch").toLowerCase();
      isPPC = "ppc".equals(arch) || "powerpc".equals(arch);
      isSPARC = "sparc".equals(arch);
      MAX_GNUC_ALIGNMENT = isSPARC?8:Native.LONG_SIZE;
      reads = new ThreadLocal() {
         protected synchronized Object initialValue() {
            return new HashMap();
         }
      };
      busy = new ThreadLocal() {
         protected synchronized Object initialValue() {
            return new StructureSet();
         }

         class StructureSet extends AbstractCollection implements Set {
            private Structure[] elements;
            private int count;

            private void ensureCapacity(int size) {
               if(this.elements == null) {
                  this.elements = new Structure[size * 3 / 2];
               } else if(this.elements.length < size) {
                  Structure[] e = new Structure[size * 3 / 2];
                  System.arraycopy(this.elements, 0, e, 0, this.elements.length);
                  this.elements = e;
               }

            }

            public int size() {
               return this.count;
            }

            public boolean contains(Object o) {
               return this.indexOf(o) != -1;
            }

            public boolean add(Object o) {
               if(!this.contains(o)) {
                  this.ensureCapacity(this.count + 1);
                  this.elements[this.count++] = (Structure)o;
               }

               return true;
            }

            private int indexOf(Object o) {
               Structure s1 = (Structure)o;

               for(int i = 0; i < this.count; ++i) {
                  Structure s2 = this.elements[i];
                  if(s1 == s2 || s1.getClass() == s2.getClass() && s1.size() == s2.size() && s1.getPointer().equals(s2.getPointer())) {
                     return i;
                  }
               }

               return -1;
            }

            public boolean remove(Object o) {
               int idx = this.indexOf(o);
               if(idx != -1) {
                  if(--this.count > 0) {
                     this.elements[idx] = this.elements[this.count];
                     this.elements[this.count] = null;
                  }

                  return true;
               } else {
                  return false;
               }
            }

            public Iterator iterator() {
               Structure[] e = new Structure[this.count];
               System.arraycopy(this.elements, 0, e, 0, this.count);
               return Arrays.asList(e).iterator();
            }
         }
      };
   }

   private class AutoAllocated extends Memory {
      public AutoAllocated(int size) {
         super((long)size);
         super.clear();
      }
   }

   static class FFIType extends Structure {
      private static Map typeInfoMap = new WeakHashMap();
      private static final int FFI_TYPE_STRUCT = 13;
      public size_t size;
      public short alignment;
      public short type = 13;
      public Pointer elements;

      private FFIType(Structure ref) {
         Pointer[] els;
         if(ref instanceof Union) {
            StructField idx = ((Union)ref).biggestField;
            els = new Pointer[]{get(ref.getField(idx), idx.type), null};
         } else {
            els = new Pointer[ref.fields().size() + 1];
            int var6 = 0;

            StructField sf;
            for(Iterator i = ref.fields().values().iterator(); i.hasNext(); els[var6++] = get(ref.getField(sf), sf.type)) {
               sf = (StructField)i.next();
            }
         }

         this.init(els);
      }

      private FFIType(Object array, Class type) {
         int length = Array.getLength(array);
         Pointer[] els = new Pointer[length + 1];
         Pointer p = get((Object)null, type.getComponentType());

         for(int i = 0; i < length; ++i) {
            els[i] = p;
         }

         this.init(els);
      }

      private void init(Pointer[] els) {
         this.elements = new Memory((long)(Pointer.SIZE * els.length));
         this.elements.write(0L, (Pointer[])els, 0, els.length);
         this.write();
      }

      static Pointer get(Object obj) {
         return obj == null? FFITypes.ffi_type_pointer:(obj instanceof Class?get((Object)null, (Class)obj):get(obj, obj.getClass()));
      }

      private static Pointer get(Object obj, Class cls) {
         Map var2 = typeInfoMap;
         synchronized(typeInfoMap) {
            Object o = typeInfoMap.get(cls);
            if(o instanceof Pointer) {
               return (Pointer)o;
            } else if(o instanceof FFIType) {
               return ((FFIType)o).getPointer();
            } else if(!Buffer.class.isAssignableFrom(cls) && !Callback.class.isAssignableFrom(cls)) {
               FFIType type;
               if(Structure.class.isAssignableFrom(cls)) {
                  if(obj == null) {
                     obj = newInstance(cls);
                  }

                  if(ByReference.class.isAssignableFrom(cls)) {
                     typeInfoMap.put(cls, FFITypes.ffi_type_pointer);
                     return FFITypes.ffi_type_pointer;
                  } else {
                     type = new FFIType((Structure)obj);
                     typeInfoMap.put(cls, type);
                     return type.getPointer();
                  }
               } else if(NativeMapped.class.isAssignableFrom(cls)) {
                  NativeMappedConverter type1 = NativeMappedConverter.getInstance(cls);
                  return get(type1.toNative(obj, new ToNativeContext()), type1.nativeType());
               } else if(cls.isArray()) {
                  type = new FFIType(obj, cls);
                  typeInfoMap.put(obj, type);
                  return type.getPointer();
               } else {
                  throw new IllegalArgumentException("Unsupported type " + cls);
               }
            } else {
               typeInfoMap.put(cls, FFITypes.ffi_type_pointer);
               return FFITypes.ffi_type_pointer;
            }
         }
      }

      static {
         if(Native.POINTER_SIZE == 0) {
            throw new Error("Native library not initialized");
         } else if(FFITypes.ffi_type_void == null) {
            throw new Error("FFI types not initialized");
         } else {
            typeInfoMap.put(Void.TYPE, FFITypes.ffi_type_void);
            typeInfoMap.put(Void.class, FFITypes.ffi_type_void);
            typeInfoMap.put(Float.TYPE, FFITypes.ffi_type_float);
            typeInfoMap.put(Float.class, FFITypes.ffi_type_float);
            typeInfoMap.put(Double.TYPE, FFITypes.ffi_type_double);
            typeInfoMap.put(Double.class, FFITypes.ffi_type_double);
            typeInfoMap.put(Long.TYPE, FFITypes.ffi_type_sint64);
            typeInfoMap.put(Long.class, FFITypes.ffi_type_sint64);
            typeInfoMap.put(Integer.TYPE, FFITypes.ffi_type_sint32);
            typeInfoMap.put(Integer.class, FFITypes.ffi_type_sint32);
            typeInfoMap.put(Short.TYPE, FFITypes.ffi_type_sint16);
            typeInfoMap.put(Short.class, FFITypes.ffi_type_sint16);
            Pointer ctype = Native.WCHAR_SIZE == 2? FFITypes.ffi_type_uint16: FFITypes.ffi_type_uint32;
            typeInfoMap.put(Character.TYPE, ctype);
            typeInfoMap.put(Character.class, ctype);
            typeInfoMap.put(Byte.TYPE, FFITypes.ffi_type_sint8);
            typeInfoMap.put(Byte.class, FFITypes.ffi_type_sint8);
            typeInfoMap.put(Boolean.TYPE, FFITypes.ffi_type_uint32);
            typeInfoMap.put(Boolean.class, FFITypes.ffi_type_uint32);
            typeInfoMap.put(Pointer.class, FFITypes.ffi_type_pointer);
            typeInfoMap.put(String.class, FFITypes.ffi_type_pointer);
            typeInfoMap.put(WString.class, FFITypes.ffi_type_pointer);
         }
      }

      private static class FFITypes {
         private static Pointer ffi_type_void;
         private static Pointer ffi_type_float;
         private static Pointer ffi_type_double;
         private static Pointer ffi_type_longdouble;
         private static Pointer ffi_type_uint8;
         private static Pointer ffi_type_sint8;
         private static Pointer ffi_type_uint16;
         private static Pointer ffi_type_sint16;
         private static Pointer ffi_type_uint32;
         private static Pointer ffi_type_sint32;
         private static Pointer ffi_type_uint64;
         private static Pointer ffi_type_sint64;
         private static Pointer ffi_type_pointer;
      }

      public static class size_t extends IntegerType {
         public size_t() {
            this(0L);
         }

         public size_t(long value) {
            super(Native.POINTER_SIZE, value);
         }
      }
   }

   class StructField {
      public String name;
      public Class type;
      public Field field;
      public int size = -1;
      public int offset = -1;
      public boolean isVolatile;
      public boolean isReadOnly;
      public FromNativeConverter readConverter;
      public ToNativeConverter writeConverter;
      public FromNativeContext context;
   }

   private static class MemberOrder {
      private static final String[] FIELDS = new String[]{"first", "second", "middle", "penultimate", "last"};
      public int first;
      public int second;
      public int middle;
      public int penultimate;
      public int last;
   }

   public interface ByReference {
   }

   public interface ByValue {
   }
}
