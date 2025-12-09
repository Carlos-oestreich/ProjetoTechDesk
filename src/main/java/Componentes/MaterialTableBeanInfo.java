package Componentes;

import java.awt.Image;
import java.beans.*;
import javax.swing.*;

public class MaterialTableBeanInfo extends SimpleBeanInfo {

    private static final String ICON_16 = "/com/mycompany/logincomcomponentes/componentes/materialtable16.png";
    private static final String ICON_32 = "/com/mycompany/logincomcomponentes/componentes/materialtable32.png";

    @Override
    public BeanDescriptor getBeanDescriptor() {
        BeanDescriptor bd = new BeanDescriptor(MaterialTable.class);
        bd.setDisplayName("MaterialTable");
        bd.setShortDescription("JTable com estilo Material, hover/seleção custom e coluna de ações (3 botões).");
        return bd;
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            // ===== Dados herdados =====
            PropertyDescriptor model = new PropertyDescriptor("model", MaterialTable.class, "getModel", "setModel");
            model.setShortDescription("Modelo de dados da tabela (DefaultTableModel ou customizado).");
            model.setValue("category", "Dados");

            PropertyDescriptor tableHeader = new PropertyDescriptor("tableHeader", MaterialTable.class, "getTableHeader", "setTableHeader");
            tableHeader.setShortDescription("Cabeçalho da tabela (JTableHeader).");
            tableHeader.setValue("category", "Dados"); tableHeader.setExpert(true);

            PropertyDescriptor columnModel = new PropertyDescriptor("columnModel", MaterialTable.class, "getColumnModel", "setColumnModel");
            columnModel.setShortDescription("Modelo de colunas (TableColumnModel).");
            columnModel.setValue("category", "Dados"); columnModel.setExpert(true);

            // ===== Aparência/UX =====
            PropertyDescriptor hoverEnabled = pd("hoverEnabled", "Aparência/UX", "Habilita/desabilita o efeito de hover na linha.");
            PropertyDescriptor hoverColor = pd("hoverColor", "Aparência/UX", "Cor de fundo da linha em hover.");
            PropertyDescriptor selectionEnabled = pd("selectionEnabled", "Aparência/UX", "Quando falso, cliques/teclas não selecionam linhas.");
            PropertyDescriptor selectionColorEnabled = pd("selectionColorEnabled", "Aparência/UX", "Habilita/desabilita cores personalizadas de seleção.");
            PropertyDescriptor selectionBg = pd("selectionBackgroundMaterial", "Aparência/UX", "Cor de fundo da linha selecionada (quando ativo).");
            PropertyDescriptor selectionFg = pd("selectionForegroundMaterial", "Aparência/UX", "Cor do texto da linha selecionada (quando ativo).");
            PropertyDescriptor focusCell = pd("focusCellIndicatorEnabled", "Aparência/UX", "Exibe contorno de foco na célula líder (FlatLaf).");
            PropertyDescriptor tableFocusBorder = pd("tableFocusBorderEnabled", "Aparência/UX", "Exibe borda de foco ao redor da tabela (FlatLaf).");

            // ===== Grade =====
            PropertyDescriptor gridColor = pd("gridLineColor", "Grade", "Cor das linhas horizontais da grade.");
            PropertyDescriptor gridThickness = pd("gridLineThickness", "Grade", "Espessura das linhas horizontais da grade."); gridThickness.setExpert(true);

            // ===== Layout =====
            PropertyDescriptor rowHeight = pd("materialRowHeight", "Layout", "Altura (px) das linhas.");
            PropertyDescriptor cellPadding = pd("cellPadding", "Layout", "Padding interno das células (Insets).");
            PropertyDescriptor autoAdjust = pd("autoAdjustColumns", "Layout", "Quando ativo, ajusta a largura das colunas ao conteúdo.");

            // ===== Interação =====
            PropertyDescriptor headerPopup = pd("headerPopupEnabled", "Interação", "Menu de contexto no cabeçalho (mostrar/ocultar colunas, auto-ajuste...).");
            headerPopup.setExpert(true);

            // ===== Ações (3 botões) – ícones e índice da coluna =====
            PropertyDescriptor actionsIndex = pd("actionsColumnIndex", "Ações (3 botões)", "Índice de VIEW da coluna onde os botões serão exibidos. -1 desativa.");
            PropertyDescriptor btn1 = pd("button1Icon", "Ações (3 botões)", "Ícone do botão 1.");
            PropertyDescriptor btn2 = pd("button2Icon", "Ações (3 botões)", "Ícone do botão 2.");
            PropertyDescriptor btn3 = pd("button3Icon", "Ações (3 botões)", "Ícone do botão 3.");

            // Cores globais (fallback)
            PropertyDescriptor gNormal  = pd("buttonIconColor", "Ações (3 botões) / Globais", "Cor global dos ícones (normal).");
            PropertyDescriptor gHover   = pd("buttonIconColorHover", "Ações (3 botões) / Globais", "Cor global (hover).");
            PropertyDescriptor gPressed = pd("buttonIconColorPressed", "Ações (3 botões) / Globais", "Cor global (pressed).");

            // Cores individuais
            PropertyDescriptor b1N = pd("button1IconColor", "Ações (3 botões) / Botão 1", "Cor do ícone 1 (normal).");
            PropertyDescriptor b1H = pd("button1IconColorHover", "Ações (3 botões) / Botão 1", "Cor do ícone 1 (hover).");
            PropertyDescriptor b1P = pd("button1IconColorPressed", "Ações (3 botões) / Botão 1", "Cor do ícone 1 (pressed).");

            PropertyDescriptor b2N = pd("button2IconColor", "Ações (3 botões) / Botão 2", "Cor do ícone 2 (normal).");
            PropertyDescriptor b2H = pd("button2IconColorHover", "Ações (3 botões) / Botão 2", "Cor do ícone 2 (hover).");
            PropertyDescriptor b2P = pd("button2IconColorPressed", "Ações (3 botões) / Botão 2", "Cor do ícone 2 (pressed).");

            PropertyDescriptor b3N = pd("button3IconColor", "Ações (3 botões) / Botão 3", "Cor do ícone 3 (normal).");
            PropertyDescriptor b3H = pd("button3IconColorHover", "Ações (3 botões) / Botão 3", "Cor do ícone 3 (hover).");
            PropertyDescriptor b3P = pd("button3IconColorPressed", "Ações (3 botões) / Botão 3", "Cor do ícone 3 (pressed).");

            return new PropertyDescriptor[]{
                // Dados
                model, tableHeader, columnModel,

                // Aparência/UX
                hoverEnabled, hoverColor,
                selectionEnabled, selectionColorEnabled, selectionBg, selectionFg,
                focusCell, tableFocusBorder,

                // Grade
                gridColor, gridThickness,

                // Layout
                rowHeight, cellPadding, autoAdjust,

                // Interação
                headerPopup,

                // Ações
                actionsIndex, btn1, btn2, btn3,
                gNormal, gHover, gPressed,
                b1N, b1H, b1P,
                b2N, b2H, b2P,
                b3N, b3H, b3P
            };

        } catch (IntrospectionException e) {
            e.printStackTrace();
            return new PropertyDescriptor[0];
        }
    }

    @Override
    public MethodDescriptor[] getMethodDescriptors() {
        try {
            MethodDescriptor autoFit = md("autoFitColumns", "Layout", "Ajusta automaticamente a largura de todas as colunas ao conteúdo.");
            MethodDescriptor installAction = md("installActionColumn", "Ações (3 botões)", "Instala a coluna de ações (3 botões) pelo índice de VIEW informado.", new Class[]{int.class}, new String[]{"viewColumnIndex"});
            MethodDescriptor ensureActionEnd = md("ensureActionColumnAtEnd", "Ações (3 botões)", "Garante que a última coluna seja a coluna de ações.");
            MethodDescriptor setAlign = md("setColumnAlignment", "Alinhamento", "Define o alinhamento de conteúdo de uma coluna (MODELO) – LEFT/CENTER/RIGHT.", new Class[]{int.class, int.class}, new String[]{"modelColumn", "alignment"});
            MethodDescriptor setWidthModel = md("setColumnPreferredWidthModel", "Layout", "Define a largura preferida (px) de uma coluna pelo índice do MODELO.", new Class[]{int.class, int.class}, new String[]{"modelColumn", "width"});
            return new MethodDescriptor[]{autoFit, installAction, ensureActionEnd, setAlign, setWidthModel};
        } catch (Exception e) {
            e.printStackTrace();
            return new MethodDescriptor[0];
        }
    }

    // ===== utilitários =====
    private static PropertyDescriptor pd(String name, String category, String desc) throws IntrospectionException {
        PropertyDescriptor p = new PropertyDescriptor(name, MaterialTable.class);
        p.setShortDescription(desc);
        p.setValue("category", category);
        return p;
    }

    private static MethodDescriptor md(String name, String category, String desc) throws NoSuchMethodException {
        java.lang.reflect.Method m = MaterialTable.class.getMethod(name);
        MethodDescriptor md = new MethodDescriptor(m);
        md.setShortDescription(desc);
        md.setValue("category", category);
        return md;
    }

    private static MethodDescriptor md(String name, String category, String desc, Class<?>[] params, String[] paramNames) throws NoSuchMethodException {
        java.lang.reflect.Method m = MaterialTable.class.getMethod(name, params);
        ParameterDescriptor[] pds = null;
        if (paramNames != null && paramNames.length == params.length) {
            pds = new ParameterDescriptor[paramNames.length];
            for (int i = 0; i < paramNames.length; i++) {
                ParameterDescriptor pd = new ParameterDescriptor();
                pd.setName(paramNames[i]);
                pds[i] = pd;
            }
        }
        MethodDescriptor md = (pds != null) ? new MethodDescriptor(m, pds) : new MethodDescriptor(m);
        md.setShortDescription(desc);
        md.setValue("category", category);
        return md;
    }

    @Override
    public Image getIcon(int kind) {
        String path = switch (kind) {
            case BeanInfo.ICON_COLOR_16x16, BeanInfo.ICON_MONO_16x16 -> ICON_16;
            case BeanInfo.ICON_COLOR_32x32, BeanInfo.ICON_MONO_32x32 -> ICON_32;
            default -> ICON_16;
        };
        java.net.URL url = getClass().getResource(path);
        return url != null ? new ImageIcon(url).getImage() : null;
    }
}
