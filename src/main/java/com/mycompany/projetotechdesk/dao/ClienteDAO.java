package com.mycompany.projetotechdesk.dao;

import com.mycompany.projetotechdesk.Model.Cliente;
import com.mycompany.projetotechdesk.Model.Contato;
import com.mycompany.projetotechdesk.database.Conexao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {
    
    private Connection con;
    
    public void adicionar(Cliente cliente, int idEmpresa) throws SQLException{
        con = Conexao.getConexao();
        if(con == null) return;
        
        // Procedure atualizada para receber apenas 5 parametros (sem endereço)
        // Garanta que você rodou o script SQL que atualiza a procedure sp_adicionar_cliente
        String sql = "CALL sp_adicionar_cliente(?, ?, ?, ?, ?)";
        PreparedStatement stmt = null;
        
        try {
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idEmpresa);
            stmt.setString(2, cliente.getNome());
            stmt.setString(3, cliente.getCpf());
            stmt.setString(4, cliente.getContato().getEmail());
            stmt.setString(5, cliente.getContato().getTelefone());
            
            stmt.execute();
        } catch (SQLException e) {
            System.out.println("ERRO add cliente: " + e.getMessage());
            throw e;
        } finally {
            if(stmt != null) stmt.close();
            if(con != null) con.close();
        }
    }
    
    public void atualizar(Cliente cliente) throws SQLException{
        con = Conexao.getConexao();
        if(con == null) return;
        
        // Procedure atualizada para 6 parametros (sem endereço)
        String sql = "CALL sp_atualizar_cliente(?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = null;
        
        try {
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, cliente.getId());
            stmt.setInt(2, cliente.getContato().getId());
            stmt.setString(3, cliente.getNome());
            stmt.setString(4, cliente.getCpf());
            stmt.setString(5, cliente.getContato().getEmail());
            stmt.setString(6, cliente.getContato().getTelefone());
            
            stmt.execute();
        } catch (SQLException e){
            System.out.println("ERRO upd cliente: " + e.getMessage());
            throw e;
        } finally {
            if(stmt != null) stmt.close();
            if(con != null) con.close();
        }
    }
    
    public void excluir(int idCliente, int idEmpresa) throws SQLException{
        con = Conexao.getConexao();
        if(con == null) return;
        
        try{
            PreparedStatement stmt = con.prepareStatement("DELETE FROM tbl_clientes WHERE id = ? AND id_empresa = ?");
            stmt.setInt(1, idCliente);
            stmt.setInt(2, idEmpresa);
            stmt.executeUpdate();
            stmt.close();
        }catch(SQLException e){
            System.out.println("ERRO del cliente: " + e.getMessage());
            throw e;
        }finally{
            if(con!= null) con.close();
        }
    }
    
    public List<Cliente> listarTodos(int idEmpresa) throws SQLException{
        con = Conexao.getConexao();
        if (con == null) return new ArrayList<>();
        
        // SQL simplificado: busca da VIEW nova que não tem mais endereço
        String sql = "SELECT * FROM vw_detalhes_clientes WHERE id_empresa = ? ORDER BY nome";
        
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Cliente> clientes = new ArrayList<>();
        
        try {
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idEmpresa);
            rs = stmt.executeQuery();
            
            while (rs.next()){
                Contato contato = new Contato();
                contato.setId(rs.getInt("id_contato"));
                contato.setEmail(rs.getString("email"));
                contato.setTelefone(rs.getString("telefone"));
                
                Cliente cliente = new Cliente();
                cliente.setId(rs.getInt("id"));
                cliente.setNome(rs.getString("nome"));
                cliente.setCpf(rs.getString("cpf"));
                cliente.setContato(contato);
                // O endereço NÃO é carregado aqui. Será carregado na tela de clique duplo.
                
                clientes.add(cliente);
            }
        } catch (SQLException e){
            System.out.println("ERRO list cliente: " + e.getMessage());
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (con != null) con.close();
        }
        return clientes;
    }
}