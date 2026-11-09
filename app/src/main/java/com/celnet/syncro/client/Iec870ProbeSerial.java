package com.celnet.syncro.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Fija en la sonda Bluetooth los parámetros del puerto serie que va hacia el contador (equivale a
 * la "CONFIGURACIÓN SERIAL" del software funcional, que en el PC la aplica el puerto COM).
 *
 * Comando de la sonda (documentación del fabricante):
 *   BaudTran,4800,N,8,0   → 4800 bps, sin paridad, 8 bits de datos, 1 bit de stop
 * Se envía con "\r\n" y la sonda responde "OK", igual que el resto de comandos que ya usa la app
 * (GetBatteryVolt, AT+ADDR).
 */
public final class Iec870ProbeSerial {

    private Iec870ProbeSerial() { }

    /**
     * Construye el comando. parityIndex: 0 = None, 1 = Even, 2 = Odd.
     * stopBits: "1", "1.5" o "2". Último campo del comando (confirmado): 0 = 1 bit, 1 = 1,5 bits,
     * 2 = 2 bits.
     */
    public static String comando(int baud, int parityIndex, int dataBits, String stopBits) {
        char paridad = parityIndex == 1 ? 'E' : parityIndex == 2 ? 'O' : 'N';
        String t = stopBits == null ? "1" : stopBits.trim().replace(',', '.');
        int stop = "2".equals(t) ? 2 : "1.5".equals(t) ? 1 : 0;
        return "BaudTran," + baud + "," + paridad + "," + dataBits + "," + stop;
    }

    /**
     * Envía el comando y espera el "OK". Devuelve la respuesta de la sonda (vacía si no contestó);
     * nunca lanza por falta de respuesta, para no impedir la conexión con sondas que no la den.
     */
    public static String aplicar(InputStream in, OutputStream out, String comando, int esperaMs)
            throws IOException {
        while (in.available() > 0) {
            in.read();
        }
        out.write((comando + "\r\n").getBytes("US-ASCII"));
        out.flush();

        StringBuilder sb = new StringBuilder();
        long inicio = System.currentTimeMillis();
        while (System.currentTimeMillis() - inicio < esperaMs) {
            if (in.available() > 0) {
                byte[] buf = new byte[128];
                int n = in.read(buf);
                if (n > 0) {
                    sb.append(new String(buf, 0, n, "US-ASCII"));
                }
                if (sb.toString().toLowerCase().contains("ok")) {
                    break;
                }
            } else {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return sb.toString().trim();
    }
}