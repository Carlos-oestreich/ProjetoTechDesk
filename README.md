# 🖥️ Projeto TechDesk  
![Status](https://img.shields.io/badge/status-finalizado-brightgreen)

<p align="center">
  <img src="https://img.shields.io/badge/Java-93.6%25-blue?style=for-the-badge&logo=java">
  <img src="https://img.shields.io/badge/PLpgSQL-6.4%25-316192?style=for-the-badge&logo=postgresql">
  <img src="https://img.shields.io/badge/NetBeans-IDE-1b6ac6?style=for-the-badge&logo=apache-netbeans-ide">
  <img src="https://img.shields.io/badge/PostgreSQL-Database-2496ED?style=for-the-badge&logo=postgresql">
</p>

<p align="center">
  Sistema completo para gestão de chamados técnicos.<br>
 <strong>Desenvolvido exclusivamente para fins acadêmicos</strong>
por <a href="https://github.com/Carlos-oestreich">Carlos Eduardo Oestreich</a> e <a href="https://github.com/larissalaumann">Larissa Maria Laumann.</p>
</p>

---

## 📌 Sobre o Projeto

O **TechDesk** é um sistema de suporte e gestão de chamados técnicos, totalmente desenvolvido em Java puro com interface gráfica desktop e integração ao banco de dados PostgreSQL.

- Código limpo, modular e orientado a objetos.
- Banco modelado em PLpgSQL (arquivos .sql).
- Ideal para aprendizado acadêmico e referência para novos projetos desktop.

---

## ✨ Funcionalidades

- Cadastro e autenticação de usuários e técnicos
- Abertura, atualização e acompanhamento de chamados técnicos
- Controle de categorias, prioridades e status dos chamados
- Relatórios de atendimentos
- Interface visual intuitiva, tornando o uso prático e amigável

---

## 💻 Interface Gráfica

O projeto inclui uma interface gráfica (desktop) desenvolvida em Java, permitindo ao usuário interação direta com as funcionalidades do sistema.

As telas e componentes visuais (views) estão localizados em:
```
src/main/java/com/mycompany/projetotechdesk/
```
Utilize o NetBeans para visualizar, modificar ou criar novas telas.

---

## 🧱 Estrutura do Projeto

```
ProjetoTechDesk/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── mycompany/
│                   └── projetotechdesk/
│                        ├── (views, controllers, models, DAOs)
├── ProjetoTechDeskBanco.sql  # Scripts de banco para PostgreSQL
├── pom.xml                   # Build Maven (gerenciamento de dependências)
└── README.md
```

---

## 🚀 Tecnologias Utilizadas

- **Java** (desktop, com NetBeans IDE)
- **PLpgSQL/PostgreSQL** (banco e procedures)
- **JDBC** (conexão Java ↔ PostgreSQL)
- **NetBeans IDE** (desenvolvimento e execução)
- **Maven** (para build/gerenciamento auxiliar)

---

## ⚙️ Como Executar

1. **Clone o repositório**
   ```bash
   git clone https://github.com/Carlos-oestreich/ProjetoTechDesk.git
   cd ProjetoTechDesk
   ```
2. **Configure o banco de dados**
   - Rode o script `ProjetoTechDeskBanco.sql` no seu PostgreSQL.
   - Ajuste usuáirio, senha e URL de conexão no código-fonte, se necessário.
3. **Abra o projeto no NetBeans**
   - Importe como projeto Java padrão (acione o Maven se desejar).
   - Certifique-se de que o driver JDBC do PostgreSQL está disponível.
4. **Compile e execute**
   - Use as opções **Build** e **Run** do NetBeans para rodar o sistema.

---

### 💻 Ambiente de Desenvolvimento

- **NetBeans IDE** (versão recomendada: 15 ou superior)
- **Java 17** ou mais recente
- **PostgreSQL** instalado localmente

---

## 🔐 Segurança & Boas Práticas

- Senhas dos usuários armazenadas com segurança, nunca expostas em código/repositório
- Separação clara dos componentes em camadas (MVC ou similar)
- Não suba dados sensíveis em repositórios públicos

---

## 👨‍💻 Autores

- [Carlos Eduardo Oestreich](https://github.com/Carlos-oestreich)
- [Larissa Maria Laumann](https://github.com/larissalaumann)

---

## 📌 Observação Final

Projeto acadêmico, estruturado para boas práticas de programação e ideal como ponto de partida para soluções desktop Java integradas a banco de dados.

<div align="center">
  💡 Dúvidas, feedbacks e colaborações são bem-vindos!<br>
</div>
