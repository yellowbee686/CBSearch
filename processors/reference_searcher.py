import tkinter as tk
from tkinter import ttk
import re
from reference_generator import ReferenceGenerator
from collections import deque

class ReferenceSearcher:
    def __init__(self, note_path: str, reference_path: str):
        self.reference_generator = ReferenceGenerator(note_path, reference_path, False)
        self.reference_generator.walk_note_files()
        
        # 存储最近的搜索词，最多保存20个
        self.search_history = deque(maxlen=200)
        
        # 创建主窗口
        self.root = tk.Tk()
        self.root.title("异文检索系统")
        self.root.geometry("1800x800")  # 加宽窗口

        # 设置默认字体
        default_font = ('Microsoft YaHei', 12)  # 使用微软雅黑，大小为12
        self.root.option_add('*Font', default_font)
        
        # 创建搜索框和按钮
        self.search_frame = ttk.Frame(self.root)
        self.search_frame.pack(fill=tk.X, padx=10, pady=5)
        
        # 添加返回上一个搜索词的按钮
        self.back_button = ttk.Button(self.search_frame, text="返回", command=self.go_back)
        self.back_button.pack(side=tk.LEFT, padx=(0, 5))
        self.back_button.state(['disabled'])  # 初始状态禁用
        
        self.search_entry = ttk.Entry(self.search_frame, font=default_font)
        self.search_entry.pack(side=tk.LEFT, fill=tk.X, expand=True)
        
        self.search_button = ttk.Button(self.search_frame, text="搜索", command=self.search)
        self.search_button.pack(side=tk.LEFT, padx=(5, 0))
        
        # 创建结果列表
        self.result_frame = ttk.Frame(self.root)
        self.result_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=5)
        
        # 创建自定义样式支持换行和大字体
        style = ttk.Style()
        style.configure(
            "Wrapped.Treeview",
            rowheight=40,  # 增加行高以适应更大的字体
            font=default_font  # 设置Treeview的字体
        )
        style.configure(
            "Wrapped.Treeview.Heading",
            font=default_font  # 设置表头的字体
        )
        
        # 修改Treeview配置
        self.result_tree = ttk.Treeview(
            self.result_frame, 
            columns=("reference"),
            show="headings",  # 只显示列标题，不显示序号列
            style="Wrapped.Treeview"  # 自定义样式用于文本换行
        )
        self.result_tree.heading("reference", text="异文内容")
        
        # 设置列宽度
        self.result_tree.column("reference", width=1700)  # 设置较大的列宽
        
        self.result_tree.pack(fill=tk.BOTH, expand=True)
        
        # 添加滚动条
        scrollbar = ttk.Scrollbar(self.result_frame, orient=tk.VERTICAL, command=self.result_tree.yview)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        self.result_tree.configure(yscrollcommand=scrollbar.set)

        # 绑定双击事件
        self.result_tree.bind("<Double-1>", self.on_double_click)
        # 绑定回车键到搜索
        self.search_entry.bind("<Return>", lambda event: self.search())

    def add_to_history(self, search_text: str):
        """添加搜索词到历史记录"""
        # 如果当前词已经在历史记录中且是最新的，则不重复添加
        if not self.search_history or self.search_history[-1] != search_text:
            self.search_history.append(search_text)
            # 启用返回按钮
            self.back_button.state(['!disabled'])

    def go_back(self):
        """返回上一个搜索词"""
        if len(self.search_history) > 1:
            # 移除当前搜索词
            self.search_history.pop()
            # 获取上一个搜索词
            previous_search = self.search_history[-1]
            # 设置搜索框并执行搜索
            self.search_entry.delete(0, tk.END)
            self.search_entry.insert(0, previous_search)
            self.perform_search(previous_search)
            
            # 如果历史记录只剩一项，禁用返回按钮
            if len(self.search_history) <= 1:
                self.back_button.state(['disabled'])

    def parse_key_from_line(self, line: str) -> str:
        """从行文本中解析出关键字"""
        # 使用正则表达式匹配方括号后的第一个字符到（之前的内容
        match = re.match(r'\[\d+\]([^（]+)', line)
        if match:
            return match.group(1)
        return ""

    def on_double_click(self, event):
        """处理双击事件"""
        # 获取选中的item
        item = self.result_tree.selection()[0]
        # 获取该item的值（即显示的文本）
        line = self.result_tree.item(item)['values'][0]
        # 解析关键字
        key = self.parse_key_from_line(line)
        if key:
            # 设置搜索框的值并执行搜索
            self.search_entry.delete(0, tk.END)
            self.search_entry.insert(0, key)
            self.search()

    def perform_search(self, search_text: str) -> bool:
        """执行搜索并返回是否成功"""
        # 清空当前结果
        for item in self.result_tree.get_children():
            self.result_tree.delete(item)
            
        if not search_text:
            return False
            
        # 根据首字获取笔画数
        stroke_idx = self.reference_generator.get_stroke_count(search_text[0])
        if 0 < stroke_idx < len(self.reference_generator.reference_arr):
            stroke_dict = self.reference_generator.reference_arr[stroke_idx]
            if search_text in stroke_dict:
                # 获取并显示结果
                lines = self.reference_generator.format_reference_lines(search_text, stroke_idx)
                # ���过第一行（标题行）
                for i, line in enumerate(lines[1:], 1):
                    self.result_tree.insert("", tk.END, text=str(i), values=(line,))
                return True
        print(f"搜索词错误: {search_text}")
        return False

    def search(self):
        """搜索入口函数"""
        search_text = self.search_entry.get()
        if self.perform_search(search_text):
            self.add_to_history(search_text)

    def run(self):
        self.root.mainloop()

if __name__ == "__main__":
    searcher = ReferenceSearcher("./notes", "./references")
    searcher.run() 