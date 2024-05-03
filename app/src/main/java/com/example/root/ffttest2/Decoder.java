package com.example.root.ffttest2;

import android.app.Activity;
import android.app.UiAutomation;
import android.graphics.Bitmap;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.complex.Complex;

public class Decoder {
    public static void decode_helper(Activity av, double[] data, int[] valid_bins, int m_attempt) {
        //
        int ptime = (int)((Constants.preambleTime/1000.0)*Constants.fs);
        int start = ptime+Constants.ChirpGap;
        double[] rx_pilots;
        String coded = "";
        String received_symbol = "";
        //for (int j = 0 ; j < start + 16 * Constants.Ns_lora; j++)
        //{
        //    received_symbol += (data[j] + ",");
        //}
        //FileOperations.writetofile(MainActivity.av, received_symbol + "",
        //        Utils.genName(Constants.SignalType.RxSymbols, m_attempt) + ".txt");
        if (Constants.scheme == Constants.Modulation.OFDM_freq_adapt || Constants.scheme == Constants.Modulation.OFDM_freq_all )
        {
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

            rx_pilots=Utils.segment(data,start+Constants.Cp,start+Constants.Cp+Constants.Ns-1);
            start = start+Constants.Cp+Constants.Ns;
            short [] tx_symbol = SymbolGeneration.getTrainingSymbol(Utils.arange(valid_bins[0],valid_bins[1]));
            double [] tx_pilots = Utils.convert(tx_symbol);
            tx_pilots = Utils.segment(tx_pilots,Constants.Cp,Constants.Cp+Constants.Ns-1); //

            // obtain weights from frequency domain equalization
            double[][] tx_spec = Utils.fftcomplexoutnative_double(tx_pilots, tx_pilots.length);
            double[][] rx_spec = Utils.fftcomplexoutnative_double(rx_pilots, rx_pilots.length);
            double[][] weights = Utils.dividenative(tx_spec, rx_spec);
            double[][] recovered_pilot_sym = Utils.timesnative(rx_spec, weights);

            int numsyms = binFillOrder[0]; // number of data symbols
            double[][][] symbols = new double[numsyms + 1][][];
            symbols[0] = recovered_pilot_sym;

            for (int i = 0; i < numsyms; i++) {
                double[] sym = Utils.segment(data, start + Constants.Cp, start + Constants.Cp + Constants.Ns - 1);
                start = start + Constants.Cp + Constants.Ns;
                double[][] sym_spec = Utils.fftcomplexoutnative_double(sym, sym.length);
                sym_spec = Utils.timesnative(sym_spec, weights);
                symbols[i + 1] = sym_spec;
            }
            short[][] bits = Modulation.pskdemod_differential(symbols, valid_bins);
            // for each symbol reorder the bits that were shuffled from interleaving
            // extract bits from the symbol corresponding to valid data
            //String coded = "";
            for (int i = 0; i < bits.length; i++) {
                short[] newbits = bits[i];
                newbits = SymbolGeneration.unshuffle(bits[i], i);
                // extract the data bits
                for (int j = 0; j < binFillOrder[i + 1]; j++) {
                    coded += newbits[j] + "";
                }
            }
        }
        else if (Constants.scheme == Constants.Modulation.LoRa)
        {
            int bitsfill[] = SymbolGeneration.binFillOrder_LoRa();
            int numsyms = bitsfill[0]; // number of data symbols

            double[] data_remove_preamble = Utils.segment(data,start,start + (numsyms+4) * Constants.Ns_lora-1);
            start = 0;
            double[][] data_deinterleava = Utils.deInterleave(data_remove_preamble);
            data_deinterleava = Utils.downsample(data_deinterleava,Constants.Sample_Lora,Constants.Ns_lora / 2);

            int[] index_count = new int[4];
            double[] pks = new double[4];
            for (int i = 0 ; i < 2 ; i++)
            {
                double[][] preamble = Utils.segment2(data_deinterleava,start, start + Constants.Sample_Lora - 1);
                start = start + Constants.Sample_Lora;
                double[] index_upchirp = Utils.dechirp(preamble,false);
                int index_tmp = Utils.MaxIndex(index_upchirp);
                //index_count[i] = ((double)(index_tmp + Constants.bin_num_lora ) / Constants.zero_padding_ratio )% (Math.pow(2,Constants.SF));
                index_count[i] = index_tmp;
                pks[i] = Utils.MaxValue(index_upchirp);
            }

            for (int i = 2 ; i < 4 ; i++)
            {
                double[][] preamble = Utils.segment2(data_deinterleava,start, start + Constants.Sample_Lora - 1);
                start = start + Constants.Sample_Lora;
                double[] index_downchirp = Utils.dechirp(preamble,true);
                int index_tmp = Utils.MaxIndex(index_downchirp);
                //index_count[i] = ((double)(index_tmp + Constants.bin_num_lora ) / Constants.zero_padding_ratio )% Math.pow(2,Constants.SF);
                index_count[i] = index_tmp;
                pks[i] = Utils.MaxValue(index_downchirp);
            }




            int[] detected_index = new int[numsyms + 4];

            for (int j =0 ; j< 4; j++)
            {
                detected_index[j] = (int)index_count[j];
            }



            // extract each symbol
            for (int i = 0; i < numsyms; i++) {
                double[][] sym = Utils.segment2(data_deinterleava, start , start  +Constants.Sample_Lora - 1);
                start = start  + Constants.Sample_Lora;
                //double[][] sym_ori = Utils.deInterleave(sym);
                //sym_ori = Utils.downsample(sym_ori,(int)Math.pow(2,Constants.SF));
                double[] index = Utils.dechirp(sym,false);
                detected_index[i+4] = Utils.MaxIndex(index);
                //if (i < 5){
                    //String energy = "";
                    //for (int j = 0 ; j < index.length; j++)
                    //{
                    //    energy += (index[j] + ",");
                    //}
                    //Utils.log("energy =>" + energy);
                    //Utils.log("data_symbol =>" + detected_index[i+4]);
                //}
            }
            String all_symbol = "";
            for (int i = 0 ; i < detected_index.length; i++)
            {
                all_symbol += (detected_index[i] + ",");
            }
            Utils.log("all_symbols =>" + all_symbol);
            // demodulate the symbols to bits
            short[][] bits_lora = Utils.symbolsToBits(detected_index);

            //if (Constants.DIFFERENTIAL)
            //{
            //    for (int i = 2; i < bits_lora.length; i++)
            //    {
            //        short[] decoded = Modulation.differential_decoding(bits_lora[i-1],bits_lora[i]);
            //        for (int j = 0; j < decoded.length; j++)
            //        {
            //            bits_lora[i][j] = decoded[j];
            //        }
            //    }
            //}

            // gray coding
            if (Constants.GRAY_CODING)
            {
                for (int i = 4; i < bits_lora.length; i++)
                {
                    int tmp = Utils.BitsToSymbols(bits_lora[i]);
                    tmp = Utils.gray_coding(tmp);
                    short[] bits_after_gray = Utils.symbolsToBits(tmp);
                    for (int j = 0; j < bits_after_gray.length; j++)
                    {
                        bits_lora[i][j] = bits_after_gray[j];
                    }
                }
            }

            // for each symbol reorder the bits that were shuffled from interleaving
            // extract bits from the symbol corresponding to valid data
            //String coded = "";
            for (int i = 4; i < bits_lora.length; i++) {
                short[] newbits = bits_lora[i];
                //short[] newbits = SymbolGeneration.unshuffle(bits_lora[i], i);
                // extract the data bits
                for (int j = 0; j < bitsfill[i-3]; j++) {
                    coded += newbits[j] + "";
                }
            }
        }


        // perform viterbi decoding
        String uncoded = Utils.decode(coded, Constants.cc[0],Constants.cc[1],Constants.cc[2]);
        FileOperations.writetofile(MainActivity.av, uncoded + "",
                Utils.genName(Constants.SignalType.RxBits, m_attempt) + ".txt");


        // extract messageID from bits
        char meta = uncoded.charAt(0);



        //String receivedBits = "..."; // The string of bits you received


        // image
        //byte[] imageBytes = Utils.convertBitStringToByteArray(uncoded);
        //String image_byte = "";
        //for (int i = 0; i < imageBytes.length; i++)
        //{
        //    image_byte += (imageBytes[i] + ",");
        //}
        //Utils.log("received image byte =>"+image_byte);

        //Bitmap image = Utils.convertByteArrayToBitmap(imageBytes);
        //FileOperations.writetofile(MainActivity.av, imageBytes + "",
        //        "recevied_bytes.txt");
        // display message
        String message="Error";


        // fish application

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
        Utils.log("rx_bits_before_coding=>"+coded);
        Utils.log("rx_bits_after_coding =>"+ uncoded);
        av.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Utils.sendNotification(av, "Notification",message, R.drawable.warning2);
                //Utils.sendNotification(av, "Notification",finalMessage, R.drawable.warning2);
                Constants.msgview.setText(finalMessage);
                //Constants.imgview.setImageBitmap(image);
                Utils.sendNotification(av, "Notification","Image received", R.drawable.warning2);

            }
        });
    }

    public static int[] demodulate(double[] data, int m_attempt)
    {

        int ptime = (int)((Constants.preambleTime/1000.0)*Constants.fs);
        int start = ptime+Constants.ChirpGap;
        int numsyms = SymbolGeneration.calc_sym_num(Constants.EmbeddindBytes);


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

        double[] data_remove_preamble = Utils.segment(data,start,start + (numsyms+4) * Constants.Ns_lora-1);


        start = 0;
        double[][] downversion_preamble = Utils.downversion(data_remove_preamble);
        double[][] data_downsample = Utils.downsample(downversion_preamble,2 * Constants.Sample_Lora,Constants.Ns_lora);


        int[] index_count = new int[4];
        double[] index_count_test = new double[4];
        double[] pks = new double[4];
        for (int i = 0 ; i < 2 ; i++)
        {
            double[][] preamble = Utils.segment2(data_downsample,start , start + 2 * Constants.Sample_Lora - 1);
            start = start + 2 * Constants.Sample_Lora;
            double[] index_upchirp = Utils.dechirp_test(preamble,false);
            int index_tmp = Utils.MaxIndex(index_upchirp);
            double index_tmp_test = (((double)index_tmp + 10 * Constants.Sample_Lora - 1 ) / 10)% Constants.Sample_Lora;
            index_count[i] = index_tmp;
            index_count_test[i] = index_tmp_test;
            pks[i] = Utils.MaxValue(index_upchirp);
        }

        for (int i = 2 ; i < 4 ; i++)
        {
            double[][] preamble = Utils.segment2(data_downsample,start, start +  2 * Constants.Sample_Lora - 1);
            start = start +  2 * Constants.Sample_Lora;
            double[] index_downchirp = Utils.dechirp_test(preamble,true);
            int index_tmp = Utils.MaxIndex(index_downchirp);
            double index_tmp_test = (((double)index_tmp + 10 * Constants.Sample_Lora -1) / 10)% Constants.Sample_Lora;
            index_count[i] = index_tmp;
            index_count_test[i] = index_tmp_test;
            pks[i] = Utils.MaxValue(index_downchirp);
        }
        // frequency and time synchronization
        double[] off_set = Utils.synchronization2(index_count_test[1], index_count_test[2]);
        Utils.log("cfo =>" + off_set[0]);
        Utils.log("to =>" + off_set[1]);

        //Constants.CFO = off_set[0] * Constants.BW / Constants.Sample_Lora; confirmed
        int time_offset = (int)Math.round(Math.abs(off_set[1]) * Constants.Ns_lora / Constants.Sample_Lora);
        double[] data_remove_preamble_shift = Utils.segment(data,ptime+Constants.ChirpGap + 4* Constants.Ns_lora +time_offset,ptime+Constants.ChirpGap +time_offset+ (numsyms+4) * Constants.Ns_lora-1);
        downversion_preamble = Utils.downversion(data_remove_preamble_shift);
        data_downsample = Utils.downsample(downversion_preamble,2 * Constants.Sample_Lora,Constants.Ns_lora);
        start = 0;


        int[] detected_index_cfo = new int[numsyms + 4];
        int[] detected_index_cfo_sfo = new int[numsyms + 4];

        for (int j =0 ; j< 4; j++)
        {
            detected_index_cfo[j] = (int)index_count_test[j];
            detected_index_cfo_sfo[j] = (int)index_count_test[j];
        }



        // extract each symbol
        for (int i = 0; i < numsyms; i++) {
            double[][] sym = Utils.segment2(data_downsample, start , start  +2 * Constants.Sample_Lora - 1);
            start = start  + 2 * Constants.Sample_Lora;
            double[] index = Utils.dechirp_test(sym,false);
            double sym_index = Utils.MaxIndex(index) ;
            double index_tmp_test = (((double)sym_index + 10 * Constants.Sample_Lora -1) / 10)% Constants.Sample_Lora;
            //index_tmp_test = index_tmp_test - off_set[0] + off_set[1];
            index_tmp_test = index_tmp_test - off_set[0] ;
            index_tmp_test = Math.round(index_tmp_test) % Constants.Sample_Lora;

            detected_index_cfo[i+4] = (int)index_tmp_test;
            double sfo_offset = i * off_set[0] * Constants.Sample_Lora / Constants.FC;
            index_tmp_test -= sfo_offset;
            detected_index_cfo_sfo[i+4] = (int)Math.round(index_tmp_test);

        }
        String all_symbol_cfo = "";
        String all_symbol_cfo_sfo = "";
        for (int i = 0 ; i < detected_index_cfo.length; i++)
        {
            all_symbol_cfo += (detected_index_cfo[i] + ",");
            all_symbol_cfo_sfo += (detected_index_cfo_sfo[i] + ",");
        }
        Utils.log("all_symbols_cfo =>" + all_symbol_cfo);
        Utils.log("all_symbol_cfo_sfo =>" + all_symbol_cfo_sfo);
        if (all_symbol_cfo.endsWith(",")) {
            all_symbol_cfo = all_symbol_cfo.substring(0, all_symbol_cfo.length() - 1);
        }
        FileOperations.writetofile(MainActivity.av, all_symbol_cfo + "",
                Utils.genName(Constants.SignalType.Rx_Symbols, m_attempt) + ".txt");


        return detected_index_cfo;
    }

    public static void decoding(Activity av, double[] received_data, int m_attempt)
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

        av.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Utils.sendNotification(av, "Notification",message, R.drawable.warning2);
                //Utils.sendNotification(av, "Notification",finalMessage, R.drawable.warning2);
                //Constants.msgview.setText(finalMessage);
                //Constants.imgview.setImageBitmap(image);
                Utils.sendNotification(av, "Notification","Embedding received", R.drawable.warning2);

            }
        });

        //return data;
    }


    public static double[] movingAverageFilter(double[] input, int windowSize) {
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            double sum = 0;
            for (int j = 0; j < windowSize; j++) {
                if (i - j < 0) break;
                sum += input[i - j];
            }
            output[i] = sum / windowSize;
        }
        return output;
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
