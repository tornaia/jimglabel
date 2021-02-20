package com.github.tornaia.jimglabel.gui;

import com.github.tornaia.jimglabel.common.event.ApplicationEventPublisher;
import com.github.tornaia.jimglabel.common.setting.ApplicationSettings;
import com.github.tornaia.jimglabel.common.util.UIUtils;
import com.github.tornaia.jimglabel.gui.component.AutoCompleteComboBox;
import com.github.tornaia.jimglabel.gui.component.EditableImagePanel;
import com.github.tornaia.jimglabel.gui.component.AutoCompleteItem;
import com.github.tornaia.jimglabel.gui.domain.DetectedObject;
import com.github.tornaia.jimglabel.gui.domain.EditableImage;
import com.github.tornaia.jimglabel.gui.event.DetectedObjectSelectedEvent;
import com.github.tornaia.jimglabel.gui.event.DetectedObjectsUpdatedEvent;
import com.github.tornaia.jimglabel.gui.event.EditableImageEventPublisher;
import com.github.tornaia.jimglabel.gui.event.EditableImageUpdatedEvent;
import com.github.tornaia.jimglabel.gui.service.ImageEditorService;
import com.github.tornaia.jimglabel.gui.service.OptimizeService;
import com.github.tornaia.jimglabel.gui.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class AppFrame {

    private final ImageEditorService imageEditorService;
    private final OptimizeService optimizeService;

    private EditableImage editableImage;
    private DetectedObject selectedObject;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ApplicationSettings applicationSettings;
    private final EditableImageEventPublisher editableImageEventPublisher;

    private final JFrame jFrame;

    private JPanel imagePanel;
    private EditableImagePanel editableImagePanel;
    private JLabel sourceValue;
    private JLabel targetValue;
    private JButton validateSourceButton;
    private JButton generateImagesButton;
    private JLabel fileValue;
    private JLabel resolutionValue;
    private JLabel sizeValue;
    private JButton deleteImageButton;
    private JScrollPane objectsScrollPanel;

    private List<AutoCompleteComboBox> autoCompleteComboBoxes;

    @Autowired
    public AppFrame(ImageEditorService imageEditorService, OptimizeService optimizeService, ApplicationSettings applicationSettings, UIUtils uiUtils, EditableImageEventPublisher editableImageEventPublisher, ApplicationEventPublisher applicationEventPublisher) {
        this.imageEditorService = imageEditorService;
        this.optimizeService = optimizeService;
        this.applicationSettings = applicationSettings;
        this.editableImageEventPublisher = editableImageEventPublisher;
        this.applicationEventPublisher = applicationEventPublisher;
        this.jFrame = new JFrame(String.format("%s (%s)", applicationSettings.getDesktopClientName(), applicationSettings.getInstallerVersion()));

        initComponents();
        Dimension screenSize = uiUtils.getScreenSize();
        jFrame.setMinimumSize(new Dimension(640, 360));
        jFrame.setPreferredSize(new Dimension((int) (screenSize.width * 0.8D), (int) (screenSize.height * 0.8D)));
        jFrame.setSize(jFrame.getPreferredSize());
        jFrame.setLocation(screenSize.width / 2 - jFrame.getSize().width / 2, screenSize.height / 2 - jFrame.getSize().height / 2);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        bringFrameToFront();
    }

    private void initComponents() {
        jFrame.setJMenuBar(createMenuBar());

        JPanel imagePanel = createImagePanel();
        JPanel detailsPanel = createDetailsPanel();

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(imagePanel, BorderLayout.CENTER);
        contentPanel.add(detailsPanel, BorderLayout.LINE_END);

        jFrame.setContentPane(contentPanel);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar;
        JMenu menu;
        JMenuItem menuItem;

        // Create the menu bar.
        menuBar = new JMenuBar();

        // File
        menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(menu);

        // File > Open source directory
        menuItem = new JMenuItem("Open source directory", KeyEvent.VK_S);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK));
        menuItem.addActionListener(e -> selectSourceDirectory());
        menu.add(menuItem);

        // File > Open target directory
        menuItem = new JMenuItem("Open target directory", KeyEvent.VK_T);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.ALT_DOWN_MASK));
        menuItem.addActionListener(e -> selectTargetDirectory());
        menu.add(menuItem);

        menu.addSeparator();

        // File > Exit
        menuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK));
        menuItem.addActionListener(e -> applicationEventPublisher.exit());
        menu.add(menuItem);

        // About
        menu = new JMenu("About");
        menu.setMnemonic(KeyEvent.VK_A);
        menuBar.add(menu);

        return menuBar;
    }

    private void selectSourceDirectory() {
        String directory = imageEditorService.getSourceDirectory();
        File fileChooserDirectory = directory != null && Files.isDirectory(Path.of(directory)) ? new File(directory) : FileSystemView.getFileSystemView().getHomeDirectory();
        JFileChooser directoryChooser = new JFileChooser(fileChooserDirectory);
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = directoryChooser.showOpenDialog(jFrame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = directoryChooser.getSelectedFile();
            imageEditorService.updateSourceDirectory(selectedDirectory.getAbsolutePath());
        }
    }

    private void selectTargetDirectory() {
        String directory = imageEditorService.getTargetDirectory();
        File fileChooserDirectory = directory != null && Files.isDirectory(Path.of(directory)) ? new File(directory) : FileSystemView.getFileSystemView().getHomeDirectory();
        JFileChooser directoryChooser = new JFileChooser(fileChooserDirectory);
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = directoryChooser.showOpenDialog(jFrame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = directoryChooser.getSelectedFile();
            imageEditorService.updateTargetDirectory(selectedDirectory.getAbsolutePath());
        }
    }

    private JPanel createImagePanel() {
        imagePanel = new JPanel(new GridBagLayout());
        imagePanel.setBackground(Color.BLACK);
        return imagePanel;
    }

    private JPanel createDetailsPanel() {
        // meta
        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JLabel sourceLabel = new JLabel("Source");
        sourceValue = new JLabel();
        JLabel targetLabel = new JLabel("Target");
        targetValue = new JLabel();
        JLabel fileLabel = new JLabel("File");
        fileValue = new JLabel();
        JLabel resolutionLabel = new JLabel("Resolution");
        resolutionValue = new JLabel();
        JLabel sizeLabel = new JLabel("Size");
        sizeValue = new JLabel();
        top.add(sourceLabel, new GridBagConstraints(0, 0, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));
        top.add(sourceValue, new GridBagConstraints(1, 0, 1, 1, 1.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 16, 0, 0), 0, 0));
        top.add(targetLabel, new GridBagConstraints(0, 1, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));
        top.add(targetValue, new GridBagConstraints(1, 1, 1, 1, 1.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 16, 0, 0), 0, 0));

        validateSourceButton = new JButton("Validate");
        validateSourceButton.setToolTipText("Validate source images");
        validateSourceButton.addActionListener(e -> validateSource());
        validateSourceButton.setEnabled(false);
        top.add(validateSourceButton, new GridBagConstraints(0, 2, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0));
        generateImagesButton = new JButton("Generate");
        generateImagesButton.setToolTipText("Generates optimized images");
        generateImagesButton.addActionListener(e -> generateImages());
        generateImagesButton.setEnabled(false);
        top.add(generateImagesButton, new GridBagConstraints(1, 2, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(16, 16, 0, 0), 0, 0));

        top.add(new JSeparator(), new GridBagConstraints(0, 3, 2, 1, 1.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(16, 0, 16, 0), 0, 0));
        top.add(fileLabel, new GridBagConstraints(0, 4, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0));
        top.add(fileValue, new GridBagConstraints(1, 4, 1, 1, 1.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(16, 16, 0, 0), 0, 0));
        top.add(resolutionLabel, new GridBagConstraints(0, 5, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));
        top.add(resolutionValue, new GridBagConstraints(1, 5, 1, 1, 1.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 16, 0, 0), 0, 0));
        top.add(sizeLabel, new GridBagConstraints(0, 6, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));
        top.add(sizeValue, new GridBagConstraints(1, 6, 1, 1, 1.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 16, 0, 0), 0, 0));

        deleteImageButton = new JButton("Delete image");
        deleteImageButton.setMnemonic(KeyEvent.VK_DELETE);
        deleteImageButton.setToolTipText("Delete image");
        deleteImageButton.addActionListener(e -> deleteImage());
        deleteImageButton.registerKeyboardAction(e -> deleteImage(), KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.ALT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        deleteImageButton.setEnabled(false);
        top.add(deleteImageButton, new GridBagConstraints(0, 7, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0));
        top.add(new JSeparator(), new GridBagConstraints(0, 8, 2, 1, 1.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(16, 0, 16, 0), 0, 0));

        JLabel detectedObjectsLabel = new JLabel("Detected object(s)");
        top.add(detectedObjectsLabel, new GridBagConstraints(0, 9, 2, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));


        // objects
        objectsScrollPanel = new JScrollPane(new JPanel());
        objectsScrollPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        objectsScrollPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));


        // controls
        JPanel controlsPanel = new JPanel(new FlowLayout());
        JButton prevButton = new JButton("\u003C");
        prevButton.setMnemonic(KeyEvent.VK_LEFT);
        prevButton.setToolTipText("Previous image");
        prevButton.addActionListener(e -> loadPreviousImage());
        prevButton.registerKeyboardAction(e -> loadPreviousImage(), KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

        JButton addButton = new JButton("Add");
        addButton.setMnemonic(KeyEvent.VK_A);
        addButton.setToolTipText("Add object");
        addButton.addActionListener(e -> addNewObject());
        addButton.registerKeyboardAction(e -> addNewObject(), KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.ALT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

        JButton deleteObjectButton = new JButton("Delete");
        deleteObjectButton.setMnemonic(KeyEvent.VK_D);
        deleteObjectButton.setToolTipText("Delete object");
        deleteObjectButton.addActionListener(e -> deleteObject());
        deleteObjectButton.registerKeyboardAction(e -> deleteObject(), KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.ALT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

        JButton resetButton = new JButton("Reset");
        resetButton.setMnemonic(KeyEvent.VK_R);
        resetButton.setToolTipText("Reset objects");
        resetButton.addActionListener(e -> resetImage());
        resetButton.registerKeyboardAction(e -> resetImage(), KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.ALT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

        JButton nextButton = new JButton("\u003E");
        nextButton.setMnemonic(KeyEvent.VK_RIGHT);
        nextButton.setToolTipText("Next image");
        nextButton.addActionListener(e -> loadNextImage());
        nextButton.registerKeyboardAction(e -> loadNextImage(), KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

        controlsPanel.add(prevButton);
        controlsPanel.add(addButton);
        controlsPanel.add(deleteObjectButton);
        controlsPanel.add(resetButton);
        controlsPanel.add(nextButton);

        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBackground(Color.LIGHT_GRAY);
        detailsPanel.setPreferredSize(new Dimension(320, 0));
        detailsPanel.add(top, BorderLayout.PAGE_START);
        detailsPanel.add(objectsScrollPanel, BorderLayout.CENTER);
        detailsPanel.add(controlsPanel, BorderLayout.PAGE_END);

        return detailsPanel;
    }

    private void validateSource() {
        optimizeService.generateImages(false);
    }

    private void generateImages() {
        optimizeService.generateImages(true);
    }

    private void deleteImage() {
        imageEditorService.deleteCurrentImage();
    }

    private void loadPreviousImage() {
        imageEditorService.loadPreviousImage();
    }

    private void loadNextImage() {
        imageEditorService.loadNextImage();
    }

    private void addNewObject() {
    }

    private void deleteObject() {
        imageEditorService.deleteDetectedObject(selectedObject);
    }

    private void resetImage() {
        imageEditorService.deleteAllObjects();
    }

    @EventListener(EditableImageUpdatedEvent.class)
    public void onEditableImageUpdatedEvent(EditableImageUpdatedEvent event) {
        editableImage = event.getEditableImage();
        selectedObject = null;

        String currentImageFileName = editableImage.getCurrentImageFileName();
        BufferedImage bufferedImage = editableImage.getBufferedImage();
        byte[] content = editableImage.getContent();

        String sourceDirectory = imageEditorService.getSourceDirectory();
        List<String> sourceImages = imageEditorService.getSourceImageFileNames();
        sourceValue.setText(sourceDirectory != null ? sourceDirectory + " (" + sourceImages.size() + ")" : "<Select directory: ALT+S>");
        String targetDirectory = imageEditorService.getTargetDirectory();
        targetValue.setText(targetDirectory != null ? targetDirectory : "<Select directory: ALT+T>");

        jFrame.setTitle(String.format("%s (%s/%s) - %s (%s)", currentImageFileName, sourceImages.indexOf(currentImageFileName) + 1, sourceImages.size(), applicationSettings.getDesktopClientName(), applicationSettings.getInstallerVersion()));

        editableImagePanel = new EditableImagePanel(editableImageEventPublisher, editableImage);
        imagePanel.removeAll();
        imagePanel.add(editableImagePanel);

        fileValue.setText(currentImageFileName);
        resolutionValue.setText(String.format("%s x %s", bufferedImage.getWidth(), bufferedImage.getHeight()));
        sizeValue.setText(FileUtil.readableFileSize(content.length));
        deleteImageButton.setEnabled(true);
        validateSourceButton.setEnabled(true);
        generateImagesButton.setEnabled(true);

        updateObjectsPanel();
    }

    @EventListener(DetectedObjectSelectedEvent.class)
    public void onDetectedObjectSelectedEvent(DetectedObjectSelectedEvent event) {
        selectedObject = event.getDetectedObject();
        if (selectedObject != null) {
            List<DetectedObject> detectedObjects = editableImage.getDetectedObjects();
            int itemIndex = detectedObjects.indexOf(selectedObject);
            AutoCompleteComboBox autoCompleteComboBox = autoCompleteComboBoxes.get(itemIndex);
            if (!autoCompleteComboBox.getEditor().getEditorComponent().hasFocus()) {
                autoCompleteComboBox.requestFocus();
            }
        }

        editableImagePanel.onDetectedObjectSelectedEvent(event);
    }

    @EventListener(DetectedObjectsUpdatedEvent.class)
    public void onDetectedObjectsUpdatedEvent() {
        updateObjectsPanel();
    }

    private void updateObjectsPanel() {
        List<AutoCompleteItem> autoCompleteItems = imageEditorService
                .getClasses()
                .stream()
                .map(e -> new AutoCompleteItem(e.getId(), e.getCardId() + " " + e.getName()))
                .collect(Collectors.toList());

        autoCompleteComboBoxes = new ArrayList<>();
        List<DetectedObject> detectedObjects = editableImage.getDetectedObjects();

        JPanel nestedObjectsPanel = new JPanel();
        nestedObjectsPanel.setLayout(new BoxLayout(nestedObjectsPanel, BoxLayout.Y_AXIS));
        AutoCompleteComboBox objectClassComboBox = null;
        for (int i = 0; i < detectedObjects.size(); i++) {
            DetectedObject detectedObject = detectedObjects.get(i);

            JPanel objectPanel = new JPanel(new GridBagLayout());

            JLabel indexLabel = new JLabel("" + (i + 1));
            objectPanel.add(indexLabel, new GridBagConstraints(0, 0, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));

            objectClassComboBox = new AutoCompleteComboBox(autoCompleteItems.toArray(new AutoCompleteItem[0]));
            objectClassComboBox.getEditor().getEditorComponent().addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    editableImageEventPublisher.selectDetectedObject(detectedObject);
                }

                @Override
                public void focusLost(FocusEvent e) {
                    editableImageEventPublisher.selectDetectedObject(null);
                }
            });

            autoCompleteComboBoxes.add(objectClassComboBox);
            objectClassComboBox.addActionListener(e -> {
                JComboBox<?> source = (JComboBox<?>) e.getSource();
                AutoCompleteItem selectedItem = (AutoCompleteItem) source.getSelectedItem();
                selectedItem = autoCompleteItems.contains(selectedItem) ? selectedItem : null;
                boolean changed = !Objects.equals(selectedItem != null ? selectedItem.getId() : null, detectedObject.getId());
                if (changed) {
                    imageEditorService.updateDetectedObjectName(detectedObject, selectedItem != null ? selectedItem.getId() : null);
                }
            });

            int selectedIndex = autoCompleteItems.stream().map(AutoCompleteItem::getId).collect(Collectors.toList()).indexOf(detectedObject.getId());
            objectClassComboBox.setSelectedIndex(selectedIndex);
            objectPanel.add(objectClassComboBox, new GridBagConstraints(1, 0, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 16, 0, 0), 0, 0));

            // area
            BufferedImage bufferedImage = editableImage.getBufferedImage();
            double imageArea = bufferedImage.getWidth() * bufferedImage.getHeight();
            double objectArea = ((detectedObject.getRight() - detectedObject.getLeft()) * bufferedImage.getWidth()) * ((detectedObject.getBottom() - detectedObject.getTop()) * bufferedImage.getHeight());
            double areaPercentage = (objectArea / imageArea) * 100D;
            objectPanel.add(new JLabel("Area"), new GridBagConstraints(0, 2, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));
            objectPanel.add(new JLabel(String.format("%.2f%%", areaPercentage)), new GridBagConstraints(1, 2, 1, 1, 1.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0));

            // position
            objectPanel.add(new JLabel("Position"), new GridBagConstraints(0, 3, 2, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));
            objectPanel.add(new JLabel("top"), new GridBagConstraints(0, 4, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 24, 0, 0), 0, 0));
            objectPanel.add(new JLabel(String.format("%.2f%%", detectedObject.getTop() * 100)), new GridBagConstraints(1, 4, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 24, 0, 0), 0, 0));
            objectPanel.add(new JLabel("right"), new GridBagConstraints(0, 5, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 24, 0, 0), 0, 0));
            objectPanel.add(new JLabel(String.format("%.2f%%", detectedObject.getRight() * 100)), new GridBagConstraints(1, 5, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 24, 0, 0), 0, 0));
            objectPanel.add(new JLabel("bottom"), new GridBagConstraints(0, 6, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 24, 0, 0), 0, 0));
            objectPanel.add(new JLabel(String.format("%.2f%%", detectedObject.getBottom() * 100)), new GridBagConstraints(1, 6, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 24, 0, 0), 0, 0));
            objectPanel.add(new JLabel("left"), new GridBagConstraints(0, 7, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 24, 0, 0), 0, 0));
            objectPanel.add(new JLabel(String.format("%.2f%%", detectedObject.getLeft() * 100)), new GridBagConstraints(1, 7, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 24, 0, 0), 0, 0));

            nestedObjectsPanel.add(objectPanel);
        }

        JPanel objectsPanel = new JPanel(new GridBagLayout());
        objectsPanel.add(nestedObjectsPanel, new GridBagConstraints(0, 0, 1, 1, 1.0D, 1.0D, GridBagConstraints.PAGE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        objectsScrollPanel.setViewportView(objectsPanel);

        if (objectClassComboBox != null) {
            objectClassComboBox.requestFocusInWindow();
        }
    }

    private void bringFrameToFront() {
        jFrame.setState(JFrame.NORMAL);
        jFrame.setVisible(false);
        jFrame.setAlwaysOnTop(true);
        jFrame.setVisible(true);
        jFrame.toFront();
        jFrame.setAlwaysOnTop(false);
    }
}
