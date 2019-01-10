package app.cbreader.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.lang.Character.UnicodeBlock;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import com.sun.istack.internal.Nullable;

public class Utils {
	public static final String CBETA_MARK = " @cbeta"; //用来标记某一条是cbeta的校注，用于在最后确定格式用
	public static final String LACK_WORD = "（缺字）";
	public static final String NOTE_PATH = "/notes";
	public static final String FULLTEXT_PATH = "/fulltext";
	//有一些符号标记的key应该和不带标记的排在一起  ぃ标记为其他校勘记中得来的校注
	private static String OPTIONAL_MARK[] = {"？", "ぃ", "ィ"}; 
	public static boolean endsWithOptionalMark(String str) {
		for (String aOPTIONAL_MARK : OPTIONAL_MARK) {
			if (str.endsWith(aOPTIONAL_MARK)) {
				return true;
			}
		}
		return false;
	}
	//string转为unicode字符串
	public static ArrayList<String> utf8ToUnicode(String inStr) {
        char[] myBuffer = inStr.toCharArray();
        
        ArrayList<String> sb = new ArrayList<>(myBuffer.length);
        for (int i = 0; i < inStr.length(); i++) {
        	UnicodeBlock ub = UnicodeBlock.of(myBuffer[i]);
            if(ub == UnicodeBlock.BASIC_LATIN){
            	//英文及数字等
            	sb.add(String.valueOf(myBuffer[i]));
            }else if(ub == UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS){
            	//全角半角字符
            	int j = (int) myBuffer[i] - 65248;
            	sb.add(String.valueOf((char)j));
            }else{
            	//汉字
            	int s = (int) myBuffer[i];
                String hexS = Integer.toHexString(s);
                //String unicode = "\\u"+hexS;
                sb.add(hexS.toUpperCase());
            }
        }
        return sb;
    }
	
	//string转为unicode字符值，用于排序和转笔画等
	//因为记录笔画的数据是以字符值作为key的，因此直接转为十进制的字符值
	//汉字的unicode编码是将字符值转为十六进制，然后两个字节拼成一个，前面再加\\u构成的
	public static ArrayList<Integer> utf8ToUnicodeValue(String inStr) {
        char[] myBuffer = inStr.toCharArray();
        
        ArrayList<Integer> sb = new ArrayList<>(myBuffer.length);
        for (int i = 0; i < inStr.length(); i++) {
        		UnicodeBlock ub = UnicodeBlock.of(myBuffer[i]);
            if(ub == UnicodeBlock.BASIC_LATIN){
            		//英文及数字等
            		sb.add((int)myBuffer[i]);
            }else if(ub == UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS){
            		//全角半角字符
            		int j = (int) myBuffer[i] - 65248;
        			sb.add(j);
            }else{
            		//汉字
            		int s = (int) myBuffer[i];
                sb.add(s);
            }
        }
        return sb;
    }

    public static String getBaseDir() {
	    return System.getProperty("user.dir");
    }

	@Nullable
	public static BufferedReader openFile(String relativeName) {
		String path = getBaseDir()+relativeName;
		File file = new File(path);
		if(file.canRead()) {
			try {
				FileInputStream fiStream = new FileInputStream(file);
				return new BufferedReader(
						new InputStreamReader(fiStream, StandardCharsets.UTF_8));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	public static String ADD_SPLITER = "＋"; //加号分割符，+是一个特殊符号
	public static String SUB_SPLITER = "－"; //减号分割符
	public static String SUB_FRONT = "〔"; //减号分割符
	public static String SUB_BACK = "〕"; //减号分割符
	// 检查字符串中是否包含作为添加字含义的 + 字符
	public static boolean checkStringContainsAdd(String str, String[] outs) {
		String[] parts = str.split(ADD_SPLITER);
		if(parts.length<2)
			return false;
		for (int i = 0; i < parts.length; i++) {
			//如果是拼字 +后面是半角( 而非全角（
			// 对应的不在括号中的才是key
			if(!parts[i].contains("（")) {
				outs[0] = parts[i];
				return true;
			}
		}
		return false;
	}
	
	// 检查字符串中是否包含作为缺字含义的 - 字符
	public static boolean checkStringContainsSub(String str, String[] outs) {
		if(!str.contains(SUB_SPLITER))
			return false;
		int frontIdx = str.indexOf(SUB_FRONT);
		int backIdx = str.indexOf(SUB_BACK);
		if(frontIdx < backIdx && frontIdx != -1) {
			outs[0] = str.substring(frontIdx+1, backIdx);
			return true;
		}
		return false;
	}
}
