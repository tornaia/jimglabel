package com.github.tornaia.jimglabel.gui.component;

import com.github.tornaia.jimglabel.gui.domain.DetectedObject;
import com.github.tornaia.jimglabel.gui.domain.EditableImage;
import com.github.tornaia.jimglabel.gui.event.DetectedObjectSelectedEvent;
import com.github.tornaia.jimglabel.gui.event.EditableImageEventPublisher;
import com.github.tornaia.jimglabel.gui.util.DetectedObjectUtil;
import com.github.tornaia.jimglabel.gui.util.ObjectControl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

public class EditableImagePanel extends JPanel {

    private final EditableImage editableImage;

    private DetectedObject selectedObject;

    private Point drawFrom;
    private Point drawTo;
    private Point mousePressedPoint;
    private Point currentPoint;

    private Image scaledImage;

    private ObjectControl selectedObjectControl;

    public EditableImagePanel(EditableImageEventPublisher editableImageEventPublisher, EditableImage editableImage) {
        this.editableImage = editableImage;

        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        List<DetectedObject> detectedObjects = editableImage.getDetectedObjects();
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                currentPoint = e.getPoint();

                if (selectedObjectControl != null) {
                    detectedObjects.remove(selectedObject);
                    if (selectedObjectControl == ObjectControl.TOP_LEFT ||
                            selectedObjectControl == ObjectControl.TOP_RIGHT ||
                            selectedObjectControl == ObjectControl.BOTTOM_LEFT ||
                            selectedObjectControl == ObjectControl.BOTTOM_RIGHT) {
                        float top = scaledImage.getHeight(null) * selectedObject.getTop();
                        float right = scaledImage.getWidth(null) * selectedObject.getRight();
                        float bottom = scaledImage.getHeight(null) * selectedObject.getBottom();
                        float left = scaledImage.getWidth(null) * selectedObject.getLeft();
                        int px = mousePressedPoint.x;
                        int py = mousePressedPoint.y;
                        float fartherX = Math.abs(px - left) < Math.abs(px - right) ? right : left;
                        float fartherY = Math.abs(py - top) < Math.abs(py - bottom) ? bottom : top;
                        drawFrom = new Point((int) fartherX, (int) fartherY);
                        drawTo = currentPoint;
                        repaint();
                    } else if (selectedObjectControl == ObjectControl.TOP) {
                        float top = scaledImage.getHeight(null) * selectedObject.getTop();
                        float right = scaledImage.getWidth(null) * selectedObject.getRight();
                        float bottom = scaledImage.getHeight(null) * selectedObject.getBottom();
                        float left = scaledImage.getWidth(null) * selectedObject.getLeft();
                        int px = mousePressedPoint.x;
                        int py = mousePressedPoint.y;
                        drawFrom = new Point((int) left, (int) bottom);
                        drawTo = new Point((int) right, e.getY());
                        repaint();
                    } else if (selectedObjectControl == ObjectControl.LEFT) {
                        float top = scaledImage.getHeight(null) * selectedObject.getTop();
                        float right = scaledImage.getWidth(null) * selectedObject.getRight();
                        float bottom = scaledImage.getHeight(null) * selectedObject.getBottom();
                        float left = scaledImage.getWidth(null) * selectedObject.getLeft();
                        int px = mousePressedPoint.x;
                        int py = mousePressedPoint.y;
                        drawFrom = new Point((int) right, (int) bottom);
                        drawTo = new Point(e.getX(), (int) top);
                        repaint();
                    } else if (selectedObjectControl == ObjectControl.MOVE) {
                        float top = scaledImage.getHeight(null) * selectedObject.getTop();
                        float right = scaledImage.getWidth(null) * selectedObject.getRight();
                        float bottom = scaledImage.getHeight(null) * selectedObject.getBottom();
                        float left = scaledImage.getWidth(null) * selectedObject.getLeft();
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

                        drawFrom = new Point((int) left, (int) top);
                        drawTo = new Point((int) right, (int) bottom);
                        repaint();
                    } else if (selectedObjectControl == ObjectControl.RIGHT) {
                        float top = scaledImage.getHeight(null) * selectedObject.getTop();
                        float right = scaledImage.getWidth(null) * selectedObject.getRight();
                        float bottom = scaledImage.getHeight(null) * selectedObject.getBottom();
                        float left = scaledImage.getWidth(null) * selectedObject.getLeft();
                        int px = mousePressedPoint.x;
                        int py = mousePressedPoint.y;
                        drawFrom = new Point((int) left, (int) bottom);
                        drawTo = new Point(e.getX(), (int) top);
                        repaint();
                    } else if (selectedObjectControl == ObjectControl.BOTTOM) {
                        float top = scaledImage.getHeight(null) * selectedObject.getTop();
                        float right = scaledImage.getWidth(null) * selectedObject.getRight();
                        float bottom = scaledImage.getHeight(null) * selectedObject.getBottom();
                        float left = scaledImage.getWidth(null) * selectedObject.getLeft();
                        int px = mousePressedPoint.x;
                        int py = mousePressedPoint.y;
                        drawFrom = new Point((int) left, e.getY());
                        drawTo = new Point((int) right, (int) top);
                        repaint();
                    }
                } else {
                    drawTo = e.getPoint();
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point point = e.getPoint();

                DetectedObject objectAtPoint = getObjectAtPoint(scaledImage, point);

                if (objectAtPoint != null) {
                    boolean objectAtPointIsTheSelectedObject = objectAtPoint.equals(selectedObject);
                    if (objectAtPointIsTheSelectedObject && DetectedObjectUtil.isTopLeftControl(scaledImage, objectAtPoint, point)) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                    } else if (objectAtPointIsTheSelectedObject && DetectedObjectUtil.isTopControl(scaledImage, objectAtPoint, point)) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                    } else if (objectAtPointIsTheSelectedObject && DetectedObjectUtil.isTopRightControl(scaledImage, objectAtPoint, point)) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                    } else if (objectAtPointIsTheSelectedObject && DetectedObjectUtil.isLeftControl(scaledImage, objectAtPoint, point)) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
                    } else if (objectAtPointIsTheSelectedObject && DetectedObjectUtil.isMoveControl(scaledImage, objectAtPoint, point)) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    } else if (objectAtPointIsTheSelectedObject && DetectedObjectUtil.isRightControl(scaledImage, objectAtPoint, point)) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                    } else if (objectAtPointIsTheSelectedObject && DetectedObjectUtil.isBottomLeftControl(scaledImage, objectAtPoint, point)) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
                    } else if (objectAtPointIsTheSelectedObject && DetectedObjectUtil.isBottomControl(scaledImage, objectAtPoint, point)) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                    } else if (objectAtPointIsTheSelectedObject && DetectedObjectUtil.isBottomRightControl(scaledImage, objectAtPoint, point)) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                    } else if (objectAtPointIsTheSelectedObject) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    }
                } else {
                    Optional<DetectedObject> optionalDetectedObject = getObject(scaledImage, point);
                    if (optionalDetectedObject.isPresent()) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    } else {
                        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    }
                }

                currentPoint = point;
                repaint();
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point point = e.getPoint();
                Optional<DetectedObject> optionalDetectedObject = getObject(scaledImage, point);
                if (optionalDetectedObject.isPresent()) {
                    DetectedObject selectedObject = optionalDetectedObject.get();
                    editableImageEventPublisher.selectDetectedObject(selectedObject);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                Point point = e.getPoint();

                DetectedObject selectedObject = getObjectAtPoint(scaledImage, point);
                editableImageEventPublisher.selectDetectedObject(selectedObject);
                selectedObjectControl = DetectedObjectUtil.getSelectedObjectControl(scaledImage, selectedObject, point);
                mousePressedPoint = point;

                if (selectedObject == null) {
                    drawFrom = e.getPoint();
                }

                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                boolean clickedAndNotDragged = drawFrom != null && drawTo == null;
                if (clickedAndNotDragged) {
                    drawFrom = null;
                    return;
                }

                int scaledImageWidth = scaledImage.getWidth(null);
                int scaledImageHeight = scaledImage.getHeight(null);
                Point c = new Point(Math.min(scaledImageWidth, Math.max(0, e.getX())), Math.min(scaledImageHeight, Math.max(0, e.getY())));
                String id = selectedObject != null ? selectedObject.getId() : null;

                if (selectedObjectControl == null ||
                        selectedObjectControl == ObjectControl.TOP_LEFT ||
                        selectedObjectControl == ObjectControl.TOP_RIGHT ||
                        selectedObjectControl == ObjectControl.BOTTOM_LEFT ||
                        selectedObjectControl == ObjectControl.BOTTOM_RIGHT) {
                    if (drawFrom != null) {
                        int fromX = drawFrom.x;
                        int fromY = drawFrom.y;
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
                        detectedObjects.add(new DetectedObject(id, top, right, bottom, left));
                        editableImageEventPublisher.updateDetectedObjects();

                        drawFrom = null;
                        drawTo = null;
                        repaint();
                    }
                } else if (selectedObjectControl == ObjectControl.MOVE) {
                    if (drawFrom != null) {
                        int fromX = drawFrom.x;
                        int fromY = drawFrom.y;
                        int toX = drawTo.x;
                        int toY = drawTo.y;
                        float top = (float) fromY / scaledImageHeight;
                        float right = (float) toX / scaledImageWidth;
                        float bottom = (float) toY / scaledImageHeight;
                        float left = (float) fromX / scaledImageWidth;
                        detectedObjects.add(new DetectedObject(id, top, right, bottom, left));
                        editableImageEventPublisher.updateDetectedObjects();

                        drawFrom = null;
                        drawTo = null;
                        repaint();
                    }
                } else if (selectedObjectControl == ObjectControl.TOP) {
                    if (drawFrom != null) {
                        float originalBottom = (scaledImage.getHeight(null) * selectedObject.getBottom());
                        float top = (c.y < originalBottom) ? (float) c.y / scaledImage.getHeight(null) : originalBottom / scaledImage.getHeight(null);
                        float bottom = (c.y < originalBottom) ? originalBottom / scaledImage.getHeight(null) : (float) c.y / scaledImage.getHeight(null);
                        detectedObjects.add(new DetectedObject(id, top, selectedObject.getRight(), bottom, selectedObject.getLeft()));
                        editableImageEventPublisher.updateDetectedObjects();

                        drawFrom = null;
                        drawTo = null;
                        repaint();
                    }
                } else if (selectedObjectControl == ObjectControl.LEFT) {
                    if (drawFrom != null) {
                        float originalRight = (scaledImage.getWidth(null) * selectedObject.getRight());
                        float left = (c.x < originalRight) ? (float) c.x / scaledImage.getWidth(null) : originalRight / scaledImage.getWidth(null);
                        float right = (c.x < originalRight) ? originalRight / scaledImage.getWidth(null) : (float) c.x / scaledImage.getWidth(null);
                        detectedObjects.add(new DetectedObject(id, selectedObject.getTop(), right, selectedObject.getBottom(), left));
                        editableImageEventPublisher.updateDetectedObjects();

                        drawFrom = null;
                        drawTo = null;
                        repaint();
                    }
                } else if (selectedObjectControl == ObjectControl.RIGHT) {
                    if (drawFrom != null) {
                        float originalLeft = (scaledImage.getWidth(null) * selectedObject.getLeft());
                        float left = (c.x < originalLeft) ? (float) c.x / scaledImage.getWidth(null) : originalLeft / scaledImage.getWidth(null);
                        float right = (c.x < originalLeft) ? originalLeft / scaledImage.getWidth(null) : (float) c.x / scaledImage.getWidth(null);
                        detectedObjects.add(new DetectedObject(id, selectedObject.getTop(), right, selectedObject.getBottom(), left));
                        editableImageEventPublisher.updateDetectedObjects();

                        drawFrom = null;
                        drawTo = null;
                        repaint();
                    }
                } else if (selectedObjectControl == ObjectControl.BOTTOM) {
                    if (drawFrom != null) {
                        float originalTop = (scaledImage.getHeight(null) * selectedObject.getTop());
                        float top = (c.y < originalTop) ? (float) c.y / scaledImage.getHeight(null) : originalTop / scaledImage.getHeight(null);
                        float bottom = (c.y < originalTop) ? originalTop / scaledImage.getHeight(null) : (float) c.y / scaledImage.getHeight(null);
                        detectedObjects.add(new DetectedObject(id, top, selectedObject.getRight(), bottom, selectedObject.getLeft()));
                        editableImageEventPublisher.updateDetectedObjects();

                        drawFrom = null;
                        drawTo = null;
                        repaint();
                    }
                }

                editableImageEventPublisher.selectDetectedObject(null);
                selectedObjectControl = null;
                mousePressedPoint = null;

                editableImageEventPublisher.updateDetectedObjects();
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                currentPoint = null;
                EditableImagePanel.this.repaint();
            }
        });
        addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
            @Override
            public void ancestorResized(HierarchyEvent e) {
                repaint();
            }
        });
    }

    // FIXME this should be removed somehow
    public void onDetectedObjectSelectedEvent(DetectedObjectSelectedEvent event) {
        selectedObject = event.getDetectedObject();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // image
        int parentPanelWidth = (int) getParent().getSize().getWidth();
        int parentPanelHeight = (int) getParent().getSize().getHeight();
        BufferedImage bufferedImage = editableImage.getBufferedImage();
        double scale = Math.max(1, Math.max((double) bufferedImage.getWidth() / parentPanelWidth, (double) bufferedImage.getHeight() / parentPanelHeight));
        int targetWidth = (int) (bufferedImage.getWidth() / scale);
        int targetHeight = (int) (bufferedImage.getHeight() / scale);
        if (this.scaledImage == null || this.scaledImage.getWidth(null) != targetWidth || this.scaledImage.getHeight(null) != targetHeight) {
            this.scaledImage = bufferedImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            this.setPreferredSize(new Dimension(targetWidth, targetHeight));
            this.setMinimumSize(new Dimension(targetWidth, targetHeight));
            revalidate();
        }
        g.drawImage(scaledImage, 0, 0, null);

        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(2));

        // object rectangles
        List<DetectedObject> detectedObjects = editableImage.getDetectedObjects();
        detectedObjects.forEach(e -> {
            float top = e.getTop();
            float right = e.getRight();
            float bottom = e.getBottom();
            float left = e.getLeft();
            int x = (int) (targetWidth * left);
            int y = (int) (targetHeight * top);
            int width = (int) (targetWidth * (right - left));
            int height = (int) (targetHeight * (bottom - top));
            boolean currentObjectIsSelected = e == selectedObject;
            boolean currentObjectIsUnderMouse = currentPoint != null && e.equals(getObject(scaledImage, currentPoint).orElse(null));
            Color color = currentObjectIsSelected ? new Color(255, 8, 0) : currentObjectIsUnderMouse ? new Color(255, 64, 56) : new Color(205, 92, 92);
            g.setColor(color);
            g.drawRect(x, y, width, height);

            if (currentObjectIsSelected) {
                // draw controls for corners and edges: top-left, top, top-right, left, right, bottom-left, bottom, bottom-right
                int controlEdgeSize = DetectedObjectUtil.CONTROL_SIZE_RADIUS * 2 + 1;
                // top-left
                {
                    int cx = Math.max(0, x - controlEdgeSize / 2);
                    int cy = Math.max(0, y - controlEdgeSize / 2);
                    int cw = Math.min(controlEdgeSize, targetWidth - cx);
                    int ch = Math.min(controlEdgeSize, targetHeight - cy);
                    g.drawRect(cx, cy, cw, ch);
                }
                // top
                {
                    int cx = Math.max(0, x + width / 2 - controlEdgeSize / 2);
                    int cy = Math.max(0, y - controlEdgeSize / 2);
                    int cw = Math.min(controlEdgeSize, targetWidth - cx);
                    int ch = Math.min(controlEdgeSize, targetHeight - cy);
                    g.drawRect(cx, cy, cw, ch);
                }
                // top-right
                {
                    int cx = Math.max(0, x + width - controlEdgeSize / 2);
                    int cy = Math.max(0, y - controlEdgeSize / 2);
                    int cw = Math.min(controlEdgeSize, targetWidth - cx);
                    int ch = Math.min(controlEdgeSize, targetHeight - cy);
                    g.drawRect(cx, cy, cw, ch);
                }
                // left
                {
                    int cx = Math.max(0, x - controlEdgeSize / 2);
                    int cy = Math.max(0, y + height / 2 - controlEdgeSize / 2);
                    int cw = Math.min(controlEdgeSize, targetWidth - cx);
                    int ch = Math.min(controlEdgeSize, targetHeight - cy);
                    g.drawRect(cx, cy, cw, ch);
                }
                // center
                {
                    int cx = Math.max(0, x + width / 2 - controlEdgeSize / 2);
                    int cy = Math.max(0, y + height / 2 - controlEdgeSize / 2);
                    int cw = Math.min(controlEdgeSize, targetWidth - cx);
                    int ch = Math.min(controlEdgeSize, targetHeight - cy);
                    g.drawRect(cx, cy, cw, ch);
                }
                // right
                {
                    int cx = Math.max(0, x + width - controlEdgeSize / 2);
                    int cy = Math.max(0, y + height / 2 - controlEdgeSize / 2);
                    int cw = Math.min(controlEdgeSize, targetWidth - cx);
                    int ch = Math.min(controlEdgeSize, targetHeight - cy);
                    g.drawRect(cx, cy, cw, ch);
                }
                // bottom-left
                {
                    int cx = Math.max(0, x - controlEdgeSize / 2);
                    int cy = Math.max(0, y + height - controlEdgeSize / 2);
                    int cw = Math.min(controlEdgeSize, targetWidth - cx);
                    int ch = Math.min(controlEdgeSize, targetHeight - cy);
                    g.drawRect(cx, cy, cw, ch);
                }
                // bottom
                {
                    int cx = Math.max(0, x + width / 2 - controlEdgeSize / 2);
                    int cy = Math.max(0, y + height - controlEdgeSize / 2);
                    int cw = Math.min(controlEdgeSize, targetWidth - cx);
                    int ch = Math.min(controlEdgeSize, targetHeight - cy);
                    g.drawRect(cx, cy, cw, ch);
                }
                // bottom-right
                {
                    int cx = Math.max(0, x + width - controlEdgeSize / 2);
                    int cy = Math.max(0, y + height - controlEdgeSize / 2);
                    int cw = Math.min(controlEdgeSize, targetWidth - cx);
                    int ch = Math.min(controlEdgeSize, targetHeight - cy);
                    g.drawRect(cx, cy, cw, ch);
                }
            }
        });

        if (drawFrom != null && drawTo != null) {
            // new rectangle
            int fromX = drawFrom.x;
            int fromY = drawFrom.y;
            int toX = drawTo.x;
            int toY = drawTo.y;
            int x = Math.min(fromX, toX);
            int y = Math.min(fromY, toY);
            int width = Math.abs(toX - fromX);
            int height = Math.abs(toY - fromY);
            g.setColor(new Color(0, 8, 255));
            g.drawRect(x, y, width, height);
        } else if (currentPoint != null) {
            // x-y-cross
            g.setColor(new Color(255, 8, 0));
            g.drawLine(0, (int) currentPoint.getY(), scaledImage.getWidth(null), (int) currentPoint.getY());
            g.drawLine((int) currentPoint.getX(), 0, (int) currentPoint.getX(), scaledImage.getHeight(null));
        }
    }

    private Optional<DetectedObject> getObject(Image scaledImage, Point point) {
        return editableImage
                .getDetectedObjects()
                .stream()
                .filter(object -> DetectedObjectUtil.isObjectArea(scaledImage, object, point))
                .findFirst();
    }

    private DetectedObject getObjectAtPoint(Image scaledImage, Point point) {
        DetectedObject selectedObject = null;
        for (DetectedObject object : editableImage.getDetectedObjects()) {
            boolean selected = DetectedObjectUtil.isSelected(scaledImage, object, point);
            if (selected) {
                selectedObject = object;
            }
        }
        return selectedObject;
    }
}
