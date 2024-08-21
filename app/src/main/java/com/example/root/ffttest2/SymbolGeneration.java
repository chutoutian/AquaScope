package com.example.root.ffttest2;

import static com.example.root.ffttest2.Constants.LOG;
import static com.example.root.ffttest2.Constants.besselFiltOffset;
import static com.example.root.ffttest2.Constants.tv2;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;

import org.apache.commons.math3.complex.Complex;

import java.util.Arrays;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

public class SymbolGeneration {
    public static short[] generatePreamble(short[] bits, int[] valid_carrier,
                                           int symreps, boolean preamble, Constants.SignalType sigType) {
        int numDataSyms = 0;
        if (valid_carrier.length > 0) {
            numDataSyms = (int) Math.ceil((double)bits.length/(valid_carrier.length));
        }

        int symlen = (Constants.Ns+Constants.Cp)*symreps + Constants.Gi;

        int siglen = symlen*numDataSyms;
        if (preamble) {
            siglen += ((Constants.preambleTime/1000.0)*Constants.fs)+Constants.ChirpGap;
        }
        short[] txsig = new short[siglen];

        int counter = 0;
        if (preamble) {
            // add preamble
            short[] preamble_sig = PreambleGen.preamble_s();
            for (Short s : preamble_sig) {
                txsig[counter++] = s;
            }
            counter += Constants.ChirpGap;
        }

        short[][] bit_list = new short[numDataSyms][valid_carrier.length];
        int bit_counter = 0;
        for (int i = 0; i < numDataSyms; i++) {
            int endpoint = bit_counter + valid_carrier.length-1;
            if (bit_counter + valid_carrier.length-1 > bits.length-1) {
                endpoint = bits.length-1;
            }
            // segment data bits to add to symbol
            short[] bits_seg = Utils.segment(bits,bit_counter,endpoint);
            if (i > 0) {
                // differential encoding
                bits_seg = transform_bits(bit_list[i-1], bits_seg);
            }
            bit_list[i] = bits_seg;
            // modulate bits into OFDM symbol
            short[] symbol = generate_helper(
                    bits_seg,
                    valid_carrier,
                    symreps,
                    sigType);
            bit_counter += valid_carrier.length;

            for (Short s : symbol) {
                txsig[counter++] = s;
            }
        }
        return txsig;
    }

    public static int[] binFillOrder(int[] valid_carrier) {
        int numrounds = 0;

        String temp = "";
        for (int i = 0; i < Constants.maxbits; i++) {
            temp+="0";
        }
        int maxcodedbits = Constants.maxbits;
        if (Constants.CODING) {
            maxcodedbits = Utils.encode(temp, Constants.cc[0],Constants.cc[1],Constants.cc[2]).length();
        }

        short[] bits = new short[maxcodedbits];

        int bit_counter = 0;
        if (valid_carrier.length > 0) {
            numrounds = (int) Math.ceil((double)maxcodedbits/ valid_carrier.length);
        }
        int[] out = new int[numrounds+1];
        out[0]=numrounds;
        for (int i = 0; i < numrounds; i++) {
            boolean oneMoreBin = i < bits.length % numrounds;

            int endpoint = (int) (bit_counter + Math.floor(bits.length / numrounds));
            if (!oneMoreBin) {
                endpoint -= 1;
            }

            short[] bits_seg = Utils.segment(bits, bit_counter, endpoint);

            short[] pad_bits = Utils.random_array(valid_carrier.length - bits_seg.length);
            Log.e("symbol", "sym " + i + ": " + bits_seg.length + "," + pad_bits.length);
            out[i+1]=bits_seg.length;
        }
        return out;
    }

    public static short[] generateDataSymbols_LoRa(int[] sym, boolean preamble,
                                                   int m_attempt)
    {
        int symlen = (Constants.Ns_lora + Constants.Gap)* sym.length;
        int siglen = symlen;
        if (preamble) {
            siglen += ((Constants.preambleTime/1000.0)*Constants.fs)+Constants.ChirpGap;
        }

        siglen += (Constants.Ns_lora + Constants.Gap) * 4;

        if (Constants.currentEqualizationMethod == Constants.NewEqualizationMethod.method3_tv_wo_to || Constants.currentEqualizationMethod == Constants.NewEqualizationMethod.method4_tv_w_to || Constants.currentEqualizationMethod == Constants.NewEqualizationMethod.method5_tv_w_to_range) {
            siglen += (Constants.Ns_lora + Constants.Gap) * (Math.floor(sym.length / Constants.Equalization2_Range)+1); // TODO: should add one more preamble at the end
        }
        short[] txsig = new short[siglen];

        int counter = 0;
        int counter_equalization = 0;
        if (preamble) {
            // add preamble
            short[] preamble_sig = PreambleGen.preamble_s();
            for (Short s : preamble_sig) {
                txsig[counter++] = s;
            }
            counter += Constants.ChirpGap;
        }

        short[] symbol;
        // add downchirp and upchirp
        short[] preamble_up_1 = Utils.GeneratePreamble_LoRa(true, 0, true);
        short[] preamble_up_2 = Utils.GeneratePreamble_LoRa(true, 0, true);
        short[] preamble_down_1 = Utils.GeneratePreamble_LoRa(false, 0, true);
        short[] preamble_down_2 = Utils.GeneratePreamble_LoRa(false, 0, true);

        if (Constants.scheme == Constants.Modulation.LoRa && Constants.currentEqualizationMethod != Constants.NewEqualizationMethod.method5_tv_w_to_range) {
            for (Short s : preamble_up_1) {
                txsig[counter++] = s;
            }
            counter += Constants.Gap;

            for (Short s : preamble_up_2) {
                txsig[counter++] = s;
            }
            counter += Constants.Gap;

            for (Short s : preamble_down_1) {
                txsig[counter++] = s;
            }
            counter += Constants.Gap;

            for (Short s : preamble_down_2) {
                txsig[counter++] = s;
            }
            counter += Constants.Gap;
        } else if (Constants.scheme == Constants.Modulation.LoRa && Constants.currentEqualizationMethod == Constants.NewEqualizationMethod.method5_tv_w_to_range) {
            // change to to a long gap
            for (Short s : preamble_up_1) {
                txsig[counter++] = 0;
            }
            counter += Constants.Gap;

            for (Short s : preamble_up_2) {
                txsig[counter++] = 0;
            }
            counter += Constants.Gap;

            for (Short s : preamble_down_1) {
                txsig[counter++] = 0;
            }
            counter += Constants.Gap;

            for (Short s : preamble_down_2) {
                txsig[counter++] = 0;
            }
            counter += Constants.Gap;

        }

        short[] equalization_preamble = Utils.GenerateEqualizationPreamble_LoRa();
//        short[] equalization2_preamble = Utils.GeneratePreamble_LoRa(true, 0, true);
        // try to use the preamble
        short[] equalization2_preamble = PreambleGen.preamble_s();
        equalization2_preamble = Utils.segment(equalization2_preamble, 0, 0 + Constants.Ns_lora - 1);

//        for (Short s: equalization_preamble) {
//            Utils.logd("short s:" + s);
//        }

        counter_equalization = counter;


        for (int i = 0; i < sym.length; i++) {

            // insert pilot for equalization 2
            if ((Constants.currentEqualizationMethod == Constants.NewEqualizationMethod.method3_tv_wo_to || Constants.currentEqualizationMethod == Constants.NewEqualizationMethod.method4_tv_w_to || Constants.currentEqualizationMethod == Constants.NewEqualizationMethod.method5_tv_w_to_range) && (i % Constants.Equalization2_Range == 0)) {
                for (Short s : equalization2_preamble) {
                    txsig[counter++] = s;
                }
                counter += Constants.Gap;
            }

            symbol = Utils.GeneratePreamble_LoRa(true, sym[i], false);
            // test
            //symbol = Utils.GeneratePreamble_LoRa(true, 0);
            for (Short s : symbol) {
                txsig[counter++] = s;
            }
            counter += Constants.Gap;

        }

        // add equalization
        if (Constants.currentEqualizationMethod == Constants.NewEqualizationMethod.method2_new_freq) {
            while (counter_equalization < counter) {
                for (Short s : equalization_preamble) {
                    txsig[counter_equalization] = (short) (txsig[counter_equalization] * Constants.Equalization_Original_Ratio + s * Constants.Equalization_Part_Ratio);
                    counter_equalization++;
                }
                counter_equalization += Constants.Equalization_Gap;
            }
        }

//        for (Short s : preamble_up_1) {
//            txsig[counter++] = s;
//        }
//        counter += Constants.Gap;
//
//        for (Short s : preamble_up_2) {
//            txsig[counter++] = s;
//        }
//        counter += Constants.Gap;
//
//        for (Short s : preamble_down_1) {
//            txsig[counter++] = s;
//        }
//        counter += Constants.Gap;
//
//        for (Short s : preamble_down_2) {
//            txsig[counter++] = s;
//        }
//        counter += Constants.Gap;
        return txsig;
    }
    public static short[] generateDataSymbols(short[] bits, int[] valid_carrier,
                                              int symreps, boolean preamble, Constants.SignalType sigType,
                                              int m_attempt) {
        int numrounds = 0;
        int symlen = 0;
        if (valid_carrier.length > 0) {
            numrounds = (int) Math.ceil((double)bits.length/valid_carrier.length);
        }
        Log.e("sym",bits.length+","+valid_carrier.length+","+numrounds+","+Constants.Ns+","+Constants.subcarrier_number_default);

        symlen = (Constants.Ns+Constants.Cp)*symreps + Constants.Gi;




        int siglen = symlen*numrounds;
        if (preamble) {
            siglen += ((Constants.preambleTime/1000.0)*Constants.fs)+Constants.ChirpGap;
        }
        siglen += symlen;
        short[] txsig = new short[siglen];

        int counter = 0;
        if (preamble) {
            // add preamble
            short[] preamble_sig = PreambleGen.preamble_s();
            for (Short s : preamble_sig) {
                txsig[counter++] = s;
            }
            counter += Constants.ChirpGap;
        }



        short[][] bit_list = null;
        short[] symbol;


        // add training symbol
        short[] training_bits = new short[0];
        bit_list = new short[numrounds+1][valid_carrier.length];
        training_bits = Utils.segment(Constants.pn60_bits, 0, valid_carrier.length - 1);
        symbol = generate_helper(
                training_bits,
                valid_carrier,
                symreps,
                sigType);
        for (Short s : symbol) {
            txsig[counter++] = s;
        }
        bit_list[0] = training_bits;

        int bit_counter = 0;
        Log.e("symbol", sigType.toString());
        Log.e("symbol", "# bits "+bits.length);
        Log.e("symbol", "# carriers "+valid_carrier.length);
        Log.e("symbol", "# symbols "+numrounds);

        String bitsWithPadding = "";
        String bitsWithoutPadding = "";
        String numberOfDataBits = "";

        for (int i = 0; i < numrounds; i++) {
            boolean oneMoreBin = i < bits.length%numrounds;

            int endpoint = (int)(bit_counter + Math.floor(bits.length/numrounds));
            if (!oneMoreBin) {
                endpoint -= 1;
            }

            short[] bits_seg = Utils.segment(bits,bit_counter,endpoint);
            numberOfDataBits += bits_seg.length+", ";
            bitsWithoutPadding += Utils.trim(Arrays.toString(bits_seg))+", ";

            short[] pad_bits = new short[0];
            pad_bits = Utils.random_array(valid_carrier.length-bits_seg.length);


            Log.e("symbol", "sym "+i+": "+bits_seg.length+","+pad_bits.length);

            short[] tx_bits = Utils.concat_short(bits_seg,pad_bits);
            bitsWithPadding += Utils.trim(Arrays.toString(tx_bits))+", ";

            if (Constants.INTERLEAVE) {
                shuffleArray(tx_bits, i);
            }

            if (Constants.DIFFERENTIAL) {
                tx_bits = transform_bits(bit_list[i], tx_bits);
                for (int j = 0; j < tx_bits.length; j++) {
                    bit_list[i+1][j] = tx_bits[j];
                }
            }


            symbol = generate_helper(
                    tx_bits,
                    valid_carrier,
                    symreps,
                    sigType);
            bit_counter += bits_seg.length;

            for (Short s : symbol) {
                txsig[counter++] = s;
            }
        }

        //FileOperations.writetofile(MainActivity.av, Utils.trim_end(bitsWithPadding),
        //        Utils.genName(Constants.SignalType.BitsAdapt_Padding, m_attempt) + ".txt");
        //FileOperations.writetofile(MainActivity.av, Utils.trim_end(bitsWithoutPadding),
        //        Utils.genName(Constants.SignalType.BitsAdapt, m_attempt) + ".txt");
        FileOperations.writetofile(MainActivity.av, Utils.trim_end(numberOfDataBits),
                Utils.genName(Constants.SignalType.Bit_Fill_Adapt, m_attempt) + ".txt");

        return txsig;
    }

    static void shuffleArray(short[] ar, int seed) {
        Random rnd = new Random(seed);
        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            short a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    static short[] unshuffle(short[] ar, int seed) {
        short[] tempArray = new short[ar.length];
        for (int i = 0; i < tempArray.length; i++) {
            tempArray[i] = (short)i;
        }
        shuffleArray(tempArray,seed);

        short[] out = new short[ar.length];
        for (int i = 0; i < ar.length; i++) {
            int index = tempArray[i];
            out[index] = ar[i];
        }
        return out;
    }

    public static short[] getTrainingSymbol(int[] valid_carrier) {
        short[] training_bits = new short[0];
        training_bits = Utils.segment(Constants.pn60_bits, 0, valid_carrier.length - 1);
        short[] symbol = generate_helper(
                training_bits,
                valid_carrier,
                1,
                Constants.SignalType.DataAdapt);
        return symbol;
    }

    public static short[] transform_bits(short[] last_bits, short[] bits) {
        short[] newbits= new short[bits.length];
        for (int i = 0; i < bits.length; i++) {
            if (last_bits[i] != bits[i]) {
                newbits[i] = 1;
            }
        }
        return newbits;
    }

    public static short[] mod(int bound1, int bound2, int[] valid_carrier, short[] bits, int subnum, Constants.SignalType sigType) {
        double[][] mod;
        mod = Modulation.pskmod(bits);
        if (bits.length < subnum) {
            bound2 = bound1+bits.length-1;
        }

        double[][] sig = new double[2][Constants.Ns];
        int counter=0;
        for (int i = bound1; i <= bound2; i++) {
            if (contains(valid_carrier, i)) {
                sig[0][i] = mod[0][counter];
                sig[1][i] = mod[1][counter++];
            }
        }

        double[][] symbol_complex = Utils.ifftnative2(sig);

        short[] symbol = new short[symbol_complex[0].length];
        double divval=(bound2-bound1)+1;
        if (sigType.equals(Constants.SignalType.Sounding)) {
            divval = divval/2;
        }
        for (int i = 0; i < symbol.length; i++) {
            symbol[i] = (short)((symbol_complex[0][i]/(double)divval)*32767.0);
        }




        return symbol;
    }

    // generate one symbol
    public static short[] generate_helper(short[] bits, int[] valid_carrier, int symreps, Constants.SignalType sigType) {
        int bound1=0;
        int bound2=0;
        int subnum=0;
        if (sigType.equals(Constants.SignalType.Sounding)) {
            bound1 = Constants.nbin1_default;
            bound2 = Constants.nbin2_default;
            subnum=Constants.subcarrier_number_chanest;
        }
        else if (sigType.equals(Constants.SignalType.DataAdapt)||
                sigType.equals(Constants.SignalType.DataFull_1000_4000)||
                sigType.equals(Constants.SignalType.DataFull_1000_2500)||
                sigType.equals(Constants.SignalType.DataFull_1000_1500)) {
            bound1 = valid_carrier[0];
            bound2 = valid_carrier[valid_carrier.length-1];
            subnum = bound2-bound1+1;
        }

        short[] symbol = mod(bound1, bound2, valid_carrier, bits, subnum, sigType);
        short[] flipped_symbol=null;
        if(Constants.FLIP_SYMBOL) {flipped_symbol = mod(bound1, bound2, valid_carrier, Utils.flip(bits), subnum, sigType);}

        int datacounter = 0;
        short[] out = null;

        if (sigType.equals(Constants.SignalType.DataAdapt)||
                sigType.equals(Constants.SignalType.DataFull_1000_4000)||
                sigType.equals(Constants.SignalType.DataFull_1000_2500)||
                sigType.equals(Constants.SignalType.DataFull_1000_1500)) {
            int long_cp = Constants.Cp * symreps;
            short[] cp = new short[long_cp];
            for (int i = 0; i < long_cp; i++) {
                cp[i] = symbol[(symbol.length - long_cp - 1) + i];
            }

            out = new short[symbol.length*symreps + long_cp + Constants.Gi];

            for (int j = 0; j < cp.length; j++) {
                out[datacounter++] = cp[j];
            }
            for (int i = 0; i < symreps; i++) {
                for (int j = 0; j < symbol.length; j++) {
                    out[datacounter++] = symbol[j];
                }
            }
        }
        else if (sigType.equals(Constants.SignalType.Sounding)) {
            int long_cp = Constants.Cp;
            short[] cp = new short[long_cp];
            for (int i = 0; i < long_cp; i++) {
                cp[i] = symbol[(symbol.length - long_cp - 1) + i];
            }

            out = new short[(symbol.length+long_cp)*symreps + Constants.Gi];

            for (int i = 0; i < symreps; i++) {
                for (int j = 0; j < cp.length; j++) {
                    out[datacounter++] = cp[j];
                }
                for (int j = 0; j < symbol.length; j++) {
                    out[datacounter++] = symbol[j];
                }
            }
        }


        return out;
    }

    public static boolean contains(int[] data, int k) {
        for (Integer i : data) {
            if (k==i) {
                return true;
            }
        }
        return false;
    }

    public static short[] getCodedBits(int m_attempt,byte[] imagebyte) {
        //String uncoded = Utils.pad2(Integer.toBinaryString(Constants.messageID));
        // fishing identification or counting
        int fish_information;
        String meta = "";
        String uncoded = "";
        if (Constants.IsDectectingFish){
            meta = "1";
            fish_information = Constants.IsFish ? 1 : 0;
            uncoded = meta + Utils.pad2(Integer.toBinaryString(fish_information));
        }
        else if (Constants.IsCountingFish){
            meta = "0";
            fish_information = Constants.NumFish;
            uncoded = meta + Utils.pad2(Integer.toBinaryString(fish_information));
        }

        uncoded = Utils.convertByteArrayToBitString(imagebyte);
        FileOperations.writetofile(MainActivity.av, uncoded + "",
                Utils.genName(Constants.SignalType.TxBits, m_attempt) + ".txt");


        String coded = "";
        if (Constants.CODING) {
            coded = Utils.encode(uncoded, Constants.cc[0],Constants.cc[1],Constants.cc[2]);
        }
        else {
            coded = uncoded;
        }

        Utils.log("tx_bits_before_coding=>"+uncoded);
        Utils.log("tx_bits_after_coding =>"+ coded);
        return Utils.convert(coded);
    }



    public static int[] encode_LoRa(byte[] payload, int m_attempt){

        byte[] data;
        data = payload;

        int plen = payload.length;
        int sym_num = calc_sym_num(plen); // calc_sym_num needs to be implemented
        int nibble_num = Constants.SF - 2 + (sym_num - 8) / (Constants.CodeRate_LoRA + 4) * Constants.SF;
        byte[] data_w = new byte[(int)Math.ceil((double)(nibble_num - 2 * data.length) / 2) + data.length];
        System.arraycopy(data, 0, data_w, 0, data.length);
        for (int i = data.length; i < data_w.length; i++) {
            data_w[i] = (byte)255;
        }

        data_w = whiten(data_w, plen); // whiten needs to be implemented, adjusted signature for example

        byte[] data_nibbles = new byte[nibble_num];
        for (int i = 0; i < nibble_num; i++) {
            int idx = i / 2;
            if (i % 2 == 0) {
                data_nibbles[i] = (byte)(data_w[idx] & 0x0F);
            } else {
                data_nibbles[i] = (byte)((data_w[idx] & 0xF0) >> 4);
            }
        }

        byte[] header_nibbles = Constants.HasHead ? gen_header(plen) : new byte[0]; // gen_header needs to be implemented this is where left Mar. 3
        //byte[] test_byte = {1,2,3,4,5};
        byte[] codewords = hamming_encode(concatArrays(header_nibbles, data_nibbles)); // hamming_encode needs to be implemented, concatArrays is a helper method
        //byte[] codewords = hamming_encode(test_byte);
        int[] symbols_i = new int[]{};
        if (Constants.SF - 2 > 0)
        {
            symbols_i = diag_interleave(Arrays.copyOfRange(codewords, 0, Constants.SF - 2),  8); // Adjusted to include start index and length
        }


        int ppm = Constants.SF;
        int rdd = Constants.CodeRate_LoRA + 4;
        for (int i = Constants.SF - 1 -1; i < codewords.length - ppm + 1; i += ppm) {
            byte[] subset = Arrays.copyOfRange(codewords, i, i+ppm);
            int[] part = diag_interleave(subset, rdd);
            symbols_i = Utils.concatArrays_int(symbols_i, part);
        }
        int[] symbols = gray_decoding2(symbols_i);

        return symbols;

    }

    private static byte[] calc_crc(byte[] data) {
        byte[] checksum;
        switch (data.length) {
            case 0:
                checksum = new byte[]{0, 0};
                break;
            case 1:
                checksum = new byte[]{data[data.length - 1], 0};
                break;
            case 2:
                checksum = new byte[]{data[data.length - 1], data[data.length - 2]};
                break;
            default:
                byte[] input = Arrays.copyOf(data, data.length - 2);
                // Assuming crc_generator returns a byte array representing the CRC sequence
                // and that you implement the conversion from byte array to binary sequence and vice versa.
                // seq = self.crc_generator(reshape(logical(de2bi(input, 8, 'left-msb'))', [], 1)); matlab implementation
                boolean[] converted_input = Utils.byteToBinaryBooleanArray(input);
                int crc = crc_generator(converted_input); // toBitArray needs to be implemented
                byte checksum_b1 = (byte) ((crc >> 8) & 0xFF);
                byte checksum_b2 = (byte) (crc & 0xFF);
                checksum = new byte[]{(byte) (checksum_b2 ^ data[data.length - 1]), (byte) (checksum_b1 ^ data[data.length - 2])};
        }
        return checksum;
    }

    private static int crc_generator(boolean[] input) {
        // Implement your CRC generation logic here
        int crc = 0x0000; // Initial value
        for (boolean bit : input) {
            int bitVal = bit ? 1 : 0; // Convert boolean to int (1 or 0)
            int msb = (crc >> 15) & 1; // Most significant bit of crc
            crc = ((crc << 1) | bitVal) & 0xFFFF; // Shift crc left, add current bit, mask to 16 bits

            if (msb != 0) {
                crc ^= 0x1021; // If msb was 1 before shift, XOR with polynomial
            }
        }

        // Additional processing for padding, as per CCITT specification
        for (int i = 0; i < 16; i++) {
            int msb = (crc >> 15) & 1; // Most significant bit of crc
            crc = (crc << 1) & 0xFFFF; // Shift crc left, mask to 16 bits

            if (msb != 0) {
                crc ^= 0x1021; // If msb was 1 before shift, XOR with polynomial
            }
        }

        return crc;
    }
    public static int calc_sym_num(int plen) {
        // Implementation needed
        //int crcFactor = Constants.CRC ? 1 : 0; // Convert CRC flag to an integer factor
        int headerFactor = Constants.HasHead ? 0 : 1; // Convert header presence flag to an integer factor for subtraction
        return 8 + Math.max((4 + Constants.CodeRate_LoRA) * (int) Math.ceil((double) (2 * plen - Constants.SF + 7  - 5 * headerFactor) / (double) Constants.SF), 0);
    }

    private static byte[] whiten(byte[] data, int plen) {
        byte[] dataW = new byte[data.length];
        for (int i = 0; i < plen; i++) {
            dataW[i] = (byte) (data[i] ^ Constants.whiteningSeq[i % Constants.whiteningSeq.length]);
        }
        for (int i = plen; i < data.length; i++){
            dataW[i] = data[i];
        }
        return dataW;
    }

    public static byte[] dewhiten(byte[] data)
    {
        int len = data.length;
        byte[] bytes_w = new byte[len];
        for (int i =0; i<len; i++)
        {
            bytes_w[i] = (byte) (data[i] ^ Constants.whiteningSeq[i % Constants.whiteningSeq.length]);
        }
        return bytes_w;
    }

    private static byte[] gen_header(int plen) {
        // Implementation needed
        byte[] headerBytes = new byte[5];
        headerBytes[0] = (byte) ((plen >> 4) & 0x0F); // High nibble
        headerBytes[1] = (byte) (plen & 0x0F); // Low nibble
        headerBytes[2] = (byte) ((2 * Constants.CodeRate_LoRA | (Constants.CRC ? 1 : 0)) & 0x0F);

        byte[] binaryVector = new byte[3];
        System.arraycopy(headerBytes, 0, binaryVector, 0, 3);
        int[] converted_binaryVector = Utils.nibbleToBinaryBooleanArray(binaryVector);


        // Perform the matrix multiplication in GF(2)
        int[] resultVector = new int[Constants.headerChecksumMatrix.length];
        for (int row = 0; row < Constants.headerChecksumMatrix.length; row++) {
            for (int col = 0; col < converted_binaryVector.length; col++) {
                resultVector[row] ^= (Constants.headerChecksumMatrix[row][col] * converted_binaryVector[col]);
            }
            // Since we are in GF(2), ensure the result is modulo 2
            resultVector[row] = resultVector[row] % 2;
        }

        headerBytes[3] = (byte) resultVector[0];
        for (int i = 0; i < 4; i++) {
            headerBytes[4] |= resultVector[i + 1] << (3 - i);
        }


        return headerBytes; // Placeholder
    }

    public static int[] hamming_decode(int[] codewords,int rdd)
    {

        int[] nibbles = new int[codewords.length];
        for (int i = 0; i < codewords.length; i++)
        {
            int codeword = codewords[i];
            byte p1 = bitReduce((byte)codeword, new int[]{8, 4, 3,1});
            byte p2 = bitReduce((byte)codeword, new int[]{7,4,2,1});
            byte p3 = bitReduce((byte)codeword, new int[]{5,3,2,1});
            byte p4 = bitReduce((byte)codeword, new int[]{5,4,3,2,1});
            byte p5 = bitReduce((byte)codeword, new int[]{6,4,3,2});

            switch (rdd){
                case 4:
                    break;
                case 5:
                case 6:
                    nibbles[i] = codewords[i] % 16;
                    break;
                case 7:
                case 8:
                    int partity = p2 * 4 + p3 * 2 + p5;
                    int pf = parityFix(partity);
                    codewords[i] = codewords[i] ^ pf;
                    nibbles[i] = codewords[i] % 16;
                    break;
            }
        }
        return nibbles;
    }
    public static Map<String, int[]>hamming_decode2(int[] codewords,int rdd)
    {

        int[] nibbles = new int[codewords.length];
        int[] parity_check = new int[codewords.length];
        for (int i = 0; i < codewords.length; i++)
        {
            int codeword = codewords[i];
            byte p1 = bitReduce((byte)codeword, new int[]{8, 4, 3,1});
            byte p2 = bitReduce((byte)codeword, new int[]{7,4,2,1});
            byte p3 = bitReduce((byte)codeword, new int[]{5,3,2,1});
            byte p4 = bitReduce((byte)codeword, new int[]{5,4,3,2,1});
            byte p5 = bitReduce((byte)codeword, new int[]{6,4,3,2});

            switch (rdd){
                case 4:
                    break;
                case 5:
                case 6:
                    nibbles[i] = codewords[i] % 16;
                    break;
                case 7:
                case 8:
                    int partity = p2 * 4 + p3 * 2 + p5;
                    parity_check[i] = partity;
                    int pf = parityFix(partity);
                    codewords[i] = codewords[i] ^ pf;
                    nibbles[i] = codewords[i] % 16;
                    break;
            }
        }

        Map<String, int[]> result = new HashMap<>();
        result.put("nibbles", nibbles);
        result.put("parity_check", parity_check);
        return result;
    }
    private static byte[] hamming_encode(byte[] nibbles) {
        int nibbleNum = nibbles.length;
        byte[] codewords = new byte[nibbleNum];

        for (int i = 0; i < nibbleNum; i++) {
            byte nibble = nibbles[i];
            byte p1 = bitReduce(nibble, new int[]{1, 3, 4});
            byte p2 = bitReduce(nibble, new int[]{1, 2, 4});
            byte p3 = bitReduce(nibble, new int[]{1, 2, 3});
            byte p4 = bitReduce(nibble, new int[]{1, 2, 3, 4});
            byte p5 = bitReduce(nibble, new int[]{2, 3, 4});

            int crNow = i < Constants.SF - 2 ? 4 : Constants.CodeRate_LoRA; // Adjust coding rate based on position

            switch (crNow) {
                case 0:
                    break;
                case 1:
                    codewords[i] = (byte) ((p4 << 4) | nibble);
                    break;
                case 2:
                    codewords[i] = (byte) ((p5 << 5) | (p3 << 4) | nibble);
                    break;
                case 3:
                    codewords[i] = (byte) ((p2 << 6) | (p5 << 5) | (p3 << 4) | nibble);
                    break;
                case 4:
                    codewords[i] = (byte) ((p1 << 7) | (p2 << 6) | (p5 << 5) | (p3 << 4) | nibble);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid Code Rate!");
            }
        }

        return codewords;
    }

    private  static  byte bitReduce(byte nibble, int[] positions) {
        byte result = 0;
        for (int pos : positions) {
            result ^= (nibble >> (pos - 1)) & 1; // Adjust for zero-based indexing
        }
        return result;
    }

    private  static  byte[] bitReduce(byte[] codewords, int[] positions) {
        byte[] result = new byte[codewords.length];
        for (int i = 0; i< codewords.length; i++)
        {
            for (int pos : positions) {
                result[i] ^= (codewords[i] >> (pos - 1)) & 1; // Adjust for zero-based indexing
            }
        }
        return result;
    }
    // Helper method to concatenate two byte arrays
    private static byte[] concatArrays(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private static int[] diag_interleave(byte[] codewords, int rdd) {
        int tmp[][] = Utils.nibbleToBinaryBooleanArray_right(codewords,rdd);

        int rows = tmp.length;
        int columns = tmp[0].length;
        int[][] tempColumn = new int[rows][columns];

        for (int x = 0; x < columns; x++) {
            int shiftAmount =  -x;

            // Copy and shift column by column
            for (int i = 0; i < rows; i++) {
                int newIndex = i + shiftAmount;

                // Correctly handle upward and downward shifts with wrap-around
                while (newIndex < 0) newIndex += rows;  // Upward shift with wrap-around
                while (newIndex >= rows) newIndex -= rows;  // Downward shift with wrap-around

                // Apply the shift
                tempColumn[newIndex][x] = tmp[i][x];
            }

            // Copy the shifted column back to the matrix
            for (int i = 0; i < rows; i++) {
                tmp[i][x] = tempColumn[i][x];
            }
        }

        int[] symbol_i = Utils.binaryMatrixToDecimal(tmp);
        for (int i = 0; i < symbol_i.length; i++) {
            // Ensure the value fits into the uint16 range
            symbol_i[i] = symbol_i[i] & 0xFFFF;
        }


        return symbol_i;
    }

    public static int[] diag_deinterleave(int[] symbols, int ppm) {
        int[][] binary = new int[symbols.length][ppm];

        // Convert symbols to binary
        for (int i = 0; i < symbols.length; i++) {
            binary[i] = toBinaryArray(symbols[i], ppm);
        }

        // Diagonal deinterleaving
        for (int i = 0; i < binary.length; i++) {
            binary[i] = circularShift(binary[i],  - i);
        }

        int[][] transpose_binary = new int[ppm][symbols.length];
        for (int i = 0;i<ppm;i++){
            for (int j = 0; j<symbols.length;j++)
            {
                transpose_binary[i][j] = binary[j][i];
            }
        }

        // Convert binary rows back to decimal
        int[] codewords = new int[transpose_binary.length];
        for (int i = 0; i < transpose_binary.length; i++) {
            codewords[i] = toDecimal(transpose_binary[i]);
        }

        // Flip the array upside down
        reverse(codewords);

        return codewords;
    }


    public static int[] gray_coding(int[] symbols)
    {
        int[] symbol_g = new int[symbols.length];
        for (int i = 0; i < Math.min(8, symbols.length); i++) {
            symbol_g[i] = symbols[i] / 4; // Java does integer division like floor for positive numbers
        }
        // Handle the remaining elements
        for (int i = 8; i < symbols.length; i++) {
            symbol_g[i] = (symbols[i] - 1) % (1 << Constants.SF); // 2^sf is computed as (1 << sf)
        }
        // Convert to Gray code
        for (int i = 0; i < symbol_g.length; i++) {
            symbol_g[i] = symbol_g[i] ^ (symbol_g[i] >> 1);
        }
        // Print the result (for example purposes, we'll use System.out.println)
        return symbol_g;
    }


    private static int[] gray_decoding2(int[] symbols_i) {
        int[] symbols = new int[symbols_i.length];
        for (int i = 0; i < symbols_i.length; i++) {
            int num = symbols_i[i];
            int mask = num >>> 1; // Logical right shift, equivalent to MATLAB's bitshift(num, -1) in java >>> only applies to int and long;
            while (mask != 0) {
                num = num ^ mask; // XOR operation, equivalent to MATLAB's bitxor
                mask = mask >>> 1; // Logical right shift
            }
            if (i < 8) {
                symbols[i] = (num * 4 + 1) % (int)Math.pow(2, Constants.SF);
            } else {
                symbols[i] = (num + 1) % (int)Math.pow(2, Constants.SF);
            }
        }
        return symbols;
    }


    private static int[] toBinaryArray(int number, int size) {
        int[] binary = new int[size];
        for (int i = size - 1; i >= 0; i--) {
            binary[i] = (number >> (size - 1 - i)) & 1;
        }
        return binary;
    }

    private static int[] circularShift(int[] array, int shift) {
        int len = array.length;
        int[] shifted = new int[len];
        for (int i = 0; i < len; i++) {
            shifted[i] = array[(i - shift + len) % len];
        }
        return shifted;
    }

    private static int toDecimal(int[] binary) {
        int number = 0;
        for (int i = 0; i < binary.length; i++) {
            number += binary[i] << i;
        }
        return number;
    }

    private static void reverse(int[] array) {
        for (int i = 0, j = array.length - 1; i < j; i++, j--) {
            int temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    public static int parityFix(int p) {
        switch (p) {
            case 3: // 011 wrong b3
                return 4;
            case 5: // 101 wrong b4
                return 8;
            case 6: // 110 wrong b1
                return 1;
            case 7: // 111 wrong b2
                return 2;
            default:
                return 0;
        }
    }
}
