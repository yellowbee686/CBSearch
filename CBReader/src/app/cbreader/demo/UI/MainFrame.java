package app.cbreader.demo.UI;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;

import app.cbreader.demo.IndexFiles;
import app.cbreader.demo.search.SearchFiles;
import app.cbreader.demo.Utils;
import app.cbreader.demo.model.SearchResult;
import app.cbreader.demo.search.SearchOption;

public class MainFrame extends JFrame implements ActionListener {
    private JButton chooseBtn;
    private JButton searchBtn;
    private JFileChooser dirChooser;
    private JTextField searchField;
    private JTextField chooseField;
    private JLabel chooseLabel;
    private JLabel searchLabel;
    private JCheckBox writeFullBox;
    private JCheckBox abaBox;

    private boolean writeFull = true;
    private boolean abaSearch = false;
    private JFileChooser fullDirChooser;
    private JButton fullBtn;
    private JTextField fullField;
    private JLabel fullLabel;

    public MainFrame() throws HeadlessException {

    }

    public MainFrame(String arg0) throws HeadlessException {
        super(arg0);
        this.setSize(600, 400);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chooseLabel = new JLabel("选择文件夹：");
        chooseBtn = new JButton("选择");
        chooseBtn.addActionListener(this);
        chooseBtn.setSize(25, 10);
        chooseField = new JTextField(20);
        chooseField.setEditable(false);
        writeFullBox = new JCheckBox("混合搜索");
        writeFullBox.setSelected(writeFull);
        writeFullBox.addActionListener(this);
        abaBox = new JCheckBox("ABA搜索");
        abaBox.setSelected(abaSearch);
        abaBox.addActionListener(this);

        dirChooser = new JFileChooser();

        JPanel choosePanel = new JPanel(new FlowLayout());
        choosePanel.setSize(600, 100);
        choosePanel.add(chooseLabel);
        choosePanel.add(chooseField);
        choosePanel.add(chooseBtn);
        choosePanel.add(writeFullBox);

        searchLabel = new JLabel("输入异文：");
        searchBtn = new JButton("搜索");
        searchBtn.addActionListener(this);
        searchBtn.setSize(25, 10);
        searchField = new JTextField(20);

        JPanel searchPanel = new JPanel(new FlowLayout());
        searchPanel.setSize(600, 100);
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(abaBox);

        fullLabel = new JLabel("选择要搜索的文件夹：");
        fullBtn = new JButton("选择");
        fullBtn.addActionListener(this);
        fullBtn.setSize(25, 10);
        fullField = new JTextField(20);
        fullField.setEditable(false);

        fullDirChooser = new JFileChooser(Utils.getBaseDir() + Utils.FULLTEXT_PATH);
        fullDirChooser.setMultiSelectionEnabled(true);
        JPanel fullPanel = new JPanel(new FlowLayout());
        fullPanel.setSize(600, 100);
        fullPanel.add(fullLabel);
        fullPanel.add(fullField);
        fullPanel.add(fullBtn);

        this.add(choosePanel, BorderLayout.PAGE_START);
        this.add(fullPanel, BorderLayout.CENTER);
        this.add(searchPanel, BorderLayout.PAGE_END);
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == chooseBtn) {
            dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int ret = dirChooser.showOpenDialog(this);
            if (ret == JFileChooser.APPROVE_OPTION) {
                String dirPath = dirChooser.getSelectedFile().getAbsolutePath();
                chooseField.setText(dirPath);
                IndexFiles indexer = new IndexFiles(dirPath, true, writeFull, true,
                        true, true);
                indexer.prepareIndex();
                // writeFull的模式下需要选择第二个对话框来确定哪些文件夹需要建索引
                if (!writeFull) {
                    indexer.buildCatalog();
                    boolean idxFlag = indexer.buildIndex(indexer.getDefaultDocDirs());
                    if (idxFlag) {
                        JOptionPane.showMessageDialog(this, "建立索引成功");
                    } else {
                        JOptionPane.showMessageDialog(this, "建立索引失败");
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "所有数据解析成功，请选择需要索引的部分");
                }
            }
        } else if (e.getSource() == searchBtn) {
            String keywords = searchField.getText().trim();
            SearchFiles searcher = new SearchFiles(keywords, writeFull);
            SearchResult searchResult = searcher.doSearch(new SearchOption(abaSearch));
            FullResultFrame uiFrame = new FullResultFrame("搜索结果", searchResult);
//            if (seaFlag) {
//                String outDirPath = Utils.getBaseDir() + "/result";
//                JOptionPane.showMessageDialog(this, "查询成功，请到" + outDirPath + "查阅结果文件");
//            } else {
//                JOptionPane.showMessageDialog(this, "搜索失败");
//            }
        } else if (e.getSource() == fullBtn) { // 选择文件夹后进行索引
            fullDirChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int ret = fullDirChooser.showOpenDialog(this);
            if (ret == JFileChooser.APPROVE_OPTION) {
                List<File> dirs = Arrays.asList(fullDirChooser.getSelectedFiles());
                // 构造一个对象，很多参数此时不是很重要
                IndexFiles indexer = new IndexFiles("", true, writeFull, true,
                        false, true);
                boolean idxFlag = indexer.buildIndex(dirs);
                if (idxFlag) {
                    JOptionPane.showMessageDialog(this, "建立索引成功");
                } else {
                    JOptionPane.showMessageDialog(this, "建立索引失败");
                }
            }
        } else if (e.getSource() == writeFullBox) {
            writeFull = writeFullBox.isSelected();
        } else if (e.getSource() == abaBox) {
            abaSearch = abaBox.isSelected();
        }
    }

    public static void main(String[] args) {
        new MainFrame("电子佛典校勘搜索");
    }

}
