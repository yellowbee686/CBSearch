package app.cbreader.demo.UI;


import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;

import javax.swing.*;

import app.cbreader.demo.model.SearchResult;

class TextArea extends JTextArea {
    private Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    public TextArea(String text) {
        super(text);
        enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    }
//    @Override
//    public String toString() {
//        return getText();
//    }
    @Override
    public void processMouseEvent(MouseEvent e) {
        String type = null;
        switch (e.getID()) {
            case MouseEvent.MOUSE_PRESSED:
                type = "MOUSE_PRESSED";
                break;
            case MouseEvent.MOUSE_RELEASED:
                type = "MOUSE_RELEASED";
                String text = getSelectedText();
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

public class FullResultTextFrame extends JFrame {
    private TextArea textArea;

    public FullResultTextFrame(String title, SearchResult result) {
        super(title);
        this.setSize(800, 600);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        StringBuilder allText = new StringBuilder();
        for (String text : result.getListData()) {
            allText.append(text).append("\r\n");
        }
        textArea = new TextArea(allText.toString());
        textArea.setAutoscrolls(true);
        textArea.setSize(750, 550);
        JScrollPane listScrollPane = new JScrollPane(textArea);
        add(listScrollPane, BorderLayout.CENTER);
        this.setVisible(true);
    }
}
