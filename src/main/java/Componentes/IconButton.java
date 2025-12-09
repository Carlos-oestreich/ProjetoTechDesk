package Componentes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.Beans;
import java.io.File;

public class IconButton extends JButton {

    // ======== Propriedades editáveis (visíveis no NetBeans) ========

    private String iconPath;              // Caminho no classpath (ex: /imagens/icon.png)
    private String hoverIconPath;         // Ícone alternativo no hover
    private Color hoverTint = Color.GRAY; // Cor de tint no hover
    private boolean useTintOnHover = true;
    private int iconWidth = 24;
    private int iconHeight = 24;
    private boolean handCursor = true;
    private boolean flat = true;
    private float hoverOpacity = 1.0f;    // Opacidade do ícone no hover
    private Color baseTint = null;
    private boolean roundIcon = false;    // NOVO: ícone circular opcional

    // ======== Suporte ao NetBeans (File e Icon editor) ========
    private File iconFile;
    private File hoverIconFile;
    private Icon normalIcon;
    private Icon hoverIconDirect;

    // ======== Cache interno ========
    private ImageIcon baseIcon;
    private ImageIcon hoverIcon;

    // ==========================================================
    // Construtores
    // ==========================================================

    public IconButton() {
        super();
        applyFlatStyle();
        installHoverBehavior();

        // Mostra algo no modo Design do NetBeans
        if (Beans.isDesignTime()) {
            setText("IconButton");
            setPreferredSize(new Dimension(40, 40));
        }
    }

    public IconButton(String iconPath) {
        this();
        setIconPath(iconPath);
    }

    // ==========================================================
    // Aparência Flat e Cursor
    // ==========================================================
    private void applyFlatStyle() {
        if (flat) {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(null);
            setMargin(new Insets(0, 0, 0, 0));
        }
        if (handCursor) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    // ==========================================================
    // Efeito de Hover e Press
    // ==========================================================
    private void installHoverBehavior() {
        final float normalAlpha = 1.0f;

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                ImageIcon hi = resolveHoverIcon();
                if (hi != null && hi != getIcon()) {
                    setIconWithOpacity(hi, hoverOpacity);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (baseIcon != null) {
                    setIconWithOpacity(baseIcon, normalAlpha);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (getIcon() != null) {
                    setIconWithOpacity((ImageIcon) getIcon(), 0.85f);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (getBounds().contains(e.getPoint())) {
                    mouseEntered(e);
                } else {
                    mouseExited(e);
                }
            }
        });
    }

    // ==========================================================
    // Helpers: carregamento e manipulação de ícones
    // ==========================================================

    private void log(String msg) {
        if (!Beans.isDesignTime()) {
            System.err.println("[IconButton] " + msg);
        }
    }

    private ImageIcon loadAndScaleFromClasspath(String path, int w, int h) {
        java.net.URL url = getClass().getResource(path);
        if (url == null) {
            log("Recurso não encontrado no classpath: " + path);
            return null;
        }
        return scaleIcon(new ImageIcon(url), w, h);
    }

    private ImageIcon loadAndScaleFromFile(File file, int w, int h) {
        if (file == null || !file.exists()) {
            log("Arquivo não encontrado: " + file);
            return null;
        }
        return scaleIcon(new ImageIcon(file.getAbsolutePath()), w, h);
    }

    private ImageIcon scaleIcon(Icon ic, int w, int h) {
        if (ic == null) return null;
        int sw = ic.getIconWidth();
        int sh = ic.getIconHeight();
        if (w <= 0 || h <= 0) w = sw;
        if (w <= 0 || h <= 0) return null;

        Image img;
        if (ic instanceof ImageIcon) {
            img = ((ImageIcon) ic).getImage();
        } else {
            BufferedImage bi = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = bi.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            ic.paintIcon(this, g2, 0, 0);
            g2.dispose();
            img = bi;
        }

        Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private void setIconWithOpacity(ImageIcon icon, float alpha) {
        if (icon == null || alpha >= 0.999f) {
            setIcon(icon);
            repaint();
            return;
        }
        Image img = icon.getImage();
        BufferedImage buf = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buf.createGraphics();
        g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        setIcon(new ImageIcon(buf));
        repaint();
    }

    public static ImageIcon recolorIcon(ImageIcon icon, Color color) {
        if (icon == null || color == null) return icon;
        Image img = icon.getImage();
        int w = img.getWidth(null), h = img.getHeight(null);
        BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buf.createGraphics();

        // Máscara circular opcional
        if (color != null) {
            g2.drawImage(img, 0, 0, null);
            g2.setComposite(AlphaComposite.SrcAtop);
            g2.setColor(color);
            g2.fillRect(0, 0, w, h);
        }

        g2.dispose();
        return new ImageIcon(buf);
    }

    private void refreshIcons() {
        ImageIcon resolvedBase =
                (normalIcon != null) ? scaleIcon(normalIcon, iconWidth, iconHeight)
                : (iconFile != null) ? loadAndScaleFromFile(iconFile, iconWidth, iconHeight)
                : (iconPath != null && !iconPath.isEmpty()) ? loadAndScaleFromClasspath(iconPath, iconWidth, iconHeight)
                : null;

        baseIcon = resolvedBase;
        if (baseIcon != null && baseTint != null) {
            baseIcon = recolorIcon(baseIcon, baseTint);
        }
        setIcon(baseIcon);
        hoverIcon = null;
        repaint();
    }

    private ImageIcon resolveHoverIcon() {
        if (hoverIcon != null) return hoverIcon;
        if (hoverIconDirect != null) {
            hoverIcon = scaleIcon(hoverIconDirect, iconWidth, iconHeight);
            return hoverIcon;
        }
        if (hoverIconFile != null) {
            hoverIcon = loadAndScaleFromFile(hoverIconFile, iconWidth, iconHeight);
            if (hoverIcon != null) return hoverIcon;
        }
        if (hoverIconPath != null && !hoverIconPath.isEmpty()) {
            hoverIcon = loadAndScaleFromClasspath(hoverIconPath, iconWidth, iconHeight);
            if (hoverIcon != null) return hoverIcon;
        }
        if (useTintOnHover && baseIcon != null) {
            hoverIcon = recolorIcon(baseIcon, hoverTint);
            return hoverIcon;
        }
        return baseIcon;
    }

    // ==========================================================
    // Getters e Setters JavaBeans
    // ==========================================================

    public String getIconPath() { return iconPath; }
    public void setIconPath(String iconPath) {
        String old = this.iconPath;
        this.iconPath = iconPath;
        firePropertyChange("iconPath", old, iconPath);
        refreshIcons();
    }

    public String getHoverIconPath() { return hoverIconPath; }
    public void setHoverIconPath(String hoverIconPath) {
        String old = this.hoverIconPath;
        this.hoverIconPath = hoverIconPath;
        firePropertyChange("hoverIconPath", old, hoverIconPath);
        this.hoverIcon = null;
        repaint();
    }

    public File getIconFile() { return iconFile; }
    public void setIconFile(File iconFile) {
        File old = this.iconFile;
        this.iconFile = iconFile;
        firePropertyChange("iconFile", old, iconFile);
        refreshIcons();
    }

    public File getHoverIconFile() { return hoverIconFile; }
    public void setHoverIconFile(File hoverIconFile) {
        File old = this.hoverIconFile;
        this.hoverIconFile = hoverIconFile;
        firePropertyChange("hoverIconFile", old, hoverIconFile);
        this.hoverIcon = null;
        repaint();
    }

    public Icon getNormalIcon() { return normalIcon; }
    public void setNormalIcon(Icon normalIcon) {
        Icon old = this.normalIcon;
        this.normalIcon = normalIcon;
        firePropertyChange("normalIcon", old, normalIcon);
        refreshIcons();
    }

    public Icon getHoverIconDirect() { return hoverIconDirect; }
    public void setHoverIconDirect(Icon hoverIconDirect) {
        Icon old = this.hoverIconDirect;
        this.hoverIconDirect = hoverIconDirect;
        firePropertyChange("hoverIconDirect", old, hoverIconDirect);
        this.hoverIcon = null;
        repaint();
    }

    public Color getHoverTint() { return hoverTint; }
    public void setHoverTint(Color hoverTint) {
        Color old = this.hoverTint;
        this.hoverTint = hoverTint;
        firePropertyChange("hoverTint", old, hoverTint);
        repaint();
    }

    public boolean isUseTintOnHover() { return useTintOnHover; }
    public void setUseTintOnHover(boolean useTintOnHover) {
        boolean old = this.useTintOnHover;
        this.useTintOnHover = useTintOnHover;
        firePropertyChange("useTintOnHover", old, useTintOnHover);
        this.hoverIcon = null;
        repaint();
    }

    public int getIconWidth() { return iconWidth; }
    public void setIconWidth(int iconWidth) {
        int old = this.iconWidth;
        this.iconWidth = Math.max(0, iconWidth);
        firePropertyChange("iconWidth", old, this.iconWidth);
        refreshIcons();
    }

    public int getIconHeight() { return iconHeight; }
    public void setIconHeight(int iconHeight) {
        int old = this.iconHeight;
        this.iconHeight = Math.max(0, iconHeight);
        firePropertyChange("iconHeight", old, this.iconHeight);
        refreshIcons();
    }

    public boolean isHandCursor() { return handCursor; }
    public void setHandCursor(boolean handCursor) {
        boolean old = this.handCursor;
        this.handCursor = handCursor;
        firePropertyChange("handCursor", old, handCursor);
        applyFlatStyle();
    }

    public boolean isFlat() { return flat; }
    public void setFlat(boolean flat) {
        boolean old = this.flat;
        this.flat = flat;
        firePropertyChange("flat", old, flat);
        applyFlatStyle();
    }

    public float getHoverOpacity() { return hoverOpacity; }
    public void setHoverOpacity(float hoverOpacity) {
        float clamped = Math.max(0.1f, Math.min(1.0f, hoverOpacity));
        float old = this.hoverOpacity;
        this.hoverOpacity = clamped;
        firePropertyChange("hoverOpacity", old, clamped);
        repaint();
    }

    public Color getBaseTint() { return baseTint; }
    public void setBaseTint(Color baseTint) {
        Color old = this.baseTint;
        this.baseTint = baseTint;
        firePropertyChange("baseTint", old, baseTint);
        refreshIcons();
    }

    public boolean isRoundIcon() { return roundIcon; }
    public void setRoundIcon(boolean roundIcon) {
        boolean old = this.roundIcon;
        this.roundIcon = roundIcon;
        firePropertyChange("roundIcon", old, roundIcon);
        refreshIcons();
    }

    public ImageIcon getBaseIcon() { return baseIcon; }
    public ImageIcon getHoverIcon() { return hoverIcon; }
}
