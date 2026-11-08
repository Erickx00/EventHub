package com.eventhub.user.entity;

/**
 * Perfil do usuario. A autorizacao no backend sempre compara com estes valores;
 * o frontend nunca decide permissao sozinho.
 */
public enum Role {
    ATTENDEE,
    ORGANIZER,
    ADMIN
}
