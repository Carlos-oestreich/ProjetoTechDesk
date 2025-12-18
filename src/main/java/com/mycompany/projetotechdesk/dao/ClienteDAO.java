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
        if (con == null) return;

        // O erro acontecia porque antes estava enviando 6 coisas (?,?,?,?,?,?)
        // Agora deixamos apenas 5: ID, Nome, CPF, Email, Telefone
        String sql = "CALL sp_atualizar_cliente(?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            // 1. ID do Cliente (Inteiro)
            stmt.setInt(1, cliente.getId());
            
            // 2. Nome (Texto)
            stmt.setString(2, cliente.getNome());
            
            // 3. CPF (Texto)
            stmt.setString(3, cliente.getCpf());
            
            // 4. Email e 5. Telefone (Textos)
            if (cliente.getContato() != null) {
                stmt.setString(4, cliente.getContato().getEmail());
                stmt.setString(5, cliente.getContato().getTelefone());
            } else {
                stmt.setString(4, "");
                stmt.setString(5, "");
            }

            stmt.execute();
            
        } catch (SQLException e) {
            if (e.getMessage().contains("unq_cpf")) {
                throw new SQLException("Este CPF já está em uso por outro cliente!");
            } else {
                throw e; 
            }
        } finally {
            con.close();
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
        
        
        String sql = """
        SELECT 
            c.id, c.nome, c.cpf,
            u.id AS id_usuario,
            ct.id AS id_contato, ct.email, ct.telefone
        FROM tbl_clientes c
        INNER JOIN tbl_usuarios u ON c.id_usuario = u.id
        INNER JOIN tbl_contatos ct ON u.id_contato = ct.id
        WHERE c.id_empresa = ?
        ORDER BY c.nome
    """;
        
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
    
    public Cliente buscarPorId(int idCliente) throws SQLException {
        con = Conexao.getConexao();
        if (con == null) return null;

        String sql = """
        SELECT 
            c.id, c.nome, c.cpf,
            u.id AS id_usuario,
            ct.id AS id_contato, ct.email, ct.telefone
        FROM tbl_clientes c
        INNER JOIN tbl_usuarios u ON c.id_usuario = u.id
        INNER JOIN tbl_contatos ct ON u.id_contato = ct.id
        WHERE c.id = ?
    """;

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idCliente);
            rs = stmt.executeQuery();

            if (rs.next()) {
                Contato contato = new Contato();
                contato.setId(rs.getInt("id_contato"));
                contato.setEmail(rs.getString("email"));
                contato.setTelefone(rs.getString("telefone"));

                Cliente cliente = new Cliente();
                cliente.setId(rs.getInt("id"));
                cliente.setNome(rs.getString("nome"));
                cliente.setCpf(rs.getString("cpf")); 
                cliente.setContato(contato);

                return cliente;
            }
        } catch (SQLException e) {
            System.out.println("ERRO buscar cliente: " + e.getMessage());
            throw e;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (con != null) con.close();
        }
        return null;
    }

}