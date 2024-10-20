public class AES {
    private static final int Nb = 4;  // Number of columns (32-bit words) comprising the state
    private static final int Nk = 4;  // Number of 32-bit words comprising the key (128 bits)
    private static final int Nr = 10; // Number of rounds (AES-128 uses 10 rounds)

    private byte[] key;  // 128-bit key (16 bytes)
    private byte[][] roundKeys;  // Expanded round keys

    // S-box for byte substitution in SubBytes step
    private static final byte[] sBox = {
        0x63, 0x7c, 0x77, 0x7b, (byte)0xf2, 0x6b, 0x6f, (byte)0xc5, 0x30, 0x01, 0x67, 0x2b, (byte)0xfe, (byte)0xd7, (byte)0xab, 0x76,
        (byte)0xca, (byte)0x82, (byte)0xc9, 0x7d, (byte)0xfa, 0x59, 0x47, (byte)0xf0, (byte)0xad, (byte)0xd4, (byte)0xa2, (byte)0xaf, (byte)0x9c, (byte)0xa4, 0x72, (byte)0xc0,
        (byte)0xb7, (byte)0xfd, (byte)0x93, 0x26, 0x36, 0x3f, (byte)0xf7, (byte)0xcc, 0x34, (byte)0xa5, (byte)0xe5, (byte)0xf1, 0x71, (byte)0xd8, 0x31, 0x15,
        0x04, (byte)0xc7, 0x23, (byte)0xc3, 0x18, (byte)0x96, 0x05, (byte)0x9a, 0x07, 0x12, (byte)0x80, (byte)0xe2, (byte)0xeb, 0x27, (byte)0xb2, 0x75,
        0x09, (byte)0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, (byte)0xa0, 0x52, 0x3b, (byte)0xd6, (byte)0xb3, 0x29, (byte)0xe3, 0x2f, (byte)0x84,
        0x53, (byte)0xd1, 0x00, (byte)0xed, 0x20, (byte)0xfc, (byte)0xb1, 0x5b, 0x6a, (byte)0xcb, (byte)0xbe, 0x39, 0x4a, 0x4c, 0x58, (byte)0xcf,
        (byte)0xd0, (byte)0xef, (byte)0xaa, (byte)0xfb, 0x43, 0x4d, 0x33, (byte)0x85, 0x45, (byte)0xf9, 0x02, 0x7f, 0x50, 0x3c, (byte)0x9f, (byte)0xa8,
        0x51, (byte)0xa3, 0x40, (byte)0x8f, (byte)0x92, (byte)0x9d, 0x38, (byte)0xf5, (byte)0xbc, (byte)0xb6, (byte)0xda, 0x21, 0x10, (byte)0xff, (byte)0xf3, (byte)0xd2,
        (byte)0xcd, 0x0c, 0x13, (byte)0xec, 0x5f, (byte)0x97, 0x44, 0x17, (byte)0xc4, (byte)0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
        0x60, (byte)0x81, 0x4f, (byte)0xdc, 0x22, 0x2a, (byte)0x90, (byte)0x88, 0x46, (byte)0xee, (byte)0xb8, 0x14, (byte)0xde, 0x5e, 0x0b, (byte)0xdb,
        (byte)0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, (byte)0xc2, (byte)0xd3, (byte)0xac, 0x62, (byte)0x91, (byte)0x95, (byte)0xe4, 0x79,
        (byte)0xe7, (byte)0xc8, 0x37, 0x6d, (byte)0x8d, (byte)0xd5, 0x4e, (byte)0xa9, 0x6c, 0x56, (byte)0xf4, (byte)0xea, 0x65, 0x7a, (byte)0xae, 0x08,
        (byte)0xba, 0x78, 0x25, 0x2e, 0x1c, (byte)0xa6, (byte)0xb4, (byte)0xc6, (byte)0xe8, (byte)0xdd, 0x74, 0x1f, 0x4b, (byte)0xbd, (byte)0x8b, (byte)0x8a,
        0x70, 0x3e, (byte)0xb5, 0x66, 0x48, 0x03, (byte)0xf6, 0x0e, 0x61, 0x35, 0x57, (byte)0xb9, (byte)0x86, (byte)0xc1, 0x1d, (byte)0x9e,
        (byte)0xe1, (byte)0xf8, (byte)0x98, 0x11, 0x69, (byte)0xd9, (byte)0x8e, (byte)0x94, (byte)0x9b, 0x1e, (byte)0x87, (byte)0xe9, (byte)0xce, 0x55, 0x28, (byte)0xdf,
        (byte)0x8c, (byte)0xa1, (byte)0x89, 0x0d, (byte)0xbf, (byte)0xe6, 0x42, 0x68, 0x41, (byte)0x99, 0x2d, 0x0f, (byte)0xb0, 0x54, (byte)0xbb, 0x16
    };

    // Inverse S-box for decryption
    private static final byte[] invSBox = {
        0x52, 0x09, 0x6a, (byte)0xd5, 0x30, 0x36, (byte)0xa5, 0x38, (byte)0xbf, 0x40, (byte)0xa3, (byte)0x9e, (byte)0x81, (byte)0xf3, (byte)0xd7, (byte)0xfb,
        0x7c, (byte)0xe3, 0x39, (byte)0x82, (byte)0x9b, 0x2f, (byte)0xff, (byte)0x87, 0x34, (byte)0x8e, 0x43, 0x44, (byte)0xc4, (byte)0xde, (byte)0xe9, (byte)0xcb,
        0x54, 0x7b, (byte)0x94, 0x32, (byte)0xa6, (byte)0xc2, 0x23, 0x3d, (byte)0xee, 0x4c, (byte)0x95, 0x0b, 0x42, (byte)0xfa, (byte)0xc3, 0x4e,
        0x08, 0x2e, (byte)0xa1, 0x66, 0x28, (byte)0xd9, 0x24, (byte)0xb2, 0x76, 0x5b, (byte)0xa2, 0x49, 0x6d, (byte)0x8b, (byte)0xd1, 0x25,
        0x72, (byte)0xf8, (byte)0xf6, 0x64, (byte)0x86, 0x68, (byte)0x98, 0x16, (byte)0xd4, (byte)0xa4, 0x5c, (byte)0xcc, 0x5d, 0x65, (byte)0xb6, (byte)0x92,
        0x6c, 0x70, 0x48, 0x50, (byte)0xfd, (byte)0xed, (byte)0xb9, (byte)0xda, 0x5e, 0x15, 0x46, 0x57, (byte)0xa7, (byte)0x8d, (byte)0x9d, (byte)0x84,
        (byte)0x90, (byte)0xd8, (byte)0xab, 0x00, (byte)0x8c, (byte)0xbc, (byte)0xd3, 0x0a, (byte)0xf7, (byte)0xe4, 0x58, 0x05, (byte)0xb8, (byte)0xb3, 0x45, 0x06,
        (byte)0xd0, 0x2c, 0x1e, (byte)0x8f, (byte)0xca, 0x3f, 0x0f, 0x02, (byte)0xc1, (byte)0xaf, (byte)0xbd, 0x03, 0x01, 0x13, (byte)0x8a, 0x6b,
        0x3a, (byte)0x91, 0x11, 0x41, 0x4f, 0x67, (byte)0xdc, (byte)0xea, (byte)0x97, (byte)0xf2, (byte)0xcf, (byte)0xce, (byte)0xf0, (byte)0xb4, (byte)0xe6, 0x73,
        (byte)0x96, (byte)0xac, 0x74, 0x22, (byte)0xe7, (byte)0xad, 0x35, (byte)0x85, (byte)0xe2, (byte)0xf9, 0x37, (byte)0xe8, 0x1c, 0x75, (byte)0xdf, 0x6e,
        0x47, (byte)0xf1, 0x1a, 0x71, 0x1d, 0x29, (byte)0xc5, (byte)0x89, 0x6f, (byte)0xb7, 0x62, 0x0e, (byte)0xaa, 0x18, (byte)0xbe, 0x1b,
        (byte)0xfc, 0x56, 0x3e, 0x4b, (byte)0xc6, (byte)0xd2, 0x79, 0x20, (byte)0x9a, (byte)0xdb, (byte)0xc0, (byte)0xfe, 0x78, (byte)0xcd, 0x5a, (byte)0xf4,
        0x1f, (byte)0xdd, (byte)0xa8, 0x33, (byte)0x88, 0x07, (byte)0xc7, 0x31, (byte)0xb1, 0x12, 0x10, 0x59, 0x27, (byte)0x80, (byte)0xec, 0x5f,
        0x60, 0x51, 0x7f, (byte)0xa9, 0x19, (byte)0xb5, 0x4a, 0x0d, 0x2d, (byte)0xe5, 0x7a, (byte)0x9f, (byte)0x93, (byte)0xc9, (byte)0x9c, (byte)0xef,
        (byte)0xa0, (byte)0xe0, 0x3b, 0x4d, (byte)0xae, 0x2a, (byte)0xf5, (byte)0xb0, (byte)0xc8, (byte)0xeb, (byte)0xbb, 0x3c, (byte)0x83, 0x53, (byte)0x99, 0x61,
        0x17, 0x2b, 0x04, 0x7e, (byte)0xba, 0x77, (byte)0xd6, 0x26, (byte)0xe1, 0x69, 0x14, 0x63, 0x55, 0x21, 0x0c, 0x7d
    };

    // Rcon array for key expansion
    private static final byte[] rcon = {
        0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, (byte)0x80, 0x1b, 0x36
    };

    public AES(byte[] key) {
        this.key = key;
        this.roundKeys = new byte[Nb * (Nr + 1)][4];
        keyExpansion();
    }

    // Key Expansion: Expands the original key into an array of round keys
    private void keyExpansion() {
        for (int i = 0; i < Nk; i++) {
            roundKeys[i] = new byte[]{key[4*i], key[4*i+1], key[4*i+2], key[4*i+3]};
        }

        for (int i = Nk; i < Nb * (Nr + 1); i++) {
            byte[] temp = roundKeys[i-1];
            if (i % Nk == 0) {
                temp = subWord(rotWord(temp));
                temp[0] ^= rcon[i/Nk - 1];
            }
            for (int j = 0; j < 4; j++) {
                roundKeys[i][j] = (byte) (roundKeys[i-Nk][j] ^ temp[j]);
            }
        }
    }

    private byte[] rotWord(byte[] word) {
        return new byte[]{word[1], word[2], word[3], word[0]};
    }

    private byte[] subWord(byte[] word) {
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            result[i] = sBox[word[i] & 0xFF];
        }
return result;
    }

    // AddRoundKey: XOR the state matrix with the round key
    private void addRoundKey(byte[][] state, int round) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < Nb; j++) {
                state[i][j] ^= roundKeys[round * Nb + j][i];
            }
        }
    }

    // SubBytes: Substitute each byte in the state using the S-box
    private void subBytes(byte[][] state) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < Nb; j++) {
                state[i][j] = sBox[state[i][j] & 0xFF];
            }
        }
    }

    // ShiftRows: Rotate each row of the state matrix to the left
    private void shiftRows(byte[][] state) {
        byte temp;
        // Row 1: shift left by 1
        temp = state[1][0];
        state[1][0] = state[1][1];
        state[1][1] = state[1][2];
        state[1][2] = state[1][3];
        state[1][3] = temp;
        // Row 2: shift left by 2
        temp = state[2][0];
        state[2][0] = state[2][2];
        state[2][2] = temp;
        temp = state[2][1];
        state[2][1] = state[2][3];
        state[2][3] = temp;
        // Row 3: shift left by 3 (or right by 1)
        temp = state[3][3];
        state[3][3] = state[3][2];
        state[3][2] = state[3][1];
        state[3][1] = state[3][0];
        state[3][0] = temp;
    }

    // MixColumns: Mix the bytes in each column of the state
    private void mixColumns(byte[][] state) {
        byte[][] temp = new byte[4][Nb];
        for (int c = 0; c < Nb; c++) {
            temp[0][c] = (byte)(gmul((byte)0x02, state[0][c]) ^ gmul((byte)0x03, state[1][c]) ^ state[2][c] ^ state[3][c]);
            temp[1][c] = (byte)(state[0][c] ^ gmul((byte)0x02, state[1][c]) ^ gmul((byte)0x03, state[2][c]) ^ state[3][c]);
            temp[2][c] = (byte)(state[0][c] ^ state[1][c] ^ gmul((byte)0x02, state[2][c]) ^ gmul((byte)0x03, state[3][c]));
            temp[3][c] = (byte)(gmul((byte)0x03, state[0][c]) ^ state[1][c] ^ state[2][c] ^ gmul((byte)0x02, state[3][c]));
        }
        for (int i = 0; i < 4; i++) {
            System.arraycopy(temp[i], 0, state[i], 0, Nb);
        }
    }

    private byte gmul(byte a, byte b) {
        byte p = 0;
        for (int i = 0; i < 8; i++) {
            if ((b & 1) != 0) {
                p ^= a;
            }
            boolean hi_bit_set = (a & 0x80) != 0;
            a <<= 1;
            if (hi_bit_set) {
                a ^= 0x1B; // x^8 + x^4 + x^3 + x + 1
            }
            b >>= 1;
        }
        return p;
    }

    // The AES encryption function
    public byte[] encrypt(byte[] input) {
        byte[][] state = new byte[4][Nb];
        // Load input into the state array
        for (int i = 0; i < input.length; i++) {
            state[i % 4][i / 4] = input[i];
        }

        // Initial round
        addRoundKey(state, 0);

        // Main rounds
        for (int round = 1; round < Nr; round++) {
            subBytes(state);
            shiftRows(state);
            mixColumns(state);
            addRoundKey(state, round);
        }

        // Final round (without MixColumns)
        subBytes(state);
        shiftRows(state);
        addRoundKey(state, Nr);

        // Convert state array back to byte array
        byte[] output = new byte[16];
        for (int i = 0; i < output.length; i++) {
            output[i] = state[i % 4][i / 4];
        }

        return output;
    }

    // The AES decryption function (inverse steps)
    public byte[] decrypt(byte[] input) {
        byte[][] state = new byte[4][Nb];
        // Load input into the state array
        for (int i = 0; i < input.length; i++) {
            state[i % 4][i / 4] = input[i];
        }

        // Initial round
        addRoundKey(state, Nr);

        // Main rounds
        for (int round = Nr - 1; round > 0; round--) {
            invShiftRows(state);
            invSubBytes(state);
            addRoundKey(state, round);
            invMixColumns(state);
        }

        // Final round
        invShiftRows(state);
        invSubBytes(state);
        addRoundKey(state, 0);

        // Convert state array back to byte array
        byte[] output = new byte[16];
        for (int i = 0; i < output.length; i++) {
            output[i] = state[i % 4][i / 4];
        }

        return output;
    }

    private void invSubBytes(byte[][] state) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < Nb; j++) {
                state[i][j] = invSBox[state[i][j] & 0xFF];
            }
        }
    }

    private void invShiftRows(byte[][] state) {
        byte temp;
        // Row 1: shift right by 1
        temp = state[1][3];
        state[1][3] = state[1][2];
        state[1][2] = state[1][1];
        state[1][1] = state[1][0];
        state[1][0] = temp;
        // Row 2: shift right by 2
        temp = state[2][0];
        state[2][0] = state[2][2];
        state[2][2] = temp;
        temp = state[2][1];
        state[2][1] = state[2][3];
        state[2][3] = temp;
        // Row 3: shift right by 3 (or left by 1)
        temp = state[3][0];
        state[3][0] = state[3][1];
        state[3][1] = state[3][2];
        state[3][2] = state[3][3];
        state[3][3] = temp;
    }

    private void invMixColumns(byte[][] state) {
        byte[][] temp = new byte[4][Nb];
        for (int c = 0; c < Nb; c++) {
            temp[0][c] = (byte)(gmul((byte)0x0e, state[0][c]) ^ gmul((byte)0x0b, state[1][c]) ^ gmul((byte)0x0d, state[2][c]) ^ gmul((byte)0x09, state[3][c]));
            temp[1][c] = (byte)(gmul((byte)0x09, state[0][c]) ^ gmul((byte)0x0e, state[1][c]) ^ gmul((byte)0x0b, state[2][c]) ^ gmul((byte)0x0d, state[3][c]));
            temp[2][c] = (byte)(gmul((byte)0x0d, state[0][c]) ^ gmul((byte)0x09, state[1][c]) ^ gmul((byte)0x0e, state[2][c]) ^ gmul((byte)0x0b, state[3][c]));
            temp[3][c] = (byte)(gmul((byte)0x0b, state[0][c]) ^ gmul((byte)0x0d, state[1][c]) ^ gmul((byte)0x09, state[2][c]) ^ gmul((byte)0x0e, state[3][c]));
        }
        for (int i = 0; i < 4; i++) {
            System.arraycopy(temp[i], 0, state[i], 0, Nb);
        }
    }
}