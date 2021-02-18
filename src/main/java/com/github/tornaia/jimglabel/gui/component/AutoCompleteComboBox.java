package com.github.tornaia.jimglabel.gui.component;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

public class AutoCompleteComboBox extends JComboBox<String> {

    private final String[] items;
    private JTextField textField;

    public AutoCompleteComboBox(String[] items) {
        super(items);
        this.items = items;
        this.setSelectedIndex(-1);
        setEditor(new BasicComboBoxEditor());
        setEditable(true);
    }

    public void setSelectedIndex(int index) {
        super.setSelectedIndex(index);
        if (index != -1) {
            textField.setText(getItemAt(index));
            textField.moveCaretPosition(textField.getText().length());
            textField.setSelectionStart(0);
            textField.setSelectionEnd(textField.getText().length());
        }
    }

    public void setEditor(ComboBoxEditor editor) {
        super.setEditor(editor);

        if (editor.getEditorComponent() instanceof JTextField) {
            textField = (JTextField) editor.getEditorComponent();

            textField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void changedUpdate(DocumentEvent e) {
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    textField.getDocument().removeDocumentListener(this);
                    textField.moveCaretPosition(textField.getText().length());
                    textField.selectAll();
                }
            });

            textField.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent keyEvent) {
                    int keyCode = keyEvent.getKeyCode();

                    String text = textField.getText();

                    if (keyCode == KeyEvent.VK_ENTER) {
                        String[] matchingItems = getMatchingItems(text);
                        if (matchingItems.length > 0) {
                            setModel(new DefaultComboBoxModel<>(matchingItems));
                            setSelectedItem(matchingItems[0]);
                            hidePopup();
                            textField.moveCaretPosition(textField.getText().length());
                            textField.selectAll();
                        } else {
                            setSelectedIndex(-1);
                            hidePopup();
                        }
                        return;
                    } else if (keyCode == KeyEvent.VK_ESCAPE) {
                        setSelectedIndex(-1);
                        hidePopup();
                        return;
                    } else if (keyCode == KeyEvent.VK_BACK_SPACE) {
                        int selectionStart = textField.getSelectionStart();
                        text = text.substring(0, selectionStart);
                        String[] matchingItems = getMatchingItems(text);
                        setModel(new DefaultComboBoxModel<>(matchingItems));
                         setSelectedIndex(-1);
                        textField.setText(text);
                        showPopup();
                        return;
                    } else if (keyCode == KeyEvent.VK_DELETE) {
                        String[] matchingItems = getMatchingItems(text);
                        setModel(new DefaultComboBoxModel<>(matchingItems));
                        setSelectedIndex(-1);
                        showPopup();
                        return;
                    }

                    if (!Character.isLetterOrDigit(keyCode) && !Character.isSpaceChar(keyCode)) {
                        return;
                    }

                    String[] matchingItems = getMatchingItems(text);
                    if (matchingItems.length > 0) {
                        setModel(new DefaultComboBoxModel<>(matchingItems));
                        setSelectedItem(text);
                        showPopup();
                    } else {
                        hidePopup();
                    }
                }

                private String[] getMatchingItems(String text) {
                    return Arrays.stream(items)
                            .filter(item -> normalize(item.toLowerCase(Locale.ENGLISH)).contains(normalize(text.toLowerCase(Locale.ENGLISH))))
                            .toArray(String[]::new);
                }

                private String normalize(String src) {
                    return Normalizer
                            .normalize(src, Normalizer.Form.NFD)
                            .replaceAll("[^\\p{ASCII}]", "");
                }
            });
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        String text = textField.getText();
        if (text.isEmpty()) {
            textField.setText("Select one");
            textField.selectAll();
        }
    }
}