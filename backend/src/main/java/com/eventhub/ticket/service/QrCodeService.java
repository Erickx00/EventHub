package com.eventhub.ticket.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Gera o PNG do QR Code a partir do token. Sem nada de pessoal no conteudo:
 * quem escanear o codigo so obtem um identificador opaco.
 */
@Service
public class QrCodeService {

    private static final int SIZE = 320;

    public byte[] pngFor(String content) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    SIZE,
                    SIZE,
                    Map.of(EncodeHintType.MARGIN, 1));

            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("Falha ao gerar o QR Code", ex);
        }
    }
}
