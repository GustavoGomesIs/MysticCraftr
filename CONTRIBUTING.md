# Contribuindo para o MysticCraft

Obrigado por querer ajudar! Mantemos o projeto simples e clássico.

## Requisitos
- Java 17
- Maven 3.9+
- Paper 1.21.8 para testes locais
- (Opcional) ItemsAdder

## Como rodar
```bash
mvn clean package
# copie target/mysticcraft-1.0.0-SNAPSHOT.jar para server/plugins/
```

## Estilo
- Código Java limpo, nomes claros em inglês ou pt-br consistente.
- Evite dependências extras; use a API do Paper quando possível.
- Mensagens via `Lang` (suporta CHAT/ACTION_BAR/OFF).
- Não faça *shade* de APIs do servidor (use `provided`).

## Commits
- Use mensagens claras: `feat: ...`, `fix: ...`, `refactor: ...`, `docs: ...`.
- Abra PR apontando o que mudou e como testar.

## Testes manuais
- Montar mesa com grade 7x7 exemplo.
- Colocar/retirar item (vanilla/IA) e verificar whitelist.
- Reiniciar servidor e verificar persistência.
- `/mystic reload` após alterar config.
- Testar proteção contra TNT/pistões/fluido.