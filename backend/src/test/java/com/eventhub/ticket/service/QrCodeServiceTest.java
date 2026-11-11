package com.eventhub.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QrCodeServiceTest {

    private final QrCodeService qrCodeService = new QrCodeService();

    @Test
    void producesPngImage() {
        byte[] png = qrCodeService.pngFor("token-de-validacao");

        // Assinatura PNG: 89 50 4E 47
        assertThat(png).isNotEmpty();
        assertThat(png[0] & 0xFF).isEqualTo(0x89);
        assertThat(png[1]).isEqualTo((byte) 'P');
        assertThat(png[2]).isEqualTo((byte) 'N');
        assertThat(png[3]).isEqualTo((byte) 'G');
    }

    @Test
    void producesDifferentImagesForDifferentTokens() {
        byte[] first = qrCodeService.pngFor("token-a");
        byte[] second = qrCodeService.pngFor("token-b");

        assertThat(first).isNotEqualTo(second);
    }
}
