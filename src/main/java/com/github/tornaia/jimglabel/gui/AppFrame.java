package com.github.tornaia.jimglabel.gui;

import com.github.tornaia.jimglabel.common.event.ApplicationEventPublisher;
import com.github.tornaia.jimglabel.common.json.SerializerUtils;
import com.github.tornaia.jimglabel.common.setting.ApplicationSettings;
import com.github.tornaia.jimglabel.common.setting.UserSettingsProvider;
import com.github.tornaia.jimglabel.common.util.UIUtils;
import com.github.tornaia.jimglabel.gui.component.AutoCompleteComboBox;
import com.github.tornaia.jimglabel.gui.domain.Annotation;
import com.github.tornaia.jimglabel.gui.domain.DetectedObject;
import com.github.tornaia.jimglabel.gui.util.ClassUtil;
import com.github.tornaia.jimglabel.gui.util.DetectedObjectUtil;
import com.github.tornaia.jimglabel.gui.util.FileUtil;
import com.github.tornaia.jimglabel.gui.util.ObjectControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
public class AppFrame {

    private static final Logger LOG = LoggerFactory.getLogger(AppFrame.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final UserSettingsProvider userSettingsProvider;
    private final ApplicationSettings applicationSettings;
    private final SerializerUtils serializerUtils;

    private final JFrame jFrame;
    private JPanel imagePanel;
    private JLabel sourceValue;
    private JLabel targetValue;
    private JButton validateDirectoryButton;
    private JLabel fileValue;
    private JLabel resolutionValue;
    private JLabel sizeValue;
    private JButton deleteImageButton;
    private JScrollPane objectsScrollPanel;

    private int currentImageIndex;
    private String currentImageFileName;
    private int currentImageWidth;
    private int currentImageHeight;
    private List<DetectedObject> detectedObjects;
    private DetectedObject selectedObject;
    private ObjectControl selectedObjectControl;
    private Point mousePressedPoint;

    @Autowired
    public AppFrame(ApplicationEventPublisher applicationEventPublisher, UserSettingsProvider userSettingsProvider, ApplicationSettings applicationSettings, SerializerUtils serializerUtils, UIUtils uiUtils) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.userSettingsProvider = userSettingsProvider;
        this.applicationSettings = applicationSettings;
        this.serializerUtils = serializerUtils;
        this.jFrame = new JFrame(String.format("%s (%s)", applicationSettings.getDesktopClientName(), applicationSettings.getInstallerVersion()));

        initComponents();
        Dimension screenSize = uiUtils.getScreenSize();
        jFrame.setMinimumSize(new Dimension(640, 360));
        jFrame.setPreferredSize(new Dimension((int) (screenSize.width * 0.85D), (int) (screenSize.height * 0.85D)));
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

        loadImage();
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
        String directory = userSettingsProvider.read().getSourceDirectory();
        File fileChooserDirectory = directory != null && Files.isDirectory(Path.of(directory)) ? new File(directory) : FileSystemView.getFileSystemView().getHomeDirectory();
        JFileChooser directoryChooser = new JFileChooser(fileChooserDirectory);
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = directoryChooser.showOpenDialog(jFrame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = directoryChooser.getSelectedFile();
            userSettingsProvider.update(userSettings -> userSettings.setSourceDirectory(selectedDirectory.getAbsolutePath()));
        }
        currentImageIndex = -1;
        loadNextImage();
    }

    private void selectTargetDirectory() {
        String directory = userSettingsProvider.read().getTargetDirectory();
        File fileChooserDirectory = directory != null && Files.isDirectory(Path.of(directory)) ? new File(directory) : FileSystemView.getFileSystemView().getHomeDirectory();
        JFileChooser directoryChooser = new JFileChooser(fileChooserDirectory);
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = directoryChooser.showOpenDialog(jFrame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = directoryChooser.getSelectedFile();
            userSettingsProvider.update(userSettings -> userSettings.setTargetDirectory(selectedDirectory.getAbsolutePath()));
        }
        loadImage();
    }

    private JPanel createImagePanel() {
        this.imagePanel = new JPanel(new GridBagLayout());
        imagePanel.setBackground(Color.BLACK);
        return imagePanel;
    }

    private JPanel createDetailsPanel() {
        // source, file
        JPanel top = new JPanel(new GridBagLayout());
        JLabel sourceLabel = new JLabel("Source");
        this.sourceValue = new JLabel();
        JLabel targetLabel = new JLabel("Target");
        this.targetValue = new JLabel();
        JLabel fileLabel = new JLabel("File");
        this.fileValue = new JLabel();
        JLabel resolutionLabel = new JLabel("Resolution");
        this.resolutionValue = new JLabel();
        JLabel sizeLabel = new JLabel("Size");
        this.sizeValue = new JLabel();
        top.add(sourceLabel, new GridBagConstraints(0, 0, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));
        top.add(sourceValue, new GridBagConstraints(1, 0, 1, 1, 1.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 16, 0, 0), 0, 0));
        top.add(targetLabel, new GridBagConstraints(0, 1, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));
        top.add(targetValue, new GridBagConstraints(1, 1, 1, 1, 1.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 16, 0, 0), 0, 0));

        this.validateDirectoryButton = new JButton("Validate");
        validateDirectoryButton.setMnemonic('V');
        validateDirectoryButton.setToolTipText("Validate images");
        validateDirectoryButton.addActionListener(e -> validateDirectory());
        validateDirectoryButton.registerKeyboardAction(e -> validateDirectory(), KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.ALT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        validateDirectoryButton.setEnabled(false);
        top.add(validateDirectoryButton, new GridBagConstraints(0, 2, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0));
        top.add(new JSeparator(), new GridBagConstraints(0, 3, 2, 1, 1.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(16, 0, 16, 0), 0, 0));
        top.add(fileLabel, new GridBagConstraints(0, 2, 4, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0));
        top.add(fileValue, new GridBagConstraints(1, 2, 4, 1, 1.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(16, 16, 0, 0), 0, 0));
        top.add(resolutionLabel, new GridBagConstraints(0, 5, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));
        top.add(resolutionValue, new GridBagConstraints(1, 5, 1, 1, 1.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 16, 0, 0), 0, 0));
        top.add(sizeLabel, new GridBagConstraints(0, 6, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));
        top.add(sizeValue, new GridBagConstraints(1, 6, 1, 1, 1.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 16, 0, 0), 0, 0));

        this.deleteImageButton = new JButton("Delete");
        deleteImageButton.setMnemonic('D');
        deleteImageButton.setToolTipText("Delete image");
        deleteImageButton.addActionListener(e -> deleteImage());
        deleteImageButton.registerKeyboardAction(e -> deleteImage(), KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.ALT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        deleteImageButton.setEnabled(false);
        top.add(deleteImageButton, new GridBagConstraints(0, 7, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0));
        top.add(new JSeparator(), new GridBagConstraints(0, 8, 2, 1, 1.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(16, 0, 16, 0), 0, 0));

        JLabel detectedObjectsLabel = new JLabel("Detected object(s)");
        top.add(detectedObjectsLabel, new GridBagConstraints(0, 9, 2, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));

        // objects
        objectsScrollPanel = new JScrollPane(new JPanel());
        objectsScrollPanel.setBorder(BorderFactory.createEmptyBorder());

        // controls
        JPanel controlsPanel = new JPanel(new FlowLayout());
        JButton prevButton = new JButton("\u003C Prev");
        prevButton.setMnemonic('\u003C');
        prevButton.setToolTipText("Previous image");
        prevButton.addActionListener(e -> loadPreviousImage());
        prevButton.registerKeyboardAction(e -> loadPreviousImage(), KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

        JButton addButton = new JButton("Add");
        addButton.setMnemonic('A');
        addButton.setToolTipText("Add object");
        addButton.addActionListener(e -> addNewObject());
        addButton.registerKeyboardAction(e -> addNewObject(), KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.ALT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

        JButton resetButton = new JButton("Reset");
        resetButton.setMnemonic('R');
        resetButton.setToolTipText("Reset objects");
        resetButton.addActionListener(e -> resetImage());
        resetButton.registerKeyboardAction(e -> resetImage(), KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.ALT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

        JButton nextButton = new JButton("Next \u003E");
        nextButton.setMnemonic('\u003E');
        nextButton.setToolTipText("Next image");
        nextButton.addActionListener(e -> loadNextImage());
        nextButton.registerKeyboardAction(e -> loadNextImage(), KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

        controlsPanel.add(prevButton);
        controlsPanel.add(addButton);
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

    private void validateDirectory() {
        if (currentImageIndex == -1) {
            loadNextImage();
        }

        if (currentImageIndex == -1) {
            return;
        }

        int startedAt = currentImageIndex;
        while (true) {
            boolean noObject = detectedObjects.isEmpty();
            if (noObject) {
                JOptionPane.showMessageDialog(jFrame, "No object found on this image");
                return;
            }

            boolean missingName = detectedObjects
                    .stream()
                    .anyMatch(e -> Objects.isNull(e.getName()));
            if (missingName) {
                JOptionPane.showMessageDialog(jFrame, "Missing name for object");
                return;
            }

            loadNextImage();

            if (startedAt == currentImageIndex) {
                return;
            }
        }
    }

    private void deleteImage() {
        String sourceDirectory = userSettingsProvider.read().getSourceDirectory();
        try {
            Files.delete(Path.of(sourceDirectory).resolve(currentImageFileName));
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }
        loadImage();
    }

    private void loadPreviousImage() {
        currentImageIndex--;
        loadImage();
    }

    private void addNewObject() {

    }

    private void resetImage() {
        detectedObjects.clear();
        updateObjectsPanel();
        updateAnnotationFile();
        imagePanel.repaint();
    }

    private void loadNextImage() {
        currentImageIndex++;
        loadImage();
    }

    private void loadImage() {
        validateDirectoryButton.setEnabled(false);
        deleteImageButton.setEnabled(false);

        this.currentImageWidth = 0;
        this.currentImageHeight = 0;
        this.detectedObjects = null;

        String targetDirectory = userSettingsProvider.read().getTargetDirectory();
        if (targetDirectory == null) {
            targetValue.setText("<Select directory: ALT+T>");
        } else {
            Path targetDirectoryFile = Path.of(targetDirectory);
            boolean targetDirectoryExist = Files.isDirectory(targetDirectoryFile);
            String targetDirectoryAbsolutePath = targetDirectoryFile.toAbsolutePath().toString();
            if (targetDirectoryExist) {
                targetValue.setText(targetDirectoryAbsolutePath);
            } else {
                targetValue.setText(targetDirectoryAbsolutePath + " not found");
            }
        }

        String sourceDirectory = userSettingsProvider.read().getSourceDirectory();
        if (sourceDirectory == null) {
            this.currentImageIndex = -1;
            sourceValue.setText("<Select directory: ALT+S>");
            return;
        }

        Path sourceDirectoryFile = Path.of(sourceDirectory);
        boolean sourceDirectoryExist = Files.isDirectory(sourceDirectoryFile);
        String sourceDirectoryAbsolutePath = sourceDirectoryFile.toAbsolutePath().toString();
        if (!sourceDirectoryExist) {
            sourceValue.setText(sourceDirectoryAbsolutePath + " not found");
            return;
        }

        List<String> imageFileNames;
        try {
            imageFileNames = Files.list(Path.of(sourceDirectory))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(e -> e.toLowerCase(Locale.ENGLISH).endsWith(".png") || e.toLowerCase(Locale.ENGLISH).endsWith(".jpg") || e.toLowerCase(Locale.ENGLISH).endsWith(".jpeg"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }

        sourceValue.setText(sourceDirectoryAbsolutePath + " (" + imageFileNames.size() + " files)");

        if (imageFileNames.isEmpty()) {
            this.currentImageIndex = -1;
            return;
        }

        if (currentImageIndex < 0) {
            currentImageIndex = imageFileNames.size() - 1;
        }

        if (currentImageIndex >= imageFileNames.size()) {
            currentImageIndex = 0;
        }

        this.currentImageFileName = imageFileNames.get(currentImageIndex);

        jFrame.setTitle(String.format("%s (%s/%s) - %s (%s)", currentImageFileName, (currentImageIndex + 1), imageFileNames.size(), applicationSettings.getDesktopClientName(), applicationSettings.getInstallerVersion()));

        Path currentImage = Path.of(sourceDirectory).resolve(currentImageFileName);
        byte[] currentImageBytes;
        try {
            currentImageBytes = Files.readAllBytes(currentImage);
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }

        BufferedImage bufferedImage;
        try {
            bufferedImage = ImageIO.read(new ByteArrayInputStream(currentImageBytes));
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }
        this.currentImageWidth = bufferedImage.getWidth();
        this.currentImageHeight = bufferedImage.getHeight();

        imagePanel.removeAll();

        Path annotationFile = getAnnotationFile();
        boolean hasAnnotationFile = Files.isRegularFile(annotationFile);
        Annotation annotation;
        if (hasAnnotationFile) {
            String annotationContent;
            try {
                annotationContent = Files.readString(annotationFile);
            } catch (IOException e) {
                throw new IllegalStateException("Must not happen", e);
            }

            annotation = serializerUtils.toObject(annotationContent, Annotation.class);
        } else {
            annotation = new Annotation(currentImageFileName, currentImageBytes.length, currentImageWidth, currentImageHeight, new ArrayList<>());
        }
        this.detectedObjects = annotation.getObjects();

        JPanel image = new JPanel() {

            private final AtomicReference<Point> currentPoint = new AtomicReference<>();
            private final AtomicReference<Point> drawFrom = new AtomicReference<>();
            private final AtomicReference<Point> drawTo = new AtomicReference<>();
            private int targetWidth;
            private int targetHeight;
            private Image scaledImage;

            {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

                JPanel enclosingJPanel = this;

                this.addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        currentPoint.set(e.getPoint());

                        if (selectedObjectControl != null) {
                            detectedObjects.remove(AppFrame.this.selectedObject);
                            if (selectedObjectControl == ObjectControl.TOP_LEFT ||
                                    selectedObjectControl == ObjectControl.TOP_RIGHT ||
                                    selectedObjectControl == ObjectControl.BOTTOM_LEFT ||
                                    selectedObjectControl == ObjectControl.BOTTOM_RIGHT) {
                                float top = scaledImage.getHeight(null) * AppFrame.this.selectedObject.getTop();
                                float right = scaledImage.getWidth(null) * AppFrame.this.selectedObject.getRight();
                                float bottom = scaledImage.getHeight(null) * AppFrame.this.selectedObject.getBottom();
                                float left = scaledImage.getWidth(null) * AppFrame.this.selectedObject.getLeft();
                                int px = mousePressedPoint.x;
                                int py = mousePressedPoint.y;
                                float fartherX = Math.abs(px - left) < Math.abs(px - right) ? right : left;
                                float fartherY = Math.abs(py - top) < Math.abs(py - bottom) ? bottom : top;
                                drawFrom.set(new Point((int) fartherX, (int) fartherY));
                                drawTo.set(e.getPoint());
                                imagePanel.repaint();
                            } else if (selectedObjectControl == ObjectControl.TOP) {
                                float top = scaledImage.getHeight(null) * AppFrame.this.selectedObject.getTop();
                                float right = scaledImage.getWidth(null) * AppFrame.this.selectedObject.getRight();
                                float bottom = scaledImage.getHeight(null) * AppFrame.this.selectedObject.getBottom();
                                float left = scaledImage.getWidth(null) * AppFrame.this.selectedObject.getLeft();
                                int px = mousePressedPoint.x;
                                int py = mousePressedPoint.y;
                                drawFrom.set(new Point((int) left, (int) bottom));
                                drawTo.set(new Point((int) right, e.getY()));
                                imagePanel.repaint();
                            } else if (selectedObjectControl == ObjectControl.LEFT) {
                                float top = scaledImage.getHeight(null) * AppFrame.this.selectedObject.getTop();
                                float right = scaledImage.getWidth(null) * AppFrame.this.selectedObject.getRight();
                                float bottom = scaledImage.getHeight(null) * AppFrame.this.selectedObject.getBottom();
                                float left = scaledImage.getWidth(null) * AppFrame.this.selectedObject.getLeft();
                                int px = mousePressedPoint.x;
                                int py = mousePressedPoint.y;
                                drawFrom.set(new Point((int) right, (int) bottom));
                                drawTo.set(new Point(e.getX(), (int) top));
                                imagePanel.repaint();
                            } else if (selectedObjectControl == ObjectControl.MOVE) {
                                float top = scaledImage.getHeight(null) * AppFrame.this.selectedObject.getTop();
                                float right = scaledImage.getWidth(null) * AppFrame.this.selectedObject.getRight();
                                float bottom = scaledImage.getHeight(null) * AppFrame.this.selectedObject.getBottom();
                                float left = scaledImage.getWidth(null) * AppFrame.this.selectedObject.getLeft();
                                int px = mousePressedPoint.x;
                                int py = mousePressedPoint.y;
                                int cx = e.getX();
                                int cy = e.getY();
                                top = top - (py - cy);
                                right = right - (px - cx);
                                bottom = bottom - (py - cy);
                                left = left - (px - cx);

                                int scaledImageWidth = scaledImage.getWidth(null);
                                int scaledImageHeight = scaledImage.getHeight(null);
                                if (left < 0) {
                                    right += -left;
                                    left = 0;
                                }
                                if (top < 0) {
                                    bottom += -top;
                                    top = 0;
                                }
                                if (right > scaledImageWidth - 1) {
                                    left -= right - scaledImageWidth - 1;
                                    right = scaledImageWidth - 1;
                                }
                                if (bottom > scaledImageHeight) {
                                    top -= bottom - scaledImageHeight;
                                    bottom = scaledImageHeight;
                                }

                                drawFrom.set(new Point((int) left, (int) top));
                                drawTo.set(new Point((int) right, (int) bottom));
                                imagePanel.repaint();
                            } else if (selectedObjectControl == ObjectControl.RIGHT) {
                                float top = scaledImage.getHeight(null) * AppFrame.this.selectedObject.getTop();
                                float right = scaledImage.getWidth(null) * AppFrame.this.selectedObject.getRight();
                                float bottom = scaledImage.getHeight(null) * AppFrame.this.selectedObject.getBottom();
                                float left = scaledImage.getWidth(null) * AppFrame.this.selectedObject.getLeft();
                                int px = mousePressedPoint.x;
                                int py = mousePressedPoint.y;
                                drawFrom.set(new Point((int) left, (int) bottom));
                                drawTo.set(new Point(e.getX(), (int) top));
                                imagePanel.repaint();
                            } else if (selectedObjectControl == ObjectControl.BOTTOM) {
                                float top = scaledImage.getHeight(null) * AppFrame.this.selectedObject.getTop();
                                float right = scaledImage.getWidth(null) * AppFrame.this.selectedObject.getRight();
                                float bottom = scaledImage.getHeight(null) * AppFrame.this.selectedObject.getBottom();
                                float left = scaledImage.getWidth(null) * AppFrame.this.selectedObject.getLeft();
                                int px = mousePressedPoint.x;
                                int py = mousePressedPoint.y;
                                drawFrom.set(new Point((int) left, e.getY()));
                                drawTo.set(new Point((int) right, (int) top));
                                imagePanel.repaint();
                            }
                        } else {
                            drawTo.set(e.getPoint());
                            imagePanel.repaint();
                        }
                    }

                    @Override
                    public void mouseMoved(MouseEvent e) {
                        Point point = e.getPoint();

                        DetectedObject selectedObject = getSelectedObject(scaledImage, point);

                        if (selectedObject != null) {
                            if (DetectedObjectUtil.isTopLeftControl(scaledImage, selectedObject, point)) {
                                enclosingJPanel.setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                            } else if (DetectedObjectUtil.isTopControl(scaledImage, selectedObject, point)) {
                                enclosingJPanel.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                            } else if (DetectedObjectUtil.isTopRightControl(scaledImage, selectedObject, point)) {
                                enclosingJPanel.setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                            } else if (DetectedObjectUtil.isLeftControl(scaledImage, selectedObject, point)) {
                                enclosingJPanel.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
                            } else if (DetectedObjectUtil.isMoveControl(scaledImage, selectedObject, point)) {
                                enclosingJPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                            } else if (DetectedObjectUtil.isRightControl(scaledImage, selectedObject, point)) {
                                enclosingJPanel.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                            } else if (DetectedObjectUtil.isBottomLeftControl(scaledImage, selectedObject, point)) {
                                enclosingJPanel.setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
                            } else if (DetectedObjectUtil.isBottomControl(scaledImage, selectedObject, point)) {
                                enclosingJPanel.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                            } else if (DetectedObjectUtil.isBottomRightControl(scaledImage, selectedObject, point)) {
                                enclosingJPanel.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                            } else {
                                enclosingJPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                            }
                        } else {
                            enclosingJPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                        }

                        // x-y-cross
                        currentPoint.set(point);
                        imagePanel.repaint();
                    }
                });
                this.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        Point point = e.getPoint();

                        DetectedObject selectedObject = getSelectedObject(scaledImage, point);
                        AppFrame.this.selectedObject = selectedObject;
                        AppFrame.this.selectedObjectControl = DetectedObjectUtil.getSelectedObjectControl(scaledImage, selectedObject, point);
                        AppFrame.this.mousePressedPoint = point;

                        if (selectedObject == null) {
                            drawFrom.set(e.getPoint());
                        }

                        imagePanel.repaint();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        int scaledImageWidth = scaledImage.getWidth(null);
                        int scaledImageHeight = scaledImage.getHeight(null);
                        Point c = new Point(Math.min(scaledImageWidth, Math.max(0, e.getX())), Math.min(scaledImageHeight, Math.max(0, e.getY())));

                        Point from = drawFrom.get();
                        Point to = drawTo.get();
                        String name = selectedObject != null ? selectedObject.getName() : null;

                        if (selectedObjectControl == null ||
                                selectedObjectControl == ObjectControl.TOP_LEFT ||
                                selectedObjectControl == ObjectControl.TOP_RIGHT ||
                                selectedObjectControl == ObjectControl.BOTTOM_LEFT ||
                                selectedObjectControl == ObjectControl.BOTTOM_RIGHT) {
                            if (from != null) {
                                int fromX = from.x;
                                int fromY = from.y;
                                int toX = c.x;
                                int toY = c.y;
                                int x = Math.min(fromX, toX);
                                int y = Math.min(fromY, toY);
                                int width = Math.abs(toX - fromX);
                                int height = Math.abs(toY - fromY);
                                float top = (float) y / scaledImageHeight;
                                float right = (float) (x + width) / scaledImageWidth;
                                float bottom = (float) (y + height) / scaledImageHeight;
                                float left = (float) x / scaledImageWidth;
                                detectedObjects.add(new DetectedObject(name, top, right, bottom, left));
                                updateObjectsPanel();

                                drawFrom.set(null);
                                drawTo.set(null);
                                imagePanel.repaint();
                            }
                        } else if (selectedObjectControl == ObjectControl.MOVE) {
                            int fromX = from.x;
                            int fromY = from.y;
                            int toX = to.x;
                            int toY = to.y;
                            float top = (float) fromY / scaledImageHeight;
                            float right = (float) toX / scaledImageWidth;
                            float bottom = (float) toY / scaledImageHeight;
                            float left = (float) fromX / scaledImageWidth;
                            detectedObjects.add(new DetectedObject(name, top, right, bottom, left));
                            updateObjectsPanel();

                            drawFrom.set(null);
                            drawTo.set(null);
                            imagePanel.repaint();
                        } else if (selectedObjectControl == ObjectControl.TOP) {
                            float originalBottom = (scaledImage.getHeight(null) * selectedObject.getBottom());
                            float top = (c.y < originalBottom) ? (float) c.y / scaledImage.getHeight(null) : originalBottom / scaledImage.getHeight(null);
                            float bottom = (c.y < originalBottom) ? originalBottom / scaledImage.getHeight(null) : (float) c.y / scaledImage.getHeight(null);
                            detectedObjects.add(new DetectedObject(name, top, selectedObject.getRight(), bottom, selectedObject.getLeft()));
                            updateObjectsPanel();

                            drawFrom.set(null);
                            drawTo.set(null);
                            imagePanel.repaint();
                        } else if (selectedObjectControl == ObjectControl.LEFT) {
                            float originalRight = (scaledImage.getWidth(null) * selectedObject.getRight());
                            float left = (c.x < originalRight) ? (float) c.x / scaledImage.getWidth(null) : originalRight / scaledImage.getWidth(null);
                            float right = (c.x < originalRight) ? originalRight / scaledImage.getWidth(null) : (float) c.x / scaledImage.getWidth(null);
                            detectedObjects.add(new DetectedObject(name, selectedObject.getTop(), right, selectedObject.getBottom(), left));
                            updateObjectsPanel();

                            drawFrom.set(null);
                            drawTo.set(null);
                            imagePanel.repaint();
                        } else if (selectedObjectControl == ObjectControl.RIGHT) {
                            float originalLeft = (scaledImage.getWidth(null) * selectedObject.getLeft());
                            float left = (c.x < originalLeft) ? (float) c.x / scaledImage.getWidth(null) : originalLeft / scaledImage.getWidth(null);
                            float right = (c.x < originalLeft) ? originalLeft / scaledImage.getWidth(null) : (float) c.x / scaledImage.getWidth(null);
                            detectedObjects.add(new DetectedObject(name, selectedObject.getTop(), right, selectedObject.getBottom(), left));
                            updateObjectsPanel();

                            drawFrom.set(null);
                            drawTo.set(null);
                            imagePanel.repaint();
                        } else if (selectedObjectControl == ObjectControl.BOTTOM) {
                            float originalTop = (scaledImage.getHeight(null) * selectedObject.getTop());
                            float top = (c.y < originalTop) ? (float) c.y / scaledImage.getHeight(null) : originalTop / scaledImage.getHeight(null);
                            float bottom = (c.y < originalTop) ? originalTop / scaledImage.getHeight(null) : (float) c.y / scaledImage.getHeight(null);
                            detectedObjects.add(new DetectedObject(name, top, selectedObject.getRight(), bottom, selectedObject.getLeft()));
                            updateObjectsPanel();

                            drawFrom.set(null);
                            drawTo.set(null);
                            imagePanel.repaint();
                        }

                        selectedObject = null;
                        selectedObjectControl = null;
                        mousePressedPoint = null;

                        updateAnnotationFile();
                    }

                    @Override
                    public void mouseExited(MouseEvent mouseEvent) {
                        currentPoint.set(null);
                        imagePanel.repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                // image
                int imagePanelWidth = (int) imagePanel.getSize().getWidth();
                int imagePanelHeight = (int) imagePanel.getSize().getHeight();
                double scale = Math.max(1, Math.max((double) currentImageWidth / imagePanelWidth, (double) currentImageHeight / imagePanelHeight));
                int targetWidth = (int) (currentImageWidth / scale);
                int targetHeight = (int) (currentImageHeight / scale);
                if (this.targetWidth != targetWidth || this.targetHeight != targetHeight) {
                    this.targetWidth = targetWidth;
                    this.targetHeight = targetHeight;
                    this.scaledImage = bufferedImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
                    setPreferredSize(new Dimension(targetWidth, targetHeight));
                    imagePanel.revalidate();
                }
                g.drawImage(scaledImage, 0, 0, null);

                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(2));

                // rectangles
                AppFrame.this.detectedObjects.forEach(e -> {
                    float top = e.getTop();
                    float right = e.getRight();
                    float bottom = e.getBottom();
                    float left = e.getLeft();
                    int x = (int) (this.targetWidth * left);
                    int y = (int) (this.targetHeight * top);
                    int width = (int) (this.targetWidth * (right - left));
                    int height = (int) (this.targetHeight * (bottom - top));
                    g.setColor(new Color(205, 92, 92));
                    g.drawRect(x, y, width, height);

                    // draw controls for corners and edges: top-left, top, top-right, left, right, bottom-left, bottom, bottom-right
                    int controlEdgeSize = DetectedObjectUtil.CONTROL_SIZE_RADIUS * 2 + 1;
                    // top-left
                    {
                        int cx = Math.max(0, x - controlEdgeSize / 2);
                        int cy = Math.max(0, y - controlEdgeSize / 2);
                        int cw = Math.min(controlEdgeSize, this.targetWidth - cx);
                        int ch = Math.min(controlEdgeSize, this.targetHeight - cy);
                        g.drawRect(cx, cy, cw, ch);
                    }
                    // top
                    {
                        int cx = Math.max(0, x + width / 2 - controlEdgeSize / 2);
                        int cy = Math.max(0, y - controlEdgeSize / 2);
                        int cw = Math.min(controlEdgeSize, this.targetWidth - cx);
                        int ch = Math.min(controlEdgeSize, this.targetHeight - cy);
                        g.drawRect(cx, cy, cw, ch);
                    }
                    // top-right
                    {
                        int cx = Math.max(0, x + width - controlEdgeSize / 2);
                        int cy = Math.max(0, y - controlEdgeSize / 2);
                        int cw = Math.min(controlEdgeSize, this.targetWidth - cx);
                        int ch = Math.min(controlEdgeSize, this.targetHeight - cy);
                        g.drawRect(cx, cy, cw, ch);
                    }
                    // left
                    {
                        int cx = Math.max(0, x - controlEdgeSize / 2);
                        int cy = Math.max(0, y + height / 2 - controlEdgeSize / 2);
                        int cw = Math.min(controlEdgeSize, this.targetWidth - cx);
                        int ch = Math.min(controlEdgeSize, this.targetHeight - cy);
                        g.drawRect(cx, cy, cw, ch);
                    }
                    // center
                    {
                        int cx = Math.max(0, x + width / 2 - controlEdgeSize / 2);
                        int cy = Math.max(0, y + height / 2 - controlEdgeSize / 2);
                        int cw = Math.min(controlEdgeSize, this.targetWidth - cx);
                        int ch = Math.min(controlEdgeSize, this.targetHeight - cy);
                        g.drawRect(cx, cy, cw, ch);
                    }
                    // right
                    {
                        int cx = Math.max(0, x + width - controlEdgeSize / 2);
                        int cy = Math.max(0, y + height / 2 - controlEdgeSize / 2);
                        int cw = Math.min(controlEdgeSize, this.targetWidth - cx);
                        int ch = Math.min(controlEdgeSize, this.targetHeight - cy);
                        g.drawRect(cx, cy, cw, ch);
                    }
                    // bottom-left
                    {
                        int cx = Math.max(0, x - controlEdgeSize / 2);
                        int cy = Math.max(0, y + height - controlEdgeSize / 2);
                        int cw = Math.min(controlEdgeSize, this.targetWidth - cx);
                        int ch = Math.min(controlEdgeSize, this.targetHeight - cy);
                        g.drawRect(cx, cy, cw, ch);
                    }
                    // bottom
                    {
                        int cx = Math.max(0, x + width / 2 - controlEdgeSize / 2);
                        int cy = Math.max(0, y + height - controlEdgeSize / 2);
                        int cw = Math.min(controlEdgeSize, this.targetWidth - cx);
                        int ch = Math.min(controlEdgeSize, this.targetHeight - cy);
                        g.drawRect(cx, cy, cw, ch);
                    }
                    // bottom-right
                    {
                        int cx = Math.max(0, x + width - controlEdgeSize / 2);
                        int cy = Math.max(0, y + height - controlEdgeSize / 2);
                        int cw = Math.min(controlEdgeSize, this.targetWidth - cx);
                        int ch = Math.min(controlEdgeSize, this.targetHeight - cy);
                        g.drawRect(cx, cy, cw, ch);
                    }
                });

                // new rectangle
                Point from = drawFrom.get();
                Point to = drawTo.get();
                if (from != null && to != null) {
                    int fromX = from.x;
                    int fromY = from.y;
                    int toX = to.x;
                    int toY = to.y;
                    int x = Math.min(fromX, toX);
                    int y = Math.min(fromY, toY);
                    int width = Math.abs(toX - fromX);
                    int height = Math.abs(toY - fromY);
                    g.setColor(new Color(0, 72, 186));
                    g.drawRect(x, y, width, height);
                }

                // x-y-cross
                Point cp = currentPoint.get();
                boolean drawCurrentPoint = cp != null;
                if (drawCurrentPoint) {
                    g.setColor(Color.RED);
                    g.drawLine(0, (int) cp.getY(), this.scaledImage.getWidth(null), (int) cp.getY());
                    g.drawLine((int) cp.getX(), 0, (int) cp.getX(), this.scaledImage.getHeight(null));
                }
            }
        };
        imagePanel.add(image);

        fileValue.setText(currentImageFileName);
        resolutionValue.setText(String.format("%s x %s", currentImageWidth, currentImageHeight));
        sizeValue.setText(FileUtil.readableFileSize(currentImageBytes.length));
        deleteImageButton.setEnabled(true);
        validateDirectoryButton.setEnabled(true);

        updateObjectsPanel();
    }

    private void updateObjectsPanel() {
        List<String> classList = ClassUtil.getClasses();

        JPanel nestedObjectsPanel = new JPanel();
        nestedObjectsPanel.setLayout(new BoxLayout(nestedObjectsPanel, BoxLayout.Y_AXIS));
        JComboBox<String> objectClassComboBox = null;
        for (int i = 0; i < detectedObjects.size(); i++) {
            DetectedObject detectedObject = detectedObjects.get(i);

            JPanel objectPanel = new JPanel(new GridBagLayout());

            // index, object class combobox
            JLabel indexLabel = new JLabel("" + (i + 1));
            objectPanel.add(indexLabel, new GridBagConstraints(0, 0, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));

            objectClassComboBox = new AutoCompleteComboBox(classList.toArray(new String[0]));
            objectClassComboBox.addActionListener(e -> {
                JComboBox<?> source = (JComboBox<?>) e.getSource();
                String selectedItem = (String) source.getSelectedItem();
                selectedItem = classList.contains(selectedItem) ? selectedItem : null;
                boolean changed = !Objects.equals(selectedItem, detectedObject.getName());
                if (changed) {
                    detectedObject.setName(selectedItem);
                    AppFrame.this.updateAnnotationFile();
                }
            });
            objectClassComboBox.setSelectedIndex(classList.indexOf(detectedObject.getName()));
            objectPanel.add(objectClassComboBox, new GridBagConstraints(1, 0, 1, 1, 0.0D, 0.0D, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 16, 0, 0), 0, 0));

            // area
            double imageArea = currentImageWidth * currentImageHeight;
            double objectArea = ((detectedObject.getRight() - detectedObject.getLeft()) * currentImageWidth) * ((detectedObject.getBottom() - detectedObject.getTop()) * currentImageHeight);
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

    private void updateAnnotationFile() {
        Path annotationFile = getAnnotationFile();
        long size = 0;
        if (Files.isRegularFile(annotationFile)) {
            try {
                size = Files.size(annotationFile);
            } catch (IOException e) {
                throw new IllegalStateException("Must not happen", e);
            }
        }

        String annotationFileName = annotationFile.getFileName().toString();
        Annotation annotation = new Annotation(annotationFileName, size, currentImageWidth, currentImageHeight, detectedObjects);
        String annotationFileContent = serializerUtils.toJSON(annotation);
        try {
            Files.writeString(annotationFile, annotationFileContent, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
        } catch (IOException e) {
            throw new IllegalStateException("Must not happen", e);
        }
        LOG.info("Annotation file updated: {}, length: {}", annotationFile, annotation.getObjects());
    }

    private Path getAnnotationFile() {
        String directory = userSettingsProvider.read().getSourceDirectory();
        String annotationFileName = this.currentImageFileName.substring(0, this.currentImageFileName.lastIndexOf('.')) + ".json";
        return Path.of(directory).resolve(annotationFileName);
    }

    private DetectedObject getSelectedObject(Image scaledImage, Point point) {
        DetectedObject selectedObject = null;
        for (DetectedObject object : detectedObjects) {
            boolean selected = DetectedObjectUtil.isSelected(scaledImage, object, point);
            if (selected) {
                selectedObject = object;
            }
        }
        return selectedObject;
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
