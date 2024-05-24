package com.example.root.ffttest2;

import android.app.Activity;
import android.app.UiAutomation;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.complex.Complex;

public class Decoder {
    public static void decode_helper(Activity av, double[] data, int[] valid_bins, int m_attempt) {

        // write the raw data into file
        StringBuilder rxRawSymbolBuilder = new StringBuilder();
        for (int j = 0; j < data.length; j++) {
            rxRawSymbolBuilder.append(data[j]);
            rxRawSymbolBuilder.append(",");
        }
        String rx_raw_symbol = rxRawSymbolBuilder.toString();
        if (rx_raw_symbol.endsWith(",")) {
            rx_raw_symbol = rx_raw_symbol.substring(0, rx_raw_symbol.length() - 1);
        }
        FileOperations.writetofile(MainActivity.av, rx_raw_symbol + "",
                Utils.genName(Constants.SignalType.Rx_Raw_Symbols, m_attempt) + ".txt");

        data = Utils.filter(data);

        valid_bins[0]=valid_bins[0]+Constants.nbin1_default;
        valid_bins[1]=valid_bins[1]+Constants.nbin1_default;

        // bin fill order
        // element 0 => number of transmitted data symbols
        // element 1...n => number of bits in a symbol corresponding to data bits (the remaining are padding bits)
        int[] binFillOrder = SymbolGeneration.binFillOrder(Utils.arange(valid_bins[0],valid_bins[1]));

        // extract pilot symbols from the first OFDM symbol
        // compare this to the transmitted the transmitted pilot symbols
        // and perform frequency domain equalization
        int ptime = (int)((Constants.preambleTime/1000.0)*Constants.fs);
        int start = ptime+Constants.ChirpGap;
        double[] rx_pilots=Utils.segment(data,start+Constants.Cp,start+Constants.Cp+Constants.Ns-1);
        start = start+Constants.Cp+Constants.Ns;

        double [] tx_pilots = Utils.convert(SymbolGeneration.getTrainingSymbol(Utils.arange(valid_bins[0],valid_bins[1])));
        tx_pilots = Utils.segment(tx_pilots,Constants.Cp,Constants.Cp+Constants.Ns-1);

        // obtain weights from frequency domain equalization
        double[][] tx_spec = Utils.fftcomplexoutnative_double(tx_pilots, tx_pilots.length);
        double[][] rx_spec = Utils.fftcomplexoutnative_double(rx_pilots, rx_pilots.length);
        double[][] weights = Utils.dividenative(tx_spec, rx_spec);
        double[][] recovered_pilot_sym = Utils.timesnative(rx_spec, weights);

        // differential decoding
        int numsyms = binFillOrder[0]; // number of data symbols
        double[][][] symbols = new double[numsyms + 1][][];
        symbols[0] = recovered_pilot_sym;

        // extract each symbol and equalize with weights
        for (int i = 0; i < numsyms; i++) {
            double[] sym = Utils.segment(data, start + Constants.Cp, start + Constants.Cp + Constants.Ns - 1);
            start = start + Constants.Cp + Constants.Ns;

            double[][] sym_spec = Utils.fftcomplexoutnative_double(sym, sym.length);
            sym_spec = Utils.timesnative(sym_spec, weights);
            symbols[i + 1] = sym_spec;
        }

        // demodulate the symbols to bits
        short[][] bits = Modulation.pskdemod_differential(symbols, valid_bins);

        // for each symbol reorder the bits that were shuffled from interleaving
        // extract bits from the symbol corresponding to valid data
        String coded = "";
        for (int i = 0; i < bits.length; i++) {
            short[] newbits = bits[i];
            newbits = SymbolGeneration.unshuffle(bits[i], i);
            // extract the data bits
            for (int j = 0; j < binFillOrder[i + 1]; j++) {
                coded += newbits[j] + "";
            }
        }

        // perform viterbi decoding
        String uncoded = Utils.decode(coded, Constants.cc[0],Constants.cc[1],Constants.cc[2]);
        FileOperations.writetofile(MainActivity.av, uncoded + "",
                Utils.genName(Constants.SignalType.RxBits, m_attempt) + ".txt");


        byte[] received_bytes = Utils.convertBitStringToByteArray(uncoded);
        long[] embedding = Utils.Bytes2Embedding(received_bytes);
        String all_embedding = "";
        for (int i = 0 ; i < embedding.length; i++)
        {
            all_embedding += (embedding[i] + ",");
        }
        Utils.log("all_embedding =>" + all_embedding);
        if (all_embedding.endsWith(",")) {
            all_embedding = all_embedding.substring(0, all_embedding.length() - 1);
        }
        FileOperations.writetofile(MainActivity.av, all_embedding + "",
                Utils.genName(Constants.SignalType.Rx_Embedding, m_attempt) + ".txt");

        /***
        String message="Error";


        // fish application
        // extract messageID from bits
        char meta = uncoded.charAt(0);
        if (meta == '1')
        {
            String data_bits = uncoded.substring(4);
            int messageID=Integer.parseInt(data_bits,2);
            if (messageID == 1){
                message = "Fish Detected";
            }
            else {
                message = "No Fish Detected";
            }
        }
        else{
            String data_bits = uncoded.substring(2);
            int messageID=Integer.parseInt(data_bits,2);
            message = "Detected " + messageID + " Fish" ;
        }

         String finalMessage = message;
         ***/

        String finalMessage = "Embedding received #" + m_attempt;
        Utils.log("rx_bits_before_coding=>"+coded);
        Utils.log("rx_bits_after_coding =>"+ uncoded);
        av.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Utils.sendNotification(av, "Notification",message, R.drawable.warning2);
                //Utils.sendNotification(av, "Notification",finalMessage, R.drawable.warning2);
                Constants.msgview.setText(finalMessage);
                //Constants.imgview.setImageBitmap(image);
                Utils.sendNotification(av, "Notification","Embedding received", R.drawable.warning2);

            }
        });
    }

    public static int[] demodulate(double[] data, int m_attempt)
    {
        // preamble for detection
        int ptime = (int)((Constants.preambleTime/1000.0)*Constants.fs);
        int start = ptime+Constants.ChirpGap;
        int numsyms = SymbolGeneration.calc_sym_num(Constants.EmbeddindBytes);

        // write into files
        StringBuilder rxRawSymbolBuilder = new StringBuilder();
        for (int j = 0; j < data.length; j++) {
            rxRawSymbolBuilder.append(data[j]);
            rxRawSymbolBuilder.append(",");
        }
        String rx_raw_symbol = rxRawSymbolBuilder.toString();
        if (rx_raw_symbol.endsWith(",")) {
            rx_raw_symbol = rx_raw_symbol.substring(0, rx_raw_symbol.length() - 1);
        }
        FileOperations.writetofile(MainActivity.av, rx_raw_symbol + "",
                Utils.genName(Constants.SignalType.Rx_Raw_Symbols, m_attempt) + ".txt");


        //
        double[] data_remove_preamble = Utils.segment(data,start,start + (numsyms+8) * (Constants.Ns_lora + Constants.Gap)-1);


        start = 0;
        double[][] downversion_preamble = Utils.downversion(data_remove_preamble);
        //double[][] data_downsample = Utils.downsample(downversion_preamble,2 * Constants.Sample_Lora,(Constants.Ns_lora + Constants.Gap));

        // check downversion_preamble
//        StringBuilder downversion_preambleBuilder = new StringBuilder();
//        for (int j = 0; j < 20; j++) {
//            downversion_preambleBuilder.append(downversion_preamble[0][j]);
//            downversion_preambleBuilder.append(",");
//        }
//        String downversion_preamble_str = downversion_preambleBuilder.toString();
//        Utils.log("downversion first 10 => " + downversion_preamble_str);

        double[] index_count_test = new double[4];
        //double[] pks = new double[4];
        for (int i = 0 ; i < 2 ; i++)
        {
            double[][] preamble = Utils.segment2(downversion_preamble,start , start + Constants.Ns_lora - 1);
            double[][] preamble_downsample = Utils.downsample(preamble, 2* Constants.Sample_Lora, Constants.Ns_lora);
            start = start + Constants.Ns_lora + Constants.Gap;
            double[] index_upchirp = Utils.dechirp_test(preamble_downsample,false);
            int index_tmp = Utils.MaxIndex(index_upchirp);
            //double index_tmp_test = (((double)index_tmp + 10 * Constants.Sample_Lora - 1 ) / 10)% Constants.Sample_Lora; // which one is better?
            double index_tmp_test = ((double)index_tmp  / 10.0)% Constants.Sample_Lora;
            index_count_test[i] = index_tmp_test;
            //pks[i] = Utils.MaxValue(index_upchirp);
        }

        for (int i = 2 ; i < 4 ; i++)
        {
            double[][] preamble = Utils.segment2(downversion_preamble,start , start + Constants.Ns_lora - 1);
            double[][] preamble_downsample = Utils.downsample(preamble, 2* Constants.Sample_Lora, Constants.Ns_lora);
            start = start + Constants.Ns_lora + Constants.Gap;
            double[] index_downchirp = Utils.dechirp_test(preamble_downsample,true);
            int index_tmp = Utils.MaxIndex(index_downchirp);
            //double index_tmp_test = (((double)index_tmp + 10 * Constants.Sample_Lora -1) / 10)% Constants.Sample_Lora;
            double index_tmp_test = ((double)index_tmp  / 10.0)% Constants.Sample_Lora;
            index_count_test[i] = index_tmp_test;
            //pks[i] = Utils.MaxValue(index_downchirp);
        }
        // frequency and time synchronization
        double[] off_set = Utils.synchronization2(index_count_test[1], index_count_test[3]);
        Utils.log("cfo =>" + off_set[0]);
        Utils.log("to =>" + off_set[1]);

        //Constants.CFO = off_set[0] * Constants.BW / Constants.Sample_Lora; confirmed
        int time_offset = (int)Math.round(Math.abs(off_set[1]) * Constants.Ns_lora / Constants.Sample_Lora);
        double[] data_remove_preamble_shift = Utils.segment(data,ptime+Constants.ChirpGap + 4* (Constants.Ns_lora + Constants.Gap) +time_offset,ptime+Constants.ChirpGap +time_offset+ (numsyms+4 + 4) * (Constants.Ns_lora+ Constants.Gap)-1);
        downversion_preamble = Utils.downversion(data_remove_preamble_shift);
        //data_downsample = Utils.downsample(downversion_preamble,2 * Constants.Sample_Lora,Constants.Ns_lora);
        start = 0;


        int[] detected_index_cfo = new int[numsyms + 4];

        for (int j =0 ; j< 4; j++)
        {
            detected_index_cfo[j] = (int)Math.round(index_count_test[j]);
        }



        // extract each symbol
        for (int i = 0; i < numsyms / 2; i++) {
            double[][] sym = Utils.segment2(downversion_preamble, start , start  + Constants.Ns_lora - 1);
            double[][] sym_downsample = Utils.downsample(sym, 2* Constants.Sample_Lora, Constants.Ns_lora);
            start = start  + Constants.Ns_lora + Constants.Gap;
            double[] index = Utils.dechirp_test(sym_downsample,false);
            double sym_index = Utils.MaxIndex(index);
            //double index_tmp_test = (((double)sym_index + 10 * Constants.Sample_Lora -1) / 10)% Constants.Sample_Lora;
            double index_tmp_test = (sym_index  / 10.0)% Constants.Sample_Lora;
            index_tmp_test = index_tmp_test - off_set[0];
            index_tmp_test = Math.round(index_tmp_test) % Constants.Sample_Lora;

            detected_index_cfo[i+4] = (int)index_tmp_test;

        }
        // remove the mid preamble
//        for (int i = numsyms/ 2 ; i < numsyms/ 2 + 2 ; i++)
//        {
//            double[][] preamble = Utils.segment2(downversion_preamble,start , start + Constants.Ns_lora - 1);
//            double[][] preamble_downsample = Utils.downsample(preamble, 2* Constants.Sample_Lora, Constants.Ns_lora);
//            start = start + Constants.Ns_lora + Constants.Gap;
//            double[] index_upchirp = Utils.dechirp_test(preamble_downsample,false);
//            int index_tmp = Utils.MaxIndex(index_upchirp);
//            //double index_tmp_test = (((double)index_tmp + 10 * Constants.Sample_Lora - 1 ) / 10)% Constants.Sample_Lora; // which one is better?
//            double index_tmp_test = ((double)index_tmp  / 10.0)% Constants.Sample_Lora;
//            index_tmp_test = index_tmp_test - off_set[0] ;
//            index_tmp_test = Math.round(index_tmp_test) % Constants.Sample_Lora;
//            index_count_test[i - numsyms/ 2] = index_tmp_test;
//            //pks[i] = Utils.MaxValue(index_upchirp);
//        }
//
//        for (int i = numsyms/ 2 + 2 ; i < numsyms/ 2 + 4 ; i++)
//        {
//            double[][] preamble = Utils.segment2(downversion_preamble,start , start + Constants.Ns_lora - 1);
//            double[][] preamble_downsample = Utils.downsample(preamble, 2* Constants.Sample_Lora, Constants.Ns_lora);
//            start = start + Constants.Ns_lora + Constants.Gap;
//            double[] index_downchirp = Utils.dechirp_test(preamble_downsample,true);
//            int index_tmp = Utils.MaxIndex(index_downchirp);
//            //double index_tmp_test = (((double)index_tmp + 10 * Constants.Sample_Lora -1) / 10)% Constants.Sample_Lora;
//            double index_tmp_test = ((double)index_tmp  / 10.0)% Constants.Sample_Lora;
//            index_tmp_test = index_tmp_test - off_set[0] ;
//            index_tmp_test = Math.round(index_tmp_test) % Constants.Sample_Lora;
//            index_count_test[i - numsyms/ 2] = index_tmp_test;
//            //pks[i] = Utils.MaxValue(index_downchirp);
//        }
        // frequency and time synchronization
//        off_set = Utils.synchronization2(index_count_test[1], index_count_test[3]);
//        Utils.log("cfo =>" + off_set[0]);
//        Utils.log("to =>" + off_set[1]);

        //Constants.CFO = off_set[0] * Constants.BW / Constants.Sample_Lora; confirmed
//        time_offset = time_offset +  (int)Math.round(Math.abs(off_set[1]) * Constants.Ns_lora / Constants.Sample_Lora);

        data_remove_preamble_shift = Utils.segment(data,ptime+Constants.ChirpGap + (8 + numsyms/ 2)* (Constants.Ns_lora + Constants.Gap) +time_offset,ptime+Constants.ChirpGap +time_offset+ (numsyms+8) * (Constants.Ns_lora+ Constants.Gap)-1);
        downversion_preamble = Utils.downversion(data_remove_preamble_shift);
//        data_downsample = Utils.downsample(downversion_preamble,2 * Constants.Sample_Lora,Constants.Ns_lora);
        start = 0;

        for (int i = 0; i < numsyms / 2; i++) {
            double[][] sym = Utils.segment2(downversion_preamble, start , start  + Constants.Ns_lora - 1);
            double[][] sym_downsample = Utils.downsample(sym, 2* Constants.Sample_Lora, Constants.Ns_lora);
            start = start  + Constants.Ns_lora + Constants.Gap;
            double[] index = Utils.dechirp_test(sym_downsample,false);
            double sym_index = Utils.MaxIndex(index);
            //double index_tmp_test = (((double)sym_index + 10 * Constants.Sample_Lora -1) / 10)% Constants.Sample_Lora;
            double index_tmp_test = (sym_index  / 10.0)% Constants.Sample_Lora;
            index_tmp_test = index_tmp_test - off_set[0] ;
            index_tmp_test = Math.round(index_tmp_test) % Constants.Sample_Lora;

            detected_index_cfo[numsyms/ 2 + i+4] = (int)index_tmp_test;

        }



        // write to file and display in log
        String all_symbol_cfo = "";
        for (int i = 0 ; i < detected_index_cfo.length; i++)
        {
            all_symbol_cfo += (detected_index_cfo[i] + ",");
        }
        Utils.log("all_symbols_cfo =>" + all_symbol_cfo);
        if (all_symbol_cfo.endsWith(",")) {
            all_symbol_cfo = all_symbol_cfo.substring(0, all_symbol_cfo.length() - 1);
        }
        FileOperations.writetofile(MainActivity.av, all_symbol_cfo + "",
                Utils.genName(Constants.SignalType.Rx_Symbols, m_attempt) + ".txt");


        return detected_index_cfo;
    }

    public static long[] decoding(Activity av, double[] received_data, int m_attempt)
    {
        //received_data = Utils.filter(received_data);
        int[] symbol = demodulate(received_data,m_attempt);

        int[] symbol_remove_preamble = Utils.segment(symbol,4,symbol.length-1);
        // gray coding
        int[] symbol_g = SymbolGeneration.gray_coding(symbol_remove_preamble);
        // deinterleaving
        int[] codewords = SymbolGeneration.diag_deinterleave(Arrays.copyOfRange(symbol_g, 0, 8),  Constants.SF - 2);
        // hamming decoding
        int[] nibbles = SymbolGeneration.hamming_decode(codewords,8);
        int rdd = Constants.CodeRate_LoRA + 4;
        for (int i = 8; i < symbol_g.length - rdd + 1; i += rdd)
        {
            codewords = SymbolGeneration.diag_deinterleave(Arrays.copyOfRange(symbol_g, i, i+ rdd ), Constants.SF - 2 * Constants.LDR);
            int[] tem_nibbles = SymbolGeneration.hamming_decode(codewords,rdd);
            nibbles = Utils.concatArrays_int(nibbles, tem_nibbles);
        }
        // convert nibbles to the bytes
        int byteCount = Math.min(255,nibbles.length /2 );
        byte[] bytes = new byte[byteCount];
        for (int i = 0; i < byteCount; i++) {
            int firstNibble = nibbles[2 * i];       // 2*i because of zero-based indexing in Java
            int secondNibble = 16 * nibbles[2 * i + 1];  // 2*i+1 to get the next nibble

            // Shift the first nibble left by 4 bits and OR with the second nibble
            bytes[i] = (byte) (firstNibble | secondNibble);
        }

        int len = 80;
        // dewhitening
        byte[] data = SymbolGeneration.dewhiten(Arrays.copyOfRange(bytes, 0, len ));


        long[] embedding = Utils.Bytes2Embedding(data);
        String all_embedding = "";
        for (int i = 0 ; i < embedding.length; i++)
        {
            all_embedding += (embedding[i] + ",");
        }
        Utils.log("all_embedding =>" + all_embedding);
        if (all_embedding.endsWith(",")) {
            all_embedding = all_embedding.substring(0, all_embedding.length() - 1);
        }
        FileOperations.writetofile(MainActivity.av, all_embedding + "",
                Utils.genName(Constants.SignalType.Rx_Embedding, m_attempt) + ".txt");

        String finalMessage = "Embedding received #" + m_attempt;
        av.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Utils.sendNotification(av, "Notification",message, R.drawable.warning2);
                //Utils.sendNotification(av, "Notification",finalMessage, R.drawable.warning2);
                Constants.msgview.setText(finalMessage);
                //Constants.imgview.setImageBitmap(image);
                Utils.sendNotification(av, "Notification","Embedding received", R.drawable.warning2);

            }
        });

        return embedding;
    }

//    public static int detect(int start_index, double[] sampled_signal){
//         int ii = start_index;
//         ArrayList<Integer> pk_bin_list = new ArrayList<>();
//         while (ii < sampled_signal.length - Constants.sample_num * Constants.preamble_length)
//         {
//             if (pk_bin_list.size() == Constants.preamble_length - 1)
//             {
//                 int lastIndex = pk_bin_list.get(pk_bin_list.size() - 1);
//                 int x = ii - Math.round((lastIndex - 1) / (float) Constants.zero_padding_ratio * 2);
//                 return x;
//             }
//             int[] pk0 = Utils.dechirp(ii,false);
//             if (!pk_bin_list.isEmpty()) {
//                 int bin_diff = Math.floorMod(pk_bin_list.get(pk_bin_list.size() - 1) - pk0[1], Constants.bin_num);
//                 if (bin_diff > Constants.bin_num / 2) {
//                     bin_diff = Constants.bin_num - bin_diff;
//                 }
//                 if (bin_diff <= Constants.zero_padding_ratio) {
//                     pk_bin_list.add(pk0[1]);
//                 } else {
//                     pk_bin_list.clear();
//                     pk_bin_list.add(pk0[1]);
//                 }
//             } else {
//                 pk_bin_list.add(pk0[1]);
//             }
//
//             ii += Constants.sample_num;
//         }
//         return -1;
//    }
}
