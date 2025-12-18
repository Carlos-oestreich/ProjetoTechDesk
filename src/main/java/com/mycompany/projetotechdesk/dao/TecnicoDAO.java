/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.projetotechdesk.dao;

import com.mycompany.projetotechdesk.Model.Contato;
import com.mycompany.projetotechdesk.Model.Endereco;
import com.mycompany.projetotechdesk.Model.Tecnico;
import com.mycompany.projetotechdesk.database.Conexao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author carlo
 */
public class TecnicoDAO {
    
    private Connection con;
    
    public void adicionar(Tecnico tecnico, String senha, int idEmpresa) throws SQLException{
        con = Conexao.getConexao();
        if (con == null) return;
        
        // Procedure nova (sem endereço)
        String sql = "CALL sp_adicionar_tecnico(?, ?, ?, ?, ?, ? )";
        PreparedStatement stmt = null;
        
        try {
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idEmpresa);
            stmt.setString(2, tecnico.getNome());
            stmt.setString(3, tecnico.getContato().getEmail());
            stmt.setString(4, tecnico.getContato().getTelefone());
            stmt.setString(5, tecnico.getEspecialidade()); 
            stmt.setString(6, senha); // Senha do login
            
            stmt.execute();
        } catch (SQLException e) {
            System.out.println("ERRO add tecnico: " + e.getMessage());
            throw e; 
        } finally {
            if (stmt != null) stmt.close();
            if (con != null) con.close();
        }
            
       
        
    }
    
    
    //atualizar tecnico
    public void atualizar(Tecnico tecnico, String senha) throws SQLException {
        con = Conexao.getConexao();
        if (con == null) throw new SQLException("Sem conexão.");

        // NOTA: Agora são 7 pontos de interrogação!
        // 1:ID, 2:Nome, 3:Login, 4:Senha, 5:Email, 6:Telefone, 7:Especialidade
        String sql = "CALL sp_atualizar_tecnico(?, ?, ?, ?, ?, ?, ?)"; 

        try (PreparedStatement stmt = con.prepareStatement(sql)) {

            // 1 a 6 (Iguais ao que já tínhamos)
            stmt.setInt(1, tecnico.getId());
            stmt.setString(2, tecnico.getNome());
            stmt.setString(3, tecnico.getLogin()); // Lembra que setamos isso como Email no botão

            // Lógica da Senha
            if (senha != null && !senha.trim().isEmpty()) {
                 stmt.setString(4, senha); 
            } else {
                 stmt.setString(4, tecnico.getSenha());
            }

            // Contatos
            if (tecnico.getContato() != null) {
                stmt.setString(5, tecnico.getContato().getEmail());
                stmt.setString(6, tecnico.getContato().getTelefone());
            } else {
                stmt.setNull(5, java.sql.Types.VARCHAR);
                stmt.setNull(6, java.sql.Types.VARCHAR);
            }

            // --- 7. ESPECIALIDADE (NOVO) ---
            stmt.setString(7, tecnico.getEspecialidade()); 
            // -------------------------------

            stmt.execute();

        } catch (SQLException e) {
            if (e.getMessage().contains("unq_usuario_login")) {
                throw new SQLException("Este Login/Email já está em uso!");
            } else {
                throw e; 
            }
        } finally {
            // Feche a conexão se necessário
            if (con != null) con.close();
        }
    }
    
    //exclui um tecnico
    public void excluir(int idTecnico, int idEmpresa) throws SQLException {
        con = Conexao.getConexao();
        if (con == null) return;
        
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // 1. VERIFICA SE TEM O.S. ATIVA (Aberto ou Aberta, Em Andamento)
            // Usamos LIKE 'Abert%' para pegar tanto "Aberto" quanto "Aberta"
            String sqlVerifica = "SELECT COUNT(*) FROM tbl_ordens_servico "
                    + "WHERE id_tecnico = ? AND id_empresa = ? "
                    + "AND (status LIKE 'Aberta' OR status = 'Em andamento')";
            
            stmt = con.prepareStatement(sqlVerifica);
            stmt.setInt(1, idTecnico);
            stmt.setInt(2, idEmpresa);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                int pendencias = rs.getInt(1);
                if (pendencias > 0) {
                    throw new SQLException("Este técnico possui " + pendencias + " O.S. ativas.\n"
                            + "Transfira ou conclua as ordens antes de excluir.");
                }
            }
            rs.close();
            stmt.close();
            
            // 2. DESVINCULAR O.S. ANTIGAS (Concluídas/Canceladas)
            // Precisamos setar o técnico como NULL nessas O.S. para o banco deixar apagar o técnico
            String sqlDesvincular = "UPDATE tbl_ordens_servico SET id_tecnico = NULL WHERE id_tecnico = ?";
            stmt = con.prepareStatement(sqlDesvincular);
            stmt.setInt(1, idTecnico);
            stmt.executeUpdate();
            stmt.close();
            
            // 3. BUSCA O ID DO CONTATO (Para apagar o login)
            String sqlBuscaUser = "SELECT id_usuario FROM tbl_tecnicos WHERE id = ?";
            stmt = con.prepareStatement(sqlBuscaUser);
            stmt.setInt(1, idTecnico);
            rs = stmt.executeQuery();
            
            int idUsuario = 0;
            if (rs.next()) {
                idUsuario = rs.getInt("id_usuario");
            }
            rs.close();
            stmt.close();
            
            // 4. EXCLUI O USUÁRIO DE LOGIN
            if (idUsuario > 0) {
                String sqlDelUser = "DELETE FROM tbl_usuarios WHERE id = ?";
                stmt = con.prepareStatement(sqlDelUser);
                stmt.setInt(1, idUsuario);
                stmt.executeUpdate();
            } else {
                // Caso de segurança: se não achou usuário, tenta apagar só o técnico
                String sqlDelTec = "DELETE FROM tbl_tecnicos WHERE id = ?";
                stmt = con.prepareStatement(sqlDelTec);
                stmt.setInt(1, idTecnico);
                stmt.executeUpdate();
            }

            
            
        } catch (SQLException e){
            System.out.println("ERRO ao deletar tecnico -> " + e.getMessage());
            throw e; // Repassa o erro para a tela
        } finally{
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (con != null) con.close();
        }
    }
    
    
    //Listar todos os tecnicos de uma empresa especifica
    public List<Tecnico> listarTodos(int idEmpresa) throws SQLException{


        con = Conexao.getConexao();

        if (con == null) {
            System.out.println("ERRO DAO: Conexão com o banco nula ao tentar listar técnicos.");
            return new ArrayList<>();
        }

        String sql = """
            SELECT 
                t.id AS tecnico_id, t.nome, t.especialidade,
                u.id AS id_usuario,
                ct.id AS contato_id, ct.email, ct.telefone 
            FROM tbl_tecnicos t 
            INNER JOIN tbl_usuarios u ON t.id_usuario = u.id
            INNER JOIN tbl_contatos ct ON u.id_contato = ct.id 
            WHERE t.id_empresa = ?
        """;

        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Tecnico> tecnicos = new ArrayList<>();

        try {
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idEmpresa);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Contato contato = new Contato();
                contato.setId(rs.getInt("contato_id"));
                contato.setEmail(rs.getString("email"));
                contato.setTelefone(rs.getString("telefone"));

                Tecnico tecnico = new Tecnico();
                tecnico.setId(rs.getInt("tecnico_id"));
                tecnico.setNome(rs.getString("nome"));
                tecnico.setEspecialidade(rs.getString("especialidade"));
                tecnico.setContato(contato);

                tecnicos.add(tecnico);
            }

        } catch (SQLException e) {
            System.out.println("ERRO ao listar técnicos -> " + e.getMessage());
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (con != null) con.close();
        }

        return tecnicos;
    }
    
    
}
