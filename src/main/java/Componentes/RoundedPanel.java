package Componentes;

import java.awt.*;
import java.beans.Beans;
import javax.swing.*;

public class RoundedPanel extends JPanel {

    // --- Propriedades JavaBeans ---
    private int arcWidth = 24;            // raio horizontal
    private int arcHeight = 24;           // raio vertical
    private int shadowSize = 10;          // "intensidade"/expansão da sombra
    private float shadowOpacity = 0.25f;  // 0..1 (alfa da sombra)
    private float backgroundOpacity = 1f; // 0..1 (alfa do fundo)
    private Color shadowColor = new Color(0, 0, 0); // cor da sombra

    public RoundedPanel() {
        setOpaque(false);
        super.setBackground(new Color(255, 255, 255));
        if (Beans.isDesignTime()) setPreferredSize(new Dimension(320, 180));
    }

    // --- Getters/Setters (bound) ---
    public int getArcWidth() { return arcWidth; }
    public void setArcWidth(int arcWidth) {
        int old = this.arcWidth; this.arcWidth = Math.max(0, arcWidth);
        firePropertyChange("arcWidth", old, this.arcWidth); repaint();
    }

    public int getArcHeight() { return arcHeight; }
    public void setArcHeight(int arcHeight) {
        int old = this.arcHeight; this.arcHeight = Math.max(0, arcHeight);
        firePropertyChange("arcHeight", old, this.arcHeight); repaint();
    }

    public int getShadowSize() { return shadowSize; }
    public void setShadowSize(int shadowSize) {
        int old = this.shadowSize; this.shadowSize = Math.max(0, shadowSize);
        firePropertyChange("shadowSize", old, this.shadowSize); revalidate(); repaint();
    }

    public float getShadowOpacity() { return shadowOpacity; }
    public void setShadowOpacity(float shadowOpacity) {
        float old = this.shadowOpacity; this.shadowOpacity = clamp01(shadowOpacity);
        firePropertyChange("shadowOpacity", old, this.shadowOpacity); repaint();
    }

    public float getBackgroundOpacity() { return backgroundOpacity; }
    public void setBackgroundOpacity(float backgroundOpacity) {
        float old = this.backgroundOpacity; this.backgroundOpacity = clamp01(backgroundOpacity);
        firePropertyChange("backgroundOpacity", old, this.backgroundOpacity); repaint();
    }

    public Color getShadowColor() { return shadowColor; }
    public void setShadowColor(Color c) {
        Color old = this.shadowColor; this.shadowColor = (c == null ? Color.BLACK : c);
        firePropertyChange("shadowColor", old, this.shadowColor); repaint();
    }

    @Override
    public void setBackground(Color bg) {
        Color old = getBackground(); super.setBackground(bg);
        firePropertyChange("background", old, bg); repaint();
    }

    // --- Atalhos HEX (#RRGGBB ou #AARRGGBB) ---
    public void setBackgroundHex(String hex) {
        Color c = parseHex(hex);
        if (c.getAlpha() < 255) setBackgroundOpacity(c.getAlpha() / 255f);
        setBackground(new Color(c.getRed(), c.getGreen(), c.getBlue()));
    }
    public void setShadowHex(String hex) {
        Color c = parseHex(hex);
        setShadowColor(new Color(c.getRed(), c.getGreen(), c.getBlue()));
        if (c.getAlpha() < 255) setShadowOpacity(c.getAlpha() / 255f);
    }

    private static Color parseHex(String hex) {
        if (hex == null) throw new IllegalArgumentException("hex nulo");
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
        if (s.length() != 6 && s.length() != 8)
            throw new IllegalArgumentException("Use #RRGGBB ou #AARRGGBB");
        long v = Long.parseLong(s, 16);
        return (s.length() == 6) ? new Color((int) v) : new Color((int) v, true);
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    @Override
    public Insets getInsets() { int s = shadowSize; return new Insets(s, s, s, s); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth(), h = getHeight(); if (w <= 0 || h <= 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int s = shadowSize; int x = s, y = s, rw = w - s * 2, rh = h - s * 2;

        // sombra com cor configurável
        if (s > 0 && shadowOpacity > 0f) {
            for (int i = s; i >= 1; i--) {
                float alpha = (shadowOpacity * i) / s;
                Color sc = new Color(shadowColor.getRed(), shadowColor.getGreen(), shadowColor.getBlue(), Math.round(alpha * 255));
                g2.setColor(sc);
                g2.fillRoundRect(x - (s - i), y - (s - i), rw + (s - i) * 2, rh + (s - i) * 2,
                                 arcWidth + (s - i) * 2, arcHeight + (s - i) * 2);
            }
        }

        // fundo (Background padrão + opacidade)
        Color bg = getBackground();
        Color bgA = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), Math.round(backgroundOpacity * 255));
        g2.setColor(bgA);
        g2.fillRoundRect(x, y, rw, rh, arcWidth, arcHeight);

        g2.dispose();
    }
}
