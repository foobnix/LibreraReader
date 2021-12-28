---
layout: main
---

# Backup e migração de dados

> O backup de dados é necessário se você pretende transferir livros para um novo dispositivo, nova pasta ou cartão SD

# Exportar (backup)

Toque em _Exportar_ para salvar todas as configurações do aplicativo em um arquivo .zip. Escolha a pasta para salvar o arquivo .zip e renomeie o arquivo, se desejar.

Assim você salvará:

* configurações do aplicativo
* Favoritos
* Progresso da leitura
* Tags de usuário
 
# Importação

Toque em _Importar_ e encontre o arquivo .zip com seus dados de backup. Toque no arquivo e depois em _SELECT_

# Migrar

A migração substituirá apenas os caminhos de arquivo nos arquivos de configuração do aplicativo.

O caminho completo é armazenado em Configurações. Por exemplo, se o caminho para o seu livro (example.pdf) for o seguinte:

/storage/Books/example.pdf

e você deseja movê-lo para a pasta **MyBooks**, você precisa alterar o local no arquivo de configuração do aplicativo para:

/storage/MyBooks/example.pdf

Execute _Migrate_ e substitua:

Caminho antigo: **/ Livros /**
Novo caminho: **/ MyBooks /**

Toque em _INICIAR MIGRAÇÃO_

Se você estiver movendo seu livro para um **cartão SD externo**, pode fazer isso facilmente substituindo o destino:

_Migrar_:/storage/AAAA-AAAA/Livros para/storage/BBBB-BBBB/Livros:

Caminho antigo: **/ storage/AAAA-AAAA /**
Novo caminho: **/ storage/BBBB-BBBB /**

> **Lembrete**: Não se esqueça de fazer _Exportar_ primeiro para ter um backup.

|1|2|3|
|-|-|-|
|![](1.png)|![](2.png)|![](3.png)|
