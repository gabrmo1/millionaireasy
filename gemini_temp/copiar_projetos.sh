#!/bin/bash

# Script para COPIAR projetos de uma pasta de origem para a pasta atual,
# EXCLUINDO arquivos e pastas de build, dependências, configurações de IDE
# E ARQUIVOS IRRELEVANTES para análise do código fonte (imagens, boilerplate, etc).
#
# Ele utiliza 'tar' para criar um fluxo de dados (stream) que é descompactado
# no destino, evitando a transferência de dados desnecessários. Funciona no Git Bash padrão.
#
# --- ATUALIZACAO ---
# Esta versao agora REMOVE o diretorio de destino do projeto se ele ja existir,
# garantindo uma copia 100% limpa e espelhada da origem, focada apenas no código útil.

# --- Configuração ---
PASTA_ORIGEM=".."
PASTA_DESTINO="."
# --- Fim da Configuração ---

echo "Iniciando copia limpa de projetos (usando 'tar')..."
ORIGEM_ABSOLUTA=$(realpath "$PASTA_ORIGEM")
DESTINO_ABSOLUTO=$(realpath "$PASTA_DESTINO")

echo "Origem: $ORIGEM_ABSOLUTA"
echo "Destino: $DESTINO_ABSOLUTO"
echo "-----------------------------------------------------------------"

if [ ! -d "$ORIGEM_ABSOLUTA" ]; then
    echo "ERRO: A pasta de origem '$ORIGEM_ABSOLUTA' não foi encontrada."
    exit 1
fi

itens_a_excluir=(
  # Específico para Node.js
  "node_modules" "package-lock.json" "yarn.lock" "npm-debug.log" "yarn-debug.log" "yarn-error.log"
  # Específico para Java (Maven/Gradle)
  "target" ".gradle" ".mvn"
  # Pastas de build/distribuição comuns
  "build" "dist"
  # Relatórios de cobertura de testes
  "coverage"
  # Arquivos de ambiente
  ".env" ".env.local" ".env.development" ".env.test" ".env.production"
  # Controle de Versão
  ".git"
  # Configurações e metadados de IDEs
  ".idea" ".vscode" ".vs"
  # Arquivos de sistema operacional
  ".DS_Store"

  # --- NOVOS ARQUIVOS IGNORADOS (Irrelevantes para análise de código) ---
  # Imagens e arquivos binários
  "*.svg" "*.png" "*.ico" "*.jpg" "*.jpeg" "*.gif"
  # Metadados e documentação boilerplate
  "robots.txt" "manifest.json" "LICENSE"
  # Arquivos de setup vazios ou de tipagem padrão
  "vite-env.d.ts" "setupTests.ts"
  # Regras de ignorados (Git e Docker)
  ".gitignore" ".dockerignore"
)

tar_exclude_opts=()
for item in "${itens_a_excluir[@]}"; do
  tar_exclude_opts+=(--exclude="$item")
done

for dir_path in "$ORIGEM_ABSOLUTA"/*/ ; do
    if [ -d "$dir_path" ]; then
        dir_name=$(basename "$dir_path")

        if [ "$(realpath "$dir_path")" == "$DESTINO_ABSOLUTO" ]; then
            continue
        fi

        if [ -f "${dir_path}package.json" ] || \
           [ -f "${dir_path}pom.xml" ] || \
           [ -f "${dir_path}build.gradle" ] || \
           [ -f "${dir_path}build.gradle.kts" ]; then

            echo ""
            echo "COPIANDO PROJETO: $dir_name"
            
            DESTINO_PROJETO="$DESTINO_ABSOLUTO/$dir_name"

            # Se o diretório de destino do projeto já existe, remove-o completamente.
            if [ -d "$DESTINO_PROJETO" ]; then
                echo "  -> Diretório de destino existente encontrado. Removendo para garantir uma cópia limpa..."
                rm -rf "$DESTINO_PROJETO"
            fi
            
            mkdir -p "$DESTINO_PROJETO"

            # Lógica principal com 'tar':
            (cd "$dir_path" && tar -c "${tar_exclude_opts[@]}" . | (cd "$DESTINO_PROJETO" && tar -x))
            
            echo "Cópia de $dir_name concluída."

        else
            echo ""
            echo "PULANDO: $dir_name (Não parece ser um projeto Node.js ou Java)."
        fi
    fi
done

echo "-----------------------------------------------------------------"
echo "Processo de cópia de projetos concluído!"