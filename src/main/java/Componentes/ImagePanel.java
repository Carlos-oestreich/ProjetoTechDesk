/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Componentes;

/**
 *
 * @author Alexis
 */

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.Beans;
import java.io.File;
import java.io.Serializable;
import javax.imageio.ImageIO;

public class ImagePanel extends JPanel implements Serializable {
    
    private static final long serialVersionUID = 1L;

    public enum ScaleType { FIT, FILL, STRETCH, CENTER }

    private Icon image;            // editor de propriedade do NetBeans permite escolher imagem
    private File imageFile;        // abre dialog de arquivo na Property Sheet
    private String imagePath;      // alternativa via String
    private ScaleType scaleType = ScaleType.STRETCH;
    private boolean highQuality = true;
    private Color placeholderColor = new Color(230, 230, 230);

    public ImagePanel() {
        setOpaque(true);
    }

    // ======= Propriedades =======

    public Icon getImage() {
        return image;
    }

    public void setImage(Icon newImage) {
        Icon old = this.image;
        this.image = newImage;
        firePropertyChange("image", old, newImage);
        repaint();
        revalidate();
    }

    public File getImageFile() {
        return imageFile;
    }

    public void setImageFile(File newFile) {
        File old = this.imageFile;
        this.imageFile = newFile;
        firePropertyChange("imageFile", old, newFile);
        if (newFile != null) {
            setImage(new ImageIcon(newFile.getAbsolutePath())); // dispara repaint via setImage
            setImagePath(newFile.getAbsolutePath());            // mantém sincronizado
        } else {
            setImage(null);
            setImagePath(null);
        }
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String newPath) {
        String old = this.imagePath;
        this.imagePath = newPath;
        firePropertyChange("imagePath", old, newPath);
        if (newPath != null && !newPath.isBlank()) {
            setImage(new ImageIcon(newPath));
        } else {
            setImage(null);
        }
    }

    public ScaleType getScaleType() {
        return scaleType;
    }

    public void setScaleType(ScaleType newScale) {
        ScaleType old = this.scaleType;
        this.scaleType = newScale != null ? newScale : ScaleType.STRETCH;
        firePropertyChange("scaleType", old, this.scaleType);
        repaint();
        revalidate();
    }

    public boolean isHighQuality() {
        return highQuality;
    }

    public void setHighQuality(boolean newValue) {
        boolean old = this.highQuality;
        this.highQuality = newValue;
        firePropertyChange("highQuality", old, newValue);
        repaint();
    }

    public Color getPlaceholderColor() {
        return placeholderColor;
    }

    public void setPlaceholderColor(Color color) {
        Color old = this.placeholderColor;
        this.placeholderColor = color;
        firePropertyChange("placeholderColor", old, color);
        repaint();
    }

    // ======= Renderização =======

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Icon ic = this.image;

        // Placeholder amigável no design-time (quando adicionado no GUI Builder)
        if (ic == null) {
            if (Beans.isDesignTime()) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    if (highQuality) enableHQ(g2);
                    g2.setColor(placeholderColor);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 0, 0);
                    g2.setColor(new Color(180, 180, 180));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 0, 0);

                    String txt = "ImagePanel (sem imagem)";
                    FontMetrics fm = g2.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(txt)) / 2;
                    int y = (getHeight() + fm.getAscent()) / 2 - 2;
                    g2.drawString(txt, Math.max(8, x), Math.max(16, y));
                } finally {
                    g2.dispose();
                }
            }
            return;
        }

        // Converter Icon -> Image para manipular dimensões facilmente
        Image img = iconToImage(ic);
        if (img == null) return;

        int iw = img.getWidth(this);
        int ih = img.getHeight(this);
        int pw = getWidth();
        int ph = getHeight();

        if (iw <= 0 || ih <= 0 || pw <= 0 || ph <= 0) return;

        Rectangle dest = computeDestRect(iw, ih, pw, ph, scaleType);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            if (highQuality) enableHQ(g2);
            g2.drawImage(img, dest.x, dest.y, dest.x + dest.width, dest.y + dest.height,
                         0, 0, iw, ih, this);
        } finally {
            g2.dispose();
        }
    }

    private static void enableHQ(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private static Image iconToImage(Icon ic) {
        if (ic == null) return null;
        if (ic instanceof ImageIcon) {
            return ((ImageIcon) ic).getImage();
        }
        // Renderiza outros tipos de Icon num BufferedImage
        int w = ic.getIconWidth();
        int h = ic.getIconHeight();
        if (w <= 0 || h <= 0) return null;
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            enableHQ(g2);
            ic.paintIcon(null, g2, 0, 0);
        } finally {
            g2.dispose();
        }
        return bi;
    }

    private static Rectangle computeDestRect(int iw, int ih, int pw, int ph, ScaleType type) {
        switch (type) {
            case CENTER: {
                int x = (pw - iw) / 2;
                int y = (ph - ih) / 2;
                return new Rectangle(x, y, iw, ih);
            }
            case STRETCH: {
                return new Rectangle(0, 0, pw, ph);
            }
            case FILL: {
                double ir = (double) iw / ih;
                double pr = (double) pw / ph;
                int nw, nh;
                if (pr > ir) {
                    // mais largo que a imagem -> escala por altura e corta laterais
                    nh = ph;
                    nw = (int) Math.round(ph * ir);
                } else {
                    // mais alto que a imagem -> escala por largura e corta topo/baixo
                    nw = pw;
                    nh = (int) Math.round(pw / ir);
                }
                int x = (pw - nw) / 2;
                int y = (ph - nh) / 2;
                return new Rectangle(x, y, nw, nh);
            }
            case FIT:
            default: {
                double ir = (double) iw / ih;
                double pr = (double) pw / ph;
                int nw, nh;
                if (pr > ir) {
                    // painel é mais largo -> limita pela altura
                    nh = ph;
                    nw = (int) Math.round(ph * ir);
                } else {
                    // painel é mais alto -> limita pela largura
                    nw = pw;
                    nh = (int) Math.round(pw / ir);
                }
                int x = (pw - nw) / 2;
                int y = (ph - nh) / 2;
                return new Rectangle(x, y, nw, nh);
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        // Tamanho default amigável para o designer
        return new Dimension(320, 180);
    }

    // Utilitário opcional para carregar de recurso do classpath (não obrigatório)
    public void setResourcePath(String resourcePath) {
        try {
            if (resourcePath == null) {
                setImage(null);
                return;
            }
            Image img = ImageIO.read(getClass().getResource(resourcePath));
            setImage(new ImageIcon(img));
            setImagePath(resourcePath);
            firePropertyChange("resourcePath", null, resourcePath);
        } catch (Exception e) {
            // silencioso no design-time; em runtime logue se quiser
        }
    }
}
