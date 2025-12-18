/* ================================================================================
   PROJETO TECHDESK - SCRIPT DE BANCO DE DADOS (REFATORADO - HERANÇA DE USUÁRIO)
   ================================================================================
   OBJETIVO: Centralizar Admin, Técnico e Cliente na tabela tbl_usuarios.
   ESTRUTURA:
     - 1 Usuário tem 1 Contato (Email/Tel)
     - 1 Usuário tem N Endereços
     - Clientes e Técnicos são extensões de Usuários
================================================================================ */

-- 0. LIMPEZA TOTAL (CUIDADO: APAGA TUDO)
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;

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
    telefone VARCHAR(30)
);

-- TABELA PAI (SUPERCLASSE)
-- Centraliza Login, Senha e o Vínculo com Contato
CREATE TABLE tbl_usuarios (
    id SERIAL PRIMARY KEY,
    id_empresa INT NOT NULL REFERENCES tbl_empresas(id) ON DELETE CASCADE,
    id_contato INT NOT NULL REFERENCES tbl_contatos(id) ON DELETE RESTRICT,
    nome_usuario VARCHAR(100) NOT NULL, -- Login ou Nome de exibição
    senha VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL, -- 'admin', 'tecnico', 'cliente'
    CONSTRAINT unq_usuario_contato UNIQUE (id_contato) -- Garante 1 contato por usuário
);

-- TABELA FILHA: CLIENTES
-- Herda de Usuarios. Removemos id_contato daqui e passamos a usar id_usuario.
CREATE TABLE tbl_clientes (
    id SERIAL PRIMARY KEY,
    id_empresa INT NOT NULL REFERENCES tbl_empresas(id) ON DELETE CASCADE,
    id_usuario INT NOT NULL REFERENCES tbl_usuarios(id) ON DELETE CASCADE, -- Vínculo Pai/Filho
    nome VARCHAR(100) NOT NULL, -- Mantido aqui para compatibilidade com sistema legado
    cpf VARCHAR(20) NOT NULL UNIQUE,
    CONSTRAINT unq_cliente_usuario UNIQUE (id_usuario),
    CONSTRAINT unq_cpf UNIQUE (cpf)
);

-- TABELA FILHA: TÉCNICOS
-- Herda de Usuarios.
CREATE TABLE tbl_tecnicos (
    id SERIAL PRIMARY KEY,
    id_empresa INT NOT NULL REFERENCES tbl_empresas(id) ON DELETE CASCADE,
    id_usuario INT NOT NULL REFERENCES tbl_usuarios(id) ON DELETE CASCADE, -- Vínculo Pai/Filho
    nome VARCHAR(100) NOT NULL,
    especialidade VARCHAR(100),
    CONSTRAINT unq_tecnico_usuario UNIQUE (id_usuario)
);

-- ENDEREÇOS (VINCULADOS AO USUÁRIO)
-- Permite que Clientes, Técnicos e Admins tenham múltiplos endereços
CREATE TABLE tbl_enderecos (
    id SERIAL PRIMARY KEY,
    id_usuario INT NOT NULL REFERENCES tbl_usuarios(id) ON DELETE CASCADE,
    descricao VARCHAR(50), -- Ex: 'Casa', 'Trabalho', 'Cobrança'
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
    valor_mao_obra DECIMAL(10, 2) DEFAULT 0.00 CHECK (valor_mao_obra >= 0),
    valor_pecas DECIMAL(10, 2) DEFAULT 0.00 CHECK (valor_pecas >= 0),
    valor_total DECIMAL(10, 2) DEFAULT 0.00 CHECK (valor_total >= 0)
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
    INSERT INTO tbl_log_auditoria (evento, id_registro_afetado, usuario_acao, data_evento)
    VALUES ('CLIENTE_DELETADO', OLD.id, current_user, now());
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_log_delete_cliente
AFTER DELETE ON tbl_clientes
FOR EACH ROW EXECUTE FUNCTION fn_log_delete_cliente();

/*
================================================================================
 PARTE 3: PROCEDURES (ADAPTADAS PARA A NOVA ESTRUTURA)
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
    -- 1. Contato
    INSERT INTO tbl_contatos (email) VALUES (p_email) RETURNING id INTO v_id_contato;

    -- 2. Empresa
    INSERT INTO tbl_empresas (nome_empresa, cnpj) VALUES (p_nome_empresa, p_cnpj) RETURNING id INTO v_id_empresa;

    -- 3. Usuário Admin
    INSERT INTO tbl_usuarios (id_empresa, id_contato, nome_usuario, senha, role)
    VALUES (v_id_empresa, v_id_contato, p_nome_usuario, p_senha, 'admin')
    RETURNING id INTO v_id_usuario;

    -- 4. Endereço do Admin/Sede
    INSERT INTO tbl_enderecos (id_usuario, descricao, logradouro, numero, bairro, cidade, estado, cep)
    VALUES (v_id_usuario, 'Sede', p_logradouro, p_numero, p_bairro, p_cidade, p_estado, p_cep);
END;
$$;


-- PROCEDURE 2: Adicionar cliente (Cria Contato -> Usuário -> Cliente)
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
    v_id_usuario INT;
BEGIN
    -- 1. Contato
    INSERT INTO tbl_contatos (email, telefone) VALUES (p_email, p_telefone) RETURNING id INTO v_id_contato;

    -- 2. Usuário (Role Cliente) - Senha padrão definida aqui
    INSERT INTO tbl_usuarios (id_empresa, id_contato, nome_usuario, senha, role)
    VALUES (p_id_empresa, v_id_contato, p_nome, 'cliente123', 'cliente') 
    RETURNING id INTO v_id_usuario;

    -- 3. Cliente (Dados específicos)
    INSERT INTO tbl_clientes (id_empresa, id_usuario, nome, cpf)
    VALUES (p_id_empresa, v_id_usuario, p_nome, p_cpf);
END;
$$;


-- PROCEDURE 3: Adicionar técnico (Cria Contato -> Usuário -> Técnico)
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
    v_id_usuario INT;
BEGIN
    -- 1. Contato
    INSERT INTO tbl_contatos (email, telefone) VALUES (p_email, p_telefone) RETURNING id INTO v_id_contato;

    -- 2. Usuário (Role Tecnico)
    INSERT INTO tbl_usuarios (id_empresa, id_contato, nome_usuario, senha, role)
    VALUES (p_id_empresa, v_id_contato, p_nome, p_senha, 'tecnico')
    RETURNING id INTO v_id_usuario;

    -- 3. Técnico (Dados específicos)
    INSERT INTO tbl_tecnicos (id_empresa, id_usuario, nome, especialidade)
    VALUES (p_id_empresa, v_id_usuario, p_nome, p_especialidade);
END;
$$;

-- PROCEDURE 4: Atualizar cliente (Atualiza Cliente, Usuário e Contato)
CREATE OR REPLACE PROCEDURE sp_atualizar_cliente(
    p_id_cliente INT,
    p_nome VARCHAR,
    p_cpf VARCHAR,
    p_email VARCHAR,
    p_telefone VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_usuario INT;
    v_id_contato INT;
BEGIN
    -- Buscar IDs vinculados
    SELECT id_usuario INTO v_id_usuario FROM tbl_clientes WHERE id = p_id_cliente;
    SELECT id_contato INTO v_id_contato FROM tbl_usuarios WHERE id = v_id_usuario;

    -- Atualiza tabela Cliente
    UPDATE tbl_clientes SET nome = p_nome, cpf = p_cpf WHERE id = p_id_cliente;

    -- Atualiza tabela Usuário (Nome de exibição)
    UPDATE tbl_usuarios SET nome_usuario = p_nome WHERE id = v_id_usuario;

    -- Atualiza Contato
    UPDATE tbl_contatos SET email = p_email, telefone = p_telefone WHERE id = v_id_contato;
END;
$$;

CREATE OR REPLACE PROCEDURE sp_atualizar_tecnico(
    IN p_id INTEGER,
    IN p_nome VARCHAR,
    IN p_login VARCHAR,
    IN p_senha VARCHAR,
    IN p_email VARCHAR,
    IN p_telefone VARCHAR,
    IN p_especialidade VARCHAR -- <--- NOVO PARÂMETRO (7º item)
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_usuario INT;
    v_id_contato INT;
BEGIN
    -- 1. Atualiza TÉCNICO (Nome e Especialidade) -> Pega ID_USUARIO
    UPDATE tbl_tecnicos 
    SET nome = p_nome,
        especialidade = p_especialidade -- <--- AQUI ESTAVA FALTANDO!
    WHERE id = p_id
    RETURNING id_usuario INTO v_id_usuario;

    IF v_id_usuario IS NULL THEN
        RAISE EXCEPTION 'Técnico ID % não encontrado.', p_id;
    END IF;

    -- 2. Atualiza USUÁRIO (Login e Senha) -> Pega ID_CONTATO
    UPDATE tbl_usuarios
    SET nome_usuario = p_login,
        senha = p_senha
    WHERE id = v_id_usuario
    RETURNING id_contato INTO v_id_contato;

    -- 3. Atualiza CONTATO (Email e Telefone)
    IF v_id_contato IS NOT NULL THEN
        UPDATE tbl_contatos
        SET email = p_email,
            telefone = p_telefone
        WHERE id = v_id_contato;
    END IF;
END;
$$;
/*
================================================================================
 PARTE 4: FUNCTIONS E VIEWS (ADAPTADAS)
================================================================================
*/

-- VIEW 1: Detalhes dos clientes (JOIN corrigido para passar por tbl_usuarios)
CREATE OR REPLACE VIEW vw_detalhes_clientes AS
SELECT 
    c.id AS id_cliente,
    c.id_empresa,
    c.nome,
    c.cpf,
    ct.email,
    ct.telefone,
    u.id AS id_usuario
FROM tbl_clientes c
JOIN tbl_usuarios u ON c.id_usuario = u.id
JOIN tbl_contatos ct ON u.id_contato = ct.id;

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

-- VIEW MATERIALIZADA: Faturamento
CREATE MATERIALIZED VIEW vw_materialized_faturamento_mensal AS
SELECT
    id_empresa,
    date_trunc('month', data_saida) AS mes_faturamento,
    SUM(valor_total) AS faturamento_total
FROM tbl_ordens_servico
WHERE status = 'Concluida' AND data_saida IS NOT NULL
GROUP BY id_empresa, mes_faturamento;

-- FUNÇÃO AUXILIAR
CREATE OR REPLACE FUNCTION fn_contar_os_abertas(p_id_empresa INT)
RETURNS INT LANGUAGE plpgsql AS $$
DECLARE v_quantidade INT;
BEGIN
    SELECT COUNT(*) INTO v_quantidade FROM tbl_ordens_servico
    WHERE id_empresa = p_id_empresa AND status NOT IN ('Concluida', 'Cancelada');
    RETURN v_quantidade;
END;
$$;


/*
================================================================================
 PARTE 5: DADOS DE EXEMPLO (POPULANDO O BANCO)
================================================================================
*/

-- 1. Registrar Empresa + Admin + Endereço Sede
CALL sp_registrar_empresa_e_usuario(
    'TechDesk Solutions', '11.111.111/0001-11', 'Admin TechDesk', 'admin@techdesk.com', 'admin',
    'Rua Sede', '100', 'Centro', 'Curitiba', 'Parana - PR', '80000-000'
);

-- 2. Adicionar Técnicos (Gera Usuário e Contato automaticamente)
CALL sp_adicionar_tecnico(1, 'Tech Pedro', 'tech.pedro@techdesk.com', '(11) 98000-0001', 'Hardware', '1234');
CALL sp_adicionar_tecnico(1, 'Tech Maria', 'tech.maria@techdesk.com', '(11) 98000-0002', 'Software', '1234');
CALL sp_adicionar_tecnico(1, 'Tech João', 'tech.joao@techdesk.com', '(11) 98000-0003', 'Redes', '1234');

-- 3. Adicionar Clientes (Gera Usuário e Contato automaticamente)
CALL sp_adicionar_cliente(1, 'João da Silva', '123.456.789-00', 'joao@email.com', '(11) 9999-9999');
CALL sp_adicionar_cliente(1, 'Mariana Costa', '111.111.111-11', 'mariana@email.com', '(11) 91000-0001');
CALL sp_adicionar_cliente(1, 'Pedro Alves', '222.222.222-22', 'pedro@email.com', '(11) 91000-0002');
CALL sp_adicionar_cliente(1, 'Juliana Lima', '333.333.333-33', 'juliana@email.com', '(11) 91000-0003');

-- 4. Inserindo Endereços Extras (Exemplo: Cliente João tem 2 endereços)
-- Precisamos pegar o ID do USUÁRIO do João, não o ID do Cliente
INSERT INTO tbl_enderecos (id_usuario, descricao, logradouro, numero, cidade, estado, cep)
VALUES 
(
    (SELECT id_usuario FROM tbl_clientes WHERE nome = 'João da Silva'),
    'Residência', 'Rua das Flores', '10', 'São Paulo', 'SP', '01000-000'
),
(
    (SELECT id_usuario FROM tbl_clientes WHERE nome = 'João da Silva'),
    'Escritório', 'Av Paulista', '2000', 'São Paulo', 'SP', '01310-000'
);

-- 5. Ordens de Serviço
INSERT INTO tbl_ordens_servico (id_empresa, id_cliente, id_tecnico, descricao_problema, status, data_entrada, data_saida, valor_mao_obra, valor_pecas)
VALUES
(1, 1, 1, 'Notebook não liga', 'Concluida', '2023-11-01', '2023-11-02', 150.00, 300.00),
(1, 2, 2, 'Formatação', 'Em Andamento', '2023-12-01', NULL, 80.00, 0.00),
(1, 3, NULL, 'Computador lento', 'Aberta', '2023-12-06', NULL, 0.00, 0.00);

-- Atualiza a View Materializada
REFRESH MATERIALIZED VIEW vw_materialized_faturamento_mensal;

/*
================================================================================
 PARTE 6: ROLES E PERMISSÕES
================================================================================
*/

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'role_admin_app') THEN
        CREATE ROLE role_admin_app WITH LOGIN PASSWORD 'admin_pass';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'role_tecnico_app') THEN
        CREATE ROLE role_tecnico_app WITH LOGIN PASSWORD 'tecnico_pass';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE "TechDesk" TO role_admin_app; -- Altere "TechDesk" se o nome do banco for outro
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO role_admin_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO role_admin_app;
GRANT EXECUTE ON ALL PROCEDURES IN SCHEMA public TO role_admin_app;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO role_admin_app;

GRANT CONNECT ON DATABASE "TechDesk" TO role_tecnico_app;
GRANT USAGE ON SCHEMA public TO role_tecnico_app;
GRANT SELECT ON vw_detalhes_os, vw_detalhes_clientes, tbl_tecnicos TO role_tecnico_app;
GRANT UPDATE ON tbl_ordens_servico TO role_tecnico_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO role_tecnico_app;
GRANT EXECUTE ON FUNCTION fn_contar_os_abertas(INT) TO role_tecnico_app;

-- Teste final de verificação
SELECT * FROM vw_detalhes_clientes;

/* COMPLEMENTO: Funcionalidades originais que faltaram no bloco anterior 
   para garantir 100% de paridade com o arquivo original.
*/

-- FUNCTION: Relatório de status das O.S. (Original mantida)
CREATE OR REPLACE FUNCTION fn_relatorio_status_os(p_id_empresa INT)
RETURNS TABLE(status_os VARCHAR, quantidade BIGINT)
LANGUAGE sql AS $$
    SELECT status, COUNT(*) AS quantidade
    FROM tbl_ordens_servico
    WHERE id_empresa = p_id_empresa
    GROUP BY status
    ORDER BY status;
$$;



/* ================================================================================
 SCRIPT DE POPULAÇÃO DE DADOS EXTRAS - TECHDESK
================================================================================
*/

-- 1. ADICIONANDO MAIS TÉCNICOS
-- (Procedure já cria: Contato + Usuário + Técnico)
CALL sp_adicionar_tecnico(1, 'Tech Carlos', 'carlos.tech@techdesk.com', '(11) 98888-0001', 'Redes e Servidores', '1234');
CALL sp_adicionar_tecnico(1, 'Tech Amanda', 'amanda.tech@techdesk.com', '(11) 98888-0002', 'Eletrônica Avançada', '1234');

-- 2. ADICIONANDO MAIS CLIENTES
-- (Procedure já cria: Contato + Usuário + Cliente com senha padrão 'cliente123')
CALL sp_adicionar_cliente(1, 'Academia BodyFit', '45.123.001/0001-99', 'contato@bodyfit.com', '(11) 3030-1111');
CALL sp_adicionar_cliente(1, 'Padaria do Seu Zé', '12.345.678/0001-00', 'ze@padaria.com', '(11) 3030-2222');
CALL sp_adicionar_cliente(1, 'Escola Aprender', '98.765.432/0001-55', 'diretoria@aprender.com', '(11) 3030-3333');
CALL sp_adicionar_cliente(1, 'Consultório Dr. André', '111.222.333-44', 'andre.medico@email.com', '(11) 99999-1001');
CALL sp_adicionar_cliente(1, 'Advocacia Silva', '222.333.444-55', 'silva.adv@email.com', '(11) 99999-1002');
CALL sp_adicionar_cliente(1, 'Condomínio Solar', '55.666.777/0001-88', 'syndico@solar.com', '(11) 3030-4444');
CALL sp_adicionar_cliente(1, 'Restaurante Sabor', '66.777.888/0001-99', 'compras@sabor.com', '(11) 3030-5555');
CALL sp_adicionar_cliente(1, 'Oficina Mecânica Fast', '77.888.999/0001-11', 'oficina@fast.com', '(11) 3030-6666');
CALL sp_adicionar_cliente(1, 'Loja de Roupas Chic', '88.999.000/0001-22', 'vendas@chic.com', '(11) 3030-7777');
CALL sp_adicionar_cliente(1, 'Mercadinho da Esquina', '99.000.111/0001-33', 'mercadinho@email.com', '(11) 3030-8888');

-- 3. ADICIONANDO ENDEREÇOS PARA OS NOVOS USUÁRIOS
-- Precisamos buscar o ID do Usuário (tabela pai) usando o nome do cliente/técnico
INSERT INTO tbl_enderecos (id_usuario, descricao, logradouro, numero, bairro, cidade, estado, cep)
VALUES
((SELECT id_usuario FROM tbl_tecnicos WHERE nome = 'Tech Carlos'), 'Casa', 'Rua dos Servidores', '101', 'Tecnologia', 'São Paulo', 'SP', '01000-000'),
((SELECT id_usuario FROM tbl_tecnicos WHERE nome = 'Tech Amanda'), 'Lab', 'Av. Eletrônica', '202', 'Centro', 'São Paulo', 'SP', '01000-001'),
((SELECT id_usuario FROM tbl_clientes WHERE nome = 'Academia BodyFit'), 'Unidade 1', 'Rua Fitness', '500', 'Jardins', 'São Paulo', 'SP', '01000-002'),
((SELECT id_usuario FROM tbl_clientes WHERE nome = 'Padaria do Seu Zé'), 'Comércio', 'Rua do Pão', '12', 'Bairro Alto', 'Curitiba', 'PR', '80000-001'),
((SELECT id_usuario FROM tbl_clientes WHERE nome = 'Escola Aprender'), 'Campus A', 'Av. Educação', '1000', 'Universitário', 'Curitiba', 'PR', '80000-002'),
((SELECT id_usuario FROM tbl_clientes WHERE nome = 'Condomínio Solar'), 'Portaria', 'Rua do Sol', '88', 'Litoral', 'Santos', 'SP', '11000-000');


-- 4. GERANDO ORDENS DE SERVIÇO (HISTÓRICO)
-- Vamos variar as datas para ter dados nos meses de Outubro, Novembro, Dezembro e Janeiro.

-- O.S. CONCLUÍDAS (Faturamento Passado)
INSERT INTO tbl_ordens_servico (id_empresa, id_cliente, id_tecnico, descricao_problema, status, data_entrada, data_saida, valor_mao_obra, valor_pecas) VALUES
-- Outubro
(1, (SELECT id FROM tbl_clientes WHERE nome='Academia BodyFit'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Carlos'), 'Sistema da catraca travado', 'Concluida', '2023-10-05', '2023-10-06', 200.00, 0.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Padaria do Seu Zé'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Amanda'), 'Computador do caixa não liga', 'Concluida', '2023-10-10', '2023-10-12', 150.00, 350.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Escola Aprender'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Pedro'), 'Instalação de Projetor', 'Concluida', '2023-10-15', '2023-10-15', 300.00, 50.00),

-- Novembro
(1, (SELECT id FROM tbl_clientes WHERE nome='Consultório Dr. André'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Maria'), 'Formatação PC Recepção', 'Concluida', '2023-11-05', '2023-11-06', 100.00, 0.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Advocacia Silva'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech João'), 'Configuração de Rede Wi-Fi', 'Concluida', '2023-11-12', '2023-11-12', 250.00, 100.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Condomínio Solar'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Carlos'), 'Câmera de segurança offline', 'Concluida', '2023-11-20', '2023-11-22', 200.00, 450.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Restaurante Sabor'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Amanda'), 'Impressora térmica falhando', 'Concluida', '2023-11-25', '2023-11-26', 120.00, 80.00),

-- Dezembro
(1, (SELECT id FROM tbl_clientes WHERE nome='Oficina Mecânica Fast'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Pedro'), 'PC com vírus', 'Concluida', '2023-12-05', '2023-12-07', 150.00, 0.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Loja de Roupas Chic'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Maria'), 'Instalação Software Estoque', 'Concluida', '2023-12-10', '2023-12-11', 400.00, 0.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Mercadinho da Esquina'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech João'), 'Troca de Fonte queimada', 'Concluida', '2023-12-15', '2023-12-16', 80.00, 200.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Academia BodyFit'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Carlos'), 'Manutenção preventiva Servidor', 'Concluida', '2023-12-20', '2023-12-21', 500.00, 0.00);


-- O.S. EM ANDAMENTO (Trabalho atual)
INSERT INTO tbl_ordens_servico (id_empresa, id_cliente, id_tecnico, descricao_problema, status, data_entrada, valor_mao_obra, valor_pecas) VALUES
(1, (SELECT id FROM tbl_clientes WHERE nome='Padaria do Seu Zé'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Amanda'), 'Troca de teclado notebook', 'Em Andamento', CURRENT_DATE - 3, 100.00, 150.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Escola Aprender'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Pedro'), 'Laboratório de informática sem rede', 'Em Andamento', CURRENT_DATE - 2, 600.00, 100.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Consultório Dr. André'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Maria'), 'Backup de dados', 'Em Andamento', CURRENT_DATE - 1, 200.00, 0.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Advocacia Silva'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech João'), 'Upgrade SSD nos notebooks', 'Em Andamento', CURRENT_DATE - 5, 300.00, 900.00),
(1, (SELECT id FROM tbl_clientes WHERE nome='Condomínio Solar'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Carlos'), 'Interfone com chiado', 'Em Andamento', CURRENT_DATE, 150.00, 50.00);


-- O.S. ABERTAS (Ainda sem técnico ou aguardando análise)
INSERT INTO tbl_ordens_servico (id_empresa, id_cliente, id_tecnico, descricao_problema, status, data_entrada) VALUES
(1, (SELECT id FROM tbl_clientes WHERE nome='Restaurante Sabor'), NULL, 'Computador não liga', 'Aberta', CURRENT_DATE),
(1, (SELECT id FROM tbl_clientes WHERE nome='Oficina Mecânica Fast'), NULL, 'Monitor piscando', 'Aberta', CURRENT_DATE),
(1, (SELECT id FROM tbl_clientes WHERE nome='Loja de Roupas Chic'), NULL, 'Sistema lento', 'Aberta', CURRENT_DATE - 1),
(1, (SELECT id FROM tbl_clientes WHERE nome='Mercadinho da Esquina'), NULL, 'Mouse não funciona', 'Aberta', CURRENT_DATE - 2),
(1, (SELECT id FROM tbl_clientes WHERE nome='Academia BodyFit'), NULL, 'Instalar Office', 'Aberta', CURRENT_DATE - 3);


-- O.S. CANCELADAS
INSERT INTO tbl_ordens_servico (id_empresa, id_cliente, id_tecnico, descricao_problema, status, data_entrada) VALUES
(1, (SELECT id FROM tbl_clientes WHERE nome='Escola Aprender'), (SELECT id FROM tbl_tecnicos WHERE nome='Tech Pedro'), 'Orçamento reprovado pela diretoria', 'Cancelada', '2023-11-15'),
(1, (SELECT id FROM tbl_clientes WHERE nome='Condomínio Solar'), NULL, 'Problema resolveu sozinho', 'Cancelada', '2023-12-01');

-- Atualizar a View Materializada de Faturamento com os novos dados
REFRESH MATERIALIZED VIEW vw_materialized_faturamento_mensal;

-- Conferência simples
SELECT 'Clientes' as Tipo, count(*) as Qtd FROM tbl_clientes
UNION ALL
SELECT 'Técnicos', count(*) FROM tbl_tecnicos
UNION ALL
SELECT 'Ordens de Serviço', count(*) FROM tbl_ordens_servico;