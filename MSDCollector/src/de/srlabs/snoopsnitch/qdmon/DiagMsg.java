package de.srlabs.snoopsnitch.qdmon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class DiagMsg {
    final static byte ESC_MASK     = 0x20;
    final static byte ESC_CHAR     = 0x7d;
    final static byte CONTROL_CHAR = 0x7e;

    byte[]            data;

    DiagMsg(byte[] data) {
        this.data = data;
    }

    public static List<ByteBuffer> fromBytes(ByteBuffer buf) {
        List<ByteBuffer> result = new ArrayList<ByteBuffer>();
        ByteBuffer de_escaped = ByteBuffer.allocate(buf.remaining()).order(ByteOrder.LITTLE_ENDIAN);

        while (buf.hasRemaining()) {
            de_escaped.mark();
            buf.mark();

            for (;;) {
                byte b = buf.get();
                if (b == CONTROL_CHAR) {
                    break;
                }
                if (b == ESC_CHAR) {
                    b = (byte) (buf.get() ^ ESC_MASK);
                }
                de_escaped.put(b);
            }

            de_escaped.position(de_escaped.position() - 2);

            ByteBuffer section = de_escaped.duplicate();
            section.limit(section.position());
            section.reset();

            ByteBuffer raw = buf.duplicate();
            raw.limit(raw.position());
            raw.reset();
            // DiagUtil.printBuf("diagmsg", "raw message: ", raw);
            // DiagUtil.printBuf("diagmsg", "unescaped message: ", section);

            int crc = de_escaped.getShort() & 0xffff;
            int should_crc = Crc16.crc16(section);
            if (crc != should_crc) {
                Log.w("diagmsg", String.format("crc message not matching: has %04x, should have %04x", crc, should_crc));
                continue;
            }

            result.add(section.slice());
        }

        return result;
    }

    byte[] frame() {
        int crc = Crc16.crc16(this.data);

        ByteBuffer buf = ByteBuffer.allocate(this.data.length + 2).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(this.data);
        buf.putShort((short) crc);
        buf.rewind();

        int total = buf.remaining();
        while (buf.hasRemaining()) {
            byte b = buf.get();
            if (b == ESC_CHAR || b == CONTROL_CHAR) {
                total++;
            }
        }

        int i = 0;
        buf.rewind();
        byte[] escaped_buf = new byte[total + 1];
        while (buf.hasRemaining()) {
            byte b = buf.get();
            if (b == ESC_CHAR || b == CONTROL_CHAR) {
                escaped_buf[i++] = ESC_CHAR;
                b ^= ESC_MASK;
            }
            escaped_buf[i++] = b;
        }
        escaped_buf[i++] = CONTROL_CHAR;
        return escaped_buf;
    }
}

class Crc16 {
    final static int INITIAL = 0x84cf;
    final static int POLY    = 0x1021;

    int              crc     = INITIAL;

    static public int crc16(byte[] ary) {
        return (new Crc16(ary)).getCrc();
    }

    static public int crc16(ByteBuffer buf) {
        return (new Crc16(buf)).getCrc();
    }

    public Crc16() {
    }

    public Crc16(byte[] data) {
        for (byte b : data) {
            addByte(b);
        }
    }

    public Crc16(ByteBuffer data) {
        data = data.duplicate();
        while (data.hasRemaining()) {
            addByte(data.get());
        }
    }

    public void addByte(byte b) {
        for (int bit = 1; bit < 0x100; bit <<= 1) {
            this.crc <<= 1;
            if ((b & bit) != 0)
                this.crc |= 1;
            if (this.crc > 0xffff) {
                this.crc &= 0xffff;
                this.crc ^= POLY;
            }
        }
    }

    public int getCrc() {
        int saved_crc = this.crc;
        addByte((byte) 0);
        addByte((byte) 0);
        int padded_crc = this.crc;
        this.crc = saved_crc;

        int final_crc = 0;
        for (int i = 1; i < 0x10000; i <<= 1) {
            final_crc <<= 1;
            if ((padded_crc & i) != 0)
                final_crc |= 1;
        }
        final_crc ^= 0xffff;
        return (final_crc);
    }
}
