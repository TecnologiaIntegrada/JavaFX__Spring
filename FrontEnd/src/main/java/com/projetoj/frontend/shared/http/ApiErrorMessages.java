package com.projetoj.frontend.shared.http;

public final class ApiErrorMessages {

    private ApiErrorMessages() {
    }

    public static String toUserMessage(ApiException exception) {
        if (exception.getErrorCode() == null) {
            return exception.getMessage();
        }
        return switch (exception.getErrorCode()) {
            case "ROLE_IN_USE" -> "Nao e possivel excluir: existem usuarios com este perfil.";
            case "MODULE_IN_USE" -> "Nao e possivel excluir: o modulo possui permissoes vinculadas.";
            case "PERMISSION_IN_USE" -> "Nao e possivel excluir: a permissao esta vinculada a perfis.";
            case "CANNOT_DELETE_ADMIN" -> "O usuario admin nao pode ser excluido.";
            case "CANNOT_DELETE_ADMIN_ROLE" -> "O perfil ADMIN nao pode ser excluido.";
            case "CANNOT_DELETE_SELF" -> "Voce nao pode excluir o proprio usuario.";
            case "INVALID_CREDENTIALS" -> "Usuario ou senha invalidos.";
            case "AUTH_SESSION_EXPIRED" -> "Tempo de autenticacao excedido. Faca login novamente.";
            case "AUTH_REQUIRED" -> "Sessao invalida ou ausente. Faca login novamente.";
            case "RATE_LIMIT_EXCEEDED" -> "Muitas requisicoes em pouco tempo. Aguarde e tente novamente.";
            case "USER_INACTIVE" -> "Usuario nao esta ativo.";
            case "ROLE_INACTIVE" -> "Perfil do usuario esta inativo.";
            case "DUPLICATE_USERNAME" -> "Ja existe um usuario com este username.";
            case "DUPLICATE_EMAIL" -> "Ja existe um usuario com este email.";
            case "DUPLICATE_ROLE" -> "Ja existe um perfil com este nome.";
            case "DUPLICATE_MODULE" -> "Ja existe um modulo com este codigo.";
            case "DUPLICATE_PERMISSION" -> "Esta permissao (modulo + acao) ja existe.";
            case "USER_NOT_FOUND" -> "Usuario nao encontrado.";
            case "ROLE_NOT_FOUND" -> "Perfil nao encontrado.";
            case "MODULE_NOT_FOUND" -> "Modulo nao encontrado.";
            case "PERMISSION_NOT_FOUND" -> "Permissao nao encontrada.";
            case "VALIDATION_ERROR" -> exception.getMessage();
            default -> exception.getMessage();
        };
    }
}
