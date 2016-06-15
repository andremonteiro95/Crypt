package pt.ubi.andremonteiro.crypt.meoutils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by André Monteiro on 13/06/2016.
 */
public class YubicryptFile {
    public String FileName;
    public String Size;
    public String Modified;
    public String InternalName;

    public String getFileName() {
        return FileName;
    }

    public void setFileName(String fileName) {
        FileName = fileName;
    }

    public String getSize() {
        return Size;
    }

    public void setSize(String size) {
        Size = size;
    }

    public String getModified() {
        return Modified;
    }

    public void setModified(String modified) {
        Modified = modified;
    }

    public String getInternalName() {
        return InternalName;
    }

    public void setInternalName(String internalName) {
        InternalName = internalName;
    }

    public String getDate(){
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("Europe/London"));
        calendar.add(Calendar.MILLISECOND,TimeZone.getTimeZone("Europe/London").getDSTSavings());
        return calendar.getTime().toString();
    }
}
