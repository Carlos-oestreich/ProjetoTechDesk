/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.projetotechdesk.dao;

import com.mycompany.projetotechdesk.Model.Cliente;
import com.mycompany.projetotechdesk.Model.OrdemServico;
import com.mycompany.projetotechdesk.Model.Tecnico;
import com.mycompany.projetotechdesk.database.Conexao;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author carlo
 */
public class OrdemServicoDAO {
    
    private Connection con;
    
    //adiciona uma nova O.S no banco.
    public void adicionar(OrdemServico os) throws SQLException {
        
        con = Conexao.getConexao();
        
        if (con == null){
            System.out.println("ERRO DAO: Conexao nula ao tentar adicionar OS.");
            return;
        }
        
        String sql = "INSERT INTO tbl_ordens_servico "
                + "(id_empresa, id_cliente, id_tecnico, descricao_problema, status, data_entrada, data_saida, valor_mao_obra, valor_pecas) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        PreparedStatement stmt = null;
        
        try {
            stmt = con.prepareStatement(sql);
            
            stmt.setInt(1, os.getEmpresa().getId());
            stmt.setInt(2, os.getCliente().getId());
            
            //verifica se tem tecnico (pode ser nulo em ordens nova)
            if (os.getTecnico() != null && os.getTecnico().getId() > 0){
                stmt.setInt(3, os.getTecnico().getId());
            }else {
                stmt.setNull(3, java.sql.Types.INTEGER);
            }
            
            stmt.setString(4, os.getDescricaoProblema());
            stmt.setString(5, os.getStatus());
            
            //Converter java.util.Date para java.sql.Date
            stmt.setDate(6, new Date(os.getDataEntrada().getTime()));
            
            if(os.getDataSaida() != null) {
                stmt.setDate(7, new Date(os.getDataSaida().getTime()));
            }else {
                stmt.setNull(7, java.sql.Types.DATE);
            }
            
            stmt.setDouble(8, os.getValorMaoObra());
            stmt.setDouble(9, os.getValorPecas());
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.out.println("ERRO, ao tentar adicionar OS -> " + e.getMessage());
            
        }finally {
            if (stmt != null) stmt.close();
            if (con != null){
                con.close();
                System.out.println("Banco fechado com sucesso (Adicionar OS");
            }
        }
    }
    
    
    //atualizar OS
    public void atualizar(OrdemServico os) throws SQLException {
        
        con = Conexao.getConexao();
        
        if (con == null) {
            System.out.println("ERRO DAO: Conexao nula ao tentar atualizar OS.");
            return;
        }
        
        String sql = "UPDATE tbl_ordens_servico SET "
                + "id_cliente = ?, id_tecnico = ?, descricao_problema = ?, status = ?, "
                + "data_entrada = ?, data_saida = ?, valor_mao_obra = ?, valor_pecas = ? "
                + "WHERE id = ? AND id_empresa = ?";
        
        PreparedStatement stmt = null;
        
        try {
            stmt = con.prepareStatement(sql);
            
            stmt.setInt(1, os.getCliente().getId());
            
            if (os.getTecnico() != null && os.getTecnico().getId() > 0) {
                stmt.setInt(2, os.getTecnico().getId());
            }else {
                stmt.setNull(2, java.sql.Types.INTEGER);
            }
            
            stmt.setString(3, os.getDescricaoProblema());
            stmt.setString(4, os.getStatus());
            stmt.setDate(5, new Date(os.getDataEntrada().getTime()));
            
            if (os.getDataSaida() != null) {
                stmt.setDate(6, new Date(os.getDataSaida().getTime()));
            } else {
                stmt.setNull(6, java.sql.Types.DATE);
            }
            
            stmt.setDouble(7, os.getValorMaoObra());
            stmt.setDouble(8, os.getValorPecas());
            
            //IDs para o WHERE
            stmt.setInt(9, os.getId());
            stmt.setInt(10, os.getEmpresa().getId());
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.out.println("ERRO, ao tentar atualizar OS -> " + e.getMessage());
            
        } finally {
            if (stmt != null) stmt.close();
            if (con != null) {
                con.close();
                System.out.println("Banco fechado com sucesso (Atualizar OS)");
            }
        }
    }
    
    
    //excluir OS
    public void excluir(int idOS, int idEmpresa) throws SQLException {
        
        con = Conexao.getConexao();
        
        if (con == null) {
            System.out.println("ERRO DAO: Conexao nula ao tentar excluir OS.");
            return;
        }
        
        String sql = "DELETE FROM tbl_ordens_servico WHERE id = ? AND id_empresa = ?";
        PreparedStatement stmt = null;
        
        try {
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idOS);
            stmt.setInt(2, idEmpresa);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.out.println("ERRO, ao deletar OS -> " + e.getMessage());
            
        } finally {
            if (stmt != null) stmt.close();
            if (con != null) {
                con.close();
                System.out.println("Banco fechado com sucesso (Deletar OS)");
            }
        }
    }
    
    
    //listar todas as OS
    public List<OrdemServico> listarTabelaOS(int idEmpresa) throws SQLException {
        con = Conexao.getConexao();
        
        if (con == null) {
            System.out.println("ERRO DAO: Conexao nula ao tentar listar OS.");
            return new ArrayList<>();
        }
        
        //usa a VIEW criada no banco
        String sql = "SELECT * FROM vw_detalhes_os WHERE id_empresa = ? ORDER BY data_entrada DESC";
        
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<OrdemServico> lista = new ArrayList<>();
        
        try { 
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idEmpresa);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                OrdemServico os = new OrdemServico();
                os.setId(rs.getInt("id_os"));
                
                
                
                os.setStatus(rs.getString("status"));
                
                os.setDataEntrada(rs.getDate("data_entrada"));
                os.setDataSaida(rs.getDate("data_saida"));
                os.setValorTotal(rs.getDouble("valor_total"));
                
                os.setDescricaoProblema(rs.getString("descricao_problema"));
                os.setValorMaoObra(rs.getDouble("valor_mao_obra"));
                os.setValorPecas(rs.getDouble("valor_pecas"));
                
                //Cliente (apenas nome e ID, vindos da view)
                Cliente c = new Cliente();
                c.setId(rs.getInt("id_cliente"));
                c.setNome(rs.getString("nome_cliente"));
                os.setCliente(c);
                
                
                //tecnico (pode ser nulo)
                int idTecnico = rs.getInt("id_tecnico");
                if (!rs.wasNull()) {
                    Tecnico t = new Tecnico();
                    t.setId(idTecnico);
                    t.setNome(rs.getString("nome_tecnico"));
                    os.setTecnico(t);
                }
                os.setDescricaoProblema(rs.getString("descricao_problema")); 

                // --- ADICIONE ISTO PARA TESTAR ---
                System.out.println("ID OS: " + os.getId());
                System.out.println("Descrição no Banco: " + rs.getString("descricao_problema"));
                lista.add(os);
            }
            
        } catch (SQLException e) {
            System.out.println("ERRO, ao listar OS -> " + e.getMessage());
            
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (con != null) {
                con.close();
                System.out.println("Banco fechado com sucesso (Listar OS)");
            }
        }
        
        return lista;
    }
    
    
    //Busca dados para relatorios (Function).
    public ResultSet carregarRelatorioStatus(int idEmpresa) throws SQLException {
        
        con = Conexao.getConexao();
        
        if (con == null) {
            System.out.println("ERRO DAO: Conexao nula ao tentar relatorio.");
            return null;
        }
        
        String sql = "SELECT * FROM fn_relatorio_status_os(?)";
        
        PreparedStatement stmt = con.prepareStatement(sql);
        stmt.setInt(1, idEmpresa);
        
        return stmt.executeQuery();
    }
    
    //retorna um Array com todos os dados do relatorio
    public double[] carregarDadosRelatorio(int idEmpresa) throws SQLException {
        con = Conexao.getConexao();
        
        //se a conexao falhar, retorna tudo zerado
        if (con == null) {
            return new double[]{0, 0, 0, 0, 0};
        }
        
        double[] resultados = new double[6];
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            //Total Abertas
            String sql1 = "SELECT COUNT(*) FROM tbl_ordens_servico WHERE id_empresa = ? AND status = 'Aberta'";
            stmt = con.prepareStatement(sql1);
            stmt.setInt(1, idEmpresa);
            rs = stmt.executeQuery();
            if (rs.next()) {
                resultados[0] = rs.getInt(1);
            }
            rs.close();
            stmt.close();
            
            //Total Em Andamento
            String sql2 = "SELECT COUNT(*) FROM tbl_ordens_servico WHERE id_empresa = ? AND status = 'Em Andamento'";
            stmt = con.prepareStatement(sql2);
            stmt.setInt(1, idEmpresa);
            rs = stmt.executeQuery();
            if (rs.next()) {
                resultados[1] = rs.getInt(1);
            }
            rs.close();
            stmt.close();
            
            //Total Concluidas
            String sql3 = "SELECT COUNT(*) FROM tbl_ordens_servico WHERE id_empresa = ? AND status = 'Concluida'";
            stmt = con.prepareStatement(sql3);
            stmt.setInt(1, idEmpresa);
            rs = stmt.executeQuery();
            if (rs.next()) {
                resultados[2] = rs.getInt(1);
            }
            rs.close();
            stmt.close();
            
            //Valor Total Faturado (soma das Concluidas)
            String sql4 = "SELECT SUM(valor_total) FROM tbl_ordens_servico WHERE id_empresa = ? AND status = 'Concluida'";
            stmt = con.prepareStatement(sql4);
            stmt.setInt(1, idEmpresa);
            rs = stmt.executeQuery();
            if (rs.next()) {
                resultados[3] = rs.getDouble(1);
            }
            rs.close();
            stmt.close();
            
            //Valor Pedentes
            String sql5 = "SELECT SUM(valor_total) FROM tbl_ordens_servico WHERE id_empresa = ? AND status != 'Concluida' AND status != 'Cancelada'";
            stmt = con.prepareStatement(sql5);
            stmt.setInt(1, idEmpresa);
            rs = stmt.executeQuery();
            if (rs.next()) {
                resultados[4] = rs.getDouble(1);
            }
            rs.close();
            stmt.close();
            
            // Total Canceladas
            String sql6 = "SELECT COUNT(*) FROM tbl_ordens_servico WHERE id_empresa = ? AND status = 'Cancelada'";
            stmt = con.prepareStatement(sql6);
            stmt.setInt(1, idEmpresa);
            rs = stmt.executeQuery();
            if (rs.next()) {
                resultados[5] = rs.getInt(1);
            }
            rs.close();
            stmt.close();

            
            
        } catch (SQLException e) {
            System.out.println("ERRO DAO Relatorio: " + e.getMessage());
            
        } finally {
            if(rs != null) rs.close();
            if(stmt != null) stmt.close();
            if(con != null) {
                con.close();
                System.out.println("Banco fechado (Relatório)");
            }
        }
        
        return resultados;
    }
    
}
