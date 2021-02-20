package com.github.tornaia.jimglabel.gui.component;

import com.github.tornaia.jimglabel.gui.domain.AutoCompleteItem;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.github.tornaia.jimglabel.gui.util.StringUtils.normalize;

public class AutoCompleteComboBox extends JComboBox<AutoCompleteItem> {

    private final AutoCompleteItem[] items;
    private final AutoCompleteComboboxModel model;
    private JTextField textField;

    public AutoCompleteComboBox(AutoCompleteItem[] items) {
        super(items);
        this.items = items;
        this.model = new AutoCompleteComboboxModel(items);
        setModel(model);
        setEditor(new BasicComboBoxEditor() {
            @Override
            public void setItem(Object anObject) {
                if (anObject == null) {
                    editor.setText(null);
                } else if (anObject instanceof String) {
                    editor.setText((String) anObject);
                } else if (anObject instanceof AutoCompleteItem) {
                    AutoCompleteItem selectedAutoCompleteItem = (AutoCompleteItem) anObject;
                    editor.setText(selectedAutoCompleteItem.getLabel());
                } else {
                    throw new IllegalStateException("Must not happen, unexpected item: " + anObject);
                }
            }
        });
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                AutoCompleteItem autoCompleteItem = (AutoCompleteItem) value;
                return super.getListCellRendererComponent(list, autoCompleteItem.getLabel(), index, isSelected, cellHasFocus);
            }
        });
        setSelectedIndex(-1);
        setEditable(true);
    }

    @Override
    public void setSelectedIndex(int index) {
        super.setSelectedIndex(index);
        if (index != -1) {
            textField.setText(getItemAt(index).getLabel());
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
                    int keyChar = keyEvent.getKeyChar();
                    int keyCode = keyEvent.getKeyCode();

                    String text = textField.getText();

                    if (keyChar == KeyEvent.VK_ENTER) {
                        AutoCompleteItem[] matchingItems = getMatchingItems(text);
                        if (matchingItems.length == 1) {
                            model.removeAllElements();
                            model.addAll(Arrays.asList(matchingItems));
                            setSelectedItem(matchingItems[0]);
                            hidePopup();
                            textField.moveCaretPosition(textField.getText().length());
                            textField.selectAll();
                        } else {
                            setSelectedIndex(-1);
                            hidePopup();
                        }
                        return;
                    } else if (keyChar == KeyEvent.VK_ESCAPE) {
                        setSelectedIndex(-1);
                        hidePopup();
                        return;
                    } else if (keyChar == KeyEvent.VK_BACK_SPACE) {
                        int selectionStart = textField.getSelectionStart();
                        text = text.substring(0, selectionStart);
                        AutoCompleteItem[] matchingItems = getMatchingItems(text);
                        model.removeAllElements();
                        model.addAll(Arrays.asList(matchingItems));
                        setSelectedIndex(-1);
                        textField.setText(text);
                        showPopup();
                        setMaximumRowCount(Math.min(8, matchingItems.length));
                        return;
                    } else if (keyChar == KeyEvent.VK_DELETE) {
                        AutoCompleteItem[] matchingItems = getMatchingItems(text);
                        model.removeAllElements();
                        model.addAll(Arrays.asList(matchingItems));
                        setSelectedIndex(-1);
                        showPopup();
                        setMaximumRowCount(Math.min(8, matchingItems.length));
                        return;
                    }

                    boolean navigating = keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_PAGE_UP || keyCode == KeyEvent.VK_PAGE_DOWN;
                    if (navigating) {
                        return;
                    }

                    if (keyCode == KeyEvent.VK_CONTROL) {
                        return;
                    }

                    if (keyCode == KeyEvent.VK_A && (keyEvent.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                        return;
                    }

                    AutoCompleteItem[] matchingItems = getMatchingItems(text);
                    Object item = getEditor().getItem();
                    model.removeAllElements();
                    model.addAll(Arrays.asList(matchingItems));
                    setSelectedItem(null);
                    getEditor().setItem(item);
                    if (matchingItems.length > 0) {
                        showPopup();
                    } else {
                        hidePopup();
                    }
                    setMaximumRowCount(Math.min(8, matchingItems.length));
                }

                private AutoCompleteItem[] getMatchingItems(String text) {
                    return Arrays.stream(items)
                            .filter(item -> normalize(item.getLabel().toLowerCase(Locale.ENGLISH)).contains(normalize(text.toLowerCase(Locale.ENGLISH))))
                            .toArray(AutoCompleteItem[]::new);
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

    private class AutoCompleteComboboxModel extends DefaultComboBoxModel<AutoCompleteItem> {

        public AutoCompleteComboboxModel(AutoCompleteItem[] matchingItems) {
            super(matchingItems);
        }

        @Override
        public void setSelectedItem(Object anObject) {
            if (anObject == null) {
                super.setSelectedItem(null);
            } else if (anObject instanceof String) {
                List<AutoCompleteItem> selectedItem = Arrays.stream(items)
                        .filter(item -> item.getLabel().equals(anObject))
                        .collect(Collectors.toList());
                if (selectedItem.isEmpty()) {
                    super.setSelectedItem(null);
                } else if (selectedItem.size() == 1) {
                    super.setSelectedItem(selectedItem.get(0));
                } else {
                    throw new IllegalStateException("Must not happen, multiple items for: " + anObject + ", count: " + selectedItem.size());
                }
            } else if (anObject instanceof AutoCompleteItem) {
                super.setSelectedItem(anObject);
            } else {
                throw new IllegalStateException("Must not happen, unexpected item: " + anObject);
            }
        }
    }
}