package app.cbreader.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.lang.Character.UnicodeBlock;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.sun.istack.internal.Nullable;

import app.cbreader.demo.model.NoteModel;

public class Utils {
	public static final String CBETA_MARK = " @cbeta"; //用来标记某一条是cbeta的校注，用于在最后确定格式用
	public static final String LACK_WORD = "（缺字）";
	public static final String NOTE_PATH = "/notes";
	public static final String FULLTEXT_PATH = "/fulltext";
	public static final String NOTE_PREFIX = "[异] ";
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

	//创建嵌套文件夹并返回absolutePath
	public static String mkdir(String path) {
		File dir = new File(path);
		if(!dir.exists()) {
			dir.mkdirs();
		}
		return path;
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

	public static String getIndexPath(boolean writeFull) {
		if (writeFull) {
			return "index/full";
		} else {
			return "index/normal";
		}
	}

	public static String[] KEY_STOPPER = new String[] {"【", "~", "ヵ", "＊"};
	public static String KEY_SPLITER = "，"; //分隔一句中的每一项
	public static String FAN_SPLITER = "～"; //梵文和中文分割的标记
	public static String FAN_POSTFIX = "."; //梵文结尾的标记
	public static String EQUAL_SPLITER = "＝"; //等号分割符

	// 解析一个经过逗号分离，初步确定的key
	private static String parseReferenceKey(String key) {
		int postIdx = key.indexOf(FAN_POSTFIX);
		//如果句点存在且不在最后一位，则省略前面的东西，适配Kammāssadhamma. 釰  T01n0026_038  这种情况
		if(postIdx != -1 && postIdx<key.length()-1) {
			key = key.substring(postIdx+1);
		}
		//如果存在梵文，则取前面的部分 例如T01n0001_005   提頭賴吒＝提帝賴吒【聖】～Dhataraṭṭha.
		int splitIdx = key.indexOf(FAN_SPLITER);
		if(splitIdx!=-1) {
			key = key.substring(0, splitIdx);
		}
		//整句都是梵文的情况
		if(splitIdx==-1 && postIdx==key.length()-1 && !key.isEmpty()) {
			//System.out.println(key);
			return "";
		}
		return key.trim();
	}

	// isStrip为true表示剥离符号，为了全文搜索使用
	public static List<NoteModel> getNotes(String sample, boolean isStrip) {
		// 假设每一行最前面都是[abcd]的行号
		int lineNumIndex = sample.indexOf("]");
		String lineNumStr = sample.substring(1, lineNumIndex);
		sample = sample.substring(lineNumIndex + 1);
		String[] arr = sample.split(KEY_SPLITER);
		//默认等号前面的正文不会出现两个key，如果出现了，是软件错误，需要人工收录
		// TODO 软件错误需要再排查，目前先不考虑
//		if(firstKey.contains(KEY_SPLITER)) {
//			wrongItems.add(new WrongReferenceItem(sample, fileName));
//			return;
//		}
		List<NoteModel> ret = new ArrayList<>();
		for (int i = 0; i < arr.length; i++) {
			String firstKey = "", secondKey = "";
			String remaining = arr[i];
			String[] outs = new String[1];
			boolean isEqual = false; //表示非+ -的情况
			boolean isAdd = false;
			if(remaining.contains(EQUAL_SPLITER)) {
				String[] parts = remaining.split(EQUAL_SPLITER);
				//如果是第一段
				if(i==0) {
					firstKey = parts[0];
					secondKey = parts[1];
				} else { //如果不是第一段，则应该只能去除最前面的=号
					secondKey = parts[1];
				}
				isEqual = true;
			} else if (checkStringContainsAdd(remaining, outs)) {
				if(i==0) {
					firstKey = outs[0];
				}
				isAdd = true;
				secondKey = remaining;
			} else if (checkStringContainsSub(remaining, outs)) {
				if(i==0) {
					firstKey = outs[0];
				}
				secondKey = remaining;
			} else { //作为一个整体的key
				secondKey = remaining;
				isEqual = true;
			}
			if(i==0) {
				firstKey = parseReferenceKey(firstKey);
				if(firstKey.isEmpty()) {
					return ret;
				}
			}
			String copy = secondKey;
			boolean hasStopper = false;
			//依次使用停止符进行分析，如果找到，则将前面的作为key
			for (int j = 0; j < KEY_STOPPER.length; j++) {
				String[] parsed = copy.split(KEY_STOPPER[j]);
				//取出最前面的分隔符分隔出的才是真正的key 如果分隔符是最后一个字符，则只能解析出一项，但去掉了分割符，比之前短
				if(parsed.length>0 && parsed[0].length()<secondKey.length()) {
					secondKey = parsed[0];
					hasStopper = true;
				}
			}
			if(!hasStopper) {
				//System.out.println(String.format("key=%s sample=%s filename=%s", secondKey, sample, fileName));
			}
			secondKey = parseReferenceKey(secondKey);
			if(secondKey.isEmpty()) {
				continue;
			}
			if (isStrip && !isEqual) {
				secondKey = stripNote(secondKey, isAdd);
			}
			NoteModel model = new NoteModel(isEqual, firstKey, secondKey, lineNumStr);
			ret.add(model);
		}
		return ret;
	}

	private static String stripNote(String note, boolean isAdd) {
		if (isAdd) {
			return note.replaceAll(ADD_SPLITER, "").replaceAll("（", "").replaceAll("）", "");
		}
		// 减号意味着直接删除该字段
		return "";
	}

	// 返回异文标记，直接用note代替 idx是为了迎合减号的情况，记录位置，方便判断
	public static String getNoteMark(String note, int idx) {
		if (note.isEmpty()) {
			return String.format("[%s]", idx);
		}
		return String.format("[%s]", note);
	}
}
