package Componentes;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.Beans;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

/**
 * MaterialTable – JTable com estilo Material, header Material, coluna de ações sempre visível
 * e propriedades para uso no NetBeans.
 */
public class MaterialTable extends JTable {

    // ========= Aparência / Material =========
    private boolean hoverEnabled = true;
    private Color hoverColor = new Color(0xE3F2FD);

    private boolean selectionEnabled = true;
    private boolean selectionColorEnabled = true;
    private Color selectionBackground = new Color(0xBBDEFB);
    private Color selectionForeground = new Color(0x212121);

    private boolean focusCellIndicatorEnabled = false;  // tira foco azul na célula
    private boolean tableFocusBorderEnabled = false;    // borda de foco da tabela

    private Color gridLineColor = new Color(0xE0E0E0);
    private int gridLineThickness = 1;

    private int materialRowHeight = 40;
    private Insets cellPadding = new Insets(0, 12, 0, 12);

    private boolean autoAdjustColumns = true;

    // alinhamento por coluna de MODEL: LEFT/CENTER/RIGHT
    private final Map<Integer, Integer> columnAlignments = new HashMap<>();

    // ========= Header Material =========
    private Color headerBg = Color.WHITE;
    private Color headerFg = new Color(0x424242);
    private Color headerDividerColor = new Color(0xE0E0E0);
    private boolean headerShadowEnabled = true;
    private int headerHeightPx = 56;
    private int headerHorzPadding = 16;
    private boolean headerUppercase = false;
    private float headerFontSize = 14f;

    // ========= Coluna de Ações =========
    private static final String ACTIONS_IDENTIFIER = "__actions__";

    // índice de VIEW da coluna de ações. Se <0, auto-detecta por nome.
    private int actionsColumnIndex = -1;
    private boolean autoDetectActionsColumn = true;
    private String actionsColumnHeaderName = "Ações";

    // ícones por botão
    private Icon button1Icon, button2Icon, button3Icon;

    // tamanho do ícone
    private int actionIconSize = 20;

    // cores globais fallback
    private Color buttonIconColor = new Color(0x424242);
    private Color buttonIconColorHover = new Color(0x1E88E5);
    private Color buttonIconColorPressed = new Color(0x0D47A1);

    // cores individuais (se null, usa globais)
    private Color button1IconColor, button1IconColorHover, button1IconColorPressed;
    private Color button2IconColor, button2IconColorHover, button2IconColorPressed;
    private Color button3IconColor, button3IconColorHover, button3IconColorPressed;

    // listeners externos (opcional)
    private ActionListener onAction1, onAction2, onAction3;

    // header popup
    private boolean headerPopupEnabled = true;

    // estado interno
    private int hoveredRow = -1;
    private final JPopupMenu headerMenu = new JPopupMenu();

    // salvar larguras quando ocultar
    private final Map<TableColumn,Integer> savedWidths = new HashMap<>();

    // debug opcional
    private boolean debugActions = false;
    private int syncAttempts = 0;

    // ========= Construtores =========
    public MaterialTable() {
        super();
        setupDefaults();
    }
    public MaterialTable(TableModel dm) {
        super(dm);
        setupDefaults();
    }

    private void setupDefaults() {
        // Modelo demo em design-time
        if (Beans.isDesignTime() && getModel() == null) {
            setModel(new DefaultTableModel(
                new Object[][]{
                    {"Ficha 001","Pago",123.45,"Sim",null},
                    {"Ficha 002","Pendente",99.90,"Não",null},
                    {"Ficha 003","Atrasado",10.00,"Não",null}
                },
                new String[]{"Descrição","Status","Valor","Concluída","Ações"}
            ){
                @Override public Class<?> getColumnClass(int c){ return c==2?Double.class:String.class; }
                @Override public boolean isCellEditable(int r,int c){ return c==4; }
            });
        }

        // aparência base
        setRowHeight(materialRowHeight);
        setShowHorizontalLines(true);
        setShowVerticalLines(false);
        setGridColor(gridLineColor);
        setIntercellSpacing(new Dimension(0, gridLineThickness));
        setFillsViewportHeight(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        super.setSelectionBackground(selectionBackground);
        super.setSelectionForeground(selectionForeground);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setOpaque(true);

        // foco/outline off
        putClientProperty("JComponent.focusWidth", tableFocusBorderEnabled ? 1 : 0);
        putClientProperty("Table.showCellFocusIndicator", Boolean.valueOf(focusCellIndicatorEnabled));
        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder());
        UIManager.put("Table.focusSelectedCellHighlightBorder", BorderFactory.createEmptyBorder());

        // Header Material
        JTableHeader header = getTableHeader();
        if (header != null) {
            header.setReorderingAllowed(true);
            header.setResizingAllowed(true);
            header.setBackground(headerBg);
            header.setForeground(headerFg);
            header.setFont(header.getFont().deriveFont(Font.BOLD, headerFontSize));
            header.setUI(new BasicTableHeaderUI());

            TableColumnModel cm = getColumnModel();
            for (int i = 0; i < cm.getColumnCount(); i++) {
                cm.getColumn(i).setHeaderRenderer(new MaterialHeaderRenderer());
            }
            header.setPreferredSize(new Dimension(header.getPreferredSize().width, headerHeightPx));
            installHeaderPopup(header);
        }

        // hover de linha
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                if (hoverEnabled) {
                    if (row != hoveredRow) {
                        int old = hoveredRow;
                        hoveredRow = row;
                        repaintRows(old, hoveredRow);
                    }
                } else if (hoveredRow != -1) {
                    int old = hoveredRow;
                    hoveredRow = -1;
                    repaintRows(old, -1);
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                if (hoveredRow != -1) {
                    int old = hoveredRow;
                    hoveredRow = -1;
                    repaintRows(old, -1);
                }
            }
        });

        // renderer padrão para dados
        setDefaultRenderer(Object.class, new MaterialCellRenderer());

        // inicial
        SwingUtilities.invokeLater(() -> {
            syncActionsColumnNow();
            applyRenderersAndAlignments();
            if (autoAdjustColumns) autoFitColumns();
        });
    }

    // ========= Robustez contra NetBeans =========
    @Override public void setModel(TableModel dataModel) {
        super.setModel(dataModel);
        SwingUtilities.invokeLater(this::syncActionsColumnNow);
    }
    @Override public void setColumnModel(TableColumnModel columnModel) {
        super.setColumnModel(columnModel);
        SwingUtilities.invokeLater(this::syncActionsColumnNow);
    }
    @Override public void tableChanged(TableModelEvent e) {
        super.tableChanged(e);
        if (e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW || e.getType()==TableModelEvent.INSERT) {
            SwingUtilities.invokeLater(this::syncActionsColumnNow);
        }
    }
    @Override public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::syncActionsColumnNow);
    }

    // ========= Sync/instalação da coluna de ações =========
    private void syncActionsColumnNow() {
        if (getColumnCount()==0) { scheduleRetrySync(); return; }

        int viewIdx = this.actionsColumnIndex;
        if (viewIdx < 0 && autoDetectActionsColumn) {
            viewIdx = findActionsColumnIndex();
            if (debugActions) System.out.println("[MaterialTable] auto-detected actions viewIdx=" + viewIdx);
            if (viewIdx >= 0) this.actionsColumnIndex = viewIdx;
        }
        if (viewIdx < 0 || viewIdx >= getColumnCount()) { scheduleRetrySync(); return; }

        TableColumn tc = getColumnModel().getColumn(viewIdx);
        boolean needInstall = !(tc.getCellRenderer() instanceof MultiButtonCellRenderer)
                           || !(tc.getCellEditor()   instanceof MultiButtonCellEditor)
                           || !ACTIONS_IDENTIFIER.equals(tc.getIdentifier());
        if (needInstall) {
            if (debugActions) System.out.println("[MaterialTable] installing action column at view " + viewIdx);
            installActionColumn(viewIdx);
        }

        ensureActionsColumnWidth(viewIdx);
        applyRenderersAndAlignments();
        if (autoAdjustColumns) autoFitColumns();

        // (re)aplica header material
        JTableHeader h = getTableHeader();
        if (h != null) {
            TableColumnModel cm = getColumnModel();
            for (int i = 0; i < cm.getColumnCount(); i++) {
                if (!(cm.getColumn(i).getHeaderRenderer() instanceof MaterialHeaderRenderer)) {
                    cm.getColumn(i).setHeaderRenderer(new MaterialHeaderRenderer());
                }
            }
            h.setPreferredSize(new Dimension(h.getPreferredSize().width, headerHeightPx));
            h.revalidate(); h.repaint();
        }

        syncAttempts = 0;
    }
    private void scheduleRetrySync() {
        if (++syncAttempts > 8) {
            if (debugActions) System.out.println("[MaterialTable] gave up syncing after 8 attempts.");
            return;
        }
        if (debugActions) System.out.println("[MaterialTable] retry sync, attempt " + syncAttempts);
        SwingUtilities.invokeLater(this::syncActionsColumnNow);
    }
    private int findActionsColumnIndex() {
        if (getColumnCount()==0) return -1;
        String target = normalize(actionsColumnHeaderName);
        for (int v=0; v<getColumnCount(); v++) {
            if (normalize(getColumnName(v)).equals(target)) return v;
        }
        String[] keys = {"acoes","acao","actions","action"};
        for (int v=0; v<getColumnCount(); v++) {
            String n = normalize(getColumnName(v));
            for (String k: keys) if (n.contains(k)) return v;
        }
        return getColumnCount()-1; // fallback
    }
    private static String normalize(String s){
        if (s==null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{InCombiningDiacriticalMarks}+","").toLowerCase().trim();
    }

    private boolean isActionsColumn(int viewCol) {
        if (viewCol<0 || viewCol>=getColumnCount()) return false;
        TableColumn c = getColumnModel().getColumn(viewCol);
        return ACTIONS_IDENTIFIER.equals(c.getIdentifier())
            || c.getCellEditor()   instanceof MultiButtonCellEditor
            || c.getCellRenderer() instanceof MultiButtonCellRenderer
            || viewCol == actionsColumnIndex;
    }

    // ========= Header popup =========
    private void installHeaderPopup(JTableHeader header) {
        header.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }
            private void maybeShow(MouseEvent e) {
                if (!headerPopupEnabled) return;
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    buildHeaderMenu();
                    headerMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
    private void buildHeaderMenu() {
        headerMenu.removeAll();

        JMenuItem auto = new JMenuItem("Auto-ajustar colunas");
        auto.addActionListener(e -> autoFitColumns());
        headerMenu.add(auto);

        JMenu colVis = new JMenu("Mostrar/ocultar colunas");
        TableColumnModel cm = getColumnModel();
        for (int i=0; i<cm.getColumnCount(); i++) {
            final TableColumn c = cm.getColumn(i);
            boolean visible = c.getMaxWidth()!=0;
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(getColumnName(i), visible);
            item.addActionListener(e -> {
                if (item.isSelected()) {
                    int w = savedWidths.getOrDefault(c, Math.max(60, c.getPreferredWidth()));
                    c.setMinWidth(15); c.setMaxWidth(5000); c.setPreferredWidth(w);
                } else {
                    savedWidths.put(c, c.getWidth()>0? c.getWidth(): Math.max(60, c.getPreferredWidth()));
                    c.setMinWidth(0); c.setMaxWidth(0); c.setPreferredWidth(0);
                }
                revalidateAndRepaint();
            });
            colVis.add(item);
        }
        headerMenu.add(colVis);

        JMenuItem fixW = new JMenuItem("Largura fixa (não redimensionar)");
        fixW.addActionListener(e -> setAutoResizeMode(JTable.AUTO_RESIZE_OFF));
        headerMenu.add(fixW);

        JMenuItem normalW = new JMenuItem("Redimensionar subsequentes");
        normalW.addActionListener(e -> setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS));
        headerMenu.add(normalW);
    }

    // ========= Pintura de linhas horizontais contínuas =========
    @Override public void paint(Graphics g) {
        super.paint(g);
        if (getRowCount()==0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(gridLineColor);
        g2.setStroke(new BasicStroke(gridLineThickness));
        Rectangle vr = getVisibleRect();
        for (int r=0; r<getRowCount(); r++) {
            Rectangle rect = getCellRect(r, 0, true);
            int lineY = rect.y + rect.height - gridLineThickness/2;
            if (lineY >= vr.y && lineY <= vr.y + vr.height) {
                g2.drawLine(vr.x, lineY, vr.x + vr.width, lineY);
            }
        }
        g2.dispose();
    }

    // ========= Controle de seleção/cores =========
    @Override public void changeSelection(int row, int col, boolean toggle, boolean extend) {
        if (!selectionEnabled) return;
        super.changeSelection(row, col, toggle, extend);
    }
    @Override public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if ((!selectionEnabled || !selectionColorEnabled) && isRowSelected(row)) {
            if (c instanceof JComponent jc) {
                jc.setBackground(getBackground());
                jc.setForeground(getForeground());
                if (jc instanceof JPanel p) p.setOpaque(true);
            }
        }
        return c;
    }
    @Override public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component comp = super.prepareEditor(editor, row, column);
        if (comp instanceof JComponent jc) {
            jc.putClientProperty("JComponent.outline", "");
            jc.setBorder(BorderFactory.createEmptyBorder());
            if (!selectionColorEnabled || !selectionEnabled) {
                jc.setBackground(getBackground());
                jc.setForeground(getForeground());
            }
        }
        return comp;
    }
    @Override public boolean isCellEditable(int row, int column) {
        if (actionsColumnIndex >= 0 && column == actionsColumnIndex) return true; // editor para clique
        return super.isCellEditable(row, column);
    }

    private void repaintRows(int r1, int r2) {
        if (r1>=0) repaint(getCellRect(r1, 0, true));
        if (r2>=0) repaint(getCellRect(r2, 0, true));
    }

    // ========= Renderers =========
    private class MaterialCellRenderer extends DefaultTableCellRenderer {
        private final JPanel wrapper = new JPanel(new BorderLayout());
        private final JLabel label = new JLabel();
        MaterialCellRenderer() {
            wrapper.setOpaque(true);
            label.setBorder(new EmptyBorder(cellPadding));
            wrapper.add(label, BorderLayout.CENTER);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            label.setText(value == null ? "" : value.toString());
            label.setFont(table.getFont());
            label.setForeground(new Color(0x212121));

            int modelCol = convertColumnIndexToModel(column);
            int align = columnAlignments.getOrDefault(modelCol, SwingConstants.LEFT);
            label.setHorizontalAlignment(
                align == SwingConstants.CENTER ? SwingConstants.CENTER :
                align == SwingConstants.RIGHT  ? SwingConstants.RIGHT  : SwingConstants.LEFT
            );

            Color bg = Color.WHITE;
            if (selectionColorEnabled && selectionEnabled && isSelected) {
                bg = selectionBackground;
                label.setForeground(selectionForeground);
            } else if (hoverEnabled && row == hoveredRow) {
                bg = hoverColor;
            }
            wrapper.setBackground(bg);

            wrapper.setBorder(new EmptyBorder(0,0,0,0));
            label.setBorder(new EmptyBorder(cellPadding));
            return wrapper;
        }
    }

    /** Renderer com 3 botões SEMPRE visíveis. */
    private class MultiButtonCellRenderer extends JPanel implements TableCellRenderer {
        private final JButton b1 = mkButton();
        private final JButton b2 = mkButton();
        private final JButton b3 = mkButton();
        MultiButtonCellRenderer() {
            super(new GridBagLayout());
            setOpaque(true);
            setBorder(new EmptyBorder(0,8,0,8));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy=0; gbc.gridx=GridBagConstraints.RELATIVE; gbc.anchor=GridBagConstraints.CENTER;
            gbc.insets=new Insets(0, 6, 0, 6);
            add(b1, gbc); add(b2, gbc); add(b3, gbc);
        }
        private JButton mkButton() {
            JButton b = new JButton();
            b.setBorderPainted(false);
            b.setContentAreaFilled(true);
            b.setFocusPainted(false);
            b.setOpaque(true);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setBorder(BorderFactory.createEmptyBorder(0,6,0,6));
            b.setBackground(new Color(245,245,245));
            updateButtonHeight(b);
            return b;
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean isSelected, boolean hasFocus, int row, int col) {
            Color bg = t.getBackground();
            if (selectionColorEnabled && selectionEnabled && isSelected) bg = selectionBackground;
            else if (hoverEnabled && row == hoveredRow) bg = hoverColor;
            setBackground(bg);

            b1.setIcon(recolor(button1Icon, effectiveColor(1, "normal")));
            b2.setIcon(recolor(button2Icon, effectiveColor(2, "normal")));
            b3.setIcon(recolor(button3Icon, effectiveColor(3, "normal")));

            installRollover(b1, 1);
            installRollover(b2, 2);
            installRollover(b3, 3);

            updateButtonHeight(b1); updateButtonHeight(b2); updateButtonHeight(b3);
            return this;
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (gridLineThickness>0) {
                g.setColor(gridLineColor);
                g.fillRect(0, getHeight()-gridLineThickness, getWidth(), gridLineThickness);
            }
        }
        private void installRollover(final JButton b, int idx) {
            for (MouseListener ml : b.getMouseListeners()) {
                if (ml.getClass().getName().contains("ActionsRollover")) return;
            }
            b.addMouseListener(new MouseAdapter(){
                @Override public void mouseEntered(MouseEvent e){ b.setBackground(new Color(224,224,224)); b.setIcon(recolor(getIconByIndex(idx), effectiveColor(idx, "hover"))); }
                @Override public void mouseExited (MouseEvent e){ b.setBackground(new Color(245,245,245)); b.setIcon(recolor(getIconByIndex(idx), effectiveColor(idx, "normal"))); }
                @Override public void mousePressed(MouseEvent e){ b.setIcon(recolor(getIconByIndex(idx), effectiveColor(idx, "pressed"))); }
                @Override public void mouseReleased(MouseEvent e){ b.setIcon(recolor(getIconByIndex(idx), effectiveColor(idx, "hover"))); }
            });
        }
    }

    /** Editor apenas para captar o clique e disparar ActionListener, mantendo a mesma aparência. */
    private class MultiButtonCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new GridBagLayout());
        private final JButton b1 = mk(1), b2 = mk(2), b3 = mk(3);

        MultiButtonCellEditor(JTable table) {
            panel.setOpaque(true);
            panel.setBorder(new EmptyBorder(0,8,0,8));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy=0; gbc.gridx=GridBagConstraints.RELATIVE; gbc.anchor=GridBagConstraints.CENTER;
            gbc.insets=new Insets(0,6,0,6);
            panel.add(b1, gbc); panel.add(b2, gbc); panel.add(b3, gbc);
            table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        }

        private JButton mk(int idx) {
            JButton b = new JButton();
            b.setBorderPainted(false);
            b.setContentAreaFilled(true);
            b.setFocusPainted(false);
            b.setOpaque(true);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setBorder(BorderFactory.createEmptyBorder(0,6,0,6));
            b.setBackground(new Color(245,245,245));
            updateButtonHeight(b);

            b.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e){ b.setBackground(new Color(224,224,224)); b.setIcon(recolor(getIconByIndex(idx), effectiveColor(idx, "hover"))); }
                @Override public void mouseExited (MouseEvent e){ b.setBackground(new Color(245,245,245)); b.setIcon(recolor(getIconByIndex(idx), effectiveColor(idx, "normal"))); }
                @Override public void mousePressed(MouseEvent e){ b.setIcon(recolor(getIconByIndex(idx), effectiveColor(idx, "pressed"))); }
                @Override public void mouseReleased(MouseEvent e){ b.setIcon(recolor(getIconByIndex(idx), effectiveColor(idx, "hover"))); }
            });

            b.addActionListener(e -> {
                ActionListener target = switch (idx) {
                    case 1 -> onAction1;
                    case 2 -> onAction2;
                    default -> onAction3;
                };
                if (target != null) {
                    int modelRow = convertRowIndexToModel(getEditingRow());
                    target.actionPerformed(new ActionEvent(MaterialTable.this, modelRow, "action"+idx));
                }
                stopCellEditing();
            });

            b.setIcon(recolor(getIconByIndex(idx), effectiveColor(idx, "normal")));
            return b;
        }

        @Override public Object getCellEditorValue() { return null; }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object value, boolean isSelected, int row, int column) {
            Color bg = t.getBackground();
            if (selectionColorEnabled && selectionEnabled && isSelected) bg = selectionBackground;
            else if (hoverEnabled && row == hoveredRow) bg = hoverColor;
            panel.setBackground(bg);

            b1.setIcon(recolor(button1Icon, effectiveColor(1, "normal")));
            b2.setIcon(recolor(button2Icon, effectiveColor(2, "normal")));
            b3.setIcon(recolor(button3Icon, effectiveColor(3, "normal")));

            updateButtonHeight(b1); updateButtonHeight(b2); updateButtonHeight(b3);
            return panel;
        }
    }

    private void updateButtonHeight(AbstractButton b){
        int rh = getRowHeight();
        int h = Math.max(24, Math.min(rh - 8, 36));
        int minW = actionIconSize + 12;
        b.setPreferredSize(new Dimension(minW, h));
        b.setMinimumSize(new Dimension(minW, h));
    }

    private void installActionColumn(int viewColumnIndex) {
        TableColumn tc = getColumnModel().getColumn(viewColumnIndex);

        // MARCA a coluna para nunca ser sobrescrita
        tc.setIdentifier(ACTIONS_IDENTIFIER);

        // renderer sempre visível + editor para clique
        tc.setCellRenderer(new MultiButtonCellRenderer());
        tc.setCellEditor(new MultiButtonCellEditor(this));

        ensureActionsColumnWidth(viewColumnIndex);

        int modelIndex = convertColumnIndexToModel(viewColumnIndex);
        setColumnAlignment(modelIndex, SwingConstants.CENTER);
        revalidateAndRepaint();
    }
    private void ensureActionsColumnWidth(int viewCol) {
        if (viewCol < 0 || viewCol >= getColumnCount()) return;
        TableColumn tc = getColumnModel().getColumn(viewCol);
        int ideal = 3 * (actionIconSize + 12) + 2*6 + 8; // 3 botões com padding + gaps
        tc.setMinWidth(72);
        tc.setPreferredWidth(Math.max(ideal, tc.getPreferredWidth()));
        tc.setMaxWidth(Math.max(ideal + 200, tc.getMaxWidth()));
    }

    // ========= Header Renderer Material =========
    private class MaterialHeaderRenderer extends DefaultTableCellRenderer {
        MaterialHeaderRenderer(){
            setHorizontalAlignment(LEFT);
            setOpaque(false); // pintamos manualmente
            setBorder(new EmptyBorder(0, headerHorzPadding, 0, headerHorzPadding));
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            String text = value == null ? "" : value.toString();
            if (headerUppercase) text = text.toUpperCase();
            super.getTableCellRendererComponent(table, text, false, false, row, col);
            setForeground(headerFg);
            setFont(getFont().deriveFont(Font.BOLD, headerFontSize));
            setBorder(new EmptyBorder(0, headerHorzPadding, 0, headerHorzPadding));
            return this;
        }
        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();

            // Fundo
            g2.setColor(headerBg);
            g2.fillRect(0, 0, w, h);

            // Sombra (elevation)
            if (headerShadowEnabled) {
                Paint old = g2.getPaint();
                g2.setPaint(new GradientPaint(0, h - 6, new Color(0,0,0,35), 0, h, new Color(0,0,0,0)));
                g2.fillRect(0, h - 6, w, 6);
                g2.setPaint(old);
            }

            // Divider 2dp
            g2.setColor(headerDividerColor);
            g2.fillRect(0, h - 2, w, 2);

            // Texto
            super.paintComponent(g2);

            // Ícone de ordenação (▲/▼) à direita
            RowSorter<? extends TableModel> sorter = getRowSorter();
            if (sorter != null) {
                int modelCol = convertColumnIndexToModel(getHeaderColumnIndex());
                java.util.List<? extends RowSorter.SortKey> keys = sorter.getSortKeys();
                if (keys != null && !keys.isEmpty() && keys.get(0).getColumn() == modelCol) {
                    boolean asc = keys.get(0).getSortOrder() == SortOrder.ASCENDING;
                    String arrow = asc ? "▲" : "▼";
                    g2.setFont(getFont().deriveFont(Font.BOLD));
                    g2.setColor(new Color(0x9E9E9E));
                    FontMetrics fm = g2.getFontMetrics();
                    int aw = fm.stringWidth(arrow);
                    int ax = w - headerHorzPadding - aw;
                    int ay = (h + fm.getAscent() - fm.getDescent()) / 2 - 1;
                    g2.drawString(arrow, ax, ay);
                }
            }
            g2.dispose();
        }
        private int getHeaderColumnIndex() {
            if (getParent() instanceof JTableHeader header) {
                TableColumnModel cm = header.getColumnModel();
                for (int i = 0; i < cm.getColumnCount(); i++) {
                    if (cm.getColumn(i).getHeaderRenderer() == this) return i;
                }
            }
            return -1;
        }
    }

    // ========= Utilidades de ícone/cores =========
    private Icon recolor(Icon icon, Color color) {
        if (icon == null || color == null) return icon;
        int w = icon.getIconWidth(), h = icon.getIconHeight();
        if (w<=0 || h<=0) return icon;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        icon.paintIcon(this, g2, 0, 0);
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.setColor(color);
        g2.fillRect(0, 0, w, h);
        g2.dispose();
        return new ImageIcon(img);
    }
    private Icon normalizeIcon(Icon src, int target) {
        int w = src.getIconWidth(), h = src.getIconHeight();
        if (w <= 0 || h <= 0) return src;
        double scale = Math.min((double) target / w, (double) target / h);
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (src instanceof ImageIcon ii) g.drawImage(ii.getImage(), 0, 0, nw, nh, null);
        else {
            BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g0 = tmp.createGraphics(); src.paintIcon(this, g0, 0, 0); g0.dispose();
            g.drawImage(tmp, 0, 0, nw, nh, null);
        }
        g.dispose();
        return new ImageIcon(out);
    }
    private Color effectiveColor(int idx, String state) {
        Color fb = switch (state) { case "hover" -> buttonIconColorHover; case "pressed" -> buttonIconColorPressed; default -> buttonIconColor; };
        return switch (idx) {
            case 1 -> state.equals("hover") ? nn(button1IconColorHover, fb)
                 : state.equals("pressed") ? nn(button1IconColorPressed, fb)
                 : nn(button1IconColor, fb);
            case 2 -> state.equals("hover") ? nn(button2IconColorHover, fb)
                 : state.equals("pressed") ? nn(button2IconColorPressed, fb)
                 : nn(button2IconColor, fb);
            default -> state.equals("hover") ? nn(button3IconColorHover, fb)
                 : state.equals("pressed") ? nn(button3IconColorPressed, fb)
                 : nn(button3IconColor, fb);
        };
    }
    private static <T> T nn(T v, T fb){ return v!=null? v: fb; }

    // ========= Aplicar renderers nas demais colunas (sem mexer na de ações) =========
    private void applyRenderersAndAlignments() {
        TableColumnModel cm = getColumnModel();
        for (int v=0; v<cm.getColumnCount(); v++) {
            if (isActionsColumn(v)) {
                int m = convertColumnIndexToModel(v);
                Integer align = columnAlignments.get(m);
                if (align != null) setColumnAlignment(m, align);
                continue;
            }
            int m = convertColumnIndexToModel(v);
            TableColumn tc = cm.getColumn(v);
            if (!(tc.getCellRenderer() instanceof MaterialCellRenderer)) {
                tc.setCellRenderer(new MaterialCellRenderer());
            }
            Integer align = columnAlignments.get(m);
            if (align != null) setColumnAlignment(m, align);
        }
        revalidateAndRepaint();
    }

    // ========= Auto-ajuste =========
    public void autoFitColumns() {
        final int margin = 26;
        TableColumnModel colModel = getColumnModel();
        for (int col=0; col<getColumnCount(); col++) {
            int w = 50;
            TableCellRenderer headerRenderer = getTableHeader()!=null ? getTableHeader().getDefaultRenderer() : null;
            if (headerRenderer != null) {
                Component comp = headerRenderer.getTableCellRendererComponent(this, getColumnName(col), false, false, -1, col);
                w = Math.max(w, comp.getPreferredSize().width + margin);
            }
            for (int row=0; row<getRowCount(); row++) {
                TableCellRenderer r = getCellRenderer(row, col);
                Component comp = prepareRenderer(r, row, col);
                w = Math.max(w, comp.getPreferredSize().width + margin);
            }
            colModel.getColumn(col).setPreferredWidth(w);
        }
        revalidateAndRepaint();
    }
    private void revalidateAndRepaint() {
        revalidate();
        repaint();
        firePropertyChange("uiRefresh", false, true);
    }

    // ========= Propriedades públicas =========
    public boolean isHoverEnabled(){ return hoverEnabled; }
    public void setHoverEnabled(boolean b){ boolean old=hoverEnabled; hoverEnabled=b; firePropertyChange("hoverEnabled",old,b); repaint(); }

    public Color getHoverColor(){ return hoverColor; }
    public void setHoverColor(Color c){ Color old=hoverColor; hoverColor=c; firePropertyChange("hoverColor",old,c); repaint(); }

    public boolean isSelectionEnabled(){ return selectionEnabled; }
    public void setSelectionEnabled(boolean enabled){ boolean old=selectionEnabled; selectionEnabled=enabled; if(!enabled) clearSelection(); firePropertyChange("selectionEnabled",old,enabled); repaint(); }

    public boolean isSelectionColorEnabled(){ return selectionColorEnabled; }
    public void setSelectionColorEnabled(boolean b){
        boolean old=selectionColorEnabled; selectionColorEnabled=b;
        if (b){ super.setSelectionBackground(selectionBackground); super.setSelectionForeground(selectionForeground); }
        else   { super.setSelectionBackground(getBackground());    super.setSelectionForeground(getForeground()); }
        firePropertyChange("selectionColorEnabled",old,b); repaint();
    }

    public Color getSelectionBackgroundMaterial(){ return selectionBackground; }
    public void setSelectionBackgroundMaterial(Color c){ Color old=selectionBackground; selectionBackground=c; if(selectionColorEnabled) super.setSelectionBackground(c); firePropertyChange("selectionBackgroundMaterial",old,c); repaint(); }

    public Color getSelectionForegroundMaterial(){ return selectionForeground; }
    public void setSelectionForegroundMaterial(Color c){ Color old=selectionForeground; selectionForeground=c; if(selectionColorEnabled) super.setSelectionForeground(c); firePropertyChange("selectionForegroundMaterial",old,c); repaint(); }

    public boolean isFocusCellIndicatorEnabled(){ return focusCellIndicatorEnabled; }
    public void setFocusCellIndicatorEnabled(boolean enabled){ boolean old=focusCellIndicatorEnabled; focusCellIndicatorEnabled=enabled; putClientProperty("Table.showCellFocusIndicator", Boolean.valueOf(enabled)); firePropertyChange("focusCellIndicatorEnabled",old,enabled); repaint(); }

    public boolean isTableFocusBorderEnabled(){ return tableFocusBorderEnabled; }
    public void setTableFocusBorderEnabled(boolean enabled){ boolean old=tableFocusBorderEnabled; tableFocusBorderEnabled=enabled; putClientProperty("JComponent.focusWidth", enabled?1:0); firePropertyChange("tableFocusBorderEnabled",old,enabled); repaint(); }

    public Color getGridLineColor(){ return gridLineColor; }
    public void setGridLineColor(Color c){ Color old=gridLineColor; gridLineColor=c; setGridColor(c); firePropertyChange("gridLineColor",old,c); repaint(); }

    public int getGridLineThickness(){ return gridLineThickness; }
    public void setGridLineThickness(int t){ int old=gridLineThickness; gridLineThickness=Math.max(1,t); setIntercellSpacing(new Dimension(0, gridLineThickness)); firePropertyChange("gridLineThickness",old,gridLineThickness); repaint(); }

    public int getMaterialRowHeight(){ return materialRowHeight; }
    public void setMaterialRowHeight(int h){ int old=materialRowHeight; materialRowHeight=Math.max(24,h); setRowHeight(materialRowHeight); firePropertyChange("materialRowHeight",old,materialRowHeight); revalidateAndRepaint(); }

    public Insets getCellPadding(){ return cellPadding; }
    public void setCellPadding(Insets p){ Insets old=cellPadding; cellPadding=(p==null? new Insets(0,12,0,12): p); applyRenderersAndAlignments(); firePropertyChange("cellPadding",old,cellPadding); }

    public boolean isAutoAdjustColumns(){ return autoAdjustColumns; }
    public void setAutoAdjustColumns(boolean b){ boolean old=autoAdjustColumns; autoAdjustColumns=b; firePropertyChange("autoAdjustColumns",old,b); if (b) SwingUtilities.invokeLater(this::autoFitColumns); }

    public boolean isHeaderPopupEnabled(){ return headerPopupEnabled; }
    public void setHeaderPopupEnabled(boolean b){ boolean old=headerPopupEnabled; headerPopupEnabled=b; firePropertyChange("headerPopupEnabled",old,b); }

    // Header Material props
    public Color getHeaderBg(){ return headerBg; }
    public void setHeaderBg(Color c){ Color old=headerBg; headerBg=c; if(getTableHeader()!=null) getTableHeader().repaint(); firePropertyChange("headerBg",old,c); }

    public Color getHeaderFg(){ return headerFg; }
    public void setHeaderFg(Color c){ Color old=headerFg; headerFg=c; if(getTableHeader()!=null) getTableHeader().repaint(); firePropertyChange("headerFg",old,c); }

    public Color getHeaderDividerColor(){ return headerDividerColor; }
    public void setHeaderDividerColor(Color c){ Color old=headerDividerColor; headerDividerColor=c; if(getTableHeader()!=null) getTableHeader().repaint(); firePropertyChange("headerDividerColor",old,c); }

    public boolean isHeaderShadowEnabled(){ return headerShadowEnabled; }
    public void setHeaderShadowEnabled(boolean b){ boolean old=headerShadowEnabled; headerShadowEnabled=b; if(getTableHeader()!=null) getTableHeader().repaint(); firePropertyChange("headerShadowEnabled",old,b); }

    public int getHeaderHeightPx(){ return headerHeightPx; }
    public void setHeaderHeightPx(int h){
        int old=headerHeightPx; headerHeightPx=Math.max(32,h);
        JTableHeader th=getTableHeader();
        if(th!=null){ th.setPreferredSize(new Dimension(th.getPreferredSize().width, headerHeightPx)); th.revalidate(); th.repaint(); }
        firePropertyChange("headerHeightPx",old,headerHeightPx);
    }

    public int getHeaderHorzPadding(){ return headerHorzPadding; }
    public void setHeaderHorzPadding(int p){ int old=headerHorzPadding; headerHorzPadding=Math.max(0,p); if(getTableHeader()!=null) getTableHeader().repaint(); firePropertyChange("headerHorzPadding",old,headerHorzPadding); }

    public boolean isHeaderUppercase(){ return headerUppercase; }
    public void setHeaderUppercase(boolean b){ boolean old=headerUppercase; headerUppercase=b; if(getTableHeader()!=null) getTableHeader().repaint(); firePropertyChange("headerUppercase",old,b); }

    public float getHeaderFontSize(){ return headerFontSize; }
    public void setHeaderFontSize(float f){ float old=headerFontSize; headerFontSize=Math.max(10f,f); JTableHeader th=getTableHeader(); if(th!=null) th.repaint(); firePropertyChange("headerFontSize",old,headerFontSize); }

    public int getActionsColumnIndex(){ return actionsColumnIndex; }
    public void setActionsColumnIndex(int viewIndex){ int old=actionsColumnIndex; actionsColumnIndex=viewIndex; SwingUtilities.invokeLater(this::syncActionsColumnNow); firePropertyChange("actionsColumnIndex",old,viewIndex); }

    public boolean isAutoDetectActionsColumn(){ return autoDetectActionsColumn; }
    public void setAutoDetectActionsColumn(boolean b){ boolean old=autoDetectActionsColumn; autoDetectActionsColumn=b; firePropertyChange("autoDetectActionsColumn",old,b); SwingUtilities.invokeLater(this::syncActionsColumnNow); }

    public String getActionsColumnHeaderName(){ return actionsColumnHeaderName; }
    public void setActionsColumnHeaderName(String s){ String old=actionsColumnHeaderName; actionsColumnHeaderName=(s==null?"Ações":s); firePropertyChange("actionsColumnHeaderName",old,actionsColumnHeaderName); SwingUtilities.invokeLater(this::syncActionsColumnNow); }

    public boolean isDebugActions(){ return debugActions; }
    public void setDebugActions(boolean b){ debugActions=b; }

    public Icon getButton1Icon(){ return button1Icon; }
    public void setButton1Icon(Icon icon){ Icon old=button1Icon; button1Icon=(icon!=null)? normalizeIcon(icon, actionIconSize): null; firePropertyChange("button1Icon",old,button1Icon); repaint(); }
    public Icon getButton2Icon(){ return button2Icon; }
    public void setButton2Icon(Icon icon){ Icon old=button2Icon; button2Icon=(icon!=null)? normalizeIcon(icon, actionIconSize): null; firePropertyChange("button2Icon",old,button2Icon); repaint(); }
    public Icon getButton3Icon(){ return button3Icon; }
    public void setButton3Icon(Icon icon){ Icon old=button3Icon; button3Icon=(icon!=null)? normalizeIcon(icon, actionIconSize): null; firePropertyChange("button3Icon",old,button3Icon); repaint(); }

    public int getActionIconSize(){ return actionIconSize; }
    public void setActionIconSize(int s){
        int old=actionIconSize;
        actionIconSize=Math.max(12, Math.min(32, s));
        if (button1Icon!=null) button1Icon=normalizeIcon(button1Icon, actionIconSize);
        if (button2Icon!=null) button2Icon=normalizeIcon(button2Icon, actionIconSize);
        if (button3Icon!=null) button3Icon=normalizeIcon(button3Icon, actionIconSize);
        firePropertyChange("actionIconSize",old,actionIconSize);
        repaint();
    }

    // cores globais
    public Color getButtonIconColor(){ return buttonIconColor; }
    public void setButtonIconColor(Color c){ Color old=buttonIconColor; buttonIconColor=c; firePropertyChange("buttonIconColor",old,c); repaint(); }
    public Color getButtonIconColorHover(){ return buttonIconColorHover; }
    public void setButtonIconColorHover(Color c){ Color old=buttonIconColorHover; buttonIconColorHover=c; firePropertyChange("buttonIconColorHover",old,c); repaint(); }
    public Color getButtonIconColorPressed(){ return buttonIconColorPressed; }
    public void setButtonIconColorPressed(Color c){ Color old=buttonIconColorPressed; buttonIconColorPressed=c; firePropertyChange("buttonIconColorPressed",old,c); repaint(); }

    // cores individuais
    public Color getButton1IconColor(){ return button1IconColor; }
    public void setButton1IconColor(Color c){ Color old=button1IconColor; button1IconColor=c; firePropertyChange("button1IconColor",old,c); repaint(); }
    public Color getButton1IconColorHover(){ return button1IconColorHover; }
    public void setButton1IconColorHover(Color c){ Color old=button1IconColorHover; button1IconColorHover=c; firePropertyChange("button1IconColorHover",old,c); repaint(); }
    public Color getButton1IconColorPressed(){ return button1IconColorPressed; }
    public void setButton1IconColorPressed(Color c){ Color old=button1IconColorPressed; button1IconColorPressed=c; firePropertyChange("button1IconColorPressed",old,c); repaint(); }

    public Color getButton2IconColor(){ return button2IconColor; }
    public void setButton2IconColor(Color c){ Color old=button2IconColor; button2IconColor=c; firePropertyChange("button2IconColor",old,c); repaint(); }
    public Color getButton2IconColorHover(){ return button2IconColorHover; }
    public void setButton2IconColorHover(Color c){ Color old=button2IconColorHover; button2IconColorHover=c; firePropertyChange("button2IconColorHover",old,c); repaint(); }
    public Color getButton2IconColorPressed(){ return button2IconColorPressed; }
    public void setButton2IconColorPressed(Color c){ Color old=button2IconColorPressed; button2IconColorPressed=c; firePropertyChange("button2IconColorPressed",old,c); repaint(); }

    public Color getButton3IconColor(){ return button3IconColor; }
    public void setButton3IconColor(Color c){ Color old=button3IconColor; button3IconColor=c; firePropertyChange("button3IconColor",old,c); repaint(); }
    public Color getButton3IconColorHover(){ return button3IconColorHover; }
    public void setButton3IconColorHover(Color c){ Color old=button3IconColorHover; button3IconColorHover=c; firePropertyChange("button3IconColorHover",old,c); repaint(); }
    public Color getButton3IconColorPressed(){ return button3IconColorPressed; }
    public void setButton3IconColorPressed(Color c){ Color old=button3IconColorPressed; button3IconColorPressed=c; firePropertyChange("button3IconColorPressed",old,c); repaint(); }

    // listeners externos
    public void setOnAction1(ActionListener l){ this.onAction1 = l; }
    public void setOnAction2(ActionListener l){ this.onAction2 = l; }
    public void setOnAction3(ActionListener l){ this.onAction3 = l; }

    // alinhamento por modelo
    public void setColumnAlignment(int modelColumn, int alignment) {
        columnAlignments.put(modelColumn, alignment);
        int view = convertColumnIndexToView(modelColumn);
        if (view >= 0 && !isActionsColumn(view)) {
            getColumnModel().getColumn(view).setCellRenderer(new MaterialCellRenderer());
        }
        revalidateAndRepaint();
    }

    public void setColumnPreferredWidthModel(int modelColumn, int width) {
        int view = convertColumnIndexToView(modelColumn);
        if (view >= 0) {
            TableColumn c = getColumnModel().getColumn(view);
            c.setPreferredWidth(Math.max(20, width));
            revalidateAndRepaint();
        }
    }

    @Override public void updateUI() {
        super.updateUI();
        if (getTableHeader() != null) {
            TableColumnModel cm = getColumnModel();
            for (int i = 0; i < cm.getColumnCount(); i++) {
                cm.getColumn(i).setHeaderRenderer(new MaterialHeaderRenderer());
            }
            getTableHeader().setPreferredSize(new Dimension(getTableHeader().getPreferredSize().width, headerHeightPx));
            getTableHeader().revalidate(); getTableHeader().repaint();
        }
        setFont(getFont().deriveFont(Font.PLAIN, 13f));
        putClientProperty("Table.showCellFocusIndicator", Boolean.FALSE);
        putClientProperty("JComponent.focusWidth", 0);
        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder());
        UIManager.put("Table.focusSelectedCellHighlightBorder", BorderFactory.createEmptyBorder());
    }

    // helpers de ícones
    private Icon getIconByIndex(int idx) {
        return switch (idx) {
            case 1 -> button1Icon;
            case 2 -> button2Icon;
            default -> button3Icon;
        };
    }
}
