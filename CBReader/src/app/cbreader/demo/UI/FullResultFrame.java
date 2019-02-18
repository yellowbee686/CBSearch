package app.cbreader.demo.UI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

/**
 * @author huangjunyi <huangjunyi@kuaishou.com>
 * Created on 2019-02-18
 */
public class FullResultFrame extends JFrame implements ActionListener {
    private JList showList;

    // TODO 传入数据
    public FullResultFrame(String title) {
        super(title);

        showList = new JList();
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }
}
