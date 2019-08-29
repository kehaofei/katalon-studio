package com.kms.katalon.composer.windows.dialog;

import java.io.File;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;

import com.kms.katalon.composer.components.impl.control.ScrollableComposite;
import com.kms.katalon.composer.components.impl.util.ControlUtils;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.components.services.UISynchronizeService;
import com.kms.katalon.composer.components.util.ColorUtil;
import com.kms.katalon.composer.windows.element.BasicWindowsElement;
import com.kms.katalon.core.mobile.keyword.internal.GUIObject;

public class WindowsScreenView {

    private Image currentScreenShot;

    private Canvas canvas;

    private double currentX = 0, currentY = 0, currentWidth = 0, currentHeight = 0;

    private double hRatio;

    private boolean isDisposed;

    private WindowsObjectDialog parentDialog;

    private ScrolledComposite scrolledComposite;

    public WindowsScreenView(WindowsObjectDialog parentDialog) {
        this.parentDialog = parentDialog;
    }

    public Composite createControls(Composite parent) {
        Composite mainComposite = new Composite(parent, SWT.NONE);
        mainComposite.setLayout(new GridLayout());
        Label lblScreenComposite = new Label(mainComposite, SWT.NONE);
        lblScreenComposite.setText("SCREEN VIEW");
        ControlUtils.setFontToBeBold(lblScreenComposite);

        scrolledComposite = new ScrollableComposite(mainComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);

        scrolledComposite.setLayout(new GridLayout());

        Composite container = new Composite(scrolledComposite, SWT.NULL);
        container.setLayout(new FillLayout());
        scrolledComposite.setContent(container);

        canvas = new Canvas(container, SWT.NONE);
        canvas.pack();

        canvas.addPaintListener(new PaintListener() {

            public void paintControl(PaintEvent e) {
                if (currentScreenShot != null && !currentScreenShot.isDisposed()) {
                    e.gc.drawImage(currentScreenShot, 0, 0);

                    if (currentWidth != 0 && currentHeight != 0) {
                        Color oldForegroundColor = e.gc.getForeground();
                        e.gc.setForeground(ColorUtil.getColor("#76BF42"));
                        int x = safeRoundDouble(currentX);
                        int y = safeRoundDouble(currentY);
                        int width = safeRoundDouble(currentWidth);
                        int height = safeRoundDouble(currentHeight);

                        e.gc.drawRectangle(x, y, width, height);
                        e.gc.drawRectangle(x + 1, Math.max(y + 1, 0), Math.max(width - 2, 0), Math.max(height - 2, 0));

                        e.gc.setForeground(oldForegroundColor);
                    }
                }
            }
        });

        canvas.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDown(MouseEvent e) {
                if (e.button == 1) {
                    inspectElementAt(e.x, e.y);
                }
            }
        });

        return mainComposite;
    }

    private void inspectElementAt(int x, int y) {
        Double realX = x / hRatio;
        Double realY = y / hRatio;
        parentDialog.setSelectedElementByLocation(safeRoundDouble(realX), safeRoundDouble(realY));
    }

    private boolean isElementOnScreen(final Double x, final Double y, final Double width, final Double height) {
        Rectangle elementRect = new Rectangle(x.intValue(), y.intValue(), width.intValue(), height.intValue());
        return elementRect.intersects(getCurrentViewportRect());
    }

    private void scrollToElement(final Double x, final Double y) {
        scrolledComposite.setOrigin(x.intValue(), y.intValue());
    }

    private Rectangle getCurrentViewportRect() {
        ScrollBar verticalBar = scrolledComposite.getVerticalBar();
        ScrollBar horizontalBar = scrolledComposite.getHorizontalBar();
        int viewPortY = (verticalBar.isVisible()) ? verticalBar.getSelection() : 0;
        int viewPortX = (horizontalBar.isVisible()) ? horizontalBar.getSelection() : 0;
        Point viewPortSize = scrolledComposite.getSize();
        Rectangle viewPortRect = new Rectangle(viewPortX, viewPortY, viewPortSize.x, viewPortSize.y);
        return viewPortRect;
    }

    public void highlight(final double x, final double y, final double width, final double height) {
        // Scale the coordinator depend on the ratio between scaled image / source image
        Display.getCurrent().syncExec(new Runnable() {
            @Override
            public void run() {
                double currentX = x * hRatio;
                double currentY = y * hRatio;
                double currentWidth = width * hRatio;
                double currentHeight = height * hRatio;
                if (!isElementOnScreen(currentX, currentY, currentWidth, currentHeight)) {
                    scrollToElement(currentX, currentY);
                }
            }
        });

        Thread highlightThread = new Thread(new Runnable() {

            @Override
            public void run() {
                for (int i = 0; i < 9; i++) {
                    if (i % 2 == 1) {
                        WindowsScreenView.this.currentX = x * hRatio;
                        WindowsScreenView.this.currentY = y * hRatio;
                        WindowsScreenView.this.currentWidth = width * hRatio;
                        WindowsScreenView.this.currentHeight = height * hRatio;
                    } else {
                        WindowsScreenView.this.currentX = 0;
                        WindowsScreenView.this.currentY = 0;
                        WindowsScreenView.this.currentWidth = 0;
                        WindowsScreenView.this.currentHeight = 0;
                    }
                    try {
                        Thread.sleep(200L);
                    } catch (InterruptedException e) {}
                    UISynchronizeService.syncExec(() -> {
                        if (!canvas.isDisposed()) {
                            canvas.redraw();
                        }
                    });
                }
            }
        });
        highlightThread.start();
    }

    private Image scaleImage(Image image, double newWidth, double newHeight) {
        Image scaled = new Image(Display.getDefault(), safeRoundDouble(newWidth), safeRoundDouble(newHeight));
        GC gc = new GC(scaled);
        gc.setAntialias(SWT.ON);
        gc.setInterpolation(SWT.HIGH);
        gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, 0, 0, safeRoundDouble(newWidth),
                safeRoundDouble(newHeight));
        gc.dispose();
        image.dispose();
        return scaled;
    }

    public void highlightElement(BasicWindowsElement selectedElement) {
        Map<String, String> attributes = selectedElement.getProperties();
        if (attributes == null || !attributes.containsKey(GUIObject.X) || !attributes.containsKey(GUIObject.Y)
                || !attributes.containsKey(GUIObject.WIDTH) || !attributes.containsKey(GUIObject.HEIGHT)) {
            return;
        }
        double x = Double.parseDouble(attributes.get(GUIObject.X));
        double y = Double.parseDouble(attributes.get(GUIObject.Y));
        double w = Double.parseDouble(attributes.get(GUIObject.WIDTH));
        double h = Double.parseDouble(attributes.get(GUIObject.HEIGHT));
        highlight(x, y, w, h);
    }

    public void refreshDialog(File imageFile) {
        try {
            if (imageFile == null) {
                currentScreenShot = null;
            } else {
                ImageDescriptor imgDesc = ImageDescriptor.createFromURL(imageFile.toURI().toURL());
                Image img = imgDesc.createImage();
    
                hRatio = 1.0d;
    
                currentScreenShot = scaleImage(img, ((double) img.getBounds().width) * hRatio, ((double) img.getBounds().height) * hRatio);
            }

            UISynchronizeService.asyncExec(new Runnable() {

                @Override
                public void run() {
                    if (!canvas.isDisposed()) {
                        canvas.redraw();
                    }
                }
            });

            refreshView();
        } catch (Exception ex) {
            LoggerSingleton.logError(ex);
        }
    }

    private void refreshView() {
        if (scrolledComposite == null || currentScreenShot == null) {
            return;
        }
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (currentScreenShot != null) {
                    scrolledComposite.setMinSize(currentScreenShot.getImageData().width + 10,
                            currentScreenShot.getImageData().height + 10);
                } else {
                    scrolledComposite.setMinSize(scrolledComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                }
            }
        });
    }

    public void dispose() {
        this.isDisposed = true;
    }

    public boolean isDisposed() {
        return isDisposed;
    }

    public static int safeRoundDouble(double d) {
        long rounded = Math.round(d);
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, rounded));
    }
}