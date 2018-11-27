package app.cbreader.demo;

import org.dom4j.Document;
import org.dom4j.Attribute;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

//解析CBeta文件，输出自己分析后的文本到/notes目录下
public class EmendationParser {
	private String dirPath; //源目录
	private String outDirPath; //写入目录
	
	public EmendationParser(String path) {
		dirPath = path;
		outDirPath = Utils.getBaseDir()+"/notes";
		File outDir = new File(outDirPath);
		if(!outDir.exists()) {
			outDir.mkdirs();
		}
	}
	
	private HashMap<String, String> charDecl = new HashMap<>(); //一篇文档中的引用集合
	// 解析出需要替换的难以表示的字的字典
	private void parseDict(Element dictElement) {
		if(null == dictElement)
			return;
		List<Element> eles = dictElement.elements();
		for (Element element : eles) {
			String key = element.attributeValue("id");
			boolean hasNormal = false;
			if(key.startsWith("CB")) {
				List<Element> mappings = element.elements("mapping");
				boolean found = false;
				for (Element element2 : mappings) {
					if("normal_unicode".equals(element2.attributeValue("type"))) {
						hasNormal = true;
					}
					if("unicode".equals(element2.attributeValue("type"))) {
						found = true;
						break;
					}
				}
				//如果有unicode字符，则可以表示，不用进一步ref(正文中和异文中都直接写好了)
				if(found) {
					continue;
				}
				if (hasNormal) {
                    List<Element> children = element.elements("charProp");
                    boolean down = false;
                    //如果存在能写出的形式，则使用这个形式，否则依然取第一项
                    for (Element child : children) {
                        if("normalized form".equals(child.elementText("localName"))) {
                            String value = child.elementText("value");
                            charDecl.put(key, value);
                            down = true;
                            break;
                        }
                    }
                    if(down) {
                        continue;
                    }
                }
			}
			//大部分情况取第一项
			Element child = element.element("charProp");
			if(null != child) {
				String value = child.elementText("value");
				charDecl.put(key, value);
			}
		}
	}
	
	//taisho-notes 指大正校注   cbeta-notes 指cbeta校注 
	private static String origAttrValue = "taisho-notes";
	private static String modAttrValue = "cbeta-notes";

	private String getTitle(Element bodyElement) {
        List<Element> eles = bodyElement.elements();
        Element tmpElement = null;
        Boolean flag = false;
        for (Element ele : eles) {
            tmpElement = ele;
            if(null!=tmpElement && "juan".equals(tmpElement.getName())) {
                flag = true;
                break;
            }
            while (null != tmpElement && null == tmpElement.element("juan"))
                tmpElement = tmpElement.element("div");

            if(null!=tmpElement && null!=tmpElement.element("juan")) {
                flag = true;
                tmpElement = tmpElement.element("juan");
                break;
            }
        }
        String title = "";
        if (flag) {
            Element jhead = tmpElement.element("jhead");
            if (null != jhead.element("title")) {
                title = jhead.elementText("title")+jhead.getText();
            } else {
                title = jhead.getText();
            }
        }
        return title;
    }

    private void parseBackPart(Element textElement, List<String> strings, String fileName) {
        HashSet<String> noteKeys = new HashSet<>();
        Element backElement = textElement.element("back");
        List<Element> cbs = backElement.elements();
        for (Element element : cbs) {
            Attribute attr = element.attribute("type");
            // 先解析cbeta校注，并且记录下来，遇到相同的taisho时就不用解析了
            if(origAttrValue.equals(attr.getValue()) || modAttrValue.equals(attr.getValue())) {
                Boolean isTaisho = origAttrValue.equals(attr.getValue());
                List<Element> notes = element.element("p").elements();
                for (Element ele : notes) {
                    //判断特殊=号
                    if("note".equals(ele.getName())) {
                        String id = ele.attributeValue("n");
                        if(!isTaisho || isTaisho && !noteKeys.contains(id)) {
                            //每一条内部可能有多个"g" 含有多个引用  也可能是多个"figure"
                            String anString = parseOneNote(ele, fileName, isTaisho);
                            anString = removeXmlAfterParse(anString);
//								if (!isTaisho) {
//									anString = anString.concat(Utils.CBETA_MARK);
//								}
                            if(!anString.isEmpty()) {
                                strings.add(anString);
                            }
                        }
                        if(!isTaisho) {
                            noteKeys.add(id);
                        }
                    }
                }
            }
        }
    }

    private void parseBodyPart(Element bodyElement, List<String> strings) {
        List<Element> items = bodyElement.elements();
	    for (Element item : items) {
	        String name = item.getName();
            if ("p".equals(name)) {
                strings.add(item.getTextTrim());
            }
            // 列表项的单独成行
            if ("lg".equals(name)) {
                List<Element> itemList = item.elements();
                for (Element innerItem : itemList) {
                    if ("l".equals(innerItem.getName())) {
                        strings.add(innerItem.getTextTrim());
                    }
                }
            }
        }
    }
    // 拼接要输出的目录path，输入是解析的源文件file
    private String getOutputPath(File doc) {
        String midPath = doc.getAbsolutePath().replace(dirPath, "");
        String outPath = outDirPath + midPath;
        outPath = outPath.replace("\r", "").replace("\n", "");
        String parts[] = outPath.split(".xml");
        return parts[0];
    }

    //解析一篇doc，
    // parseBack表示解析异文并输出到文件中
    // parseBody表示解析正文到文件中
    // 为了适配两边的需求，已经改成无状态的了
	public Boolean parseOneDoc(File doc, ParseDocType parseType, String outPath) {
		Boolean ret = true;
		SAXReader reader = new SAXReader();
		ArrayList<String> strings = new ArrayList<>(); //存放异文结果
        System.out.println(String.format("start parse %s", outPath));
		charDecl.clear();
		try {
			Document document = reader.read(doc);
			String fileName = doc.getName();
			fileName = fileName.substring(0, fileName.length()-4); //去除扩展名
			Element root = document.getRootElement();
			Element dictElement = root.element("teiHeader").element("encodingDesc").element("charDecl");
			parseDict(dictElement);
			Element textElement = root.element("text");
			Element bodyElement = textElement.element("body");
			String title = getTitle(bodyElement);

			if (parseType == ParseDocType.BACK) {
			    parseBackPart(textElement, strings, fileName);
            }
            if (parseType == ParseDocType.BODY) {
                /*
                TODO 序的title比较特殊，目前没有放进来，但序的正文放在卷正文前了
                TODO SAXReader解析一个节点时无法正确判断其下的文本和其中的各种Element之间的顺序，这里没有调用parseOnePartWithG来替换显示不出的字
                TODO 需要复查建索引的过程，索引似乎没考虑这种字，如果不做替换的话是搜索不出的
                */
                strings.add(title); //把title放在最前
                parseBodyPart(bodyElement, strings);
            }

			if(!strings.isEmpty()) {
				outPath = outPath+"_"+title+".txt";
				File outFile = new File(outPath);
				if(!outFile.exists()) {
					//先创建父目录
					outFile.getParentFile().mkdirs();
					outFile.createNewFile();
				}
				//要指定utf-8编码
				PrintWriter pw = new PrintWriter(outFile, StandardCharsets.UTF_8.toString());
				StringBuffer sb = new StringBuffer();
				for (String str : strings) {
					sb.append(str).append("\r\n");
				}
				pw.write(sb.toString());
				pw.flush();
				pw.close();
			}
		} catch (DocumentException e) {
			e.printStackTrace();
			ret = false;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	
	// 在解析之后删除多余的xml元素，多余的note因为可能影响choice的key，因此在前面就进行替换
	private String removeXmlAfterParse(String str) {
		//将space替换为（缺字）
		int spaceIdx = str.indexOf(SPACE_PREFIX);
		if(spaceIdx!=-1) {
			int spacebackIdx = str.indexOf(BACK_ATTR_FIX, spaceIdx);
			str = str.substring(0, spaceIdx)+Utils.LACK_WORD+str.substring(spacebackIdx+1);
		}
		
		int frontIdx = str.indexOf(FRONT_ATTR_FIX);
		if(frontIdx == -1) {
			return str;
		}
		StringBuilder builder = new StringBuilder();
		int backIdx = -1;
		int startIdx = 0;
		while (frontIdx!=-1) {
			backIdx = str.indexOf(BACK_ATTR_FIX, frontIdx);
			//如果遇到不匹配的情况，直接返回原始字符串
			if(backIdx == -1) {
				return str;
			}
			builder.append(str, startIdx, frontIdx);
			startIdx = backIdx+1;
			frontIdx = str.indexOf(FRONT_ATTR_FIX, startIdx);
		}
		builder.append(str.substring(startIdx));
		return builder.toString();
	}
	
	private HashMap<String, AppendixItem> appendix = new HashMap<String, AppendixItem>();
	
	public String appendix2Str() {
		StringBuilder builder = new StringBuilder();
		for (AppendixItem item : appendix.values()) {
			builder.append(item.toString()).append("\r\n");
		}
		return builder.toString();
	}
	private static String SPACE_PREFIX = "<space";
	private static String FRONT_ATTR_FIX = "<";
	private static String BACK_ATTR_FIX = ">";
	private static String COMMENT_PREFIX = "<!--";
	private static String COMMENT_POSTFIX = "-->";
	private static String NOTE_PREFIX = "<note";
	private static String NOTE_POSTFIX = "</note>";
	// SAXReader解析一个节点时无法正确判断其下的文本和其中的各种Element之间的顺序，因此改为自己解析
	private String parseOneNote(Element ele, String volumn, Boolean isTaisho) {
		// 如果节点下还有各种节点
		if(!ele.isTextOnly()) {
			String noteStr = ele.asXML();
			Element figureEle = ele.element("figure");
			//图片类的先不解析，直接返回原始字符串，应该是错误而不被解析的
			if(null!=figureEle) {
				return "";
			}
			noteStr = noteStr.substring(noteStr.indexOf(BACK_ATTR_FIX)+1);
			noteStr = noteStr.substring(0, noteStr.length()-7); //删前后的note
			//删除可能存在的<!--CBETA todo type: a-->
			int commentIdx = noteStr.indexOf(COMMENT_PREFIX);
			while(commentIdx!=-1) {
				int postIdx = noteStr.indexOf(COMMENT_POSTFIX, commentIdx);
				noteStr = noteStr.substring(0, commentIdx) + noteStr.substring(postIdx+COMMENT_POSTFIX.length());
				commentIdx = noteStr.indexOf(COMMENT_PREFIX);
			}
			//删除可能存在的中间的一些note标记的引用
			int noteIdx = noteStr.indexOf(NOTE_PREFIX);
			while(noteIdx!=-1) {
				int postIdx = noteStr.indexOf(NOTE_POSTFIX, noteIdx);
				String inNote = noteStr.substring(noteIdx, postIdx);
				if(inNote.startsWith("<note place")) {
					int backIdx = noteStr.indexOf(BACK_ATTR_FIX, noteIdx);
					StringBuilder sb = new StringBuilder();
					sb.append(noteStr, 0, noteIdx).append("(").append(noteStr, backIdx+1, postIdx).append(")");
					sb.append(noteStr.substring(postIdx+NOTE_POSTFIX.length()));
					noteStr = sb.toString();
				} else {
					noteStr = noteStr.substring(0, noteIdx) + noteStr.substring(postIdx+NOTE_POSTFIX.length());
				}
				noteIdx = noteStr.indexOf(NOTE_PREFIX);
			}
			
			List<Element> choices = ele.elements("choice");
			if(choices.size() == 0) {
				return parseOnePartWithG(noteStr);
			}else {
				String choicePrefix = "<choice"; //choice的格式可能为<choice> 也可能为 <choice cb:resp="#resp5">
				String choicePostfix = "</choice>";
				String corrFix = "corr";
				String sicFix = "sic";
				if(isTaisho) {
					corrFix = "orig";
					sicFix = "reg";
				}
				int startIdx = 0;
				StringBuilder builder = new StringBuilder();
				for (int i = 0; i < choices.size(); i++) {
					String partStr = noteStr.substring(startIdx);
					int preIdx = partStr.indexOf(choicePrefix);
					if(preIdx<0) {
						System.out.println("oops");
					}
					builder.append(parseOnePartWithG(partStr.substring(0, preIdx)));
					Element corr = choices.get(i).element(corrFix);
					if(null == corr) {
						return ele.getText();	
					}
					String corrStr = corr.asXML();
					String corrText = "";
					//源文件中是<corr></corr> 但 asXML会自己修改为<corr/> 如果<choice> 有属性 也会继承其属性
					if(!corr.getText().isEmpty()) {
						corrText = parseOnePartWithG(corrStr.substring(corrStr.indexOf(BACK_ATTR_FIX)+1, corrStr.length()-7));
					}
					builder.append(corrText);
					Element wrong = choices.get(i).element(sicFix);
					// 不需要解析wrong的key，为未知结构
					if(null != wrong.element("space")) {
						
					} else {
						String wrongStr = wrong.asXML();
						String wrongText = parseOnePartWithG(wrongStr.substring(wrongStr.indexOf(BACK_ATTR_FIX)+1, wrongStr.length()-6));
						//如果corr为空，则不列入附录
						if(!corrText.isEmpty()) {
							//corrText = " ";
							if(!appendix.containsKey(corrText)) {
								AppendixItem item = new AppendixItem(corrText);
								appendix.put(corrText, item);
							}
							appendix.get(corrText).add(wrongText, volumn);
						}
					}
					startIdx = noteStr.indexOf(choicePostfix, startIdx)+choicePostfix.length(); //解析下一段
				}
				builder.append(parseOnePartWithG(noteStr.substring(startIdx))); //解析最后剩余的一段
				return builder.toString();
			}
		} else {
			return ele.getText();	
		}
	}
	
	private String parseOnePartWithG(String noteStr) {
		int beginIdx = 0;
		String prefix = "<g ref=\"#";
		String postfix = "</g>";
		String backfix = ">";
		StringBuilder builder = new StringBuilder();
		//每次循环解析一个完整的g /g
		while(beginIdx<noteStr.length()) {
			int frontGIdx = noteStr.indexOf(prefix, beginIdx);
			// 如果没找到，将剩下的都添加到builder
			if(frontGIdx == -1) {
				builder.append(noteStr.substring(beginIdx));
				break;
			}
			builder.append(noteStr, beginIdx, frontGIdx);
			int backIdx = noteStr.indexOf(backfix, frontGIdx);
			int postIdx = noteStr.indexOf(postfix, backIdx);
			if(postIdx>backIdx) {
				String gText = noteStr.substring(backIdx+1, postIdx); //g包裹的text
				String key = noteStr.substring(frontGIdx+prefix.length(), backIdx-1); //backIdx指向>的前一位，再-1 去掉\"
				if(charDecl.containsKey(key)) {
					gText = charDecl.get(key);
				}
				builder.append(gText);
			}
			beginIdx = postIdx+postfix.length(); //移到下一段
		}
		return builder.toString();
	}
	
	public Boolean parseAllDocs() {
		Boolean ret = true;
		File rootDir = new File(dirPath);
		LinkedList<File> dirList = new LinkedList<>();
		if (rootDir.exists()) {
			if(rootDir.isDirectory()) {
				dirList.add(rootDir);
			} else {
				parseOneDoc(rootDir, ParseDocType.BACK, getOutputPath(rootDir));
			}
		} else {
			ret = false;
		}
		
		while (!dirList.isEmpty()) {
			File dir = dirList.pop();
			if (dir.exists()) {
				File dirs[] = dir.listFiles();
				for (File file : dirs) {
					if (file.isDirectory()) {
						dirList.add(file);
					} else {
						// T用来标记大正藏
						if(file.getName().startsWith("T"))
							parseOneDoc(file, ParseDocType.BACK, getOutputPath(file));
					}
				}
			} else {
				ret = false;
			}
		}
		
		return ret;
	}
}
