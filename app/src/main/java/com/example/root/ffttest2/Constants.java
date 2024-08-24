package com.example.root.ffttest2;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.camera.view.PreviewView;
import androidx.core.widget.NestedScrollView;
import androidx.camera.lifecycle.ProcessCameraProvider;

import com.jjoe64.graphview.GraphView;

import org.pytorch.Module;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.google.common.util.concurrent.ListenableFuture;


public class Constants {

    // ****************************** Start of Codec Related Global Variables ******************************
    public enum Experiment {
        testExp, // for doing all test and experiment
        end2endTest, // for sending test image
        end2endCam, // for sending image captured by camere
        dataCollection
    }

    public static Experiment expMode = Experiment.testExp; // store current expMode
    public static Bitmap testExpBitmap;
    public static ArrayList<Bitmap> testEnd2EndImageBitmaps = new ArrayList<>(); // store bitmap of all test images (bypass access assets in class other than the main class)
    public static Boolean didLoadTestImages = false; // flag to guarantee bitmaps are only loaded once
    ;
    public static int end2endTestDelay = 15000; // sending delay between two test images (leave time for propagation and decode)
    public static int dataCollectionSendingDelay = 15000; // sending delay between two test instances in data collection (leave time for propagation and decode)

    public static int end2endCamDelay = 3000; // sending delay between two captured images/ camera capture time interval (leave time for propagation and decode)

    // later maybe we can add a wrapper to merge these parts to one model
    public static Module mEncoder1 = null; // encoder part 1
    public static Module mEncoder2 = null; // encoder part 2
    public static Module mEncoder3 = null; // encoder part 3

    public static Module mDecoder1 = null; // decoder part 1
    public static Module mDecoder2 = null; // decoder part 2
    public static Module mDecoder3 = null; // decoder part 3
    public static Module mEmbedding_256 = null;

    public static Module mTransformer = null; // recover transformer
    public static Module mTransformer_256 = null; // recover transformer

    public static Module newEncoder = null;
    public static Module newDecoder = null;
    public static Module newTransformer = null;

    public static int recover_round = 1;

    public static long[] encode_sequence; // encoder final output (64 long integer)

    public static int compressImageSize = 256; // rescale image for encoding

    public static Button cameraCaptureBtn;
    public static FrameLayout frameLayout;
    public static PreviewView preview;
    public static ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    public static Bitmap currentCameraCapture;
    public static String Sender_Latency_Str = "";
    public static String Receiver_Latency_Str = "";

    public static Switch logswitch, chirptypeswitch;
    public static boolean allowLog;

    public static List<String> modelIgnoreDisplayInSpinnerList = Arrays.asList("embedding_optimized.ptl",
            "encoder_optimized.ptl",
            "post_quant_conv_optimized.ptl",
            "quant_conv_optimized.ptl",
            "quantize_optimized.ptl",
            "decoder.ptl");

    public static Map<String, String> serviceNameToModelMap = new HashMap<String, String>() {{
        put("Encode Image", "VQGANEncode");
        put("Decode Image", "VQGANDecode");
        put("Recover Image", "transformer_optimized.ptl");
        put("Detect Fish", "lite_optimized_clf.ptl");
        put("Count Fish", "lite_optimized_count_fish_224_224.ptl");
        put("Seg Fish Low Res", "lite_optimized_seg_240p.ptl");
        put("Seg Fish High Res", "deepfish_scripted_optimized.ptl");


    }};

    public static float[] VolumeCandidates = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f};
    public static int[] SF_Candidates = {3, 4, 5, 6, 7};
    public static int[] FC_Candidates = {2000, 2500, 3000, 3500, 4000, 4500, 5000, 5500, 6000};
    public static int[] BW_Candidates = {1000, 2000, 2500, 3000, 3500, 4000, 4500, 5000, 5500, 6000};
    public static int[] SendDelay_Candidates = {1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 12000, 15000};
    public static int[] Mattempts_Candidates = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    public static int SendInterval = 15000; // 7000 ms between two images

    public static int spinnerStateChangeSleepTime = 300;

    public static boolean isLinearChirp = true;

    public enum NewEqualizationMethod {
        nouse, // do not use any equalization
        method1_once, // only do equalization at the beginning, no time varied channel consideration
        method2_new_freq,
        method3_tv_wo_to, // time varied channel considered, insert new preambles, simple time correct
        method4_tv_w_to, // time varied channel considered, insert new preambles, time correct, resample
        method5_tv_w_to_range // most advanced with range limit
    }

    public static NewEqualizationMethod currentEqualizationMethod = NewEqualizationMethod.nouse;

    public static double[] NonlinearCoeff = {1, 0};

    public static int BW_Equalization = 400;
    public static int SF_Equalization = 3;
    public static double Equalization_Original_Ratio = 0.8;
    public static double Equalization_Part_Ratio = 0.2;
    public static int Equalization_Range = 5;
    public static int Equalization_Gap = Constants.Equalization_Range * (Constants.Ns + Constants.Gap) - Constants.Ns_Equalization;
    public static int Equalization2_Range = 3; // insert a preamble every 5 symbols

    public static double findPeakHeightThreshold = 0.90;

    public static int findPeakMinDistanceThreshold = 8;

    public static int datacollection_times = 1;
    public static int datacollection_image_count = 1; // use the 1- datacollection_image_count image in the sendTestImages folder
    public static String datacollection_env = "air";
    public static String datacollection_distance = "1m";
    public static String datacollection_mobility = "static";
    public static String datacollection_depth = "1m";
    public static String datacollection_orientation = "0";

    public static String setup_description = "";
    public static int estimated_time_in_second = 0;

    // data collection related
    public static int datacollection_init_delay_time = 25; // init delay time, time for put phone under water, can be set by the spinner
    public static int datacollection_proposed_time = 25; // only use this
    public static int datacollection_css_time = 25;
    public static int datacollection_ofdm_adapt_time = 30; // make it longer
    public static int datacollection_ofdm_wo_adapt_time = 25;
    public static int datacollection_mode_switch_time = 20;
    public static int[] datacollection_time_delay_map;
    public static int[] datacollection_time_out_map;
    public static int datacollection_current_instance_index = 0;

    public static View overlayView = null;

    //    public static String[] all_datacollection_schemes = {"proposed", "css", "ofdm_wo_adapt", "ofdm_adapt"}; // first 3 time are predicable
    public static String[] all_datacollection_schemes = {"proposed"};


    public static String currentDirPath = "";

    public static TextView overlay_textview = null;
    public static int datacollection_total_instance_count = 0;

    public static long datacollection_send_start_time = 0;
    public static long datacollection_receive_start_time = 0;
    public static int receive_time_offset = 8; // allow 5 seconds offset

    public static char[] datacollection_receiver_res;

    public static int after_experiment_sleep_time = 5000;

    public static int gap_from_spinner = 0; // default test mode gap is 0

    public static String codebookSize = "4096"; // default 4096, 256, 1024 and 4096

    public static Map<Integer, Integer> cb_1024_to_256 = null;
    public static Map<Integer, Integer> cb_256_to_1024 = null;

    public static double soundSpeed = 340;

    public static TextView symbol_error_count_view = null;
    public static TextView embedding_error_count_view = null;
    public static long[] gt_embeddings_for_text_exp = {
            2591L, 1405L, 3845L, 1339L, 3290L, 3343L, 3995L, 3843L, 3336L, 1405L, 1469L, 3393L,
            1976L, 153L, 3244L, 726L, 3927L, 3363L, 1666L, 1987L, 3742L, 1396L, 1531L, 816L,
            156L, 2433L, 3713L, 1359L, 3753L, 3497L, 2845L, 608L, 214L, 3585L, 307L, 2149L,
            28L, 1717L, 57L, 2489L, 3791L, 1128L, 160L, 1681L, 3730L, 3360L, 2776L, 689L, 958L,
            2369L, 1318L, 1514L, 166L, 1336L, 1901L, 3724L, 1175L, 2145L, 3382L, 1449L, 433L,
            3813L, 1313L, 1412L
    };

    public static int[] gt_symbols_for_text_exp = {
            17,17,17,25,1,17,17,1,4,1,1,31,8,9,6,18,14,12,19,10,31,17,1,4,4,16,17,12,3,17,31,24,11,0,14,1,18,13,22,14,7,31,19,17,9,16,22,4,5,22,15,16,26,17,21,1,22,12,10,13,14,19,15,6,31,6,3,4,6,29,27,19,7,19,0,23,2,20,16,21,30,15,27,18,22,4,20,29,18,19,31,24,9,27,7,24,11,24,4,26,17,4,12,17,13,19,4,0,17,10,1,15,8,13,26,25,9,6,20,2,21,12,31,13,16,16,16,6,15,17,17,10,27,18,16,0,5,27,3,8,13,31,30,24,9,13,27,20,26,13,28,2,27,5,25,3,27,8,3,18,6,13,22,17,13,30,12,8,15,12,11,30,17,23,28,14,19,4,17,5,18,28,6,22,6,6,31,20,21,21,26,25,25,0,24,22,9,31,4,29,25,22,14,16,5,18,1,14,14,15,27,23,20,27,13,10,18,10,12,19,18,17,23,28,22,22,31,7,3,0,11,4,16,20,7,13,29,30,27,6,17,3,19,26,3,8,13,1,25,3,31,13,15,28,10,0,19,24,5,15,2,30,21,13,19,12,13,19,15,6,27,14,30,13 };
    // ****************************** End of Codec Related Global Variables ******************************


    public enum EqMethod {
        Freq,
        Time
    }
    public enum User {
        Alice,
        Bob
    }
    public enum SignalType {
        Sounding,
        Start,
        FreqEsts,
        AXcorr,
        ANaiser,
        ASymCheckSNR,
        ASymCheckFreq,
        Feedback,
        FeedbackFreqs,
        ExactFeedbackFreqs,
        AdaptParams,
        SNRs,
        DataRx,
        Data,
        DataAdapt,
        DataFull_1000_4000,
        DataFull_1000_2500,
        DataFull_1000_1500,
        DataChirp,

        DataNoise,
        BitsAdapt,
        BitsFull_1000_4000,
        BitsFull_1000_2500,
        BitsFull_1000_1500,
        BitsAdapt_Padding,
        BitsFull_1000_4000_Padding,
        BitsFull_1000_2500_Padding,
        BitsFull_1000_1500_Padding,
        Bit_Fill_Adapt,
        Bit_Fill_1000_4000,
        Bit_Fill_1000_2500,
        Bit_Fill_1000_1500,
        CodeRate,
        DataRate,
        SNRMethod,
        ValidBins,
        FlipSyms,
        TxBits,
        RxBits,
        Rx_Raw_Symbols,
        Rx_Symbols,
        Sent_Symbols,
        Rx_Embedding,
        Rx_Mask,
        Battery_Level,
        Timestamp,
        Before_Equalization_Rx_Raw_Symbols,
        Latency_Sender,
        Latency_Receiver,
        Raw_Input_Bitmap,
        Sent_Gt_Bitmap,
        Received_Bitmap,
        Recovered_Bitmap,
        Send_Embedding_Sequence,
        Rx_Embedding_Recovered,
        SNR_Raw_Data,
        Receiver_Success_Indicator,
        Receiver_Sensor,
        Sender_Sensor
    }
    public enum EstSignalType {
        Chirp,
        Symbol
    }
    public enum ExpType {
        BER,
        PER
    }
    public enum CodeRate {
        None,
        C4_8,
        C4_6,
        C4_7
    }
    // LoRa related
    public enum Modulation {
        OFDM_freq_adapt,
        OFDM_freq_all,
        LoRa,
        Noise,
        Chirp
    }

    public static Modulation scheme = Modulation.LoRa;

    public static boolean CRC = false; // CRC = 1 if CRC Check is enabled else 0

    public static int SF = 5; //  now support SF = 4-7

    public static int Sample_Lora = 32;

    public static int CodeRate_LoRA = 4; // (code rate = 4/8 (1:4/5 2:4/6 3:4/7 4:4/8))
    public static int LDR = 0;

    public static int BW = 2000; // bandwidth 125kHz which should be changed in the acoustic system (12.5kHz for testing)

    public static int FC = 2500;
    public static int FC_Equalization = 3800;

    public static int Gap = 0;

    public static int Preamble_Num = 2;

    public static int Center_Freq = 0;
    public static int Center_Freq_Equalization = 3800;
    public static int Center_Freq_Equalization2 = 2500;

    public static int Offset_Freq = 1000;
    public static int Offset_Freq_Equalization = 200;
    public static int Offset_Freq_Equalization2 = 1000;

    // changed after switch from lora preamble from ofdm preamble
    public static int Center_Freq_Equalization3 = 2500;
    public static int Offset_Freq_Equalization3 = 1500;




    public static int FS = 48000; // sampling rate 1Mhz which should be changed to fs = 48000hz

    public static int Ns_lora = 768;
    public static int Ns_Equalization = 960;

    public static int EmbeddindBytes = 80;

    public static float Battery_Level = 100;

    public static int Send_Delay = 3000;

    public static double[][] carrier = new double[2][Ns_lora];
    public static double[][] carrier_Equalization = new double[2][Ns_Equalization];


    public static double CFO = 0.0;

    public static int bin_num;

    public static int sample_num;

    public static boolean HasHead = false;

    public static int preamble_length = 8;

    public static boolean GRAY_CODING = true;

    public static byte[] whiteningSeq = new byte[]{
            (byte)0xff, (byte)0xfe, (byte)0xfc, (byte)0xf8, (byte)0xf0, (byte)0xe1, (byte)0xc2, (byte)0x85, (byte)0x0b, (byte)0x17, (byte)0x2f, (byte)0x5e, (byte)0xbc, (byte)0x78, (byte)0xf1, (byte)0xe3,
            (byte)0xc6, (byte)0x8d, (byte)0x1a, (byte)0x34, (byte)0x68, (byte)0xd0, (byte)0xa0, (byte)0x40, (byte)0x80, (byte)0x01, (byte)0x02, (byte)0x04, (byte)0x08, (byte)0x11, (byte)0x23, (byte)0x47,
            (byte)0x8e, (byte)0x1c, (byte)0x38, (byte)0x71, (byte)0xe2, (byte)0xc4, (byte)0x89, (byte)0x12, (byte)0x25, (byte)0x4b, (byte)0x97, (byte)0x2e, (byte)0x5c, (byte)0xb8, (byte)0x70, (byte)0xe0,
            (byte)0xc0, (byte)0x81, (byte)0x03, (byte)0x06, (byte)0x0c, (byte)0x19, (byte)0x32, (byte)0x64, (byte)0xc9, (byte)0x92, (byte)0x24, (byte)0x49, (byte)0x93, (byte)0x26, (byte)0x4d, (byte)0x9b,
            (byte)0x37, (byte)0x6e, (byte)0xdc, (byte)0xb9, (byte)0x72, (byte)0xe4, (byte)0xc8, (byte)0x90, (byte)0x20, (byte)0x41, (byte)0x82, (byte)0x05, (byte)0x0a, (byte)0x15, (byte)0x2b, (byte)0x56,
            (byte)0xad, (byte)0x5b, (byte)0xb6, (byte)0x6d, (byte)0xda, (byte)0xb5, (byte)0x6b, (byte)0xd6, (byte)0xac, (byte)0x59, (byte)0xb2, (byte)0x65, (byte)0xcb, (byte)0x96, (byte)0x2c, (byte)0x58,
            (byte)0xb0, (byte)0x61, (byte)0xc3, (byte)0x87, (byte)0x0f, (byte)0x1f, (byte)0x3e, (byte)0x7d, (byte)0xfb, (byte)0xf6, (byte)0xed, (byte)0xdb, (byte)0xb7, (byte)0x6f, (byte)0xde, (byte)0xbd,
            (byte)0x7a, (byte)0xf5, (byte)0xeb, (byte)0xd7, (byte)0xae, (byte)0x5d, (byte)0xba, (byte)0x74, (byte)0xe8, (byte)0xd1, (byte)0xa2, (byte)0x44, (byte)0x88, (byte)0x10, (byte)0x21, (byte)0x43,
            (byte)0x86, (byte)0xd,  (byte)0x1b, (byte)0x36, (byte)0x6c, (byte)0xd8, (byte)0xb1, (byte)0x63, (byte)0xc7, (byte)0x8f, (byte)0x1e, (byte)0x3c, (byte)0x79, (byte)0xf3, (byte)0xe7, (byte)0xce,
            (byte)0x9c, (byte)0x39, (byte)0x73, (byte)0xe6, (byte)0xcc, (byte)0x98, (byte)0x31, (byte)0x62, (byte)0xc5, (byte)0x8b, (byte)0x16, (byte)0x2d, (byte)0x5a, (byte)0xb4, (byte)0x69, (byte)0xd2,
            (byte)0xa4, (byte)0x48, (byte)0x91, (byte)0x22, (byte)0x45, (byte)0x8a, (byte)0x14, (byte)0x29, (byte)0x52, (byte)0xa5, (byte)0x4a, (byte)0x95, (byte)0x2a, (byte)0x54, (byte)0xa9, (byte)0x53,
            (byte)0xa7, (byte)0x4e, (byte)0x9d, (byte)0x3b, (byte)0x77, (byte)0xee, (byte)0xdd, (byte)0xbb, (byte)0x76, (byte)0xec, (byte)0xd9, (byte)0xb3, (byte)0x67, (byte)0xcf, (byte)0x9e, (byte)0x3d,
            (byte)0x7b, (byte)0xf7, (byte)0xef, (byte)0xdf, (byte)0xbf, (byte)0x7e, (byte)0xfd, (byte)0xfa, (byte)0xf4, (byte)0xe9, (byte)0xd3, (byte)0xa6, (byte)0x4c, (byte)0x99, (byte)0x33, (byte)0x66,
            (byte)0xcd, (byte)0x9a, (byte)0x35, (byte)0x6a, (byte)0xd4, (byte)0xa8, (byte)0x51, (byte)0xa3, (byte)0x46, (byte)0x8c, (byte)0x18, (byte)0x30, (byte)0x60, (byte)0xc1, (byte)0x83, (byte)0x7,
            (byte)0xe, (byte)0x1d, (byte)0x3a, (byte)0x75, (byte)0xea, (byte)0xd5, (byte)0xaa, (byte)0x55, (byte)0xab, (byte)0x57, (byte)0xaf, (byte)0x5f, (byte)0xbe, (byte)0x7c, (byte)0xf9, (byte)0xf2,
            (byte)0xe5, (byte)0xca, (byte)0x94, (byte)0x28, (byte)0x50, (byte)0xa1, (byte)0x42, (byte)0x84, (byte)0x9, (byte)0x13, (byte)0x27, (byte)0x4f, (byte)0x9f, (byte)0x3f, (byte)0x7f
    };

    public static int[][] headerChecksumMatrix = {
            {1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1},
            {0, 1, 0, 0, 1, 0, 0, 1, 1, 0, 1, 0},
            {0, 0, 1, 0, 0, 1, 0, 1, 0, 1, 1, 1},
            {0, 0, 0, 1, 0, 0, 1, 0, 1, 1, 1, 1}
    };

    // fish related
    public static boolean IsFish = false;
    public static int NumFish = 0;

    public static long[] SegFish;
    public static boolean IsDectectingFish = false;
    public static boolean IsCountingFish = false;

    public static boolean ImagingFish = true;
    public static double XCORR_MAX_VAL_HEIGHT_FAC = .8;
    public static boolean CODING = true;
    public static int XcorrVersion = 2;
    public static boolean work = false;
    public static int AliceTime = 0;
    public static int BobTime = 0;
    public static boolean FIFO = false;
    public static boolean IO = false;
    public static double SoundingOffset = .5;

    //xcorr
//    public static double MinXcorrVal = 2E13;
    public static double MinXcorrVal = 1E12;
    public static int XcorrAboveThresh = 25;
    public static int VAR_THRESH = 20;
    public static int XcorrAmpDiff = 10;
    public static int xcorr_method=2;

    public static int FEEDBACK_SNR_THRESH = 13;
    public static int CheckSymSNRThresh = 5;
//    public static int FEEDBACK_SNR_THRESH = 5;

    public static int SNR_THRESH1 = 5;
    public static int SNR_THRESH2 = 8;
    public static int SNR_THRESH2_2 = 4;

    static int SyncLag = 2;
    static int WaitForFeedbackTimeDefault = 1;
    static int WaitForSoundingTimeDefault = 1;
    static int WaitForBerTimeDefault = 12;
    static int WaitForPerTimeDefault = 4;

    static int RecorderStepSize = 24000;
    static float NaiserThresh = .6f;
    static double WaitForFeedbackTime;
    static double WaitForSoundingTime;
    static double WaitForBerTime;
    static double WaitForPerTime;
    static int AdaptationMethod = 2;
    static double WaitForDataTime = 0;

    static TextToSpeech tts = null;
    static boolean DIFFERENTIAL=true;
    static boolean INTERLEAVE=true;
    static float FreAdaptScaleFactor;

    static long StartingTimestamp;

    public static double GammaThresh = .8;

    public static int maxbits=640;
    //public static int maxbits=5;
    public static int exp_num=5;
    public static int SNR_THRESH = 10; //unused
    public static Spinner spinner,spinner2,spinner3,spinner4, spinner5, spinnerCB;
    public static CodeRate codeRate = CodeRate.None;
    public static int DATA_LEN = 32;
    public static int mattempts=1;
    public static long ts;
    public static Button startButton,clearButton,stopButton,sendButton, readyButton;
    public static Drawable defaultBackground;
    public static float volume=0.6f;
    public static TextView tv1,tv2,tv3,tv4, debugPane,tv5,tv6,tv7,tv8,tv9,tv10,tv13,tv14,tv15,tv16,tv17,tv18,tv19,tv20,tv21,msgview;
    public static ImageView imgview, imgfish;
    public static NestedScrollView sview;
    public static CountDownTimer timer;
    public static EditText et1,et2,et3,et4,et5,et6,et7,et8,et9,et10,et11,et12,et13,et14,et15,et17,et18,et25,et26,et27;
    public static SendChirpAsyncTask task;
    public static User user;
    public static Switch sw1,sw2,sw3,sw4,sw5,sw6,sw7,sw8,sw9,sw10,sw11,sw12;
    public static EqMethod eqMethod = EqMethod.Freq;
    public static String LOG="log";
    public static String LOGD="logd";
    public static int Ns=960; // original is 960
    public static int Gi=0;
    public static int[] f_range={1000,4000};
    public static int sym_eq_freq = 10;
    public static int fs = 48000;
    public static int Cp = 67;
    public static int Nsyms=100;
    public static int tap_num = 480;
    public static int initSleep=0;
    public static int sync_offset=60;

    public static int nbin1_default, nbin2_default;
    public static int subcarrier_number_default;

    public static int nbin1_chanest, nbin2_chanest;
    public static int subcarrier_number_chanest;

    public static int butterworthFiltOffset = 35;
//    public static int besselFiltOffset = 23;
    public static int besselFiltOffset = 0;

    public static int nbin1_data, nbin2_data;
    public static int subcarrier_number_data;

    public static int inc = fs/Ns;
    public static int snr_method=2;

    public static int sym_len;
    public static int blocklen;

    public static GraphView gview,gview2,gview3,gview4;
    public static int ChirpGap = 960;
    public static int[] valid_carrier_preamble;
    public static int[] valid_carrier_data;
    public static int[] valid_carrier_default;
    public static LinkedList<Integer> f_seq;
    public static int[] null_carrier = {};
    public static HashSet<Integer> pilots;

    public static boolean FLIP_SYMBOL = false;
    public static boolean stereo = false; // set recording to stereo
    public static LinkedList<String>acc;
    public static LinkedList<String>gyro;
    public static boolean sensorFlag=false;
    public static boolean imu=true;
    public static boolean writing=true;
    static Random random;
    static double feedbackSignalThreshold=40;
    static AudioSpeaker sp1;
    static OfflineRecorder _OfflineRecorder;

    static short[] pn600_bits = new short[]{1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0,1,1,0,1,1,1,0,1,1,0,0,1,1,0,1,0,1,0,1,1,1,1,1,1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0,1,1,0,1,1,1,0,1,1,0,0,1,1,0,1,0,1,0,1,1,1,1,1,1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0,1,1,0,1,1,1,0,1,1,0,0,1,1,0,1,0,1,0,1,1,1,1,1,1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0,1,1,0,1,1,1,0,1,1,0,0,1,1,0,1,0,1,0,1,1,1,1,1,1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0,1,1,0,1,1,1,0,1,1,0,0,1,1,0,1,0,1,0,1,1,1,1,1,1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0,1,1,0,1,1,1,0,1,1,0,0,1,1,0,1,0,1,0,1,1,1,1,1,1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0,1,1,0,1,1,1,0,1,1,0,0,1,1,0,1,0,1,0,1,1,1,1,1,1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0,1,1,0,1,1,1,0,1,1,0,0,1,1,0,1,0,1,0,1,1,1,1,1,1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0,1,1,0,1,1,1,0,1,1,0,0,1,1,0,1,0,1,0,1,1,1,1,1,1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1};
    static short[] pn300_bits = new short[]{1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0,1,1,0,1,1,1,0,1,1,0,0,1,1,0,1,0,1,0,1,1,1,1,1,1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0,1,1,0,1,1,1,0,1,1,0,0,1,1,0,1,0,1,0,1,1,1,1,1,1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0,1,1,0,1,1,1,0,1,1,0,0,1,1,0,1,0,1,0,1,1,1,1,1,1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0,1,1,0,1,1,1,0,1,1,0,0,1,1,0,1,0,1,0,1,1,1,1,1,1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0,1,1,0,1,1,1,0,1};
    static short[] pn120_bits = new short[]{1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0,1,1,0,1,1,1,0,1,1,0,0,1,1,0,1,0,1,0,1,1,1,1,1,1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0,1,1,0,1,1,1,0,1,1,0,0,1,1,0,1,0,1};
    static short[] pn60_bits = new short[]{1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0,1,1,0,1,1,1,0,1,1,0,0,1,1,0,1,0,1,0,1,1};
    static short[] pn40_bits = new short[]{1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0,0,1,1,1,1,0,1,0,0,0,1,1,1,0,0,1,0,0,1,0};
    static short[] pn20_bits = new short[]{1,0,0,0,0,0,1,0,0,0,0,1,1,0,0,0,1,0,1,0};

    static double[][] pn20_syms = new double[][]
    {{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1}};

    static double[][] pn40_syms = new double[][]
    {{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1}};

    static double[][] pn60_syms = new double[][]
    {{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    };

    static double[][] pn120_syms = new double[][]
    {{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1}};

    static double[][] pn300_syms = new double[][]
    {{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1}};

    static double[][] pn600_syms = new double[][]
    {{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {1,1,1,1,1},{-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},
    {1,1,1,1,1},{1,1,1,1,1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{-1,-1,-1,-1,-1},
    {-1,-1,-1,-1,-1},{-1,-1,-1,-1,-1},{1,1,1,1,1},{1,1,1,1,1},{1,1,1,1,1}};

    static boolean feedbackPreamble=false;
    static int preambleStartFreq;
    static int preambleEndFreq;
    static int chirpPreambleTime = 100; // milliseconds
    static int preambleTime = 160; // milliseconds
    static int fbackTime = 200; // milliseconds
    static boolean DecodeData = false;
    static boolean SEND_DATA = true;

    static boolean ADD_GAP = true;
    static int SendPad = 100;
    static int data_symreps = 1;
    static int chanest_symreps = 7;
    static EstSignalType est_sig = EstSignalType.Chirp;
    static boolean TEST = true;
    static short[] data_nocode;
    static short[] data12;
    static short[] data23;
    static boolean NAISER = true;
    static boolean CHECK_SYM = false;
    static int messageID=-1;
    static HashMap<Integer,String>mmap=new HashMap<>();
    static int[] cc = new int[]{7,5,10};
    static boolean SPEECH_IN=false;
    static boolean SPEECH_OUT=false;

    public static void toggleUI(boolean val) {
        Constants.sw1.setEnabled(val);
        Constants.sw2.setEnabled(val);
        Constants.sw3.setEnabled(val);
//        Constants.sw4.setEnabled(val);
//        Constants.sw5.setEnabled(val);
//        Constants.sw6.setEnabled(val);
        Constants.sw7.setEnabled(val);
        Constants.sw8.setEnabled(val);
        Constants.sw9.setEnabled(val);
        Constants.sw10.setEnabled(val);
        Constants.sw11.setEnabled(val);
//        Constants.startButton.setEnabled(val);
        Constants.clearButton.setEnabled(val);
//        Constants.stopButton.setEnabled(!val);
        Constants.et1.setEnabled(val);
        Constants.et2.setEnabled(val);
        Constants.et3.setEnabled(val);
        Constants.et4.setEnabled(val);
        Constants.et5.setEnabled(val);
        Constants.et6.setEnabled(val);
        Constants.et7.setEnabled(val);
        Constants.et8.setEnabled(val);
        Constants.et9.setEnabled(val);
        Constants.et10.setEnabled(val);
        Constants.et11.setEnabled(val);
        Constants.et12.setEnabled(val);
        Constants.et13.setEnabled(val);
        Constants.et14.setEnabled(val);
        Constants.et15.setEnabled(val);
        Constants.et17.setEnabled(val);
        Constants.et18.setEnabled(val);
        Constants.et25.setEnabled(val);
        Constants.et26.setEnabled(val);
        Constants.et27.setEnabled(val);
        Constants.spinner.setEnabled(val);
        Constants.spinner2.setEnabled(val);
        Constants.spinner3.setEnabled(val);
    }

    public static void resetRandom() {random = new Random(1);};
    public static double[] naiser = null;
    static View vv;

    static double[][] preamble_spec = null;
    public static void setup(Context cxt) {
        mmap.put(1,"Ascend");
        mmap.put(2,"Descend");
        mmap.put(3,"Something's wrong");
        mmap.put(4,"Are you okay?");
        mmap.put(5,"Okay");
        mmap.put(6,"Stop!");
        mmap.put(7,"Turn around");
        mmap.put(8,"Which way?");
        mmap.put(9,"Boat");
        mmap.put(10,"Go to buddy");
        mmap.put(11,"Hold on!");
        mmap.put(12,"Who's leading?");
        mmap.put(13,"Level off");
        mmap.put(14,"Relax");
        mmap.put(15,"Give me air!");
        mmap.put(16,"Out of air!");
        mmap.put(17,"Help!");
        mmap.put(18,"I don't know");
        mmap.put(19,"Danger over there");
        mmap.put(20,"I'm cold");
        mmap.put(21,"Look");
        mmap.put(22,"Think");
        mmap.put(23,"Ear is blocked");
        mmap.put(24,"Cut the line");
//        double[] preamble_spec1=null;
//        double[] preamble_spec2=null;

//        preamble_spec = new double[2][];
//        preamble_spec[0] = preamble_spec1;
//        preamble_spec[1] = preamble_spec2;
//        Utils.conjnative(preamble_spec);

        data_nocode = FileOperations.readrawasset_binary(cxt, R.raw.data_nocode);
        data12 = FileOperations.readrawasset_binary(cxt, R.raw.encode_data_1_2);
        data23 = FileOperations.readrawasset_binary(cxt, R.raw.encode_data_2_3);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(cxt);
        // populate UI elements
        Constants.user=User.valueOf(prefs.getString("user", User.Alice.toString()));
        sw1.setText(Constants.user.toString());
        sw1.setChecked(Constants.user.equals(User.Alice));
        Constants.sw2.setEnabled(!sw1.isChecked());

//        if (Constants.user.equals(User.Alice)) {
//            Constants.gview3.setVisibility(View.GONE);
//        }
//        else if (Constants.user.equals(User.Bob)) {
//            Constants.gview3.setVisibility(View.VISIBLE);
//        }
        Constants.gview.setVisibility(View.GONE);
        Constants.gview2.setVisibility(View.VISIBLE);

        Constants.volume=prefs.getFloat("volume",Constants.volume);
        et1.setText(Constants.volume+"");

        Constants.preambleTime=prefs.getInt("preamble_len",Constants.preambleTime);
        et2.setText(Constants.preambleTime+"");

        Constants.initSleep=prefs.getInt("init_sleep",Constants.initSleep);
        et3.setText(Constants.initSleep+"");

        Constants.DecodeData=prefs.getBoolean("decode_data", Constants.DecodeData);
        sw2.setChecked(Constants.DecodeData);

        Constants.TEST=prefs.getBoolean("test", Constants.TEST);
        sw3.setChecked(Constants.TEST);

        Constants.stereo=prefs.getBoolean("stereo", Constants.stereo);
        sw4.setChecked(Constants.stereo);

        Constants.imu=prefs.getBoolean("imu", Constants.imu);
        sw5.setChecked(Constants.imu);

        Constants.est_sig=EstSignalType.valueOf(prefs.getString("est_sig", Constants.est_sig.toString()));
        sw6.setChecked(Constants.est_sig.equals(EstSignalType.Chirp));
        sw6.setText(Constants.est_sig.toString());

        Constants.feedbackPreamble=prefs.getBoolean("feed_pre", Constants.feedbackPreamble);
        sw7.setChecked(Constants.feedbackPreamble);

        Constants.ADD_GAP=prefs.getBoolean("gap", Constants.ADD_GAP);
        sw8.setChecked(Constants.ADD_GAP);

        Constants.FLIP_SYMBOL=prefs.getBoolean("flip_symbol", Constants.FLIP_SYMBOL);
        sw9.setChecked(Constants.FLIP_SYMBOL);

        Constants.NAISER=prefs.getBoolean("naiser", Constants.NAISER);
        sw10.setChecked(Constants.NAISER);

        Constants.CHECK_SYM=prefs.getBoolean("check_sym", Constants.CHECK_SYM);
        sw11.setChecked(Constants.CHECK_SYM);

        updateNaiser(MainActivity.av);

        Constants.Nsyms=prefs.getInt("nsyms",Constants.Nsyms);
        et5.setText(Constants.Nsyms+"");

        //Constants.BW=prefs.getInt("BW",Constants.BW);
        et6.setText(Constants.BW+"");
        //Constants.FC=prefs.getInt("FC",Constants.FC);
        et7.setText(Constants.FC+"");
        Constants.SF =prefs.getInt("SF",Constants.SF);  //this code of line is used to remember the last setting
        et8.setText(Constants.SF +"");

        Constants.mattempts=prefs.getInt("mattempts",Constants.mattempts);
        et9.setText(Constants.mattempts+"");

        Constants.exp_num=prefs.getInt("exp_num",Constants.exp_num);
        et10.setText(Constants.exp_num+"");

        Constants.SyncLag=prefs.getInt("sync_lag",Constants.SyncLag);
        et11.setText(Constants.SyncLag+"");

        Constants.FreAdaptScaleFactor=prefs.getFloat("scale_factor",(float)Constants.FreAdaptScaleFactor);
        et12.setText(Constants.FreAdaptScaleFactor+"");

        Constants.SNR_THRESH2_2=(int)prefs.getInt("snr_thresh2_2",Constants.SNR_THRESH2_2);
        et13.setText(Constants.SNR_THRESH2_2+"");

        Constants.MinXcorrVal=prefs.getFloat("xcorr_thresh",(float)Constants.MinXcorrVal);
        et14.setText(Constants.MinXcorrVal+"");

        Constants.XCORR_MAX_VAL_HEIGHT_FAC=prefs.getFloat("xcorr_thresh2",(float)Constants.XCORR_MAX_VAL_HEIGHT_FAC);
        et15.setText(Constants.XCORR_MAX_VAL_HEIGHT_FAC+"");

        Constants.VAR_THRESH=prefs.getInt("var_thresh",Constants.VAR_THRESH);
        et17.setText(Constants.VAR_THRESH+"");

        Constants.XcorrAboveThresh=prefs.getInt("xcorr_above_thresh",Constants.XcorrAboveThresh);
        et18.setText(Constants.XcorrAboveThresh+"");

//        Constants.NaiserThresh=prefs.getFloat("naiser_thresh",Constants.NaiserThresh);
//        et25.setText(Constants.NaiserThresh+"");

        Constants.FEEDBACK_SNR_THRESH=prefs.getInt("feedback_thresh",Constants.FEEDBACK_SNR_THRESH);
        et26.setText(Constants.FEEDBACK_SNR_THRESH+"");

        Constants.CheckSymSNRThresh=prefs.getInt("checksym_snrthresh",Constants.CheckSymSNRThresh);
        et27.setText(Constants.CheckSymSNRThresh+"");

        preambleStartFreq = f_range[0];
        preambleEndFreq = f_range[1];

        Constants.codeRate=CodeRate.valueOf(prefs.getString("code_rate", Constants.codeRate.toString()));
        if (Constants.codeRate == Constants.CodeRate.None) {
            Constants.CodeRate_LoRA = 0;
            Constants.spinner.setSelection(0);
        }
        else if (Constants.codeRate == Constants.CodeRate.C4_8) {
            Constants.CodeRate_LoRA = 4;
            Constants.spinner.setSelection(1);
        }
        else if (Constants.codeRate == Constants.CodeRate.C4_6) {
            Constants.CodeRate_LoRA = 2;
            Constants.spinner.setSelection(2);
        } else if (Constants.codeRate == Constants.CodeRate.C4_7) {
            Constants.CodeRate_LoRA = 3;
            Constants.spinner.setSelection(3);
        }

        //String s =prefs.getString("Tx protocol",Constants.scheme.toString());
       //Log.e("snr",Constants.snr_method+"");
        if (Constants.scheme == Modulation.LoRa) {
            Constants.spinner2.setSelection(0);
        }
        else if (Constants.scheme== Constants.Modulation.OFDM_freq_adapt) {
            Constants.spinner2.setSelection(1);
        }
        else if (Constants.scheme== Modulation.OFDM_freq_all) {
            Constants.spinner2.setSelection(2);
        }
        else if (Constants.scheme== Modulation.Noise) {
            Constants.spinner2.setSelection(3);
        }
        else if (Constants.scheme== Modulation.Chirp) {
            Constants.spinner2.setSelection(4);
        }

        Constants.Send_Delay = prefs.getInt("Send Delay", Constants.Send_Delay);
        et4.setText(Constants.Send_Delay + "");

        Constants.Ns=prefs.getInt("ns",Constants.Ns);
        if (Constants.Ns==960) {
            Constants.spinner3.setSelection(0);
        }
        else if (Constants.Ns==1920) {
            Constants.spinner3.setSelection(1);
        }
        else if (Constants.Ns==4800) {
            Constants.spinner3.setSelection(2);
        }
        else if (Constants.Ns==9600) {
            Constants.spinner3.setSelection(3);
        }

        ////////////////////////////////////////////////////////////////////////////////

        updateNbins();

        updateChirp_Parameters();


        ////////////////////////////////////////////////////////

//        pilots = new HashSet<>();
//        for (int i = 0; i < Nsyms; i++) {
//            if (i%10==0) {
//                pilots.add(i);
//            }
//        }

        Log.e(LOG, String.format("number of valid carriers %d %d [%d,%d]",
                valid_carrier_default.length, subcarrier_number_default, f_seq.get(nbin1_default), f_seq.get(nbin2_default)));
    }

    public static void updateNaiser(Context cxt) {
        if (NAISER) {
            Constants.preambleTime = 195;
            naiser = FileOperations.readrawasset(cxt, R.raw.naiser3, 1);
//            preamble_spec1 = FileOperations.readrawasset(cxt, R.raw.real_naiser,1);
//            preamble_spec2 = FileOperations.readrawasset(cxt, R.raw.imag_naiser,1);
        }
        else {
//            preamble_spec1 = FileOperations.readrawasset(cxt, R.raw.real_preamble1, 1);
//            preamble_spec2 = FileOperations.readrawasset(cxt, R.raw.imag_preamble1, 1);
            if (Constants.exp_num==5) {
                Constants.preambleTime=200;
            }
            else if (Constants.exp_num==4||Constants.exp_num==3||Constants.exp_num==2||Constants.exp_num==1) {
                Constants.preambleTime=100;
            }
        }
    }

    public static void updateChirp_Parameters()
    {
        Offset_Freq = BW / 2;

        Sample_Lora = (int)Math.pow(2,Constants.SF);
        Ns_lora = (int)Math.round(FS / (double)BW * Sample_Lora);
        carrier = new double[2][Ns_lora];
        if (Constants.ADD_GAP)
        {
            // TODO: remove gap?

//            Gap = (int)(Ns_lora * 0.05);
            Gap = (int)(Ns_lora * ((float)Constants.gap_from_spinner)/((float)100));

        }

        Equalization_Gap = Constants.Equalization_Range * (Constants.Ns + Constants.Gap) - Constants.Ns_Equalization;

        double[] t = new double[Ns_lora];
        for (int i = 0; i<t.length; i++){
            t[i] = i / (double)Constants.FS ;
        }
        for (int i = 0; i< t.length; i++)
        {
            carrier[0][i] = Math.cos(2* Math.PI* Constants.FC * t[i]);
            carrier[1][i] = Math.sin(2* Math.PI* Constants.FC * t[i]);
        }

        // equalization
        double[] t_equalization = new double[Ns_Equalization];
        for (int i = 0; i<t_equalization.length; i++){
            t_equalization[i] = i / (double)Constants.FS ;
        }
        for (int i = 0; i< t_equalization.length; i++)
        {
            carrier_Equalization[0][i] = Math.cos(2* Math.PI* Constants.FC_Equalization * t_equalization[i]);
            carrier_Equalization[1][i] = Math.sin(2* Math.PI* Constants.FC_Equalization * t_equalization[i]);
        }


    }
    public static void updateNbins() {
        if (Constants.Ns == 960) {
            Cp = 67;
        }
        else if (Constants.Ns == 1920) {
            Cp = 135;
        }
        else if (Constants.Ns == 4800) {
            Cp = 336;
        }
        else if (Constants.Ns == 9600) {
            Cp = 672;
        }
        inc=fs/Ns;
        sym_len = Ns + Cp + Gi;

        nbin1_data = Math.round(f_range[0] / (inc/data_symreps));
        nbin2_data = Math.round(f_range[1] / (inc/data_symreps))-1;
        subcarrier_number_data = (nbin2_data - nbin1_data + 1)/data_symreps;

        nbin1_chanest = nbin1_data * chanest_symreps;
        nbin2_chanest = nbin1_chanest+(subcarrier_number_data*chanest_symreps);
        subcarrier_number_chanest = (nbin2_chanest - nbin1_chanest + 1)/ chanest_symreps;

        nbin1_default = Math.round(f_range[0] / inc);
        nbin2_default = Math.round(f_range[1] / inc)-1;
        subcarrier_number_default = (nbin2_default - nbin1_default + 1);

        double[] preamble = PreambleGen.preamble_d();
        blocklen = preamble.length + ChirpGap + (sym_len) * Nsyms;

        f_seq = new LinkedList<>();
        for (int i = 0; i < Ns; i++) {
            f_seq.add(inc*i);
        }

        HashSet<Integer> null_carrier_set = new HashSet<>();
        for (Integer i : null_carrier) {
            null_carrier_set.add(i);
        }

        // calculate valid carriers for preamble
        LinkedList<Integer> valid_carrier_list_preamble = new LinkedList<>();
        for (int i = nbin1_chanest; i < nbin2_chanest; i+= chanest_symreps) {
            if (!null_carrier_set.contains(i)) {
                valid_carrier_list_preamble.add(i);
            }
        }
        valid_carrier_preamble = new int[valid_carrier_list_preamble.size()];
        for (int i = 0; i < valid_carrier_list_preamble.size(); i++) {
            valid_carrier_preamble[i] = valid_carrier_list_preamble.get(i);
        }

        // calculate valid carriers for data
        LinkedList<Integer> valid_carrier_list_data = new LinkedList<>();
        for (int i = nbin1_data; i <= nbin2_data; i+=data_symreps) {
            if (!null_carrier_set.contains(i)) {
                valid_carrier_list_data.add(i);
            }
        }
        valid_carrier_data = new int[valid_carrier_list_data.size()];
        for (int i = 0; i < valid_carrier_list_data.size(); i++) {
            valid_carrier_data[i] = valid_carrier_list_data.get(i);
        }

        // calculate valid carriers for default
        LinkedList<Integer> valid_carrier_list_default = new LinkedList<>();
        for (int i = nbin1_default; i <= nbin2_default; i++) {
            if (!null_carrier_set.contains(i)) {
                valid_carrier_list_default.add(i);
            }
        }
        valid_carrier_default = new int[valid_carrier_list_default.size()];
        for (int i = 0; i < valid_carrier_list_default.size(); i++) {
            valid_carrier_default[i] = valid_carrier_list_default.get(i);
        }
    }
}
