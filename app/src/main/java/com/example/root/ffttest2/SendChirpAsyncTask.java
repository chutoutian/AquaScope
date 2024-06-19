package com.example.root.ffttest2;

import static com.example.root.ffttest2.Constants.tv4;

import android.app.Activity;
import android.app.UiAutomation;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Button;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import org.pytorch.IValue;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class SendChirpAsyncTask extends AsyncTask<Void, Void, Void> {
    Activity av;
    int num_measurements = 0;
    Button button;
    Drawable defaultBackground;
    ArrayList<Bitmap> testEnd2EndImageBitmaps;
    ImageView mImageView; // left image
    ImageView mImageView2; // right image
    String TaskID; // task create time
    public SendChirpAsyncTask(Activity activity, int num_measurements, Button button, Drawable defaultBackground, ArrayList<Bitmap> testEnd2EndImageBitmaps, ImageView mImageView, ImageView mImageView2, String TaskID) {
        this.av = activity;
        this.num_measurements = num_measurements;
        this.button = button;
        this.defaultBackground = defaultBackground;
        this.testEnd2EndImageBitmaps = testEnd2EndImageBitmaps;
        this.mImageView = mImageView;
        this.mImageView2 = mImageView2;
        this.TaskID = TaskID;
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

        if (defaultBackground != null) { // otherwise the button will be transparent
            button.setBackground(defaultBackground);
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Constants.WaitForFeedbackTime = Constants.WaitForFeedbackTimeDefault + Constants.SyncLag;
        Constants.WaitForSoundingTime = Constants.WaitForSoundingTimeDefault + Constants.SyncLag - Constants.SoundingOffset;
        Constants.WaitForBerTime = Constants.WaitForBerTimeDefault + Constants.SyncLag;
        Constants.WaitForPerTime = Constants.WaitForPerTimeDefault + Constants.SyncLag;

        Constants.SEND_DATA = true;
        Constants.WaitForDataTime = Constants.WaitForPerTime;
        Constants.AdaptationMethod = 3;

        if (Constants.allowLog) {
            FileOperations.writetofile(MainActivity.av, Constants.SNR_THRESH2 + "\n" + Constants.FreAdaptScaleFactor + "\n" + Constants.SNR_THRESH2_2,
                    Utils.genName(Constants.SignalType.AdaptParams, 0) + ".txt");
        }

        setupTimer();

        sleep(Constants.initSleep * 1000);

        Constants.StartingTimestamp = System.currentTimeMillis();
        appendToLog(Constants.SignalType.Start.toString());

        //if (Constants.user.equals(Constants.User.Alice)) {
        //    FileOperations.writetofile(MainActivity.av, Constants.FLIP_SYMBOL + "",
        //            Utils.genName(Constants.SignalType.FlipSyms, 0) + ".txt");
        //}

        /* textExp mode
        Traditional test mode.
         */
        if (Constants.expMode == Constants.Experiment.testExp) {

            if (Constants.user == Constants.User.Alice) {
                Utils.imageSendPrepare(Constants.testExpBitmap, mImageView, TaskID);
            }

            for (int i = 0; i < num_measurements; i++) {
                Utils.logd("work " + i);
                int flag = 0;
                if (i == num_measurements - 1) {
                    flag = work(i, true);
                } else {
                    flag = work(i, false);
                }
                updateTimer((i + 1) + "");
                if (flag == -1) {
                    updateTimer("-1");
                    break;
                }
            }
        }
        /* end2endTest mode
        Sender sends 5 test images to receiver.
         */
        else if (Constants.expMode == Constants.Experiment.end2endTest) {
            // end2endTest sender
            if (Constants.user.equals(Constants.User.Alice)) {
                // step 1 read out file path in testImages and convert each image to scaled bitmap
                // done in app onCreate and the bitmaps are passed as arguments

                // step 2 for each bitmap
                for (Bitmap mBitmap : testEnd2EndImageBitmaps) {

                    // step 2-1 prepare send
                    Utils.imageSendPrepare(mBitmap, mImageView, TaskID);

                    // step 2-2 send
                    work(0, true);

                    // step 2-3 sleep til next send
                    try {
                        Thread.sleep(Constants.end2endTestDelay);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            // end2endTest receiver
            else if (Constants.user.equals(Constants.User.Bob)) {
                work(0, true);
            }
        }
        /* end2endTest mode
        Sender send camera captured image to receiver.
        end2endCam just has different sender from that of end2endTest
         */
        else if (Constants.expMode == Constants.Experiment.end2endCam) {
            // end2endCam sender
            if (Constants.user.equals(Constants.User.Alice)) {

                // step 1 load the camera captured bitmap
                Bitmap mBitmap = Constants.currentCameraCapture;

                // step 2-1 prepare send
                Utils.imageSendPrepare(mBitmap, mImageView, TaskID);

                // step 2-2 send
                work(0, true);

                // step 2-3 sleep til next send
                try {
                    Thread.sleep(Constants.end2endCamDelay);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if (Constants.user.equals(Constants.User.Bob)) {
                work(0, true);
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
                String bl = Constants.Battery_Level + "";
                filename = Constants.user.toString() + "-" + Constants.SignalType.Battery_Level + "-" + "log";
                FileOperations.appendtofile(MainActivity.av, bl + "\n", filename + ".txt");
            }
            else {
                String filename = Constants.user.toString() + "-" + Constants.SignalType.Feedback + "-" + "log";
                FileOperations.appendtofile(MainActivity.av, System.currentTimeMillis() + "\n", filename + ".txt");
            }
        }
        else if (s.equals(Constants.SignalType.Battery_Level.toString()))
        {
            String bl = Constants.Battery_Level + "";
            String filename = Constants.user.toString() + "-" + Constants.SignalType.Battery_Level + "-" + "log";
            FileOperations.appendtofile(MainActivity.av, bl + "\n", filename + ".txt");
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

    // encode, generate signal, send signal
    public int work(int m_attempt, boolean skipSleep) {
        if (Constants.scheme == Constants.Modulation.OFDM_freq_adapt) {
            double[] tx_preamble = PreambleGen.preamble_d();
            if (Constants.user.equals(Constants.User.Alice)) {
                // $ ofdm time get valid bin time
                final long get_valid_bin_startTime = SystemClock.elapsedRealtime();

                int chirpLoopNumber = 0;
                double[] feedback_signal = null;
                do {
                    short[] sig = PreambleGen.sounding_signal_s();
                    if (Constants.allowLog) {
                        FileOperations.writetofile(MainActivity.av, sig, Utils.genName(Constants.SignalType.Sounding, m_attempt) + ".txt");
                    }
                    Constants.sp1 = new AudioSpeaker(av, sig, Constants.fs, 0, sig.length, false);
                    appendToLog(Constants.SignalType.Sounding.toString());
                    Constants.sp1.play(Constants.volume);

                    int sig_len = (int)(((double)sig.length/Constants.fs)*1000);
                    sleep(sig_len+Constants.SendPad);

                    feedback_signal = Utils.waitForChirp(Constants.SignalType.Feedback, m_attempt, chirpLoopNumber, TaskID);
                    chirpLoopNumber++;
                    if (chirpLoopNumber >= 3 || !Constants.work) {
                        return -1;
                    }
                } while (feedback_signal == null);

                double[] seg = Utils.segment(feedback_signal,0,24000-1);
                double[] xcorr_out = Utils.xcorr_online(tx_preamble, seg);

                int[] valid_bins = FeedbackSignal.extractSignalHelper(feedback_signal, (int)xcorr_out[1], m_attempt);

                // $ ofdm time get valid bin time
                final long validBinTime = SystemClock.elapsedRealtime() - get_valid_bin_startTime;
                Constants.Sender_Latency_Str = Constants.Sender_Latency_Str + "sender get valid bin (ms): " + validBinTime + "\n";
                Utils.log("Sender_Latency_Str: " + Constants.Sender_Latency_Str);

                if (Constants.SEND_DATA) {
                    appendToLog(Constants.SignalType.Data.toString());
                    if (valid_bins.length >= 1 && valid_bins[0] != -1) {
                        sendData(valid_bins, m_attempt);
                    }
                    if (skipSleep == false) {
                        try {
                            Thread.sleep(Constants.SendInterval);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                return 0;
            }
            else if (Constants.user.equals(Constants.User.Bob)) {
                int chirpLoopNumber = 0;
                int[] valid_bins = null;
                double[] sounding_signal = null;
                do {
                    sounding_signal = Utils.waitForChirp(Constants.SignalType.Sounding, m_attempt, chirpLoopNumber, TaskID);
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
                    data_signal = Utils.waitForData(Constants.SignalType.DataRx, m_attempt, 0, TaskID);
                }
                if (data_signal!=null) {
                    long[] embeddings = Decoder.decode_helper(av, data_signal, valid_bins,m_attempt);
                    // recover
                    long[] prediction = Utils.transformer_recover(embeddings);

                    // decode 1, before recover
                    Utils.decode_image_receiver(embeddings, mImageView, true);

                    // decode 2, after recover
                    Utils.decode_image_receiver(prediction, mImageView2, false);

                    // save receiver latency to file
                    Utils.log("Receiver_Latency_Str: " + Constants.Receiver_Latency_Str);
                    FileOperations.writetofile(MainActivity.av, Constants.Receiver_Latency_Str,
                            Utils.genName(Constants.SignalType.Latency_Receiver, m_attempt) + ".txt");
                }
                return 0;
            }
        }
        else if (Constants.scheme == Constants.Modulation.LoRa || Constants.scheme == Constants.Modulation.OFDM_freq_all){
            if (Constants.user.equals(Constants.User.Alice)) {
                int[] valid_bins = new int[]{20,49};
                if (Constants.SEND_DATA) {
                    if (valid_bins.length >= 1 && valid_bins[0] != -1) {
                        sendData(valid_bins, m_attempt);
                    }
                    if (skipSleep == false) {
                        try {
                            Thread.sleep(Constants.SendInterval);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            else if (Constants.user.equals(Constants.User.Bob)){
                int[] valid_bins = new int[]{20,49}; // TODO: fixed for 1000 - 2500?

                double[] data_signal = null;
                if (Constants.SEND_DATA) {
                    // need new packet detection algorithms
                    data_signal = Utils.waitForData(Constants.SignalType.DataRx, m_attempt, 0, TaskID);
                }
                if (data_signal!=null) {
                    if (Constants.scheme == Constants.Modulation.LoRa)
                    {
                        // get embedding
                        long[] embeddings = Decoder.decoding(av,data_signal,m_attempt);

                        // recover
                        long[] prediction = Utils.transformer_recover(embeddings);

                        // decode 1, before recover
                        Utils.decode_image_receiver(embeddings, mImageView, true);

                        // decode 2, after recover
                        Utils.decode_image_receiver(prediction, mImageView2, false);

                        // save receiver latency to file
                        Utils.log("Receiver_Latency_Str: " + Constants.Receiver_Latency_Str);
                        FileOperations.writetofile(MainActivity.av, Constants.Receiver_Latency_Str,
                                Utils.genName(Constants.SignalType.Latency_Receiver, m_attempt) + ".txt");

                        // finish
                        if (Constants.allowLog) {
                            FileOperations.writetofile(MainActivity.av, Constants.SF + "\n" + Constants.BW + "\n" + Constants.CodeRate_LoRA + "\n" + Constants.FC,
                                    Utils.genName(Constants.SignalType.AdaptParams, 0) + ".txt");
                        }
                    }
                    else if (Constants.scheme == Constants.Modulation.OFDM_freq_all)
                    {
                        long[] embeddings = Decoder.decode_helper(av,data_signal,valid_bins,m_attempt);
                        // recover
                        long[] prediction = Utils.transformer_recover(embeddings);

                        // decode 1, before recover
                        Utils.decode_image_receiver(embeddings, mImageView, true);

                        // decode 2, after recover
                        Utils.decode_image_receiver(prediction, mImageView2, false);

                        // save receiver latency to file
                        Utils.log("Receiver_Latency_Str: " + Constants.Receiver_Latency_Str);
                        FileOperations.writetofile(MainActivity.av, Constants.Receiver_Latency_Str,
                                Utils.genName(Constants.SignalType.Latency_Receiver, m_attempt) + ".txt");

                    }
                }
                return 0;
            }
        }
        else if (Constants.scheme == Constants.Modulation.Noise) {
            Utils.listen_to_noise(Constants.SignalType.DataNoise,m_attempt,0,TaskID);
            av.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utils.sendNotification(av, "Notification","Noise collected", R.drawable.warning2);

                }
            });
        }
        else if (Constants.scheme == Constants.Modulation.Chirp) {
            if (Constants.user.equals(Constants.User.Alice)) {
                sendChirp(m_attempt,Constants.SignalType.DataChirp);
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    Log.e("asdf", e.toString());
                }
            }
            else if (Constants.user.equals(Constants.User.Bob)){
                double[] chirp_signal = Utils.waitForData(Constants.SignalType.DataChirp, m_attempt, 0, TaskID); // sync, when the receiver is receiving the sound (after detecting sound) and processing the sound (demodulate, decode), all other signal will be ignored
                if (chirp_signal != null)
                {
                    if (Constants.allowLog) {
                        StringBuilder noiseBuilder = new StringBuilder();
                        for (int j = 0; j < chirp_signal.length; j++) {
                            noiseBuilder.append(chirp_signal[j]);
                            noiseBuilder.append(",");
                        }
                        String raw_chirp_signal = noiseBuilder.toString();
                        if (raw_chirp_signal.endsWith(",")) {
                            raw_chirp_signal = raw_chirp_signal.substring(0, raw_chirp_signal.length() - 1);
                        }
                        Utils.log("raw_chirp =>" + raw_chirp_signal);

                        FileOperations.writetofile(MainActivity.av, raw_chirp_signal + "",
                                Utils.genName(Constants.SignalType.DataChirp, m_attempt, 0) + ".txt");
                    }
                    av.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.sendNotification(av, "Notification","Chirp collected", R.drawable.warning2);

                        }
                    });
                }
                return 0;
            }
        }
        return 0;
    }

    public static void sendChirp(int m_attempt, Constants.SignalType sigType)
    {


        int siglen = Constants.FS / 2;
        siglen += ((Constants.preambleTime/1000.0)*Constants.fs)+Constants.ChirpGap;
        short[] txsig = new short[siglen];

        int counter = 0;
        short[] sig = PreambleGen.preamble_s();
        for (Short s : sig) {
            txsig[counter++] = s;
        }
        counter += Constants.ChirpGap;

        short[] chirp_sig = Utils.GenerateChirp_LoRa(true);
        for (Short s : chirp_sig) {
            txsig[counter++] = s;
        }

        FileOperations.writetofile(MainActivity.av, txsig,
                Utils.genName(sigType, m_attempt) + ".txt");

        Constants.sp1 = new AudioSpeaker(MainActivity.av, txsig, Constants.fs , 0, txsig.length, false); // this is where I leave to solve Mar. 19.
        Constants.sp1.play(Constants.volume);


        int sleepTime = (int) (((double) txsig.length / Constants.fs) * 1000);
        sleep(sleepTime + Constants.SendPad);
    }

    public static void sendData(int[] valid_bins, int m_attempt) {
        send_data_per(valid_bins,m_attempt);
    }

    // shared by three protocols
    public static void send_data_helper(int[] valid_bins, int m_attempt,
                                 Constants.SignalType sigType) {

//        byte[] embedding_bytes = Utils.Embedding2Bytes(Constants.SegFish);
        // sender t2 - encode image
        final long startTime_encoding_signal = SystemClock.elapsedRealtime();

        byte[] embedding_bytes_test;
//        if (Constants.expMode == Constants.Experiment.testExp) {
//            embedding_bytes_test = new byte[]{31, 69, 72, -112, -19, -104, 60, -51, -84, -72, -112, 95, 45, -33, -118, 43, 33, 8, 111, -96, 127, 57, 37, -8, -39, -74, -91, 25, 54, -85, -123, 114, -84, -44, 92, 42, -21, -49, 90, 67, -59, 37, -103, 52, -30, -100, 50, -34, 30, 98, -22, 124, -95, -74, 97, -122, -38, 20, -45, 67, -66, 93, 117, -102, -19, 117, 118, 31, -48, -106, 125, 50, 84, 20, 40, 125, -30, 79, 22, -55};
//        } else {
        byte[] embedding_bytes = Utils.Embedding2Bytes(Constants.encode_sequence);
        embedding_bytes_test = embedding_bytes;
//        }

            short[] txsig = new short[0];
        if (Constants.scheme == Constants.Modulation.LoRa)
        {
            // sender t2 - encode signal
            int[] encoded_symbol = SymbolGeneration.encode_LoRa(embedding_bytes_test,m_attempt);
            final long inferenceTime_encoding_signal = SystemClock.elapsedRealtime() - startTime_encoding_signal;
            Constants.Sender_Latency_Str = Constants.Sender_Latency_Str + "sender encode signal (ms): " + inferenceTime_encoding_signal + "\n";
            Utils.log("Sender_Latency_Str: " + Constants.Sender_Latency_Str);

            // sender t3 - generate signal
            final long startTime_generate_signal = SystemClock.elapsedRealtime();
            txsig = SymbolGeneration.generateDataSymbols_LoRa(encoded_symbol,true,m_attempt);
            final long inferenceTime_generate_signal = SystemClock.elapsedRealtime() - startTime_generate_signal;
            Constants.Sender_Latency_Str = Constants.Sender_Latency_Str + "sender generate signal (ms): " + inferenceTime_generate_signal + "\n";
            Utils.log("Sender_Latency_Str: " + Constants.Sender_Latency_Str);

        }
        else if (Constants.scheme == Constants.Modulation.OFDM_freq_all || Constants.scheme == Constants.Modulation.OFDM_freq_adapt)
        {
            // encode signal
            short[] bits = SymbolGeneration.getCodedBits(m_attempt,embedding_bytes_test);
            final long inferenceTime_encoding_signal = SystemClock.elapsedRealtime() - startTime_encoding_signal;
            Constants.Sender_Latency_Str = Constants.Sender_Latency_Str + "sender encode signal (ms): " + inferenceTime_encoding_signal + "\n";
            Utils.log("Sender_Latency_Str: " + Constants.Sender_Latency_Str);

            // generate signal
            final long startTime_generate_signal = SystemClock.elapsedRealtime();
            txsig=SymbolGeneration.generateDataSymbols(bits, valid_bins, Constants.data_symreps, true, sigType,m_attempt);
            final long inferenceTime_generate_signal = SystemClock.elapsedRealtime() - startTime_generate_signal;
            Constants.Sender_Latency_Str = Constants.Sender_Latency_Str + "sender generate signal (ms): " + inferenceTime_generate_signal + "\n";
            Utils.log("Sender_Latency_Str: " + Constants.Sender_Latency_Str);
        }

        // write send signal to txt
        if (Constants.allowLog) {
            FileOperations.writetofile(MainActivity.av, txsig,
                    Utils.genName(sigType, m_attempt) + ".txt");
        }

        // sender t4 - send signal
        final long startTime_send_signal = SystemClock.elapsedRealtime();

        Constants.sp1 = new AudioSpeaker(MainActivity.av, txsig, Constants.fs , 0, txsig.length, false); // this is where I leave to solve Mar. 19.
        Constants.sp1.play(Constants.volume);

        int sleepTime = (int) (((double) txsig.length / Constants.fs) * 1000);
        sleep(sleepTime + Constants.SendPad);

        // sender t4 - send signal
        final long inferenceTime_send_signal = SystemClock.elapsedRealtime() - startTime_send_signal;
        Constants.Sender_Latency_Str = Constants.Sender_Latency_Str + "sender send signal (ms): " + inferenceTime_send_signal + "\n";
        Utils.logd("Sender_Latency_Str: " + Constants.Sender_Latency_Str);
        FileOperations.writetofile(MainActivity.av, Constants.Sender_Latency_Str,
                Utils.genName(Constants.SignalType.Latency_Sender, m_attempt) + ".txt");
        // sender finish
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
        //FileOperations.writetofile(MainActivity.av, Constants.codeRate.toString(),
        // Utils.genName(Constants.SignalType.CodeRate,m_attempt)+".txt");


        if (Constants.scheme == Constants.Modulation.OFDM_freq_adapt  || Constants.scheme == Constants.Modulation.OFDM_freq_all)
        {
            if (Constants.allowLog) {
                FileOperations.writetofile(MainActivity.av, Utils.trim(Arrays.toString(valid_bins)),
                        Utils.genName(Constants.SignalType.ValidBins, m_attempt) + ".txt");
            }
        }

        // adapt//////////////////////////////////////////////
        if (Constants.scheme == Constants.Modulation.OFDM_freq_adapt )
        {
            send_data_helper(valid_bins, m_attempt,
                    Constants.SignalType.DataAdapt);
        }
        else if(Constants.scheme == Constants.Modulation.OFDM_freq_all)
        {
            // full bandwidth////////////////////////////////////////////// utilize the full bandwidth
            int[] end_bins = new int[]{49};
            //int[] end_bins = new int[]{79,49,29};
            // only choose 1
            // TODO: should we choose 1000 - 2500 or 1000 to 4000
            Constants.SignalType[] sigTypes = new Constants.SignalType[]{
                    //Constants.SignalType.DataFull_1000_4000,
                    Constants.SignalType.DataFull_1000_2500,
                    //Constants.SignalType.DataFull_1000_1500,
            };
            // actually there is only 1 endbin
            for (int i = 0; i < end_bins.length; i++) {
                int[] bins = generateBins(20, end_bins[i]);
                send_data_helper(bins, m_attempt,
                        sigTypes[i]);
            }
        }
        else if (Constants.scheme == Constants.Modulation.LoRa)
        {
            send_data_helper(new int[0], m_attempt,
                    Constants.SignalType.DataChirp);
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
