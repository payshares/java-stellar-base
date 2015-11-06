package org.stellar.base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.codec.binary.Base32;

public class StrKey {
    public enum VersionByte {
        ACCOUNT_ID((byte)0x30),
        SEED((byte)0x90);
        private final byte value;
        VersionByte(byte value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }

    public static String encodeStellarAddress(byte[] data) {
        try {
            return encodeCheck(VersionByte.ACCOUNT_ID, data);
        } catch (IOException e) {
            return null;
        }
    }

    public static String encodeStellarSecretSeed(byte[] data) {
        try {
            return encodeCheck(VersionByte.SEED, data);
        } catch (IOException e) {
            return null;
        }
    }

    public static byte[] decodeStellarAddress(String data) throws FormatException {
        return decodeCheck(VersionByte.ACCOUNT_ID, data);
    }

    public static byte[] decodeStellarSecretSeed(String data) throws FormatException {
        return decodeCheck(VersionByte.SEED, data);
    }

    protected static String encodeCheck(VersionByte versionByte, byte[] data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(versionByte.getValue());
        outputStream.write(data);
        byte payload[] = outputStream.toByteArray();
        byte checksum[] = StrKey.calculateChecksum(payload);
        outputStream.write(checksum);
        byte unencoded[] = outputStream.toByteArray();
        Base32 base32Codec = new Base32();
        return base32Codec.encodeAsString(unencoded);
    }

    protected static byte[] decodeCheck(VersionByte versionByte, String encoded) throws FormatException {
        Base32 base32Codec = new Base32();
        byte[] decoded = base32Codec.decode(encoded);
        byte decodedVersionByte = decoded[0];
        byte[] payload  = Arrays.copyOfRange(decoded, 0, decoded.length-2);
        byte[] data     = Arrays.copyOfRange(payload, 1, payload.length);
        byte[] checksum = Arrays.copyOfRange(decoded, decoded.length-2, decoded.length);

        if (decodedVersionByte != versionByte.getValue()) {
            throw new FormatException("Version byte is invalid");
        }

        byte[] expectedChecksum = StrKey.calculateChecksum(payload);

        if (!Arrays.equals(expectedChecksum, checksum)) {
            throw new FormatException("Checksum invalid");
        }

        return data;
    }

    protected static byte[] calculateChecksum(byte[] bytes) {
        // This code calculates CRC16-XModem checksum
        // http://introcs.cs.princeton.edu/51data/CRC16CCITT.java.html
        int crc = 0x0000;
        int polynomial = 0x1021;
        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }
        crc &= 0xffff;
        // little-endian
        return new byte[] {
            (byte)crc,
            (byte)(crc >>> 8)};
    }
}
