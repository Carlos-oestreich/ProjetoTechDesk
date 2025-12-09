/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.projetotechdesk.dao;

import com.mycompany.projetotechdesk.Model.Contato;
import com.mycompany.projetotechdesk.Model.Empresa;
import com.mycompany.projetotechdesk.Model.Endereco;
import com.mycompany.projetotechdesk.Model.Usuario;
import com.mycompany.projetotechdesk.database.Conexao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author carlo
 */
public class UsuarioDAO {
    
    private Connection con;
    
    
    public Usuario validarLogin(String email, String senha) {
        con = Conexao.getConexao();
        
        if (con == null) {
            System.out.println("ERRO DAO: Conexao nula ao tentar login.");
            return null;
        }
        
        String sql = "SELECT u.id, u.nome_usuario, u.role, u.id_empresa, "
                + " e.nome_empresa, e.cnpj, "
                + "c.id AS id_contato, c.email, c.telefone, "
                + "endr.id AS id_endereco, endr.logradouro, endr.numero, endr.bairro, endr.cidade, endr.estado, endr.cep "
                + "FROM tbl_usuarios u "
                + "JOIN tbl_empresas e ON u.id_empresa = e.id "
                + "JOIN tbl_contatos c ON u.id_contato = c.id "
                + "LEFT JOIN tbl_enderecos endr ON endr.id_usuario = u.id "
                + "WHERE c.email = ? AND u.senha = ?";
        
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Usuario usuario = null;
        
        try {
            stmt = con.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, senha);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                //login valido! monta o objeto.
                
                //cria o contato
                Contato contato = new Contato();
                contato.setId(rs.getInt("id_contato"));
                contato.setEmail(rs.getString("email"));
                contato.setTelefone(rs.getString("telefone"));
                
                //cria empresa
                Empresa empresa = new Empresa();
                empresa.setId(rs.getInt("id_empresa"));
                empresa.setNomeEmpresa(rs.getString("nome_empresa"));
                empresa .setCnpj(rs.getString("cnpj"));
                
                
                //cria o endereco
                Endereco endereco = new Endereco();
                if (rs.getInt("id_endereco") > 0){
                endereco.setId(rs.getInt("id_endereco"));
                endereco.setLogradouro(rs.getString("logradouro"));
                endereco.setNumero(rs.getString("numero"));
                endereco.setBairro(rs.getString("bairro"));
                endereco.setCidade(rs.getString("cidade"));
                endereco.setEstado(rs.getString("estado"));
                endereco.setCep(rs.getString("cep"));
                }
                
                //cria Usuario
                usuario = new Usuario();
                usuario.setId(rs.getInt("id"));
                usuario.setNomeUsuario(rs.getString("nome_usuario"));
                usuario.setRole(rs.getString("role"));
                
                //juntar objetos
                usuario.setEmpresa(empresa);
                usuario.setContato(contato);
                usuario.setEndereco(endereco);    
            }
            
        } catch (SQLException e) {
            System.out.println("ERRO, ao tentar fazer login -> " + e.getMessage());
            
        } finally {
            
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (con != null) {
                    con.close();
                    System.out.println("Banco fechado com sucesso (LOGIN)");
                }
            } catch (SQLException ex) {
                System.out.println("ERRO ao fechar conexao: " + ex.getMessage());
            }
        }
        
        return usuario;
    }
    
    
    //registrar nova empresa e seu admin.
    
    public boolean registrarNovaEmpresaEUsuario(Empresa empresa, Usuario admin){
        
        con = Conexao.getConexao();
        
        if (con == null){
            System.out.println("ERRO DAO: Conexao nula ao registrar empresa.");
            return false;
        }
        
        String sql = "CALL sp_registrar_empresa_e_usuario(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = null;
        
        try {
            stmt = con.prepareStatement(sql);
            
            //empresa
            stmt.setString(1, empresa.getNomeEmpresa());
            stmt.setString(2, empresa.getCnpj());
            
            //usuario e contato
            stmt.setString(3, admin.getNomeUsuario());
            stmt.setString(4, admin.getContato().getEmail());
            stmt.setString(5, admin.getSenha());
            
            //endereco
            stmt.setString(6, admin.getEndereco().getLogradouro());
            stmt.setString(7, admin.getEndereco().getNumero());
            stmt.setString(8, admin.getEndereco().getBairro());
            stmt.setString(9, admin.getEndereco().getCidade());
            stmt.setString(10, admin.getEndereco().getEstado());
            stmt.setString(11, admin.getEndereco().getCep());
            
            stmt.execute();
            return true;
            
        } catch (SQLException e) {
            System.out.println("ERRO, ao registrar empresa -> " + e.getMessage());
            return false;
            
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (con != null) {
                    con.close();
                    System.out.println("Banco fechado com sucesso (Registrar Empresa)");
                }
            } catch (SQLException ex) {
                System.out.println("ERRO ao fechar conexão: " + ex.getMessage());
            }
        }
        
    }
}
