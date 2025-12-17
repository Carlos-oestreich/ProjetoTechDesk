/* ================================================================================
PROJETO TECHDESK - SCRIPT DE BANCO DE DADOS 
================================================================================
*/

/*
================================================================================
 PARTE 1: DDL (Criação da Estrutura)
================================================================================
*/

CREATE TABLE tbl_empresas (
    id SERIAL PRIMARY KEY,
    nome_empresa VARCHAR(100) NOT NULL,
    cnpj VARCHAR(20) UNIQUE NOT NULL
);

CREATE TABLE tbl_contatos (
    id SERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    telefone VARCHAR(20)
);

CREATE TABLE tbl_usuarios (
    id SERIAL PRIMARY KEY,
    id_empresa INT NOT NULL REFERENCES tbl_empresas(id) ON DELETE CASCADE,
    id_contato INT NOT NULL REFERENCES tbl_contatos(id) ON DELETE RESTRICT,
    senha VARCHAR(255) NOT NULL,
    nome_usuario VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL -- 'admin', 'tecnico'
);

CREATE TABLE tbl_clientes (
    id SERIAL PRIMARY KEY,
    id_empresa INT NOT NULL REFERENCES tbl_empresas(id) ON DELETE CASCADE,
    id_contato INT REFERENCES tbl_contatos(id) ON DELETE SET NULL,
    nome VARCHAR(100) NOT NULL,
    cpf VARCHAR(20)
);

CREATE TABLE tbl_tecnicos (
    id SERIAL PRIMARY KEY,
    id_empresa INT NOT NULL REFERENCES tbl_empresas(id) ON DELETE CASCADE,
    id_contato INT REFERENCES tbl_contatos(id) ON DELETE SET NULL,
    nome VARCHAR(100) NOT NULL,
    especialidade VARCHAR(100)
);


CREATE TABLE tbl_enderecos (
    id SERIAL PRIMARY KEY,
    id_usuario INT NOT NULL REFERENCES tbl_usuarios(id) ON DELETE CASCADE, -- ALTERADO (ENDEREÇO)

    descricao VARCHAR(50),
    logradouro VARCHAR(255) NOT NULL,
    numero VARCHAR(20),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    estado VARCHAR(50), 
    cep VARCHAR(9)
);

CREATE TABLE tbl_ordens_servico (
    id SERIAL PRIMARY KEY,
    id_empresa INT NOT NULL REFERENCES tbl_empresas(id),
    id_cliente INT NOT NULL REFERENCES tbl_clientes(id),
    id_tecnico INT REFERENCES tbl_tecnicos(id), -- Pode ser NULL
    descricao_problema TEXT,
    status VARCHAR(50) DEFAULT 'Aberta',
    data_entrada DATE DEFAULT CURRENT_DATE,
    data_saida DATE,
    valor_mao_obra DECIMAL(10, 2) DEFAULT 0.00,
    valor_pecas DECIMAL(10, 2) DEFAULT 0.00,
    valor_total DECIMAL(10, 2) DEFAULT 0.00
);

CREATE TABLE tbl_log_auditoria (
    id SERIAL PRIMARY KEY,
    evento VARCHAR(100) NOT NULL,
    id_registro_afetado INT,
    usuario_acao VARCHAR(100),
    data_evento TIMESTAMP NOT NULL DEFAULT now()
);

/*
================================================================================
 PARTE 2: TRIGGERS
================================================================================
*/

-- TRIGGER 1: Calcular valor total da O.S.
CREATE OR REPLACE FUNCTION fn_calcula_total_os()
RETURNS TRIGGER AS $$
BEGIN
    NEW.valor_total := COALESCE(NEW.valor_mao_obra, 0) 
                     + COALESCE(NEW.valor_pecas, 0);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_calcula_total_os
BEFORE INSERT OR UPDATE ON tbl_ordens_servico
FOR EACH ROW EXECUTE FUNCTION fn_calcula_total_os();


-- TRIGGER 2: Log de auditoria ao deletar cliente
CREATE OR REPLACE FUNCTION fn_log_delete_cliente()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO tbl_log_auditoria (
        evento,
        id_registro_afetado,
        usuario_acao,
        data_evento
    )
    VALUES (
        'CLIENTE_DELETADO',
        OLD.id,
        current_user,
        now()
    );

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_log_delete_cliente
AFTER DELETE ON tbl_clientes
FOR EACH ROW EXECUTE FUNCTION fn_log_delete_cliente();


/*
================================================================================
 PARTE 3: PROCEDURES
================================================================================
*/

-- PROCEDURE 1: Registrar empresa + usuário admin + endereço
CREATE OR REPLACE PROCEDURE sp_registrar_empresa_e_usuario(
    p_nome_empresa VARCHAR,
    p_cnpj VARCHAR,
    p_nome_usuario VARCHAR,
    p_email VARCHAR,
    p_senha VARCHAR,
    p_logradouro VARCHAR,
    p_numero VARCHAR,
    p_bairro VARCHAR,
    p_cidade VARCHAR,
    p_estado VARCHAR,
    p_cep VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_empresa INT;
    v_id_contato INT;
    v_id_usuario INT;
BEGIN
    INSERT INTO tbl_contatos (email)
    VALUES (p_email)
    RETURNING id INTO v_id_contato;

    INSERT INTO tbl_empresas (nome_empresa, cnpj)
    VALUES (p_nome_empresa, p_cnpj)
    RETURNING id INTO v_id_empresa;

    INSERT INTO tbl_usuarios (
        id_empresa, id_contato, nome_usuario, senha, role
    )
    VALUES (
        v_id_empresa, v_id_contato, p_nome_usuario, p_senha, 'admin'
    )
    RETURNING id INTO v_id_usuario;

    -- ALTERADO (ENDEREÇO): endereço vinculado somente ao usuário
    INSERT INTO tbl_enderecos (
        id_usuario, descricao, logradouro, numero,
        bairro, cidade, estado, cep
    )
    VALUES (
        v_id_usuario, 'Sede', p_logradouro, p_numero,
        p_bairro, p_cidade, p_estado, p_cep
    );
END;
$$;


-- PROCEDURE 2: Adicionar cliente (sem endereço)
CREATE OR REPLACE PROCEDURE sp_adicionar_cliente(
    p_id_empresa INT,
    p_nome VARCHAR,
    p_cpf VARCHAR,
    p_email VARCHAR,
    p_telefone VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_contato INT;
BEGIN
    INSERT INTO tbl_contatos (email, telefone)
    VALUES (p_email, p_telefone)
    RETURNING id INTO v_id_contato;

    INSERT INTO tbl_clientes (
        id_empresa, id_contato, nome, cpf
    )
    VALUES (
        p_id_empresa, v_id_contato, p_nome, p_cpf
    );
END;
$$;


-- PROCEDURE 3: Adicionar técnico (com usuário/login, sem endereço)
CREATE OR REPLACE PROCEDURE sp_adicionar_tecnico(
    p_id_empresa INT,
    p_nome VARCHAR,
    p_email VARCHAR,
    p_telefone VARCHAR,
    p_especialidade VARCHAR,
    p_senha VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_contato INT;
BEGIN
    INSERT INTO tbl_contatos (email, telefone)
    VALUES (p_email, p_telefone)
    RETURNING id INTO v_id_contato;

    INSERT INTO tbl_tecnicos (
        id_empresa, id_contato, nome, especialidade
    )
    VALUES (
        p_id_empresa, v_id_contato, p_nome, p_especialidade
    );

    INSERT INTO tbl_usuarios (
        id_empresa, id_contato, nome_usuario, senha, role
    )
    VALUES (
        p_id_empresa, v_id_contato, p_nome, p_senha, 'tecnico'
    );
END;
$$;


-- PROCEDURE 4: Atualizar cliente
CREATE OR REPLACE PROCEDURE sp_atualizar_cliente(
    p_id_cliente INT,
    p_id_contato INT,
    p_nome VARCHAR,
    p_cpf VARCHAR,
    p_email VARCHAR,
    p_telefone VARCHAR
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE tbl_clientes
       SET nome = p_nome,
           cpf  = p_cpf
     WHERE id = p_id_cliente;

    UPDATE tbl_contatos
       SET email    = p_email,
           telefone = p_telefone
     WHERE id = p_id_contato;
END;
$$;


-- PROCEDURE 5: Atualizar técnico
CREATE OR REPLACE PROCEDURE sp_atualizar_tecnico(
    p_id_tecnico INT,
    p_id_contato INT,
    p_nome VARCHAR,
    p_especialidade VARCHAR,
    p_email VARCHAR,
    p_telefone VARCHAR,
    p_senha VARCHAR
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE tbl_tecnicos
       SET nome = p_nome,
           especialidade = p_especialidade
     WHERE id = p_id_tecnico;

    UPDATE tbl_contatos
       SET email    = p_email,
           telefone = p_telefone
     WHERE id = p_id_contato;

    IF p_senha IS NOT NULL AND p_senha <> '' THEN
        UPDATE tbl_usuarios
           SET senha = p_senha
         WHERE id_contato = p_id_contato;
    END IF;
END;
$$;

/*
================================================================================
 PARTE 4: FUNCTIONS E VIEWS
================================================================================
*/

-- FUNCTION: Relatório de status das O.S.
CREATE OR REPLACE FUNCTION fn_relatorio_status_os(p_id_empresa INT)
RETURNS TABLE(status_os VARCHAR, quantidade BIGINT)
LANGUAGE sql AS $$
    SELECT status, COUNT(*) AS quantidade
    FROM tbl_ordens_servico
    WHERE id_empresa = p_id_empresa
    GROUP BY status
    ORDER BY status;
$$;


-- VIEW 1: Detalhes dos clientes (sem endereço)
CREATE OR REPLACE VIEW vw_detalhes_clientes AS
SELECT 
    c.id,
    c.id_empresa,
    c.nome,
    ct.id AS id_contato,
    ct.email,
    ct.telefone
FROM tbl_clientes c
LEFT JOIN tbl_contatos ct ON c.id_contato = ct.id;


-- VIEW 2: Detalhes das Ordens de Serviço
CREATE OR REPLACE VIEW vw_detalhes_os AS
SELECT 
    os.id AS id_os,
    os.status,
    os.data_entrada,
    os.data_saida,
    os.descricao_problema,
    os.valor_mao_obra,
    os.valor_pecas,
    os.valor_total,
    os.id_empresa,
    c.id AS id_cliente,
    c.nome AS nome_cliente,
    t.id AS id_tecnico,
    t.nome AS nome_tecnico
FROM tbl_ordens_servico os
JOIN tbl_clientes c ON os.id_cliente = c.id
LEFT JOIN tbl_tecnicos t ON os.id_tecnico = t.id;


-- MATERIALIZED VIEW: Faturamento mensal
CREATE MATERIALIZED VIEW vw_materialized_faturamento_mensal AS
SELECT
    id_empresa,
    date_trunc('month', data_saida) AS mes_faturamento,
    SUM(valor_total) AS faturamento_total
FROM tbl_ordens_servico
WHERE status = 'Concluida'
  AND data_saida IS NOT NULL
GROUP BY id_empresa, mes_faturamento;


/*
================================================================================
 PARTE FINAL: CONSTRAINTS E FUNÇÕES AUXILIARES
================================================================================
*/

-- CPF único
ALTER TABLE tbl_clientes
ADD CONSTRAINT unq_cpf UNIQUE (cpf);

-- Campos financeiros obrigatórios
ALTER TABLE tbl_ordens_servico
ALTER COLUMN valor_mao_obra SET NOT NULL;

ALTER TABLE tbl_ordens_servico
ALTER COLUMN valor_pecas SET NOT NULL;

ALTER TABLE tbl_ordens_servico
ALTER COLUMN valor_total SET NOT NULL;

-- Checks de valores
ALTER TABLE tbl_ordens_servico
ADD CONSTRAINT chk_valor_mao_obra CHECK (valor_mao_obra >= 0);

ALTER TABLE tbl_ordens_servico
ADD CONSTRAINT chk_valor_pecas CHECK (valor_pecas >= 0);

ALTER TABLE tbl_ordens_servico
ADD CONSTRAINT chk_valor_total CHECK (valor_total >= 0);


-- FUNCTION: Contar O.S. abertas por empresa
CREATE OR REPLACE FUNCTION fn_contar_os_abertas(p_id_empresa INT)
RETURNS INT
LANGUAGE plpgsql
AS $$
DECLARE
    v_quantidade INT;
BEGIN
    SELECT COUNT(*)
      INTO v_quantidade
      FROM tbl_ordens_servico
     WHERE id_empresa = p_id_empresa
       AND status NOT IN ('Concluida', 'Cancelada');

    RETURN v_quantidade;
END;
$$;

/*
================================================================================
 PARTE 5: DADOS DE EXEMPLO
================================================================================
*/

-- Empresa + Admin
CALL sp_registrar_empresa_e_usuario(
    'TechDesk Solutions',
    '11.111.111/0001-11',
    'Admin TechDesk',
    'admin@techdesk.com',
    'admin',
    'Rua Sede',
    '100',
    'Centro',
    'Curitiba',
    'Parana - PR',
    '80000-000'
);

-- Clientes
CALL sp_adicionar_cliente(1, 'João da Silva', '123.456.789-00', 'joao@email.com', '(11) 9999-9999');

CALL sp_adicionar_cliente(1, 'Mariana Costa', '111.111.111-11', 'mariana@email.com', '(11) 91000-0001');
CALL sp_adicionar_cliente(1, 'Pedro Alves', '222.222.222-22', 'pedro@email.com', '(11) 91000-0002');
CALL sp_adicionar_cliente(1, 'Juliana Lima', '333.333.333-33', 'juliana@email.com', '(11) 91000-0003');
CALL sp_adicionar_cliente(1, 'Roberto Santos', '444.444.444-44', 'roberto@email.com', '(11) 91000-0004');
CALL sp_adicionar_cliente(1, 'Fernanda Mota', '555.555.555-55', 'fernanda@email.com', '(11) 91000-0005');
CALL sp_adicionar_cliente(1, 'Lucas Pereira', '666.666.666-66', 'lucas@email.com', '(11) 91000-0006');
CALL sp_adicionar_cliente(1, 'Amanda Rocha', '777.777.777-77', 'amanda@email.com', '(11) 91000-0007');
CALL sp_adicionar_cliente(1, 'Bruno Dias', '888.888.888-88', 'bruno@email.com', '(11) 91000-0008');
CALL sp_adicionar_cliente(1, 'Carla Nunes', '999.999.999-99', 'carla@email.com', '(11) 91000-0009');
CALL sp_adicionar_cliente(1, 'Diego Farias', '101.101.101-10', 'diego@email.com', '(11) 91000-0010');
CALL sp_adicionar_cliente(1, 'Elisa Martins', '121.121.121-12', 'elisa@email.com', '(11) 91000-0011');
CALL sp_adicionar_cliente(1, 'Fábio Gomes', '131.131.131-13', 'fabio@email.com', '(11) 91000-0012');
CALL sp_adicionar_cliente(1, 'Gabriela Silva', '141.141.141-14', 'gabriela@email.com', '(11) 91000-0013');
CALL sp_adicionar_cliente(1, 'Hugo Teixeira', '151.151.151-15', 'hugo@email.com', '(11) 91000-0014');
CALL sp_adicionar_cliente(1, 'Isabela Cruz', '161.161.161-16', 'isabela@email.com', '(11) 91000-0015');
CALL sp_adicionar_cliente(1, 'Jorge Amado', '171.171.171-17', 'jorge@email.com', '(11) 91000-0016');
CALL sp_adicionar_cliente(1, 'Kelly Key', '181.181.181-18', 'kelly@email.com', '(11) 91000-0017');
CALL sp_adicionar_cliente(1, 'Leandro Hassum', '191.191.191-19', 'leandro@email.com', '(11) 91000-0018');
CALL sp_adicionar_cliente(1, 'Monica Iozzi', '202.202.202-20', 'monica@email.com', '(11) 91000-0019');
CALL sp_adicionar_cliente(1, 'Nelson Rubens', '212.212.212-21', 'nelson@email.com', '(11) 91000-0020');

-- Técnicos
CALL sp_adicionar_tecnico(1, 'Tech Pedro', 'tech.pedro@techdesk.com', '(11) 98000-0001', 'Hardware', '1234');
CALL sp_adicionar_tecnico(1, 'Tech Maria', 'tech.maria@techdesk.com', '(11) 98000-0002', 'Software', '1234');
CALL sp_adicionar_tecnico(1, 'Tech João', 'tech.joao@techdesk.com', '(11) 98000-0003', 'Redes', '1234');

-- =========================================================
-- ENDEREÇOS
-- ALTERADO (ENDEREÇO): agora vinculados ao USUÁRIO
-- =========================================================

INSERT INTO tbl_enderecos (id_usuario, descricao, logradouro, numero, cidade, estado, cep)
VALUES
(
    (SELECT u.id FROM tbl_usuarios u
     JOIN tbl_tecnicos t ON t.id_contato = u.id_contato
     WHERE t.nome = 'Tech Pedro'),
    'Casa', 'Rua Tech 1', '10', 'São Paulo', 'SP', '05000-000'
);

-- Ordens de Serviço
INSERT INTO tbl_ordens_servico
(id_empresa, id_cliente, id_tecnico, descricao_problema, status, data_entrada, data_saida, valor_mao_obra, valor_pecas)
VALUES
(1, 1, 1, 'Notebook não liga', 'Concluida', '2023-11-01', '2023-11-02', 150.00, 300.00),
(1, 2, 2, 'Formatação', 'Em Andamento', '2023-12-01', NULL, 80.00, 0.00),
(1, 3, NULL, 'Computador lento', 'Aberta', '2023-12-06', NULL, 0.00, 0.00);

-- O.S. Concluídas (Geram Faturamento)
INSERT INTO tbl_ordens_servico (id_empresa, id_cliente, id_tecnico, descricao_problema, status, data_entrada, data_saida, valor_mao_obra, valor_pecas) VALUES
(1, (SELECT id FROM tbl_clientes WHERE nome='Mariana Costa'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Pedro'), 'Troca de Tela', 'Concluida', '2023-11-01', '2023-11-02', 100.00, 200.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Pedro Alves'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Maria'), 'Formatação', 'Concluida', '2023-11-03', '2023-11-04', 80.00, 0.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Juliana Lima'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech João'), 'Limpeza Interna', 'Concluida', '2023-11-05', '2023-11-05', 50.00, 0.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Roberto Santos'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Ana'), 'Troca de Bateria', 'Concluida', '2023-11-06', '2023-11-06', 60.00, 150.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Fernanda Mota'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Lucas'), 'Reparo Placa Mãe', 'Concluida', '2023-11-07', '2023-11-10', 300.00, 50.00);

-- O.S. Em Andamento
INSERT INTO tbl_ordens_servico (id_empresa, id_cliente, id_tecnico, descricao_problema, status, data_entrada, valor_mao_obra, valor_pecas) VALUES
(1, (SELECT id FROM tbl_clientes WHERE nome='Lucas Pereira'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Julia'), 'Não liga', 'Em Andamento', '2023-12-01', 150.00, 0.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Amanda Rocha'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Marcos'), 'Tela Azul', 'Em Andamento', '2023-12-02', 100.00, 0.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Bruno Dias'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Sofia'), 'Teclado falhando', 'Em Andamento', '2023-12-03', 80.00, 120.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Carla Nunes'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Rafael'), 'Upgrade SSD', 'Em Andamento', '2023-12-04', 100.00, 250.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Diego Farias'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Bruna'), 'Instalação Office', 'Em Andamento', '2023-12-05', 120.00, 0.00);

-- O.S. Abertas (Sem Técnico definido ainda)
INSERT INTO tbl_ordens_servico (id_empresa, id_cliente, id_tecnico, descricao_problema, status, data_entrada) VALUES
(1, (SELECT id FROM tbl_clientes WHERE nome='Elisa Martins'), NULL, 'Computador lento', 'Aberta', '2023-12-06'),
(1, (SELECT id FROM tbl_clientes WHERE nome='Fábio Gomes'), NULL, 'Impressora travando', 'Aberta', '2023-12-06'),
(1, (SELECT id FROM tbl_clientes WHERE nome='Gabriela Silva'), NULL, 'Sem internet', 'Aberta', '2023-12-07'),
(1, (SELECT id FROM tbl_clientes WHERE nome='Hugo Teixeira'), NULL, 'Vírus', 'Aberta', '2023-12-07'),
(1, (SELECT id FROM tbl_clientes WHERE nome='Isabela Cruz'), NULL, 'Backup', 'Aberta', '2023-12-08');

-- O.S. Canceladas
INSERT INTO tbl_ordens_servico (id_empresa, id_cliente, id_tecnico, descricao_problema, status, data_entrada) VALUES
(1, (SELECT id FROM tbl_clientes WHERE nome='Jorge Amado'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Pedro'), 'Orçamento recusado', 'Cancelada', '2023-11-01'),
(1, (SELECT id FROM tbl_clientes WHERE nome='Kelly Key'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Maria'), 'Cliente desistiu', 'Cancelada', '2023-11-02'),
(1, (SELECT id FROM tbl_clientes WHERE nome='Leandro Hassum'), NULL, 'Peça indisponível', 'Cancelada', '2023-11-03'),
(1, (SELECT id FROM tbl_clientes WHERE nome='Monica Iozzi'), NULL, 'Sem conserto', 'Cancelada', '2023-11-04'),
(1, (SELECT id FROM tbl_clientes WHERE nome='Nelson Rubens'), NULL, 'Erro cadastro', 'Cancelada', '2023-11-05');

-- REFRESH da Materialized View
REFRESH MATERIALIZED VIEW vw_materialized_faturamento_mensal;

/*
================================================================================
 PARTE 6: ROLES E PERMISSÕES
================================================================================
*/

CREATE ROLE role_admin_app WITH LOGIN PASSWORD 'admin_pass';
GRANT CONNECT ON DATABASE "TechDesk" TO role_admin_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO role_admin_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO role_admin_app;
GRANT EXECUTE ON ALL PROCEDURES IN SCHEMA public TO role_admin_app;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO role_admin_app;

CREATE ROLE role_tecnico_app WITH LOGIN PASSWORD 'tecnico_pass';
GRANT CONNECT ON DATABASE "TechDesk" TO role_tecnico_app;
GRANT USAGE ON SCHEMA public TO role_tecnico_app;
GRANT SELECT ON vw_detalhes_os, vw_detalhes_clientes, tbl_tecnicos TO role_tecnico_app;
GRANT UPDATE ON tbl_ordens_servico TO role_tecnico_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO role_tecnico_app;

-- Permissões finais
GRANT EXECUTE ON FUNCTION fn_contar_os_abertas(INT) TO role_tecnico_app;
GRANT EXECUTE ON FUNCTION fn_contar_os_abertas(INT) TO role_admin_app;

SELECT id, id_empresa, status, valor_total
FROM tbl_ordens_servico;
