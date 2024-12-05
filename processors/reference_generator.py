import os
import re

class ReferenceGenerator:
    def __init__(self, note_path: str):
        self.note_path = note_path
        self.reference_dict = {}

    def walk_note_files(self): 
        for root, dirs, files in os.walk(self.note_path):
            for file in files:
                # 获取文件名
                name = os.path.splitext(file)[0]
                # 获取文件路径
                path = os.path.join(root, file)
                self.parse_file(name, path)

    def parse_file(self, file_name, file_path):
        # 去除多余的0
        file_name = re.sub(r'(?<=[Tn_])0+(?=\d)', '', file_name)
        # 读取文件的每一行
        with open(file_path, 'r', encoding='utf-8') as f:
            for line in f:
                juan_num = line[1:5]
                line = line[6:].strip()
                parts = line.split("，")
                first_key = None
                for i, part in enumerate(parts):
                    parts[i] = part.strip()
                    if i == 0 and (part.startswith("＝") or part.startswith("～")):
                        print(f"error format file:{file_path} line:{line}")
                        continue
                    backtrack = False
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
                    elif "～" in part:
                        items = part.split("～")
                        if len(items[0].strip()) > 0:
                            first_key = items[0].strip()
                        second_key = items[1]
                    elif "＋" in part:
                        # 蜜＋（二合）夾註【明】
                        # （聖）＋諦【宋】【元】【明】
                        bracket_pos = part.find("【")
                        if bracket_pos != -1:
                            part = part[:bracket_pos]
                        part = part.strip()
                        second_key = part
                        items = part.split("＋")
                        if "（" in items[0]:
                            first_key = items[1].strip()
                        else:
                            first_key = items[0].strip()
                    elif "－" in part:
                        start_bracket = part.find("〔")
                        end_bracket = part.find("〕")
                        if start_bracket != -1 and end_bracket != -1:
                            first_key = part[start_bracket + 1:end_bracket]
                        second_key = part
                        bracket_pos = second_key.find("【")
                        if bracket_pos != -1:
                            second_key = second_key[:bracket_pos]
                        second_key = second_key.strip()

                    if first_key is None or len(first_key) == 0:
                        print(f"error format file:{file_path} line:{line}")
                        break