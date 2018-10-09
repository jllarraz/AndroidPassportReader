package example.jllarraz.com.passportreader.utils;



import org.jmrtd.lds.icao.MRZInfo;

import java.util.ArrayList;
import java.util.Iterator;

public class MRZUtil {

    public static final String TAG= MRZUtil.class.getSimpleName();

    private static String PASSPORT_LINE_1 ="[P]{1}[A-Z<]{1}[A-Z<]{3}[A-Z0-9<]{39}$";
    private static String PASSPORT_LINE_2 ="[A-Z0-9<]{9}[0-9]{1}[A-Z<]{3}[0-9]{6}[0-9]{1}[FM<]{1}[0-9]{6}[0-9]{1}[A-Z0-9<]{14}[0-9<]{1}[0-9]{1}$";

    public static ArrayList<String> mLines1=new ArrayList<>();
    public static ArrayList<String> mLines2=new ArrayList<>();


    public static String cleanString(String mrz) throws IllegalArgumentException{
        String[] lines = mrz.split("\n");
        if(lines.length>2){
            return cleanLine1(lines[0])+"\n"+cleanLine2(lines[1]);
        }
        throw new IllegalArgumentException("Not enough lines");
    }

    public static String cleanLine1(String line) throws IllegalArgumentException{
        if(line==null||line.length()!=44){
            throw new IllegalArgumentException("Line 1 doesnt have the right length");
        }
        String group1= line.substring(0, 2);
        String group2= line.substring(2, 5);
        String group3= line.substring(5, line.length());

        group2 = replaceNumberWithAlfa(group2);


        return group1+group2+group3;
    }

    public static String cleanLine2(String line) throws IllegalArgumentException{
        if(line==null||line.length()!=44){
            throw new IllegalArgumentException("Line 2 doesnt have the right length");
        }

        String group1= line.substring(0, 9);
        String group2= line.substring(9, 10);
        String group3= line.substring(10, 13);
        String group4= line.substring(13, 19);
        String group5= line.substring(19, 20);
        String group6= line.substring(20, 21);
        String group7= line.substring(21, 27);
        String group8= line.substring(27, 28);
        String group9= line.substring(28, 42);
        String group10= line.substring(42, 43);
        String group11= line.substring(43, 44);

        group2 = replaceAlfaWithNumber(group2);
        group3 = replaceNumberWithAlfa(group3);
        group4 = replaceAlfaWithNumber(group4);
        group5 = replaceAlfaWithNumber(group5);
        group7 = replaceAlfaWithNumber(group7);
        group8 = replaceAlfaWithNumber(group8);
        group10 = replaceAlfaWithNumber(group10);
        group11 = replaceAlfaWithNumber(group11);

        return group1+group2+group3+group4+group5+group6+group7+group8+group9+group10+group11;
    }

    public static String replaceNumberWithAlfa(String str){
        str = str.replaceAll("0", "O");
        str = str.replaceAll("1", "I");
        str = str.replaceAll("2", "Z");
        str = str.replaceAll("5", "S");
        return str;
    }

    public static String replaceAlfaWithNumber(String str){
        str = str.replaceAll("O", "0");
        str = str.replaceAll("I", "1");
        str = str.replaceAll("Z", "2");
        str = str.replaceAll("S", "5");
        return str;
    }

    public static void addLine1(String line1){
        if(!mLines1.contains(line1)){
            mLines1.add(line1);
        }
    }

    public static void addLine2(String line2){
        if(!mLines2.contains(line2)){
            mLines2.add(line2);
        }
    }

    public static void cleanStorage(){
        mLines1.clear();
        mLines2.clear();
    }

    public static MRZInfo getMRZInfo() throws IllegalArgumentException{
        Iterator<String> iteratorLine1 = mLines1.iterator();
        while (iteratorLine1.hasNext()){
            String line1 = iteratorLine1.next();
            Iterator<String> iteratorLine2 = mLines2.iterator();
            while (iteratorLine2.hasNext()){
                String line2 = iteratorLine2.next();
                try {
                    MRZInfo mrzInfo = new MRZInfo(line1 + "\n" + line2);
                    return mrzInfo;
                }catch (Exception e){
                }
            }
        }
        throw new IllegalArgumentException("Unable to find a combination of lines that pass MRZ checksum");
    }
}
