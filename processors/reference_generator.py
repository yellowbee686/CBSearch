import os
import re

from collections import namedtuple

Reference = namedtuple('Reference', ['file_name', 'position', 'backtrack', 'ref_type'])

# ref_type 构造为包含4项的enum
class RefType:
    NORMAL = 0,  # 正文异文
    ADD = 1, # 增加部分
    SUB = 2, # 减少部分
    SANSKRIT = 3, # 梵文

# 由于cbeta中使用程序在此处插入了某字符，导致从原文件中解析不到
ERR_TAG = "[ERR]"

def parse_stroke_data(file_path):
    # 存储汉字与笔画数的映射
    stroke_data = {}
    
    # 打开文件并逐行读取
    with open(file_path, 'r', encoding='utf-8') as file:
        for line in file:
            # 跳过注释和空行
            if line.startswith('#') or not line.strip():
                continue
            
            # 分割行数据
            parts = line.strip().split('\t')
            if len(parts) >= 3 and parts[1] == 'kTotalStrokes':
                # 获取 Unicode 编码和笔画数
                unicode_code = parts[0]  # 如 'U+4E00'
                stroke_values = parts[2].split()
                strokes = int(stroke_values[0])  # 简体中文笔画数或唯一值
                # 转换为汉字
                char = chr(int(unicode_code[2:], 16))
                # 存储映射关系
                stroke_data[char] = strokes
                
    return stroke_data


class ReferenceGenerator:
    def __init__(self, note_path: str, reference_path: str):
        self.note_path = note_path
        self.reference_path = reference_path
        self.stroke_data = parse_stroke_data("processors/Unihan_IRGSources.txt")
        # index by stroke
        self.reference_arr = []

    def get_stroke_count(self, char):
        if char in self.stroke_data:
            return self.stroke_data[char]
        else:
            return 0

    def walk_note_files(self): 
        for root, dirs, files in os.walk(self.note_path):
            for file in files:
                # 忽略隐藏文件
                if file.startswith('.'):
                    continue
                # 获取文件名
                name = os.path.splitext(file)[0]
                # 获取文件路径
                path = os.path.join(root, file)
                self.parse_file(name, path)

    def record_one_reference(self, first_key: str, second_key: str, simple_file_name: str, pos: str, backtrack: bool, ref_type: RefType):
        stroke = self.get_stroke_count(first_key[0])
        if stroke >= len(self.reference_arr):
            for i in range(stroke + 1 - len(self.reference_arr)):
                self.reference_arr.append({})

        if first_key not in self.reference_arr[stroke]:
            self.reference_arr[stroke][first_key] = {}

        if second_key not in self.reference_arr[stroke][first_key]:
            self.reference_arr[stroke][first_key][second_key] = []

        # simple_file_name, pos, backtrack构造成一个namedtpule使其更有可读性
        self.reference_arr[stroke][first_key][second_key].append(
            Reference(file_name=simple_file_name, position=pos, backtrack=backtrack, ref_type=ref_type)
        )



    def parse_file(self, file_name: str, file_path: str):
        # 去除多余的0
        simple_file_name = re.sub(r'(?<=[Tn_])0+(?=\d)', '', file_name)
        #print(f"parse file:{simple_file_name}")
        # 读取文件的每一行
        with open(file_path, 'r', encoding='utf-8') as f:
            for line in f:
                pos = line[1:5]
                line = line[6:].strip()
                parts = line.split("，")
                first_key = None
                for i, part in enumerate(parts):
                    parts[i] = part.strip()
                    if i == 0 and part.startswith("～"):
                        # print(f"error format file:{file_path} pos:{pos} line:{line}")
                        continue
                    backtrack = False
                    ref_type = RefType.NORMAL
                    # [8009]清淨＝靜暄【宋】，＝靜喧【元】【明】，＝清涼【聖】
                    if "＝" in part:
                        backtrack = True
                        items = part.split("＝")
                        # 说明要替换first_key 否则继续使用之前的first_key
                        if len(items[0].strip()) > 0:
                            first_key = items[0].strip()
                        
                        second_key = items[1]
                        # 去除第一个【后的所有字符
                        bracket_pos = second_key.find("【")
                        if bracket_pos != -1:
                            second_key = second_key[:bracket_pos]
                        second_key = second_key.strip()
                        if i==0 and first_key is None:
                            first_key = ERR_TAG
                        if len(second_key) == 0:
                            second_key = ERR_TAG
                    elif "～" in part:
                        ref_type = RefType.SANSKRIT
                        items = part.split("～")
                        if len(items[0].strip()) > 0:
                            first_key = items[0].strip()
                        second_key = items[1]
                    elif "＋" in part:
                        # 蜜＋（二合）夾註【明】
                        # （聖）＋諦【宋】【元】【明】
                        ref_type = RefType.ADD
                        bracket_pos = part.find("【")
                        if bracket_pos != -1:
                            part = part[:bracket_pos]
                        part = part.strip()
                        if i == 0:
                            if part.startswith("＋"):
                                part = ERR_TAG + part
                            elif part.endswith("＋"):
                                part = part + ERR_TAG
                        # 現＋（身）【宋】【元】【明】，＋（世）【聖】第二段解析时补上
                        # 五分律＝彌沙塞律【聖】＊，（彌沙塞部）＋【明】＊
                        if first_key is not None:
                            if part.startswith("＋"):
                                part = first_key + part
                            else:
                                part = part + first_key
                        second_key = part
                        items = part.split("＋")
                        if len(items) == 1:
                            print(f"error format with add:{file_path} pos:{pos} line:{line}")
                            break
                        if "（" in items[0]:
                            first_key = items[1].strip()
                        else:
                            first_key = items[0].strip()
                    elif "－" in part:
                        ref_type = RefType.SUB
                        start_bracket = part.find("〔")
                        end_bracket = part.find("〕")
                        if start_bracket != -1 and end_bracket != -1:
                            first_key = part[start_bracket + 1:end_bracket]
                        second_key = part
                        bracket_pos = second_key.find("【")
                        if bracket_pos != -1:
                            second_key = second_key[:bracket_pos]
                        second_key = second_key.strip()
                    else:
                        continue

                    if first_key is None or len(first_key) == 0:
                        print(f"error format first_key:{file_path} pos:{pos} line:{line}")
                        break

                    self.record_one_reference(first_key, second_key, simple_file_name, pos, False, ref_type)
                    if backtrack:
                        if second_key is None or len(second_key) == 0:
                            print(f"error format second_key:{file_path} pos:{pos} line:{line}")
                            break
                        else:
                            self.record_one_reference(second_key, first_key, simple_file_name, pos, True, ref_type)

    def write_all_reference(self):  
        # mkdir self.reference_path
        if not os.path.exists(self.reference_path):
            os.mkdir(self.reference_path)                  
        for i in range(len(self.reference_arr)):
            # file_name为 i.txt 前面补足0直到总共3位
            file_name = f"{i:03d}.txt"
            with open(os.path.join(self.reference_path, file_name), 'w', encoding='utf-8') as f:
                # 先对self.reference_arr[i].keys()排序，按顺序读取
                first_keys = sorted(self.reference_arr[i].keys())

                for first_key in first_keys:
                    lines = []
                    lines.append(f"【{first_key}】")
                    refs = self.reference_arr[i][first_key]
                    # refs现在是一个dict{second_key:[Reference]}，先按ref_type从小到大排，同type内按len([Reference])排序
                    refs = sorted(refs.items(), 
                                key=lambda x: (min(ref.ref_type for ref in x[1]), -len(x[1])))
                    for j, one_key_refs in enumerate(refs):
                        second_key, key_pair_refs = one_key_refs
                        ref_str = f"[{j+1}]{second_key}（"
                        # key_pair_refs是[Reference] 按backtrack分成True False两组
                        true_refs = [ref for ref in key_pair_refs if ref.backtrack]
                        false_refs = [ref for ref in key_pair_refs if not ref.backtrack]
                        # 每一类再按文件名排序
                        true_refs = sorted(true_refs, key=lambda x: x.file_name)
                        false_refs = sorted(false_refs, key=lambda x: x.file_name)
                        
                        if len(false_refs) > 0:
                            ref_str += "正："
                        for k, ref in enumerate(false_refs):
                            if k > 0:
                                last_ref = false_refs[k - 1]
                                last_juan = last_ref.file_name.split("_")[0]
                                current_parts = ref.file_name.split("_")
                                juan = current_parts[0]
                                if last_juan == juan:
                                    ref_str += f"+{current_parts[1]}({ref.position})" 
                                else:
                                    ref_str += f"/{ref.file_name}({ref.position})"
                            else:
                                ref_str += f"{ref.file_name}({ref.position})"
                        
                        if len(true_refs) > 0:
                            if len(false_refs) > 0:
                                ref_str += "|"
                            ref_str += "異："
                            for k, ref in enumerate(true_refs):
                                if k > 0:
                                    last_ref = true_refs[k - 1]
                                    last_juan = last_ref.file_name.split("_")[0]
                                    current_parts = ref.file_name.split("_")
                                    juan = current_parts[0]
                                    if last_juan == juan:
                                        ref_str += f"+{current_parts[1]}({ref.position})" 
                                    else:
                                        ref_str += f"/{ref.file_name}({ref.position})"
                                else:
                                    ref_str += f"{ref.file_name}({ref.position})"
                            ref_str += "）"
                        lines.append(ref_str)
                    f.write("\r\n".join(lines))
                    f.write("\r\n\r\n")
                                

                        


# 写入一段main方法

if __name__ == "__main__":
    rg = ReferenceGenerator("./notes", "./new_references")
    # print("walk note files")
    rg.walk_note_files()
    # print("write all references")
    rg.write_all_reference()
