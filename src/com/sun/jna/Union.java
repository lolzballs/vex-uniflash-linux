package com.sun.jna;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.TypeMapper;
import com.sun.jna.WString;
import java.util.Iterator;

public abstract class Union extends Structure {
   private Structure.StructField activeField;
   Structure.StructField biggestField;

   protected Union() {
   }

   protected Union(Pointer p) {
      super(p);
   }

   protected Union(Pointer p, int alignType) {
      super(p, alignType);
   }

   protected Union(TypeMapper mapper) {
      super(mapper);
   }

   protected Union(Pointer p, int alignType, TypeMapper mapper) {
      super(p, alignType, mapper);
   }

   public void setType(Class type) {
      this.ensureAllocated();
      Iterator i = this.fields().values().iterator();

      Structure.StructField f;
      do {
         if(!i.hasNext()) {
            throw new IllegalArgumentException("No field of type " + type + " in " + this);
         }

         f = (Structure.StructField)i.next();
      } while(f.type != type);

      this.activeField = f;
   }

   public void setType(String fieldName) {
      this.ensureAllocated();
      Structure.StructField f = (Structure.StructField)this.fields().get(fieldName);
      if(f != null) {
         this.activeField = f;
      } else {
         throw new IllegalArgumentException("No field named " + fieldName + " in " + this);
      }
   }

   public Object readField(String fieldName) {
      this.ensureAllocated();
      this.setType(fieldName);
      return super.readField(fieldName);
   }

   public void writeField(String fieldName) {
      this.ensureAllocated();
      this.setType(fieldName);
      super.writeField(fieldName);
   }

   public void writeField(String fieldName, Object value) {
      this.ensureAllocated();
      this.setType(fieldName);
      super.writeField(fieldName, value);
   }

   public Object getTypedValue(Class type) {
      this.ensureAllocated();
      Iterator i = this.fields().values().iterator();

      Structure.StructField f;
      do {
         if(!i.hasNext()) {
            throw new IllegalArgumentException("No field of type " + type + " in " + this);
         }

         f = (Structure.StructField)i.next();
      } while(f.type != type);

      this.activeField = f;
      this.read();
      return this.getField(this.activeField);
   }

   public Object setTypedValue(Object object) {
      this.ensureAllocated();
      Structure.StructField f = this.findField(object.getClass());
      if(f != null) {
         this.activeField = f;
         this.setField(f, object);
         return this;
      } else {
         throw new IllegalArgumentException("No field of type " + object.getClass() + " in " + this);
      }
   }

   private Structure.StructField findField(Class type) {
      Iterator i = this.fields().values().iterator();

      Structure.StructField f;
      do {
         if(!i.hasNext()) {
            return null;
         }

         f = (Structure.StructField)i.next();
      } while(!f.type.isAssignableFrom(type));

      return f;
   }

   void writeField(Structure.StructField field) {
      if(field == this.activeField) {
         super.writeField(field);
      }

   }

   Object readField(Structure.StructField field) {
      return field != this.activeField && (Structure.class.isAssignableFrom(field.type) || String.class.isAssignableFrom(field.type) || WString.class.isAssignableFrom(field.type))?null:super.readField(field);
   }

   int calculateSize(boolean force) {
      int size = super.calculateSize(force);
      if(size != -1) {
         int fsize = 0;
         Iterator i = this.fields().values().iterator();

         while(true) {
            Structure.StructField f;
            do {
               if(!i.hasNext()) {
                  size = this.calculateAlignedSize(fsize);
                  if(size > 0 && this instanceof Structure.ByValue) {
                     this.getTypeInfo();
                  }

                  return size;
               }

               f = (Structure.StructField)i.next();
               f.offset = 0;
            } while(f.size <= fsize && (f.size != fsize || !Structure.class.isAssignableFrom(f.type)));

            fsize = f.size;
            this.biggestField = f;
         }
      } else {
         return size;
      }
   }

   protected int getNativeAlignment(Class type, Object value, boolean isFirstElement) {
      return super.getNativeAlignment(type, value, true);
   }

   Pointer getTypeInfo() {
      return this.biggestField == null?null:super.getTypeInfo();
   }
}
