package com.celnet.syncro.models.prime;

public class ConstellationCoding {
    public boolean dbpsk, res1, dqpsk, d8psk, res2, dbpskC, dqpskC, d8pskC,
            res3, res4, res5, res6, rDbpsk, rDqpsk, res7, res8;

    @Override
    public String toString() {
        return " DBPSK   : " + b(dbpsk) + "\n" +
                " RES     : " + b(res1) + "\n" +
                " DQPSK   : " + b(dqpsk) + "\n" +
                " D8PSK   : " + b(d8psk) + "\n" +
                " RES     : " + b(res2) + "\n" +
                " DBPSK_C : " + b(dbpskC) + "\n" +
                " DQPSK_C : " + b(dqpskC) + "\n" +
                " D8PSK_C : " + b(d8pskC) + "\n" +
                " RES     : " + b(res3) + "\n" +
                " RES     : " + b(res4) + "\n" +
                " RES     : " + b(res5) + "\n" +
                " RES     : " + b(res6) + "\n" +
                " R_DBPSK : " + b(rDbpsk) + "\n" +
                " R_DQPSK : " + b(rDqpsk) + "\n" +
                " RES     : " + b(res7) + "\n" +
                " RES     : " + b(res8);
    }

    private String b(boolean v) {
        return v ? "1" : "0";
    }
}