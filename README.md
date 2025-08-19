# DevTicket

![DevTicket Logo](https://img.shields.io/badge/DevTicket-v1.0.1-blue.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.18--1.21+-green.svg)
![Spigot](https://img.shields.io/badge/Spigot-Compatible-orange.svg)

## Descrição

O **DevTicket** é um sistema avançado de tickets e suporte para servidores de Minecraft, desenvolvido para facilitar a comunicação entre jogadores e staff. Com armazenamento YAML nativo, categorias personalizáveis, sistema de prioridades e uma interface gráfica moderna e intuitiva, o DevTicket oferece uma solução completa para gerenciamento de suporte em servidores.

## Funcionalidades

### 🎫 Sistema de Tickets
- **Criação de tickets** com categorias e prioridades personalizáveis
- **Interface gráfica moderna** para fácil navegação
- **Sistema de comentários** para comunicação contínua
- **Atribuição de tickets** para membros específicos da staff
- **Fechamento e reabertura** de tickets com motivos
- **Limite configurável** de tickets por jogador
- **Sistema de cooldown** para evitar spam
- **Auto-fechamento** de tickets antigos

### 📊 Gerenciamento e Estatísticas
- **Painel de staff** com visão geral dos tickets
- **Estatísticas detalhadas** por jogador e globais
- **Listagem de tickets** próprios e de outros jogadores (para staff)
- **Histórico completo** de mensagens e ações

### 🔧 Configuração Avançada
- **Categorias personalizáveis**: Suporte Técnico, Denúncia, Sugestão, Bug Report, Outros
- **Níveis de prioridade**: Baixa, Média, Alta, Urgente
- **Mensagens customizáveis** com suporte a códigos de cor
- **Integração com Discord** via webhooks
- **Notificações para staff** sobre novos tickets e atualizações

### 🛡️ Sistema de Permissões
- **Permissões granulares** para diferentes níveis de acesso
- **Controle de visualização** (próprios tickets vs. todos os tickets)
- **Separação entre jogadores e staff**

## Instalação

### Pré-requisitos
- **Servidor Minecraft** versão 1.18 ou superior
- **Spigot ou fork compatível**
- **Java 17** ou superior

### Passos de Instalação

1. **Baixe o plugin**
   - Faça o download do arquivo `DevTicket-1.0.1.jar`

2. **Instale no servidor**
   ```bash
   # Copie o arquivo para a pasta plugins do seu servidor
   cp DevTicket-1.0.1.jar /caminho/para/seu/servidor/plugins/
   ```

3. **Reinicie o servidor**
   ```bash
   # Reinicie ou recarregue o servidor
   /restart
   # ou
   /reload
   ```

4. **Configuração inicial**
   - O plugin criará automaticamente os arquivos de configuração na pasta `plugins/DevTicket/`
   - Edite o arquivo `config.yml` conforme suas necessidades
   - Configure as permissões no seu plugin de permissões preferido

## Configuração

### Arquivo config.yml

O arquivo de configuração principal está localizado em `plugins/DevTicket/config.yml`:

```yaml
# Configurações de Tickets
tickets:
  max-per-player: 5          # Máximo de tickets por jogador
  cooldown-seconds: 300       # Cooldown entre criação de tickets (segundos)
  auto-close-days: 7          # Auto-fechar tickets após X dias
  categories:                 # Categorias disponíveis
    - "Suporte Técnico"
    - "Denúncia"
    - "Sugestão"
    - "Bug Report"
    - "Outros"
  priorities:                 # Níveis de prioridade
    - "Baixa"
    - "Média"
    - "Alta"
    - "Urgente"

# Configurações de Notificações
notifications:
  discord:
    enabled: false           # Ativar notificações Discord
    webhook-url: ""          # URL do webhook Discord
  staff-alerts:
    new-ticket: true         # Alertar staff sobre novos tickets
    ticket-updated: true     # Alertar sobre atualizações
    old-tickets: true        # Alertar sobre tickets antigos
    old-ticket-days: 3       # Considerar ticket antigo após X dias

# Mensagens Personalizáveis
messages:
  prefix: "&b[DevTicket]&r "
  no-permission: "&cVocê não tem permissão para usar este comando!"
  ticket-created: "&aTicket criado com sucesso! ID: &e{id}"
  # ... outras mensagens
```

### Integração Discord

Para ativar as notificações Discord:

1. Crie um webhook no seu servidor Discord
2. Copie a URL do webhook
3. Configure no `config.yml`:
   ```yaml
   notifications:
     discord:
       enabled: true
       webhook-url: "https://discord.com/api/webhooks/SEU_WEBHOOK_AQUI"
   ```

## Comandos

### Comando Principal: `/ticket`

**Aliases:** `/tickets`, `/suporte`, `/support`

| Comando | Descrição | Permissão | Exemplo |
|---------|-----------|-----------|----------|
| `/ticket` | Abrir menu principal | `devticket.create` | `/ticket` |
| `/ticket create <categoria> <título> [descrição]` | Criar novo ticket | `devticket.create` | `/ticket create "Bug Report" "Erro no spawn" "Não consigo fazer spawn"` |
| `/ticket list [jogador]` | Listar tickets | `devticket.view.own` / `devticket.view.all` | `/ticket list` ou `/ticket list Steve` |
| `/ticket view <id>` | Ver detalhes do ticket | `devticket.view.own` / `devticket.view.all` | `/ticket view 12345` |
| `/ticket close <id> [motivo]` | Fechar ticket (staff) | `devticket.manage` | `/ticket close 12345 "Problema resolvido"` |
| `/ticket assign <id> <staff>` | Atribuir ticket (staff) | `devticket.manage` | `/ticket assign 12345 Admin` |
| `/ticket reopen <id>` | Reabrir ticket (staff) | `devticket.manage` | `/ticket reopen 12345` |
| `/ticket comment <id> <mensagem>` | Adicionar comentário | `devticket.view.own` / `devticket.manage` | `/ticket comment 12345 "Verificando o problema"` |
| `/ticket stats` | Ver estatísticas (admin) | `devticket.admin` | `/ticket stats` |
| `/ticket reload` | Recarregar configuração (admin) | `devticket.admin` | `/ticket reload` |

## Permissões

### Permissões Principais

| Permissão | Descrição | Padrão |
|-----------|-----------|--------|
| `devticket.*` | Acesso completo ao DevTicket | `op` |
| `devticket.create` | Permite criar tickets | `false` |
| `devticket.view.own` | Permite ver próprios tickets | `true` |
| `devticket.view.all` | Permite ver tickets de outros jogadores | `op` |
| `devticket.manage` | Permite gerenciar tickets (staff) | `op` |
| `devticket.admin` | Acesso administrativo completo | `op` |

### Configuração de Permissões

#### Para LuckPerms:
```bash
# Permissões para jogadores
/lp group default permission set devticket.create true
/lp group default permission set devticket.view.own true

# Permissões para moderadores
/lp group moderator permission set devticket.manage true

# Permissões para administradores
/lp group admin permission set devticket.admin true
```

#### Para PermissionsEx:
```bash
# Permissões para jogadores
/pex group default add devticket.create
/pex group default add devticket.view.own

# Permissões para moderadores
/pex group moderator add devticket.manage

# Permissões para administradores
/pex group admin add devticket.admin
```

## Compatibilidade

### Versões do Minecraft
- ✅ **Minecraft 1.21.x** - Totalmente compatível com todas as funcionalidades
- ✅ **Minecraft 1.20.x** - Funcionalidades completas
- ✅ **Minecraft 1.19.x** - Totalmente compatível
- ✅ **Minecraft 1.18.x** - Versão base suportada

### Plataformas de Servidor
- ✅ **Spigot** 1.18+ 
- ✅ **Paper** 1.18+ (Recomendado)
- ✅ **Purpur** 1.18+
- ✅ **Pufferfish** 1.18+
- ❌ **Bukkit Vanilla** (Não suportado)
- ❌ **Forge/Fabric** (Não suportado)

### Requisitos do Sistema
- **Java 17** ou superior
- **RAM mínima:** 512MB disponível
- **Espaço em disco:** 50MB para logs e dados

### Plugins Compatíveis
- **LuckPerms** - Sistema de permissões recomendado
- **PermissionsEx** - Sistema de permissões alternativo
- **Vault** - Integração econômica (futuras versões)
- **PlaceholderAPI** - Suporte a placeholders (futuras versões)

## Suporte

### 🆘 Onde Obter Ajuda

- **Discord Oficial:** [https://discord.gg/bdxGxCbqCj](https://discord.gg/bdxGxCbqCj)
- **GitHub Issues:** [Reportar Bugs e Sugestões](https://github.com/DevPlugins/DevTicket/issues)
- **Documentação:** [Wiki do Projeto](https://github.com/DevPlugins/DevTicket/wiki)

### 🐛 Reportando Bugs

Ao reportar um bug, inclua:

1. **Versão do plugin:** `1.0.1`
2. **Versão do Minecraft/Spigot**
3. **Descrição detalhada** do problema
4. **Passos para reproduzir** o bug
5. **Logs de erro** (se houver)
6. **Lista de outros plugins** instalados

### 💡 Sugestões de Funcionalidades

Temos uma roadmap ativa! Sugestões são bem-vindas:

- Abra uma **Issue** no GitHub com a tag `enhancement`
- Participe das discussões no **Discord**
- Vote nas funcionalidades mais solicitadas

### 📋 Roadmap

- [ ] **v1.1.0** - Integração com PlaceholderAPI
- [ ] **v1.2.0** - Sistema de templates de resposta
- [ ] **v1.3.0** - API para desenvolvedores
- [ ] **v1.4.0** - Interface web administrativa
- [ ] **v2.0.0** - Migração para banco de dados SQL

## Créditos

### 👥 Equipe de Desenvolvimento
- **DevPlugins** - Desenvolvimento principal
- **Comunidade Discord** - Testes e feedback

### 📚 Bibliotecas e Recursos
- **Spigot API** - Framework base do plugin
- **YAML Configuration** - Sistema de configuração nativo
- **Java CompletableFuture** - Operações assíncronas

### 🙏 Agradecimentos Especiais
- Comunidade **ZappyCraft** pelo suporte técnico
- **Beta testers** que ajudaram no desenvolvimento
- Todos os usuários que reportaram bugs e sugeriram melhorias

## Licença

```
MIT License

Copyright (c) 2024 DevPlugins

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<div align="center">

**DevTicket v1.0.1** - Sistema Avançado de Tickets para Minecraft

[Discord](https://discord.gg/bdxGxCbqCj) • [GitHub](https://github.com/Devzinh/DevTicket) • [Documentação](https://github.com/DevPlugins/DevTicket/wiki)

*Desenvolvido com ❤️ pela equipe DevPlugins*

</div>
