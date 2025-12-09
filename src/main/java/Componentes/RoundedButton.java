package Componentes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;

/**
 * JButton com cantos arredondados configuráveis por canto,
 * suporte a ícone com cor personalizada e autoajuste de tamanho.
 * Compatível com o editor visual do NetBeans.
 */
public class RoundedButton extends JButton implements Serializable {

    // ------- Raio por canto -------
    private int arcTopLeft = 16;
    private int arcTopRight = 16;
    private int arcBottomRight = 16;
    private int arcBottomLeft = 16;

    // ------- Cores de estado -------
    private Color hoverBackground = new Color(0, 0, 0, 20);
    private Color pressedBackground = new Color(0, 0, 0, 40);
    private Color borderColor = null;

    // ------- Borda -------
    private int borderThickness = 0;

    // ------- Ícone -------
    private Color iconColor = null;           // cor aplicada sobre o ícone
    private boolean autoResizeIcon = true;    // ajusta tamanho conforme o botão
    private int iconPadding = 6;              // margem interna para ícone
    private Icon baseIcon = null;             // ícone original (não pintado)
    private Icon tintedIcon = null;           // cache do ícone recolorido

    // ------- Estado interno -------
    private boolean hovered = false;
    private boolean pressed = false;

    public RoundedButton() {
        super();
        setContentAreaFilled(false);
        setOpaque(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setMargin(new Insets(8, 14, 8, 14));
        setBackground(new Color(25, 118, 210)); // Azul Material
        setForeground(Color.WHITE);

        // Hover / Press listeners
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hovered = false; pressed = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { if (SwingUtilities.isLeftMouseButton(e)) { pressed = true; repaint(); } }
            @Override public void mouseReleased(MouseEvent e){ pressed = false; repaint(); }
        };
        addMouseListener(ma);
    }

    // ------- Getters e Setters -------
    public int getArcTopLeft() { return arcTopLeft; }
    public void setArcTopLeft(int v) { arcTopLeft = Math.max(0, v); repaint(); }

    public int getArcTopRight() { return arcTopRight; }
    public void setArcTopRight(int v) { arcTopRight = Math.max(0, v); repaint(); }

    public int getArcBottomRight() { return arcBottomRight; }
    public void setArcBottomRight(int v) { arcBottomRight = Math.max(0, v); repaint(); }

    public int getArcBottomLeft() { return arcBottomLeft; }
    public void setArcBottomLeft(int v) { arcBottomLeft = Math.max(0, v); repaint(); }

    public Color getHoverBackground() { return hoverBackground; }
    public void setHoverBackground(Color c) { hoverBackground = c; repaint(); }

    public Color getPressedBackground() { return pressedBackground; }
    public void setPressedBackground(Color c) { pressedBackground = c; repaint(); }

    public Color getBorderColor() { return borderColor; }
    public void setBorderColor(Color c) { borderColor = c; repaint(); }

    public int getBorderThickness() { return borderThickness; }
    public void setBorderThickness(int t) { borderThickness = Math.max(0, t); repaint(); }

    public Color getIconColor() { return iconColor; }
    public void setIconColor(Color iconColor) {
        this.iconColor = iconColor;
        updateTintedIcon();
    }

    public boolean isAutoResizeIcon() { return autoResizeIcon; }
    public void setAutoResizeIcon(boolean autoResizeIcon) {
        this.autoResizeIcon = autoResizeIcon;
        updateTintedIcon();
    }

    public int getIconPadding() { return iconPadding; }
    public void setIconPadding(int iconPadding) {
        this.iconPadding = Math.max(0, iconPadding);
        updateTintedIcon();
    }

    @Override
    public void setIcon(Icon icon) {
        this.baseIcon = icon;
        updateTintedIcon();
    }

    // ------- Atualiza o ícone conforme cor e tamanho -------
    private void updateTintedIcon() {
        if (baseIcon == null) {
            super.setIcon(null);
            return;
        }

        int targetW = baseIcon.getIconWidth();
        int targetH = baseIcon.getIconHeight();

        if (autoResizeIcon) {
            int s = Math.min(getHeight(), getWidth()) - (iconPadding * 2);
            if (s > 4) {
                targetW = s;
                targetH = s;
            }
        }

        Image img = iconToImage(baseIcon);
        if (iconColor != null) img = applyColorTint(img, iconColor);
        Image scaled = img.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
        tintedIcon = new ImageIcon(scaled);
        super.setIcon(tintedIcon);
        repaint();
    }

    private Image iconToImage(Icon icon) {
        if (icon instanceof ImageIcon) return ((ImageIcon) icon).getImage();
        BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        icon.paintIcon(null, g2, 0, 0);
        g2.dispose();
        return bi;
    }

    private Image applyColorTint(Image img, Color tint) {
        BufferedImage buf = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buf.createGraphics();
        g2.drawImage(img, 0, 0, null);
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.setColor(tint);
        g2.fillRect(0, 0, buf.getWidth(), buf.getHeight());
        g2.dispose();
        return buf;
    }

    // ------- Pintura -------
    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Shape shape = createRoundRect(w, h, arcTopLeft, arcTopRight, arcBottomRight, arcBottomLeft);

            // Fundo base
            g2.setColor(getBackground());
            g2.fill(shape);

            // Overlay de hover / pressed
            if (pressed && pressedBackground != null) {
                g2.setColor(pressedBackground);
                g2.fill(shape);
            } else if (hovered && hoverBackground != null) {
                g2.setColor(hoverBackground);
                g2.fill(shape);
            }

            // Borda
            if (borderThickness > 0 && borderColor != null) {
                g2.setStroke(new BasicStroke(borderThickness));
                g2.setColor(borderColor);
                g2.draw(shape);
            }

            super.paintComponent(g);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public boolean contains(int x, int y) {
        Shape s = createRoundRect(getWidth(), getHeight(), arcTopLeft, arcTopRight, arcBottomRight, arcBottomLeft);
        return s.contains(x, y);
    }

    // ------- Utilitário: cria shape arredondado -------
    private static Shape createRoundRect(int w, int h, int tl, int tr, int br, int bl) {
        tl = Math.max(0, Math.min(tl, Math.min(w, h) / 2));
        tr = Math.max(0, Math.min(tr, Math.min(w, h) / 2));
        br = Math.max(0, Math.min(br, Math.min(w, h) / 2));
        bl = Math.max(0, Math.min(bl, Math.min(w, h) / 2));
        Path2D p = new Path2D.Float();
        p.moveTo(tl, 0);
        p.lineTo(w - tr, 0);
        if (tr > 0) p.quadTo(w, 0, w, tr); else p.lineTo(w, 0);
        p.lineTo(w, h - br);
        if (br > 0) p.quadTo(w, h, w - br, h); else p.lineTo(w, h);
        p.lineTo(bl, h);
        if (bl > 0) p.quadTo(0, h, 0, h - bl); else p.lineTo(0, h);
        p.lineTo(0, tl);
        if (tl > 0) p.quadTo(0, 0, tl, 0); else p.lineTo(0, 0);
        p.closePath();
        return p;
    }
}