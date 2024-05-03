package com.example.root.ffttest2;

import static com.example.root.ffttest2.Constants.tv4;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import java.util.Arrays;

public class SendChirpAsyncTask extends AsyncTask<Void, Void, Void> {
    Activity av;
    int num_measurements = 0;
    public SendChirpAsyncTask(Activity activity, int num_measurements) {
        this.av = activity;
        this.num_measurements = num_measurements;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    public void setupTimer() {
        av.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                double totalTime = 0;
                if (Constants.user.equals(Constants.User.Alice)) {
                    double soundingTimeTx = 1;
                    double extractionFeedbackTime = 1;
                    totalTime += (soundingTimeTx + Constants.WaitForFeedbackTime +
                            extractionFeedbackTime);
                    if (Constants.SEND_DATA) {
                        totalTime+=Constants.WaitForDataTime;
                    }
                    Constants.AliceTime = (int)totalTime;
                    totalTime *= 1000;
                    totalTime *= num_measurements;

                    totalTime += 1000+(Constants.initSleep*1000);
                }
                else if (Constants.user.equals(Constants.User.Bob)) {
                    int extractSoundingTime = 1;
                    int sendFeedbackTime = 1;
                    totalTime += Constants.WaitForSoundingTime+
                            extractSoundingTime+sendFeedbackTime;
                    totalTime += Constants.SoundingOffset;
                    if (Constants.SEND_DATA) {
                        totalTime+=Constants.WaitForDataTime;
                    }
                    Constants.BobTime = (int)totalTime;
                    totalTime *= 1000;
                    totalTime *= num_measurements;
                    totalTime += 1000+(Constants.initSleep*1000);
                }
            }
        });
    }

    @Override
    protected void onPostExecute(Void unused) {
        super.onPostExecute(unused);

        MainActivity.unreg(av);

        if (Constants.timer!=null) {
            Constants.timer.cancel();
            tv4.setText("0");
        }

        Constants.sp1=null;
        Constants._OfflineRecorder = null;
        Constants.user  = Constants.User.Bob;
        MainActivity.startMethod(av);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Constants.WaitForFeedbackTime = Constants.WaitForFeedbackTimeDefault + Constants.SyncLag;
        Constants.WaitForSoundingTime = Constants.WaitForSoundingTimeDefault + Constants.SyncLag - Constants.SoundingOffset;
        Constants.WaitForBerTime = Constants.WaitForBerTimeDefault + Constants.SyncLag;
        Constants.WaitForPerTime = Constants.WaitForPerTimeDefault + Constants.SyncLag;

        Constants.SEND_DATA=true;
        Constants.WaitForDataTime = Constants.WaitForPerTime;
        Constants.AdaptationMethod = 3;

        FileOperations.writetofile(MainActivity.av, Constants.SNR_THRESH2+"\n"+Constants.FreAdaptScaleFactor+"\n"+Constants.SNR_THRESH2_2,
                Utils.genName(Constants.SignalType.AdaptParams,0)+".txt");

        setupTimer();

        sleep(Constants.initSleep * 1000);

        Constants.StartingTimestamp = System.currentTimeMillis();
        appendToLog(Constants.SignalType.Start.toString());

        if (Constants.user.equals(Constants.User.Alice)) {
            FileOperations.writetofile(MainActivity.av, Constants.FLIP_SYMBOL + "",
                    Utils.genName(Constants.SignalType.FlipSyms, 0) + ".txt");
        }

        for (int i = 0; i < num_measurements; i++) {
            Log.e("timer","work "+i);
            int flag = work(i);
            updateTimer((i+1)+"");
            if (flag == -1) {
                updateTimer("-1");
                break;
            }
        }
        return null;
    }

    public static void appendToLog(String s) {
        if (s.equals(Constants.SignalType.Start.toString())) {
            if (Constants.user.equals(Constants.User.Alice)) {
                String ts = System.currentTimeMillis()+"";
                String filename = Constants.user.toString() + "-" + Constants.SignalType.Sounding + "-" + "log";
                FileOperations.appendtofile(MainActivity.av, ts + "\n", filename + ".txt");
                filename = Constants.user.toString() + "-" + Constants.SignalType.Data + "-" + "log";
                FileOperations.appendtofile(MainActivity.av, ts + "\n", filename + ".txt");
            }
            else {
                String filename = Constants.user.toString() + "-" + Constants.SignalType.Feedback + "-" + "log";
                FileOperations.appendtofile(MainActivity.av, System.currentTimeMillis() + "\n", filename + ".txt");
            }
        }
        else {
            String filename = Constants.user.toString() + "-" + s + "-" + "log";
            FileOperations.appendtofile(MainActivity.av, System.currentTimeMillis() + "\n", filename + ".txt");
        }
    }

    public void updateTimer(String ss) {
        MainActivity.av.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv4.setText(ss);
            }
        });
    }

    public int work(int m_attempt) {
        //
        if (Constants.scheme == Constants.Modulation.OFDM_freq_adapt) {
            double[] tx_preamble = PreambleGen.preamble_d();
            if (Constants.user.equals(Constants.User.Alice)) {
                int chirpLoopNumber = 0;
                double[] feedback_signal = null;
                do {
                    short[] sig = PreambleGen.sounding_signal_s();
                    FileOperations.writetofile(MainActivity.av, sig, Utils.genName(Constants.SignalType.Sounding, m_attempt) + ".txt");

                    Constants.sp1 = new AudioSpeaker(av, sig, Constants.fs, 0, sig.length, false);
                    appendToLog(Constants.SignalType.Sounding.toString());
                    Constants.sp1.play(Constants.volume);

                    int sig_len = (int)(((double)sig.length/Constants.fs)*1000);
                    sleep(sig_len+Constants.SendPad);

                    feedback_signal = Utils.waitForChirp(Constants.SignalType.Feedback, m_attempt, chirpLoopNumber);
                    chirpLoopNumber++;
                    if (chirpLoopNumber >= 3 || !Constants.work) {
                        return -1;
                    }
                } while (feedback_signal == null);

                double[] seg = Utils.segment(feedback_signal,0,24000-1);
                double[] xcorr_out = Utils.xcorr_online(tx_preamble, seg);

                int[] valid_bins = FeedbackSignal.extractSignalHelper(feedback_signal, (int)xcorr_out[1], m_attempt);

                if (Constants.SEND_DATA) {
                    appendToLog(Constants.SignalType.Data.toString());
                    if (valid_bins.length >= 1 && valid_bins[0] != -1) {
                        sendData(valid_bins, m_attempt);
                    }
                    try {
                        Thread.sleep(3000);
                    }
                    catch(Exception e){
                        Log.e("asdf",e.toString());
                    }
                }
                return 0;
            }
            else if (Constants.user.equals(Constants.User.Bob)) {
                int chirpLoopNumber = 0;
                int[] valid_bins = null;
                double[] sounding_signal = null;
                do {
                    sounding_signal = Utils.waitForChirp(Constants.SignalType.Sounding, m_attempt, chirpLoopNumber);
                    if (sounding_signal == null) {
                        return -1;
                    }

                    double[] seg = Utils.segment(sounding_signal,0,24000-1);
                    double[] xcorr_out = Utils.xcorr_online(tx_preamble, seg);

                    valid_bins = ChannelEstimate.extractSignal_withsymbol_helper(av, sounding_signal, (int)xcorr_out[1], m_attempt);
                    chirpLoopNumber++;

                    if (!Constants.work) {
                        return -1;
                    }
                } while (valid_bins == null || valid_bins.length == 0 || valid_bins[0] == -1);

                short[] feedback = FeedbackSignal.encodeFeedbackSignal(valid_bins[0], valid_bins[valid_bins.length - 1],
                        Constants.fbackTime, true, m_attempt);

                Constants.sp1 = new AudioSpeaker(av, feedback, Constants.fs, 0, feedback.length, false);
                appendToLog(Constants.SignalType.Feedback.toString());
                Constants.sp1.play(Constants.volume);

                int stime = (int) ((feedback.length / (double) Constants.fs) * 1000);
                sleep(stime+Constants.SendPad);

                double[] data_signal = null;
                if (Constants.SEND_DATA) {
                    data_signal = Utils.waitForChirp(Constants.SignalType.DataRx, m_attempt, 0);
                }
                if (data_signal!=null) {
                    Decoder.decode_helper(av, data_signal, valid_bins,m_attempt);
                }
                return 0;
            }
        }
        else if (Constants.scheme == Constants.Modulation.LoRa || Constants.scheme == Constants.Modulation.OFDM_freq_all){
            if (Constants.user.equals(Constants.User.Alice)) {
                int[] valid_bins = new int[]{20,49};

                if (Constants.SEND_DATA) {
                    appendToLog(Constants.SignalType.Data.toString());
                    if (valid_bins.length >= 1 && valid_bins[0] != -1) {
                        sendData(valid_bins, m_attempt);
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (Exception e) {
                        Log.e("asdf", e.toString());
                    }
                }
            }
            else if (Constants.user.equals(Constants.User.Bob)){
                int[] valid_bins = new int[]{20,49};

                double[] data_signal = null;
                if (Constants.SEND_DATA) {
                    // need new packet detection algorithms
                    data_signal = Utils.waitForChirp(Constants.SignalType.DataRx, m_attempt, 0);
                }



                if (data_signal!=null) {
                    Decoder.decoding(av,data_signal,m_attempt);
                }
                return 0;
            }
        }
        return 0;
    }

    public static void sendData(int[] valid_bins, int m_attempt) {
        send_data_per(valid_bins,m_attempt);
    }

    public static void send_data_helper(int numbits, int[] valid_bins, int m_attempt,
                                 Constants.SignalType sigType,Constants.ExpType expType) {
        //short[] bits = SymbolGeneration.getCodedBits(m_attempt);


        //byte[] embedding_bytes = Utils.Embedding2Bytes(Constants.SegFish);
        byte[] embedding_bytes_test = {31, 69, 72, -112, -19, -104, 60, -51, -84, -72, -112, 95, 45, -33, -118, 43, 33, 8, 111, -96, 127, 57, 37, -8, -39, -74, -91, 25, 54, -85, -123, 114, -84, -44, 92, 42, -21, -49, 90, 67, -59, 37, -103, 52, -30, -100, 50, -34, 30, 98, -22, 124, -95, -74, 97, -122, -38, 20, -45, 67, -66, 93, 117, -102, -19, 117, 118, 31, -48, -106, 125, 50, 84, 20, 40, 125, -30, 79, 22, -55};

        int[] encoded_symbol = SymbolGeneration.encode_LoRa(embedding_bytes_test,m_attempt);

        String encoded_byte = "";
        for (int i = 0; i < encoded_symbol.length; i++)
        {
            encoded_byte += (encoded_symbol[i] + ",");
        }
        Utils.log("encoded_symbol =>"+encoded_byte);
        //short[] sig_tx = SymbolGeneration.modulate_LoRa(encoded_symbol,m_attempt);

        //double[] sig_rx = new double[sig_tx.length];
        //for (int i = 0; i < sig_rx.length; i++)
        //{
        //    sig_rx[i] = (double) sig_tx[i] / 32767;
        //}

        //int[] demodulated_symbol = Decoder.demodulate(sig_rx, m_attempt);




        //String out="";
        //for (int i = 0; i < bits.length; i++) {
        //    out+=bits[i]+"";
        //}
        short[] txsig_lora = SymbolGeneration.generateDataSymbols_LoRa(encoded_symbol,true,m_attempt);
        //short[] txsig=SymbolGeneration.generateDataSymbols(bits, valid_bins, Constants.data_symreps, true, sigType,m_attempt);
        // demodulate and decoding test
        //double[] rxsig_lora = new double[txsig_lora.length];
        //for (int i = 0; i<txsig_lora.length; i++)
        //{
        //    rxsig_lora[i] = txsig_lora[i];
        //}
        //int[] symbols_d =  Decoder.demodulate(rxsig_lora, m_attempt);

        FileOperations.writetofile(MainActivity.av, txsig_lora,
                Utils.genName(sigType, m_attempt) + ".txt");

        Constants.sp1 = new AudioSpeaker(MainActivity.av, txsig_lora, Constants.fs, 0, txsig_lora.length, false); // this is where I leave to solve Mar. 19.
        Constants.sp1.play(Constants.volume);

        int sleepTime = (int) (((double) txsig_lora.length / Constants.fs) * 1000);
        sleep(sleepTime + Constants.SendPad);
    }

    public static void send_data_ber(int[] valid_bins, int m_attempt) {
        FileOperations.writetofile(MainActivity.av, Constants.codeRate.toString(),
                Utils.genName(Constants.SignalType.CodeRate,m_attempt)+".txt");
        FileOperations.writetofile(MainActivity.av, Utils.trim(Arrays.toString(valid_bins)),
                Utils.genName(Constants.SignalType.ValidBins, m_attempt) + ".txt");

        // adaptive  //////////////////////////////////////////////
        send_data_helper(valid_bins.length*Constants.Nsyms,
                valid_bins, m_attempt,
                Constants.SignalType.DataAdapt,
                Constants.ExpType.BER);
        // full bandwidth//////////////////////////////////////////////
        int[] end_bins = new int[]{79,49,29};
        Constants.SignalType[] sigTypes = new Constants.SignalType[]{
                Constants.SignalType.DataFull_1000_4000,
                Constants.SignalType.DataFull_1000_2500,
                Constants.SignalType.DataFull_1000_1500,
        };
        for (int i = 0; i < end_bins.length; i++) {
            int[] bins = generateBins(20, end_bins[i]);
            send_data_helper(bins.length * Constants.Nsyms, bins, m_attempt,
                    sigTypes[i],Constants.ExpType.BER);
        }
        //////////////////////////////////////////////
    }

    public static int[] generateBins(int bin1, int bin2) {
        int[] bins = new int[bin2-bin1+1];
        int counter=0;
        for (int i = bin1; i <= bin2; i++) {
            bins[counter++]=i;
        }
        return bins;
    }

    public static void send_data_per(int[] valid_bins, int m_attempt) {
        FileOperations.writetofile(MainActivity.av, Constants.codeRate.toString(),
                Utils.genName(Constants.SignalType.CodeRate,m_attempt)+".txt");
        FileOperations.writetofile(MainActivity.av, Utils.trim(Arrays.toString(valid_bins)),
                Utils.genName(Constants.SignalType.ValidBins, m_attempt) + ".txt");


        // newly added
        double ratio = 1.0;
        if (Constants.codeRate == Constants.CodeRate.None)
        {
            ratio = 1.0;
        }
        else  if (Constants.codeRate == Constants.CodeRate.C1_2)
        {
            ratio = 1/2.0;
        }
        else if (Constants.codeRate == Constants.CodeRate.C2_3)
        {
            ratio = 2/3.0;
        }


        int[] valid_freqs = Utils.bins2freqs(valid_bins);
        int freqSpacing = Constants.fs/Constants.Ns;
        double data_rate = (valid_freqs[valid_freqs.length - 1] - valid_freqs[0] + freqSpacing) * 0.5 ;
        FileOperations.writetofile(MainActivity.av, data_rate + "",
                Utils.genName(Constants.SignalType.DataRate, m_attempt) + ".txt");



        // calc bits//////////////////////////////////////////////
        int msgbits = 16;
        int traceDepth = 0;
        msgbits += traceDepth;

        // adapt//////////////////////////////////////////////
        if (Constants.scheme == Constants.Modulation.OFDM_freq_adapt )
        {
            send_data_helper(msgbits,
                    valid_bins, m_attempt,
                    Constants.SignalType.DataAdapt, Constants.ExpType.PER);
            Log.e("numbits","adapt "+msgbits);
        }
        else if(Constants.scheme == Constants.Modulation.OFDM_freq_all || Constants.scheme == Constants.Modulation.LoRa)
        {
            // full bandwidth////////////////////////////////////////////// utilize the full bandwidth
            int[] end_bins = new int[]{49};
            Constants.SignalType[] sigTypes = new Constants.SignalType[]{
                    //Constants.SignalType.DataFull_1000_4000,
                    Constants.SignalType.DataFull_1000_2500,
                    //Constants.SignalType.DataFull_1000_1500,
            };
            for (int i = 0; i < end_bins.length; i++) {
                int[] bins = generateBins(20, end_bins[i]);
                send_data_helper(bins.length * Constants.Nsyms, bins, m_attempt,
                        sigTypes[i],Constants.ExpType.BER);
            }
        }


    }

    public static void sleep(int s) {
        try {
            Thread.sleep(s);
        }
        catch (Exception e) {
            Utils.log(e.getMessage());
        }
    }
}
