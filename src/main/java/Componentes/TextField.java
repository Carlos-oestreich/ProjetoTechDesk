package Componentes;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;

public class TextField extends JPanel {

    // ---- Ícone + campos ----
    private final JLabel iconLabel = new JLabel();

    // Dois campos: um normal e um de senha; alternamos por CardLayout
    private final JTextField tf = new JTextField();
    private final JPasswordField pf = new JPasswordField();
    private final JPanel inputDeck = new JPanel(new CardLayout()); // "TF" / "PF"

    // ---- Propriedades “Material” ----
    private Color lineColor = new Color(190, 200, 210);
    private Color focusedLineColor = new Color(25, 118, 210);
    private Color errorLineColor = new Color(211, 47, 47);
    private boolean error = false;
    private String placeholder = "Digite aqui...";
    private int leftPadding = 8;
    private int rightPadding = 8;
    private int lineThickness = 2;
    private int focusedLineThickness = 3;
    private int iconTextGap = 8;

    // ---- Auto-escala do ícone ----
    private boolean iconAutoSize = true;
    private int iconMin = 12;
    private int iconMax = 28;
    private float iconScaleFactor = 0.90f;
    private Icon originalIcon = null;

    // ---- Senha & tipos de entrada ----
    public enum InputType { TEXT, INTEGER, DECIMAL, EMAIL, PHONE }
    private boolean passwordMode = false;
    private char echoChar = '\u2022'; // •
    private InputType inputType = InputType.TEXT;
    private int maxLength = -1; // -1 = sem limite
    private boolean allowNegative = false;   // para INTEGER/DECIMAL

    // Filtro compartilhado; aplicado ao documento ativo
    private final InputTypeFilter filter = new InputTypeFilter();

    public TextField() {
        setOpaque(true);
        setBackground(Color.WHITE);      // <- AQUI!
        setLayout(new BorderLayout());

        // Ícone à esquerda
        iconLabel.setOpaque(false);
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, leftPadding, 0, iconTextGap));
        add(iconLabel, BorderLayout.WEST);

        // Preparar campos
        tf.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, rightPadding));
        tf.setOpaque(false);
        tf.setFont(tf.getFont().deriveFont(Font.PLAIN, 14f));
        pf.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, rightPadding));
        pf.setOpaque(false);
        pf.setFont(pf.getFont().deriveFont(Font.PLAIN, 14f));
        pf.setEchoChar(echoChar);

        // Adiciona ao deck
        inputDeck.setOpaque(false);
        inputDeck.add(tf, "TF");
        inputDeck.add(pf, "PF");
        add(inputDeck, BorderLayout.CENTER);
        showCard(passwordMode ? "PF" : "TF");

        // Eventos para repintar placeholder/underline
        FocusAdapter focusPainter = new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e) { repaint(); }
        };
        tf.addFocusListener(focusPainter);
        pf.addFocusListener(focusPainter);

        DocumentListener dl = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { repaint(); }
            @Override public void removeUpdate(DocumentEvent e) { repaint(); }
            @Override public void changedUpdate(DocumentEvent e) { repaint(); }
        };
        tf.getDocument().addDocumentListener(dl);
        pf.getDocument().addDocumentListener(dl);

        // Filtro inicial
        setFilterOnActiveDoc();

        // Recalcula escala do ícone quando o componente muda de tamanho
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { rescaleIconIfNeeded(); }
        });

        setPreferredSize(new Dimension(260, 40));
    }

    // ====== utilidades ======
    private JTextComponent activeField() { return passwordMode ? pf : tf; }

    private void showCard(String key) {
        ((CardLayout) inputDeck.getLayout()).show(inputDeck, key);
        revalidate();
        repaint();
    }

    private void setFilterOnActiveDoc() {
        JTextComponent comp = activeField();
        Document doc = comp.getDocument();
        if (doc instanceof AbstractDocument) {
            ((AbstractDocument) doc).setDocumentFilter(filter);
        }
    }

    // ====== Auto-escala do ícone ======
    private void rescaleIconIfNeeded() {
        if (!iconAutoSize || originalIcon == null) return;

        int h = getHeight();
        int underline = Math.max(lineThickness, focusedLineThickness);
        int usefulH = Math.max(0, h - underline);

        FontMetrics fm = activeField().getFontMetrics(activeField().getFont());
        int textH = fm.getAscent() + fm.getDescent();

        int target = Math.round(Math.max(usefulH, textH) * iconScaleFactor);
        target = Math.max(iconMin, Math.min(iconMax, target));
        if (target <= 0) return;

        Icon scaled = scaleIconKeepingAlpha(originalIcon, target, target);
        iconLabel.setIcon(scaled);
        iconLabel.setVisible(scaled != null);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, leftPadding, 0, iconTextGap));
        revalidate();
        repaint();
    }

    private static Icon scaleIconKeepingAlpha(Icon ic, int w, int h) {
        if (ic == null || w <= 0 || h <= 0) return null;
        int srcW = ic.getIconWidth();
        int srcH = ic.getIconHeight();
        if (srcW <= 0 || srcH <= 0) return null;

        BufferedImage src = new BufferedImage(srcW, srcH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = src.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ic.paintIcon(null, g, 0, 0);
        g.dispose();

        Image scaled = src.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        return new ImageIcon(out);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final int w = getWidth();
        final int h = getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Placeholder
        JTextComponent field = activeField();
        boolean focused = field.hasFocus();
        String current = (field == pf) ? new String(((JPasswordField) field).getPassword()) : field.getText();

        if (!focused && (current == null || current.isEmpty()) && placeholder != null && !placeholder.isEmpty()) {
            g2.setFont(field.getFont());
            g2.setColor(new Color(150, 150, 150));
            int leftBlock = 0;
            if (iconLabel.isVisible() && iconLabel.getIcon() != null) {
                leftBlock = iconLabel.getPreferredSize().width;
            }
            int x = Math.max(leftPadding, leftBlock + leftPadding);
            int y = (h + g2.getFontMetrics().getAscent()) / 2 - 3;
            g2.drawString(placeholder, x, y);
        }

        // Linha inferior (underline)
        int thickness = focused ? focusedLineThickness : lineThickness;
        int yLine = h - thickness;
        g2.setColor(error ? errorLineColor : (focused ? focusedLineColor : lineColor));
        g2.fillRect(0, yLine, w, thickness);

        g2.dispose();
    }

    // ====== Filtro de entrada ======
    private class InputTypeFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string == null) return;
            String toInsert = filterString(fb, offset, string);
            if (toInsert != null && !toInsert.isEmpty()) {
                super.insertString(fb, offset, toInsert, attr);
            }
        }
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null) { super.replace(fb, offset, length, text, attrs); return; }
            String toReplace = filterString(fb, offset, text);
            if (toReplace != null && !toReplace.isEmpty()) {
                super.replace(fb, offset, length, toReplace, attrs);
            } else if (length > 0) {
                // permitir deleção mesmo se texto rejeitado
                super.replace(fb, offset, length, "", attrs);
            }
        }

        private String filterString(FilterBypass fb, int offset, String incoming) throws BadLocationException {
            Document doc = fb.getDocument();
            String current = doc.getText(0, doc.getLength());
            StringBuilder sb = new StringBuilder(current);
            sb.insert(offset, incoming);

            // aplica maxLength
            if (maxLength >= 0 && sb.length() > maxLength) {
                int allowed = maxLength - current.length();
                if (allowed <= 0) return "";
                incoming = incoming.substring(0, Math.min(allowed, incoming.length()));
                sb = new StringBuilder(current);
                sb.insert(offset, incoming);
            }

            // valida conforme tipo
            if (inputType == InputType.TEXT) return incoming;

            switch (inputType) {
                case INTEGER:
                    if (isValidInteger(sb.toString())) return incoming;
                    break;
                case DECIMAL:
                    if (isValidDecimal(sb.toString())) return incoming;
                    break;
                case EMAIL:
                    if (isValidEmailPartial(sb.toString())) return incoming;
                    break;
                case PHONE:
                    if (isValidPhonePartial(sb.toString())) return incoming;
                    break;
                default:
                    return incoming;
            }
            // se não validou, rejeita
            Toolkit.getDefaultToolkit().beep();
            return "";
        }

        private boolean isValidInteger(String s) {
            if (s.isEmpty()) return true; // permitir digitação progressiva
            if (allowNegative) {
                return s.matches("-?\\d+");
            } else {
                return s.matches("\\d+");
            }
        }

        private boolean isValidDecimal(String s) {
            if (s.isEmpty()) return true;
            // aceita ponto ou vírgula como separador; permite parcial (terminar com separador)
            String sign = allowNegative ? "-?" : "";
            return s.matches(sign + "\\d*([\\.,]\\d*)?");
        }

        private boolean isValidEmailPartial(String s) {
            if (s.isEmpty()) return true;
            // parcial: letras/dígitos + . _ - + @ e apenas um @
            if (!s.matches("[A-Za-z0-9._%+\\-@]*")) return false;
            int at = s.indexOf('@');
            return at == -1 || s.indexOf('@', at + 1) == -1;
        }

        private boolean isValidPhonePartial(String s) {
            if (s.isEmpty()) return true;
            // aceita dígitos, espaço, parênteses, + e -
            return s.matches("[0-9 ()+\\-]*");
        }
    }

    // -----------------------
    // Getters / Setters (JavaBeans)
    // -----------------------
    public Icon getIconLeft() { return originalIcon; }
    public void setIconLeft(Icon icon) {
        this.originalIcon = icon;
        if (iconAutoSize) {
            rescaleIconIfNeeded();
        } else {
            iconLabel.setIcon(icon);
            iconLabel.setVisible(icon != null);
            revalidate();
            repaint();
        }
    }

    public boolean isIconAutoSize() { return iconAutoSize; }
    public void setIconAutoSize(boolean iconAutoSize) {
        this.iconAutoSize = iconAutoSize;
        rescaleIconIfNeeded();
    }

    public int getIconMin() { return iconMin; }
    public void setIconMin(int iconMin) { this.iconMin = Math.max(1, iconMin); rescaleIconIfNeeded(); }

    public int getIconMax() { return iconMax; }
    public void setIconMax(int iconMax) { this.iconMax = Math.max(this.iconMin, iconMax); rescaleIconIfNeeded(); }

    public float getIconScaleFactor() { return iconScaleFactor; }
    public void setIconScaleFactor(float iconScaleFactor) {
        this.iconScaleFactor = Math.max(0.1f, Math.min(1.5f, iconScaleFactor));
        rescaleIconIfNeeded();
    }

    // Texto (funciona nos dois modos)
    public String getText() {
        return passwordMode ? new String(pf.getPassword()) : tf.getText();
    }
    public void setText(String text) {
        if (passwordMode) pf.setText(text); else tf.setText(text);
        repaint();
    }

    public String getPlaceholder() { return placeholder; }
    public void setPlaceholder(String placeholder) { this.placeholder = placeholder; repaint(); }

    public Color getLineColor() { return lineColor; }
    public void setLineColor(Color lineColor) { this.lineColor = lineColor; repaint(); }

    public Color getFocusedLineColor() { return focusedLineColor; }
    public void setFocusedLineColor(Color focusedLineColor) { this.focusedLineColor = focusedLineColor; repaint(); }

    public Color getErrorLineColor() { return errorLineColor; }
    public void setErrorLineColor(Color errorLineColor) { this.errorLineColor = errorLineColor; repaint(); }

    public boolean isError() { return error; }
    public void setError(boolean error) { this.error = error; repaint(); }

    public int getLeftPadding() { return leftPadding; }
    public void setLeftPadding(int leftPadding) {
        this.leftPadding = leftPadding;
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, leftPadding, 0, iconTextGap));
        revalidate(); repaint();
    }

    public int getRightPadding() { return rightPadding; }
    public void setRightPadding(int rightPadding) {
        this.rightPadding = rightPadding;
        tf.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, rightPadding));
        pf.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, rightPadding));
        revalidate(); repaint(); rescaleIconIfNeeded();
    }

    public int getLineThickness() { return lineThickness; }
    public void setLineThickness(int lineThickness) { this.lineThickness = Math.max(1, lineThickness); repaint(); rescaleIconIfNeeded(); }

    public int getFocusedLineThickness() { return focusedLineThickness; }
    public void setFocusedLineThickness(int focusedLineThickness) { this.focusedLineThickness = Math.max(1, focusedLineThickness); repaint(); rescaleIconIfNeeded(); }

    public int getIconTextGap() { return iconTextGap; }
    public void setIconTextGap(int iconTextGap) {
        this.iconTextGap = iconTextGap;
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, leftPadding, 0, iconTextGap));
        revalidate(); repaint();
    }

    // ====== Novas propriedades ======
    public boolean isPasswordMode() { return passwordMode; }
    public void setPasswordMode(boolean passwordMode) {
        boolean old = this.passwordMode;
        this.passwordMode = passwordMode;
        firePropertyChange("passwordMode", old, passwordMode);
        // mantém texto ao alternar
        String txt = getText();
        showCard(passwordMode ? "PF" : "TF");
        setText(txt);
        // echo char
        pf.setEchoChar(echoChar);
        // reaplicar filtro no doc correto
        setFilterOnActiveDoc();
        repaint();
    }

    public char getEchoChar() { return echoChar; }
    public void setEchoChar(char echoChar) {
        char old = this.echoChar;
        this.echoChar = echoChar;
        pf.setEchoChar(echoChar);
        firePropertyChange("echoChar", old, echoChar);
        repaint();
    }

    public InputType getInputType() { return inputType; }
    public void setInputType(InputType inputType) {
        InputType old = this.inputType;
        this.inputType = (inputType == null) ? InputType.TEXT : inputType;
        firePropertyChange("inputType", old, this.inputType);
        setFilterOnActiveDoc();
    }

    public int getMaxLength() { return maxLength; }
    public void setMaxLength(int maxLength) {
        int old = this.maxLength;
        this.maxLength = maxLength < 0 ? -1 : maxLength;
        firePropertyChange("maxLength", old, this.maxLength);
        setFilterOnActiveDoc();
    }

    public boolean isAllowNegative() { return allowNegative; }
    public void setAllowNegative(boolean allowNegative) {
        boolean old = this.allowNegative;
        this.allowNegative = allowNegative;
        firePropertyChange("allowNegative", old, allowNegative);
        setFilterOnActiveDoc();
    }

    // Acesso direto aos campos, se precisar
    public JTextField getTextField() { return tf; }
    public JPasswordField getPasswordField() { return pf; }
}
