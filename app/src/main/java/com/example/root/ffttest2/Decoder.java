package com.example.root.ffttest2;

import android.app.Activity;
import android.os.SystemClock;

import java.util.Arrays;

import Jama.Matrix;
public class Decoder {
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
        for (int i = 0 ; i < 2 ; i++)
        {
            double[][] preamble = Utils.segment2(downversion_preamble,start , start + Constants.Ns_lora - 1);
            double[][] preamble_downsample = Utils.downsample(preamble, 2* Constants.Sample_Lora, Constants.Ns_lora);
            start = start + Constants.Ns_lora + Constants.Gap;
            double[] index_upchirp = Utils.dechirp_test(preamble_downsample,false, true);
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
            double[] index_downchirp = Utils.dechirp_test(preamble_downsample,true, true);
            int index_tmp = Utils.MaxIndex(index_downchirp);
            //double index_tmp_test = (((double)index_tmp + 10 * Constants.Sample_Lora -1) / 10)% Constants.Sample_Lora;
            double index_tmp_test = ((double)index_tmp  / 10.0)% Constants.Sample_Lora;
            index_count_test[i] = index_tmp_test;
            //pks[i] = Utils.MaxValue(index_downchirp);
        }
        // frequency and time synchronization
        double[] off_set = Utils.synchronization2(index_count_test[1], index_count_test[3]);
//        double[] off_set2 = Utils.synchronization2(index_count_test[0], index_count_test[2]);


        Utils.log("first index  =>" + index_count_test[1]);
        Utils.log("second index  =>" + index_count_test[3]);
        Utils.log("cfo =>" + off_set[0]);
        Utils.log("to =>" + off_set[1]);

        //Constants.CFO = off_set[0] * Constants.BW / Constants.Sample_Lora; confirmed
//        int time_offset = (int)Math.round(Math.abs(off_set[1]) * Constants.Ns_lora / Constants.Sample_Lora); // remove abs
        int time_offset = (int)Math.round(off_set[1] * Constants.Ns_lora / Constants.Sample_Lora);

        double[] data_remove_preamble_shift = Utils.segment(data,ptime+Constants.ChirpGap + 4* (Constants.Ns_lora + Constants.Gap) +time_offset,ptime+Constants.ChirpGap +time_offset+ (numsyms+4) * (Constants.Ns_lora+ Constants.Gap)-1);
        downversion_preamble = Utils.downversion(data_remove_preamble_shift);
        //data_downsample = Utils.downsample(downversion_preamble,2 * Constants.Sample_Lora,Constants.Ns_lora);
        start = 0;


        int[] detected_index_cfo = new int[numsyms + 4];

        for (int j =0 ; j< 4; j++)
        {
            detected_index_cfo[j] = (int)Math.round(index_count_test[j]);
        }



        // extract each symbol
        for (int i = 0; i < numsyms; i++) {
            double[][] sym = Utils.segment2(downversion_preamble, start , start  + Constants.Ns_lora - 1);
            double[][] sym_downsample = Utils.downsample(sym, 2* Constants.Sample_Lora, Constants.Ns_lora);
            start = start  + Constants.Ns_lora + Constants.Gap;
            double[] index = Utils.dechirp_test(sym_downsample,false, false);
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


        return detected_index_cfo;
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

    public static long[] decoding(Activity av, double[] received_data, int m_attempt)
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

        if (Constants.isLinearChirp && Constants.isNewEqualization == false && Constants.isNewEqualization2 == false) {
            // add time equalization
            // receiver t2 time domain equalization (part of demodulate)
            final long startTime_time_domain_equalization = SystemClock.elapsedRealtime();

            int gtIdx = 0;
            int Ns = 960;
            int tapNum = 480;
            int offset = 100;
            int pkgIdx = 1200;
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

        if (Constants.isLinearChirp && Constants.isNewEqualization == true) {
            // new equalization
            final long startTime_time_domain_equalization = SystemClock.elapsedRealtime();
            double[] equalization_data = Arrays.copyOf(received_data, received_data.length);
            double[] equalization_data2 = Arrays.copyOf(received_data, received_data.length);
            // filtered
            equalization_data = Utils.bpass_filter2(equalization_data, Constants.Center_Freq_Equalization, Constants.Offset_Freq_Equalization, Constants.FS);
            equalization_data2 = Utils.bpass_filter2(equalization_data, Constants.Center_Freq_Equalization2, Constants.Offset_Freq_Equalization2, Constants.FS);

            // your code here

            int Ns = 960;
            int tapNum = 480;
            int offset = 100;
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
        if (Constants.isLinearChirp && Constants.isNewEqualization2 == true) {
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
                    start = start + (Constants.Ns_lora + Constants.Gap);
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



        // receiver t3 demodulate
        final long startTime_demodulate = SystemClock.elapsedRealtime();

        int[] symbol = demodulate(received_data,m_attempt);

        // receiver t3 demodulate
        final long inferenceTime_demodulate = SystemClock.elapsedRealtime() - startTime_demodulate;
        Constants.Receiver_Latency_Str = Constants.Receiver_Latency_Str + "receiver demodulate (ms): " + inferenceTime_demodulate + "\n";
        Utils.log("Receiver_Latency_Str: " + Constants.Receiver_Latency_Str);

        // receiver t4 decode signal
        final long startTime_decode_signal = SystemClock.elapsedRealtime();

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
            if (all_embedding.endsWith(",")) {
                all_embedding = all_embedding.substring(0, all_embedding.length() - 1);
            }
            FileOperations.writetofile(MainActivity.av, Arrays.toString(embedding),
                    Utils.genName(Constants.SignalType.Rx_Embedding, m_attempt) + ".txt");
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
