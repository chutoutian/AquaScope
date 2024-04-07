package com.example.root.ffttest2;

import android.app.Activity;
import android.app.UiAutomation;
import android.graphics.Bitmap;

import java.util.ArrayList;

import org.apache.commons.math3.complex.Complex;

public class Decoder {
    public static void decode_helper(Activity av, double[] data, int[] valid_bins, int m_attempt) {
        //
        int ptime = (int)((Constants.preambleTime/1000.0)*Constants.fs);
        int start = ptime+Constants.ChirpGap;
        double[] rx_pilots;
        String coded = "";
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
            int[] index_count = new int[5];
            //int[] error_index_up = new int[Constants.Ns_lora /2 + 5];
            double[] pks_down = new double[5];
            //double[] pks_up = new double[Constants.Ns_lora /2 + 5];
            for (int i = 0; i<index_count.length  ; i++)
            {
                // test the different synchronization algorithm
                int start_test = ptime+Constants.ChirpGap;
                double[] preamble_upchirp = Utils.segment(data,start_test + i, start_test +i+ Constants.Ns_lora-1);
                double[][] preamble_upchirp_ori = Utils.deInterleave(preamble_upchirp);
                preamble_upchirp_ori = Utils.downsample(preamble_upchirp_ori,(int)Math.pow(2,Constants.SF));
                double[] index_upchirp_pk  = Utils.dechirp(preamble_upchirp_ori, false);
                //String upchirp_symbol = index_upchirp_pk + "";
                //Utils.log("upchirp_symbol =>" + upchirp_symbol);
                start_test = start_test + i + Constants.Ns_lora;
                //index_count[i * 2] = (int)index_upchirp_pk[0];
                //pks_down[i * 2] = index_upchirp_pk[1];

                double[] preamble_downchirp = Utils.segment(data,start_test , start_test+Constants.Ns_lora-1);
                double[][] preamble_downchirp_ori = Utils.deInterleave(preamble_downchirp);
                preamble_downchirp_ori = Utils.downsample(preamble_downchirp_ori,(int)Math.pow(2,Constants.SF));
                double[] index_downchirp_pk  = Utils.dechirp(preamble_downchirp_ori, false);
                //double[] index_upchirp_pk  = Utils.dechirp(preamble_downchirp_ori, true);
                index_count[i ] = (int)index_downchirp_pk[0];
                pks_down[i] = index_downchirp_pk[1];
                //error_index_up[i + error] = (int)index_upchirp_pk[0];
                //pks_up[i + error] = index_upchirp_pk[1];

            }
            //int offset = Utils.getMaxIndex(pks_down); //// is it good ?
            int offset = 0;
            start = start +  2* Constants.Ns_lora + offset;
            String index_test = "";
            String pks_test = "";
            for (int i = 0 ; i < index_count.length; i++)
            {
                index_test += (index_count[i] + ",");
                pks_test += (pks_down[i] + ",");
            }
            Utils.log("index =>" + index_test);
            Utils.log("pks =>" + pks_test );
            //String downchirp_symbol = "";
            //for (int i = 0 ; i < error_index.length; i++)
            //{
            //    downchirp_symbol += (error_index[i] + "");
            //}
            //Utils.log("downchirp_symbol =>" + downchirp_symbol);
            //start = start + Constants.Ns_lora;

            //rx_pilots=Utils.segment(data,start+Constants.Cp,start+Constants.Cp+Constants.Ns_lora-1);
            //double[][] rx_pilots_ori = Utils.deInterleave(rx_pilots);
            //start = start+Constants.Cp+Constants.Ns_lora;

            //short [] tx_symbol = SymbolGeneration.getTrainingSymbol(Utils.arange(valid_bins[0],valid_bins[1]));
            //double [] tx_pilots = Utils.convert(tx_symbol);
            //int[] test = new int[4];
            //tx_pilots = Utils.segment(tx_pilots,Constants.Cp ,Constants.Cp  +  Constants.Ns_lora-1); //
            //double[][] tx_pilots_ori = Utils.deInterleave(tx_pilots);
            //tx_pilots_ori = Utils.downsample(tx_pilots_ori,(int)Math.pow(2,Constants.SF));
            //test[0]  = Utils.dechirp(tx_pilots_ori, false);
            //for (int i = 1; i < 4; i++)
            //{
            //    tx_pilots_sub = Utils.segment(tx_pilots,Constants.Cp  + i * Constants.Ns_lora,Constants.Cp  + (i+1) * Constants.Ns_lora-1); //
            //    tx_pilots_ori = Utils.deInterleave(tx_pilots_sub);
            //    tx_pilots_ori = Utils.downsample(tx_pilots_ori,(int)Math.pow(2,Constants.SF));
            //    test[i]  = Utils.dechirp(tx_pilots_ori, false);

            //}



            String temp = "";
            for (int i = 0; i < Constants.maxbits; i++) {
                temp+="0";
            }
            int maxcodedbits = Constants.maxbits;
            if (Constants.CODING) {
                maxcodedbits = Utils.encode(temp, Constants.cc[0],Constants.cc[1],Constants.cc[2]).length();
            }

            int numsyms = (int) Math.ceil((double)maxcodedbits/ Constants.SF); // number of data symbols
            int bitsfill[] = new int[numsyms];
            for (int i = 0; i < numsyms; i++) {
                if (maxcodedbits % numsyms == 0)
                {
                    bitsfill[i] = Constants.SF;
                }
                else {
                    // If maxcodedbits % numsyms != 0
                    if (i < numsyms - (maxcodedbits % numsyms)) {
                        // For the first (numsyms - remainder) elements, set to Constants.SF
                        bitsfill[i] = Constants.SF;
                    } else {
                        // For the last remainder elements, set to 6
                        bitsfill[i] = Constants.SF - 1;
                    }
                }
            }

            int[] detected_index = new int[numsyms + 2];

            //rx_pilots_ori = Utils.downsample(rx_pilots_ori,(int)Math.pow(2,Constants.SF));
            //double[] index_received_test_bits  = Utils.dechirp(rx_pilots_ori, false);
            //detected_index[0] = (int)index_received_test_bits[0];

            //tx_pilots_ori = Utils.downsample(tx_pilots_ori,(int)Math.pow(2,Constants.SF));
            //double[] index_transmit_test_bits  = Utils.dechirp(tx_pilots_ori, false);
            //detected_index[1] = (int)index_transmit_test_bits[0];

            // extract each symbol and equalize with weights
            for (int i = 0; i < numsyms; i++) {
                double[] sym = Utils.segment(data, start , start  + Constants.Ns_lora - 1);
                start = start  + Constants.Ns_lora;
                double[][] sym_ori = Utils.deInterleave(sym);
                sym_ori = Utils.downsample(sym_ori,(int)Math.pow(2,Constants.SF));
                double[] index = Utils.dechirp(sym_ori,false);
                detected_index[i+2] = (int)index[0];

            }
            String all_symbol = "";
            for (int i = 0 ; i < detected_index.length; i++)
            {
                all_symbol += (detected_index[i] + ",");
            }
            Utils.log("all_symbols =>" + all_symbol);
            // demodulate the symbols to bits
            short[][] bits_lora = Utils.symbolsToBits(detected_index);
            //String test_transmit_bits = "";
            //String test_receive_bits = "";
            //for (int j = 0; j < Constants.SF; j++) {
            //    test_receive_bits += bits_lora[0][j] + "";
            //    test_transmit_bits += bits_lora[1][j] + "";
            //}
            //Utils.log("test_transmit_bits =>" + test_transmit_bits);
            //Utils.log("test_receive_bits =>" + test_receive_bits);

            // differential decoding
            if (Constants.DIFFERENTIAL)
            {
                for (int i = 2; i < bits_lora.length; i++)
                {
                    short[] decoded = Modulation.differential_decoding(bits_lora[i-1],bits_lora[i]);
                    for (int j = 0; j < decoded.length; j++)
                    {
                        bits_lora[i][j] = decoded[j];
                    }
                }
            }

            // gray coding
            if (Constants.GRAY_CODING)
            {
                for (int i = 2; i < bits_lora.length; i++)
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
            for (int i = 2; i < bits_lora.length; i++) {
                short[] newbits = bits_lora[i];
                //short[] newbits = SymbolGeneration.unshuffle(bits_lora[i], i);
                // extract the data bits
                for (int j = 0; j < bitsfill[i-2]; j++) {
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



        String receivedBits = "..."; // The string of bits you received


        // image
        //byte[] imageBytes = Utils.convertBitStringToByteArray(uncoded);
        //Bitmap image = Utils.convertByteArrayToBitmap(imageBytes);
        // FileOperations.writetofile(MainActivity.av, imageBytes + "",
        //        "recevied_bytes.txt");
        // display message
        String message="Error";


        //Utils.log(coded +"=>"+uncoded);
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
        //if (Constants.mmap.containsKey(messageID)) { message = Constants.mmap.get(messageID); }
        String finalMessage = message;
        Utils.log(coded +"=>"+uncoded+"=>"+message);
        av.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Utils.sendNotification(av, "Notification",message, R.drawable.warning2);
                Utils.sendNotification(av, "Notification",finalMessage, R.drawable.warning2);
                Constants.msgview.setText(finalMessage);
                //Constants.imgview.setImageBitmap(image);
                //Utils.sendNotification(av, "Notification","Image received", R.drawable.warning2);

            }
        });
    }

    public static int[] demodulate(double[] data, int m_attempt)
    {

        Constants.CFO = 0;
        // initialization
        Constants.bin_num = (int)Math.pow(2,Constants.SF) * Constants.zero_padding_ratio;
        Constants.sample_num = 2 * (int)Math.pow(2, Constants.SF);
        Constants.fft_length = Constants.sample_num * Constants.zero_padding_ratio;



        if ( (int)Math.pow(2,Constants.SF) / Constants.BW > 0.016)
        {
            Constants.LDR = 1;
        }
        else {
            Constants.LDR = 0;
        }
        //data = Utils.filter(data);
        //double[] sig_sampled = movingAverageFilter(data, 4);
        double[] sig_sampled = downsample(data, Constants.FS / (2 * Constants.BW));

        int x = 0;
        //while (x < sig_sampled.length)
        //{
            //x = detect(x,sig_sampled);
            //if (x < 0) break;
        //}



        return new int[0];
    }

    public static byte[] decoding(int[] symbol, int m_attempt)
    {
        return new byte[0];
    }

    public static double[] downsample(double[] filteredSignal, int factor) {
        if (factor <= 1) return filteredSignal;
        int newSize = (int) Math.ceil((double) filteredSignal.length / factor);
        double[] downsampled = new double[newSize];
        for (int i = 0; i < newSize; i++) {
            downsampled[i] = filteredSignal[i * factor];
        }
        return downsampled;
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
