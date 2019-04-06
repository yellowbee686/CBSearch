package app.cbreader.demo.UI;


import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import app.cbreader.demo.model.SearchResult;
// JList始终会劫持下层的JTextArea的mouse事件，导致下层无法进行选中复制等操作，因此选用整体的JTextArea来实现
class ListTextArea extends JTextArea {
    public ListTextArea(String text) {
        super(text);
    }
    // toString会被上层的JList调用，来决定显示什么
    @Override
    public String toString() {
        return getText();
    }
}

class TextList extends JList<ListTextArea> {
    private Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    public TextList(Vector<ListTextArea> dataModel) {
        super(dataModel);
        disableEvents(AWTEvent.MOUSE_EVENT_MASK);
    }
    @Override
    public void processMouseEvent(MouseEvent e) {
        String type = null;
        switch (e.getID()) {
            case MouseEvent.MOUSE_PRESSED:
                type = "MOUSE_PRESSED";
                break;
            case MouseEvent.MOUSE_RELEASED:
                type = "MOUSE_RELEASED";
                String text = getSelectedValue().getSelectedText();
                clipboard.setContents(new StringSelection(text), null);
                break;
            case MouseEvent.MOUSE_CLICKED:
                type = "MOUSE_CLICKED";
                break;
            case MouseEvent.MOUSE_ENTERED:
                type = "MOUSE_ENTERED";
                break;
            case MouseEvent.MOUSE_EXITED:
                type = "MOUSE_EXITED";
                break;
        }
        super.processMouseEvent(e);
    }
}

class ListScrollPanel extends JScrollPane {
    public ListScrollPanel(TextList view) {
        super(view);
    }
}

public class FullResultFrame extends JFrame implements ListSelectionListener {
    private TextList showList;

    public FullResultFrame(String title, SearchResult result) {
        super(title);
        this.setSize(800, 600);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        Vector<ListTextArea> areas = new Vector<>();
        for (String text : result.getListData()) {
            areas.add(new ListTextArea(text));
        }
        showList = new TextList(areas);
        showList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        showList.setSelectedIndex(0);
        showList.addListSelectionListener(this);
        showList.setVisibleRowCount(5);
        ListScrollPanel listScrollPane = new ListScrollPanel(showList);
        add(listScrollPane, BorderLayout.CENTER);
        this.setVisible(true);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {

    }
}
