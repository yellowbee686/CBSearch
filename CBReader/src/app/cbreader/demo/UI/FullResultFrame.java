package app.cbreader.demo.UI;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import app.cbreader.demo.model.SearchResult;

public class FullResultFrame extends JFrame implements ActionListener, ListSelectionListener {
    private JList<String> showList;

    // TODO 传入数据
    public FullResultFrame(String title, SearchResult result) {
        super(title);
        this.setSize(600, 400);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        showList = new JList<>(result.getListData());
        showList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        showList.setSelectedIndex(0);
        showList.addListSelectionListener(this);
        showList.setVisibleRowCount(5);
        JScrollPane listScrollPane = new JScrollPane(showList);
        add(listScrollPane, BorderLayout.CENTER);

        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void valueChanged(ListSelectionEvent e) {

    }
}
