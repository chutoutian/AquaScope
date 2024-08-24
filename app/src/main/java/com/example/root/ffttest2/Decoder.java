package com.example.root.ffttest2;

import android.app.Activity;
import android.os.SystemClock;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;

import Jama.Matrix;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.HashMap;
import java.util.Map;
public class Decoder {
    // for ofdm decoding
    public static long[]  decode_helper(Activity av, double[] data, int[] valid_bins, int m_attempt) {

        // write the raw data into file
        if (Constants.allowLog) {
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
        }

        final long startTime_decode = SystemClock.elapsedRealtime();

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
        if (Constants.allowLog) {
            FileOperations.writetofile(MainActivity.av, uncoded + "",
                    Utils.genName(Constants.SignalType.RxBits, m_attempt) + ".txt");
        }


        byte[] received_bytes = Utils.convertBitStringToByteArray(uncoded);
        long[] embedding = Utils.Bytes2Embedding(received_bytes);

        final long inferenceTime_time_decode = SystemClock.elapsedRealtime() - startTime_decode;
        Constants.Receiver_Latency_Str = Constants.Receiver_Latency_Str + "receiver demodulate + decode (signal to embedding) signal time (ms): " + inferenceTime_time_decode + "\n";
        Utils.log("Receiver_Latency_Str: " + Constants.Receiver_Latency_Str);

        if (Constants.allowLog) {
            String all_embedding = "";
            for (int i = 0; i < embedding.length; i++) {
                all_embedding += (embedding[i] + ",");
            }
            Utils.log("all_embedding =>" + all_embedding);
            FileOperations.writetofile(MainActivity.av, Arrays.toString(embedding),
                    Utils.genName(Constants.SignalType.Rx_Embedding, m_attempt) + ".txt");
        }

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
        return embedding;
    }

    public static void update_symbol_error_count(int[] rx_symbols) {
        int[] new_rx_symbols = Arrays.copyOfRange(rx_symbols, 4, rx_symbols.length);

        int diff_count_raw = countDifferentElementsAtSamePosition_Symbol(Constants.gt_symbols_for_text_exp, new_rx_symbols);

        Constants.symbol_error_count_view.post(new Runnable() {
            @Override
            public void run() {
                Constants.symbol_error_count_view.setText(
                        "Symbol Error Count: " + diff_count_raw + " / " + new_rx_symbols.length +
                                " (" + Math.round((diff_count_raw / (float) new_rx_symbols.length) * 10000.0f) / 100.0f + "%)"
                );            }
        });
    }


    public static int countDifferentElementsAtSamePosition_Symbol(int[] array1, int[] array2) {
        // Ensure both arrays have the same length to compare corresponding elements
        if (array1.length != array2.length) {
            throw new IllegalArgumentException("Arrays must be of the same length");
        }

        int count = 0;

        // Iterate through the arrays and compare elements at the same index
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                count++;
            }
        }
        return count;
    }

    public static int[] demodulate(double[] data, int m_attempt)
    {
        // preamble for detection
        int ptime = (int)((Constants.preambleTime/1000.0)*Constants.fs);
        int start = ptime+Constants.ChirpGap;
        int numsyms = SymbolGeneration.calc_sym_num(Constants.EmbeddindBytes);

        // write into files
        if (Constants.allowLog) {
            StringBuilder rxRawSymbolBuilder = new StringBuilder();
            for (int j = 0; j < data.length; j++) {
                rxRawSymbolBuilder.append(data[j]);
                rxRawSymbolBuilder.append("\n");
            }
            String rx_raw_symbol = rxRawSymbolBuilder.toString();
//            if (rx_raw_symbol.endsWith(",")) {
//                rx_raw_symbol = rx_raw_symbol.substring(0, rx_raw_symbol.length() - 1);
//            }
            FileOperations.writetofile(MainActivity.av, rx_raw_symbol + "",
                    Utils.genName(Constants.SignalType.Rx_Raw_Symbols, m_attempt) + ".txt");
        }

        // TODO improve performance
        double[] data_remove_preamble = Utils.segment(data,start,start + (numsyms+4) * (Constants.Ns_lora + Constants.Gap)-1);

        // new 0711 we will not use the timeoffset in our latest method
        if (Constants.scheme == Constants.Modulation.LoRa && Constants.currentEqualizationMethod != Constants.NewEqualizationMethod.method5_tv_w_to_range) {
            start = 0;

            double[][] downversion_preamble = Utils.downversion(data_remove_preamble);
            //double[][] data_downsample = Utils.downsample(downversion_preamble,2 * Constants.Sample_Lora,(Constants.Ns_lora + Constants.Gap));

//         check downversion_preamble
            if (Constants.allowLog) {
                StringBuilder downversion_preambleBuilder = new StringBuilder();
                for (int j = 0; j < 20; j++) {
                    downversion_preambleBuilder.append(downversion_preamble[0][j]);
                    downversion_preambleBuilder.append(",");
                    downversion_preambleBuilder.append(downversion_preamble[1][j]);
                    downversion_preambleBuilder.append(",");
                }
                String downversion_preamble_str = downversion_preambleBuilder.toString();
                Utils.log("downversion first 10, check filter => " + downversion_preamble_str);
            }

            double[] index_count_test = new double[4];
            //double[] pks = new double[4];
            for (int i = 0; i < 2; i++) {
                double[][] preamble = Utils.segment2(downversion_preamble, start, start + Constants.Ns_lora - 1);
                double[][] preamble_downsample = Utils.downsample(preamble, 2 * Constants.Sample_Lora, Constants.Ns_lora);
                start = start + Constants.Ns_lora + Constants.Gap;
                double[] index_upchirp = Utils.dechirp_test(preamble_downsample, false, true);
                int index_tmp = Utils.MaxIndex(index_upchirp);
                //double index_tmp_test = (((double)index_tmp + 10 * Constants.Sample_Lora - 1 ) / 10)% Constants.Sample_Lora; // which one is better?
                double index_tmp_test = ((double) index_tmp / 10.0) % Constants.Sample_Lora;
                index_count_test[i] = index_tmp_test;
                //pks[i] = Utils.MaxValue(index_upchirp);
            }

            for (int i = 2; i < 4; i++) {
                double[][] preamble = Utils.segment2(downversion_preamble, start, start + Constants.Ns_lora - 1);
                double[][] preamble_downsample = Utils.downsample(preamble, 2 * Constants.Sample_Lora, Constants.Ns_lora);
                start = start + Constants.Ns_lora + Constants.Gap;
                double[] index_downchirp = Utils.dechirp_test(preamble_downsample, true, true);
                int index_tmp = Utils.MaxIndex(index_downchirp);
                //double index_tmp_test = (((double)index_tmp + 10 * Constants.Sample_Lora -1) / 10)% Constants.Sample_Lora;
                double index_tmp_test = ((double) index_tmp / 10.0) % Constants.Sample_Lora;
                index_count_test[i] = index_tmp_test;
                //pks[i] = Utils.MaxValue(index_downchirp);
            }
            // frequency and time synchronization
            double[] off_set = Utils.synchronization2(index_count_test[1], index_count_test[3]);
//      double[] off_set2 = Utils.synchronization2(index_count_test[0], index_count_test[2]);


            Utils.log("first index  =>" + index_count_test[1]);
            Utils.log("second index  =>" + index_count_test[3]);
            Utils.log("cfo =>" + off_set[0]);
            Utils.log("to =>" + off_set[1]);

            //Constants.CFO = off_set[0] * Constants.BW / Constants.Sample_Lora; confirmed
//        int time_offset = (int)Math.round(Math.abs(off_set[1]) * Constants.Ns_lora / Constants.Sample_Lora); // remove abs
            int time_offset = (int) Math.round(off_set[1] * Constants.Ns_lora / Constants.Sample_Lora);

            double[] data_remove_preamble_shift = Utils.segment(data, ptime + Constants.ChirpGap + 4 * (Constants.Ns_lora + Constants.Gap) + time_offset, ptime + Constants.ChirpGap + time_offset + (numsyms + 4) * (Constants.Ns_lora + Constants.Gap) - 1);

            downversion_preamble = Utils.downversion(data_remove_preamble_shift);
            //data_downsample = Utils.downsample(downversion_preamble,2 * Constants.Sample_Lora,Constants.Ns_lora);
            start = 0;


            int[] detected_index_cfo = new int[numsyms + 4];

            for (int j = 0; j < 4; j++) {
                detected_index_cfo[j] = (int) Math.round(index_count_test[j]);
            }

            // extract each symbol
            for (int i = 0; i < numsyms; i++) {
                double[][] sym = Utils.segment2(downversion_preamble, start, start + Constants.Ns_lora - 1);
                double[][] sym_downsample = Utils.downsample(sym, 2 * Constants.Sample_Lora, Constants.Ns_lora);
                start = start + Constants.Ns_lora + Constants.Gap;
                double[] index = Utils.dechirp_test(sym_downsample, false, false);
                double sym_index = Utils.MaxIndex(index);
                //double index_tmp_test = (((double)sym_index + 10 * Constants.Sample_Lora -1) / 10)% Constants.Sample_Lora;
                double index_tmp_test = (sym_index / 10.0) % Constants.Sample_Lora;
                index_tmp_test = index_tmp_test - off_set[0];
                index_tmp_test = Math.round(index_tmp_test) % Constants.Sample_Lora;
                if (index_tmp_test < 0) {
                    index_tmp_test += Constants.Sample_Lora;
                }
                detected_index_cfo[i + 4] = (int) index_tmp_test;

            }
            // write to file and display in log
            if (Constants.allowLog) {
                String all_symbol_cfo = "";
                for (int i = 0; i < detected_index_cfo.length; i++) {
                    all_symbol_cfo += (detected_index_cfo[i] + ",");
                }
                Utils.log("all_symbols_cfo =>" + all_symbol_cfo);
                if (all_symbol_cfo.endsWith(",")) {
                    all_symbol_cfo = all_symbol_cfo.substring(0, all_symbol_cfo.length() - 1);
                }
                FileOperations.writetofile(MainActivity.av, all_symbol_cfo + "",
                        Utils.genName(Constants.SignalType.Rx_Symbols, m_attempt) + ".txt");
            }

            update_symbol_error_count(detected_index_cfo);

            return detected_index_cfo;
        }
        else {
            // method 5
            int time_offset = 0;

            int delay2 = (int) Math.ceil(3.6*Constants.FS/1000.0)-1;
            delay2 = delay2%2 + delay2;
            delay2 = (int) delay2 / 2;
            double[] data_remove_preamble_shift = Utils.segment(data, ptime + Constants.ChirpGap + 4 * (Constants.Ns_lora + Constants.Gap) + time_offset, ptime + Constants.ChirpGap + time_offset + (numsyms + 4) * (Constants.Ns_lora + Constants.Gap) - 1 + delay2);

//            int delay2 = 86;  // for the specific center 0 and offset freq 1k, and FS 48000

            double[][] downversion_preamble = Utils.downversion(data_remove_preamble_shift);
            downversion_preamble[0] = Utils.segment(downversion_preamble[0], delay2, downversion_preamble[0].length-1);
            downversion_preamble[1] = Utils.segment(downversion_preamble[1], delay2, downversion_preamble[1].length-1);

            //data_downsample = Utils.downsample(downversion_preamble,2 * Constants.Sample_Lora,Constants.Ns_lora);
            start = 0;


            int[] detected_index_cfo = new int[numsyms + 4];

            for (int j = 0; j < 4; j++) {
                detected_index_cfo[j] = 0; // fake
            }

            // extract each symbol
            Utils.logd("ddddebug" + numsyms);
            Utils.logd("ddddebug" + downversion_preamble[0].length);
            Utils.logd("ddddebug" + Constants.Ns_lora);
            Utils.logd("ddddebug" + Constants.Gap);

            for (int i = 0; i < numsyms; i++) {
                if (i == 273) {
                    Utils.log("273");
                }
                double[][] sym = Utils.segment2(downversion_preamble, start, start + Constants.Ns_lora - 1);
                double[][] sym_downsample = Utils.downsample(sym, 2 * Constants.Sample_Lora, Constants.Ns_lora);
                start = start + Constants.Ns_lora + Constants.Gap;

                double[] index = Utils.dechirp_test(sym_downsample, false, false);
                double sym_index = Utils.MaxIndex(index);
                //double index_tmp_test = (((double)sym_index + 10 * Constants.Sample_Lora -1) / 10)% Constants.Sample_Lora;
                double index_tmp_test = (sym_index / 10.0) % Constants.Sample_Lora;
                index_tmp_test = Math.round(index_tmp_test) % Constants.Sample_Lora;

                detected_index_cfo[i + 4] = (int) index_tmp_test;

            }
            // write to file and display in log
            if (Constants.allowLog) {
                String all_symbol_cfo = "";
                for (int i = 0; i < detected_index_cfo.length; i++) {
                    all_symbol_cfo += (detected_index_cfo[i] + ",");
                }
                Utils.log("all_symbols_cfo =>" + all_symbol_cfo);
                if (all_symbol_cfo.endsWith(",")) {
                    all_symbol_cfo = all_symbol_cfo.substring(0, all_symbol_cfo.length() - 1);
                }


                FileOperations.writetofile(MainActivity.av, all_symbol_cfo + "",
                        Utils.genName(Constants.SignalType.Rx_Symbols, m_attempt) + ".txt");
            }

            update_symbol_error_count(detected_index_cfo);
            return detected_index_cfo;

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

//        data_remove_preamble_shift = Utils.segment(data,ptime+Constants.ChirpGap + (8 + numsyms/ 2)* (Constants.Ns_lora + Constants.Gap) +time_offset,ptime+Constants.ChirpGap +time_offset+ (numsyms+8) * (Constants.Ns_lora+ Constants.Gap)-1);
//        downversion_preamble = Utils.downversion(data_remove_preamble_shift);
//        data_downsample = Utils.downsample(downversion_preamble,2 * Constants.Sample_Lora,Constants.Ns_lora);
//        start = 0;

//        for (int i = 0; i < numsyms / 2; i++) {
//            double[][] sym = Utils.segment2(downversion_preamble, start , start  + Constants.Ns_lora - 1);
//            double[][] sym_downsample = Utils.downsample(sym, 2* Constants.Sample_Lora, Constants.Ns_lora);
//            start = start  + Constants.Ns_lora + Constants.Gap;
//            double[] index = Utils.dechirp_test(sym_downsample,false, false);
//            double sym_index = Utils.MaxIndex(index);
//            //double index_tmp_test = (((double)sym_index + 10 * Constants.Sample_Lora -1) / 10)% Constants.Sample_Lora;
//            double index_tmp_test = (sym_index  / 10.0)% Constants.Sample_Lora;
//            index_tmp_test = index_tmp_test - off_set[0] ;
//            index_tmp_test = Math.round(index_tmp_test) % Constants.Sample_Lora;
//
//            detected_index_cfo[numsyms/ 2 + i+4] = (int)index_tmp_test;
//
//        }

    }

    public static Matrix timeEqualizerEstimation(Matrix tx, Matrix rx, int tapNum) {
        double lambda = 1e-5;

        int txRows = tx.getRowDimension();
        int txCols = tx.getColumnDimension();
        int rxRows = rx.getRowDimension();
        int rxCols = rx.getColumnDimension();

        Utils.log("txRows  " + txRows);
        Utils.log("txCols  " + txCols);
        Utils.log("rxRows  " + rxRows);
        Utils.log("rxCols  " + rxCols);

        if ((txCols + tapNum - 1) != rxRows) {
            System.out.println("Warning: tx and rx different size");
            return null;
        }
        if (tapNum > txCols) {
            System.out.println("Tap number is too large");
            return null;
        }

        int symbolsNum = txRows;
        int P = txCols;
        int L = tapNum;
        Matrix M = new Matrix(P * symbolsNum, L);
        Matrix Y = new Matrix(P * symbolsNum, 1);
        int ii = 0;

        for (int a = 0; a < symbolsNum; a++) {
            for (int b = 0; b < P; b++) {
                for (int k = 0; k < L; k++) {
                    M.set(ii, k, rx.get(b + k, a));
                }
                ii++;
            }
        }

        for (int i = 0; i < symbolsNum; i++) {
            Matrix row = tx.getMatrix(i, i, 0, P - 1);
            for (int j = 0; j < P; j++) {
                Y.set(i * P + j, 0, row.get(0, j));
            }
        }

        Matrix Mt = M.transpose();
        Matrix MtM = Mt.times(M);
        Matrix MtMPlusLambdaI = MtM.plus(Matrix.identity(L, L).times(lambda));
        Matrix MtY = Mt.times(Y);
        Matrix g = MtMPlusLambdaI.solve(MtY);

        return g;
    }

    public static Matrix timeEqualizerRecover(Matrix rx, Matrix g) {
        int L = g.getRowDimension();
        int P = rx.getRowDimension() - L + 1;

        double[] txArray = new double[P];

        for (int i = 0; i < P; i++) {
            double sum = 0.0;
            for (int j = 0; j < L; j++) {
                sum += rx.get(i + j, 0) * g.get(j, 0);
            }
            txArray[i] = sum;
        }

        Matrix tx = new Matrix(txArray, 1).transpose();
        return tx;
    }



    public static double[] convertShortArrayToDoubleArray(short[] shortArray) {
        // Create a new double array of the same length
        double[] doubleArray = new double[shortArray.length];

        // Copy each short value to the corresponding double element
        for (int i = 0; i < shortArray.length; i++) {
            doubleArray[i] = (double) shortArray[i];
        }

        return doubleArray;
    }



    public static Map.Entry<long[], String> decoding(Activity av, double[] received_data, int m_attempt)
    {
        //received_data = Utils.filter(received_data);
        // save data before time domain equalization
        if (Constants.allowLog) {
            StringBuilder before_equqlization_rx_preambleBuilder = new StringBuilder();
            for (int j = 0; j < received_data.length; j++) {
                before_equqlization_rx_preambleBuilder.append(received_data[j]);
                before_equqlization_rx_preambleBuilder.append("\n");
            }
            String before_equalization_rx_str = before_equqlization_rx_preambleBuilder.toString();
//            if (before_equalization_rx_str.endsWith(",")) {
//                before_equalization_rx_str = before_equalization_rx_str.substring(0, before_equalization_rx_str.length() - 1);
//            }
            FileOperations.writetofile(MainActivity.av, before_equalization_rx_str + "",
                    Utils.genName(Constants.SignalType.Before_Equalization_Rx_Raw_Symbols, m_attempt) + ".txt");
        }

        if (Constants.isLinearChirp && Constants.currentEqualizationMethod == Constants.NewEqualizationMethod.method1_once) {
            // add time equalization
            // receiver t2 time domain equalization (part of demodulate)
            final long startTime_time_domain_equalization = SystemClock.elapsedRealtime();

            int gtIdx = 0;
            int Ns = 960;
            int tapNum = 120;
            int offset = 24;
            int pkgIdx = 1200*7;
            int lenRx = Ns + tapNum - 1;
            short[] preamble_sig = PreambleGen.preamble_s();
            double[] sendingSignalArray = convertShortArrayToDoubleArray(preamble_sig);

            Matrix sendingSignal = new Matrix(sendingSignalArray, 1).transpose();
            Matrix dataBad = new Matrix(received_data, 1).transpose();

            Matrix symbolTx = sendingSignal.getMatrix(gtIdx, gtIdx + Ns - 1, 0, 0).transpose(); // []
            Matrix symbolRx = dataBad.getMatrix(pkgIdx - offset, pkgIdx + lenRx - offset - 1, 0, 0);

            Matrix g = timeEqualizerEstimation(symbolTx, symbolRx, tapNum);
            if (g != null) {
                // Recover the transmitted signal
                Matrix tx = timeEqualizerRecover(dataBad, g);
                received_data = tx.getColumnPackedCopy(); // Convert Matrix to double[]
                Utils.log("Equalizer estimation success.");

                if (Constants.allowLog) {
                    StringBuilder gBuilder = new StringBuilder();
                    for (int j = 0; j < 20; j++) {
                        gBuilder.append(g.get(j, 0));
                        gBuilder.append(",");
                    }
                    String g_string = gBuilder.toString();
                    Utils.log("time equalization g => " + g_string);
                }

            } else {
                Utils.log("Equalizer estimation failed.");
            }
            // receiver t2 time domain equalization
            final long inferenceTime_time_domain_equalization = SystemClock.elapsedRealtime() - startTime_time_domain_equalization;
            Constants.Receiver_Latency_Str = Constants.Receiver_Latency_Str + "receiver time domain equalization (ms): " + inferenceTime_time_domain_equalization + "\n";
            Utils.log("Receiver_Latency_Str: " + Constants.Receiver_Latency_Str);

            // end add time equalization
        }

        if (Constants.isLinearChirp && Constants.currentEqualizationMethod == Constants.NewEqualizationMethod.method2_new_freq) {
            // new equalization
            final long startTime_time_domain_equalization = SystemClock.elapsedRealtime();
            double[] equalization_data = Arrays.copyOf(received_data, received_data.length);
            double[] equalization_data2 = Arrays.copyOf(received_data, received_data.length);
            // filtered
            equalization_data = Utils.bpass_filter2(equalization_data, Constants.Center_Freq_Equalization, Constants.Offset_Freq_Equalization, Constants.FS);
            equalization_data2 = Utils.bpass_filter2(equalization_data, Constants.Center_Freq_Equalization2, Constants.Offset_Freq_Equalization2, Constants.FS);

            // your code here
            int Ns = 960;
            int tapNum = 120;
            int offset = 24;
            int start = 0;
            int lenRx = Ns + tapNum - 1;
            short[] preamble_sig = Utils.GenerateEqualizationPreamble_LoRa();
            Utils.logd("preamble_sig length " + preamble_sig.length);

            double[] sendingSignalArray = convertShortArrayToDoubleArray(preamble_sig);

            int num_sections = SymbolGeneration.calc_sym_num(Constants.EmbeddindBytes) / 5;

            Matrix symbolTx = new Matrix(sendingSignalArray, 1).transpose();

            int ptime = (int) ((Constants.preambleTime / 1000.0) * Constants.fs);
            start = ptime + Constants.ChirpGap; // skip preambles
            start = start + 4 * (Constants.Ns_lora + Constants.Gap); // skip to and cfo preambles

            int section_len = Constants.Equalization_Range * (Constants.Ns_lora + Constants.Gap);
            Utils.logd("Ns length " + Constants.Ns);
            Utils.logd("Gap length " + Constants.Gap);

            Utils.logd("section_len length " + section_len);

            int last_section_len = (SymbolGeneration.calc_sym_num(Constants.EmbeddindBytes) % 5) * (Constants.Ns + Constants.Gap);

            double[] output_data = Arrays.copyOf(received_data, received_data.length);

            for (int i = 0; i < num_sections; i++) {
                double[] temp_received_data = Utils.segment(equalization_data, start - offset, start + lenRx - 1 - offset);
                double[] temp_to_recover_data;
                if (i == num_sections - 1) {
                    temp_to_recover_data = Utils.segment(equalization_data2, start - offset, start + last_section_len - 1- 1 - offset+ tapNum );
                } else {
                    temp_to_recover_data = Utils.segment(equalization_data2, start - offset, start + section_len - 1- 1 - offset+ tapNum );
                }
                Utils.logd("to recover data length " + temp_to_recover_data.length);
                Matrix symbolRx = new Matrix(temp_received_data, 1).transpose();
                Matrix badData = new Matrix(temp_to_recover_data, 1).transpose();
                Matrix g = timeEqualizerEstimation(symbolTx.transpose(), symbolRx, tapNum);
                if (g != null) {
                    // Recover the transmitted signal
                    Matrix tx = timeEqualizerRecover(badData, g);
                    double[] temp_output_data = tx.getColumnPackedCopy();
                    Utils.logd("recovered data length " + temp_output_data.length);

                    if (i == num_sections - 1) {
                        System.arraycopy(temp_output_data, 0, output_data, start, last_section_len);
                    } else {
                        System.arraycopy(temp_output_data, 0, output_data, start, section_len);
                    }

                    Utils.log("Equalizer estimation success.");

                } else {
                    Utils.log("Equalizer estimation failed.");
                }
                if (i == num_sections - 1) {
                    start = start + last_section_len;
                } else {
                    start = start + section_len;
                }
            }
        }

        // insert pilot/preamble
        if (Constants.isLinearChirp && Constants.currentEqualizationMethod == Constants.NewEqualizationMethod.method3_tv_wo_to) {
                Utils.logd("use new equalization");
                // new equalization
                final long startTime_time_domain_equalization = SystemClock.elapsedRealtime();
//                double[] equalization_data =  Arrays.copyOf(received_data, received_data.length);
                double[] equalization_data2 =  Arrays.copyOf(received_data, received_data.length);

            // filtered
                // TODO: all long filter can be optimized
//                equalization_data = Utils.bpass_filter2(equalization_data,Constants.Center_Freq_Equalization2,Constants.Offset_Freq_Equalization2,Constants.FS);
                equalization_data2 = Utils.bpass_filter2(equalization_data2,Constants.Center_Freq_Equalization3,Constants.Offset_Freq_Equalization3,Constants.FS);

            // place holder for equalization output data
                double[] output_data =  Arrays.copyOf(received_data, received_data.length);
                // preamble time
                int ptime = (int)((Constants.preambleTime/1000.0)*Constants.fs);

                // recover to estimator
                int gtIdx = 0;
                int Ns_to = 960;
                int tapNum_to = 120;
                int offset_to = 24;
                int pkgIdx = 1200*7;
                int lenRx_to = Ns_to + tapNum_to - 1;
                int to_len = 4 * (Constants.Ns_lora + Constants.Gap);
                int start_to = ptime + Constants.ChirpGap;

                short[] preamble_sig_to = PreambleGen.preamble_s();
                preamble_sig_to = Utils.segment(preamble_sig_to, gtIdx, gtIdx + Ns_to - 1);
                double[] sendingSignalArray_to = convertShortArrayToDoubleArray(preamble_sig_to);
                Matrix symbolTx_to = new Matrix(sendingSignalArray_to, 1);
                double[] temp_received_data_to = Utils.segment(equalization_data2, pkgIdx - offset_to, pkgIdx + lenRx_to - offset_to - 1);
                Matrix symbolRx_to = new Matrix(temp_received_data_to, 1).transpose();
                double[] to_recover_data = Utils.segment(equalization_data2,start_to-offset_to, start_to+to_len+tapNum_to-1-offset_to-1);
                Matrix dataBad = new Matrix(to_recover_data, 1).transpose();

                Matrix g_to = timeEqualizerEstimation(symbolTx_to, symbolRx_to, tapNum_to);
                if (g_to != null) {
                    // Recover the transmitted signal
                    Matrix tx = timeEqualizerRecover(dataBad, g_to);
                    double[] temp_output_data = tx.getColumnPackedCopy(); // Convert Matrix to double[]
                    System.arraycopy(temp_output_data, 0, output_data, start_to, to_len);
                    Utils.log("TO Equalizer estimation success.");
                } else {
                    Utils.log("TO Equalizer estimation failed.");
                }

                // recover symbols
                int Ns = Constants.Ns_lora;
                int tapNum = 120;
                int offset = 24;
                int start = 0;
                int lenRx = Ns + tapNum - 1;
//                short[] preamble_sig = Utils.GeneratePreamble_LoRa(true, 0, true);
                // try to use the preamble
                short[] preamble_sig = PreambleGen.preamble_s();
                preamble_sig = Utils.segment(preamble_sig, 0, 0 + Constants.Ns_lora - 1);
                double[] sendingSignalArray = convertShortArrayToDoubleArray(preamble_sig);
                int num_sections = (int) Math.ceil((double)SymbolGeneration.calc_sym_num(Constants.EmbeddindBytes) / (double)Constants.Equalization2_Range);
                Utils.logd("num symbol " + SymbolGeneration.calc_sym_num(Constants.EmbeddindBytes));
                Utils.logd("num sections " + num_sections);
                Matrix symbolTx = new Matrix(sendingSignalArray, 1);

                start = ptime+Constants.ChirpGap; // skip preambles
                start = start + 4 * (Constants.Ns_lora + Constants.Gap); // skip to and cfo preambles
                int res_start = start;

                int section_len = Constants.Equalization2_Range * (Constants.Ns_lora + Constants.Gap);
                int last_section_len = (SymbolGeneration.calc_sym_num(Constants.EmbeddindBytes) % Constants.Equalization2_Range) * (Constants.Ns_lora + Constants.Gap);


                for (int i = 0; i < num_sections; i++) {
                    double[] temp_received_data = Utils.segment(equalization_data2, start - offset, start + lenRx - 1 - offset);

                    // new doppler
                    double[] xcorr = Utils.xcorr_matlab_abs(temp_received_data, sendingSignalArray);
                    double[] max_idx_info = Utils.max_idx(xcorr);
                    int max_index = (int)max_idx_info[0];
                    Utils.logd("max_index " + max_index);
                    max_index = max_index - temp_received_data.length;
                    int newstart = start + max_index;
                    temp_received_data = Utils.segment(equalization_data2, newstart - offset, newstart + lenRx - 1 - offset);
                    start = newstart + (Constants.Ns_lora + Constants.Gap);
                    double[] temp_to_recover_data;
                    if (i == num_sections - 1) {
                        temp_to_recover_data = Utils.segment(equalization_data2, start - offset, start + last_section_len + tapNum - 1 - offset - 1);
                    } else {
                        temp_to_recover_data = Utils.segment(equalization_data2, start - offset, start + section_len + tapNum -1 - offset - 1);
                    }
                    Utils.logd("to recover data length " + temp_to_recover_data.length);
                    Matrix symbolRx = new Matrix(temp_received_data, 1).transpose();
                    Matrix badData = new Matrix(temp_to_recover_data, 1).transpose();
                    Matrix g = timeEqualizerEstimation(symbolTx, symbolRx, tapNum);
                    if (g != null) {
                        // Recover the transmitted signal
                        Matrix tx = timeEqualizerRecover(badData, g);
                        double[] temp_output_data = tx.getColumnPackedCopy();
                        Utils.logd("recovered data length " + temp_output_data.length);

                        if (i == num_sections - 1) {
                            System.arraycopy(temp_output_data, 0, output_data, res_start, last_section_len);
                        } else {
                            System.arraycopy(temp_output_data, 0, output_data, res_start, section_len);
                        }

                        Utils.log("Equalizer estimation success.");

                    } else {
                        Utils.log("Equalizer estimation failed.");
                    }
                    if (i == num_sections - 1) {
                        start = start + last_section_len;
                        res_start = res_start + last_section_len;
                    } else {
                        start = start + section_len;
                        res_start = res_start + section_len;
                    }
                }
            final long inferenceTime_time_domain_equalization = SystemClock.elapsedRealtime() - startTime_time_domain_equalization;
            Constants.Receiver_Latency_Str = Constants.Receiver_Latency_Str + "receiver time domain equalization (new continuous method) (ms): " + inferenceTime_time_domain_equalization + "\n";
            Utils.log("Receiver_Latency_Str: " + Constants.Receiver_Latency_Str);

            for (int i = res_start; i < output_data.length; i++) {
                output_data[i] = 0.0;
            }
            received_data = output_data;
        }

        // insert pilot/preamble, complex to correct, resample
        if (Constants.isLinearChirp && Constants.currentEqualizationMethod == Constants.NewEqualizationMethod.method4_tv_w_to) {
            Utils.logd("use new equalization new new");
            // new equalization
            final long startTime_time_domain_equalization = SystemClock.elapsedRealtime();
//                double[] equalization_data =  Arrays.copyOf(received_data, received_data.length);
            double[] equalization_data2 =  Arrays.copyOf(received_data, received_data.length);

            // filtered
            equalization_data2 = Utils.bpass_filter2(equalization_data2,Constants.Center_Freq_Equalization3,Constants.Offset_Freq_Equalization3,Constants.FS);

            // place holder for equalization output data
            double[] output_data =  Arrays.copyOf(received_data, received_data.length);
            // preamble time
            int ptime = (int)((Constants.preambleTime/1000.0)*Constants.fs);

            // recover to estimator
            int gtIdx = 0;
            int Ns_to = 960;
            int tapNum_to = 120;
            int offset_to = 24;
            int pkgIdx = 1200*7;
            int lenRx_to = Ns_to + tapNum_to - 1;
            int to_len = 4 * (Constants.Ns_lora + Constants.Gap);
            int start_to = ptime + Constants.ChirpGap;

            short[] preamble_sig_to = PreambleGen.preamble_s();
            preamble_sig_to = Utils.segment(preamble_sig_to, gtIdx, gtIdx + Ns_to - 1);
            double[] sendingSignalArray_to = convertShortArrayToDoubleArray(preamble_sig_to);
            Matrix symbolTx_to = new Matrix(sendingSignalArray_to, 1);
            double[] temp_received_data_to = Utils.segment(equalization_data2, pkgIdx - offset_to, pkgIdx + lenRx_to - offset_to - 1);
            Matrix symbolRx_to = new Matrix(temp_received_data_to, 1).transpose();
            double[] to_recover_data = Utils.segment(equalization_data2,start_to-offset_to, start_to+to_len+tapNum_to-1-offset_to-1);
            Matrix dataBad = new Matrix(to_recover_data, 1).transpose();

            Matrix g_to = timeEqualizerEstimation(symbolTx_to, symbolRx_to, tapNum_to);
            if (g_to != null) {
                // Recover the transmitted signal
                Matrix tx = timeEqualizerRecover(dataBad, g_to);
                double[] temp_output_data = tx.getColumnPackedCopy(); // Convert Matrix to double[]
                System.arraycopy(temp_output_data, 0, output_data, start_to, to_len);
                Utils.log("TO Equalizer estimation success.");
            } else {
                Utils.log("TO Equalizer estimation failed.");
            }

            // find correct timestamps for each segment (key innovation, handle time varied channel)
            int Ns = Constants.Ns_lora;
            int Ns_equalizaiton = Constants.Ns_lora;
            short[] preamble_sig = PreambleGen.preamble_s();
            preamble_sig = Utils.segment(preamble_sig, 0, 0 + Constants.Ns_lora - 1);
            double[] sendingSignalArray = convertShortArrayToDoubleArray(preamble_sig);
            int num_symbols = SymbolGeneration.calc_sym_num(Constants.EmbeddindBytes);
            int num_sections = (int) Math.ceil((double)num_symbols / (double)Constants.Equalization2_Range);
            int section_len = (Constants.Equalization2_Range + 1) * (Constants.Ns_lora + Constants.Gap); // res
            int section_len_res = (Constants.Equalization2_Range) * (Constants.Ns_lora + Constants.Gap);
            int last_section_len = 0;
            int last_section_len_res = 0;
            if (num_symbols % Constants.Equalization2_Range == 0) {
                last_section_len = (Constants.Equalization2_Range + 1) * (Constants.Ns_lora + Constants.Gap);
                last_section_len_res = (Constants.Equalization2_Range) * (Constants.Ns_lora + Constants.Gap);
            } else {
                last_section_len = (num_symbols % Constants.Equalization2_Range + 1) * (Constants.Ns_lora + Constants.Gap);
                last_section_len_res = (num_symbols % Constants.Equalization2_Range) * (Constants.Ns_lora + Constants.Gap);
            }

            Utils.logd("num symbol " + SymbolGeneration.calc_sym_num(Constants.EmbeddindBytes));
            Utils.logd("num sections " + num_sections);
            Matrix symbolTx = new Matrix(sendingSignalArray, 1);
            int start = 0;
            start = ptime+Constants.ChirpGap; // skip preambles
            start = start + 4 * (Constants.Ns_lora + Constants.Gap); // skip to and cfo preambles
            int archive_start = start;
            int[] all_peaks = new int[num_sections]; // TODO: later + 1
            int[] all_peaks2 = new int[num_sections]; // TODO: later + 1
            int len_rx = Ns_equalizaiton;
            int offset1 = 24;
            int offset2 = -24;
            int peakIndex = 0;
            int peakIndex2 = 0;
            int peakIndex_global_expected = 0;
            int peakIndex_prev = 0;
            int peakIndex_global = 0;
            double[] symbol_rx;
            double[] symbol_rx2 = new double[0];
            for (int i = 0; i < num_sections; i++) {
                if (i==0) {
                    symbol_rx = Utils.segment(equalization_data2, start - offset1, start + len_rx - 1 - offset2);
                    double[] xcorr = Utils.xcorr_matlab_abs(symbol_rx, sendingSignalArray);
                    peakIndex = Utils.findFirstValidMaxima(xcorr);
                    all_peaks[i] = peakIndex;
                    peakIndex_global = peakIndex-(symbol_rx.length) + start-offset1;
                    peakIndex_global_expected = i * ((1+Constants.Equalization2_Range)*Math.round(Constants.Ns_lora + Constants.Gap)) + archive_start;
                    all_peaks2[i] = peakIndex_global - peakIndex_global_expected;
                    peakIndex = peakIndex - symbol_rx.length;
                } else {
                    all_peaks[i] = peakIndex;
                    peakIndex_global_expected = i * ((1+Constants.Equalization2_Range)*Math.round(Constants.Ns_lora + Constants.Gap)) + archive_start;
                    all_peaks2[i] = peakIndex_global - peakIndex_global_expected;
                    peakIndex = peakIndex_prev;
                }

                if (i == num_sections-1) {
                    peakIndex2 = peakIndex+last_section_len;
                } else {
                    symbol_rx2 = Utils.segment(equalization_data2, start - offset1 + section_len, start -offset2 + section_len + len_rx - 1);
                    double[] xcorr = Utils.xcorr_matlab_abs(symbol_rx2, sendingSignalArray);
                    peakIndex2 = Utils.findFirstValidMaxima(xcorr);
                }

                peakIndex_global = peakIndex2 - symbol_rx2.length + start+section_len-offset1;
                peakIndex = peakIndex2;
                peakIndex_prev = peakIndex2 - symbol_rx2.length;
                start = peakIndex_global;
            }

            // smooth all peaks
            double[] all_peaks2_double = Utils.convertIntToDouble(all_peaks2);
            all_peaks2_double = Utils.movingMedian(all_peaks2_double, 10);
            List<Utils.Peak> peaks = Utils.findPeaks2(all_peaks2_double);
            for (Utils.Peak peak : peaks) {
                double peakWidth = peak.width;
                int peakLoc = peak.location;
                double halfWidth = Math.ceil(peakWidth / 2.0) + 1;
                int startIdx = Math.max(0, peakLoc - (int) halfWidth);
                int endIdx = Math.min(all_peaks2_double.length - 1, peakLoc + (int) halfWidth);
                Arrays.fill(all_peaks2_double, startIdx, endIdx + 1, Double.NaN);
            }

            all_peaks2_double = Utils.fillMissing(all_peaks2_double);
            // Smooth data again
            all_peaks2_double = Utils.movingMedian(all_peaks2_double, 10);
            all_peaks2_double = Utils.movingMean(all_peaks2_double, 3);

            // recover symbols

            int tapNum = 120;
            int offset = 24;
            len_rx = Ns + tapNum - 1;
            start = ptime+Constants.ChirpGap; // skip preambles
            start = start + 4 * (Constants.Ns_lora + Constants.Gap); // skip to and cfo preambles
            archive_start = start;
            int res_start = start;
            double ratio = 1.0;
            int diff = 0;
            int real_rx_length = 0;
            int real_tx_length = 0;
            int new_start = 0;
            double[] databad;

            for (int i = 0; i < num_sections; i++) {
                peakIndex_global_expected = i * ((1 + Constants.Equalization2_Range) * Math.round(Constants.Ns_lora + Constants.Gap)) + archive_start;
                peakIndex = (int) Math.round(all_peaks2_double[i] + peakIndex_global_expected);
                if (i == num_sections - 1) {
                    peakIndex2 = peakIndex;
                    diff = 3224;
                    real_rx_length = diff;
                } else {
                    real_rx_length = (int) Math.round(all_peaks2_double[i + 1] - all_peaks2_double[i] + section_len);
                }

                if (i == num_sections - 1) {
                    real_tx_length = last_section_len;
                } else {
                    real_tx_length = section_len;
                }

                ratio = (double) real_tx_length / (double) real_rx_length;

                new_start = peakIndex + 1;
                double[] data_to_subsample = Utils.segment(equalization_data2, (int) (new_start - Math.ceil(offset / ratio)), (int) (new_start - Math.ceil(offset / ratio) - 1 + real_rx_length + Math.ceil(tapNum / ratio)));
                double[] original_index = Utils.linspace2(0.0, data_to_subsample.length-1, data_to_subsample.length);
                double[] new_index = Utils.linspace2(0.0, (double) data_to_subsample.length-1, (int) ((data_to_subsample.length) * ratio));
                double[] data_processed = Utils.previousInterpolate(original_index, data_to_subsample, new_index);

                symbol_rx = Utils.segment(data_processed, 0, len_rx-1);
                if (i == num_sections - 1) {
                    databad = Utils.segment(data_processed, Constants.Ns_lora + Constants.Gap,  last_section_len+ tapNum - 1 - 1);
                } else {
                    databad = Utils.segment(data_processed, Constants.Ns_lora + Constants.Gap, section_len + tapNum - 1 - 1);
                }

                Matrix symbolRx = new Matrix(symbol_rx, 1).transpose();
                Matrix badData = new Matrix(databad, 1).transpose();
                Matrix g = timeEqualizerEstimation(symbolTx, symbolRx, tapNum);
                if (g != null) {
                    // Recover the transmitted signal
                    Matrix tx = timeEqualizerRecover(badData, g);
                    double[] temp_output_data = tx.getColumnPackedCopy();
                    Utils.logd("recovered data length " + temp_output_data.length);

                    if (i == num_sections - 1) {
                        System.arraycopy(temp_output_data, 0, output_data, res_start, last_section_len_res);
                    } else {
                        System.arraycopy(temp_output_data, 0, output_data, res_start, section_len_res);
                    }
                    Utils.log("Equalizer estimation success.");
                } else {
                    Utils.log("Equalizer estimation failed.");
                }

                start = start + real_rx_length;
                if (i == num_sections - 1) {
                    res_start = res_start + last_section_len_res;
                } else {
                    res_start = res_start + section_len_res;
                }
            }

            final long inferenceTime_time_domain_equalization = SystemClock.elapsedRealtime() - startTime_time_domain_equalization;
            Constants.Receiver_Latency_Str = Constants.Receiver_Latency_Str + "receiver time domain equalization (new continuous method 0612) (ms): " + inferenceTime_time_domain_equalization + "\n";
            Utils.log("Receiver_Latency_Str: " + Constants.Receiver_Latency_Str);

            received_data = output_data;
        }


        if (Constants.isLinearChirp && Constants.currentEqualizationMethod == Constants.NewEqualizationMethod.method5_tv_w_to_range) {
            Utils.logd("use new equalization new new new 06152024");
            // new equalization
            final long startTime_time_domain_equalization = SystemClock.elapsedRealtime();
//                double[] equalization_data =  Arrays.copyOf(received_data, received_data.length);
            double[] equalization_data2 =  Arrays.copyOf(received_data, received_data.length);

            // filtered
            equalization_data2 = Utils.bpass_filter2(equalization_data2,Constants.Center_Freq_Equalization3,Constants.Offset_Freq_Equalization3,Constants.FS);
            int delay1 = (int) Math.ceil(3.6*Constants.FS/Constants.Offset_Freq_Equalization3)-1;
            delay1 = delay1%2 + delay1;
            delay1 = (int) delay1 / 2;
            equalization_data2 = Utils.segment(equalization_data2, delay1, equalization_data2.length-1); // compensate the filter delay

            // place holder for equalization output data
            double[] output_data =  Arrays.copyOf(received_data, received_data.length);
            // preamble time
            int ptime = (int)((Constants.preambleTime/1000.0)*Constants.fs);

            // recover to estimator
            int gtIdx = 0;
            int Ns_to = 960;
            int tapNum_to = 120;
            int offset_to = 24;
            int pkgIdx = 1200*7;
            int lenRx_to = Ns_to + tapNum_to - 1;
            int to_len = 4 * (Constants.Ns_lora + Constants.Gap);
            int start_to = ptime + Constants.ChirpGap;

            short[] preamble_sig_to = PreambleGen.preamble_s();
            preamble_sig_to = Utils.segment(preamble_sig_to, gtIdx, gtIdx + Ns_to - 1);
            double[] sendingSignalArray_to = convertShortArrayToDoubleArray(preamble_sig_to);
            Matrix symbolTx_to = new Matrix(sendingSignalArray_to, 1);
            double[] temp_received_data_to = Utils.segment(equalization_data2, pkgIdx - offset_to, pkgIdx + lenRx_to - offset_to - 1);
            Matrix symbolRx_to = new Matrix(temp_received_data_to, 1).transpose();
//            double[] to_recover_data = Utils.segment(equalization_data2,start_to-offset_to, start_to+to_len+tapNum_to-1-offset_to-1);
//            Matrix dataBad = new Matrix(to_recover_data, 1).transpose();

            Matrix g_to = timeEqualizerEstimation(symbolTx_to, symbolRx_to, tapNum_to);
//            if (g_to != null) {
//                // Recover the transmitted signal
//                Matrix tx = timeEqualizerRecover(dataBad, g_to);
//                double[] temp_output_data = tx.getColumnPackedCopy(); // Convert Matrix to double[]
//                System.arraycopy(temp_output_data, 0, output_data, start_to, to_len);
//                Utils.log("TO Equalizer estimation success.");
//            } else {
//                Utils.log("TO Equalizer estimation failed.");
//            }

            // find correct timestamps for each segment (key innovation, handle time varied channel)
            int Ns = Constants.Ns_lora;
            int Ns_equalizaiton = Constants.Ns_lora;
            short[] preamble_sig = PreambleGen.preamble_s();
            preamble_sig = Utils.segment(preamble_sig, 0, 0 + Constants.Ns_lora - 1);
            double[] sendingSignalArray = convertShortArrayToDoubleArray(preamble_sig);
            int num_symbols = SymbolGeneration.calc_sym_num(Constants.EmbeddindBytes);
            int num_sections = (int) Math.ceil((double)num_symbols / (double)Constants.Equalization2_Range);
            int section_len = (Constants.Equalization2_Range + 1) * (Constants.Ns_lora + Constants.Gap); // res
            int section_len_res = (Constants.Equalization2_Range) * (Constants.Ns_lora + Constants.Gap);
            int last_section_len = 0;
            int last_section_len_res = 0;
            if (num_symbols % Constants.Equalization2_Range == 0) {
                last_section_len = (Constants.Equalization2_Range + 1) * (Constants.Ns_lora + Constants.Gap);
                last_section_len_res = (Constants.Equalization2_Range) * (Constants.Ns_lora + Constants.Gap);
            } else {
                last_section_len = (num_symbols % Constants.Equalization2_Range + 1) * (Constants.Ns_lora + Constants.Gap);
                last_section_len_res = (num_symbols % Constants.Equalization2_Range) * (Constants.Ns_lora + Constants.Gap);
            }

            double max_relative_speed = 2;
            double sound_speed = Constants.soundSpeed;
            double to_change_limit_factor = 1.5;
            int to_change_limit = (int)Math.ceil((double)section_len / (double)Constants.FS * max_relative_speed / sound_speed * (double)Constants.FS * to_change_limit_factor);


            Utils.logd("num symbol " + SymbolGeneration.calc_sym_num(Constants.EmbeddindBytes));
            Utils.logd("num sections " + num_sections);
            Matrix symbolTx = new Matrix(sendingSignalArray, 1);
            int start = 0;
            start = ptime+Constants.ChirpGap; // skip preambles
            start = start + 4 * (Constants.Ns_lora + Constants.Gap); // skip to and cfo preambles
            int archive_start = start;
            int[] all_peaks = new int[num_sections]; // TODO: later + 1
            int[] all_peaks2 = new int[num_sections]; // TODO: later + 1
            int len_rx = Ns_equalizaiton;
            int offset1 = 24;
            int offset2 = -24;
            int peakIndex = 0;
            int peakIndex2 = 0;
            int peakIndex_global_expected = 0;
            int peakIndex_global_expected2 = 0;
            int peakIndex_prev = 0;
            int peakIndex_global = 0;
            int peakIndex_global2 = 0;
            double[] symbol_rx;
            double[] symbol_rx2 = new double[0];
            int tapNum = 120;
            int offset = 24;
            int archive_peak_value = 0;
            for (int i = 0; i < num_sections; i++) {
                if (i==0) {
                    symbol_rx = Utils.segment(equalization_data2, start - offset1 - offset, start + len_rx - 1 - offset2 - offset + tapNum - 1);
                    Matrix symbol_rx_matrix = new Matrix(symbol_rx, 1).transpose();
                    Matrix tx = timeEqualizerRecover(symbol_rx_matrix, g_to);
                    symbol_rx = tx.getColumnPackedCopy(); // time equalize first preamble to get precise timestamp
                    double[] xcorr = Utils.xcorr_matlab_abs(symbol_rx, sendingSignalArray);
                    // hilbert
                    xcorr = Utils.computeAnalyticSignalMagnitude(xcorr);
                    peakIndex = Utils.findFirstValidMaxima2(xcorr, -1, to_change_limit);
                    archive_peak_value = symbol_rx.length + offset1;
                    all_peaks[i] = peakIndex;
                    peakIndex_global = peakIndex-(symbol_rx.length) + start-offset1;
                    peakIndex_global_expected = i * ((1+Constants.Equalization2_Range)*Math.round(Constants.Ns_lora + Constants.Gap)) + archive_start;
                    peakIndex_global_expected2 = (i+1) * ((1+Constants.Equalization2_Range)*Math.round(Constants.Ns_lora + Constants.Gap)) + archive_start;
                    all_peaks2[i] = peakIndex_global - peakIndex_global_expected;
                } else {
                    all_peaks[i] = peakIndex;
                    peakIndex_global_expected = i * ((1+Constants.Equalization2_Range)*Math.round(Constants.Ns_lora + Constants.Gap)) + archive_start;
                    peakIndex_global_expected2 = (i+1) * ((1+Constants.Equalization2_Range)*Math.round(Constants.Ns_lora + Constants.Gap)) + archive_start;

                    all_peaks2[i] = peakIndex_global - peakIndex_global_expected;
                    peakIndex = peakIndex_prev;
                }

                if (i == num_sections-1) {
                    peakIndex2 = peakIndex+last_section_len;
                } else {
                    symbol_rx2 = Utils.segment(equalization_data2, start - offset1 + section_len, start -offset2 + section_len + len_rx - 1);
                    double[] xcorr = Utils.xcorr_matlab_abs(symbol_rx2, sendingSignalArray);
                    // hilbert
                    xcorr = Utils.computeAnalyticSignalMagnitude(xcorr);
                    peakIndex2 = Utils.findFirstValidMaxima2(xcorr, archive_peak_value, to_change_limit);
                    archive_peak_value = peakIndex2;
                }

                peakIndex_global = peakIndex2 - symbol_rx2.length + start+section_len-offset1;
                peakIndex = peakIndex2;
                peakIndex_prev = peakIndex2 - symbol_rx2.length;
                start = peakIndex_global_expected2;
            }

            // smooth all peaks
            double[] all_peaks2_double = Utils.convertIntToDouble(all_peaks2);
            all_peaks2_double = Utils.movingMedian(all_peaks2_double, 10);
            all_peaks2_double = Utils.movingMean(all_peaks2_double, 3);

            // recover symbols
            len_rx = Ns + tapNum - 1;
            start = ptime+Constants.ChirpGap; // skip preambles
            start = start + 4 * (Constants.Ns_lora + Constants.Gap); // skip to and cfo preambles
            archive_start = start;
            int res_start = start;
            double ratio = 1.0;
            int diff = 0;
            int real_rx_length = 0;
            int real_tx_length = 0;
            int new_start = 0;
            double[] databad;

            for (int i = 0; i < num_sections; i++) {
                peakIndex_global_expected = i * ((1 + Constants.Equalization2_Range) * Math.round(Constants.Ns_lora + Constants.Gap)) + archive_start;
                peakIndex = (int) Math.round(all_peaks2_double[i] + peakIndex_global_expected);
                if (i == num_sections - 1) {
                    peakIndex2 = peakIndex;
                    diff = 3224;
                    real_rx_length = diff;
                } else {
                    real_rx_length = (int) Math.round(all_peaks2_double[i + 1] - all_peaks2_double[i] + section_len);
                }

                if (i == num_sections - 1) {
                    real_tx_length = last_section_len;
                } else {
                    real_tx_length = section_len;
                }

                ratio = (double) real_tx_length / (double) real_rx_length;

                new_start = peakIndex + 1;
                double[] data_to_subsample = Utils.segment(equalization_data2, (int) (new_start - Math.ceil(offset / ratio)), (int) (new_start - Math.ceil(offset / ratio) - 1 + real_rx_length + Math.ceil(tapNum / ratio)));
                double[] original_index = Utils.linspace2(0.0, data_to_subsample.length-1, data_to_subsample.length);
                double[] new_index = Utils.linspace2(0.0, (double) data_to_subsample.length-1, (int) ((data_to_subsample.length) * ratio));

                double[] data_processed = Utils.interp1(original_index, data_to_subsample, new_index);

                symbol_rx = Utils.segment(data_processed, 0, len_rx-1);
                if (i == num_sections - 1) {
                    databad = Utils.segment(data_processed, Constants.Ns_lora + Constants.Gap,  last_section_len+ tapNum - 1 - 1);
                } else {
                    databad = Utils.segment(data_processed, Constants.Ns_lora + Constants.Gap, section_len + tapNum - 1 - 1);
                }

                Matrix symbolRx = new Matrix(symbol_rx, 1).transpose();
                Matrix badData = new Matrix(databad, 1).transpose();
                Matrix g = timeEqualizerEstimation(symbolTx, symbolRx, tapNum);
                if (g != null) {
                    // Recover the transmitted signal
                    Matrix tx = timeEqualizerRecover(badData, g);
                    double[] temp_output_data = tx.getColumnPackedCopy();
                    Utils.logd("recovered data length " + temp_output_data.length);

                    if (i == num_sections - 1) {
                        System.arraycopy(temp_output_data, 0, output_data, res_start, last_section_len_res);
                    } else {
                        System.arraycopy(temp_output_data, 0, output_data, res_start, section_len_res);
                    }
                    Utils.log("Equalizer estimation success.");
                } else {
                    Utils.log("Equalizer estimation failed.");
                }

                if (i == num_sections - 1) {
                     res_start = res_start + last_section_len_res;
                } else {
                    res_start = res_start + section_len_res;
                }
            }

            final long inferenceTime_time_domain_equalization = SystemClock.elapsedRealtime() - startTime_time_domain_equalization;
            Constants.Receiver_Latency_Str = Constants.Receiver_Latency_Str + "receiver time domain equalization (new continuous method 0612) (ms): " + inferenceTime_time_domain_equalization + "\n";
            Utils.log("Receiver_Latency_Str: " + Constants.Receiver_Latency_Str);

            received_data = output_data;
        }
        // receiver t3 demodulate
        final long startTime_demodulate = SystemClock.elapsedRealtime();

        int[] symbol = demodulate(received_data,m_attempt);

        // receiver t3 demodulate
        final long inferenceTime_demodulate = SystemClock.elapsedRealtime() - startTime_demodulate;
        Constants.Receiver_Latency_Str = Constants.Receiver_Latency_Str + "receiver demodulate (ms): " + inferenceTime_demodulate + "\n";
        Utils.log("Receiver_Latency_Str: " + Constants.Receiver_Latency_Str);

        // receiver t4 decode signal //TODO: change here with error detection
        final long startTime_decode_signal = SystemClock.elapsedRealtime();

        int[] symbol_remove_preamble = Utils.segment(symbol,4,symbol.length-1);
        // gray coding
        int[] symbol_g = SymbolGeneration.gray_coding(symbol_remove_preamble);
        // deinterleaving
        int[] codewords = SymbolGeneration.diag_deinterleave(Arrays.copyOfRange(symbol_g, 0, 8),  Constants.SF - 2);
        // hamming decoding
//        int[] nibbles = SymbolGeneration.hamming_decode(codewords,8);
        Map<String, int[]> result = SymbolGeneration.hamming_decode2(codewords, 8); // TODO: rdd
        int[] nibbles = result.get("nibbles");
        int[] parityCheck = result.get("parity_check");
        int rdd = Constants.CodeRate_LoRA + 4;

        for (int i = 8; i < symbol_g.length - rdd + 1; i += rdd)
        {
            codewords = SymbolGeneration.diag_deinterleave(Arrays.copyOfRange(symbol_g, i, i+ rdd ), Constants.SF - 2 * Constants.LDR);
//            int[] tem_nibbles = SymbolGeneration.hamming_decode(codewords,rdd);
            result = SymbolGeneration.hamming_decode2(codewords, rdd);
            int[] tem_nibbles = result.get("nibbles");
            int[] tem_parityCheck = result.get("parity_check");
            nibbles = Utils.concatArrays_int(nibbles, tem_nibbles);
            parityCheck = Utils.concatArrays_int(parityCheck,tem_parityCheck);
        }
        // convert nibbles to the bytes
        String errorMask = Utils.generateErrorBitString(parityCheck);
        int byteCount = Math.min(255,nibbles.length /2 );
        byte[] bytes = new byte[byteCount];
        for (int i = 0; i < byteCount; i++) {
            int firstNibble = nibbles[2 * i];       // 2*i because of zero-based indexing in Java
            int secondNibble = 16 * nibbles[2 * i + 1];  // 2*i+1 to get the next nibble

            // Shift the first nibble left by 4 bits and OR with the second nibble
            bytes[i] = (byte) (firstNibble | secondNibble);
        }

        // Beitong07112024 change it to not hardcoding
        int len = Constants.EmbeddindBytes;
        // dewhitening
        byte[] data = SymbolGeneration.dewhiten(Arrays.copyOfRange(bytes, 0, len ));


        Map.Entry<long[], String> result1 = Utils.Bytes2Embedding2(data, errorMask);
        long[] embedding = result1.getKey();
        String errorBits = result1.getValue();
//        long[] embedding = Utils.Bytes2Embedding(data);

        // receiver t4 decode signal
        final long inferenceTime_decode_signal = SystemClock.elapsedRealtime() - startTime_decode_signal;
        Constants.Receiver_Latency_Str = Constants.Receiver_Latency_Str + "receiver decode signal (ms): " + inferenceTime_decode_signal + "\n";
        Utils.log("Receiver_Latency_Str: " + Constants.Receiver_Latency_Str);

        if (Constants.allowLog) {
            String all_embedding = "";
            for (int i = 0; i < embedding.length; i++) {
                all_embedding += (embedding[i] + ",");
            }
            Utils.log("all_embedding =>" + all_embedding);
            Utils.log("mask of detected wrong embeddings =>" + errorBits);
            if (all_embedding.endsWith(",")) {
                all_embedding = all_embedding.substring(0, all_embedding.length() - 1);
            }
            FileOperations.writetofile(MainActivity.av, Arrays.toString(embedding),
                    Utils.genName(Constants.SignalType.Rx_Embedding, m_attempt) + ".txt");

            // write mask
            FileOperations.writetofile(MainActivity.av, errorBits,
                    Utils.genName(Constants.SignalType.Rx_Mask, m_attempt) + ".txt");
        }

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

        return new AbstractMap.SimpleEntry<>(embedding, errorBits);
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
