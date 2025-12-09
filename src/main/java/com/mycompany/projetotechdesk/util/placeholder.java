/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.projetotechdesk.util;

import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JPasswordField;

/**
 *
 * @author carlo
 */
public class placeholder {
    
    public static void adicionarPlaceholder(JPasswordField campo, String placeholder) {
        
        campo.setEchoChar((char) 0);
        campo.setText(placeholder);
        campo.setForeground(Color.GRAY);
        
        campo.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                String senhaAtual = new String(campo.getPassword());
                if (senhaAtual.equals(placeholder)) {
                    campo.setText("");
                    campo.setEchoChar('*');
                    campo.setForeground(Color.BLACK);
                }
            }
            
            public void focusLost(FocusEvent e) {
                String senhaAtual = new String(campo.getPassword());
                if (senhaAtual.isEmpty()) {
                    campo.setEchoChar((char) 0);
                    campo.setText(placeholder);
                    campo.setForeground(Color.GRAY);
                }
            }
        });
    }
    
    public static boolean ehVazio(JPasswordField campo, String placeholder) {
        String senha = new String(campo.getPassword());
        return senha.isEmpty() || senha.equals(placeholder);
    }
}
