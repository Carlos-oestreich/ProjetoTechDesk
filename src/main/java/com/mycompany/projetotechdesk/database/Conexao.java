/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.projetotechdesk.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author carlo
 */
public class Conexao {
    private static final String URL = "jdbc:postgresql://localhost:5432/TechDesk"; 
    private static final String USUARIO = "postgres";
    private static final String SENHA = "root";
    private static final String DRIVER = "org.postgresql.Driver";
    
    
    private static Connection conexao = null;
    
    public static Connection getConexao(){
        
        try{
            if (conexao == null || conexao.isClosed()){
            Class.forName(DRIVER);

            conexao = DriverManager.getConnection(URL, USUARIO, SENHA);

                System.out.println("Conexao com o banco bem sucedida!");
            }
        }catch (ClassNotFoundException e){
            System.out.println("ERRO driver -> " + e.getMessage());
            e.printStackTrace();
            return null;

        } catch(SQLException e){
        System.out.println("ERRO SQL -> " + e.getMessage());
        e.printStackTrace();
        return null;
        }
        return conexao;
    }
}
