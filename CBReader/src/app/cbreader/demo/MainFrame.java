package app.cbreader.demo;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class MainFrame extends JFrame implements ActionListener {
	private JButton chooseBtn;
	private JButton searchBtn;
	private JFileChooser dirChooser;
	private JTextField searchField;
	private JTextField chooseField;
	private JLabel chooseLabel;
	private JLabel searchLabel;

	public MainFrame() throws HeadlessException {

	}

	public MainFrame(String arg0) throws HeadlessException {
		super(arg0);
		this.setSize(400, 150);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		chooseLabel = new JLabel("选择文件夹：");
		searchLabel = new JLabel("输入异文：");
		chooseBtn = new JButton("选择");
		chooseBtn.addActionListener(this);
		chooseBtn.setSize(25, 10);
		searchBtn = new JButton("搜索");
		searchBtn.addActionListener(this);
		searchBtn.setSize(25, 10);
		dirChooser = new JFileChooser();
		searchField = new JTextField(20);
		chooseField = new JTextField(20);
		chooseField.setEditable(false);

		JPanel choosePanel = new JPanel(new FlowLayout());
		choosePanel.setSize(400, 100);
		choosePanel.add(chooseLabel);
		choosePanel.add(chooseField);
		choosePanel.add(chooseBtn);

		JPanel searchPanel = new JPanel(new FlowLayout());
		searchPanel.setSize(400, 100);
		searchPanel.add(searchLabel);
		searchPanel.add(searchField);
		searchPanel.add(searchBtn);
		this.add(choosePanel, BorderLayout.PAGE_START);
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
				IndexFiles indexer = new IndexFiles(dirPath, true, true, false);
				Boolean idxFlag = indexer.buildIndex();
				if (idxFlag) {
					JOptionPane.showMessageDialog(this, "建立索引成功");
				} else {
					JOptionPane.showMessageDialog(this, "建立索引失败");
				}
			}
		} else if (e.getSource() == searchBtn) {
			String keywords = searchField.getText().trim();
			SearchFiles searcher = new SearchFiles(keywords);
			Boolean seaFlag = searcher.doSearch();
			if (seaFlag) {
				String outDirPath = Utils.getBaseDir() + "/result";
				JOptionPane.showMessageDialog(this, "查询成功，请到" + outDirPath + "查阅结果文件");
			} else {
				JOptionPane.showMessageDialog(this, "建立索引失败");
			}
		}
	}

	public static void main(String[] args) {
		new MainFrame("电子佛典校勘搜索");
	}

}
