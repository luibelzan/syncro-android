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

    public boolean[] toBitArray() {
        return new boolean[] {
                dbpsk, res1, dqpsk, d8psk, res2, dbpskC, dqpskC, d8pskC,
                res3, res4, res5, res6, rDbpsk, rDqpsk, res7, res8
        };
    }

    public static ConstellationCoding fromBitArray(boolean[] bits) {
        ConstellationCoding cc = new ConstellationCoding();
        cc.dbpsk   = bits[0];
        cc.res1    = bits[1];
        cc.dqpsk   = bits[2];
        cc.d8psk   = bits[3];
        cc.res2    = bits[4];
        cc.dbpskC  = bits[5];
        cc.dqpskC  = bits[6];
        cc.d8pskC  = bits[7];
        cc.res3    = bits[8];
        cc.res4    = bits[9];
        cc.res5    = bits[10];
        cc.res6    = bits[11];
        cc.rDbpsk  = bits[12];
        cc.rDqpsk  = bits[13];
        cc.res7    = bits[14];
        cc.res8    = bits[15];
        return cc;
    }
}