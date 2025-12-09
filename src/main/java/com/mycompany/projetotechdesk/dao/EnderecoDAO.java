package com.mycompany.projetotechdesk.dao;

import com.mycompany.projetotechdesk.Model.Endereco;
import com.mycompany.projetotechdesk.database.Conexao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EnderecoDAO {
    private Connection con;

    // Adicionar Endereço
    public void adicionar(Endereco end, int idDono, String tipoDono) throws SQLException {
        con = Conexao.getConexao();
        if(con == null) return;
        
        // Define qual coluna de ID usar baseada no tipo (CLIENTE ou TECNICO)
        String colunaId = "id_cliente"; // Padrão
        if(tipoDono.equals("TECNICO")) colunaId = "id_tecnico";
        
        String sql = "INSERT INTO tbl_enderecos (" + colunaId + ", descricao, logradouro, numero, bairro, cidade, estado, cep) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idDono);
            stmt.setString(2, end.getDescricao());
            stmt.setString(3, end.getLogradouro());
            stmt.setString(4, end.getNumero());
            stmt.setString(5, end.getBairro());
            stmt.setString(6, end.getCidade());
            stmt.setString(7, end.getEstado());
            stmt.setString(8, end.getCep());
            stmt.execute();
        } finally {
            if(stmt!=null) stmt.close();
            if(con!=null) con.close();
        }
    }

    // Atualizar Endereço
    public void atualizar(Endereco end) throws SQLException {
        con = Conexao.getConexao();
        if(con == null) return;
        
        String sql = "UPDATE tbl_enderecos SET descricao=?, logradouro=?, numero=?, bairro=?, cidade=?, estado=?, cep=? WHERE id=?";
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(sql);
            stmt.setString(1, end.getDescricao());
            stmt.setString(2, end.getLogradouro());
            stmt.setString(3, end.getNumero());
            stmt.setString(4, end.getBairro());
            stmt.setString(5, end.getCidade());
            stmt.setString(6, end.getEstado());
            stmt.setString(7, end.getCep());
            stmt.setInt(8, end.getId());
            stmt.execute();
        } finally {
            if(stmt!=null) stmt.close();
            if(con!=null) con.close();
        }
    }

    // Excluir Endereço
    public void excluir(int id) throws SQLException {
        con = Conexao.getConexao();
        if(con == null) return;
        try {
            PreparedStatement stmt = con.prepareStatement("DELETE FROM tbl_enderecos WHERE id=?");
            stmt.setInt(1, id);
            stmt.executeUpdate();
            stmt.close();
        } finally {
            if(con!=null) con.close();
        }
    }

    // Listar Endereços de uma Pessoa
    public List<Endereco> listarPorDono(int idDono, String tipoDono) throws SQLException {
        con = Conexao.getConexao();
        if(con == null) return new ArrayList<>();
        
        String colunaId = "id_cliente";
        if(tipoDono.equals("TECNICO")) colunaId = "id_tecnico";
        
        List<Endereco> lista = new ArrayList<>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT * FROM tbl_enderecos WHERE " + colunaId + " = ?");
            stmt.setInt(1, idDono);
            rs = stmt.executeQuery();
            while(rs.next()) {
                Endereco e = new Endereco();
                e.setId(rs.getInt("id"));
                e.setDescricao(rs.getString("descricao"));
                e.setLogradouro(rs.getString("logradouro"));
                e.setNumero(rs.getString("numero"));
                e.setBairro(rs.getString("bairro"));
                e.setCidade(rs.getString("cidade"));
                e.setEstado(rs.getString("estado"));
                e.setCep(rs.getString("cep"));
                lista.add(e);
            }
        } finally {
            if(rs!=null) rs.close();
            if(stmt!=null) stmt.close();
            if(con!=null) con.close();
        }
        return lista;
    }
}
